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

;; Configure behavior for "missing" vars

(deftest configure-missing-vars-test
  (let [test-string "Hello, {{world}}!"]
    (is (= "Hello, David!" (render-string test-string {} :missing-var-fn (constantly "David"))))
    (is (= test-string (render-string test-string {} :missing-var-fn :ignore)))
    (is (thrown? Exception (render-string test-string {} :missing-var-fn :throw)))))
