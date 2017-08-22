(ns dynamic-ranking.db
  (:require [re-frame.core :refer [dispatch]]))

(def default-db
  {:page              :home
   :data-type         :pe
   :time-interval-id  0
   :timer-id          (js/setInterval #(dispatch [:inc-time]) 2000)
   :time              0
   :data              []
   :current-date      ""
   :current-rank      []
   :secucodes         []
   :data-length       10
   :stocknames        {}
   :current-top       "000002.SZ"
   :top-stockname     "万  科Ａ"
   :first-holder-days 1})
