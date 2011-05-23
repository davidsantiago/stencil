(ns dali.core
  (:require [clojure.string :as string]
            [dali.loader :as loader])
  (:use [dali.parser :exclude [partial]]
        [dali.ast :rename {render node-render
                           partial node-partial}]
        [clojure.java.io :only [resource]]))

(defn render
  "Given a parsed template (output of load or parse) and map of args,
   renders the template."
  [template data-map]
  (let [sb (StringBuilder.)
        context-stack (conj '() data-map)]
    (node-render template sb context-stack)
    (.toString sb)))

(defn render-file
  "Given a template name (string) and map of args, loads and renders the named
   template."
  [template-name data-map]
  (render (loader/load template-name) data-map))

(defn render-string
  "Renders a given string containing the source of a template and a map
   of args."
  [template-src data-map]
  (render (parse template-src) data-map))
