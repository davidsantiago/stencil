(ns stencil.cake.tasks
  (:use cake cake.core uncle.core
        [clojure.java.shell :only [sh]]
        [clojure.java.io :only [file]]
        [bake.core :only [log]])
  (:import [org.apache.tools.ant.taskdefs Delete]))

(def repo-url "https://github.com/mustache/spec.git")
(def spec-loc "test/spec")

(defn spec-present?
  "Check if the spec is available in the test/spec dir. Checks for the
   existence of the specs subdir."
  []
  (.exists (file spec-loc "specs")))


(defn clone-spec
  "Use git to clone the specs into the spec-loc dir."
  []
  (try (sh "git" "clone" repo-url spec-loc)
       (catch java.io.IOException e)))

(deftask pull-spec
  (when (not (spec-present?))
    (clone-spec)))

(defn delete-dir
  "Delete a directory from the source tree."
  [dir]
  (let [dir (file dir)]
    (when (seq (rest (file-seq dir))) 
      (ant Delete {:dir dir}))))

(deftask clean-spec
  (delete-dir spec-loc))

(deftask test #{pull-spec})
(deftask deps #{pull-spec})
(deftask clean #{clean-spec})