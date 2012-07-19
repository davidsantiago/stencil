(ns stencil.test.extensions
  (:use clojure.test
        stencil.core))

;; Test case to make sure we can run a lambda with the :stencil/pass-context
;; option in all the places a lambda can be used (escaped interpolation,
;; unescaped interpolation, and sections).

(deftest extension-pass-context-test
  ;; This calls an escaped interpolation lambda that returns some
  ;; mustache code based on the current context.
  (is (= "things"
         (render-string "{{lambda}}"
                        {:stuff "things"
                         :tag "stuff"
                         :lambda ^{:stencil/pass-context true}
                         (fn [ctx] (str "{{" (:tag ctx) "}}"))})))
  ;; This calls an unescaped interpolation lambda that returns some mustache
  ;; code based on the current context.
  (is (= "things"
         (render-string "{{{lambda}}}"
                        {:stuff "things"
                         :tag "stuff"
                         :lambda ^{:stencil/pass-context true}
                         (fn [ctx] (str "{{" (:tag ctx) "}}"))})))
  ;; This calls a section lambda that returns some mustache code based on the
  ;; current context.
  (is (= "peanut butter jelly time"
         (render-string "{{#lambda}}{{thing1}}{{/lambda}} time"
                        {:thing1 "peanut butter"
                         :thing2 "jelly"
                         :new-tag "thing2"
                         :lambda ^{:stencil/pass-context true}
                         (fn [src ctx] (str src " {{" (:new-tag ctx) "}}"))}))))
