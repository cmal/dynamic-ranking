(ns dynamic-ranking.core
  (:require [clojure.string :as str]
            [cljs.reader :refer [read-string]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [goog.style :refer [getPageOffset]]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [dynamic-ranking.ajax :refer [load-interceptors!]]
            [dynamic-ranking.handlers]
            [dynamic-ranking.subscriptions]
            [dynamic-ranking.img :refer [postfix]]
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

(def bar-height 34)

(defn div-rec-component [i time rank]
  (r/create-class
   {:display-name (str "div-rec-component" @time @rank)
    :component-did-update
    (fn []
      #_(println "==> component did update"))
    :component-did-mount
    (fn []
      #_(println "==> component did mount"))
    :component-will-unmount
    (fn []
      #_(println "==> component will unmount"))
    :reagent-render
    (fn [i time rank]
      #_(println "==> component render")
      [:div.canvas-rect
       {:style {:width (str
                        (+ (* (inc i) bar-height) (* 5 (mod @time 10)))
                        "px")
                :top   (str
                        (if (= (nth @rank i) (dec (count @rank)))
                          (* 20 (+ 3 (nth @rank i)))
                          (* 20 (nth @rank i)))
                        "px")
                }}
       i])}))

(defn dynamic-rank []
  (let [time (rf/subscribe [:time])
        rank (rf/subscribe [:rank])]
    [:div.canvas-inner
     (doall
      (for [i (range 5)]
        ^{:key (str "dynr-" i)}
        [div-rec-component i time rank]
        ))]))

(defn chart []
  (let [time (rf/subscribe [:time])]
    [:div
     [:div @time]
     [:div [dynamic-rank]]
     [:div.canvas-cover]]))


(defn get-width-by-pe [maxpe minpe cnt pe]
  (let [max-width 80
        min-width 20
        interval (- max-width min-width)]
    (str (min 80 (+ min-width (* interval (/ (- pe minpe) maxpe)))) "%")))

(defn get-tiny-logo-url [secucode]
  (str "http://dev.joudou.com/static/enterprise_logos/logos/" (str/join (concat (take 1 secucode) '(\/) (take 6 secucode)))))

(defn div-pe-component [i secucodes]
  (r/create-class
   {:display-name (str "div-pe-component" i)
    :reagent-render
    (fn [i secucodes]
      (let [pe-rank (rf/subscribe [:current-pe-rank])
            rank-secu (vec (map first @pe-rank))
            code (nth @secucodes i)
            index (.indexOf rank-secu code)
            pes (map second @pe-rank)
            pe (if (neg? index) 0 (nth pes index))
            cnt (count pes)
            overflow? (> (- (first pes) (second pes)) (second pes) 1000)
            maxpe (if overflow? (second pes) (first pes))
            minpe (last pes)]
        [:div.pe-rect
         {:style {:position "absolute"
                  :top (if (neg? index)
                         "400px"
                         (str (* bar-height index) "px"))
                  :width (get-width-by-pe maxpe minpe cnt pe)
                  :background (case (first code)
                                \0 "green"
                                \3 "orange"
                                \6 "purple")}}
         [:span.in-bar
          #_(when-not (neg? index)
            #_[:img.logo {:src (str (get-tiny-logo-url code)
                                  "." (get postfix (str/join (take 6 code))))}])
          [:span.code code (when (and overflow? (zero? index)) "==>>")]]
         [:span.out-bar
          [:span.pe (if (zero? pe) "" (.toFixed pe 2))]]]))}))

(defn dynamic-pe-rank []
  (let [secucodes (rf/subscribe [:secucodes])]
    [:div.pe-rank
     (doall
      (for [i (range (count @secucodes))]
        ^{:key (str "dype-" i)}
        [div-pe-component i secucodes]))]))

(defn main-chart []
  [:div.chart
   [:div [dynamic-pe-rank]]
   [:div.canvas-cover]])

(defn rank-desc []
  [:div.rank-desc
   [:div.rank-desc-item "1st"]
   [:div.rank-desc-item "2nd"]
   [:div.rank-desc-item "3rd"]
   [:div.rank-desc-item "4th"]
   [:div.rank-desc-item "5th"]
   [:div.rank-desc-item "6th"]
   [:div.rank-desc-item "7th"]
   [:div.rank-desc-item "8th"]
   [:div.rank-desc-item "9th"]
   [:div.rank-desc-item "10th"]])

(defn progress-bar [total index]
  (let [width 530]
    [:div#progress-bar.progress-bar
     {:style {:width (str width "px")}
      :on-click (fn [e]
                  (when-let [node (js/document.getElementById "progress-bar")]
                    (rf/dispatch [:set-time (* total (/ (- (.-clientX e) (.-x (getPageOffset node))) width))])))}
     [:div.progress-past
      {:style {:width (str (* width (/ index total)) "px")}}]]))

(defn chart-page []
  (let [date (rf/subscribe [:current-date])
        pe-rank (rf/subscribe [:current-pe-rank])
        total (rf/subscribe [:data-length])
        time (rf/subscribe [:time])
        secucode (rf/subscribe [:current-top])
        stockname (rf/subscribe [:top-stockname])]
    [:div.container
     [progress-bar @total (- (mod @time @total) 5)]
     [:div.top-desc #_(ffirst @pe-rank)
      [:div @secucode]
      [:div @stockname]]
     [:div.title "Top PE of Chinese stock market's history on"]
     [:div.date @date]
     [rank-desc]
     [main-chart]]))

(def pages
  {:home  #'home-page
   #_:about #_#'about-page
   :chart #'chart-page})

(defn page []
  [:div
   #_[navbar]
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

(defn fetch-pe! []
  (GET "/pe" {:handler #(do
                         (rf/dispatch [:set-pe %])
                         (rf/dispatch [:set-secucodes
                                       (->> %
                                            (map second)
                                            (mapcat (fn [rec] (map first rec)))
                                            set
                                            vec)]))}))

(defn fetch-stocknames! []
  (GET "/stocknames" {:handler #(rf/dispatch [:set-stocknames (read-string %)])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (fetch-pe!)
  (fetch-stocknames!)
  (hook-browser-navigation!)
  (mount-components))

;; -----
;; initialize timer
(defonce time-updater
  (js/setInterval
   #(rf/dispatch [:inc-time]) 2000))
;; -----
