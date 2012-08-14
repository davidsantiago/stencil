(defproject stencil "0.3.1-SNAPSHOT"
  :description "Mustache in Clojure"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [scout "0.1.0"]
                 [quoin "0.1.0"]
                 [slingshot "0.8.0"]
                 [org.clojure/core.cache "0.6.2"]]
  :profiles {:dev {:dependencies [[org.clojure/data.json "0.1.2"]]}
             :clj1.2 {:dependencies [[org.clojure/clojure "1.2.1"]]}
             :clj1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :clj1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :clj1.5 {:dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}}
  :aliases {"all" ["with-profile" "dev:dev,clj1.4:dev,clj1.5"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :test-paths ["test/" "target/test/spec"])
