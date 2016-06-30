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

(defn ->cve-success
  [api-res public? {:keys [lang page] :or {page 1}}]
  (letfn [(to-cve-list [cves]
            (for [cve cves]
              {:title (str (:prod_key cve) " - " (:affected_versions_string cve))
               :title_link (to-veye-url (:language cve)
                                        (to-safe-key (:prod_key cve))
                                        (first (:affected_versions cve)))
               :text (str 
                       (when (:published_date cve)
                         (str "Published: " (:published_date cve) "\n"))
                       (:summary cve))
               :fields [
                  {:title "Affected versions"
                   :value (:affected_versions_string cve)
                   :short true}
                  {:title "Patched versions"
                   :value (:patched_versions_string cve)
                   :short true}
                  {:title "Framework"
                   :value (:framework cve)
                   :short true}
                  {:title "Platform"
                   :value (:platform cve)
                   :short true}
                  {:title "Links"
                   :value (apply str
                            (for [[title url] (:links cve)]
                              (str "<" url "|" (:name title) ">\n")))
                   :short true}
                  {:title "CVE id"
                   :value (:cve cve)
                   :short true}
                ]}))]
    {:response_type (display-type public?)
     :text (str "List of CVEs for " lang ", page." page)
     :attachments (-> api-res :body :results to-cve-list)}))

(defn ->cve-failure
  [api-res public? {:keys [lang page] :or {page 1}}]
  {:response_type (display-type public?)
   :text (str "Found no CVE alerts for " lang " on page." page)})
