(ns text-to-wardley.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [text-to-wardley.events :as events]
   [text-to-wardley.routes :as routes]
   [text-to-wardley.subs :as subs]
   [clojure.string :as str]
   [text-to-wardley.db :as db]
  ;;  [cljsjs.quill]
  ;;  [text-to-wardley.quill :as quill]
  ;;  [text-to-wardley.section.views :as section.views]
   ))


;; home

(defn home-title []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :src   (at)
     :label (str "Text to Wardley Map.")
     :level :level1]))

(defn editor []
  (let [text (re-frame/subscribe [::subs/editor-raw])]
    [re-com/input-textarea
     :model text
     :width "400px"
     :height "455px"
     :rows 22
     :change-on-blur? false
     :on-change #(re-frame/dispatch [::events/update-editor-contents %])]))

(defn round-to-five [number]
  (* (Math/round (/ number 5)) 5))


(defn calculate-phase-box [xmin xmax]
  (let [phase-size (round-to-five (/ (- xmax xmin) 4))
        genesis [xmin (+ xmin phase-size)]
        custom [(second genesis) (+ (second genesis) phase-size)]
        product [(second custom) (+ (second custom) phase-size)]
        commodity [(second product) (+ (second product) phase-size)]]
    {:Genesis genesis :Custom-Built custom :Product product :Commodity commodity}))


(defn calculate-x [node-details phase-box]
  (if (contains? node-details :Evolution)
    (let [_ (println phase-box)
          phase (:phase (:Evolution node-details))
          xmin (first (phase phase-box))
          xmax (second (phase phase-box))
          diff (- xmax xmin)
          offset (* diff (/ (:x-axis (:Evolution node-details)) 100))]
      (+ xmin offset))
    (+ (/ (- (first (:Genesis phase-box)) (second (:Commodity phase-box))) 2) (first (:Genesis phase-box)))))

(defn calculate-y [node-details ymin ymax]
  (if (contains? node-details :Visible)
    (let [diff (db/trace "diff " (- ymax ymin))
          value (:y-axis (:Visible node-details))
          invert-value (- 100 value)
          offset (db/trace "offset " (* diff (/ invert-value 100)))]
      (+ ymin offset))
    (+ (/ (- ymax ymin) 2) ymin)))

(defn map-node [value phase-box ymin ymax label]
  (let [x (calculate-x value phase-box)
        y (calculate-y value ymin ymax)
        x-mod (- x (* 3.2 (count label)))
        ]
    [[:rect {:x (- x-mod 5) :y (- y 35) :width (* (count label) 8) :height 20 :fill "white"}]
     [:text {:x x-mod :y (- y 20) :fill "black"} label]
     [:circle {:cx x :cy y :r "5" :stroke "black" :stroke-width "2" :fill "white"}]]))

(defn map-nodes [xmin xmax ymin ymax]
  (let [nodes (re-frame/subscribe [::subs/editor-parsed])
        labeled-nodes (db/trace "labels" (map (fn [[key value]] [(db/labelize-node key) value]) (seq @nodes)))
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
        _ (println @nodes)
        phase-box (calculate-phase-box xmin xmax)
        links (db/trace "links -> " (map (fn [node-key] (let [node-coords (calculate-coords-node node-key phase-box ymin ymax)]
                                                          
                                                          (map
                                                           (fn [node] (let [phase-box (calculate-phase-box 55 800)
                                                                            result [node-coords (calculate-coords-node node phase-box 30 420)]]
                                                                        result))
                                                           (:links (:Needs (node-key @nodes))))
                                                          
                                                          )) 
                                         (keys @nodes)))
        svg-lines (map
                   (fn [out]
                     (map (fn [in] (draw-deps-line (first in) (last in) 0))
                          out))
                   links)]
    svg-lines))

(map-deps 55 800 30 420)

(defn diagram [xmin xmax ymin ymax]
  (let [background [:svg {:style {:border "1px solid" :background "white" :width "50%" :height "90%"}}
                    [:line {:x1 "55" :y1 "30" :x2 "55" :y2 "420" :style {:stroke-width 2 :stroke "black"}}]
                    [:line {:x1 "240" :y1 "30" :x2 "240" :y2 "420" :stroke-dasharray "5,5" :style {:stroke-width 2 :stroke "grey"}}]
                    [:line {:x1 "425" :y1 "30" :x2 "425" :y2 "420" :stroke-dasharray "5,5" :style {:stroke-width 2 :stroke "grey"}}]
                    [:line {:x1 "610" :y1 "30" :x2 "610" :y2 "420" :stroke-dasharray "5,5" :style {:stroke-width 2 :stroke "grey"}}]
                    [:line {:x1 "55" :y1 "420" :x2 "800" :y2 "420" :style {:stroke-width 2 :stroke "black"}}]
                    [:text {:x 5 :y 45 :fill "black" :transform "rotate(270) translate(-90,5)"} "Visible"]
                    [:text {:x 5 :y 200 :fill "black" :font-weight "bold" :transform "rotate(270) translate(-270,-150)"} "Value Chain"]
                    [:text {:x 5 :y 400 :fill "black" :transform "rotate(270) translate(-420,-350)"} "Invisible"]
                    [:text {:x 65 :y 440 :fill "black"} "Genesis"]
                    [:text {:x 250 :y 440 :fill "black"} "Custom Built"]
                    [:text {:x 440 :y 440 :fill "black"} "Product"]
                    [:text {:x 620 :y 440 :fill "black"} "Commodity"]]
        nodes (apply concat (map-nodes xmin xmax ymin ymax))
        lines (apply concat (map-deps xmin xmax ymin ymax))
        ]
    (into [] (concat background lines nodes))))

(defn link-to-about-page []
  [re-com/hyperlink
   :src      (at)
   :label    "go to About Page"
   :on-click #(re-frame/dispatch [::events/navigate :about])])

(defn home-panel []
  [re-com/v-box
   :src      (at)
   :gap      "1em"
   :children [[home-title]
              [re-com/h-box
               :width "100%"
               :height "505px"
               :src (at)
               :gap "1em"
               :children [[editor]
                          [diagram 55 800 30 420]]]
              [link-to-about-page]]])



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
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :src      (at)
     :height   "100%"
     :children [(routes/panels @active-panel)]]))


(def data {:Customer {:Evolution {:phase :Custom-Built, :x-axis 65}, :Visible {:y-axis 98}, :Needs {:links [:Online-Image-Manipulation :Online-Photo-Storage]}}, :Online-Image-Manipulation {:Evolution {:phase :Custom-Built, :x-axis 20}, :Visible {:y-axis 85}}, :Online-Photo-Storage {:Evolution {:phase :Custom-Built, :x-axis 50}, :Visible {:y-axis 70}}})


(map
  (fn [node] (let [phase-box (calculate-phase-box 55 800)
                   result ["foo" (calculate-coords-node node phase-box 30 420)]]
     result))
 (:links (:Needs (:Customer data)))
 )