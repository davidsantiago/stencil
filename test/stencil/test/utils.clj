(ns stencil.test.utils
  (:use clojure.test
        stencil.utils))

(deftest test-contains-fuzzy?
  (is (= :a
         (contains-fuzzy? {:a 1} :a)))
  (is (= :a
         (contains-fuzzy? {:a 1} "a")))
  (is (= "a"
         (contains-fuzzy? {"a" 1} :a)))
  (is (= "a"
         (contains-fuzzy? {"a" 1} "a")))
  (is (= 1
         (contains-fuzzy? {:a 2} "b" 1))))

(deftest test-get-fuzzy
  (is (= "success"
         (get-fuzzy {"test" "success"} "test")))
  (is (= "success"
         (get-fuzzy {"test" "success"} :test)))
  (is (= "success"
         (get-fuzzy {:test "success"} :test)))
  (is (= "success"
         (get-fuzzy {:test "success"} "test")))
  (is (= "failure"
         (get-fuzzy {:test "success"} "TEST" "failure"))))

(deftest test-assoc-fuzzy
  (is (= {:a 1}
         (assoc-fuzzy {:a 0} :a 1)))
  (is (= {:a 1}
         (assoc-fuzzy {:a 0} "a" 1)))
  (is (= {"a" 1}
         (assoc-fuzzy {"a" 0} :a 1)))
  (is (= {"a" 1}
         (assoc-fuzzy {"a" 0} "a" 1)))
  (is (= {:a 1 :b 2}
         (assoc-fuzzy {:b 0} :a 1 "b" 2))))

(deftest test-dissoc-fuzzy
  (is (= {}
         (dissoc-fuzzy {"test" 1} "test")))
  (is (= {}
         (dissoc-fuzzy {"test" 1} :test)))
  (is (= {}
         (dissoc-fuzzy {"test1" 1 :test2 2} :test1 "test2"))))

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