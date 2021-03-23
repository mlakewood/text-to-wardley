(ns text-to-wardley.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [text-to-wardley.events :as events]
   [text-to-wardley.routes :as routes]
   [text-to-wardley.subs :as subs]
   [clojure.string :as str]
   [text-to-wardley.db :as db]
   [clojure.pprint :as pp]
  ;;  [cljsjs.quill]
  ;;  [text-to-wardley.quill :as quill]
  ;;  [text-to-wardley.section.views :as section.views]
   ))


;; home


(defn percent-of [number percent]
  (* number (/ percent  100)))

(defn home-title []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label (str "Text to Wardley Map.")
     :level :level1]))

(defn editor []
  (let [window-size (re-frame/subscribe [::subs/window-size])
        height (percent-of (:height @window-size) 75)
        width (percent-of (:width @window-size) 30)
        _ (db/trace "editor width ->" width)
        _ (db/trace "editor height ->" height)
        text (re-frame/subscribe [::subs/editor-raw])]
    [re-com/input-textarea
     :model text
     :width (str width "px")
     :height (str height "px")
    ;;  :rows 22
     :change-on-blur? false
     :on-change #(re-frame/dispatch [::events/update-editor-contents %])]))

(defn round-to-five [number]
  (* (Math/round (/ number 5)) 5))

(defn is-node? [node-key node-collection]
  (if (db/in? node-collection node-key)
    true
    false))

(defn calculate-phase-box [xmin xmax]
  (let [phase-size (round-to-five (/ (- xmax xmin) 4))
        genesis [xmin (+ xmin phase-size)]
        custom [(second genesis) (+ (second genesis) phase-size)]
        product [(second custom) (+ (second custom) phase-size)]
        commodity [(second product) (+ (second product) phase-size)]]
    {:Genesis genesis :Custom-Built custom :Product product :Commodity commodity}))


(defn calculate-x [node-details phase-box]
  (if (contains? node-details :Evolution)
    (let [phase (:phase (:Evolution node-details))
          xmin (first (phase phase-box))
          xmax (second (phase phase-box))
          diff (- xmax xmin)
          offset (* diff (/ (:x-axis (:Evolution node-details)) 100))]
      (+ xmin offset))
    (+ (/ (- (first (:Genesis phase-box)) (second (:Commodity phase-box))) 2) (first (:Genesis phase-box)))))

(defn calculate-y [node-details ymin ymax]
  (if (contains? node-details :Visible)
    (let [diff (- ymax ymin)
          value (:y-axis (:Visible node-details))
          invert-value (- 100 value)
          offset (* diff (/ invert-value 100))]
      (+ ymin offset))
    (+ (/ (- ymax ymin) 2) ymin)))

(defn calc-backing-box-size [label]
  (* (count label) 8))

(defn map-node [value phase-box ymin ymax label]
  (let [x (calculate-x value phase-box)
        y (calculate-y value ymin ymax)
        x-mod (- x (* 3.2 (count label)))
        ]
    [[:rect {:x (- x-mod 5) :y (- y 35) :width (calc-backing-box-size label) :height 20 :fill "white"}]
     [:text {:x x :y (- y 20) :fill "black" :text-anchor "middle" :dominant-baseline "auto"} label]
     [:circle {:cx x :cy y :r "5" :stroke "black" :stroke-width "2" :fill "white"}]]))

(defn map-nodes [xmin xmax ymin ymax]
  (let [nodes (re-frame/subscribe [::subs/editor-parsed])
        labeled-nodes (map (fn [[key value]] [(db/labelize-node key) value]) (seq @nodes))
        phase-box (calculate-phase-box xmin xmax)
        pairs (map (fn [[key value]]
               (map-node value phase-box ymin ymax key))
             labeled-nodes)]
    pairs))

(defn calculate-coords-node [node-key phase-box ymin ymax]
  (let [nodes (re-frame/subscribe [::subs/editor-parsed])
        x (calculate-x (node-key @nodes) phase-box)
        y (calculate-y (node-key @nodes) ymin ymax)
        ]
    {:x x :y y}))

(defn draw-deps-line [start end offset]
  (let [x1 (- (:x start) offset)
        y1 (+ (:y start) offset)
        x2 (+ (:x end) offset)
        y2 (- (:y end) offset)]
    [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :style {:stroke-width 2 :stroke "black"}}]))
  

(defn map-deps [xmin xmax ymin ymax]
  (let [nodes (re-frame/subscribe [::subs/editor-parsed])
        phase-box (calculate-phase-box xmin xmax)
        links     (map (fn [node-key] (let [node-coords (calculate-coords-node node-key phase-box ymin ymax)]

                                        (map
                                         (fn [node]
                                           (if (is-node? node (keys @nodes))
                                             (let [phase-box (calculate-phase-box xmin xmax)
                                                   result [node-coords (calculate-coords-node node phase-box ymin ymax)]]
                                               result)
                                             {}))
                                         (:links (:Needs (node-key @nodes))))))
                       (keys @nodes))
        svg-lines (map
                   (fn [out]
                     (map (fn [in] (draw-deps-line (first in) (last in) 0))
                          out))
                   links)]
    (filter not-empty svg-lines)))

(defn x-axis-labels [xmin ymin ymax]
  (let [y-axis-length (- ymax ymin)
        visible-transform (str "rotate(270, " xmin ", " ymin ") translate(-20, -5)")
        value-chain-transform (str "rotate(270, " xmin ", " ymin ") translate("(* (/ y-axis-length 2) -1) ", -5)")
        invisible-transform (str "rotate(270, " xmin ", " ymin ") translate(" (* (- ymax 60) -1) ", -5)")]
    [[:text {:x xmin :y ymin :fill "black" :style {:text-anchor "middle"} :transform visible-transform} "Visible"]
     [:text {:x xmin :y ymin :fill "black" :style {:text-anchor "middle"} :font-weight "bold" :transform value-chain-transform} "Value Chain"]
     [:text {:x xmin :y ymin :fill "black" :style {:text-anchor "middle"} :transform invisible-transform} "Invisible"]
     ]))

(defn diagram [xmin xmax ymin ymax]
  (let [phase-box (calculate-phase-box xmin xmax)
        evo-pad-x 30
        evo-pad-y 20
        background [:svg {:style {:border "1px solid" :background "white" :width "70%" :height "100%"}}
                    [:line {:x1 (first (:Genesis phase-box)) :y1 ymin :x2 (first (:Genesis phase-box)) :y2 ymax :style {:stroke-width 2 :stroke "black"}}]
                    [:line {:x1 (first (:Custom-Built phase-box)) :y1 ymin :x2 (first (:Custom-Built phase-box)) :y2 ymax :stroke-dasharray "5,5" :style {:stroke-width 2 :stroke "grey"}}]
                    [:line {:x1 (first (:Product phase-box)) :y1 ymin :x2 (first (:Product phase-box)) :y2 ymax :stroke-dasharray "5,5" :style {:stroke-width 2 :stroke "grey"}}]
                    [:line {:x1 (first (:Commodity phase-box)) :y1 ymin :x2 (first (:Commodity phase-box)) :y2 ymax :stroke-dasharray "5,5" :style {:stroke-width 2 :stroke "grey"}}]
                    [:line {:x1 (first (:Genesis phase-box)) :y1 ymax :x2 xmax :y2 ymax :style {:stroke-width 2 :stroke "black"}}]
                    [:text {:x (+ evo-pad-x (first (:Genesis phase-box))) :y (+ evo-pad-y ymax) :fill "black"} "Genesis"]
                    [:text {:x (+ evo-pad-x (first (:Custom-Built phase-box))) :y (+ evo-pad-y ymax) :fill "black"} "Custom Built"]
                    [:text {:x (+ evo-pad-x (first (:Product phase-box))) :y (+ evo-pad-y ymax) :fill "black"} "Product"]
                    [:text {:x (+ evo-pad-x (first (:Commodity phase-box))) :y (+ evo-pad-y ymax) :fill "black"} "Commodity"]]
        
        nodes (apply concat (map-nodes xmin xmax ymin ymax))
        lines (apply concat (map-deps xmin xmax ymin ymax))
        labels (db/trace "labels" (x-axis-labels xmin ymin ymax))
        ]
    (into [] (concat background lines nodes labels))))

(defn link-to-about-page []
  [re-com/hyperlink-href
   :src      (at)
   :label    "Problems or questions?"
   :style {:padding-left "24px"}
   :href     "https://github.com/mlakewood/text-to-wardley/issues"])


(defn home-panel []
  (let [window-size (re-frame/subscribe [::subs/window-size])
        height (:height @window-size)
        width (:width @window-size)
        ]
    [re-com/v-box
     :src      (at)
     :gap      "1em"
     :children [[re-com/h-box
                 :width (str (percent-of width 98) "px")
                 :height (str (percent-of height 15) "px")
                 :src (at)
                 :gap "1em"
                 :children [[re-com/gap :size "10px"]
                            [home-title]
                            [re-com/gap :size "25px"]
                            [re-com/p {:style {:color "black" :padding-top "25px"}} "Don't know what a Wardley map is? Read the book -> " [:a "https://medium.com/wardleymaps"]]
                            
                            
                            ]]
                [re-com/h-box
                 :width (str (percent-of width 98) "px")
                 :height (str (percent-of height 75) "px")
                 :src (at)
                 :gap "1em"
                 :children [[re-com/gap :size "10px"]
                            [editor]
                            [diagram 55 (percent-of width 65) 30 (percent-of height 70)]]]
              [link-to-about-page]
                ]]
    ))



(defmethod routes/panels :home-panel [] [home-panel])

;; about

(defn about-title []
  [re-com/title
   :src   (at)
   :label "This is the About Page."
   :level :level1])

(defn link-to-home-page []
  [re-com/hyperlink
   :src      (at)
   :label    "go to Home Page"
   :on-click #(re-frame/dispatch [::events/navigate :home])])

(defn about-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[about-title]
              [link-to-home-page]]])

(defmethod routes/panels :about-panel [] [about-panel])

;; main

(defn main-panel []
  (let [_ (re-frame/dispatch [::events/track-window-size])
        active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :src      (at)
     :height   "100%"
     :children [(routes/panels @active-panel)]]))


(* 100 (/ 90  100))

(keys @(re-frame/subscribe [::subs/editor-parsed]))