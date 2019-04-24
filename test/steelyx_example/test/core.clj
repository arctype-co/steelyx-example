(ns steelyx-example.test.core
  (:require
    [clojure.test :refer :all]
    [schema.core :as S]))

(defn with-fn-validation
  [f]
  (S/with-fn-validation
    (f)))
