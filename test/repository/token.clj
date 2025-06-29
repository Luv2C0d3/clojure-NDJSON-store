(ns repository.token
  (:require [repository.ndjson :refer [create-repository find-by-key add! delete!]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn create-token-repository
  "Creates a new token repository"
  [filename]
  (create-repository filename [:access_token :refresh_token]))

(defn find-token-by-access-token
  "Find a token entry by its access_token"
  [repo access-token]
  (find-by-key repo :access_token access-token))

(defn find-token-by-refresh-token
  "Find a token entry by its refresh_token"
  [repo refresh-token]
  (find-by-key repo :refresh_token refresh-token))

(defn store-access-token!
  "Store a new access token with its associated data"
  [repo access-token client-id scope expires-at]
  (let [token-entry {:access_token access-token
                     :client_id client-id
                     :scope (str/split scope #" ")
                     :expires_at expires-at}
        new-repo (add! repo token-entry)]
    [new-repo token-entry]))

(defn store-refresh-token!
  "Store a new refresh token with its associated data"
  [repo refresh-token client-id scope]
  (let [token-entry {:refresh_token refresh-token
                     :client_id client-id
                     :scope (str/split scope #" ")}
        new-repo (add! repo token-entry)]
    [new-repo token-entry]))

(defn- remove-token-by-type!
  "Internal helper to remove a token by its type"
  [repo token-type token-id]
  (if-let [entry (find-by-key repo token-type token-id)]
    (let [new-repo (delete! repo token-type token-id)]
      [new-repo entry])
    [repo nil]))

(defn remove-token!
  "Remove a token entry from the repository by either access_token or refresh_token"
  [repo token-id]
  (let [[new-repo entry] (or (remove-token-by-type! repo :access_token token-id)
                            (remove-token-by-type! repo :refresh_token token-id))]
    (or new-repo repo)))

(defn remove-access-token!
  "Remove an access token entry from the repository"
  [repo token-id]
  (let [[new-repo _] (remove-token-by-type! repo :access_token token-id)]
    (or new-repo repo)))

(defn remove-refresh-token!
  "Remove a refresh token entry from the repository"
  [repo token-id]
  (let [[new-repo _] (remove-token-by-type! repo :refresh_token token-id)]
    (or new-repo repo))) 