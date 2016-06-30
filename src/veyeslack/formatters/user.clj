(ns veyeslack.formatters.user
  (:require [veyeslack.formatters.helpers :refer [display-type to-safe-key to-veye-url]]))

(defn ->connect-success
  [api-key-dt]
  {:response_type (display-type false)
   :text "Your API key is now saved - all the commands are unlocked."})

(defn ->connect-failure
  [api-key-dt]
  {:response_type (display-type false)
   :text "Failed to save your API key. Contact info: /veye info"})

(defn ->notification-success
  [notif-dt public?]
  (let [pkg-notifs (get-in notif-dt [:items :pkgs] [])
        to-pkg (fn [pkg-dt]
                 {:title (str (:name pkg-dt) " - " (:version pkg-dt))
                  :title_link (to-veye-url (:language pkg-dt)
                                           (to-safe-key (:prod_key pkg-dt))
                                           (:version pkg-dt))
                  :color "good"})]
    (if (empty? pkg-notifs)
      {:response_type (display-type public?)
       :text "No updates for the followed packages"}
      ;;if user had any updates
      {:response_type (display-type public?)
       :text "VersionEye found today new releases for these followed packages:"
       :attachments (doall (map #(to-pkg %) pkg-notifs))})))

(defn ->notification-failure
  [notif-dt public?]
  {:response_type (display-type public?)
   :text "No notifications for today."})
