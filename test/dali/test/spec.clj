(ns dali.test.spec
  (:use clojure.test
        [clojure.java.io :only [file]]
        dali.core
        [dali.loader :exclude [load]])
  (:require [clojure.contrib.json :as json]))

(def spec-dir "test/spec")

(defn spec-json
  []
  ;; JSON are duplicates of YAML, and we don't have a YAML parser
  ;; that can handle !code tags, so for now we use JSON.
  (filter #(and (.endsWith (.getName %) ".json")
                (not (.startsWith (.getName %) "~")))
          (file-seq (file spec-dir "specs"))))

(defn read-spec-file
  [^File spec-file]
  (json/read-json (slurp spec-file)))

(defn tests-from-spec
  "Given a spec (a list of tests), create the corresponding tests."
  [spec]
  (let [tests (:tests spec)]
    (doseq [test tests]
      (let [{:keys [name data expected template desc partials]} test]
        ;; If there are partials, register them before test clauses.
        #_(eval `(do (def ~(symbol name)
                     (fn [] (test-var (var ~(symbol name)))))
                   (alter-meta! (var ~(symbol name))
                                assoc :test
                                (fn []
                                  (is (= ~expected
                                         (render-string ~template
                                                        ~data))
                                      ~desc)))))
        (eval `(deftest ~(symbol name)
                 (doseq [[partial-name# partial-src#] ~partials]
                   (register-template partial-name# partial-src#))
                 
                 (is (= ~expected (render-string ~template ~data)) ~desc)))))))

(doseq [spec (spec-json)]
  (tests-from-spec (read-spec-file spec)))

