(defproject dali "0.1.0-SNAPSHOT"
  :description "Mustache in Clojure"
  :dependencies [[clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clj "0.1.4"] 
                     [org.clojure/clojure-contrib "1.2.0"]]
  :tasks [cake.tasks.swank-clj dali.cake.tasks])