(ns org.clojars.luv2c0d3.ndjson-repository.tokens-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [repository.token :as token]
            [clojure.java.io :as io]))

(defn- cleanup-test-files! []
  (doseq [file ["tokens1.nosql" "tokens2.ndjson" "tokens3.ndjson"]]
    (when (.exists (io/file file))
      (io/delete-file file))))

(use-fixtures :each
  (fn [test-fn]
    (cleanup-test-files!)
    (test-fn)
    (cleanup-test-files!)))

(defn- create-example-token!
  [repo-atom token-num]
  (let [token-id (str "token-" token-num)
        client-id "client-456"
        scope "read write"
        expires-at (+ (System/currentTimeMillis) (* 3600 1000))
        [new-repo entry] (token/store-access-token! @repo-atom token-id client-id scope expires-at)]
    (reset! repo-atom new-repo)
    entry))

(defn- create-example-refresh-token!
  [repo-atom refresh-token-num]
  (let [refresh-token-id (str "refresh-token-" refresh-token-num)
        client-id "client-456"
        scope "read"
        [new-repo entry] (token/store-refresh-token! @repo-atom refresh-token-id client-id scope)]
    (reset! repo-atom new-repo)
    entry))

(defn- assert-repositories-match
  "Helper function to compare two repositories' tokens and refresh tokens"
  [repo1 repo2 expected-token-count expected-refresh-token-count]
  (let [token-keys1 (-> repo1 :indexes (get :access_token) keys set)
        token-keys2 (-> repo2 :indexes (get :access_token) keys set)
        refresh-token-keys1 (-> repo1 :indexes (get :refresh_token) keys set)
        refresh-token-keys2 (-> repo2 :indexes (get :refresh_token) keys set)]
    (is (= token-keys1 token-keys2) "Token keys should match between repositories")
    (is (= expected-token-count (count token-keys1)) "Should have correct number of tokens")
    (is (= refresh-token-keys1 refresh-token-keys2) "Refresh token keys should match between repositories")
    (is (= expected-refresh-token-count (count refresh-token-keys1)) "Should have correct number of refresh tokens")))

(deftest test-repository-add-and-remove
  (let [repo (atom (token/create-token-repository "tokens1.nosql"))]

    ;; Add initial entries
    (doseq [i (range 1 5)]
      (create-example-token! repo i))
    (doseq [i (range 1 4)]
      (create-example-refresh-token! repo i))

    ;; Remove some entries
    (let [new-repo (token/remove-token! @repo "token-1")]
      (reset! repo new-repo))
    (let [new-repo (token/remove-token! @repo "token-2")]
      (reset! repo new-repo))
    (let [new-repo (token/remove-refresh-token! @repo "refresh-token-1")]
      (reset! repo new-repo))

    ;; Test counts in original repository
    (testing "Repository after removals"
      (let [token-keys (-> @repo :indexes (get :access_token) keys set)
            refresh-token-keys (-> @repo :indexes (get :refresh_token) keys set)]
        (is (= 2 (count token-keys)) "Should have 2 tokens remaining")
        (is (= 2 (count refresh-token-keys)) "Should have 2 refresh tokens remaining")
        (is (= #{"token-3" "token-4"} token-keys) "Should have correct tokens")
        (is (= #{"refresh-token-2" "refresh-token-3"} refresh-token-keys) "Should have correct refresh tokens")))

    ;; Load repository from file and compare
    (testing "Repository reloaded from file"
      (let [loaded-repo (token/create-token-repository "tokens1.nosql")]
        (assert-repositories-match @repo loaded-repo 2 2)))

    ;; Add more entries
    (create-example-token! repo 5)  ;; Add one more token
    (doseq [i (range 4 7)]         ;; Add three more refresh tokens
      (create-example-refresh-token! repo i))

    ;; Test counts after adding more entries
    (testing "Repository after adding more entries"
      (let [token-keys (-> @repo :indexes (get :access_token) keys set)
            refresh-token-keys (-> @repo :indexes (get :refresh_token) keys set)]
        (is (= 3 (count token-keys)) "Should have 3 tokens total")
        (is (= 5 (count refresh-token-keys)) "Should have 5 refresh tokens total")
        (is (= #{"token-3" "token-4" "token-5"} token-keys) "Should have correct tokens")
        (is (= #{"refresh-token-2" "refresh-token-3" "refresh-token-4" "refresh-token-5" "refresh-token-6"} refresh-token-keys) "Should have correct refresh tokens")))

    ;; Load repository from file again and compare
    (testing "Repository reloaded from file after adding more entries"
      (let [loaded-repo (token/create-token-repository "tokens1.nosql")]
        (assert-repositories-match @repo loaded-repo 3 5)))))


(deftest test-repository-synchronization
  (let [repo1 (atom (token/create-token-repository "tokens1.nosql"))
        repo2 (atom (token/create-token-repository "tokens2.ndjson"))
        read-repo1 (atom nil)
        read-repo2 (atom nil)]
    
    ;; Create test data
    (doseq [i (range 1 5)]
      (create-example-token! repo1 i)
      (create-example-refresh-token! repo1 i)
      (create-example-token! repo2 i)
      (create-example-refresh-token! repo2 i))

    ;; Initialize read repositories
    (reset! read-repo1 (token/create-token-repository "tokens1.nosql"))
    (reset! read-repo2 (token/create-token-repository "tokens2.ndjson"))

    ;; Test individual repository synchronization
    (testing "Individual repository synchronization"
      (assert-repositories-match @repo1 @read-repo1 4 4)
      (assert-repositories-match @repo2 @read-repo2 4 4))

    ;; Test cross-repository synchronization
    (testing "Cross-repository synchronization"
      (assert-repositories-match @repo1 @read-repo2 4 4))))