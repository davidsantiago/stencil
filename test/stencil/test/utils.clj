(ns stencil.test.utils
  (:use clojure.test
        stencil.utils))

(deftest test-find-containing-context
  (is (= {:a 1}
         (find-containing-context '({:a 1}) :a)))
  (is (= {:a 1}
         (find-containing-context '({:a 1}) "a")))
  (is (= {:a 1}
         (find-containing-context '({:b 2} {:a 1}) "a"))))

(deftest test-context-get
  (is (= "success"
         (context-get '({:a "success"})
                      ["a"])))
  (is (= "success"
         (context-get '({:a {:b "success"}})
                      ["a" :b])))
  (is (= "success"
         (context-get '({:b 1} {:a "success"})
                      ["a"])))
  (is (= "failure"
         (context-get '({:a "problem?"} {:a {:b "success"}})
                      ["a" "b"] "failure"))))

(deftest test-pass-context
  (is (= "foo" (call-lambda (fn [] "foo") nil)))
  (is (= "foo*bar" (call-lambda ^{:stencil/pass-context true}
                                (fn [ctx] (str "foo*" (:addition ctx)))
                                {:addition "bar"})))
  (is (= "foo*" (call-lambda (fn [x] (str x "*")) "foo" nil)))
  (is (= "foo*bar"
         (call-lambda ^{:stencil/pass-context true}
                      (fn [x ctx] (str x "*" (:second-arg ctx)))
                      "foo" {:second-arg "bar"}))))