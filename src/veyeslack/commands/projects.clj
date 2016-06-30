(ns veyeslack.commands.projects
  (:require [manifold.deferred :as md]
            [veyeslack.services.versioneye :as versioneye]))

(defn list-n
  [api-key params on-success on-error]
  (let [res-p (md/deferred)]
    (md/on-realized
      (md/future (versioneye/project-list api-key params))
      (fn [res] (md/success! res-p (on-success res)))
      (fn [res] (md/success! res-p (on-error res))))
    res-p))

(defn project
  [api-key project-id on-success on-error]
  (let [res-p (md/deferred)]
    (md/on-realized
      (md/future (versioneye/project-details api-key project-id))
      (fn [res] (md/success! res-p (on-success res)))
      (fn [res] (md/success! res-p (on-error res))))
    res-p))
