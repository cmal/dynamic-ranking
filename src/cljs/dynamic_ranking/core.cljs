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

#_(defn get-width-by-val [max-val val index]
  (let [max-width 80
        min-width 30
        interval  (- max-width min-width)]
    (str (min max-width (+ min-width (* interval (/ val max-val)))) "%")))

(defn get-width-by-val [max-width min-width max-val val]
  (let [interval (- max-width min-width)]
    (str (min max-width (+ min-width (* interval (/ val max-val)))) "%")))

(defn get-tiny-logo-url [secucode]
  (let [code (str/join (take 6 secucode))]
    (str "http://dev.joudou.com/static/enterprise_logos/logos/"
         (first secucode) \/ code \. (get postfix code))))

(defn large-num [val digits]
  (cond
    (< val 1E4)  [:span.out-bar [:span.val-num (.toFixed val digits)]]
    (< val 1E8)  [:span.out-bar
                  [:span.val-num (.toFixed (/ val 1E4) digits)] [:span.val-chn "万"]]
    (< val 1E12) [:span.out-bar
                  [:span.val-num (.toFixed (/ val 1E8) digits)] [:span.val-chn "亿"]]
    :else        [:span.out-bar
                  [:span.val-num (.toFixed (/ val 1E12) digits)] [:span.val-chn "万亿"]]))

(defn large-num-fmtr [val digits]
  (cond
    (< val 1E4) (.toFixed val digits)
    (< val 1E8) (str (.toFixed (/ val 1E4) digits) "万")
    (< val 1E12) (str (.toFixed (/ val 1E8) digits) "亿")
    :else (str (.toFixed (/ val 1E12) digits) "万亿")))

(defn data-fmtr [f val data-type digits]
  (case data-type
    :pe        (when-not (zero? val) (f (* 1 val) digits))
    :lowest-pe (when-not (zero? val) (f (* 1 val) digits))
    :mv        (when-not (zero? val) (f (* 1 val) digits))
    :lowest-mv (when-not (zero? val) (f (* 1 val) digits))))

(def bar-height 28)

(defn transition-css
  [transition]
  {:transition         transition
   :-webkit-transition transition
   :-moz-transition    transition
   :-o-transition      transition})

(defn chart-rect [i code]
  (r/create-class
   {:display-name (str "chart-rect" i)
    :reagent-render
    (fn [i code]
      (let [rank             (rf/subscribe [:current-rank])
            stocknames       (rf/subscribe [:stocknames])
            data-type        (rf/subscribe [:data-type])
            itv              (rf/subscribe [:interval-sec])
            max-width (rf/subscribe [:chart-max-percent])
            min-width (rf/subscribe [:chart-min-percent])
            max-val          (rf/subscribe [:max-val])
            rank-secu        (vec (map first @rank))
            index            (.indexOf rank-secu code)
            vals             (map second @rank)
            unfmt-val        (if (neg? index) @max-val (nth vals index))
            stockname        (get @stocknames (str/join (take 6 code)))
            top              (if (neg? index) 320 (* bar-height index))
            width            (get-width-by-val @max-width @min-width @max-val unfmt-val)
            background       (case (first code)
                               \0 "#00B692"
                               \3 "#F79018"
                               \6 "#8536A3")
            transition       (str "top " @itv "s ease-out, width " @itv "s linear")]
        [:div.rect-wrapper
         [:div.rect
          {:style (merge {:top        top
                          :width      width
                          :background background
                          }
                         (transition-css transition))}
          [:span.in-bar
           [:span.name stockname]
           [:span.code code]]
          (data-fmtr large-num unfmt-val @data-type 2)]]))}))

(defn dynamic-rank []
  (let [secucodes (rf/subscribe [:secucodes])]
    [:div.rank
     (doall
      (for [i    (range (count @secucodes))
            :let [code (nth @secucodes i)]]
        ^{:key (str "dyrk-" i)}
        [chart-rect i code]))]))

(defn next-type [item]
  (let [coll [:pe :lowest-pe :mv :lowest-mv]]
    (get coll
     (mod (inc (.indexOf coll item))
          (count coll)))))

(defn data-type-controller []
  (let [data-type (rf/subscribe [:data-type])]
    [:div.data-type-btn
     {:on-click (fn [e]
                  (rf/dispatch [:set-type (next-type @data-type)]))}
     "切换类型"]))

(defn axes-controller []
  (let [show-axes? (rf/subscribe [:show-axes])]
    [:div.axes-btn {:on-click #(rf/dispatch [:switch-axes])}
     (if @show-axes? "隐藏轴" "显示轴")]))

(def speed ["1x" "2x" "4x" "10x" "暂停"])

(defn time-controller []
  (let [id (rf/subscribe [:time-interval-id])]
    [:div.timer-btn {:on-click #(rf/dispatch [:switch-timer])}
     #_"速度 " (get speed @id) " "]))

(defn main-chart []
  [:div.chart
   [dynamic-rank]
   [:div.canvas-cover]])

(defn rank-desc []
  [:div.rank-desc
   (for [text ["1st" "2nd" "3rd" "4th" "5th"
               "6th" "7th" "8th" "9th" "10th"]]
     ^{:key (str "rank-desc-item-" text)} [:div.rank-desc-item text])])

(defn progress-bar []
  (let [width 424
        total (rf/subscribe [:data-length])
        time (rf/subscribe [:time])
        itv (rf/subscribe [:interval-sec])]
    [:div#progress-bar.progress-bar
     {:style    {:width width}
      :on-click (fn [e]
                  (when-let [node (js/document.getElementById "progress-bar")]
                    (rf/dispatch [:set-time (* @total
                                               (/ (- (.-clientX e)
                                                     (.-x (getPageOffset node))
                                                     1)
                                                  width))])))}
     [:div.progress-past
      {:style {:width (* width (/ (mod @time @total) @total))
               :animation (str "ants " (* 100 @itv) "s linear infinite")}}]]))

(defn v-axes []
  (let [axes       (rf/subscribe [:axes])
        rank       (rf/subscribe [:current-rank])
        itv        (rf/subscribe [:interval-sec])
        ratio      (rf/subscribe [:x-axis-ratio])
        data-type  (rf/subscribe [:data-type])
        max-val (rf/subscribe [:max-val])
        vals       (map second @rank)
        transition (str "left " @itv "s linear, opacity " @itv "s ease-out")]
    [:div.v-axes
     (doall
      (for [a    @axes
            :let [
;;                  _ (println @max-val)
                  ;; max-val (if (str/index-of (name @data-type) "lowest")
                  ;;           (last vals)
                  ;;           (first vals))
;;                  _ (println max-val)
;;                  min-val (last vals)
                  left (+ 331 (* @ratio a)) ;; 331 = 100 + .3 * 770
                  log10 (Math/log 10)
                  q1 (quot (Math/log (* 1.01 a)) log10)
                  q2 (quot (Math/log @max-val) log10)
                  ]
            ]
        ^{:key (str "v-axis-" a)}
        [:div.v-axis
         {:style (merge {:left (min left #_960 770)
                         :opacity (if (and (= q1 q2) (< left #_900 770)) 1 0)
                         }
                        (transition-css transition))}
         [:div.v-axis-cover]
         [:div.v-axis-label
          {:style {:left (cond
                           (< a 10000) -2
                           :else -5)
                   }} ;; 1. :when 2. left > some-val
          (cond
            (< a 1) (.toFixed a 1)
            :else   (data-fmtr large-num-fmtr a @data-type 0))]]))]))


#_(def month-names
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

(defn v-line []
  [:div.v-line])

(defn get-data-type-name [data-type]
  (case data-type
    :pe "PE"
    :lowest-pe "最小PE"
    :mv "市值"
    :lowest-mv "最小市值"))

(defn chart-page []
  (let [date              (rf/subscribe [:current-date])
        pe-rank           (rf/subscribe [:current-rank])
        secucode          (rf/subscribe [:current-top])
        stockname         (rf/subscribe [:top-stockname])
        first-holder-days (rf/subscribe [:first-holder-days])
        data-type         (rf/subscribe [:data-type])
        lowest-pe (rf/subscribe [:lowest-pe])
        mv (rf/subscribe [:mv])
        lowest-mv (rf/subscribe [:lowest-mv])
        show-axes? (rf/subscribe [:show-axes])
        name (.toUpperCase (name @data-type))]
    [:div.container
     [progress-bar]
     [:div.top-desc #_(ffirst @pe-rank)
      [:div @secucode]
      [:div @stockname]
      [:div.secu-img
       [:img.logo {:src (get-tiny-logo-url @secucode)}]]
      [:div (get-data-type-name @data-type) "保持者"]
      [:div "第 " @first-holder-days " 天"]]
     [:div.title "A股" (get-data-type-name @data-type) "历史前10位"]
     (let [[y m d] (str/split @date #"-")]
       #_[:div.date (month-names m) " " d ", " y]
       [:div.date y "年" m "月" d "日"])
     [rank-desc]
     [main-chart]
     (when (and @lowest-pe @mv @lowest-mv)
       [data-type-controller])
     [axes-controller]
     [time-controller]
     (when @show-axes?
       [:div
        [v-axes]
        [v-line]])]))

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
    (rf/dispatch [:set-data type d])))

(defn fetch-pe! []
  (GET "/pe" {:handler #(data-handler % :pe)}))

(defn fetch-lowest-pe! []
  (GET "/lowest-pe" {:handler #(data-handler % :lowest-pe)}))

(defn fetch-mv! []
  (GET "/mv" {:handler #(data-handler % :mv)}))

(defn fetch-lowest-mv! []
  (GET "/lowest-mv" {:handler #(data-handler % :lowest-mv)}))

(defn fetch-stocknames! []
  (GET "/stocknames" {:handler #(rf/dispatch [:set-stocknames (read-string %)])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  #_(fetch-docs!)
  (fetch-lowest-pe!)
  (fetch-mv!)
  (fetch-lowest-mv!)
  (fetch-stocknames!)
  (hook-browser-navigation!)
  (mount-components))
