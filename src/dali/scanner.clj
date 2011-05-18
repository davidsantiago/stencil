(ns dali.scanner
  "An old-fashioned string scanner."
  (:refer-clojure :exclude [peek])
  (:import java.util.regex.Matcher))

(defrecord MatchInfo [start    ;; Start index of a match.
                      end      ;; End index of a match.
                      groups]) ;; Vector of groups.

(defrecord Scanner [src                ;; String to be matched.
                    curr-loc           ;; Current location in string.
                    ^MatchInfo match]) ;; Information about the last match.

(defn scanner
  ([source-string]
     (scanner source-string 0 nil))
  ([source-string pos]
     (scanner source-string pos nil))
  ([source-string pos match]
     (Scanner. source-string pos match)))

(defn ^{:private true} re-groups-vec
  "Clojure's re-groups will return a plain string if there is only one match
   in the group. Really inconvenient for what we're doing, so here's a similar
   function that always returns a vector of strings."
  [^Matcher m]
  (let [groupCount (.groupCount m)]
    (loop [result (transient [])
           i 0]
      (if (<= i groupCount)
        (recur (conj! result (.group m i))
               (inc i))
        (persistent! result)))))

(defn match-info
  ([^Matcher matcher]
     (match-info (.start matcher)
                 (.end matcher)
                 (re-groups-vec matcher)))
  ([start end]
     (match-info start end nil))
  ([start end groups]
     (MatchInfo. start end groups)))

;;
;; Positional information. These functions act on a Scanner and return
;; the requested values.
;;

(defn position
  "Returns the current position in the string (an integer index)."
  [^Scanner scanner]
  (:curr-loc scanner))

(defn beginning-of-line?
  "Return true if the current position is the beginning of a line."
  [^Scanner scanner]
  (let [curr-loc (:curr-loc scanner)]
    (or (= 0 curr-loc)
        (= \newline (get (:src scanner) (dec curr-loc))))))

(defn end?
  "Return true if the current position is the end of the input string."
  [^Scanner scanner]
  (>= (:curr-loc scanner) (count (:src scanner))))

(defn remainder
  "Return what remains of the string after the scan pointer."
  [^Scanner scanner]
  (let [src (:src scanner)]
    (subs src (min (:curr-loc scanner) (count src)))))

(defn groups
  "Return the groups from the last match. Remember that the first group
   will be the complete match."
  [^Scanner scanner]
  (get-in scanner [:match :groups]))

(defn matched
  "Return the last matched string."
  [^Scanner scanner]
  (first (groups scanner)))

(defn pre-match
  "Return the 'pre-match' of the last scan. This is the part of the input
   before the beginning of the match."
  [^Scanner scanner]
  (let [match (:match scanner)]
    (if match
      (subs (:src scanner) 0 (:start match)))))

(defn post-match
  "Return the 'post-match' of the last scan. This is the part of the input
   after the end of the last match."
  [^Scanner scanner]
  (let [match (:match scanner)]
    (if match
      (subs (:src scanner) (:end match)))))

;;
;; Scanning/Advancing. These functions advance the scan pointer, returning a
;; Scanner object with the new configuration.
;;

(defn scan
  "Match pattern starting at current location. On match, advances the
   current location and puts the matched string in result. Otherwise,
   just returns the same scanner, minus any previous match data."
  [^Scanner s pattern]
  (let [src (:src s)
        ;; Need to set the region to restrict the window the matcher
        ;; looks at to start at the current position.
        matcher (.region (re-matcher pattern src)
                         (position s)
                         (count src))
        match-result (if (.lookingAt matcher)
                       matcher)]
    (if match-result
      (let [mi (match-info matcher)
            matched-string (first (:groups mi))]
        (scanner src
                 (+ (position s) (count matched-string))
                 mi))
      (assoc s :match nil))))

(defn scan-until
  "Match pattern at any point after the current location. On match, advances
   the current location to the end of the match, and puts just the matching
   part in the match info. Otherwise, just returns the same scanner, minus
   any previous match data."
  [^Scanner s pattern]
  (let [src (:src s)
        matcher (.region (re-matcher pattern src)
                         (position s)
                         (count src))
        match-result (if (.find matcher)
                       matcher)]
    (if match-result
      (let [mi (match-info matcher)]
        (scanner src
                 (:end mi)
                 mi))
      ;; Remove the match data from the input scanner, since we failed to match.
      (assoc s :match nil))))

(defn skip-to-match-start
  "Match pattern at any point after the current location. On match, advances
   the current location to the beginning of the match, so that a subsequent
   scan with the same pattern will succeed. Matched pattern is stored in the
   result. Otherwise, just returns the same scanner, minus any previous match
   data."
  [^Scanner s pattern]
  (let [src (:src s)
        scan-result (scan-until s pattern)
        matched-string (matched scan-result)]
    ;; Note: scan-until may have failed, but the calculation below should work.
    (scanner src
             (- (position scan-result) (count matched-string))
             (:match scan-result))))

;;
;; Looking ahead. These functions tell you about what is further ahead in
;; the string. Return the answers instead of a new Scanner.
;;

(defn check
  "Returns what scan would return as its result."
  [^Scanner s pattern]
  (matched (scan s pattern)))

(defn check-until
  "Returns what scan-until would return as its match."
  [^Scanner s pattern]
  (matched (scan-until s pattern)))

(defn check-until-inclusive
  "Returns the string between the scanner's starting position and the end
   of what scan-until would match."
  [^Scanner s pattern]
  (let [start-pos (position s)]
    (subs (:src s) start-pos (position (scan-until s pattern)))))

(defn peek
  "Returns the string containing the next n characters after current location."
  [^Scanner s n]
  (let [remainder (remainder s)]
    (subs remainder 0 (min n
                           (count remainder)))))

