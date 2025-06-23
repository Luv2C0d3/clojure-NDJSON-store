(ns repository.client-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [repository.client :as client]
            [clojure.java.io :as io]))

(defn- cleanup-test-files! []
  (doseq [file ["test-clients1.nosql" "test-clients2.ndjson"]]
    (when (.exists (io/file file))
      (io/delete-file file))))

(use-fixtures :each
  (fn [test-fn]
    (cleanup-test-files!)
    (test-fn)
    (cleanup-test-files!)))

(defn- create-example-client!
  [repo-atom client-num]
  (let [client-id (str "client-" client-num)
        client-secret (str "secret-" client-num)
        redirect-uris [(str "https://example" client-num ".com/callback")]
        scopes ["read" "write"]
        client-entry {:client_id client-id
                     :client_secret client-secret
                     :redirect_uris redirect-uris
                     :scopes scopes}
        new-repo (client/add-client! @repo-atom client-entry)]
    (reset! repo-atom new-repo)
    client-entry))

(defn- assert-repositories-match
  "Helper function to compare two repositories' clients"
  [repo1 repo2 expected-client-count]
  (let [client-keys1 (-> repo1 :indexes (get :client_id) keys set)
        client-keys2 (-> repo2 :indexes (get :client_id) keys set)]
    (is (= client-keys1 client-keys2) "Client keys should match between repositories")
    (is (= expected-client-count (count client-keys1)) "Should have correct number of clients")))

(deftest test-repository-add-clients
  (let [repo (atom (client/create-client-repository "test-clients1.nosql"))]
    
    ;; Add two clients
    (create-example-client! repo 1)
    (create-example-client! repo 2)

    ;; Test counts in original repository
    (testing "Repository after adding clients"
      (let [client-keys (-> @repo :indexes (get :client_id) keys set)]
        (is (= 2 (count client-keys)) "Should have 2 clients")
        (is (= #{"client-1" "client-2"} client-keys) "Should have correct client IDs")))

    ;; Test client lookup
    (testing "Client lookup"
      (let [client1 (client/find-client-by-id @repo "client-1")
            client2 (client/find-client-by-id @repo "client-2")]
        (is (= "secret-1" (:client_secret client1)) "Client 1 should have correct secret")
        (is (= ["https://example1.com/callback"] (:redirect_uris client1)) "Client 1 should have correct redirect URI")
        (is (= "secret-2" (:client_secret client2)) "Client 2 should have correct secret")
        (is (= ["https://example2.com/callback"] (:redirect_uris client2)) "Client 2 should have correct redirect URI")))

    ;; Load repository from file and compare
    (testing "Repository reloaded from file"
      (let [loaded-repo (client/create-client-repository "test-clients1.nosql")]
        (assert-repositories-match @repo loaded-repo 2))))) 