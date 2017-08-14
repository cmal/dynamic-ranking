(ns user
  (:require [mount.core :as mount]
            [dynamic-ranking.figwheel :refer [start-fw stop-fw cljs]]
            dynamic-ranking.core))

(defn start []
  (mount/start-without #'dynamic-ranking.core/repl-server))

(defn stop []
  (mount/stop-except #'dynamic-ranking.core/repl-server))

(defn restart []
  (stop)
  (start))


