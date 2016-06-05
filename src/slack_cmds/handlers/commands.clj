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

(defn to-veye-url [item]
  (str "https://www.versioneye.com/"
       (:language item) "/"
       (to-safe-key (:prod_key item)) "/"
       (:version item)))

;;TODO: refactor into slack-cmds.dispatchers.command
(defmulti cmd-dispatcher
  (fn [cmd-dt]
    (-> cmd-dt :text str (string/split #"\s+") first keyword)))

(defmethod cmd-dispatcher :search [cmd-dt]
  (let [query-term (-> cmd-dt :text str (string/split #"\s+") vec rest join-tokens)
        response-p (md/deferred)
        to-item-list (fn [items]
                       (for [item items]
                         {:title (str (:name item) ", " (:version item))
                          :title_link (to-veye-url item)
                          :text (str (:language item) ", " (:prod_key item))}))
        on-success (fn [res]
                    (md/success! response-p
                      {:response_type "ephemeral"
                       :text (str "Search results for `" query-term "`")
                       :attachments (-> res :body :results (#(take 10 %)) to-item-list)}))
        on-error (fn [err]
                   (md/success! response-p
                     {:response_type "ephemeral"
                      :text (str "Failed to execute search query for " query-term)}))]
    (md/on-realized (md/future (api/search query-term {:n 10}))
                    on-success
                    on-error)
    response-p))

(defmethod cmd-dispatcher :connect [cmd-dt]
  {:response_type "ephemeral"
   :text "Not implemented"})

;;delicate this task to help-dispatcher
(defmethod cmd-dispatcher :help [cmd-dt]
  {:response_type "ephemeral"
   :text "VersionEye commands:"
   :attachments [{:text "/veye clojure - search a package"}
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


