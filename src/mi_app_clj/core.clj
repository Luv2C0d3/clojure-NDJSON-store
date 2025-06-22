(ns mi-app-clj.core
  (:require [mi-app-clj.utils :as utils]
            [repository.bubas :as bubas]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn create-example-bear!
  "Creates an example bear entry"
  [repo-atom bear-num]
  (let [bear-id (str "bear-" bear-num)
        client-id "client-456"
        scope "read write"
        expires-at (+ (System/currentTimeMillis) (* 3600 1000)) ; 1 hour from now
        [new-repo entry] (bubas/store-bear! @repo-atom bear-id client-id scope expires-at)]
    (reset! repo-atom new-repo)
    (log/debug "Creating bear:" bear-id)
    entry))

(defn create-example-grizzly!
  "Creates an example grizzly entry"
  [repo-atom grizzly-num]
  (let [grizzly-id (str "grizzly-" grizzly-num)
        client-id "client-456"
        scope "read"
        [new-repo entry] (bubas/store-grizzly! @repo-atom grizzly-id client-id scope)]
    (reset! repo-atom new-repo)
    (log/debug "Creating grizzly:" grizzly-id)
    entry))

(defn- test1 []
  (let [bubas-repository-1 (atom (bubas/create-bubas-repository "ositos.nosql"))
        bubas-repository-2 (atom (bubas/create-bubas-repository "yoggies.ndjson"))
        bubas-repository-read-1 (atom nil)
        bubas-repository-read-2 (atom nil)]
    ;; Create example bear and grizzly
    (println "\nCreating example bear and grizzly in first repo:")
    (doseq [i (range 1 10001)]
      (create-example-bear! bubas-repository-1 i)
      (create-example-grizzly! bubas-repository-1 i)
      (create-example-bear! bubas-repository-2 i)
      (create-example-grizzly! bubas-repository-2 i))

    (reset! bubas-repository-read-1 (bubas/create-bubas-repository "ositos.nosql"))
    (reset! bubas-repository-read-2 (bubas/create-bubas-repository "yoggies.ndjson"))

    ;; First comparison (original)
    (let [bear-keys1 (-> @bubas-repository-1 :indexes (get "bear") keys sort)
          bear-keys1-read (-> @bubas-repository-read-1 :indexes (get "bear") keys sort)]
      (log/info "\nComparing bear keys between bubas-repository-1 and bubas-repository-read-1:")
      (log/info "Original bubas-repository-1 keys count:" (count bear-keys1))
      (log/info "Read bubas-repository-read-1 keys count:" (count bear-keys1-read))
      (log/info "Keys match?" (= bear-keys1 bear-keys1-read))
      (log/debug "Extra keys in bubas-repository-read-1:" (seq (remove (set bear-keys1) bear-keys1-read)))
      (log/debug "Extra keys in original bubas-repository-1:" (seq (remove (set bear-keys1-read) bear-keys1))))

    (let [grizzly-keys1 (-> @bubas-repository-1 :indexes (get "grizzly") keys sort)
          grizzly-keys1-read (-> @bubas-repository-read-1 :indexes (get "grizzly") keys sort)]
      (log/info "\nComparing grizzly keys between bubas-repository-1 and bubas-repository-read-1:")
      (log/info "Original bubas-repository-1 keys count:" (count grizzly-keys1))
      (log/info "Read bubas-repository-read-1 keys count:" (count grizzly-keys1-read))
      (log/info "Keys match?" (= grizzly-keys1 grizzly-keys1-read))
      (log/debug "Extra keys in bubas-repository-read-1:" (seq (remove (set grizzly-keys1) grizzly-keys1-read)))
      (log/debug "Extra keys in original bubas-repository-1:" (seq (remove (set grizzly-keys1-read) grizzly-keys1))))

    ;; Second comparison (repo2 vs read2)
    (let [bear-keys2 (-> @bubas-repository-2 :indexes (get "bear") keys sort)
          bear-keys2-read (-> @bubas-repository-read-2 :indexes (get "bear") keys sort)]
      (log/info "\nComparing bear keys between bubas-repository-2 and bubas-repository-read-2:")
      (log/info "Original bubas-repository-2 keys count:" (count bear-keys2))
      (log/info "Read bubas-repository-read-2 keys count:" (count bear-keys2-read))
      (log/info "Keys match?" (= bear-keys2 bear-keys2-read))
      (log/debug "Extra keys in bubas-repository-read-2:" (seq (remove (set bear-keys2) bear-keys2-read)))
      (log/debug "Extra keys in original bubas-repository-2:" (seq (remove (set bear-keys2-read) bear-keys2))))

    (let [grizzly-keys2 (-> @bubas-repository-2 :indexes (get "grizzly") keys sort)
          grizzly-keys2-read (-> @bubas-repository-read-2 :indexes (get "grizzly") keys sort)]
      (log/info "\nComparing grizzly keys between bubas-repository-2 and bubas-repository-read-2:")
      (log/info "Original bubas-repository-2 keys count:" (count grizzly-keys2))
      (log/info "Read bubas-repository-read-2 keys count:" (count grizzly-keys2-read))
      (log/info "Keys match?" (= grizzly-keys2 grizzly-keys2-read))
      (log/debug "Extra keys in bubas-repository-read-2:" (seq (remove (set grizzly-keys2) grizzly-keys2-read)))
      (log/debug "Extra keys in original bubas-repository-2:" (seq (remove (set grizzly-keys2-read) grizzly-keys2))))

    ;; Third comparison (repo1 vs read2)
    (let [bear-keys1 (-> @bubas-repository-1 :indexes (get "bear") keys sort)
          bear-keys2-read (-> @bubas-repository-read-2 :indexes (get "bear") keys sort)]
      (log/info "\nComparing bear keys between bubas-repository and bubas-repository-read-2:")
      (log/info "Original bubas-repository-1 keys count:" (count bear-keys1))
      (log/info "Read bubas-repository-read-2 keys count:" (count bear-keys2-read))
      (log/info "Keys match?" (= bear-keys1 bear-keys2-read))
      (log/debug "Extra keys in bubas-repository-read-2:" (seq (remove (set bear-keys1) bear-keys2-read)))
      (log/debug "Extra keys in original bubas-repository-1:" (seq (remove (set bear-keys2-read) bear-keys1))))

    (let [grizzly-keys1 (-> @bubas-repository-1 :indexes (get "grizzly") keys sort)
          grizzly-keys2-read (-> @bubas-repository-read-2 :indexes (get "grizzly") keys sort)]
      (log/info "\nComparing grizzly keys between bubas-repository-1 and bubas-repository-read-2:")
      (log/info "Original bubas-repository-1 keys count:" (count grizzly-keys1))
      (log/info "Read bubas-repository-read-2 keys count:" (count grizzly-keys2-read))
      (log/info "Keys match?" (= grizzly-keys1 grizzly-keys2-read))
      (log/debug "Extra keys in bubas-repository-read-2:" (seq (remove (set grizzly-keys1) grizzly-keys2-read)))
      (log/debug "Extra keys in original bubas-repository-1:" (seq (remove (set grizzly-keys2-read) grizzly-keys1))))))

(defn -main
  "Main function that runs when starting the application"
  [& args]
  (println "Aplicación de consola para tester repositorio polimorfico")
  (println "Argumentos recibidos:" (or args "ninguno"))
  (println "Versión:" (utils/version))
  (test1)
  (println "Fin de la aplicación"))

