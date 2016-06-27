(ns veyeslack.commands.user
  (:require [manifold.deferred :as md]
            [veyeslack.models.api-key :as api-key-mdl]))

(defn connect
  "saves or updates users api-key"
  [db-client the-slack-message the-api-key on-success on-error]
  (let [res-p (md/deferred)]
    (md/on-realized
      (md/future (api-key-mdl/upsert! db-client the-slack-message the-api-key))
      (fn [res] (md/success! res-p (on-success res)))
      (fn [res] (md/success! res-p (on-error res))))
    res-p))
