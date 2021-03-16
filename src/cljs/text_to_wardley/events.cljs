(ns text-to-wardley.events
  (:require
   [re-frame.core :as re-frame]
   [text-to-wardley.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
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
             
