(ns text-to-wardley.events
  (:require
   [re-frame.core :as re-frame]
   [text-to-wardley.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [district0x.re-frame.window-fx]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-fx
  ::navigate
  (fn-traced [_ [_ handler]]
   {:navigate handler}))

(re-frame/reg-event-fx
 ::set-active-panel
 (fn-traced [{:keys [db]} [_ active-panel]]
   {:db (assoc db :active-panel active-panel)}))

(re-frame/reg-event-fx
 ::update-editor-contents
 (fn-traced [{:keys [db]} [_ editor-contents]]
              {:db (assoc db :editor (db/populate-editor editor-contents))}))

(re-frame.core/reg-event-db        ;; <-- call this to register a handler
 ::window-resized                      ;; this is an event id
 (fn [db [_ width height]]          ;; this function does the handling
   (assoc db :window-size {:width width :height height})))

(re-frame/reg-event-fx
 ::track-window-size
 (fn-traced []
   {:window/on-resize {:dispatch [::window-resized]
                       :debounce-ms 250}}))