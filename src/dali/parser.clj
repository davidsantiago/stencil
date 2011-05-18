(ns dali.parser
  (:require [dali.scanner :as scan]
            [clojure.zip :as zip])
  (:import java.util.regex.Pattern)
  (:use dali.re-utils
        clojure.contrib.condition))

;; These tags, when used standalone (only content on a line, excluding
;; whitespace before the tag), will cause all whitespace to be removed from
;; the line.
(def standalone-tag-sigils #{\# \^ \/ \< \> \= \!})

;; These tags will allow anything in their content.
(def freeform-tag-sigils #{\! \=})

(defn closing-sigil
  "Given a sigil (char), returns what its closing sigil could possibly be."
  [sigil]
  (if (= \{ sigil)
    \}
    sigil))

(def valid-tag-content #"(\w|[?!/.-])*")

(def parser-defaults {:tag-open "{{" :tag-close "}}"})

;; The main parser data structure. The only tricky bit is the output, which is
;; a zipper. The zipper is kept in a state where new things are added with
;; append-child. This means that the current loc in the zipper is a branch
;; vector, and the actual "next location" is enforced in the code through using
;; append-child, and down or up when necessary due to the creation of a section.
;; This makes it easier to think of sections as being a stack.
(defrecord Parser [scanner       ;; The current scanner state.
                   output        ;; Current state of the output (a zipper).
                   state])       ;; Various options as the parser progresses.

(defn parser
  ([scanner]
     (parser scanner (zip/vector-zip [])))
  ([scanner output]
     (parser scanner output parser-defaults))
  ([scanner output state]
     (Parser. scanner output state)))

(defn get-line-col-from-index
  "Given a string and an index into the string, returns which line of text
   the position is on. Specifically, returns an index containing a pair of
   numbers, the row and column."
  [s idx]
  (if (> idx (count s))
    (raise :message "String index is greater than length of string."))
  (loop [lines 0
         last-line-start 0 ;; Index in string of the last line beginning seen.
         i 0]
    (cond (= i idx) ;; Reached the index, return the number of lines we saw.
          [(inc lines) (inc (- i last-line-start))] ;; Un-zero-index.
          (= "\n" (subs s i (+ 1 i)))
          (recur (inc lines) (inc i) (inc i))
          :else
          (recur lines last-line-start (inc i)))))

(defn format-location
  "Given either a scanner or a string and index into the string, return a
   message describing the location by row and column."
  ([^dali.scanner.Scanner sc]
     (format-location (:src sc) (scan/position sc)))
  ([s idx]
      (let [[line col] (get-line-col-from-index s idx)]
        (str "line " line ", column " col))))

(defn write-string-to-output
  "Given a zipper and a string, adds the string to the zipper at the current
   cursor location (as zip/append-child would) and returns the new zipper. This
   function will collate adjacent strings and remove empty strings, so use it
   when adding strings to a parser's output."
  [zipper ^String s]
  (let [preceding-value (-> zipper zip/down zip/rightmost)]
    (cond (empty? s) ;; If the string is empty, just throw it away!
          zipper
          ;; Otherwise, if the value right before the one we are trying to add
          ;; is also a string, we should replace the existing value with the
          ;; concatenation of the two.
          (and preceding-value
               (string? (zip/node preceding-value)))
          (-> zipper zip/down zip/rightmost
              (zip/replace (str (zip/node preceding-value) s)) zip/up)
          ;; Otherwise, actually append it.
          :else
          (-> zipper (zip/append-child s)))))

(defn tag-position?
  "Takes a scanner and returns true if it is currently in \"tag position.\"
   That is, if the only thing between it and the start of a tag is possibly some
   non-line-breaking whitespace padding."
  [^dali.scanner.Scanner s parser-state]
  (let [tag-open-re (re-concat #"([ \t]*)?"
                               (re-quote (:tag-open parser-state)))]
    ;; Return true if first expr makes progress.
    (not= (scan/position (scan/scan s tag-open-re))
          (scan/position s))))

(defn parse-text
  "Given a parser that is not in tag position, reads text until it is and
   appends it to the output of the parser."
  [^Parser p]
  (let [scanner (:scanner p)
        state (:state p)
        ffwd-scanner (scan/skip-to-match-start
                      scanner
                      ;; (?m) is to turn on MULTILINE mode for the pattern. This
                      ;; will make it so ^ matches embedded newlines and not
                      ;; just the start of the input string.
                      (re-concat #"(?m)(^[ \t]*)?"
                                 (re-quote (:tag-open state))))
        text (subs (:src scanner)
                   (scan/position scanner)
                   (scan/position ffwd-scanner))]
    (if (nil? (:match ffwd-scanner))
      ;; There was no match, so the remainder of input is plain text.
      ;; Jump scanner to end of input and add rest of text to output.
      (parser (scan/scanner (:src scanner) (count (:src scanner)))
              (write-string-to-output (:output p) (scan/remainder scanner))
              state)
      ;; Otherwise, add the text chunk we found.
      (parser ffwd-scanner
              (write-string-to-output (:output p) text)
              state))))

(defn parse-tag
  "Given a parser that is in tag position, reads the next tag and appends it
   to the output of the parser with appropriate processing."
  [^Parser p]
  (let [{:keys [scanner output state]} p
        beginning-of-line? (scan/beginning-of-line? scanner)
        ;; Skip and save any leading whitespace.
        padding-scanner (scan/scan scanner
                                   (re-concat #"([ \t]*)?"
                                              (re-quote (:tag-open state))))
        padding (second (scan/groups padding-scanner))
        ;; Identify the sigil (and then eat any whitespace).
        sigil-scanner (scan/scan padding-scanner
                                 #"#|\^|\/|=|!|<|>|&|\{")
        sigil (first (scan/matched sigil-scanner)) ;; first gets the char.
        sigil-scanner (scan/scan sigil-scanner #"\s*")
        ;; Scan the tag content, taking into account the content allowed by
        ;; this type of tag.
        tag-content-scanner (if (freeform-tag-sigils sigil)
                              (scan/skip-to-match-start
                               sigil-scanner
                               (re-concat #"\s*"
                                          (re-quote (closing-sigil sigil)) "?"
                                          (re-quote (:tag-close state))))
                              ;; Otherwise, restrict tag content.
                              (scan/scan sigil-scanner
                                         valid-tag-content))
        tag-content (subs (:src scanner)
                          (scan/position sigil-scanner)
                          (scan/position tag-content-scanner))
        ;; Finish the tag: any trailing whitespace, closing sigils, and tag end.
        ;; Done separately so they can succeed/fail independently.
        tag-content-scanner (scan/scan (scan/scan tag-content-scanner #"\s*")
                                       (re-quote (closing-sigil sigil)))
        close-scanner (scan/scan tag-content-scanner
                                 (re-quote (:tag-close state)))
        ;; Check if the newline comes right after... if this is a "standalone"
        ;; tag, we should remove the padding and newline.
        trailing-newline-scanner (scan/scan close-scanner #"\r?\n")
        strip-whitespace? (and beginning-of-line?
                               (standalone-tag-sigils sigil)
                               (not (nil? (:match trailing-newline-scanner))))
        ;; Go ahead and add the padding to the current state now, if we should.
        p (if strip-whitespace?
            (parser trailing-newline-scanner ;; Which has moved past newline...
                    output state)
            ;; Otherwise, need to add padding to output and leave parser with
            ;; a scanner that is looking at what came right after closing tag.
            (parser close-scanner
                    (write-string-to-output output padding)
                    state))
        {:keys [scanner output state]} p]
    ;; First, let's analyze the results and throw any errors necessary.
    (cond (empty? tag-content)
          (raise :message (str "Illegal content in tag: " tag-content
                               " at " (format-location tag-content-scanner)))
          (nil? (:match close-scanner))
          (raise :message (str "Unclosed tag: " tag-content
                               " at " (format-location close-scanner))))
    (case sigil
          (\{ \&) (parser scanner
                          (zip/append-child output
                                            [:unescaped-variable tag-content])
                          state)
          \# (parser scanner
                     (-> output
                         (zip/append-child [:section tag-content])
                         zip/down zip/rightmost)
                     state)
          \^ (parser scanner
                     (-> output
                         (zip/append-child [:inverted-section tag-content])
                         zip/down zip/rightmost)
                     state)
          \/ (let [top-section (zip/node output)] ;; Do consistency checks...
               (if (not= (second top-section) tag-content)
                 (raise :message (str "Attempt to close section out of order: "
                                      tag-content
                                      " at "
                                      (format-location tag-content-scanner)))
                 ;; Otherwise, just close it by moving up the tree.
                 (parser scanner (-> output zip/up) state)))
          ;; Just ignore comments.
          \! p
          (\> \<) (parser scanner
                          (-> output
                              (zip/append-child [:partial tag-content]))
                          state)
          ;; Set delimiters only affect parser state.
          \= (let [[tag-open tag-close]
                   (drop 1 (re-matches #"([\S|[^=]]+)\s+([\S|[^=]]+)"
                                       tag-content))]
               (parser scanner
                       output
                       (assoc state :tag-open tag-open :tag-close tag-close)))
          ;; No sigil: it was an escaped variable reference.
          (parser scanner
                  (zip/append-child output
                                    [:escaped-variable tag-content])
                  state))))

(defn parse
  [template-string]
  (loop [p (parser (scan/scanner template-string))]
    (println "parser state: " p)
    (let [s (:scanner p)]
      (cond
       ;; If we are at the end of input, return the output.
       (scan/end? s)
       (let [output (:output p)]
         ;; If we can go up from the zipper's current loc, then there is an
         ;; unclosed tag, so raise an error.
         (if (zip/up output)
           (raise :message (str "Unclosed section: "
                                (second (zip/node output))
                                " at " (format-location s)))
           (zip/root output)))
       ;; If we are in tag-position, read a tag.
       (tag-position? s (:state p))
       (recur (parse-tag p))
       ;; Otherwise, we must have some text to read. Read until the next line.
       :else
       (recur (parse-text p))))))
