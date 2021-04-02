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
   (:panel (:navigation db))))

(re-frame/reg-sub
 ::editor-raw
 (fn [db _]
   (:raw (:editor db))))

(re-frame/reg-sub
 ::editor-parsed
 (fn [db _]
   (:parsed (:editor db))))

(re-frame/reg-sub
 ::window-size
 (fn [db _]
   (:window-size db)))

(re-frame/reg-sub
 ::query-params
 (fn [db _]
   (:query-params (:navigation db))))

(re-frame/reg-sub
 ::link-encoded
 (fn [db _]
   (:encoded (:editor db))))
