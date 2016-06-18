(ns veyeslack.commands.help)

(def commands
  {:connect {:text "saves or updates your VersionEye API token"
             :examples ["/veye connect 123456789"]}
   :search {:text "find a package with name `clojure`"
            :examples ["/veye search rails"]}
   :list {:text "list your project, possible to filter by organization and team"
          :examples ["/veye list"
                     "/veye list versionEye"
                     "/veye list versionEye team1"]}
   :project {:text "shows a list of outdated project dependencies"
             :examples ["/veye project 52126476632bac1ae3007c88"]}
   :help {:text "shows short list of commands or detailed help for the command"
          :examples ["/veye help"
                     "/veye help connect"]}})

(defn get-details
  [the-command]
  (if (empty? the-command)
    commands
    (select-keys commands [(keyword the-command)])))

