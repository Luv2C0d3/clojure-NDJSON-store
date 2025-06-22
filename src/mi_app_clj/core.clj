(ns mi-app-clj.core
  (:require [mi-app-clj.utils :as utils]
            [repository.bubas :as bubas])
  (:gen-class))

(defn create-example-bear!
  "Creates an example bear entry"
  []
  (let [bear-id "bear-123"
        client-id "client-456"
        scope "read write"
        expires-at (+ (System/currentTimeMillis) (* 3600 1000))] ; 1 hour from now
    (println "Creating bear:" bear-id)
    (bubas/store-bear! bear-id client-id scope expires-at)))

(defn create-example-grizzly!
  "Creates an example grizzly entry"
  []
  (let [grizzly-id "grizzly-789"
        client-id "client-456"
        scope "read"]
    (println "Creating grizzly:" grizzly-id)
    (bubas/store-grizzly! grizzly-id client-id scope)))

(defn -main
  "Main function that runs when starting the application"
  [& args]
  (println "¡Bienvenido a mi aplicación de consola!")
  (println "Argumentos recibidos:" (or args "ninguno"))
  (println "Versión:" (utils/version))
  
  ;; Create example bear and grizzly
  (println "\nCreating example bear and grizzly:")
  (let [bear (create-example-bear!)
        grizzly (create-example-grizzly!)]
    (println "Created bear:" bear)
    (println "Created grizzly:" grizzly)))