(ns steelyx-example.schema
  (:require
    [schema.core :as S]))

(def KinesisStreamConfig
  {:shards S/Int
   :region S/Str
   (S/optional-key :poll-interval-ms) S/Int})

(def KinesisConfig
  {:streams {S/Keyword KinesisStreamConfig}})
