(ns stencil.utils
  (:require [clojure.string :as str]
            [quoin.map-access :as map])
  (:import [java.io FileNotFoundException]))

;;
;; Context stack access logic
;;
;; find-containing-context and context-get are a significant portion of
;; execution time during rendering, so they are written in a less beautiful
;; way to make them go faster.
;;

(defn find-containing-context
  "Given a context stack and a key, walks down the context stack until
   it finds a context that contains the key. The key logic is fuzzy as
   in get-named/contains-named? in quoin. Returns the context, not the
   key's value, so nil when no context is found that contains the
   key."
  [context-stack key]
  (loop [curr-context-stack context-stack]
    (if-let [context-top (peek curr-context-stack)]
      (if (and (associative? context-top)
               (map/contains-named? context-top key))
        context-top
        ;; Didn't have the key, so walk down the stack.
        (recur (next curr-context-stack)))
      ;; Either ran out of context stack or key, in either case, we were
      ;; unsuccessful in finding the key.
      nil)))

(defn get-named
  "Fixed version of quoin's get-named which doesn't get confused on nils values."
  [map key]
  (get map (map/contains-named? map key)))

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
           (recur (list (get-named matching-context
                                   (first key))) ;; Singleton ctx stack.
                  (next key)
                  not-found)
           ;; Otherwise, we found the item!
           (get-named matching-context (first key)))
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

(defn core-cache-present?
  "Returns true if the core.cache library is available, and false otherwise."
  []
  (try
    (require 'clojure.core.cache)
    true
    (catch FileNotFoundException _
      false)))
