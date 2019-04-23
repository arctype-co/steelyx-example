(ns steelyx-example.schema
  (:require
    [schema.core :as S]))

(def SQSQueueConfig
  {:name S/Str
   :region S/Str})

(def SQSConfig
  {:queues {S/Keyword SQSQueueConfig}})

(def KinesisStreamConfig
  {:shards S/Int
   :region S/Str
   (S/optional-key :poll-interval-ms) S/Int})

(def KinesisConfig
  {:streams {S/Keyword KinesisStreamConfig}})
