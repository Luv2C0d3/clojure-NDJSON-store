(ns mi-app-clj.core
  (:require [mi-app-clj.utils :as utils]
            [repository.ndjson :refer [create-repository find-by-key add! delete!]]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn -main
  "Main function that runs when starting the application"
  [& args]
  (println "Aplicación de consola para tester repositorio polimorfico")
  (println "Argumentos recibidos:" (or args "ninguno"))
  (println "Versión:" (utils/version))
  (println "Fin de la aplicación"))

