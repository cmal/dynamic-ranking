(ns dynamic-ranking.handlers
  (:require [dynamic-ranking.db :as db]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]
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
 :set-data
 (fn [db [_ data]]
   (assoc db
          :data data
          :data-length (count data))))

(reg-event-db
 :set-type
 (fn [db [_ type]]
   (assoc db
          :data-type type)))

(reg-event-db
 :set-secucodes
 (fn [db [_ secucodes]]
   (assoc db :secucodes secucodes)))

(defn change-time
  [db time]
  (let [data      (:data db)
        index   (mod time (:data-length db))
        rec     (nth data (if (neg? index) 0 index))
        rank (second rec)]
    (assoc db
           :time time
           :current-date (first rec)
           :current-rank (vec rank)
           :current-top (ffirst rank)
           )))

(reg-event-fx
 :set-time
 (fn [{:keys [db]} [_ time]]
   {:db (if (empty? (:data db))
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
                  (if (empty? (:data db))
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

(def time-intervals [2000 1000 500 200])

(reg-fx
 :clear-timer
 (fn [timer]
   (js/clearInterval timer)))

(reg-event-fx
 :switch-timer
 (fn [{:keys [db]} _]
   (let [{:keys [timer-id time-interval-id]} db
         new-time-interval-id (mod
                               (inc time-interval-id)
                               (count time-intervals))]
     {:clear-timer timer-id
      :db          (assoc db
                          :time-interval-id new-time-interval-id
                          :timer-id (js/setInterval
                                     #(dispatch [:inc-time])
                                     (get time-intervals new-time-interval-id)))}))) ;; ??? not pure
