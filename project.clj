(defproject steelyx-example "0.1.0-SNAPSHOT" 

  :dependencies
  [[org.clojure/clojure "1.10.0"]
   [org.clojure/core.async "0.4.490"]
   [org.clojure/core.match "0.3.0-alpha5"]
   [org.clojure/tools.cli "0.3.6"]
   [org.clojure/tools.namespace "0.3.0-alpha4"]
   [org.clojure/tools.nrepl "0.2.13"]
   [org.clojure/tools.logging "0.4.0"]
   [arctype/service "1.0.0"
    :exclusions [log4j]]
   [arctype/steelyx "0.2.0-SNAPSHOT"
    :exclusions [org.slf4j/slf4j-nop org.slf4j/slf4j-api org.slf4j/slf4j-log4j12]]
   [cheshire "5.8.0"]
   [http-kit "2.3.0"]
   [org.apache.logging.log4j/log4j-api "2.11.1"]
   [org.apache.logging.log4j/log4j-core "2.11.1"]
   [org.apache.logging.log4j/log4j-slf4j-impl "2.11.1"]
   [org.apache.logging.log4j/log4j-jcl "2.11.1"]
   [org.onyxplatform/onyx-http "0.14.1.0"
    :exclusions [onyxplatform/onyx]]
   ; onyx-amazon-kinesis pending publishing to clojars by org.onyxplatform
   [arctype/onyx-amazon-kinesis "0.14.1.1" 
    :exclusions [onyxplatform/onyx]]
   [prismatic/schema "1.1.10"]
   [sundbry/resource "0.4.0"]]

  :source-paths ["src"]

  :test-selectors
  {:default #(not (:skip %))
   :unit #(and (not (:skip %)) (:unit %)) }

  :main ^:skip-aot steelyx-example.main

  :jvm-opts ["-server"]

  :repl-options
  {:timeout 300000}

  :profiles
  {:debug [:default]
   :release [:default :aot]

   :aot 
   {:aot :all
    :main steelyx-example.main
    :omit-source true
    :javac-options ["-Dclojure.compiler.direct-linking=true"]
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
