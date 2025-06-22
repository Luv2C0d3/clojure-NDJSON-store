(ns mi-app-clj.core
  (:require [mi-app-clj.utils :as utils]
            [repository.bubas :as bubas])
  (:gen-class))

(defonce bubas-repository (atom nil))
(defonce bubas-repository-2 (atom nil))

(defn create-example-bear!
  "Creates an example bear entry"
  [repo-atom]
  (let [bear-id "bear-123"
        client-id "client-456"
        scope "read write"
        expires-at (+ (System/currentTimeMillis) (* 3600 1000)) ; 1 hour from now
        [new-repo entry] (bubas/store-bear! @repo-atom bear-id client-id scope expires-at)]
    (reset! repo-atom new-repo)
    (println "Creating bear:" bear-id)
    entry))

(defn create-example-grizzly!
  "Creates an example grizzly entry"
  [repo-atom]
  (let [grizzly-id "grizzly-789"
        client-id "client-456"
        scope "read"
        [new-repo entry] (bubas/store-grizzly! @repo-atom grizzly-id client-id scope)]
    (reset! repo-atom new-repo)
    (println "Creating grizzly:" grizzly-id)
    entry))

(defn -main
  "Main function that runs when starting the application"
  [& args]
  ;; Initialize the repository
  (reset! bubas-repository (bubas/create-bubas-repository "ositos.nosql"))
  (reset! bubas-repository-2 (bubas/create-bubas-repository "yoggies.ndjson"))

  (println "¡Bienvenido a mi aplicación de consola!")
  (println "Argumentos recibidos:" (or args "ninguno"))
  (println "Versión:" (utils/version))
  
  ;; Create example bear and grizzly
  (println "\nCreating example bear and grizzly in first repo:")
  (let [bear (create-example-bear! bubas-repository)
        grizzly (create-example-grizzly! bubas-repository)
        bear2 (create-example-bear! bubas-repository-2)
        grizzly2 (create-example-grizzly! bubas-repository-2)]
    (println "Created bear:" bear)
    (println "Created grizzly:" grizzly)
    (println "Created bear2:" bear2)
    (println "Created grizzly2:" grizzly2))

  (println "\nCreating example bear and grizzly in second repo:")
  (let [bear (create-example-bear! bubas-repository-2)
        grizzly (create-example-grizzly! bubas-repository-2)]
    (println "Created bear:" bear)
    (println "Created grizzly:" grizzly)))