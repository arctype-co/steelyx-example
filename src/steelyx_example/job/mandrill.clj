(ns steelyx-example.job.mandrill
  (:import
    [java.time Instant])
  (:require
    [clojure.tools.logging :as log]
    [cheshire.core :as json]
    [schema.core :as S]
    [onyx.plugin.kinesis]
    [onyx.plugin.http-output]
    [steelyx-example.job.core :as core]))

(defn- Message
  [param-types]
  (merge {:subject S/Str
          :template S/Str}
         param-types))

(def Config
  (core/job-config
    {:kinesis-stream S/Str
     :mandrill-api-key S/Str
     :from-email S/Str
     :from-name S/Str
     :messages
     {:password-recovery (Message {:link S/Str})
      :email-validation (Message {:link S/Str})}}))

(def default-job-config 
  core/default-job-config)

(defn- api-request
  [method api-key body]
  {:url (str "https://mandrillapp.com/api/1.0" method)
   :method :post 
   :args {:content-type :application/json
          :throw-exceptions false
          :form-params (assoc body :key api-key)}})

(defn- merge-var
  [[var-name content]]
  {:name (name var-name) :content content})

(S/defn ^:private send-template
  [job-config 
   message-type :- S/Keyword
   to :- [{:name S/Str :email S/Str}]
   template-content :- {S/Keyword S/Any}]
  ; https://www.mandrillapp.com/api/docs/messages.JSON.html#method-send-template
  (let [message-config (or (get-in job-config [:messages message-type])
                           (throw (ex-info "Undefined message type" {:message-type message-type
                                                                     :job-config job-config})))
        template-content (mapv merge-var template-content)
        params {:template_name (:template message-config)
                :template_content template-content
                :async true
                :message
                {:from_email (:from-email job-config)
                 :from_name (:from-name job-config)
                 :to to
                 :subject (:subject message-config)
                 :global_merge_vars template-content}}]
    (api-request "/messages/send-template.json" (:mandrill-api-key job-config) params)))

(defn- to-user-email
  [{:keys [firstName lastName]} email]
  [{:name (str firstName " " lastName)
    :email email}])

(defmulti event-handler (fn [_ event] (-> event :data :type keyword)))

(defmethod event-handler :default
  [job-config event]
  [])

(defmethod event-handler :emailValidationRequest
  [job-config {timestamp :timestamp
               {event-type :type
                event-id :eventId
                {user :user
                 email :email
                 activation-code :activationCode} :data} :data
               :as event}]
  (log/debug {:message "Email validation request"
              :event event})
  (->>
    {:link (format (-> job-config :messages :email-validation :link)
                   (:id user) email activation-code)}
    (send-template job-config :email-validation (to-user-email user email))))

(defmethod event-handler :passwordRecoveryRequest
  [job-config {timestamp :timestamp
               {event-type :type
                event-id :eventId
                {user :user
                 code :passwordRecoveryCode} :data} :data
               :as event}]
  (log/debug {:message "Password recovery request"
              :event event})
  (->>
    {:link (format (-> job-config :messages :password-recovery :link)
                   (:email user) code)}
    (send-template job-config :password-recovery (to-user-email user (:email user)))))

(S/defn catalog-event-handler
  [job-config :- Config]
  {:onyx/name ::event-handler
   :onyx/type :function
   :onyx/fn ::event-handler
   :onyx/batch-timeout 50
   :onyx/n-peers 2
   :onyx/batch-size 10
   :onyx/params [::config]
   ::config job-config})

(defn- response-ok?
  [response]
  (core/status-2xx? response))

(defn- catalog-post-to-mandrill
  []
  {:onyx/name ::post-to-mandrill
   :onyx/plugin :onyx.plugin.http-output/output
   :onyx/medium :http
   :onyx/type :output
   :onyx/n-peers 2
   :onyx/batch-timeout 1000
   :onyx/batch-size 1 
   :http-output/success-fn ::response-ok?
   :http-output/retry-params 
   {:base-sleep-ms 200
    :max-sleep-ms 5000
    :max-total-sleep-ms 20000}
   #_{:base-sleep-ms 200
    :max-sleep-ms 30000
    :max-total-sleep-ms 3600000}})

(S/defn ^{:job-name "mandrill"} job
  "Return a job spec for the Mandrill job"
  [data-service
   job-config :- Config]
  (let [job-config (merge default-job-config job-config)
        kinesis-config (:kinesis (:config data-service))
        kinesis-input (core/catalog-kinesis-input kinesis-config (:kinesis-stream job-config))
        kinesis-task-name (:onyx/name kinesis-input)]
    {:job-name "mandrill"
     :workflow [[kinesis-task-name ::event-handler]
                [::event-handler ::post-to-mandrill]]
     :catalog [kinesis-input
               (assoc (catalog-event-handler job-config)
                      :onyx/n-peers (:onyx/n-peers kinesis-input))
               (assoc (catalog-post-to-mandrill)
                      :onyx/n-peers (:onyx/n-peers kinesis-input))]
     :lifecycles [(core/standard-lifecycle job-config)
                  (core/lifecycle-kinesis-input kinesis-task-name)]
     :flow-conditions []
     :task-scheduler :onyx.task-scheduler/balanced}))
