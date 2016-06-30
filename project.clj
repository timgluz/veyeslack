(defproject veyeslack "0.3.2"
  :description "Slack integration for VersionEye"
  :url "https://www.github.com/timgluz/veyeslack"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [funcool/catacumba "0.17.0"]
                 [clj-http "3.1.0"]
                 [clj-time "0.12.0"]
                 [timgluz/spyglass "1.2.0"]
                 [org.postgresql/postgresql "9.4.1208"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.immutant/scheduling "2.1.5"]
                 [selmer "1.0.7"]
                 [prismatic/schema "1.1.2"]
                 ;; Infrastructure
                 [org.slf4j/slf4j-simple "1.7.21" :scope "provided"]
                 [com.stuartsierra/component "0.3.1"]
                 [environ "1.0.3"]
                 [circleci/rollcage "0.2.2"]]

  :main veyeslack.main
  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")})
