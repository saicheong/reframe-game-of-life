(ns conway.core
  "An implementation of Conway Game of Life.

  Reference:
  1. Clojure Programming, Chas Emerick, Brian Carper & Christophe Grand
  2. Wikipedia: https://en.wikipedia.org/wiki/Conway's_Game_of_Life"
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as rf]
    [conway.config :as config]
    [clojure.set :as set]))

(defn neighbours [[x y]]
  (for [dx [-1 0 1]
        dy [-1 0 1]
        :when (not= 0 dx dy)]
    [(+ x dx) (+ y dy)]))

(defn next-generation
  "Returns the next generation of cells"
  [cells]
  (set (for [[loc n] (frequencies (mapcat neighbours cells))
             :when (or (= n 3) (and (= n 2) (cells loc)))]
         loc)))

(defn toggle-cell [cells loc]
  (if (cells loc) (disj cells loc) (conj cells loc)))

(def default-db
  {:live-cells #{[1 5] [1 6] [2 5] [2 6]
                 [11 5] [11 6] [11 7]
                 [12 4] [12 8]
                 [13 3] [13 9]
                 [14 3] [14 9]
                 [15 6]
                 [16 4] [16 8]
                 [17 5] [17 6] [17 7]
                 [18 6]
                 [21 3] [21 4] [21 5]
                 [22 3] [22 4] [22 5]
                 [23 2] [23 6]
                 [25 1] [25 2] [25 6] [25 7]
                 [35 3] [35 4]
                 [36 3] [36 4]}                             ; Gosper Gliding Gun
   :interval   100                                          ; milliseconds
   :running    false})

;; Other well known seeds:
;; Blinker [1 2] [2 2] [3 2]
;; Pentadecathlon
;; Glider [2 1] [3 2] [1 3] [2 3] [3 3]
;; Lightweight spaceship [1 1] [1 3] [2 4] [3 4] [4 1] [4 4] [5 2] [5 3] [5 4]

;; TODO: (challenge) Playback button

;; -- Domino 1 - Event Dispatch -----------------------------------------------


;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-event-db
  :initialize-db
  (fn [_ _]
    default-db))

(rf/reg-event-db
  :toggle-cell
  (fn [db [_ grid-pos]]
    (update db :live-cells toggle-cell grid-pos)))

(rf/reg-event-db
  :mouse-over
  (fn [db [_ grid-pos]]
    (assoc db :mouse-over grid-pos)))

(rf/reg-event-db
  :mouse-out
  (fn [db [_ _]]
    (assoc db :mouse-over nil)))

(rf/reg-event-db
  :interval-change
  (fn [db [_ new-interval]]
    (if-let [timing (re-matches #"\d+" new-interval)]
      (assoc db :interval (js/parseInt timing 10))
      db)))

(rf/reg-event-db
  :next-step
  (fn [db _]
    (assoc db :live-cells (next-generation (:live-cells db)))))

(rf/reg-event-db
  :pause-play
  (fn [db _]
    (assoc db :running false)))

(rf/reg-event-fx
  :play
  (fn [{:keys [db running]} _]
    (if running
      {:db db}
      {:db       (assoc db :running true)
       :dispatch [:run]})))

(rf/reg-event-fx
  :run
  (fn [cofx _]
    (let [{:keys [db]} cofx
          {:keys [live-cells interval running]} db]
      {:db             (assoc db :live-cells (next-generation live-cells))
       :dispatch-later [(when running {:ms interval :dispatch [:run]})]})))


;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :live-cells
  (fn [db]
    (:live-cells db)))

(rf/reg-sub
  :interval
  (fn [db]
    (:interval db)))

(rf/reg-sub
  :mouse-over
  (fn [db]
    (:mouse-over db)))

(rf/reg-sub
  :running
  (fn [db]
    (:running db)))

;; -- Domino 5 - View Functions ----------------------------------------------

(defn client-xy [ev]
  [(.-clientX ev) (.-clientY ev)])

(defn svg-xy-fn
  "Create the browser coordinates to SVG cordinates transformation function for
   the SVG element

   Reference:
   https://stackoverflow.com/questions/12752519/svg-capturing-mouse-coordinates"
  [svg-elem]
  (let [svg-point (.createSVGPoint svg-elem)]
    (fn [[x y]]
      (let [matrix (-> svg-elem .getScreenCTM .inverse)]
        (set! (.-x svg-point) x)
        (set! (.-y svg-point) y)
        (let [gpt (.matrixTransform svg-point matrix)]
          [(int (.-x gpt)) (int (.-y gpt))])))))

(defn grid-xy-fn
  "Create the SVG coordinates to grid coordinates transformation function for
   the given grid size"
  [cell-sz]
  (fn [svg-xy] (mapv #(.floor js/Math (/ % cell-sz)) svg-xy)))

(defn svg-cell
  ([loc sz]
   (let [[x y] loc]
     [:rect {:key    (str x "," y)
             :x      (* x sz)
             :y      (* y sz)
             :height sz
             :width  sz}]))
  ([loc sz style]
   (assoc-in (svg-cell loc sz) [1 :style] style)))

(defn svg-grid
  "A React/Reagent component that renders life
   to an SVG element.

   Uses a ref function to capture the backing svg element - needed to convert
   the mouse location to svg coordinates.

   Reference:
   https://github.com/reagent-project/reagent/blob/master/doc/CreatingReagentComponents.md
   https://presumably.de/reagent-mysteries-part-3-manipulating-the-dom.html
   http://timothypratley.blogspot.com/2017/01/reagent-deep-dive-part-2-lifecycle-of.html
   "
  [width height cell-size]
  (let [svg-xy (atom nil)]
    (fn [width height cell-size]
      (let [live-cells @(rf/subscribe [:live-cells])
            mouse-over @(rf/subscribe [:mouse-over])
            grid-xy (grid-xy-fn cell-size)]
        [:svg {:key            "grid"
               :style          {:border-style "solid"
                                :border-width "1px"
                                :margin       "10px"}
               :width          width
               :height         height
               :ref            (fn [svg-elem]
                                 (when (nil? @svg-xy)
                                   (reset! svg-xy (svg-xy-fn svg-elem))))

               :on-click       (fn [ev]
                                 (rf/dispatch [:toggle-cell (-> ev client-xy (@svg-xy) grid-xy)]))

               :on-mouse-move  (fn [ev]
                                 (rf/dispatch [:mouse-over (-> ev client-xy (@svg-xy) grid-xy)]))

               :on-mouse-leave (fn [_]
                                 (rf/dispatch [:mouse-out]))}

         ;; draw the life cells
         (for [loc live-cells]
           (svg-cell loc cell-size))

         ;; mark the moused-over cell location
         (if-let [loc mouse-over]
           (svg-cell loc cell-size {:stroke         "red"
                                    :stroke-width   "2"
                                    :stroke-opacity "0.3"
                                    :fill-opacity   "0"}))

         ])))

  )

(defn svg-grid2
  "Same as svg-grid, but implemented as a form 3 component, so that the svg element
   is captured when component is mounted"
  [width height cell-size]
  (let [svg-xy (atom nil)]
    (reagent/create-class
      {:display-name
       "svg-grid2"

       :component-did-mount
       (fn [this]
         (println "SVG2 did mount")
         (let [svg-elem (reagent/dom-node this)]
           (reset! svg-xy (svg-xy-fn svg-elem))))

       :reagent-render
       (fn [width height cell-size]
         (let [live-cells @(rf/subscribe [:live-cells])
               mouse-over @(rf/subscribe [:mouse-over])
               grid-xy (grid-xy-fn cell-size)]
           [:svg {:key            "grid2"
                  :style          {:border-style "solid"
                                   :border-width "1px"
                                   :margin       "10px"}
                  :width          width
                  :height         height

                  :on-click       (fn [ev]
                                    (rf/dispatch [:toggle-cell (-> ev client-xy (@svg-xy) grid-xy)]))

                  :on-mouse-move  (fn [ev]
                                    (rf/dispatch [:mouse-over (-> ev client-xy (@svg-xy) grid-xy)]))

                  :on-mouse-leave (fn [_]
                                    (rf/dispatch [:mouse-out]))}
            (for [loc live-cells]
              (svg-cell loc cell-size {:fill         "grey"
                                       :fill-opacity 0.5}))

            (if-let [loc mouse-over]
              (svg-cell loc cell-size {:stroke         "blue"
                                       :stroke-width   "2"
                                       :stroke-opacity "0.3"
                                       :fill-opacity   "0"}))
            ]))

       })))


(defn canvas-xy-fn
  "Returns a function to translate browser coordinate to canvas coordinate"
  [elem]
  (fn [[x y]]
    (let [rect (.getBoundingClientRect elem)
          cx (- x (.-left rect))
          cy (- y (.-top rect))]
      [cx cy])))

(defn canvas-grid-xy-fn
  "Returns a function to translate canvas coordinate to game grid coordinate"
  [cell-size]
  (fn [xy]
    (mapv #(.floor js/Math (/ % cell-size)) xy)))

(defn draw-cell [ctx [x y] sz]
  (.fillRect ctx (* x sz) (* y sz) sz sz))

(defn canvas-grid
  "A Reagent/React component that renders life to a html canvas"
  [width height cell-size]
  (let [elem (atom nil)
        canvas-cells (atom nil)
        live-cells (atom nil)
        cell-size (atom cell-size)

        canvas-xy (atom nil)
        canvas-grid-xy (canvas-grid-xy-fn @cell-size)
        render (fn [sz]
                 (let [ctx (.getContext @elem "2d")
                       cells @live-cells
                       old-cells @canvas-cells
                       new-cells (set/difference cells old-cells)
                       dead-cells (set/difference old-cells cells)]

                   ;; draw new cells
                   (set! (.-fillStyle ctx) "grey")
                   (doseq [pos new-cells]
                     (draw-cell ctx pos sz))

                   ;; draw over dead cells
                   (set! (.-fillStyle ctx) "white")
                   (doseq [pos dead-cells]
                     (draw-cell ctx pos sz))

                   ;; remember cells drawn on canvas
                   (reset! canvas-cells cells)))]

    (reagent/create-class
      {:display-name
       "canvas-grid"

       :component-did-mount
       (fn [this]
         (println "canvas did mount")
         (reset! elem (reagent/dom-node this))
         (reset! canvas-xy (canvas-xy-fn @elem))
         (render @cell-size))

       :reagent-render
       (fn [wd ht sz]
         (let []
           (reset! live-cells @(rf/subscribe [:live-cells]))
           (reset! cell-size sz)
           [:canvas {:key            "canvas"
                     :style          {:border-style "solid"
                                      :border-width "1px"
                                      :margin       "10px"}
                     :width          wd
                     :height         ht
                     :on-click       (fn [ev]
                                       (rf/dispatch [:toggle-cell (-> ev client-xy (@canvas-xy) canvas-grid-xy)]))
                     :on-mouse-move  (fn [ev]
                                       (rf/dispatch [:mouse-over (-> ev client-xy (@canvas-xy) canvas-grid-xy)]))
                     :on-mouse-leave (fn [_]
                                       (rf/dispatch [:mouse-out]))}]))

       :component-did-update
       (fn [_]
         (render @cell-size))})))

(defn button [label event]
  ;; Have to return a function for this to work
  (fn []
    [:input {:type     "button"
             :value    label
             :on-click #(rf/dispatch [event])}]))

(defn step-btn []
  (let [running @(rf/subscribe [:running])]
    [:input {:type     "button"
             :value    " Step "
             :disabled running
             :on-click #(rf/dispatch [:next-step])}]))

(defn play-btn []
  (let [running @(rf/subscribe [:running])]
    [:input {:type     "button"
             :value    " Play "
             :disabled running
             :on-click #(rf/dispatch [:play])}]))

(defn interval-input []
  [:input {:type      "input"
           :value     @(rf/subscribe [:interval])
           :on-change (fn [e]
                        (rf/dispatch [:interval-change (-> e .-target .-value)]))}])

(defn grid-pos []
  (let [pos @(rf/subscribe [:mouse-over])]
    [:div "Pointer Position: " (str pos)]))

(defn main-panel []
  [:div
   [:h1 "Conway Game of Life - using Re-frame"]
   [:div {:style {:margin-bottom "10px"}} [grid-pos]]
   [:div
    [button " Reset " :initialize-db]
    [step-btn]
    [play-btn]
    [button " Pause " :pause-play]
    "  Interval: "
    [interval-input]
    ]
   [:div [svg-grid 800 500 10]]
   #_[:div {:style {:margin-top "10px"}} [svg-grid2 800 500 10]]
   #_[:div {:style {:margin-top "10px"}} [canvas-grid 800 500 10]]])


;; -- Entry Point -------------------------------------------------------------

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
