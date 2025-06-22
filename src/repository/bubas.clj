(ns repository.bubas
  (:require [repository.ndjson :refer [create-repository find-by-key add! delete!]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

;; Define the token repository with two primary keys: access_token and refresh_token
(def bubas-repository
  (create-repository
   "resources/bubas.nosql" 
   ["bear" "grizzly"])) ;; Two primary keys for O(1) lookups

(defn find-bubas-by-grizzly
  "Find a token entry by its access_token"
  [grizzly]
  (find-by-key bubas-repository "grizzly" grizzly))

(defn find-bubas-by-bear
  "Find a bear entry by its bear"
  [bear]
  (let [bear-entry (find-by-key bubas-repository "bear" bear)]
    (log/info "-->in bubas::find-bubas-by-bear: Found bear data:" (pr-str bear-entry))
    (log/info "-->in bubas::find-bubas-by-bear: Bear:" bear)
    bear-entry))

(defn store-bear!
  "Store a new access bear"
  [bear client-id scope expires-at]
  (let [bubas-entry {"bear" bear      ; FIXED: Use string keys for consistency
                     "client_id" client-id
                     "scope" (str/split scope #" ")
                     "expires_at" expires-at}]
    (alter-var-root #'bubas-repository #(add! % bubas-entry))  ; FIXED: Proper var mutation
    bubas-entry))

(defn store-grizzly!
  "Store a new grizzly with its associated data"
  [grizzly client-id scope]
  (let [bubas-entry {"grizzly" grizzly    ; FIXED: Use string keys for consistency
                     "client_id" client-id
                     "scope" (str/split scope #" ")}]
    (alter-var-root #'bubas-repository #(add! % bubas-entry))  ; FIXED: Proper var mutation
    bubas-entry))

(defn remove-bubas!
  "Remove a bubas entry from the repository by either bear or grizzly"
  [bubas]
  (when-let [entry (or (find-bubas-by-bear (get bubas "bear"))  ; FIXED: Use string keys
                       (find-bubas-by-grizzly (get bubas "grizzly")))]
    (let [key-name (if (get bubas "bear") "bear" "grizzly")
          key-value (if (get bubas "bear")
                      (get bubas "bear")
                      (get bubas "grizzly"))]
      (alter-var-root #'bubas-repository #(delete! % key-name key-value))  ; FIXED: Proper var mutation
      entry)))