(ns veyeslack.models.notification
  (:require [clojure.java.jdbc :as jdbc]
            [schema.core :as s]
            [clj-time.core :as dt]
            [clj-time.format :as df]
            [clj-time.jdbc]
            [veyeslack.models.utils :refer [normalize-str pg-jsonb-extension]]))

(s/defschema with-timestamps
  {:created_at org.joda.time.DateTime
   :updated_at org.joda.time.DateTime})

(s/defschema NotificationItem
  {:name s/Str
   :language s/Str
   :prod_key s/Str
   :version s/Str
   :prod_type s/Str})

(s/defschema NewNotification
  {:team_id s/Str
   :success s/Bool
   :items [NotificationItem]})

(s/defschema Notification
  (merge {:id s/Num
          :n_tries s/Num
          :sent_at org.joda.time.DateTime}
         NewNotification
         with-timestamps))

(defn ->safely-to-datetime
  [date-txt]
  (try
    (df/parse (df/formatters :date-time) (str date-txt))
    (catch Exception e
      (println "Failed to parse date: " date-txt))))

(defn process-api-result
  "transforms a item of VersionEye notifications to NewNotification"
  [team-id notification-dt]
  (letfn [(published-today? [the-notif]
            (if-let [notif-dt (->safely-to-datetime (:created_at the-notif))]
              (dt/within?
                (dt/interval (dt/today-at 0 0) (dt/today-at 23 59))
                notif-dt)
              false))]
    {:team_id (normalize-str team-id)
     :success false
     :items {:pkgs (->> notification-dt
                      :notifications
                      (filter published-today?) ;;ignore old notifications
                      (map #(get % :product))
                      (remove empty?)
                      (vec)
                      (doall))}}))

(s/defn add!
  "saves pre-processed notification into database"
  [db-client :- s/Any
   the-notif :- NewNotification]
  (first
    (jdbc/insert! (:spec db-client)
                  :notifications
                  (assoc the-notif
                         :n_tries 0
                         :created_at (dt/now)
                         :updated_at (dt/now)))))

(defn get-one
  [db-client notif-id]
  (first
    (jdbc/query
      (:spec db-client)
      ["SELECT * FROM notifications WHERE id = ?" notif-id])))

(defn mark-sent!
  "marks that message were sent out"
  [db-client notif-id]
  (-> (:spec db-client)
    (jdbc/update! :notifications
                  {:success true
                   :sent_at (dt/now)}
                  ["id = ?" notif-id])
    first
    pos?))

(defn mark-failed!
  "marks that sending a message failed and increase n_tries"
  [db-client notif-id]
  (-> (:spec db-client)
    (jdbc/execute!
      ["UPDATE notifications SET success = false, n_tries = n_tries + 1 WHERE id = ?" notif-id])
    first
    pos?))

(defn get-many-resendable
  "returns a list of notifications which are possible to re-send"
  [db-client]
  (jdbc/query
    (:spec db-client)
    ["SELECT * FROM notifications WHERE success = false AND (n_tries > 0 AND n_tries < 4)"]))

