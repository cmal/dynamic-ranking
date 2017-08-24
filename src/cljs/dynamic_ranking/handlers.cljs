(ns dynamic-ranking.handlers
  (:require [dynamic-ranking.db :refer [default-db time-intervals]]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]
            [clojure.string :as str]
))

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

(reg-event-db
 :set-data
 (fn [db [_ data]]
   (assoc db
          :data data
          :data-length (count data))))

(def v-axes
  (let [lst (map (fn [mul] (vec (map #(* mul %) (range 1 10))))
                 (iterate #(* 10 %) 0.1))]
    {:pe        (vec (apply concat (take 20 (drop 1 lst))))
     :lowest-pe (vec (apply concat (take 6 lst)))
     :mv        (vec (apply concat (take 20 (drop 5 lst)) ))}))

(reg-event-db
 :set-type
 (fn [db [_ type]]
   (assoc db
          :data-type type
          :axes (v-axes type))))

(reg-event-db
 :set-secucodes
 (fn [db [_ secucodes]]
   (assoc db :secucodes secucodes)))

(defn change-time
  [db time]
  (let [data      (:data db)
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
           :x-axis-ratio (/ (* 770 0.01 (- (:chart-max-percent db)
                                           (:chart-min-percent db)))
                            max-val)
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

#_(reg-fx
 :switch-interval
 (let [live-intervals (atom {})] ;; bug: every time figwheel recompiles, the setIntervals in atom will loss
   (when-not (get @live-intervals "switch-timer")
           (js/setInterval
            #(dispatch [:inc-time])
            (get time-intervals 0)));; get initialized
   (fn [{:keys [action id delay event]}]
     (js/clearInterval (get @live-intervals id))
     (swap! live-intervals assoc id (js/setInterval #(dispatch event) delay)))))

#_(reg-event-fx
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

#_(reg-event-db
 :set-x-axis-ratio
 (fn [db [_ val max-val]]
   (assoc db :x-axis-ratio (/ val max-val))))


(reg-fx
 :clear-timer
 (fn [timer-id]
   (js/clearInterval timer-id)))

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
                                     (get time-intervals new-time-interval-id)))}))) ;; not pure
