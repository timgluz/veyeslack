(ns veyeslack.handlers.commands
  (:require [catacumba.core :as ct]
            [manifold.deferred :as md]
            [clojure.string :as string]
            [veyeslack.commands.packages :as packages-cmd]
            [veyeslack.formatters.packages :as packages-fmt]
            [veyeslack.commands.projects :as projects-cmd]
            [veyeslack.formatters.projects :as projects-fmt]
            [veyeslack.commands.help :as help-cmd]
            [veyeslack.formatters.help :as help-fmt]
            [veyeslack.commands.user :as user-cmd]
            [veyeslack.formatters.user :as user-fmt]
            [veyeslack.models.api-key :as api-key-mdl]
            [veyeslack.version]))

(def not-authorized-response
  {:response_type "ephemeral"
   :text "You havent save your API key yet;"
   :attachments [{:title "Tip: use `/veye connect YOURAPIKEY`"
                  :text "This command will temporary save your VersionEye key"}]})

(defn join-tokens
  [tkns]
  (apply str (interpose " " tkns)))

(defn split-args [task-txt]
  (-> task-txt str (string/split #"\s+") (#(remove empty? %)) doall vec))

(defn get-api-key
  [db-client {:keys [user_id team_id]}]
  (-> db-client
    (api-key-mdl/get-one-by-user-or-team-id user_id team_id)
    :api_key))

(defmulti cmd-dispatcher
  (fn [the-service cmd-dt] (-> cmd-dt :text split-args first keyword)))

(defmethod cmd-dispatcher :connect [the-service cmd-dt]
  (let [[_ the-api-key] (split-args (:text cmd-dt))]
    (if (empty? the-api-key)
      {:response_type "ephemeral"
       :text "You forgot API key"}
      (user-cmd/connect (:db the-service)
                        cmd-dt
                        the-api-key
                        #(user-fmt/->connect-success %)
                        #(user-fmt/->connect-failure %)))))

(defmethod cmd-dispatcher :search [the-service cmd-dt]
  (let [api-key (get-api-key (:db the-service) cmd-dt) 
        query-term (-> cmd-dt :text split-args rest join-tokens)]
    (packages-cmd/search api-key
                         query-term
                         {:n 10}
                         #(packages-fmt/->search-success % false query-term)
                         #(packages-fmt/->search-failure % false query-term))))

(defmethod cmd-dispatcher :list [the-service cmd-dt]
  (if-let [api-key (get-api-key (:db the-service) cmd-dt)]
    (let [[_ org-name team-name _] (split-args (:text str))
          qparams {:org org-name :team team-name}]
      (projects-cmd/list-n api-key
                           qparams 
                           #(projects-fmt/->list-success % qparams)
                           #(projects-fmt/->list-error % qparams)))
    ;;when user had no api-key
    not-authorized-response))

(defmethod cmd-dispatcher :project [the-service cmd-dt]
  (if-let [api-key (get-api-key (:db the-service) cmd-dt)]
    (let [[_ project-id] (split-args (:text cmd-dt))]
      (if (empty? project-id)
        {:response_type "ephemeral"
         :text "You forgot to send a project-id.\n hint: /veye project PROJECT_ID"}
        (projects-cmd/project api-key
                              project-id
                              #(projects-fmt/->project-success % {:id project-id})
                              #(projects-fmt/->project-error % {:id project-id}))))
    ;;when user has no active session
    not-authorized-response))


(defmethod cmd-dispatcher :cve [the-service cmd-dt]
  (let [api-key (get-api-key (:db the-service) cmd-dt)
        [_ the-lang the-page] (split-args (:text cmd-dt))
        qparams {:lang the-lang
                 :page (or the-page 1)}]
    (packages-cmd/get-cves
      api-key
      qparams
      #(packages-fmt/->cve-success % false qparams)
      #(packages-fmt/->cve-failure % false qparams))))

(defmethod cmd-dispatcher :notifications [the-service cmd-dt]
  (if-let [api-key (get-api-key (:db the-service) cmd-dt)]
    (user-cmd/notifications
      api-key
      #(user-fmt/->notification-success % false)
      #(user-fmt/->notification-failure % false))
    ;;when user have no authorization token
    not-authorized-response))

(defmethod cmd-dispatcher :help [_ cmd-dt]
  (let [[_ the-command] (split-args (:text cmd-dt))
        commands (help-cmd/get-details the-command)]
    (if (empty? the-command)
      (help-fmt/->full-help commands)
      (help-fmt/->command-help commands the-command))))

(defmethod cmd-dispatcher :info [_ cmd-dt]
  {:response_type "ephemeral"
   :text (str 
           "VersionEye integration for Slack with publicly open source-code;\n"
           "Current release: " (veyeslack.version/as-semver) "\n")
   :attachments [{:text "Contact: info@veyeslack.xyz"}
                 {:text "Source: <https://github.com/timgluz/veyeslack>"}]})

;;TODO: add did-you-mean if edit distance <= 2
(defmethod cmd-dispatcher :default [_ cmd-dt]
  {:response_type "ephemeral"
   :text (str "unsupported cmd `" (:text cmd-dt) "` - more info `/veye help`")})

(def timed-out-response
  {:response_type "ephemeral"
   :text "execution time-out"})

(defn handler
  [context]
  (if-let [cmd-dt (:data context)]
    (let [res (cmd-dispatcher (:app context) cmd-dt)]
      (ct/delegate
        {:status 200
         :body (if (md/deferrable? res)
                @(md/timeout! res 2500 timed-out-response)
                res)}))
    ;;when we didnt get any useful data from user
    (ct/delegate
      {:status 400
       :body {:reason "misformatted Slack command data"}})))

