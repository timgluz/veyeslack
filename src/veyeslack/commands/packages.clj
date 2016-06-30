(ns veyeslack.commands.packages
  (:require [manifold.deferred :as md]
            [veyeslack.services.versioneye :as versioneye]))

(defn search
  [api-key query-term {:keys [n] :or {n 10}} on-success on-error]
  (let [res-p (md/deferred)]
    (md/on-realized
      (md/future (versioneye/search api-key query-term {:n n}))
      (fn [res] (md/success! res-p (on-success res)))
      (fn [res] (md/success! res-p (on-error res))))
    res-p))

