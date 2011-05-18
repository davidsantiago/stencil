(ns dali.core
  (:require [clojure.string :as string])
  (:use dali.parser))

(defn render
  [template input]
  (let [sb (StringBuilder.)]
    (doseq [item template]
      (if (string? item)
        (.append sb item)
        ;; Otherwise, need to interpret the structure.
        (let [[section-type section-name & rest] item]
          
          )
        )
      )
    )
  )