(ns conway.core
  "An implementation of Conway Game of Life.

  Played out on a infinite 2 dimensional grid of square cells, each of which
  may be alive (populated) or dead (unpopulated). Each cell has eight neighbours.

  At each step in time, the following transitions occur:

  1. Any live cell with fewer than 2 live neighbour dies (as if by under population).
  2. Any live cell with 2 or 3 live neighbour lives on.
  3. Any live cell with more than 3 live neighbour dies (as if by over population).
  4. Any dead cell with exactly 3 live neighbour becomes a live cell. (as if by reproduction)

  Reference:
  1. Clojure Programming, Chas Emerick, Brian Carper & Christophe Grand
  2. Wikipedia: https://en.wikipedia.org/wiki/Conway's_Game_of_Life"
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as rf]
    [conway.config :as config]
    [conway.grid :refer [svg-grid svg-grid2 canvas-grid]]))

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

;; setting up

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

;; running

(rf/reg-event-db
  :pause
  (fn [db _]
    (assoc db :running false)))

(rf/reg-event-fx
  :play
  (fn [{:keys [db running]} _]
    (if running
      {:db db}
      {:db       (assoc db :running true)
       :dispatch [:step]})))

(rf/reg-event-fx
  :step
  (fn [cofx _]
    (let [{:keys [db]} cofx
          {:keys [live-cells interval running]} db]
      (cond-> {:db (assoc db :live-cells (next-generation live-cells))}
              running (assoc :dispatch-later [{:ms interval :dispatch [:step]}])))))


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
             :on-click #(rf/dispatch [:step])}]))

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

(defn pointer-pos []
  (let [pos @(rf/subscribe [:mouse-over])]
    [:div "Pointer Position: " (str pos)]))

(defn main-panel []
  [:div
   [:h1 "Conway Game of Life - using Re-frame"]
   [:div {:style {:margin-bottom "10px"}} [pointer-pos]]
   [:div
    [button " Reset " :initialize-db]
    [step-btn]
    [play-btn]
    [button " Pause " :pause]
    "  Interval: "
    [interval-input]
    ]


   [:div [svg-grid {:width     800
                    :height    500
                    :cell-size 10
                    :style     {:border-style "solid"
                                :border-width "3px"
                                :border-color "#bccad6"
                                :margin       "10px"}}]]

   #_
   [:div [svg-grid2 {:width     800
                    :height    500
                    :cell-size 10
                    :style     {:border-style "solid"
                                :border-width "3px"
                                :border-color "#622569"
                                :margin       "10px"}}]]

   #_
   [:div [canvas-grid {:width     800
                       :height    500
                       :cell-size 10
                       :style     {:border-style "solid"
                                   :border-color "#588c7e"
                                   :border-width "3px"
                                   :margin       "10px"}}]]
   ])


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
