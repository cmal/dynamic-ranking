(ns dynamic-ranking.db
  )

(defonce init-time
  (int (/ (int (js/Date.)) 1000)))

(def default-db
  {:page      :home
   :time      init-time
   :rank      (range 5)
   :pe        []
   :secucodes []})
