(ns mi-app-clj.core
  (:require [mi-app-clj.utils :as utils])
  (:gen-class))

(defn -main
  "Función principal que se ejecuta al iniciar la aplicación"
  [& args]
  (println "¡Bienvenido a mi aplicación de consola!")
  (println "Argumentos recibidos:" (or args "ninguno"))
  (println "Versión:" (utils/version))
  (println "Suma de 2 + 3:" (utils/sumar 2 3)))