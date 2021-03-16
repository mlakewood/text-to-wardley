(ns text-to-wardley.section.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as re-frame]
   [text-to-wardley.slatejs.core :as ui.slatejs.core]
   [text-to-wardley.slatejs.db :as ui.slatejs.db]
   [text-to-wardley.slatejs.views :as ui.slatejs.views]))

(defn section-static
  "Renders a static (non-editable) section identified by `section-key`."
  [section-key]
  (let [section-data @(re-frame/subscribe [:text-to-wardley.section/sub-data section-key])
        html (:html section-data)
        title (:title section-data)]
    [:div.text-to-wardley-section.text-to-wardley-section--static
     {:on-click #(re-frame/dispatch [:text-to-wardley.editor/cmd-select-current-section section-key])}
     [:h3 title]
     [:div.section-content {:dangerouslySetInnerHTML {:__html html}}]]))

(defn section-editable
  "Renders an editable section identified by `section-key`."
  [section-key]
  (let [section-data @(re-frame/subscribe [:text-to-wardley.section/sub-data section-key])
        title (:title section-data)]
    [:div.text-to-wardley-section.text-to-wardley-section--editable
     {:on-click #(re-frame/dispatch [:text-to-wardley.editor/cmd-select-current-section section-key])}
     [:h3 title]
     [ui.slatejs.views/editor section-key]]))

(defn section
  "Renders a section identifed by `section-key`."
  [section-key]
  (let [current-section-key @(re-frame/subscribe [:text-to-wardley.editor/sub-current-section-key])
        current? (= section-key current-section-key)]
    (if current?
      [section-editable section-key]
      [section-static section-key])))
