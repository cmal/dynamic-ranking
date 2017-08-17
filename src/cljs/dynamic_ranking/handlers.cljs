(ns dynamic-ranking.handlers
  (:require [dynamic-ranking.db :as db]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]
            [clojure.string :as str]
))

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

(defn change-time
  [db time]
  (let [pe      (:pe db)
        index   (mod time (:data-length db))
        rec     (nth pe (if (neg? index) 0 index))
        pe-rank (second rec)]
    (assoc db
           :time time
           :current-date (first rec)
           :current-pe-rank (vec pe-rank)
           :current-top (ffirst pe-rank)
           )))

(reg-event-fx
 :set-time
 (fn [{:keys [db]} [_ time]]
   {:db (if (empty? (:pe db))
          (assoc db :time time)
          (-> (change-time db time)
              (assoc :first-holder-days 1)))
    :dispatch [:update-top-stockname]}))

(defn update-holder-days
  [db old-top]
  (let [current-top (:current-top db)]
    (if (= current-top old-top)
      (update db :first-holder-days inc)
      (assoc db :first-holder-days 1))))

(reg-event-fx
 :inc-time
 (fn [{:keys [db]} _]
   (let [old-top (:current-top db)]
     {:db       (let [time (inc (:time db))]
                  (if (empty? (:pe db))
                    (assoc db :time time)
                    (-> (change-time db time)
                        (update-holder-days old-top))))
      :dispatch [:update-top-stockname]})))

(reg-event-db
 :set-stocknames
 (fn [db [_ stocknames]]
   (assoc db :stocknames stocknames)))

(reg-event-db
 :update-top-stockname
 (fn [db _]
   (let [{:keys [stocknames current-top]} db]
     (assoc db :top-stockname
            (get stocknames (str/join (take 6 current-top)))))))
