(ns slack-cmds.handlers.commands
  (:require [catacumba
              [http :as http]
              [serializers :as sz]]
            [manifold.deferred :as md]
            [clojure.string :as string]
            [slack-cmds.api :as api]))

(defn join-tokens
  [tkns]
  (apply str (interpose " " tkns)))

(defn to-safe-key [prod-key]
  (-> prod-key (string/replace #"\/" ":")))

(def veye-url  "https://www.versioneye.com")

(defn to-veye-url [& path-items]
  (->> path-items
    (cons veye-url)
    (interpose "/")
    (apply str)))

(defn split-args [task-txt]
  (-> task-txt str (string/split #"\s+") vec))

;;TODO: refactor into slack-cmds.dispatchers.command
(defmulti cmd-dispatcher (fn [cmd-dt] (-> cmd-dt :text split-args first keyword)))

(defmethod cmd-dispatcher :search [cmd-dt]
  (let [api-key (api/get-user-key (:team_id cmd-dt) (:user_id cmd-dt))
        query-term (-> cmd-dt :text split-args rest join-tokens)
        response-p (md/deferred)
        to-item-list (fn [items]
                       (for [item items]
                         {:title (str (:name item) ", " (:version item))
                          :title_link (to-veye-url (:language item)
                                                   (to-safe-key (:prod_key item))
                                                   (:version item))
                          :text (str (:language item) ", " (:prod_key item))}))
        on-success (fn [res]
                    (md/success! response-p
                      {:response_type "ephemeral"
                       :text (str "Search results for `" query-term "`")
                       :attachments (-> res :body :results (#(take 10 %)) to-item-list)}))
        on-error (fn [err]
                   (md/success! response-p
                     {:response_type "ephemeral"
                      :text (str "Failed to execute search query for " query-term)
                      :attachments [{:title "Response from VersionEye"
                                     :text (str err)}]}))]
    (md/on-realized (md/future (api/search api-key query-term {:n 10}))
                    on-success
                    on-error)
    response-p))

(def not-authorized-response
  {:response_type "ephemeral"
   :text "You havent save connect your API key yet;"
   :attachments [{:title "Tip: use `/veye connect YOURAPIKEY`"
                  :text "This command will temporary save your VersionEye key"}]})

(defmethod cmd-dispatcher :list [cmd-dt]
  (if-let [api-key (api/get-user-key (:team_id cmd-dt) (:user_id cmd-dt))]
    (let [[_ org-name team-name _] (split-args (:text str))
          response-p (md/deferred)
          outdated-ratio (fn [proj]
                            (let [[total outdated] ((juxt :dep_number :out_number) proj)]
                              (if (and (number? total) (pos? total))
                                (/ outdated (float total))
                                0)))
          to-attachment (fn [proj]
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
                                     :short true}]})
          to-response (fn [dt]
                        {:text (str "VersionEye projects "
                                     (cond
                                       (and org-name team-name) (str "for team " team-name)
                                       (and org-name (nil? team-name)) (str "for organization " org-name)
                                       :else " you have access to"))
                         :attachments (doall
                                        (map #(to-attachment %) dt))})
          api-error-response {:text "Failed to make API request"
                              :color "danger"}]
     (md/on-realized
       (md/future (api/project-list api-key {:org org-name :team team-name}))
       (fn [res] (md/success! response-p (-> res :body to-response)))
       (fn [res] (md/success! response-p api-error-response)))
      response-p)
    ;;when user had no api-key attached
    not-authorized-response
    ))

(defmethod cmd-dispatcher :connect [cmd-dt]
  (let [[_ the-api-key] (split-args (:text cmd-dt))]
    (if (empty? the-api-key)
      {:response_type "ephemeral"
       :text "You forgot API key"}
      (do
        (api/set-user-key! (:team_id cmd-dt) (:user_id cmd-dt) the-api-key)
        {:response_type "ephemeral"
         :text "Your API key is now memorized for 12hours."}))))

;;delicate this task to help-dispatcher
(defmethod cmd-dispatcher :help [cmd-dt]
  {:response_type "ephemeral"
   :text "VersionEye commands:"
   :attachments [{:text "/veye search clojure - search a package"}
                 {:text "/veye list <ORGA_NAME> <TEAM_NAME> - list your projects"}
                 {:text "/veye connect - save your api-key"}
                 {:text "/veye help    - show commands"}]})

(defmethod cmd-dispatcher :default [cmd-dt]
  {:response_type "ephemeral"
   :text (str "unsupported cmd `" (:text cmd-dt) "` - more info `/veye help`")})

(def timed-out-response
  {:response_type "ephemeral"
   :text "execution time-out"})

(defn handler
  [context]
  (if-let [cmd-dt (:data context)]
    (let [res (cmd-dispatcher cmd-dt)]
      (http/ok
        (sz/encode (if (md/deferrable? res)
                     @(md/timeout! res 2500 timed-out-response)
                     res)
                   :json)
        {:content-type "application/json"}))
    ;;when we didnt get any useful data from user
    (http/bad-request
      (sz/encode {:reason "not valid command"} :json)
      {:content-type "application/json"})))


