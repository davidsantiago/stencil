(ns stencil.test.core
  (:require [clojure.edn :as edn])
  (:use clojure.test
        stencil.core))

;; Test case to make sure we don't get a regression on inverted sections with
;; list values for a name.

(deftest inverted-section-list-key-test
  (is (= ""
         (render-string "{{^a}}a{{b}}a{{/a}}" {:a [:b "11"]})))
  (is (= ""
         (render-string "{{^a}}a{{b}}a{{/a}}" {"a" ["b" "11"]}))))

;; Test case to make sure we print a boolean false as "false"

(deftest boolean-false-print-test
  (is (= "false" (render-string "{{a}}" {:a false})))
  (is (= "false" (render-string "{{{a}}}" {:a false}))))

(deftest custom-evaluate-test
  (testing "Special syntax {{()}} that behaves like a Lambda called evaluate"

    (is (= "hello "
           (render-string "hello {{(inc 1)}}" {}))
        "produces empty string when no evaluate function exists in context")

    (is (= "hello hi(inc 1)"
           (render-string "hello {{(inc 1)}}"
                          {:evaluate (fn [x]
                                       (str "hi" x))}))
        "when an evaluate function is provided, it is passed the text inside the tag")

    (is (= "hello 2"
           (render-string "hello {{(inc 1)}}"
                          {:evaluate (fn [x]
                                       (eval (read-string x)))}))
        "the evaluate function can be interpreted as logic")

    (is (thrown? Exception
           (render-string "hello {{(drop tables)}}"
                          {:evaluate (fn [x]
                                       (let [[op a b] (edn/read-string x)]
                                         (if (= op '+)
                                           (+ a b)
                                           (throw (ex-info "Unknown opperand" {:op op})))))}))
        "custom evaluators should avoid eval for public facing inputs")))
