(defproject stencil "0.2.0-SNAPSHOT"
  :description "Mustache in Clojure"
  :dependencies [[clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clj "0.1.7-SNAPSHOT"]
                     [org.clojure/clojure-contrib "1.2.0"]]
  :tasks [cake.tasks.swank-clj stencil.cake.tasks])