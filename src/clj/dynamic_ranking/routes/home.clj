(ns dynamic-ranking.routes.home
  (:require [dynamic-ranking.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:use [ring.util.json-response]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8")))
  (GET "/pe" []
       (-> (json-response (-> "docs/pe.txt" io/resource slurp edn/read-string))))
  (GET "/stocknames" []
       (-> (response/ok (-> "docs/stock-name.txt" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8"))))

