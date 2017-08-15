(ns dynamic-ranking.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(reg-sub
 :time
 (fn [db _]
   (:time db)))

(reg-sub
 :rank
 (fn [db _]
   (:rank db)))
