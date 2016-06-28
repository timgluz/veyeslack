(ns veyeslack.scheduler
  (:require [com.stuartsierra.component :as component]
            [clj-time.core :as dt]
            [immutant.scheduling :as qs] 
            [immutant.scheduling.joda]))

(defn pulse-job []
  (println "#-- tick"))

(defrecord JobScheduler [db]
  component/Lifecycle
  (start [this]
    (let [the-scheduler (qs/schedule pulse-job
                                     {:id :pulse
                                      :in [2 :minutes]
                                      :every [10 :seconds]})]
      (println "#-- starting a JobScheduler")
      (assoc this :scheduler the-scheduler)))
  
  (stop [this]
    (println "#-- stopping scheduler")
    (qs/stop (qs/id :pulse))
    (dissoc this :scheduler)))

(defn create [db]
  (->JobScheduler db))
