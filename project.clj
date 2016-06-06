(defproject slack-cmds "0.1.1"
  :description "Slack commands For VersionEye"
  :url "https://www.github.com/timgluz/slack-cmds"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [funcool/catacumba "0.17.0"]
                 [clj-http "3.1.0"]
                 [timgluz/spyglass "1.2.0"]
                 ;; Infrastructure
                 [org.slf4j/slf4j-simple "1.7.21" :scope "provided"]
                 [com.stuartsierra/component "0.3.1"]
                 [prismatic/schema "1.0.5"]
                 [environ "1.0.3"]]
  
  :main slack-cmds.server
  :repl-options {:init-ns user
                 :welcome (println "Type (dev) to start")}
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [reloaded.repl "0.2.1"]]
                   :source-paths ["dev"]}}
  )
