(ns dynamic-ranking.handlers
  (:require [dynamic-ranking.db :refer [default-db time-intervals]]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]
            [clojure.string :as str]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

(reg-event-fx
 :set-data
 (fn [{:keys [db]} [_ type data]]
   (merge
    {:db (assoc db
                type data
                :data-length (count data))
     }
    (when (= :mv type)
      {:dispatch [:set-type type]}))))

(def v-axes
  (let [lst (map (fn [mul] (vec (map #(* mul %) (range 1 10))))
                 (iterate #(* 10 %) 1))]
    {:pe        (vec (apply concat (take 20 lst)))
     :lowest-pe (vec (apply concat (take 5 lst)))
     :mv        (vec (apply concat (take 20 (drop 4 lst))))}))

(reg-event-db
 :set-type
 (fn [db [_ type]]
   (assoc db
          :data-type type
          :data-length (count (get db type))
          :axes (v-axes type)
          :secucodes (->> (get db type)
                          (map second)
                          (mapcat (fn [rec] (map first rec)))
                          set
                          vec))))

(defn change-time
  [db time]
  (let [data    (get db (:data-type db))
        index   (mod time (:data-length db))
        rec     (nth data (if (neg? index) 0 index))
        rank (second rec)
        data (map second rank)
        max-val (case (:data-type db)
                  :lowest-pe (max 10 (last data))
                  :pe (max 500 (first data))
                  (first data))
        ]
    (assoc db
           :time time
           :current-date (first rec)
           :current-rank (vec rank)
           :current-top (ffirst rank)
           :x-axis-ratio (/ (* 770
                               0.01
                               (- (:chart-max-percent db)
                                  (:chart-min-percent db)))
                            max-val))))

(reg-event-fx
 :set-time
 (fn [{:keys [db]} [_ time]]
   {:db (if (empty? (get db (:data-type db)))
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
                  (if (empty? (get db (:data-type db)))
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

(defonce live-intervals (atom {}))

;; get initialized
(when-not (get @live-intervals "switch-timer")
    (swap! live-intervals assoc
           "switch-timer" (js/setInterval
                           #(dispatch [:inc-time])
                           (get time-intervals 0))))
(reg-fx
 :switch-interval
 (fn [{:keys [action id delay event]}]
   (let [timer-id (js/setInterval #(dispatch event) delay)]
     (js/clearInterval (get @live-intervals id))
     (swap! live-intervals assoc id timer-id))))

(reg-event-fx
 :switch-timer
 (fn [{:keys [db]} _]
   (let [{:keys [time-interval-id]} db
         new-time-interval-id (mod
                               (inc time-interval-id)
                               (count time-intervals))]
     {:switch-interval
      {;; NOTE: the `:id` is not what is returned by `setInterval`  !!!
       :id    "switch-timer"                      ;; it is MY id for this interval
       :delay (get time-intervals new-time-interval-id) ;; how many milli secs
       :event [:inc-time]                               ;; what event to dispatch
       }
      :db (assoc db
                 :time-interval-id new-time-interval-id)})))

(reg-event-db
 :switch-axes
 (fn [db [_ show-axes]]
   (update db :show-axes not)))
