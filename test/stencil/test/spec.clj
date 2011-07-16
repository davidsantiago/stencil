(ns stencil.test.spec
  (:use clojure.test
        [clojure.java.io :only [file]]
        stencil.core
        [stencil.loader :exclude [load]])
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

(defn compile-data-map
  "Given the data map for a test, compiles the clojure lambdas for any keys
   that have as their value maps with a key :__tag__ with value \"code\".
   Should pass through maps that don't have such keys."
  [data-map]
  (into {} (for [[key val] data-map]
             (if (and (map? val)
                      (contains? val :__tag__)
                      (= "code" (:__tag__ val)))
               [key (load-string (:clojure val))]
               [key val]))))

(defn tests-from-spec
  "Given a spec (a list of tests), create the corresponding tests."
  [spec]
  (let [tests (:tests spec)]
    (doseq [test tests]
      (let [{:keys [name data expected template desc partials]} test]
        ;; If there are partials, register them before test clauses.
        (eval `(deftest ~(symbol name)
                 ;; Clear the dynamic template store to ensure a clean env.
                 (unregister-all-templates)
                 (doseq [[partial-name# partial-src#] ~partials]
                   (register-template partial-name# partial-src#))
                 (let [data# (compile-data-map ~data)]
                   (is (= ~expected
                          (render-string ~template data#)) ~desc))))))))

(doseq [spec (spec-json)]
  (tests-from-spec (read-spec-file spec)))

