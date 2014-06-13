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

(deftest test-keywords-take-precedence-over-string-keys
  (is (= ""
         (render-string "{{a}}" {:a nil "a" "bar"})))
  (is (= "foo"
         (render-string "{{a}}" {:a "foo" "a" "bar"}))))
