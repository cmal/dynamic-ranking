(ns dynamic-ranking.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[dynamic-ranking started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[dynamic-ranking has shut down successfully]=-"))
   :middleware identity})
