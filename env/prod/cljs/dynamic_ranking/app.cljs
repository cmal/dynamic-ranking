(ns dynamic-ranking.app
  (:require [dynamic-ranking.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
