(ns stencil.test.spec
  (:use clojure.test
        stencil.core
        [stencil.loader :exclude [load]])
  (:require [clojure.data.json :as json]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [stencil.utils :as utils])
  (:import [java.io FileNotFoundException]))

(def repo-url "https://github.com/mustache/spec.git")
(def spec-dir "target/test/spec")

;; Acquiring the specs

(defn spec-present?
  "Check if the spec is available in the test/spec dir. Checks for the
   existence of the specs subdir."
  []
  (.exists (io/file spec-dir "specs")))

(defn clone-spec
  "Use git to clone the specs into the spec-dir."
  []
  (try (sh/sh "git" "clone" repo-url spec-dir)
       (catch java.io.IOException e)))

(defn pull-spec-if-missing
  "Get the spec if it isn't already present."
  []
  (when (not (spec-present?))
    (clone-spec)))


;; Read specs and create tests from them.

(defn spec-json
  []
  ;; JSON are duplicates of YAML, and we don't have a YAML parser
  ;; that can handle !code tags, so for now we use JSON.
  (filter #(.endsWith (.getName %) ".json")
          (file-seq (io/file spec-dir "specs"))))

(defn read-spec-file
  [^java.io.File spec-file]
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
                   (register-template (name partial-name#) partial-src#))
                 (let [data# (compile-data-map ~data)]
                   (is (= ~expected
                          (render-string ~template data#)) ~desc))))))))

(pull-spec-if-missing)

;; We support a mode where core.cache is not present, so the tests should
;; also handle this case gracefully. When it is not present, we want to
;; ensure that the tests work with a map instead of a cache.
(when (not (utils/core-cache-present?))
  (set-cache {}))

(doseq [spec (spec-json)]
  (tests-from-spec (read-spec-file spec)))

