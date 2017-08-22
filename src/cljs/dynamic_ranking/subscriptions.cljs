(ns dynamic-ranking.subscriptions
  (:require [re-frame.core :refer [reg-sub]]
            [dynamic-ranking.handlers :refer [time-intervals]]))

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(reg-sub
 :data
 (fn [db _]
   (:data db)))

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
 :current-rank
 (fn [db _]
   (:current-rank db)))

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

(reg-sub
 :first-holder-days
 (fn [db _]
   (:first-holder-days db)))

(reg-sub
 :stocknames
 (fn [db _]
   (:stocknames db)))

(reg-sub
 :data-type
 (fn [db _]
   (:data-type db)))

(reg-sub
 :time-interval-id
 (fn [db _]
   (:time-interval-id db)))
