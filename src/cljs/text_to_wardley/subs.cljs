(ns text-to-wardley.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::editor-raw
 (fn [db _]
   (:raw (:editor db))))

(re-frame/reg-sub
 ::editor-parsed
 (fn [db _]
   (:parsed (:editor db))))