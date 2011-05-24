(ns stencil.test.scanner
  (:use clojure.test
        [stencil.scanner :rename {peek peep}]))

;;
;; Information access tests.
;;

(deftest test-position
  (is (= 0 (position (scanner ""))))
  (is (= 0 (position (scanner "test"))))
  (is (= 1 (position (stencil.scanner.Scanner. "test" 1 nil)))))

(deftest test-beginning-of-line?
  (is (= true (beginning-of-line? (scanner ""))))
  (is (= true (beginning-of-line? (scanner "test"))))
  (is (= false (beginning-of-line?
                (stencil.scanner.Scanner. "test\r\ntest" 5 nil))))
  (is (= true (beginning-of-line?
               (stencil.scanner.Scanner. "test\r\ntest" 6 nil)))))

(deftest test-end?
  (is (= true (end? (scanner ""))))
  (is (= false (end? (scanner "test")))))

(deftest test-remainder
  (is (= "test" (remainder (scanner "test"))))
  (is (= "" (remainder (scanner ""))))
  (is (= "" (remainder (stencil.scanner.Scanner. "test" 4 nil))))
  (is (= "" (remainder (stencil.scanner.Scanner. "test" 5 nil)))))

(deftest test-groups
  (is (= ["m"]
           (groups (scanner "test" 0 (match-info 0 1 ["m"]))))))

(deftest test-matched
  (is (= "m"
         (matched (scanner "test" 0 (match-info 0 1 ["m"]))))))

(deftest test-pre-match
  (is (= "beginn"
         (pre-match (scanner "beginning" 9 (match-info 6 8 ["in"])))))
  (is (= "test"
         (pre-match (scan (scan (scanner "test string") #"test") #"\s+")))))

(deftest test-post-match
  (is (= "ning"
         (post-match (scanner "beginning" 5 (match-info 3 5 ["in"])))))
  (is (= "string"
         (post-match (scan (scan (scanner "test string") #"test") #"\s+")))))

;;
;; Scanning/Advancing tests.
;;

(deftest test-scan
  (is (= "t"
         (matched (scan (scanner "test") #"t"))))
  (is (= 1 (position (scan (scanner "test") #"t"))))
  (is (= "test"
         (matched (scan (scanner "test") #"test"))))
  (is (end? (scan (scanner "test") #"test")))
  (is (= ["t"]
           (groups (scan (scanner "test string") #"t"))))
  ;; Compounded scans should work.
  (is (= 5
         (position (scan (scan (scanner "test string") #"test") #"\s+"))))
  (is (= 4
         (:start (:match (scan (scan (scanner "test string")
                                     #"test")
                               #"\s+")))))
  (is (= 5
         (:end (:match (scan (scan (scanner "test string")
                                   #"test")
                             #"\s+")))))
  ;; Failing to match shoud leave us in the same position
  (is (= 0 (position (scan (scanner "testgoal") #"notinthestring"))))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (:match (scan (scan (scanner "test string") #"test")
                           #"notinthestring")))))

(deftest test-scan-until
  (is (= "goal"
         (matched (scan-until (scanner "testgoal")
                              #"goal"))))
  (is (= 8 (position (scan-until (scanner "testgoal") #"goal"))))
  (is (= "goal"
         (matched (scan-until (scanner "goal") #"goal"))))
  (is (end? (scan-until (scanner "goal") #"goal")))
  (is (end? (scan-until (scanner "testgoal") #"goal")))
  (is (= ["s"]
           (groups (scan-until (scanner "test string") #"s"))))
  ;; Compounded scan-untils should work.
  (is (= 8
         (position (scan-until (scan-until (scanner "test string")
                                           #"s")
                               #"r"))))
  (is (= 7
         (:start (:match (scan-until (scan-until (scanner "test string")
                                                 #"s")
                                     #"r")))))
  (is (= 8
         (:end (:match (scan-until (scan-until (scanner "test-string")
                                               #"s")
                                   #"r")))))
  ;; Failing to match should leave us in the same position.
  (is (= 0 (position (scan-until (scanner "testgoal") #"notinthestring"))))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (:match (scan-until (scan (scanner "test string") #"test")
                                 #"notinthestring")))))

(deftest test-skip-to-match-start
  (is (= "goal"
         (matched (skip-to-match-start (scanner "testgoal")
                                       #"goal"))))
  (is (= 4 (position (skip-to-match-start (scanner "testgoal") #"goal"))))
  (is (= "goal"
         (matched (skip-to-match-start (scanner "goal") #"goal"))))
  ;; Calling scan on result of skip-to-match-start should work.
  (is (= "goal"
         (matched (scan (skip-to-match-start (scanner "testgoal")
                                             #"goal")
                        #"goal"))))
  (is (end? (scan (skip-to-match-start (scanner "testgoal")
                                       #"goal")
                  #"goal")))
  ;; Failing to match should leave us in the same position.
  (is (= 0 (position (skip-to-match-start (scanner "testgoal") #"yes"))))
  ;; Failing to match should remove pre-existing match data.
  (is (= nil (:match (skip-to-match-start (scan (scanner "test string") #"test")
                                          #"notinthestring")))))

;;
;; Look-ahead tests.
;;

(deftest test-check
  (is (= "t"
         (check (scanner "test") #"t")))
  (is (= "test"
         (check (scanner "test") #"test"))))

(deftest test-check-until
  (is (= "goal"
         (check-until (scanner "testgoal") #"goal")))
  (is (= "goal"
         (check-until (scanner "goal") #"goal"))))

(deftest test-check-until-inclusive
  (is (= "testgoal"
         (check-until-inclusive (scanner "testgoal") #"goal")))
  (is (= "goal"
         (check-until-inclusive (scanner "goal") #"goal"))))

(deftest test-peek ;; Renamed to peep here.
  (is (= "t"
         (peep (scanner "test") 1)))
  (is (= "test"
         (peep (scanner "test") 4)))
  (is (= "test"
         (peep (scanner "test") 500))))
