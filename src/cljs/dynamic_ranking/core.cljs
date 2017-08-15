(ns dynamic-ranking.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [dynamic-ranking.ajax :refer [load-interceptors!]]
            [dynamic-ranking.handlers]
            [dynamic-ranking.subscriptions]
            )
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  (let [selected-page (rf/subscribe [:page])]
    [:li.nav-item
     {:class (when (= page @selected-page) "active")}
     [:a.nav-link
      {:href uri
       :on-click #(reset! collapsed? true)} title]]))

(defn navbar []
  (r/with-let [collapsed? (r/atom true)]
    [:nav.navbar.navbar-dark.bg-primary
     [:button.navbar-toggler.hidden-sm-up
      {:on-click #(swap! collapsed? not)} "☰"]
     [:div.collapse.navbar-toggleable-xs
      (when-not @collapsed? {:class "in"})
      [:a.navbar-brand {:href "#/"} "dynamic-ranking"]
      [:ul.nav.navbar-nav
       [nav-link "#/" "Home" :home collapsed?]
       [nav-link "#/about" "About" :about collapsed?]
       [nav-link "#/chart" "Chart" :chart collapsed?]]]]))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])

(defn home-page []
  [:div.container
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(defn div-rec-component [width top a]
  (r/create-class
   {:display-name (str "div-rec-component" @a)
    :component-did-update
    (fn []
      (println @a "==>" width top " component did update"))
    :component-did-mount
    (fn []
      (println @a "==>" width top " component did mount"))
    :component-will-unmount
    (fn []
      (println @a "==>" width top " component will unmount"))
    :reagent-render
    (fn [width top a]
      (println @a "==>" width top " component render")
      [:div.canvas-rect
       {:style {:width (str width "px")
                :top   (str top "px")}}])}))


(defn dynamic-rank []
  (let [time (rf/subscribe [:time])
        rank (rf/subscribe [:rank])]
    (fn []
      [:div.canvas-inner
       ^{:key (str "dynr-" 0)}
       [div-rec-component (* 0 30) (* 20 (inc (nth @rank 0))) time]
       ^{:key (str "dynr-" 1)}
       [div-rec-component (* 1 30) (* 20 (inc (nth @rank 1))) time]
       ^{:key (str "dynr-" 2)}
       [div-rec-component (* 2 30) (* 20 (inc (nth @rank 2))) time]
       ^{:key (str "dynr-" 3)}
       [div-rec-component (* 3 30) (* 20 (inc (nth @rank 3))) time]
       ^{:key (str "dynr-" 4)}
       [div-rec-component (* 4 30) (* 20 (inc (nth @rank 4))) time]
       ])))

(defn chart []
  (let [time (rf/subscribe [:time])]
    [:div
     [:div @time]
     [:div [dynamic-rank]]]))

(defn chart-page []
  [:div.container
   [:div.chart
    [chart]]])

(def pages
  {:home  #'home-page
   :about #'about-page
   :chart #'chart-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))

(secretary/defroute "/about" []
  (rf/dispatch [:set-active-page :about]))

(secretary/defroute "/chart" []
  (rf/dispatch [:set-active-page :chart]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))

;; -----
;; initialize data
(defonce time-updater
  (js/setInterval
   #(rf/dispatch [:set-time (int (/ (int (js/Date.)) 1000))]) 1000))
