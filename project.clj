(defproject stencil "0.2.0"
  :description "Mustache in Clojure"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [slingshot           "0.8.0"]]
  :profiles {:dev {:dependencies [[ritz "0.2.0"]
                                  [org.clojure/data.json "0.1.2"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}}
  :aliases  {"all" ["with-profile" "dev:dev,1.4:dev,1.5"]}
  :extra-files-to-clean ["test/spec"]
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :min-lein-version "2.0.0")
