(defproject org.clojars.luv2c0d3/ndjson-repository "0.1.0-SNAPSHOT"
  :description "A Clojure library for NDJSON-based repositories with O(1) lookups through in-memory indexing"
  :url "https://github.com/luv2c0d3/ndjson-repository"
  :license {:name "EPL-2.0 OR GPL-2.0-with-classpath-exception"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [cheshire "5.12.0"]]
  :target-path "target/%s"
  :jar-exclusions [#"\.nosql$"]    ; Exclude test database files
  :source-paths ["src"]
  :java-source-paths ["src/java"]  ; If we add Java sources in the future
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}}
  :deploy-repositories [["releases" :clojars]
                       ["snapshots" :clojars]]
  :aot [org.clojars.luv2c0d3.ndjson-repository.java-api])
