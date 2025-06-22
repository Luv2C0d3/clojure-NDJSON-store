(ns repository.bubas-test
  (:require [clojure.test :refer :all]
            [repository.bubas :as bubas]
            [clojure.java.io :as io]))

(defn- cleanup-test-files! []
  (doseq [file ["test-ositos.nosql" "test-yoggies.ndjson"]]
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
      (let [bear-keys1 (-> @repo1 :indexes (get "bear") keys set)
            bear-keys1-read (-> @read-repo1 :indexes (get "bear") keys set)
            grizzly-keys1 (-> @repo1 :indexes (get "grizzly") keys set)
            grizzly-keys1-read (-> @read-repo1 :indexes (get "grizzly") keys set)]
        (is (= bear-keys1 bear-keys1-read) "Bear keys should match in repo1")
        (is (= 4 (count bear-keys1)) "Should have exactly 4 bears in repo1")
        (is (= grizzly-keys1 grizzly-keys1-read) "Grizzly keys should match in repo1")
        (is (= 4 (count grizzly-keys1)) "Should have exactly 4 grizzlies in repo1")))

    ;; Test repository 2
    (testing "Repository 2 synchronization"
      (let [bear-keys2 (-> @repo2 :indexes (get "bear") keys set)
            bear-keys2-read (-> @read-repo2 :indexes (get "bear") keys set)
            grizzly-keys2 (-> @repo2 :indexes (get "grizzly") keys set)
            grizzly-keys2-read (-> @read-repo2 :indexes (get "grizzly") keys set)]
        (is (= bear-keys2 bear-keys2-read) "Bear keys should match in repo2")
        (is (= 4 (count bear-keys2)) "Should have exactly 4 bears in repo2")
        (is (= grizzly-keys2 grizzly-keys2-read) "Grizzly keys should match in repo2")
        (is (= 4 (count grizzly-keys2)) "Should have exactly 4 grizzlies in repo2")))

    ;; Test cross-repository
    (testing "Cross-repository synchronization"
      (let [bear-keys1 (-> @repo1 :indexes (get "bear") keys set)
            bear-keys2-read (-> @read-repo2 :indexes (get "bear") keys set)
            grizzly-keys1 (-> @repo1 :indexes (get "grizzly") keys set)
            grizzly-keys2-read (-> @read-repo2 :indexes (get "grizzly") keys set)]
        (is (= bear-keys1 bear-keys2-read) "Bear keys should match across repositories")
        (is (= grizzly-keys1 grizzly-keys2-read) "Grizzly keys should match across repositories")))))