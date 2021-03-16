(ns text-to-wardley.views
  (:require
   [re-frame.core :as re-frame]
   [re-com.core :as re-com :refer [at]]
   [text-to-wardley.events :as events]
   [text-to-wardley.routes :as routes]
   [text-to-wardley.subs :as subs]
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
       :width "30%"
       :height "90%"
       :rows 22
       :on-change #(re-frame/dispatch [::events/update-editor-contents %])]
    )
  )

(defn map-nodes []
  (let [nodes (re-frame/subscribe [::subs/editor-parsed])
        x 300
        y 100
        ]
    [:text {:x 280 :y 440 :fill "black"} "Print"]
    [:circle {:cx x :cy y :r "5" :stroke "black" :stroke-width "3" :fill "white"}]
    ))

(defn diagram [map-nodes]
  [:svg {:style {:border "1px solid" :background "white" :width "50%" :height "90%"}}
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
   [:text {:x 440 :y 440 :fill "black"} "Product (+ rental)"]
   [:text {:x 620 :y 440 :fill "black"} "Commodity ( + utility)"]
   map-nodes
   ])

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
                 :children [
                  [editor]
                  [diagram (map-nodes)]         
                 ]]
              [link-to-about-page]
              ]]
  )



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

