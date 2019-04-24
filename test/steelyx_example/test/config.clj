(ns ^{:doc "Configuration unit tests"}
  steelyx-example.test.config
  (:require
    [clojure.test :refer :all]
    [arctype.service.config :as config]))

(deftest ^:unit test-read-config
  (is (map? (config/read "resources/config/app.yml"))))
