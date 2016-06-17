(ns veyeslack.formatters.packages
  (:require [veyeslack.formatters.helpers :refer [display-type to-safe-key to-veye-url]]))

(defn ->search-success
  [api-res public? query-term]
  (letfn [(to-item-list [items]
            (for [item items]
              {:title (str (:name item) ", " (:version item))
               :title_link (to-veye-url (:language item)
                                        (to-safe-key (:prod_key item))
                                        (:version item))
               :text (str (:language item) ", " (:prod_key item))}))]
    
    {:response_type (display-type public?) 
     :text (str "Search results for `" query-term "`")
     :attachments (-> api-res :body :results (#(take 10 %)) to-item-list)}))

(defn ->search-failure
  [api-res public? query-term]
  {:response_type (display-type public?)
   :text (str "Failed to execute search query for " query-term)
   :attachments [{:title "Response from VersionEye"
                  :text (str api-res)}]})


