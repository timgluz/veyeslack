(ns veyeslack.commands.user
  (:require [manifold.deferred :as md]
            [veyeslack.models.api-key :as api-key-mdl]
            [veyeslack.models.notification :as notif-mdl]
            [veyeslack.services.versioneye :as versioneye]))

(defn connect
  "saves or updates users api-key"
  [db-client the-slack-message the-api-key on-success on-error]
  (let [res-p (md/deferred)]
    (md/on-realized
      (md/future (api-key-mdl/upsert! db-client the-slack-message the-api-key))
      (fn [res] (md/success! res-p (on-success res)))
      (fn [res] (md/success! res-p (on-error res))))
    res-p))

(defn notifications
  "fetches user todays notifications from the API"
  [api-key on-success on-error]
  (let [res-p (md/deferred)]
    (md/on-realized
      (md/future (versioneye/user-notifications api-key))
      (fn [res]
        (let [notif-dt (notif-mdl/process-api-result "" (:body res))]
          (md/success! res-p (on-success notif-dt))))
      ;render response when smt oopsie-doopsie occured
      (fn [res] (md/success! res-p (on-error res))))
    res-p))
