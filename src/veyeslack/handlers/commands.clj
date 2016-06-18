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
            [veyeslack.formatters.projects :as projects-fmt]
            [veyeslack.commands.help :as help-cmd]
            [veyeslack.formatters.help :as help-fmt]))

(def not-authorized-response
  {:response_type "ephemeral"
   :text "You havent save connect your API key yet;"
   :attachments [{:title "Tip: use `/veye connect YOURAPIKEY`"
                  :text "This command will temporary save your VersionEye key"}]})

(defn join-tokens
  [tkns]
  (apply str (interpose " " tkns)))

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

(defmethod cmd-dispatcher :project [cmd-dt]
  (if-let [api-key (api/get-user-key (:team_id cmd-dt) (:user_id cmd-dt))]
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

;;delicate this task to help-dispatcher
(defmethod cmd-dispatcher :help [cmd-dt]
  (let [[_ the-command] (split-args (:text cmd-dt))
        commands (help-cmd/get-details the-command)]
    (if (empty? the-command)
      (help-fmt/->full-help commands)
      (help-fmt/->command-help commands the-command))))

;;TODO: add did-you-mean if edit distance <= 2
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

