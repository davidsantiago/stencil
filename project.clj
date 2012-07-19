(defproject stencil "0.3.0-SNAPSHOT"
  :description "Mustache in Clojure"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [scout "0.1.0"]
                 [quoin "0.1.0"]
                 [slingshot "0.8.0"]
                 [org.clojure/core.cache "0.6.1"]]
  :profiles {:dev {:dependencies [[org.clojure/data.json "0.1.2"]]}
             :clj1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :clj1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :clj1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}}
  :extra-files-to-clean ["test/spec"])