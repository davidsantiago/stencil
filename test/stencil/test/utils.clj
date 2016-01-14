(ns stencil.test.utils
  (:use clojure.test
        stencil.utils
        stencil.core))

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
  (is (= "foo" (call-lambda (fn [] "foo") nil render-string)))
  (is (= "foo*bar" (call-lambda ^{:stencil/pass-context true}
                                (fn [ctx] (str "foo*" (:addition ctx)))
                                '({:addition "bar"})
                                render-string)))
  (is (= "foo*" (call-lambda (fn [x] (str x "*"))
                             nil
                             render-string
                             "foo")))
  (is (= "foo*bar"
         (call-lambda ^{:stencil/pass-context true}
                      (fn [x ctx] (str x "*" (:second-arg ctx)))
                      '({:second-arg "bar"})
                      render-string
                      "foo"))))

(deftest test-pass-render
  (is (= "{{foo}}*bar" (call-lambda ^{:stencil/pass-render true}
                                    (fn [ctx _] (str "{{foo}}*" (:addition ctx)))
                                    '({:addition "bar"})
                                    render-string)))
  (is (= "{{baz}}" (call-lambda ^{:stencil/pass-render true}
                                (fn [content _ _]
                                  content)
                                nil
                                render-string
                                "{{baz}}")))
  (is (= "baz*" (call-lambda ^{:stencil/pass-render true}
                             (fn [content ctx render]
                               (render content ctx))
                             '({:baz "baz*"})
                             render-string
                             "{{baz}}")))
  (is (= "bar" (call-lambda ^{:stencil/pass-render true}
                            (fn [ctx render] (render "{{addition}}" ctx))
                            '({:addition "bar"})
                            render-string))))

(deftest test-pass-context-stack
  (is (= "ITEM: FOO" (call-lambda ^{:stencil/pass-context-stack true}
                         (fn [text context render]
                           (clojure.string/upper-case
                             (render text context)))
                         '({:text "foo"}
                            {:history [{:text "foo"} {:text "bar"}]
                             :prefix "item:"})
                         render-string
                         "{{prefix}} {{text}}"))))
