(ns dynamic-ranking.db
  (:require [re-frame.core :refer [dispatch]]))

(def time-intervals [2000 1000 500 200])

(def default-db
  {:page              :home
   :data-type         :pe
   :axes              nil
   :time-interval-id  0
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
