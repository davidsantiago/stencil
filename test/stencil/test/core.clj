(ns stencil.test.core
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

;; Test case to make sure we can lookup by index

(deftest index-lookup-test
  (is (= "5" (render-string "{{a.0}}" {:a [5]})))
  (is (= "23" (render-string "{{a.0.b}}" {:a [{:b 23}]})))
  (is (= "42" (render-string "{{{a.0.b}}}" {:a {"0" {:b 42}}}))))

