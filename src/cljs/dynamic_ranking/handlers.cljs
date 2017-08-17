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
;;   (println "max pe" (apply max (map second (map #(get % 10) (map second pe)))))
   (assoc db
          :pe pe
          :data-length (count pe))))
;; max pe of order:
;; 1 3058599.43
;; 2 650522.54
;; 3 34359.54
;; 4 30732.17
;; 5 22677.57
;; 6 15360.9
;; 7 14463.39
;; 8 12153.43
;; 9 10291.51

(reg-event-db
 :set-secucodes
 (fn [db [_ secucodes]]
;;   (println "set-secucodes" (count secucodes))
   (assoc db :secucodes secucodes)))

(reg-event-db
 :set-time
 (fn [db [_ time]]
   (println "set time: " time)
   (if (empty? (:pe db))
     (assoc db :time time)
     (let [pe    (:pe db)
           index (- (mod time (:data-length db)) 5)
           rec   (nth pe (if (neg? index) 0 index))]
       (assoc db
              :time time
              :current-date (first rec)
              :current-pe-rank (vec (second rec)))))))

(reg-event-db
 :inc-time
 (fn [db [_]]
   (let [time (inc (:time db))]
     (println "inc time")
     (if (empty? (:pe db))
       (assoc db :time time))
     (let [pe    (:pe db)
           index (- (mod time (:data-length db)) 5)
           rec   (nth pe (if (neg? index) 0 index))]
       (assoc db
              :time time
              :current-date (first rec)
              :current-pe-rank (vec (second rec)))))))
