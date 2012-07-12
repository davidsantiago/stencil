(ns stencil.utils
  (:require [clojure.string :as str]))

(defn html-escape
  "HTML-escapes the given string."
  [^String s]
  (let [sb (StringBuilder.)]
    (loop [idx 0]
      (if (>= idx (count s))
        (.toString sb)
        (let [c (.charAt s idx)]
          (case c
            \& (.append sb "&amp;")
            \< (.append sb "&lt;")
            \> (.append sb "&gt;")
            \" (.append sb "&quot;")
            (.append sb c))
          (recur (inc idx)))))))

(defn indent-string
  "Given a String s, indents each line by inserting the string indentation
   at the beginning."
  [^String s ^String indentation]
  (let [str+padding (StringBuilder.)]
    (loop [start-idx 0
           next-idx (.indexOf s "\n")] ;; \n handles both \r\n & \n linebreaks.
      (if (= -1 next-idx)
        ;; We've reached the end. If the start and end are the same, don't
        ;; indent before an empty string. Either way, return the string. 
        (do (when (not= start-idx (count s))
              (.append str+padding indentation)
              (.append str+padding s start-idx (count s)))
            (.toString str+padding))
        (let [next-idx (inc next-idx)]
          (.append str+padding indentation)
          (.append str+padding s start-idx next-idx)
          (recur next-idx (.indexOf s "\n" next-idx)))))))

;;
;; Fuzzy map access routines
;;

(defn contains-fuzzy?
  "Given a map and a key, returns \"true\" if the map contains the key, allowing
   for differences of type between string and keyword. That is, :blah and
   \"blah\" are the same key. The key of the same type is preferred. Returns
   the variant of the found key for true, nil for false."
  ([map key] (contains-fuzzy? map key nil))
  ([map key not-found]
      (if (contains? map key)
        key
        (let [str-key (name key)]
          (if (contains? map str-key)
            str-key
            (let [kw-key (keyword key)]
              (if (contains? map kw-key)
                kw-key
                not-found)))))))

(defn get-fuzzy
  "Given a map and a key, gets the value out of the map, trying various
   permitted combinations of the key. Key can be either a keyword or string,
   and is tried first as it is, before being converted to the other."
  ([map key]
     (get-fuzzy map key nil))
  ([map key not-found]
     (or (get map key)
         (get map (name key))
         (get map (keyword key))
         not-found)))

(defn assoc-fuzzy
  "Just like clojure.core/assoc, except considers keys that are keywords and
   strings equivalent. That is, if you assoc :keyword into a map with a key
   \"keyword\", the latter is replaced."
  ([map key val]
     (let [found-key (contains-fuzzy? map key key)]
       (assoc map found-key val)))
  ([map key val & kvs]
     (let [new-map (assoc-fuzzy map key val)]
       (if kvs
         (recur new-map (first kvs) (second kvs) (nnext kvs))
         new-map))))

(defn dissoc-fuzzy
  "Given a map and key(s), returns a map without the mappings for the keys,
   allowing for the keys to be certain combinations (ie, string/keyword are
   equivalent)."
  ([map] map)
  ([map key]
     (if-let [found-key (contains-fuzzy? map key)]
       (dissoc map found-key)))
  ([map key & ks]
     (let [new-map (dissoc-fuzzy map key)]
       (if ks
         (recur new-map (first ks) (next ks))
         new-map))))

;;
;; Context stack access logic
;;
;; find-containing-context and context-get are a significant portion of
;; execution time during rendering, so they are written in a less beautiful
;; way to make them go faster.
;;

(defn find-containing-context
  "Given a context stack and a key, walks down the context stack until it
   finds a context that contains the key. The key logic is fuzzy as in
   get-fuzzy/contains-fuzzy?. Returns the context, not the key's value,
   so nil when no context is found that contains the key."
  [context-stack key]
  (loop [curr-context-stack context-stack]
    (if-let [context-top (peek curr-context-stack)]
      (if (contains-fuzzy? context-top key)
        context-top
        ;; Didn't have the key, so walk down the stack.
        (recur (next curr-context-stack)))
      ;; Either ran out of context stack or key, in either case, we were
      ;; unsuccessful in finding the key.
      nil)))

(defn context-get
  "Given a context stack and key, implements the rules for getting the
   key out of the context stack (see interpolation.yml in the spec). The
   key is assumed to be either the special keyword :implicit-top, or a list of
   strings or keywords."
  ([context-stack key]
     (context-get context-stack key nil))
  ([context-stack key not-found]
     ;; First need to check for an implicit top reference.
     (if (.equals :implicit-top key) ;; .equals is faster than =
       (first context-stack)
       ;; Walk down the context stack until we find one that has the
       ;; first part of the key.
       (if-let [matching-context (find-containing-context context-stack
                                                          (first key))]
         ;; If we found a matching context and there are still segments of the
         ;; key left, we repeat the process using only the matching context as
         ;; the context stack.
         (if (next key)
           (recur (list (get-fuzzy matching-context
                                   (first key))) ;; Singleton ctx stack.
                  (next key)
                  not-found)
           ;; Otherwise, we found the item!
           (get-fuzzy matching-context (first key)))
         ;; Didn't find a matching context.
         not-found))))

(defn call-lambda
  "Calls a lambda function, respecting the options given in its metadata, if
   any. The content arg is the content of the tag being processed as a lambda in
   the template, and the context arg is the current context at this point in the
   processing. The latter will be ignored unless metadata directs otherwise.
 
   Respected metadata:
     - :stencil/pass-context: passes the current context to the lambda as the
       second arg."
  ([lambda-fn context]
     (if (:stencil/pass-context (meta lambda-fn))
       (lambda-fn context)
       (lambda-fn)))
  ([lambda-fn content context]
      (if (:stencil/pass-context (meta lambda-fn))
        (lambda-fn content context)
        (lambda-fn content))))