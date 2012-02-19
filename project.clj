(defproject stencil "0.3.0"
  :description "Mustache in Clojure"
  :dependencies [[clojure "1.3.0"]
                 [slingshot "0.8.0"]]
  :multi-deps {"1.2" [[org.clojure/clojure "1.2.1"]]
               "1.3" [[org.clojure/clojure "1.3.0"]]
               "1.4" [[org.clojure/clojure "1.4.0-beta1"]]
               :all [[slingshot "0.8.0"]]}
  :dev-dependencies [[ritz "0.2.0"]
                     [org.clojure/data.json "0.1.1"]]
  :extra-files-to-clean ["test/spec"])