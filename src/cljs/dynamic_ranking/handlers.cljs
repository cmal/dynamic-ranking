(ns dynamic-ranking.handlers
  (:require [dynamic-ranking.db :as db]
            [re-frame.core :refer [dispatch reg-event-db]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

(reg-event-db
 :set-pe
 (fn [db [_ pe]]
   (assoc db :pe pe)))

(reg-event-db
 :set-secucodes
 (fn [db [_ secucodes]]
   (println "set-secucodes" (count secucodes))
   (assoc db :secucodes secucodes)))

(reg-event-db
 :set-time
 (fn [db [_ time]]
   (assoc db
          :time time
          :rank (shuffle (range 5)))))
