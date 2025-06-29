(ns org.clojars.luv2c0d3.ndjson-repository.ndjson
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as string]))

(defprotocol NDJsonRepository
  (load-data! [this] "Load data from NDJSON file into memory")
  (find-by-key [this key-name key-value] "Find an entry by a specific key value")
  (add! [this entry] "Add a new entry to the repository")
  (delete! [this key-name key-value] "Delete an entry by key value")
  #_(update! [this key-name key-value entry] "Update an entry by key value"))

(defn- normalize-key
  "Convert string keys to keywords, leave keywords as is"
  [k]
  (if (string? k)
    (keyword k)
    k))

(defn- stringify-key
  "Convert keyword keys to strings, leave strings as is"
  [k]
  (if (keyword? k)
    (name k)
    k))

(defn- normalize-entry
  "Convert all string keys in an entry to keywords"
  [entry]
  (reduce-kv (fn [m k v]
               (assoc m (normalize-key k) v))
             {}
             entry))

(defn- stringify-entry
  "Convert all keyword keys in an entry to strings"
  [entry]
  (reduce-kv (fn [m k v]
               (assoc m (stringify-key k) v))
             {}
             entry))

(defn- load-ndjson-file [file-path]
  (with-open [rdr (io/reader file-path)]
    (->> (line-seq rdr)
         (filter #(not (string/blank? %)))  ; Skip empty lines
         (map json/parse-string)
         (filter some?)  ; Remove nil results from parsing
         (doall))))

(defn- index-data [data key-name]
  (reduce (fn [acc entry]
            (if-let [key-val (get entry key-name)]  ; Use keyword key directly since entry is already normalized
              (assoc acc key-val entry)
              acc))  ; Skip entries without this key
          {}
          data))

(defn- append-to-file! [file-path entry]
  (spit file-path (str (json/generate-string entry) "\n") :append true))

(defrecord NDJsonRepositoryImpl [file-path primary-keys]
  NDJsonRepository
  (load-data! [this]
    (try
      ;; Ensure file exists
      (let [file (io/file file-path)]
        (when-let [parent (.getParentFile file)]
          (.mkdirs parent))
        (when-not (.exists file)
          (log/info "Creating new NDJSON repository file:" file-path)
          (.createNewFile file)))
      
      (let [data (map normalize-entry (load-ndjson-file file-path))
            ;; Create indexes for each primary key for O(1) lookup
            ;; Normalize primary keys to keywords
            normalized-keys (map normalize-key primary-keys)
            indexes (reduce (fn [acc key-name]
                            (assoc acc key-name (index-data data key-name)))
                          {}
                          normalized-keys)]
        (log/debug "Loaded NDJSON file:" file-path)
        (log/debug "Primary keys:" normalized-keys)
        (log/debug "Data count:" (count data))
        (log/debug "Sample data:" (take 2 data))
        (doseq [key-name normalized-keys]
          (log/debug "Index for" key-name "has" (count (get indexes key-name)) "entries"))
        (assoc this
               :data data
               :indexes indexes
               :primary-keys normalized-keys))
      (catch Exception e
        (log/error "Failed to load NDJSON file:" file-path "Error:" (.getMessage e))
        ;; Initialize with empty collections when file doesn't exist
        (let [normalized-keys (map normalize-key primary-keys)]
          (assoc this
                 :data []
                 :indexes (reduce #(assoc %1 %2 {}) {} normalized-keys)
                 :primary-keys normalized-keys)))))

  (find-by-key [this key-name key-value]
    (let [key-name (normalize-key key-name)]
      (log/debug "Looking up key:" key-name "value:" key-value)
      (when-let [index (get (:indexes this) key-name)]
        (let [result (get index key-value)]
          (log/debug "Found entry:" result)
          result))))

  (add! [this entry]
    (try
      ;; Convert entry keys to strings before storing
      (let [string-entry (stringify-entry entry)]
        ;; Append to file first
        (append-to-file! file-path string-entry)
        ;; Update in-memory data with normalized entry
        (let [normalized-entry (normalize-entry string-entry)
              new-data (conj (:data this) normalized-entry)
              ;; Update indexes
              new-indexes (reduce (fn [acc key-name]
                                  (let [existing-index (get (:indexes this) key-name)
                                        key-val (get normalized-entry key-name)]
                                    (log/debug "Processing key:" key-name "value:" key-val "has-value:" (boolean key-val))
                                    (assoc acc key-name
                                           (if key-val
                                             ;; Only update index if this entry has this key
                                             (do
                                               (log/debug "Updating index for key:" key-name "with value:" key-val)
                                               (assoc existing-index key-val normalized-entry))
                                             ;; Keep existing index unchanged if entry doesn't have this key
                                             (do
                                               (log/debug "Keeping existing index for key:" key-name)
                                               existing-index)))))
                                (:indexes this)
                                (:primary-keys this))
              updated-keys (filter #(contains? normalized-entry %) (:primary-keys this))]
          (log/debug "Added entry with keys:" (keys normalized-entry))
          (log/debug "Primary keys:" (:primary-keys this))
          (log/debug "Updated indexes for keys:" updated-keys)
          (assoc this
                 :data new-data
                 :indexes new-indexes)))
      (catch Exception e
        (log/error "Failed to add entry:" entry "Error:" (.getMessage e))
        this)))

  (delete! [this key-name key-value]
    (let [key-name (normalize-key key-name)]
      (when-let [entry (find-by-key this key-name key-value)]
        (try
          ;; Update in-memory data
          (let [new-data (filterv #(not= % entry) (:data this))
                ;; Update indexes
                new-indexes (reduce (fn [acc k]
                                    (assoc acc k
                                           (if-let [key-val (get entry k)]
                                             (dissoc (get (:indexes this) k) key-val)
                                             (get (:indexes this) k))))
                                  (:indexes this)
                                  (:primary-keys this))]
            ;; Rewrite entire file with stringified entries
            (with-open [writer (io/writer file-path)]
              (doseq [item new-data]
                (.write writer (str (json/generate-string (stringify-entry item)) "\n"))))
            (assoc this
                   :data new-data
                   :indexes new-indexes))
          (catch Exception e
            (log/error "Failed to delete entry for key:" key-name "value:" key-value "Error:" (.getMessage e))
            this)))))

  #_(update! [this key-name key-value entry]
             (throw (UnsupportedOperationException. "Not implemented yet"))))

(defn create-repository [file-path primary-keys]
  (-> (NDJsonRepositoryImpl. file-path primary-keys)
      (load-data!)))