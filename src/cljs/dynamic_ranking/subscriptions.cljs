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
 :pe
 (fn [db _]
   (:pe db)))

(reg-sub
 :secucodes
 (fn [db _]
   (:secucodes db)))

(reg-sub
 :time
 (fn [db _]
   (:time db)))

(reg-sub
 :rank
 (fn [db _]
   (:rank db)))

(reg-sub
 :current-date
 (fn [db _]
   (:current-date db)))

(reg-sub
 :current-pe-rank
 (fn [db _]
   (:current-pe-rank db)))

(reg-sub
 :data-length
 (fn [db _]
   (:data-length db)))

(reg-sub
 :current-top
 (fn [db _]
   (:current-top db)))

(reg-sub
 :top-stockname
 (fn [db _]
   (:top-stockname db)))
