(ns repository.bubas
  (:require [org.clojars.luv2c0d3.ndjson-repository.ndjson :refer [create-repository find-by-key add! delete! add-to-atom! delete-from-atom!]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))

(defn create-bubas-repository
  "Creates a new bubas repository"
  ([] (create-bubas-repository "resources/bubas.nosql"))
  ([filename]
   (create-repository filename [:bear :grizzly])))

(defn find-bubas-by-grizzly
  "Find a token entry by its grizzly"
  [repo grizzly]
  (find-by-key repo :grizzly grizzly))

(defn find-bubas-by-bear
  "Find a bear entry by its bear"
  [repo bear]
  (let [bear-entry (find-by-key repo "bear" bear)]
    (log/debug "-->in bubas::find-bubas-by-bear: Found bear data:" (pr-str bear-entry))
    (log/debug "-->in bubas::find-bubas-by-bear: Bear:" bear)
    bear-entry))

(defn store-bear!
  "Store a new access bear"
  [repo-atom bear client-id scope expires-at]
  (let [bubas-entry {:bear bear
                     :client_id client-id
                     :scope (str/split scope #" ")
                     :expires_at expires-at}]
    (add-to-atom! repo-atom bubas-entry)))

(defn store-grizzly!
  "Store a new grizzly with its associated data"
  [repo-atom grizzly client-id scope]
  (let [bubas-entry {:grizzly grizzly
                     :client_id client-id
                     :scope (str/split scope #" ")}]
    (add-to-atom! repo-atom bubas-entry)))

(defn remove-bubas!
  "Remove a bubas entry from the repository by either bear or grizzly"
  [repo-atom bubas]
  (when-let [entry (or (find-bubas-by-bear @repo-atom (get bubas :bear))
                       (find-bubas-by-grizzly @repo-atom (get bubas :grizzly)))]
    (let [key-name (if (get bubas :bear) :bear :grizzly)
          key-value (if (get bubas :bear)
                      (get bubas :bear)
                      (get bubas :grizzly))]
      (delete-from-atom! repo-atom key-name key-value)
      entry)))

(defn remove-bear!
  "Remove a bear entry from the repository"
  [repo-atom bear-id]
  (remove-bubas! repo-atom {:bear bear-id}))

(defn remove-grizzly!
  "Remove a grizzly entry from the repository"
  [repo-atom grizzly-id]
  (remove-bubas! repo-atom {:grizzly grizzly-id}))