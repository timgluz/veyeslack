(ns veyeslack.test_helpers)

(defmacro with-component-server
  [{:keys [system sleep] :or {sleep 50}} & body]
  "Evaluates code in context of initialized system and components"
  `(let [the-system# ~system]
     (try
       ~@body
       (finally
         (.stop (:app the-system#))
         (Thread/sleep ~sleep)))))
