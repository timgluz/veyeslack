(ns slack-cmds.server
  (:require [catacumba
              [core :as ct]
              [http :as http]
              [serializers :as sz]]
            [catacumba.handlers.parse :as parse]
            [clojure.string :as string])
  (:gen-class))

(defmulti cmd-dispatcher (fn [msg] (-> msg :text str (string/split #"\s+") first keyword)))

(defmethod cmd-dispatcher :search [msg]
  (let [query (-> msg :text str string/split)]
    {:response_type "ephemeral"
     :text (str "Search results for `" (:text msg) "`")
     :attachments [{:text "not implemented"}]}))

(defmethod cmd-dispatcher :help [msg]
  {:response_type "ephemeral"
   :text "VersionEye commands:"
   :attachments [{:text "/veye clojure - search a package"}
                 {:text "/veye connect - save your api-key"}
                 {:text "/veye help    - show commands"}]})

(defmethod cmd-dispatcher :default [msg]
  {:text (str "unsupported cmd `" (:text msg) "` - more info `/veye help`")})


(defn help-handler
  [context]
  (http/ok
    (sz/encode 
      (cmd-dispatcher {:text "help"})
      :json)
    {:content-type "application/json"}))

(defn cmd-handler
  [context]
  (let [dt (:data context)]
    (println "#-- command data: " context)
    (http/ok
      (sz/encode dt :json)
      {:content-type "application/json"})))

(def app
  (ct/routes
    [[:prefix "commands"
      [:any (parse/body-params)]
      [:post cmd-handler]]
     [:all help-handler]]))

(defn -main [& args]
  (ct/run-server app {:port 3030
                      :debug true}))
