(ns stencil.test.utils
  (:use clojure.test
        stencil.utils))

(deftest test-html-escape
  (is (= "&lt;script&gt;"
         (html-escape "<script>")))
  (is (= "&amp;lt;script&amp;gt;"
         (html-escape (html-escape "<script>"))))
  (is (= "&lt;script src=&quot;blah.js&quot;&gt;"
         (html-escape "<script src=\"blah.js\">"))))

(deftest test-indent-string
  (is (= " blah\n blah")
      (indent-string "blah\nblah" " "))
  (is (= " blah\r\n blah"
         (indent-string "blah\r\nblah" " ")))
  (is (= " blah"
         (indent-string "blah" " ")))
  ;; Shouldn't indent a non-existing last line when string ends on \n.
  (is (= " blah\n"
         (indent-string "blah\n" " "))))

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
