(ns stencil.loader
  (:refer-clojure :exclude [load])
  (:use [clojure.java.io :only [resource]]
        [stencil.parser :exclude [partial]]
        [stencil.ast :exclude [partial]]
        [quoin.text :as qtext]
        stencil.utils)
  (:import [java.io FileNotFoundException]))

;;
;; Support for operation without core.cache. We can't just
;; error out when core.cache isn't present, so we default to
;; an object that prints an informative error whenever it is
;; used.
;;

(def ^{:private true} no-core-cache-msg
  "Could not load core.cache. To use Stencil without core.cache, you must first use set-cache to provide a map(-like object) to use as a cache, and consult the readme to make sure you fully understand the ramifications of running Stencil this way.")

(defn- no-core-cache-ex []
  (Exception. no-core-cache-msg))

(deftype CoreCacheUnavailableStub_SeeReadme []
  clojure.lang.ILookup
  (valAt [this key] (throw (no-core-cache-ex)))
  (valAt [this key notFound] (throw (no-core-cache-ex)))
  clojure.lang.IPersistentCollection
  (count [this] (throw (no-core-cache-ex)))
  (cons [this o] (throw (no-core-cache-ex)))
  (empty [this] (throw (no-core-cache-ex)))
  (equiv [this o] (throw (no-core-cache-ex)))
  clojure.lang.Seqable
  (seq [this] (throw (no-core-cache-ex)))
  clojure.lang.Associative
  (containsKey [this key] (throw (no-core-cache-ex)))
  (entryAt [this key] (throw (no-core-cache-ex)))
  (assoc [this key val] (throw (no-core-cache-ex))))

;; The dynamic template store just maps a template name to its source code.
(def ^{:private true} dynamic-template-store (atom {}))

;; The parsed template cache maps a template name to its parsed versions.
(def ^{:private true} parsed-template-cache
  (atom (try
          (require 'clojure.core.cache)
          ((resolve 'clojure.core.cache/lru-cache-factory) {})
          (catch ExceptionInInitializerError _
            (CoreCacheUnavailableStub_SeeReadme.))
          (catch FileNotFoundException _
            (CoreCacheUnavailableStub_SeeReadme.)))))


;; Holds a cache entry
(defrecord TemplateCacheEntry [src          ;; The source code of the template
                               parsed])     ;; Parsed ASTNode structure.

(defn template-cache-entry
  "Given template source and parsed ASTNodes, creates a cache entry.
   If only source is given, parse tree is calculated automatically."
  ([src]
     (template-cache-entry src (parse src)))
  ([src parsed]
     (TemplateCacheEntry. src parsed)))

(defn set-cache
  "Takes a core.cache cache as the single argument and resets the cache to that
   cache. In particular, the cache will now follow the cache policy of the given
   cache. Also note that using this function has the effect of flushing
   the template cache."
  [cache]
  (reset! parsed-template-cache cache))

(declare invalidate-cache-entry invalidate-cache)

(defn register-template
  "Allows one to register a template in the dynamic template store. Give the
   template a name and provide its content as a string."
  [template-name content-string]
  (swap! dynamic-template-store assoc template-name content-string)
  (invalidate-cache-entry template-name))

(defn unregister-template
  "Removes the template with the given name from the dynamic template store."
  [template-name]
  (swap! dynamic-template-store dissoc template-name)
  (invalidate-cache-entry template-name))

(defn unregister-all-templates
  "Clears the dynamic template store. Also necessarily clears the template
   cache."
  []
  (reset! dynamic-template-store {})
  (invalidate-cache))

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
;; The template cache has two string keys, the template name, and a
;; secondary key that is called the variant. A variant of a template
;; is created when a partial has to change the whitespace of the
;; template (or when a user wants it), and the key is a string unless
;; it is a special value for internal use; the default variant is
;; set/fetched with :default as the variant key. Invalidating an entry
;; invalidates all variants. The variants do NOT work with "fuzzy" map
;; logic for getting/setting, they must be strings.
;;

(defn cache
  "Given a template name (string), variant key (string), template source
   (string), and optionally a parsed AST, and stores that entry in the
   template cache. Returns the parsed template."
  ([template-name template-variant template-src]
     (cache template-name template-variant template-src (parse template-src)))
  ([template-name template-variant template-src parsed-template]
     (swap! parsed-template-cache
            assoc-in [template-name template-variant]
            (template-cache-entry template-src
                                  parsed-template))
     parsed-template))

(defn invalidate-cache-entry
  "Given a template name, invalidates the cache entry for that name, if there
   is one."
  [template-name]
  (swap! parsed-template-cache dissoc template-name))

(defn invalidate-cache
  "Clears all entries out of the cache."
  []
  ;; Need to use empty to make sure we get a new cache of the same type.
  (reset! parsed-template-cache (empty @parsed-template-cache)))

(defn cache-get
  "Given a template name, attempts to fetch the template with that
   name from the template cache. If it is not in the cache, nil will
   be returned. Single argument version gets the default variant."
  ([template-name]
     (cache-get template-name :default))
  ([template-name template-variant]
     (get-in @parsed-template-cache [template-name template-variant])))


;;
;; Loader API
;;

(defn load
  "Attempts to load a mustache template by name. When given something like
   \"myfile\", it attempts to load the mustache template called myfile. First it
   will look in the dynamic template store, then look in the classpath for
   a file called myfile.mustache or just myfile.

   With additional arguments template-variant and variant-fn, supports the load
   and caching of template variants. The template-variant arg is a variant key,
   while the variant-fn arg is a single argument function that will be called
   with the template source as argument before it is cached or returned."
  ([template-name]
     (load template-name nil identity))
  ([template-name template-variant variant-fn]
     (if-let [cached (cache-get template-name template-variant)]
       (:parsed cached)
       ;; It wasn't cached, so we have to load it. Try dynamic store first.
       (if-let [dynamic-src (get @dynamic-template-store template-name)]
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
  stencil.ast.Partial
  (render [this sb context-stack]
    (let [padding (:padding this)
          template (if padding
                     (load (:name this)
                           padding
                           #(qtext/indent-string % padding))
                     (load (:name this)))]
      (when template
        (render template sb context-stack)))))
