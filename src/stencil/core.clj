(ns stencil.core
  (:require [clojure.string :as string]
            [stencil.loader :as loader])
  (:use [stencil.parser :exclude [partial]]
        [stencil.ast :rename {render node-render
                              partial node-partial}]
        [quoin.text :as qtext]
        [clojure.java.io :only [resource]]
        stencil.utils))

(declare render)
(declare render-string)
(declare missing-string)

;; This is stupid. Clojure can't do circular dependencies between namespaces
;; at all. Some types need access to render/render-string to do what they are
;; supposed to do. But render-string depends on parser, parser depends on ast,
;; and to implement, ast would have to depend on core. So instead of doing what
;; Clojure wants you to do, and jam it all into one huge file, we're going to
;; just implement ASTNode for some of the ASTNode types here.

(extend-protocol ASTNode
  stencil.ast.Section
  (render [this ^StringBuilder sb context-stack opts]
    (let [ctx-val (context-get context-stack (:name this))]
      (cond (or (not ctx-val) ;; "False" or the empty list -> do nothing.
                (and (sequential? ctx-val)
                     (empty? ctx-val)))
            nil
            ;; Non-empty list -> Display content once for each item in list.
            (sequential? ctx-val)
            (doseq [val ctx-val]
              ;; For each render, push the value to top of context stack.
              (node-render (:contents this) sb (conj context-stack val) opts))
            ;; Callable value -> Invoke it with the literal block of src text.
            (instance? clojure.lang.Fn ctx-val)
            (let [current-context (first context-stack)
                  lambda-return (call-lambda ctx-val (:content (:attrs this))
                                             current-context)]
              ;; We have to manually parse because the spec says lambdas in
              ;; sections get parsed with the current parser delimiters.
              (.append sb (render (parse lambda-return
                                         (select-keys (:attrs this)
                                                      [:tag-open :tag-close]))
                                         current-context opts)))
            ;; Non-false non-list value -> Display content once.
            :else
            (node-render (:contents this) sb (conj context-stack ctx-val) opts))))
  stencil.ast.EscapedVariable
  (render [this ^StringBuilder sb context-stack opts]
    (let [cursor (:name this)
          value (context-get context-stack cursor)]
      ;; Need to explicitly check for nilness so we render boolean false.
      (if (not (nil? value))
        (if (instance? clojure.lang.Fn value)
          (.append sb (qtext/html-escape
                       (render-string (str (call-lambda value
                                                        (first context-stack)))
                                      (first context-stack))))
          ;; Otherwise, just append its html-escaped value by default.
          (.append sb (qtext/html-escape (str value))))
        (.append sb (missing-string cursor opts)))))
  stencil.ast.UnescapedVariable
  (render [this ^StringBuilder sb context-stack opts]
    (let [cursor (:name this)
          value (context-get context-stack cursor)]
      ;; Need to explicitly check for nilness so we render boolean false.
      (if (not (nil? value))
        (if (instance? clojure.lang.Fn value)
          (.append sb (render-string (str (call-lambda value
                                                       (first context-stack)))
                                     (first context-stack)))
          ;; Otherwise, just append its value.
          (.append sb value))
        (.append sb (missing-string cursor (assoc opts :classifier ::unescaped)))))))

(defn missing-string
  "Determine what to return in the case of a missing replacement variable"
  [cursor {:keys [replace-missing-vars classifier] :as opts
           :or {replace-missing-vars true classifier ""}}]
  (if replace-missing-vars ""
    (let [var (->> (map name cursor) (clojure.string/join "."))
          classifier (if (= classifier ::unescaped) "& " classifier)]
      (str "{{" classifier var "}}"))))

(defn render
  "Given a parsed template (output of load or parse) and map of args,
   renders the template."
  [template data-map opts]
  (let [sb (StringBuilder.)
        context-stack (conj '() data-map)]
    (node-render template sb context-stack opts)
    (.toString sb)))

(defn render-file
  "Given a template name (string) and map of args, loads and renders the named
   template."
  [template-name data-map & {:as opts}]
  (render (loader/load template-name) data-map opts))

(defn render-string
  "Renders a given string containing the source of a template and a map
   of args."
  [template-src data-map & {:as opts}]
  (render (parse template-src) data-map opts))
