(ns text-to-wardley.slatejs.events
  (:require
   [re-frame.core :as re-frame]
   [text-to-wardley.db :as db]
   [text-to-wardley.section.db :as section.db]
   [text-to-wardley.slatejs.core :as slatejs.core]
   [text-to-wardley.slatejs.db :as slatejs.db]))


;; Events


;; Triggered when the slatejs editor value changes.
(re-frame/reg-event-db :text-to-wardley.slatejs/evt-editor-value-changed
                       (fn evt-editor-changed-handler
                         [db [_ section-key editor-value]]
                         (let [editor-value-attrs (slatejs.core/editor-value-js->clj editor-value)
                               {html             :html
                                plain-text       :plain-text
                                active-marks     :active-marks
                                selection-digest :selection-digest} editor-value-attrs
                               existing-section-data (section.db/db-get-data db section-key)
                               new-section-data (-> existing-section-data
                                                    (assoc :html html)
                                                    (assoc :plain-text plain-text))]
                           (-> db
                               (assoc-in (section.db/data-path section-key) new-section-data)
                               (assoc-in (slatejs.db/data-path :active-marks) active-marks)
                               (assoc-in (slatejs.db/data-path :selection-digest) selection-digest)))))
