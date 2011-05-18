(ns dali.test.spec
  (:use clojure.test
        [clojure.java.io :only [file]])
  (:require [clojure.contrib.json :as json]))

(def spec-dir "test/spec")

(defn spec-json
  []
  ;; JSON are duplicates of YAML, and we don't have a YAML parser
  ;; that can handle !code tags, so for now we use JSON.
  (filter #(.endsWith (.getName %) ".json")
          (file-seq (file spec-dir "specs"))))

(defn read-spec-file
  [^File spec-file]
  (json/read-json (slurp spec-file)))

#_(defn tests-from-spec
  "Given a spec (a list of tests), create the corresponding tests."
  [spec]
  (let [tests (:tests spec)]
    (for [test tests]
      (let [{:keys [name data expected template desc]} test]
        (deftest name
          (is (= expected (render template data)) desc))))))

