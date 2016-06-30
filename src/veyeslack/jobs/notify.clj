(ns veyeslack.jobs.notify
  (:require [manifold.deferred :as md]
            [manifold.stream :as ms]
            [veyeslack.services.versioneye :as versioneye]
            [veyeslack.services.slack :as slack]
            [veyeslack.models.notification :as notif-mdl]
            [veyeslack.models.team :as team-mdl]
            [veyeslack.formatters.user :as user-fmt]))

(defn fetch-teams
  "puts list of authorized teams onto teams-list"
  [db-client out-ch]
  (if-let [teams (team-mdl/get-many-with-key db-client)]
    (doseq [the-team teams]
      @(ms/put! out-ch the-team)
      (Thread/sleep 50))
    (println "veyeslack.jobs.notify: found no teams."))
  (ms/close! out-ch)
  out-ch)

(defn fetch-notifications
  "fetches notifications for each team"
  [db-client team-ch delivery-ch]
  (loop []
    (when-not (ms/drained? team-ch)
      (let [the-team @(ms/take! team-ch)]
        @(md/on-realized
          (md/future (versioneye/user-notifications (:api_key the-team)))
          (fn [res]
            (->> (:body res)
              (notif-mdl/process-api-result (:team_id the-team))
              (notif-mdl/add! db-client)
              (merge the-team) ;;attaches team_id, channel_name and token to notif
              (ms/put! delivery-ch)
              (deref)))
          (fn [res]
            (println "veyeslack.jobs.notify: failed to pull notifications for "
                     (:team_id the-team) "reason: \n" res))))
      ;;after iteration
      (Thread/sleep 50)
      (recur)))
  (ms/close! delivery-ch)
  delivery-ch)

(defn deliver-messages
  "sends out notifications to a team channel"
  [db-client delivery-ch]
  (loop []
    (when-not (ms/drained? delivery-ch)
      (let [the-notif @(ms/take! delivery-ch)]
        (when-not (empty? the-notif)
          @(md/on-realized
            (md/future
              (slack/deliver-hook
                the-notif
                (user-fmt/->notification-success the-notif)))
             ;;on-success mark it as delivered
             (fn [res]
               (notif-mdl/mark-sent! db-client (:id the-notif)))
             ;;on-failure mark it failed
             (fn [res]
               (println "failed to deliver notification:\n" res)
               (notif-mdl/mark-failed! db-client (:id the-notif))))))

      ;;after each step
      (Thread/sleep 50)
      (recur)))
  (ms/close! delivery-ch) ;;should be closed now, just to be good scout
  delivery-ch)

(defn execute!
  [db-client]
  (let [teams-ch (ms/stream)
        notif-ch (ms/stream)
        delivery-ch (ms/stream)]
    @(md/zip
      (md/future (fetch-teams db-client teams-ch))
      (md/future (fetch-notifications db-client teams-ch delivery-ch))
      (md/future (deliver-messages db-client delivery-ch)))
    ::done))


(comment
  (require '[veyeslack.server :as server])
  (def the-db (-> (server/get-system-configs)
                (server/create-system)
                :postgres
                .start))
  
  (require '[veyeslack.models.team :as team-mdl])
  (def the-team (first (team-mdl/get-many-with-key the-db)))
 
  (identity the-team)

  (require '[veyeslack.services.slack :as slack] :reload)
  (slack/deliver-hook the-team {:text "Ping-Pong!"})

  (require '[veyeslack.jobs.notify :as notify-job] :reload-all)
  (notify-job/execute! the-db)
  )
