(ns veyeslack.server
  (:require [catacumba.core :as ct]
            [catacumba.handlers.parse :as parse]
            [catacumba.handlers.misc :refer [log]]
            [com.stuartsierra.component :as component]
            [catacumba.components :refer [catacumba-server assoc-routes! extra-data]]
            [clojure.string :as string]
            [schema.core :as s]
            [environ.core :refer [env]]
            [circleci.rollcage.core :as rollcage]
            [veyeslack.db :as db]
            [veyeslack.handlers.commands :as commands]
            [veyeslack.handlers.help :as help]
            [veyeslack.handlers.oauth :as oauth]
            [veyeslack.handlers.pages :as pages]
            [veyeslack.handlers.utils :refer [json-response default-error-handler
                                              command-error-handler]]))

(defn make-app-routes
  [app-system]
  [[:assets "static" {:dir "resources/static"}]
   [:error default-error-handler]
   [:any (extra-data {:app app-system})]
   [:prefix "commands"
      [:error command-error-handler]
      [:any (parse/body-params)]
      [:post commands/handler json-response]]
   [:prefix "oauth"
      [:any (parse/body-params)]
      [:get "request" oauth/request-handler]
      [:get "success" oauth/success-handler]
      [:get "failure" oauth/failure-handler]]
   [:prefix "pages"
    [:get "index" pages/index-handler]
    [:get "commands" pages/command-handler]]
   [:all pages/index-handler]])

(defrecord WebApp [server db]
  component/Lifecycle
  (start [this]
    (let [the-app-system (assoc this
                                :server server
                                :db db)]
    (println "#-- starting the app: \n")
    (assoc-routes! server ::web (make-app-routes the-app-system))
    the-app-system))
  
  (stop [this]
    (when (:server this)
      (.stop (:server this)))
    (when (:db this)
      (.stop (:db this)))))

(defn create-system
  "sets up and initializes system"
  [configs]
  (->
    (component/system-map
      :rollbar (rollcage/client
                 (:rollbar-token configs)
                 {:environment (get-in configs [:server :enviroment] "dev")})
      :catacumba (catacumba-server (:server configs))
      :postgres (db/create (:db configs))
      :app (->WebApp nil nil))
    (component/system-using
      {:app {:server :catacumba
             :db :postgres
             :yeller :rollbar}})))

(defn get-system-configs
  "collects system configs into unified hash-map"
  []
  {:rollbar-token (env :rollbar-token)
   :server {:enviroment (env :app-env)
            :port (Long. (or (env :app-port) 3030))
            :debug (= "dev" (env :app-env))
            :basedir (or (env :app-basedir) ".")}
   :db {:host (env :db-host)
        :port (Long. (or (env :db-port) 5432))
        :database (env :db-name)
        :user (env :db-user)
        :password (env :db-password)}
   :slack {:client-id (env :slack-client-id)
           :client-secret (env :slack-client-secret)
           :redirect-url (env :slack-redirect-url)
           :auth-url (env :slack-auth-url)}})

(defn start! []
  (let [the-system (create-system (get-system-configs))]
    (component/start the-system)))

