(ns steelyx-example.job.core
  (:import
    [java.sql Timestamp])
  (:require
    [clojure.tools.logging :as log]
    [byte-streams :as bs]
    [cheshire.core :as json]
    [schema.core :as S]
    [sundbry.resource :as resource :refer [with-resources]]
    [steelyx-example.schema :refer :all]))

(defn job-config
  [specific-config]
  (merge {(S/optional-key :exception-policy) (S/maybe (S/enum :restart :kill))}
         specific-config))

(def default-job-config
  {:exception-policy :kill})

(defn deserialize-json
  [buf]
  (let [json-str (if (string? buf) buf (String. buf "UTF-8"))]
    (json/decode json-str true)))

(defn serialize-json
  [obj]
  (-> obj
      (json/encode)
      (.getBytes "UTF-8")))

(defn serialize-str
  [a-str]
  (.getBytes a-str "UTF-8"))

(defn handle-exception
  [event lifecycle lifecycle-phase e]
  (let [config (::config lifecycle)]
    (log/error e {:message "Onyx lifecycle exception"
                  ;:event event ; very verbose
                  :lifecycle lifecycle
                  :phase lifecycle-phase})
    (:exception-policy config)))

(def standard-lifecycle-calls
  {:lifecycle/handle-exception handle-exception})

(defn standard-lifecycle
  [job-config]
  {:lifecycle/task :all
   :lifecycle/calls ::standard-lifecycle-calls
   :lifecycle/doc "Standard lifecycles including error handling"
   ::config job-config})

(S/defn catalog-kinesis-input
  [kinesis-config :- KinesisConfig
   stream-id :- S/Str]
  (if-let [stream-config (get-in kinesis-config [:streams (keyword stream-id)])]
    {:onyx/name (keyword (str "kinesis-input-" stream-id))
     :onyx/plugin :onyx.plugin.kinesis/read-messages
     :onyx/type :input
     :onyx/medium :kinesis
     :kinesis/stream-name (name stream-id)
     :kinesis/shard-initialize-type :trim-horizon
     :kinesis/deserializer-fn ::deserialize-json
     :kinesis/region (:region stream-config)
     :kinesis/reader-backoff-ms 1000
     :kinesis/poll-interval-ms (or (:poll-interval-ms stream-config) 200)
     :onyx/batch-timeout 100 
     :onyx/n-peers (:shards stream-config)
     :onyx/batch-size 100
     :onyx/doc "Reads messages from a kinesis topic"}
    (throw (ex-info "Undefined Kinesis stream" {:stream-id stream-id}))))

(defn lifecycle-kinesis-input
  [task-name]
  {:lifecycle/task task-name
   :lifecycle/calls :onyx.plugin.kinesis/read-messages-calls})

(defn status-2xx?
  [{:keys [status] :as response}]
  (let [ok? (and (some? status)
                 (<= 200 status)
                 (< status 300))]
    (when-not ok?
      (log/warn "HTTP response error" (update response :body bs/to-string)))
    ok?))
