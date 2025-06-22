(ns mi-app-clj.core-test
  (:require [clojure.test :refer :all]
            [mi-app-clj.core :refer :all]
            [mi-app-clj.utils :as utils]))

(deftest test-utils
  (testing "Versión de la aplicación"
    (is (= "0.1.0" (utils/version))))
  (testing "Suma de números"
    (is (= 5 (utils/sumar 2 3)))))