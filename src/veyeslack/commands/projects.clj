(ns veyeslack.commands.projects
  (:require [manifold.deferred :as md]
            [veyeslack.api :as api]))

(defn list-n
  [api-key params on-success on-error]
  (let [res-p (md/deferred)]
    (md/on-realized
      (md/future (api/project-list api-key params))
      (fn [res] (md/success! res-p (on-success res)))
      (fn [res] (md/success! res-p (on-error res))))
    res-p))
