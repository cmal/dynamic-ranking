(ns dynamic-ranking.routes.home
  (:require [dynamic-ranking.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:use [ring.util.json-response]))

(defn home-page []
  (layout/render "home.html"))

(defn send-file [filename]
  (-> (response/ok (-> filename io/resource slurp))
      (response/header "Content-Type" "text/plain; charset=utf-8")))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8")))
  (GET "/pe" [] (send-file "docs/pe.txt"))
  (GET "/lowest-pe" [] (send-file "docs/lowest-pe.txt"))
  (GET "/mv" [] (send-file "docs/mv.txt"))
  (GET "/lowest-mv" [] (send-file "docs/lowest-mv.txt"))
  (GET "/stocknames" [] (send-file "docs/stock-name.txt")))

