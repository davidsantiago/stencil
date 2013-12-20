(ns stencil.test.no-cache
  (:use clojure.test)
  (:require [stencil.loader :as sldr]
            [stencil.utils :as utils])
  (:import 
           [stencil.loader CoreCacheUnavailableStub_SeeReadme]))

;; This namespace only runs a test when core.cache is unavailable.
;; It merely tests that the stencil.loader functions will barf with
;; a message to the user when the user has not set a usable cache
;; alternative using set-cache.



(defn core-cache-unavailable-stub-fixture
  [f]
  (sldr/set-cache (CoreCacheUnavailableStub_SeeReadme.))
  (f)
  (sldr/set-cache {}))

(use-fixtures :once core-cache-unavailable-stub-fixture)

(when (not (utils/core-cache-present?))
  (deftest barfs-properly-test
    (is (thrown-with-msg? Exception #"Could not load core.cache."
                          (sldr/load "nonexistentfile.mustache")))))

