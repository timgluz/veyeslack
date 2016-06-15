(ns veyeslack.db
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]))


(s/defschema DBConfigMap
  {:host s/Str
   :port s/Num
   :database s/Str
   :user s/Str
   :password (s/maybe s/Str)})

(s/defrecord DBClient [configs :- DBConfigMap]
  component/Lifecycle
  (start [this]
    (assoc this :spec {:classname "org.postgresql.Driver"
                       :subprotocol "postgresql"
                       :subname (str "//"
                                     (:host configs) "/"
                                     (:database configs))
                       :user (:user configs)
                       :password (:password configs)}))
  (stop [this]
    (dissoc this :spec)))

(s/defn create [db-configs :- DBConfigMap]
  (->DBClient db-configs))


