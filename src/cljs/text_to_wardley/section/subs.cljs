(ns text-to-wardley.section.subs
  (:require
   [re-frame.core :refer [reg-sub subscribe]]
   [text-to-wardley.section.db :as section.db]))

;; Returns data for section under `section-key`.
(reg-sub :text-to-wardley.section/sub-data
         (fn sub-data
           [db [_ section-key]]
           (get-in db (section.db/data-path section-key))))