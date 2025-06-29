(ns org.clojars.luv2c0d3.ndjson-repository.java-api
  (:require [org.clojars.luv2c0d3.ndjson-repository.ndjson :as ndjson])
  (:import [java.util Map List])
  (:gen-class
    :name org.clojars.luv2c0d3.ndjson_repository.NDJsonRepository
    :state state
    :init init
    :constructors {[String "[Ljava.lang.String;"] []}
    :methods [[add [java.util.Map] Object]
             [delete [String String] void]
             [findByKey [String String] java.util.Map]]))

(defn- to-clojure [java-map]
  (into {} java-map))

(defn- to-java [clojure-map]
  (when clojure-map
    (java.util.HashMap. clojure-map)))

(defn -init [file-path primary-keys]
  [[] (atom (ndjson/create-repository file-path (mapv keyword primary-keys)))])

(defn -add [this entry]
  (let [clj-entry (to-clojure entry)
        repo-atom (.state this)
        [new-repo added-entry] (ndjson/add! @repo-atom clj-entry)]
    (reset! repo-atom new-repo)
    (to-java added-entry)))

(defn -delete [this key-name key-value]
  (let [repo-atom (.state this)]
    (ndjson/delete-from-atom! repo-atom (keyword key-name) key-value)
    nil))

(defn -findByKey [this key-name key-value]
  (let [repo-atom (.state this)]
    (-> (ndjson/find-by-key @repo-atom (keyword key-name) key-value)
        to-java))) 