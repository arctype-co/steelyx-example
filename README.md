# Example Onyx/Steelyx Data Engine

[Steelyx](https://github.com/arctype-co/steelyx/) provides a high level API service for managing [Onyx](https://github.com/onyxplatform/onyx/) clusters.

![API overview](https://i.imgur.com/Ryl3ClP.png)

## Usage

```clj
(def job-set #{#'foo/job #'bar/job})
(steelyx/create :steelyx (:steelyx config) job-set)
```
    
Construct steelyx with a configuration dictionary, and a set of job constructors. The job constructors should be a function taking a reference to a context object, and their specific configuration. Give the job a name in the metadata of the function - this will be the name of the job in Onyx for named job management.

You can set your own context object with ```(steelyx/set-job-context! steelyx my-context)```

We use plumatic/schema for type checking our job configs, to prevent any bad configurations from running in production.

Here is an example job which reads data from AWS Kinesis and posts emails via Mandrill.

```clj
(S/defn ^{:job-name "mandrill"} job
  "Return a job spec for the Mandrill job"
  [context
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
```
    

## Job Configuration
The server loads a config file from the default path at resources/config/app.yml.
Set the envrionment variable *APP_CONFIG* to override the default config file path.
Each job should have some configuration options in your app config.

## Development
Launch a repl with the following command:

    make repl

Run the server from the repl:

    ; Start the server
    (start)
    ; Stop server, reload code, and restart
    (reload)

Steelyx includes clojure.tools.namespace for hot-reloading your code for fast development. The (reload) macro will handle stopping, reloading, and restarting for you to work at a fast pace. 
