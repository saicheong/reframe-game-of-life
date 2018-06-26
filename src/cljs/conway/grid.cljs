(ns conway.grid
  (:require [clojure.set :as set]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))


(defn- client-pos [ev]
  [(.-clientX ev) (.-clientY ev)])

(defn- svg-pos [client-pos svg-elem]
  (let [[x y] client-pos
        svg-point (.call (goog.object/get svg-elem "createSVGPoint") svg-elem)
        ctm (.call (goog.object/get svg-elem "getScreenCTM") svg-elem)
        matrix (.call (goog.object/get ctm "inverse") ctm)]
    (set! (.-x svg-point) x)
    (set! (.-y svg-point) y)
    (let [gpt (.call (goog.object/get svg-point "matrixTransform") svg-point matrix)]
      [(int (.-x gpt)) (int (.-y gpt))])))

(defn- grid-pos [svg-pos cell-size]
  (mapv #(.floor js/Math (/ % cell-size)) svg-pos))

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

  []
  (let [elem (atom nil)]
    (fn [props]
      (let [{:keys [cell-size] :or {cell-size 10}} props
            live-cells @(rf/subscribe [:live-cells])
            mouse-over @(rf/subscribe [:mouse-over])
            cell (fn [loc props]
                   (let [[x y] loc
                         wd (dec cell-size)]
                     [:rect (merge props
                                   {:key    (str x "," y)
                                    :x      (* x cell-size)
                                    :y      (* y cell-size)
                                    :height wd
                                    :width  wd})]))
            grid-loc (fn [ev]
                       (-> (client-pos ev)
                           (svg-pos @elem)
                           (grid-pos cell-size)))]
        [:svg (merge (dissoc props :cell-size)              ; remove non-svg attr
                     {:key            "grid"
                      :ref            #(reset! elem %)
                      :on-click       #(rf/dispatch [:toggle-cell (grid-loc %)])
                      :on-mouse-move  #(rf/dispatch [:mouse-over (grid-loc %)])
                      :on-mouse-leave #(rf/dispatch [:mouse-out])})
         ;; draw the life cells
         (for [loc live-cells]
           (cell loc {:fill "#667292"}))

         ;; mark the moused-over cell location
         (if-let [loc mouse-over]
           (cell loc {:stroke       "#87bdd8"
                      :fill-opacity "0"}))]))))

(defn svg-grid2
  "Same as svg-grid, but implemented as a form 3 component, so that the svg element
   is captured when component is mounted"
  []
  (let [elem (atom nil)]
    (reagent/create-class
      {:display-name
       "svg-grid2"

       :component-did-mount
       (fn [this]
         (println "SVG2 did mount")
         (reset! elem (reagent/dom-node this)))

       :reagent-render
       (fn [props]
         (let [{:keys [cell-size] :or {cell-size 10}} props
               live-cells @(rf/subscribe [:live-cells])
               mouse-over @(rf/subscribe [:mouse-over])
               cell (fn [loc props]
                      (let [[x y] loc
                            wd (dec cell-size)]
                        [:rect (merge props
                                      {:key    (str x "," y)
                                       :x      (* x cell-size)
                                       :y      (* y cell-size)
                                       :height wd
                                       :width  wd})]))
               grid-loc (fn [ev]
                          (-> (client-pos ev)
                              (svg-pos @elem)
                              (grid-pos cell-size)))]
           [:svg (merge (dissoc props :cell-size)
                        {:key            "grid2"
                         :on-click       #(rf/dispatch [:toggle-cell (grid-loc %)])
                         :on-mouse-move  #(rf/dispatch [:mouse-over (grid-loc %)])
                         :on-mouse-leave #(rf/dispatch [:mouse-out])})
            (for [loc live-cells]
              (cell loc {:fill         "#c83349"
                         :fill-opacity 0.5}))

            (if-let [loc mouse-over]
              (cell loc {:stroke         "#5b9aa0"
                         :stroke-opacity "0.8"
                         :fill-opacity   "0"}))]))})))


(defn- canvas-pos
  "Returns a function to translate browser coordinate to canvas coordinate"
  [client-pos elem]
  (let [[x y] client-pos
        rect (.getBoundingClientRect elem)
        cx (- x (.-left rect))
        cy (- y (.-top rect))]
    [cx cy]))

(defn canvas-grid
  "A Reagent/React component that renders life to a html canvas"
  [_]
  (let [elem (atom nil)
        canvas-cells (atom nil)
        live-cells (atom nil)
        cell-size (atom nil)

        grid-loc (fn [ev]
                   (-> (client-pos ev)
                       (canvas-pos @elem)
                       (grid-pos @cell-size)))
        render (fn []
                 (let [ctx (.getContext @elem "2d")
                       cells @live-cells
                       old-cells @canvas-cells
                       new-cells (set/difference cells old-cells)
                       dead-cells (set/difference old-cells cells)
                       draw-cell (fn [c [x y]]
                                   (let [sz @cell-size
                                         wd (dec sz)]
                                     (.fillRect c (* x sz) (* y sz) wd wd)))]

                   ;; draw new cells
                   (set! (.-fillStyle ctx) "#ff6f69")
                   (doseq [pos new-cells]
                     (draw-cell ctx pos))

                   ;; draw over dead cells
                   (set! (.-fillStyle ctx) "#ffffff")
                   (doseq [pos dead-cells]
                     (draw-cell ctx pos))

                   ;; TODO:
                   ;; I have not figured out how to draw the mouse over cell
                   ;; and make sure it does not draw over the live/dead cells

                   ;; remember cells drawn on canvas
                   (reset! canvas-cells cells)))]

    (reagent/create-class
      {:display-name
       "canvas-grid"

       :component-did-mount
       (fn [this]
         (println "canvas did mount")
         (reset! elem (reagent/dom-node this))
         (render))

       :reagent-render
       (fn [props]
         (reset! live-cells @(rf/subscribe [:live-cells]))
         (reset! cell-size (:cell-size props 10))
         [:canvas (merge (dissoc props :cell-size)
                         {:key            "canvas"
                          :on-click       #(rf/dispatch [:toggle-cell (grid-loc %)])
                          :on-mouse-move  #(rf/dispatch [:mouse-over (grid-loc %)])
                          :on-mouse-leave #(rf/dispatch [:mouse-out])})])

       :component-did-update
       (fn [_] (render))})))
