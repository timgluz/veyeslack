(ns veyeslack.handlers.commands
  (:require [catacumba
              [http :as http]
              [serializers :as sz]]
            [manifold.deferred :as md]
            [clojure.string :as string]
            [veyeslack.api :as api]
            [veyeslack.commands.packages :as packages-cmd]
            [veyeslack.formatters.packages :as packages-fmt]
            [veyeslack.commands.projects :as projects-cmd]
            [veyeslack.formatters.projects :as projects-fmt]))

(def not-authorized-response
  {:response_type "ephemeral"
   :text "You havent save connect your API key yet;"
   :attachments [{:title "Tip: use `/veye connect YOURAPIKEY`"
                  :text "This command will temporary save your VersionEye key"}]})

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

;;TODO: refactor into veyeslack.dispatchers.command
(defmulti cmd-dispatcher (fn [cmd-dt] (-> cmd-dt :text split-args first keyword)))

(defmethod cmd-dispatcher :connect [cmd-dt]
  (let [[_ the-api-key] (split-args (:text cmd-dt))]
    (if (empty? the-api-key)
      {:response_type "ephemeral"
       :text "You forgot API key"}
      (do
        (api/set-user-key! (:team_id cmd-dt) (:user_id cmd-dt) the-api-key)
        {:response_type "ephemeral"
         :text "Your API key is now memorized for 12hours."}))))

(defmethod cmd-dispatcher :search [cmd-dt]
  (let [api-key (api/get-user-key (:team_id cmd-dt) (:user_id cmd-dt))
        query-term (-> cmd-dt :text split-args rest join-tokens)]
    (packages-cmd/search api-key
                         query-term
                         {:n 10}
                         #(packages-fmt/->search-success % false query-term)
                         #(packages-fmt/->search-failure % false query-term))))

(defmethod cmd-dispatcher :list [cmd-dt]
  (if-let [api-key (api/get-user-key (:team_id cmd-dt) (:user_id cmd-dt))]
    (let [[_ org-name team-name _] (split-args (:text str))
          qparams {:org org-name :team team-name}]
      (projects-cmd/list-n api-key
                           qparams               
                           #(projects-fmt/->list-success % qparams)
                           #(projects-fmt/->list-error % qparams)))
    ;;when user had no api-key
    not-authorized-response))

(defn to-project-details
  [project-dt]
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
    {:response_type "ephemeral"
     :text (str "Project details for: " (:id project-dt))
     :attachments (->> (:dependencies project-dt)
                    (filter #(true? (:outdated %)))
                    (map #(to-dep %))
                    doall)}))

(defmethod cmd-dispatcher :project [cmd-dt]
  (if-let [api-key (api/get-user-key (:team_id cmd-dt) (:user_id cmd-dt))]
    (let [[_ project-id] (split-args (:text cmd-dt))]
      (if (empty? project-id)
        {:response_type "ephemeral"
         :text "You missed to send a project-id.\n hint: /veye project PROJECT_ID"}
        (let [response-p (md/deferred)]
          (md/on-realized
            (md/future (api/project-details api-key project-id))
            (fn [res] (md/success! response-p (-> res :body to-project-details)))
            (fn [res] (md/success! response-p
                                   {:text "Failed to fetch a project details"
                                    :color "danger"})))
          response-p)))
    ;;when user has no active session
    not-authorized-response))

;;delicate this task to help-dispatcher
(defmethod cmd-dispatcher :help [cmd-dt]
  {:response_type "ephemeral"
   :text "VersionEye commands:"
   :attachments [{:text "/veye search clojure - search a package"}
                 {:text "/veye list <ORGA_NAME> <TEAM_NAME> - list your projects"}
                 {:text "/veye project <PROJECT_ID> - show outdated project dependencies"}
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

