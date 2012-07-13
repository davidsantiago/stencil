(ns stencil.test.parser
  (:refer-clojure :exclude [partial])
  (:require [clojure.zip :as zip])
  (:use clojure.test
        [stencil ast parser utils]
        [stencil.scanner :rename {peek peep}]))

(deftest test-get-line-col-from-index
  (is (= [1 1] (get-line-col-from-index "a\nb\nc" 0)))
  (is (= [1 2] (get-line-col-from-index "a\nb\nc" 1)))
  (is (= [2 1] (get-line-col-from-index "a\nb\nc" 2)))
  ;; Same, but with the other line endings.
  (is (= [1 1] (get-line-col-from-index "a\r\nb\r\nc" 0)))
  (is (= [1 2] (get-line-col-from-index "a\r\nb\r\nc" 1)))
  (is (= [1 3] (get-line-col-from-index "a\r\nb\r\nc" 2)))
  (is (= [2 1] (get-line-col-from-index "a\r\nb\r\nc" 3))))

(deftest test-format-location
  (is (= "line 1, column 1"
         (format-location (scanner "a\r\nb\r\nc"))))
  (is (= "line 1, column 1"
         (format-location "a\r\nb\r\nc" 0))))

(deftest test-tag-position?
  (is (= true
         (tag-position? (scanner "    {{test}}") parser-defaults)))
  (is (= true
         (tag-position? (scanner "{{test}}") parser-defaults)))
  (is (= true
         (tag-position? (scanner "\t{{test}}") parser-defaults)))
  (is (= false
         (tag-position? (scanner "\r\n{{test}}") parser-defaults)))
  (is (= false
         (tag-position? (scanner "Hi. {{test}}") parser-defaults))))

(deftest test-parse-tag-name
  (is (= [:test]
           (parse-tag-name "test")))
  (is (= [:test :test2]
           (parse-tag-name "test.test2"))))

(deftest test-parse-text
  (is (= ["test string"]
           (zip/root (:output (parse-text (parser (scanner "test string")))))))
  (is (= ["test string"]
           (zip/root (:output (parse-text
                               (parser (scanner "test string{{tag}}")))))))
  (is (= ["test string\n"]
           (zip/root (:output (parse-text
                               (parser (scanner "test string\n{{tag}}")))))))
  (is (= ["test string\n"]
           (zip/root (:output (parse-text
                               (parser (scanner "test string\n  {{tag}}")))))))
  (is (= ["\ntest string"]
           (zip/root (:output (parse-text
                               (parser (scanner "\ntest string{{tag}}")))))))
  (is (= ["\ntest string\n"]
           (zip/root (:output (parse-text
                               (parser (scanner "\ntest string\n{{tag}}"))))))))

(deftest test-parse-tag
  (is (= ["   " (escaped-variable (parse-tag-name "blah"))]
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{blah}}")))))))
  (is (= ["   " (unescaped-variable (parse-tag-name "blah"))]
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{{blah}}}")))))))
  (is (= ["   " (unescaped-variable (parse-tag-name "blah"))]
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{{ blah}}}")))))))
  (is (= ["   " (unescaped-variable (parse-tag-name "blah"))]
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{{ blah }}}")))))))
  (is (= ["   " (unescaped-variable (parse-tag-name "blah"))]
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{&blah}}")))))))
  (is (= ["   " (unescaped-variable (parse-tag-name "blah"))]
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{& blah}}")))))))
  (is (= ["   " (unescaped-variable (parse-tag-name "blah"))]
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{& blah   }}")))))))
  ;; Test whitespace removal on a standalone tag.
  (is (= []
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{!blah}}\n")))))))
  (is (= []
           (zip/root (:output (parse-tag
                               (parser (scanner "   {{!blah}}\r\n"))))))))
