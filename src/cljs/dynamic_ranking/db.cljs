(ns dynamic-ranking.db
  )

(defonce init-time
  (int (/ (int (js/Date.)) 1000)))

(defonce ranks (repeatedly 1000 (partial shuffle (range 5))))

(def default-db
  {:page :home
   :time init-time
   :rank (first ranks)})
