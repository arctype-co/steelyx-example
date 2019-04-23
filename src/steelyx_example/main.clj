(ns ^{:doc "System launcher"}
  steelyx-example.main
  (:gen-class)
  (:import
    [java.util TimeZone])
  (:require
    [clojure.tools.cli :as cli]
    [clojure.tools.logging :as log]
    [clojure.tools.namespace.repl :as tools-ns]
    [arctype.service.config :as config]
    [arctype.service.protocol :as proto]
    [schema.core :as S]
    [sundbry.resource :refer [initialize invoke invoke-reverse]]
    [steelyx-example.service :as service]))

(def ^:private cli-options
  [;; A boolean option defaulting to nil
   ["-v"  "--version" "Show version"]
   ["-h"  "--help" "Show help"]
   [nil "--once" "Run once and stop"]])

(def ^:private ^:dynamic *cli* nil)

(TimeZone/setDefault (TimeZone/getTimeZone "UTC"))

(defmacro software-version
  []
  (let  [version-str (System/getProperty  "steelyx-example.version")]
    version-str))

(defonce instance (atom nil))

; Enable type checking initially.
; When the system launches, we may disable it again.
(S/set-fn-validation! true)

(defn- start-service
  [sys cfg]
  (if (some? sys)
    (do 
      (log/warn {:message "System already started!"})
      sys)
    (try 
      (-> cfg
          (service/create)
          (initialize)
          (invoke proto/start))
      (catch Exception e
        (log/error e)
        nil))))

(defn- default-config-path
  []
  (or (System/getenv "APP_CONFIG") "resources/config/app.yml"))

(defn start 
  ([]
   (swap! instance start)
   nil)

  ([sys]     
   (start-service sys 
                  (assoc (config/read (default-config-path))
                         :cli *cli*)))

  ([sys cfg] 
   (start-service sys cfg)))

(defn stop 
  ([]
   (swap! instance stop)
   nil)

  ([sys]
   (if (some? sys)
     (try
       (-> sys
           (invoke-reverse proto/stop))
       (catch Exception e
         (log/error e)))
     (log/warn {:message "System already stopped!"}))
   nil))

(defn restart
  ([]
   (swap! instance restart)
   nil)

  ([sys]
   (-> sys stop start)))

(defn reload
  "Stop the server, reload code, and restart."
  []
  (stop)
  (try 
    (tools-ns/set-refresh-dirs "src")
    (tools-ns/refresh :after 'steelyx-example.main/start)
       (catch Throwable e
         (.printStackTrace e)))
  nil)

(defn- app-main
  [{:keys [options] :as cli}]
  (binding [*cli* cli]
    (doto (Runtime/getRuntime)
      (.addShutdownHook
        (Thread. (fn []
                   (log/info "Invoking shutdown hook")
                   (swap! instance stop)))))
    (start)
    (when (:once options)
      (stop)
      (System/exit 0))))

(defn -main
  [& args] 
  (let [{:keys [options errors] :as cli} (cli/parse-opts args cli-options)]
    (cond 

      (some? errors)
      (do
        (doseq [error errors]
          (println error))
        (System/exit 1))

      (true? (:version options))
      (println (software-version))

      (true? (:help options))
      (println (:summary cli))

      :else 
      (app-main cli))))
