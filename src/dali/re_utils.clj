(ns dali.re-utils
  "Some utility functions to make working with regular expressions easier."
  (:import java.util.regex.Pattern))

(defn re-concat
  "Concatenates its arguments into one regular expression
   (java.util.regex.Pattern). Args can be strings or java.util.regex.Pattern
   (what the #\"...\" reader macro creates). Or anything that responds to
   .toString, really."
  [& args]
  (re-pattern (apply str args)))

(defn re-quote
  "Turns its argument into a regular expression that recognizes its literal
   content as a string, quoting for any RE control characters as needed."
  [s]
  (re-pattern (Pattern/quote (str s))))