(ns dali.loader
  (:refer-clojure :exclude [load])
  (:use [clojure.java.io :only [resource]]
        [dali.parser :exclude [partial]]
        [dali.ast :exclude [partial]]
        dali.utils)
  (:import java.util.Date))

;; The dynamic template store just maps a template name to its source code.
(def ^{:private true} dynamic-template-store (atom {}))

;; The parsed template cache maps template names to its parsed versions.
(def ^{:private true} parsed-template-cache (atom {}))

;;
;; Cache policies
;;

(defn cache-forever
  "This cache policy will let entries live on forever (until explicitly
   invalidated). Could be useful in production if mustache templates can't be
   changed in that environment."
  [cache-entry]
  true)

(defn cache-never
  "This cache policy will consider cache entries to never be valid, essentially
   disabling caching. Could be useful for development."
  [cache-entry]
  false)

(defn cache-timeout
  "This is a cache policy generator. Takes a timeout in milliseconds as an
   argument and returns a cache policy that considers cache entries valid for
   only that long."
  [timeout-ms]
  (fn [cache-entry]
    (let [now (Date.)]
      (< (.getTime now)
         (+ (.getTime (:entry-date cache-entry))
            timeout-ms)))))

;; Cache policy dictates when a given cache entry is valid. It should be a
;; function that takes an entry and returns true if it is still valid.
;; By default, caches templates for 5 seconds.
(def ^{:private true} cache-policy (atom (cache-timeout 5000)))

;; Holds a cache entry
(defrecord TemplateCacheEntry [src          ;; The source code of the template
                               parsed       ;; Parsed ASTNode structure.
                               entry-date]) ;; Date when we cached this.

(defn template-cache-entry
  "Given template source, parsed ASTNodes, and timestamp, creates a cache entry.
   If only source is given, parsed and timestamp are calculated automatically."
  ([src]
     (template-cache-entry src (parse src)))
  ([src parsed]
     (template-cache-entry src parsed (Date.)))
  ([src parsed timestamp]
     (TemplateCacheEntry. src parsed timestamp)))

(declare invalidate-cache-entry)

(defn register-template
  "Allows one to register a template in the dynamic template store. Give the
   template a name and provide its content as a string."
  [template-name content-string]
  (swap! dynamic-template-store assoc-fuzzy template-name content-string)
  (invalidate-cache-entry template-name))

(defn find-file
  "Given a name of a mustache template, attempts to find the corresponding
   file. Returns a URL if found, nil if not. First tries to find
   filename.mustache on the classpath. Failing that, looks for filename on the
   classpath. Note that you can use slashes as path separators to find a file
   in a subdirectory."
  [template-name]
  (if-let [file-url (resource (str template-name ".mustache"))]
    file-url
    (if-let [file-url (resource template-name)]
      file-url)))

;;
;; Cache mechanics
;;
;; The template cache has two keys, the template name, and a secondary key that
;; is called the variant. The default variant is set/fetched with nil as the
;; variant key. Invalidating an entry invalidates all variants. The variants
;; do NOT work with "fuzzy" map logic for getting/setting.
;;

(defn cache-assoc
  "Function used to make atomic updates to the cache. Inserts val at the
   hierarchical position in the map given by the pair of keys template-name and
   template-variant. The first key (template-name) is fuzzy, the variant is
   not."
  [map [template-name template-variant] val]
  (let [template-variants (get-fuzzy map template-name)]
    (assoc-fuzzy map template-name (assoc template-variants
                                     template-variant val))))

(defn cache
  "Given a template name, variant key, template source, and parsed AST,
   stores that entry in the template cache. Returns the parsed template"
  ([template-name template-variant template-src]
     (cache template-name template-variant template-src (parse template-src)))
  ([template-name template-variant template-src parsed-template]
     (swap! parsed-template-cache
            cache-assoc [template-name template-variant]
            (template-cache-entry template-src
                                  parsed-template))
     parsed-template))

(defn invalidate-cache-entry
  "Given a template name, invalidates the cache entry for that name, if there
   is one."
  [template-name]
  (swap! parsed-template-cache dissoc-fuzzy template-name))

(defn cache-get
  "Given a template name, attempts to fetch the template with that name from
   the template cache. Will apply the cache policy, so if the cache policy says
   the entry is too old, it will return nil. Otherwise, returns the
   cache-entry. Single argument version gets the default (nil) variant."
  ([template-name]
     (cache-get template-name nil))
  ([template-name template-variant]
     (let [cache-entry (get (get-fuzzy @parsed-template-cache template-name)
                            template-variant)]
       (when (and (not (nil? cache-entry))
                  (@cache-policy cache-entry))
         cache-entry))))

(defn set-cache-policy
  "Sets the function given as an argument to be the cache policy function (takes
   a cache-entry as argument, returns true if it is still valid)."
  [new-cache-policy-fn]
  (reset! cache-policy new-cache-policy-fn))

;;
;; Loader API
;;

(defn load
  "Attempts to load a mustache template by name. When given something like
   \"myfile\", it attempts to load the mustache template called myfile. First it
   will look in the dynamic template store, then look in the classpath for
   a file called myfile.mustache or just myfile.

   With addition arguments template-variant and variant-fn, supports the load
   and caching of template variants. The template-variant arg is a variant key,
   while the variant-fn arg is a single argument function that will be called
   with the template source as argument before it is cached or returned."
  ([template-name]
     (load template-name nil identity))
  ([template-name template-variant variant-fn]
     (if-let [cached (cache-get template-name template-variant)]
       (:parsed cached)
       ;; It wasn't cached, so we have to load it. Try dynamic store first.
       (if-let [dynamic-src (get-fuzzy @dynamic-template-store template-name)]
         ;; If found, parse and cache it, then return it.
         (cache template-name template-variant (variant-fn dynamic-src))
         ;; Otherwise, try to load it from disk.
         (if-let [file-url (find-file template-name)]
           (let [template-src (slurp file-url)]
             (cache template-name
                    template-variant
                    (variant-fn template-src))))))))

;; This is stupid. Clojure can't do circular dependencies between namespaces
;; at all. Partials need access to load to do what they are supposed to do.
;; But loader depends on parser, parser depends on ast, and to implement, ast
;; would have to depend on loader. So instead of doing what Clojure wants you
;; to do, and jam it all into one huge file, we're going to just implement
;; ASTNode for Partial here.
(extend-protocol ASTNode
  dali.ast.Partial
  (render [this sb context-stack]
    (let [padding (:padding this)
          template (if padding
                     (load (:name this) padding #(indent-string % padding))
                     (load (:name this)))]
      (render template sb context-stack))))
