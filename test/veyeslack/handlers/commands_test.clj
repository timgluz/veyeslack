(ns veyeslack.handlers.commands-test
  (:require [clojure.test :refer :all]
            [clj-http.fake :refer [with-global-fake-routes]]
            [veyeslack.handlers.commands :as cmd-handler]))


(deftest split-args-test
  (testing "splits arguments into tokens"
    (is (= ["cmd" "arg"] (cmd-handler/split-args "cmd arg")))
    (is (= ["cmd" "arg"] (cmd-handler/split-args " cmd arg  ")))))
