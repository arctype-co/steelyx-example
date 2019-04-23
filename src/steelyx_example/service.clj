(ns ^{:doc "Data processing service"}
  steelyx-example.service
  (:require
    [clojure.tools.logging :as log]
    [arctype.service.protocol :refer [PLifecycle]]
    [arctype.service.io.http.http-kit :as http-kit]
    [arctype.service.onyx :as data-service]
    [cheshire.core :as json]
    [schema.core :as S]
    [sundbry.resource :as resource :refer [with-resources]]
    [steelyx-example.schema :refer :all]
    [steelyx-example.job.mandrill :as mandrill]))

(def CliConfig
  {:arguments [S/Str]
   :options {S/Keyword S/Any}
   :summary S/Str ; Help summary
   :errors [S/Str]}) ; Error messages

(def Config
  {:cli (S/maybe CliConfig)
   (S/optional-key :type-validation?) S/Bool
   :http http-kit/Config
   :data-service data-service/Config
   :sqs SQSConfig
   :kinesis KinesisConfig})

(def ^:private default-config {})

(defrecord MainService [config]
  PLifecycle
  (start [this]
    (with-resources this [:data-service]
      (data-service/set-job-context! data-service this))
    (log/info {:message "Started example service"}) 
    this)

  (stop [this]
    (log/info {:message "Stopping example service"})
    this)

  )

(def job-set #{#'mandrill/job})

(defn create
  "Return a system to run the service"
  [config]
  (let [config (merge default-config config)]
    (S/validate Config config)
    (S/set-fn-validation! (boolean (:type-validation? config)))
    (resource/make-system
      (map->MainService
        {:config config})
      :system
      [(http-kit/create :http-kit (:http config) :data-service)
       (data-service/create :data-service (:data-service config) job-set)])))
