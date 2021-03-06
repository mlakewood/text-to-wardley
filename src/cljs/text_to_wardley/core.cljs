(ns text-to-wardley.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [text-to-wardley.events :as events]
   [text-to-wardley.routes :as routes]
   [text-to-wardley.views :as views]
   [text-to-wardley.config :as config]
   [text-to-wardley.subs :as subs]
   [text-to-wardley.db :as db]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/start!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
