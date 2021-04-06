(ns text-to-wardley.lambda)

(def state-ref (atom {:start-time (js/Date.)
                      :counter 0}))

(defn handler [request response]
  (swap! state-ref update :counter inc)

  (clj->js
   {:statusCode 200
    :headers {"content-type" "application/edn; charset=utf-8"}
    :body (pr-str @state-ref)}))
