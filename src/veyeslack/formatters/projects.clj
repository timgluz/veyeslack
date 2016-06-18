(ns veyeslack.formatters.projects
  (:require [veyeslack.formatters.helpers :refer [to-veye-url display-type to-safe-key]]))

(defn ->list-success
  [api-res {:keys [org-name team-name public?] :or {public? false}}]
  (letfn [(outdated-ratio [proj]
            (let [[total outdated] ((juxt :dep_number :out_number) proj)]
              (if (and (number? total) (pos? total))
                (/ outdated (float total))
                0)))
          
          (to-project-detail [proj]
            {:title (:name proj)
             :title_link (to-veye-url "user/projects" (:id proj))
             :text (str "project id: " (:id proj))
             :color (cond
                      (= 0 (:out_number proj)) "good"
                      (< 0 (outdated-ratio proj) 0.25) "warning"
                      :else "danger")
             :fields [{:title "Dependencies"
                       :value (:dep_number proj)
                       :short true}
                      {:title "Outdated"
                       :value (:out_number proj)
                       :short true}
                      {:title "Licenses Red"
                       :value (:licenses_red proj)
                       :short true}
                      {:title "Project type"
                       :value (:project_type proj)
                       :short true}]})]
    {:response_type (display-type public?)
     :text (str "VersionEye projects "
                 (cond
                   (and org-name team-name) (str "for team " team-name)
                   (and org-name (nil? team-name)) (str "for organization " org-name)
                   :else " you have access to"))
     :attachments (doall
                    (map #(to-project-detail %)
                         (:body api-res)))}))

(defn ->list-error
  [api-res {:keys [org-name team-name]}]
  {:response_type (display-type false)
   :text "Failed to make API request"
   :color "danger"})

(defn ->project-success
  [api-res {:keys [id public?] :or {public? false}}]
  (letfn [(to-dep [dep-dt]
            {:title (str (:prod_key dep-dt))
             :title_link (to-veye-url (:language dep-dt)
                                      (to-safe-key (:prod_key dep-dt))
                                      (:version dep-dt))
             :color (if (true? (:outdated dep-dt))
                      "danger"
                      "good")
             :fields [{:title "Locked version"
                       :value (:version_requested dep-dt)
                       :short true}
                      {:title "Current version"
                       :value (:version_current dep-dt)
                       :short true}]})]
    (let [project-dt (:body api-res)]
      {:response_type (display-type public?)
       :text (str "Project details for: " id "\n"
                  "Outdated dependencies: "
                  (:out_number project-dt) "/" (:dep_number project-dt))
       :attachments (->> project-dt
                      (:dependencies)
                      (filter #(true? (:outdated %)))
                      (map #(to-dep %))
                      doall)})))

(defn ->project-error
  [api-res {:keys [id]}]
  {:response_type (display-type false)
   :text "Failed to fetch a project details"
   :color "danger"})

