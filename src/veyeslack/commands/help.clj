(ns veyeslack.commands.help)

(def commands
  {:connect {:text "saves or updates your VersionEye API token"
             :examples ["/veye connect 123456789"]}
   :search {:text "looks for a package with name `clojure`"
            :examples ["/veye search rails"]}
   :list {:text (str "shows status of dependencies in your project" 
                     " possible to filter by organization and team")
          :examples ["/veye list"
                     "/veye list versionEye"
                     "/veye list versionEye team1"]}
   :project {:text "shows a list of outdated project dependencies"
             :examples ["/veye project 52126476632bac1ae3007c88"]}
   :cve {:text "shows a list of CVE alerts for the language/platform. \n
               supported languages: ruby, nodejs, php, java, python, clojure."
         :examples ["/veye cve ruby"
                    "/veye cve nodejs 2"
                    "/veye cve php"]}
   :notifications {:text "shows list of packages followed by you \n
                         and for which VersionEye has detected new release today."
                   :examples ["/veye notifications"]}
   :help {:text "shows short list of commands or detailed help for the command"
          :examples ["/veye help"
                     "/veye help connect"]}
   :info {:text "displays VeyeSlack version and contact information"
          :examples ["/veye info"]}})

(defn get-details
  [the-command]
  (if (empty? the-command)
    commands
    (select-keys commands [(keyword the-command)])))

