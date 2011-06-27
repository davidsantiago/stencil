(ns stencil.ast
  (:refer-clojure :exclude [partial])
  (:require [clojure.zip :as zip]
            [clojure.string :as string])
  (:use stencil.utils))

;;
;; Data structures
;;

(defprotocol ASTZipper
  (branch? [this] "Returns true if this node can possibly have children,
                   whether it currently does or not.")
  (children [this] "When called on a branch node, returns its children.")
  (make-node [this children] "Given a node (potentially with existing children)
                              and a seq of children that should totally replace
                              the existing children, make the new node."))

(defprotocol ASTNode
  (render [this ^StrinbBuilder sb context-stack]
    "Given a StringBuilder and the current context-stack, render this node to
     the result string in the StringBuilder."))

;; Section and InvertedSection need to keep track of the raw source code of
;; their contents, since lambdas need access to that. The attrs field lets them
;; keep track of that, with fields
;;      - content-start : position in source string of content start
;;      - content-end   : position in source string of end of content
;;      - content       : string holding the raw content
(defrecord Section [name attrs contents]
  ASTZipper
  (branch? [this] true)
  (children [this] contents)
  (make-node [this children] (Section. name attrs (vec children))))
;; ASTNode IS implemented, but not here. To avoid Clojure's circular
;; dependency inadequacies, we have to implement ASTNode at the top of
;; core.clj.
(defn section [name attrs contents]
  (Section. name attrs contents))

(defrecord InvertedSection [name attrs contents]
  ASTZipper
  (branch? [this] true)
  (children [this] contents)
  (make-node [this children] (InvertedSection. name attrs (vec children)))
  ASTNode
  (render [this sb context-stack]
    ;; Only render the section if the value is not present, false, or
    ;; an empty list.
    (let [ctx (first context-stack)
          ctx-val (context-get context-stack name)]
      ;; Per the spec, a function is truthy, so we should not render.
      (if (and (not (instance? clojure.lang.Fn ctx-val))
               (or (not ctx-val)
                   (and (sequential? ctx-val)
                        (empty? ctx-val))))
        (render contents sb context-stack)))))
(defn inverted-section [name attrs contents]
  (InvertedSection. name attrs contents))

;; Partials can be obligated to indent the entire contents of the sub-template's
;; output, so we hold on to any padding here and apply it after the sub-
;; template renders.
(defrecord Partial [name padding]
  ASTZipper
  (branch? [this] false)
  (children [this] nil)
  (make-node [this children] nil))
;; ASTNode IS implemented, but not here. To avoid Clojure's circular
;; dependency inadequacies, we have to implement ASTNode at the end of
;; loader.clj.
(defn partial [name padding] (Partial. name padding))

(defrecord EscapedVariable [name]
  ASTZipper
  (branch? [this] false)
  (children [this] nil)
  (make-node [this children] nil))
;; ASTNode IS implemented, but not here. To avoid Clojure's circular
;; dependency inadequacies, we have to implement ASTNode at the top of
;; core.clj.
(defn escaped-variable [name] (EscapedVariable. name))

(defrecord UnescapedVariable [name]
  ASTZipper
  (branch? [this] false)
  (children [this] nil)
  (make-node [this children] nil))
;; ASTNode IS implemented, but not here. To avoid Clojure's circular
;; dependency inadequacies, we have to implement ASTNode at the top of
;; core.clj.
(defn unescaped-variable [name] (UnescapedVariable. name))

(extend-protocol ASTZipper
  ;; Want to be able to just stick Strings in the AST.
  java.lang.String
  (branch? [this] false)
  (children [this] nil)
  (make-node [this children] nil)
  ;; Want to be able to use vectors to create lists in the AST.
  clojure.lang.PersistentVector
  (branch? [this] true)
  (children [this] this)
  (make-node [this children] (vec children)))

(extend-protocol ASTNode
  java.lang.String
  (render [this ^StringBuilder sb context-stack] (.append sb this))
  clojure.lang.PersistentVector
  (render [this sb context-stack]
    (doseq [node this]
      (render node sb context-stack))))

;; Implement a Zipper over ASTZippers.

(defn ast-zip
  "Returns a zipper for ASTZippers, given a root ASTZipper."
  [root]
  (zip/zipper branch?
              children
              make-node
              root))