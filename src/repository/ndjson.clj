(ns repository.ndjson
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(defprotocol NDJsonRepository
  (load-data! [this] "Load data from NDJSON file into memory")
  (find-by-key [this key-name key-value] "Find an entry by a specific key value")
  (add! [this entry] "Add a new entry to the repository")
  (delete! [this key-name key-value] "Delete an entry by key value")
  #_(update! [this key-name key-value entry] "Update an entry by key value"))

(defn- load-ndjson-file [file-path]
  (with-open [rdr (io/reader file-path)]
    (->> (line-seq rdr)
         (map json/parse-string)
         (doall))))

(defn- index-data [data key-name]
  (reduce (fn [acc entry]
            (assoc acc (get entry key-name) entry))
          {}
          data))

(defn- append-to-file! [file-path entry]
  (spit file-path (str (json/generate-string entry) "\n") :append true))

(defrecord NDJsonRepositoryImpl [file-path primary-keys]
  NDJsonRepository
  (load-data! [this]
    (try
      (let [data (load-ndjson-file file-path)
            ;; Create indexes for each primary key for O(1) lookup
            indexes (reduce (fn [acc key-name]
                              (assoc acc key-name (index-data data key-name)))
                            {}
                            primary-keys)]
        (log/info "Loaded NDJSON file:" file-path)
        (log/info "Primary keys:" primary-keys)
        (log/info "Data count:" (count data))
        (log/info "Sample data:" (take 2 data))
        (doseq [key-name primary-keys]
          (log/info "Index for" key-name "has" (count (get indexes key-name)) "entries"))
        (assoc this
               :data data
               :indexes indexes))
      (catch Exception e
        (log/error "Failed to load NDJSON file:" file-path "Error:" (.getMessage e))
        this)))

  (find-by-key [this key-name key-value]
    (log/info "-->in ndjson::find-by-key: Key name:" key-name "Key value:" key-value)
    (log/info "-->Available indexes:" (keys (:indexes this)))
    (log/info "-->Index for" key-name "contains keys:" (keys (get-in this [:indexes key-name])))
    (when-let [index (get-in this [:indexes key-name])]
      (let [result (get index key-value)]
        (log/info "-->in ndjson::find-by-key: Result:" (pr-str result))
        result)))

  (add! [this entry]
    (try
      ;; Append to file first
      (append-to-file! file-path entry)
      ;; Update in-memory data
      (let [new-data (conj (:data this) entry)
            ;; Update indexes - FIXED: handle keyword vs string mismatch
            new-indexes (reduce (fn [acc key-name]
                                  (let [existing-index (get (:indexes this) key-name)
                                        ;; Try both string and keyword versions of the key
                                        key-val (or (get entry key-name)
                                                    (get entry (keyword key-name))
                                                    (get entry (name key-name)))]
                                    (log/info "Processing key:" key-name "value:" key-val "has-value:" (boolean key-val))
                                    (assoc acc key-name
                                           (if key-val
                                             ;; Only update index if this entry has this key
                                             (do
                                               (log/info "Updating index for key:" key-name "with value:" key-val)
                                               (assoc existing-index key-val entry))
                                             ;; Keep existing index unchanged if entry doesn't have this key
                                             (do
                                               (log/info "Keeping existing index for key:" key-name)
                                               existing-index)))))
                                (:indexes this)  ; Start with existing indexes
                                primary-keys)
            updated-keys (filter #(or (contains? entry %)
                                      (contains? entry (keyword %))
                                      (contains? entry (name %))) primary-keys)]
        (log/info "Added entry with keys:" (keys entry))
        (log/info "Primary keys:" primary-keys)
        (log/info "Updated indexes for keys:" updated-keys)
        (assoc this
               :data new-data
               :indexes new-indexes))
      (catch Exception e
        (log/error "Failed to add entry:" entry "Error:" (.getMessage e))
        this)))

  (delete! [this key-name key-value]
    (when-let [entry (find-by-key this key-name key-value)]
      (try
        ;; Update in-memory data
        (let [new-data (filterv #(not= % entry) (:data this))
              ;; Update indexes - FIXED: start with existing indexes, not empty map
              new-indexes (reduce (fn [acc k]
                                    (assoc acc k
                                           (if-let [key-val (get entry k)]
                                             (dissoc (get (:indexes this) k) key-val)
                                             (get (:indexes this) k))))
                                  (:indexes this)  ; <-- FIXED: start with existing indexes
                                  primary-keys)]
          ;; Rewrite entire file
          (with-open [writer (io/writer file-path)]
            (doseq [item new-data]
              (.write writer (str (json/generate-string item) "\n"))))
          (assoc this
                 :data new-data
                 :indexes new-indexes))
        (catch Exception e
          (log/error "Failed to delete entry for key:" key-name "value:" key-value "Error:" (.getMessage e))
          this))))

  #_(update! [this key-name key-value entry]
             (throw (UnsupportedOperationException. "Not implemented yet"))))

(defn create-repository [file-path primary-keys]
  (-> (NDJsonRepositoryImpl. file-path primary-keys)
      (load-data!)))