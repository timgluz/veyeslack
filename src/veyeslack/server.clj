(ns veyeslack.server
  (:require [catacumba.core :as ct]
            [catacumba.handlers.parse :as parse]
            [com.stuartsierra.component :as component]
            [catacumba.components :refer [catacumba-server assoc-routes! extra-data]]
            [clojure.string :as string]
            [schema.core :as s]
            [environ.core :refer [env]]
            [veyeslack.db :as db]
            [veyeslack.handlers.commands :as commands]
            [veyeslack.handlers.help :as help]
            [veyeslack.handlers.oauth :as oauth]))

(defn make-app-routes
  [app-system]
  [[:any (extra-data {:app app-system})]
    [:prefix "commands"
      [:any (parse/body-params)]
      [:post commands/handler]]
     [:prefix "oauth"
      [:any (parse/body-params)]
      [:get "request" oauth/request-handler]]
     [:all help/handler] ;;TODO refactor it as info-handler + include release dt
     ])

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
      :catacumba (catacumba-server (:server configs))
      :postgres (db/create (:db configs))
      :app (->WebApp nil nil))
    (component/system-using
      {:app {:server :catacumba
             :db :postgres}})))

(defn get-system-configs
  "collects system configs into unified hash-map"
  []
  {:server {:port (Long. (or (env :port) 3030))
            :debug (= "dev" (env :environment))}
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
    (println "Using database:" (:db (get-system-configs)))
    (component/start the-system)))

