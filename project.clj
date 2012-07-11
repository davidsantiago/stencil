(defproject stencil "0.3.0"
  :description "Mustache in Clojure"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [slingshot "0.8.0"]
                 [org.clojure/core.cache "0.5.0"]]
  :multi-deps {"1.2" [[org.clojure/clojure "1.2.1"]]
               "1.3" [[org.clojure/clojure "1.3.0"]]
               "1.4" [[org.clojure/clojure "1.4.0-beta1"]]
               :all [[slingshot "0.8.0"]
                     [org.clojure/core.cache]]}
  :dev-dependencies [[ritz "0.2.0"]
                     [org.clojure/data.json "0.1.2"]]
  :profiles {:dev {:dependencies [[org.clojure/data.json "0.1.2"]]}}
  :extra-files-to-clean ["test/spec"])