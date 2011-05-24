(ns stencil.test.re-utils
  (:use clojure.test
        stencil.re-utils))

(deftest test-re-concat
  ;; Obviously regular expressions don't have a sensible way of comparing for
  ;; equivalent expressions (ie, (= #"a" #"a") -> false). So just compare the
  ;; string version in these tests.
  (is (= "test" (str (re-concat #"t" #"e" #"s" #"t"))))
  (is (= "test" (str (re-concat "t" "e" "s" "t"))))
  (is (= "test" (str (re-concat #"te" "st")))))

(deftest test-re-quote
  (is (= java.util.regex.Pattern (type (re-quote "test"))))
  (is (= "\\Qtest^|?\\E" (str (re-quote "test^|?")))))
