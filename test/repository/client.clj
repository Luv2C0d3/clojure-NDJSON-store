(ns repository.client
  (:require [repository.ndjson :refer [create-repository find-by-key add! delete!]]
            [clojure.tools.logging :as log]))

(defn create-client-repository
  "Creates a new client repository"
  [filename]
  (create-repository filename [:client_id]))

(defn find-client-by-id
  "Find a client by its client_id"
  [repo client-id]
  (find-by-key repo :client_id client-id))

(defn add-client!
  "Add a new client to the repository"
  [repo client]
  (add! repo client))

(defn remove-client!
  "Remove a client from the repository"
  [repo client-id]
  (if-let [entry (find-client-by-id repo client-id)]
    (let [new-repo (delete! repo :client_id client-id)]
      [new-repo entry])
    [repo nil])) 