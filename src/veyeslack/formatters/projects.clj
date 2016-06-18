(ns veyeslack.formatters.projects
  (:require [veyeslack.formatters.helpers :refer [to-veye-url]]))

(defn ->list-success
  [api-res {:keys [org-name team-name]}]
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
    {:text (str "VersionEye projects "
                 (cond
                   (and org-name team-name) (str "for team " team-name)
                   (and org-name (nil? team-name)) (str "for organization " org-name)
                   :else " you have access to"))
     :attachments (doall
                    (map #(to-project-detail %)
                         (:body api-res)))}))

(defn ->list-error
  [api-res {:keys [org-name team-name]}]
  {:text "Failed to make API request"
   :color "danger"})
