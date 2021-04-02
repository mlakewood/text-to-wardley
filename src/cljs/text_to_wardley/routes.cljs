(ns text-to-wardley.routes
  (:require
   [bidi.bidi :as bidi]
   [pushy.core :as pushy]
   [re-frame.core :as re-frame]
   [text-to-wardley.events :as events]
   [text-to-wardley.db :as db]
   [lambdaisland.uri :as uri]
   ))

(defmulti panels identity)
(defmethod panels :default [] [:div "No panel found for this route."])

(def routes
  (atom
    ["/" {""      :home
          "about" :about}]))

(defn parse
  [url]
  (let [query-params (db/trace "url ->" (uri/query-string->map (:query (uri/uri url))))]
    (bidi/match-route @routes url :query-params query-params)))

(defn url-for
  [& args]
  (apply bidi/path-for (into [@routes] args)))

(defn dispatch
  [route]
  (let [query-params (db/trace "dispatch " (:query-params route))
        panel (keyword (str (name (:handler route)) "-panel"))]
    (re-frame/dispatch [::events/set-navigation {:panel panel :query-params query-params}])))

(def history
  (pushy/pushy dispatch parse))

(defn navigate!
  [handler]
  (pushy/set-token! history (url-for handler)))

(defn start!
  []
  (pushy/start! history))

(re-frame/reg-fx
  :navigate
  (fn [handler]
    (navigate! handler)))