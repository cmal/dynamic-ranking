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
      {:href     uri
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

(defn get-width-by-val [max-val val index]
  (let [max-width 80
        min-width 30
        interval  (- max-width min-width)]
    (str (min max-width (+ min-width (* interval (/ val max-val)))) "%")))

(defn get-tiny-logo-url [secucode]
  (str "http://dev.joudou.com/static/enterprise_logos/logos/" (str/join (concat (take 1 secucode) '(\/) (take 6 secucode)))))


(defn large-num-formatter [val]
  (cond
    (< val 1E4)  [:span.val-num (.toFixed val 2)]
    (< val 1E8)  [:span
                  [:span.val-num (.toFixed (/ val 1E4) 2)] [:span.val-chn "万"]]
    (< val 1E12) [:span
                  [:span.val-num (.toFixed (/ val 1E8) 2)] [:span.val-chn "亿"]]
    :else        [:span
                  [:span.val-num (.toFixed (/ val 1E12) 2)] [:span.val-chn "万亿"]]))

(defn data-formatter [val data-type]
  (case data-type
    :pe        (if (zero? val) "" (.toFixed (* 1 val) 2))
    :lowest-pe (if (zero? val) "" (.toFixed (* 1 val) 2))
    :mv        (if (zero? val) "" (large-num-formatter val))))

(def bar-height 34)

(defn transition-css
  [transition]
  {:transition         transition
   :-webkit-transition transition
   :-moz-transition    transition
   :-o-transition      transition})

(defn div-rect-component [i code]
  (r/create-class
   {:display-name (str "div-rect-component" i)
    :reagent-render
    (fn [i code]
      (let [rank             (rf/subscribe [:current-rank])
            stocknames       (rf/subscribe [:stocknames])
            data-type        (rf/subscribe [:data-type])
            itv              (rf/subscribe [:interval-sec])
            rank-secu        (vec (map first @rank))
            index            (.indexOf rank-secu code)
            vals             (map second @rank)
            unfmt-val        (if (neg? index) 0 (nth vals index))
            val              (data-formatter unfmt-val @data-type)
            max-val          (case @data-type :lowest-pe (max 30 (last vals)) :pe (max 500 (first vals)) (first vals)) ;; 这招好用
            stockname        (get @stocknames (str/join (take 6 code)))
            top              (if (neg? index) 400 (* bar-height index))
            width            (get-width-by-val max-val unfmt-val index)
            background       (case (first code)
                               \0 "#00B692"
                               \3 "#F79018"
                               \6 "#8536A3")
            transition       (str "top " itv "s ease-out, width " itv "s linear")
            ]
        [:div.rect
         {:style (merge {:top        top
                         :width      width
                         :background background
                         }
                        (transition-css transition))}
         [:span.in-bar
          [:span.name stockname]
          [:span.code code]]
         [:span.out-bar
          [:span.val val]]]))}))

(defn dynamic-rank []
  (let [secucodes (rf/subscribe [:secucodes])]
    [:div.rank
     (doall
      (for [i    (range (count @secucodes))
            :let [code (nth @secucodes i)]]
        ^{:key (str "dyrk-" i)}
        [div-rect-component i code]))]))

(def speed ["1x" "2x" "4x" "10x"])

(defn time-controller []
  (let [id (rf/subscribe [:time-interval-id])]
    [:div.timer-btn {:on-click #(rf/dispatch [:switch-timer])}
     "速度 " (get speed @id) " "]))

(defn main-chart []
  [:div.chart
   [:div [dynamic-rank]]
   [:div.canvas-cover]])

(defn rank-desc []
  [:div.rank-desc
   (for [text ["1st" "2nd" "3rd" "4th" "5th"
               "6th" "7th" "8th" "9th" "10th"]]
     ^{:key (str "rank-desc-item-" text)} [:div.rank-desc-item text])])

(defn progress-bar [total index]
  (let [width 530]
    [:div#progress-bar.progress-bar
     {:style    {:width width}
      :on-click (fn [e]
                  (when-let [node (js/document.getElementById "progress-bar")]
                    (rf/dispatch [:set-time (* total (/ (- (.-clientX e) (.-x (getPageOffset node))) width))])))}
     [:div.progress-past
      {:style {:width (* width (/ index total))}}]]))

(defn v-axes []
  (let [axes (rf/subscribe [:axes])
        rank (rf/subscribe [:current-rank])
        vals (map second @rank)
        itv (rf/subscribe [:interval-sec])
        transition (str "left " itv "s linear")]
    [:div.v-axes
     (for [a @axes
           :let [fst-val (first vals)
                 lst-val (last vals)]]
       ^{:key (str "v-axis-" a)}
       [:div.v-axis
        {:style (merge {:left a
                        }
                       (transition-css transition))}])]))


(defonce month-names
  {"01" "Jan"
   "02" "Feb"
   "03" "Mar"
   "04" "Apr"
   "05" "May"
   "06" "Jun"
   "07" "Jul"
   "08" "Aug"
   "09" "Sep"
   "10" "Oct"
   "11" "Nov"
   "12" "Dec"})

(defn chart-page []
  (let [date              (rf/subscribe [:current-date])
        pe-rank           (rf/subscribe [:current-rank])
        total             (rf/subscribe [:data-length])
        time              (rf/subscribe [:time])
        secucode          (rf/subscribe [:current-top])
        stockname         (rf/subscribe [:top-stockname])
        first-holder-days (rf/subscribe [:first-holder-days])
        data-type         (rf/subscribe [:data-type])
        name (.toUpperCase (name @data-type))]
    [:div.container
     [progress-bar @total (mod @time @total)]
     [:div.top-desc #_(ffirst @pe-rank)
      [:div @secucode]
      [:div @stockname]
      [:div "#1 " name " holder"]
      [:div "for " @first-holder-days " days"]]
     [:div.title "Top " name " of Chinese stock market's history on"]
     (let [[y m d] (str/split @date #"-")]
       [:div.date (month-names m) " " d ", " y])
     [rank-desc]
     [main-chart]
     [time-controller]
     [v-axes]]))

(def pages
  {:home    #'home-page
   #_:about #_#'about-page
   :chart   #'chart-page})

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

(defn data-handler
  [data type]
  (let [d (read-string data)]
    (rf/dispatch [:set-data d])
    (rf/dispatch [:set-secucodes (->> d
                                      (map second)
                                      (mapcat (fn [rec] (map first rec)))
                                      set
                                      vec)])
    (rf/dispatch [:set-type type])))

(defn fetch-pe! []
  (GET "/pe" {:handler #(data-handler % :pe)}))

(defn fetch-lowest-pe! []
  (GET "/lowest-pe" {:handler #(data-handler % :lowest-pe)}))

(defn fetch-mv! []
  (GET "/mv" {:handler #(data-handler % :mv)}))

(defn fetch-stocknames! []
  (GET "/stocknames" {:handler #(rf/dispatch [:set-stocknames (read-string %)])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (fetch-lowest-pe!)
  (fetch-stocknames!)
  (hook-browser-navigation!)
  (mount-components))
