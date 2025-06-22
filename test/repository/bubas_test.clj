(ns repository.bubas-test
  (:require [clojure.test :refer :all]
            [repository.bubas :as bubas]
            [clojure.java.io :as io]))

(defn- cleanup-test-files! []
  (doseq [file ["test-ositos.nosql" "test-yoggies.ndjson" "winnie-the-pooh.ndjson"]]
    (when (.exists (io/file file))
      (io/delete-file file))))

(use-fixtures :each
  (fn [test-fn]
    (cleanup-test-files!)
    (test-fn)
    (cleanup-test-files!)))

(defn- create-example-bear!
  [repo-atom bear-num]
  (let [bear-id (str "bear-" bear-num)
        client-id "client-456"
        scope "read write"
        expires-at (+ (System/currentTimeMillis) (* 3600 1000))
        [new-repo entry] (bubas/store-bear! @repo-atom bear-id client-id scope expires-at)]
    (reset! repo-atom new-repo)
    entry))

(defn- create-example-grizzly!
  [repo-atom grizzly-num]
  (let [grizzly-id (str "grizzly-" grizzly-num)
        client-id "client-456"
        scope "read"
        [new-repo entry] (bubas/store-grizzly! @repo-atom grizzly-id client-id scope)]
    (reset! repo-atom new-repo)
    entry))

(defn- assert-repositories-match
  "Helper function to compare two repositories' bears and grizzlies"
  [repo1 repo2 expected-bear-count expected-grizzly-count]
  (let [bear-keys1 (-> repo1 :indexes (get :bear) keys set)
        bear-keys2 (-> repo2 :indexes (get :bear) keys set)
        grizzly-keys1 (-> repo1 :indexes (get :grizzly) keys set)
        grizzly-keys2 (-> repo2 :indexes (get :grizzly) keys set)]
    (is (= bear-keys1 bear-keys2) "Bear keys should match between repositories")
    (is (= expected-bear-count (count bear-keys1)) "Should have correct number of bears")
    (is (= grizzly-keys1 grizzly-keys2) "Grizzly keys should match between repositories")
    (is (= expected-grizzly-count (count grizzly-keys1)) "Should have correct number of grizzlies")))

(deftest test-repository-add-and-remove
  (let [repo (atom (bubas/create-bubas-repository "winnie-the-pooh.ndjson"))]

    ;; Add initial entries
    (doseq [i (range 1 5)]
      (create-example-bear! repo i))
    (doseq [i (range 1 4)]
      (create-example-grizzly! repo i))

    ;; Remove some entries
    (let [new-repo (bubas/remove-bear! @repo "bear-1")]
      (reset! repo new-repo))
    (let [new-repo (bubas/remove-bear! @repo "bear-2")]
      (reset! repo new-repo))
    (let [new-repo (bubas/remove-grizzly! @repo "grizzly-1")]
      (reset! repo new-repo))

    ;; Test counts in original repository
    (testing "Repository after removals"
      (let [bear-keys (-> @repo :indexes (get :bear) keys set)
            grizzly-keys (-> @repo :indexes (get :grizzly) keys set)]
        (is (= 2 (count bear-keys)) "Should have 2 bears remaining")
        (is (= 2 (count grizzly-keys)) "Should have 2 grizzlies remaining")
        (is (= #{"bear-3" "bear-4"} bear-keys) "Should have correct bears")
        (is (= #{"grizzly-2" "grizzly-3"} grizzly-keys) "Should have correct grizzlies")))

    ;; Load repository from file and compare
    (testing "Repository reloaded from file"
      (let [loaded-repo (bubas/create-bubas-repository "winnie-the-pooh.ndjson")]
        (assert-repositories-match @repo loaded-repo 2 2)))

    ;; Add more entries
    (create-example-bear! repo 5)  ;; Add one more bear
    (doseq [i (range 4 7)]         ;; Add three more grizzlies
      (create-example-grizzly! repo i))

    ;; Test counts after adding more entries
    (testing "Repository after adding more entries"
      (let [bear-keys (-> @repo :indexes (get :bear) keys set)
            grizzly-keys (-> @repo :indexes (get :grizzly) keys set)]
        (is (= 3 (count bear-keys)) "Should have 3 bears total")
        (is (= 5 (count grizzly-keys)) "Should have 5 grizzlies total")
        (is (= #{"bear-3" "bear-4" "bear-5"} bear-keys) "Should have correct bears")
        (is (= #{"grizzly-2" "grizzly-3" "grizzly-4" "grizzly-5" "grizzly-6"} grizzly-keys) "Should have correct grizzlies")))

    ;; Load repository from file again and compare
    (testing "Repository reloaded from file after adding more entries"
      (let [loaded-repo (bubas/create-bubas-repository "winnie-the-pooh.ndjson")]
        (assert-repositories-match @repo loaded-repo 3 5)))))


(deftest test-repository-synchronization
  (let [repo1 (atom (bubas/create-bubas-repository "test-ositos.nosql"))
        repo2 (atom (bubas/create-bubas-repository "test-yoggies.ndjson"))
        read-repo1 (atom nil)
        read-repo2 (atom nil)]
    
    ;; Create test data
    (doseq [i (range 1 5)]
      (create-example-bear! repo1 i)
      (create-example-grizzly! repo1 i)
      (create-example-bear! repo2 i)
      (create-example-grizzly! repo2 i))

    ;; Initialize read repositories
    (reset! read-repo1 (bubas/create-bubas-repository "test-ositos.nosql"))
    (reset! read-repo2 (bubas/create-bubas-repository "test-yoggies.ndjson"))

    ;; Test repository 1
    (testing "Repository 1 synchronization"
      (let [bear-keys1 (-> @repo1 :indexes (get :bear) keys set)
            bear-keys1-read (-> @read-repo1 :indexes (get :bear) keys set)
            grizzly-keys1 (-> @repo1 :indexes (get :grizzly) keys set)
            grizzly-keys1-read (-> @read-repo1 :indexes (get :grizzly) keys set)]
        (is (= bear-keys1 bear-keys1-read) "Bear keys should match in repo1")
        (is (= 4 (count bear-keys1)) "Should have exactly 4 bears in repo1")
        (is (= grizzly-keys1 grizzly-keys1-read) "Grizzly keys should match in repo1")
        (is (= 4 (count grizzly-keys1)) "Should have exactly 4 grizzlies in repo1")))

    ;; Test repository 2
    (testing "Repository 2 synchronization"
      (let [bear-keys2 (-> @repo2 :indexes (get :bear) keys set)
            bear-keys2-read (-> @read-repo2 :indexes (get :bear) keys set)
            grizzly-keys2 (-> @repo2 :indexes (get :grizzly) keys set)
            grizzly-keys2-read (-> @read-repo2 :indexes (get :grizzly) keys set)]
        (is (= bear-keys2 bear-keys2-read) "Bear keys should match in repo2")
        (is (= 4 (count bear-keys2)) "Should have exactly 4 bears in repo2")
        (is (= grizzly-keys2 grizzly-keys2-read) "Grizzly keys should match in repo2")
        (is (= 4 (count grizzly-keys2)) "Should have exactly 4 grizzlies in repo2")))

    ;; Test cross-repository
    (testing "Cross-repository synchronization"
      (let [bear-keys1 (-> @repo1 :indexes (get :bear) keys set)
            bear-keys2-read (-> @read-repo2 :indexes (get :bear) keys set)
            grizzly-keys1 (-> @repo1 :indexes (get :grizzly) keys set)
            grizzly-keys2-read (-> @read-repo2 :indexes (get :grizzly) keys set)]
        (is (= bear-keys1 bear-keys2-read) "Bear keys should match across repositories")
        (is (= grizzly-keys1 grizzly-keys2-read) "Grizzly keys should match across repositories")))))