(ns text-to-wardley.db
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pp]
   [clojure.set :as set]
   [text-to-wardley.config :as config]
   [goog.crypt.base64 :as b64]))

(def starting-editor-value
  (str
   "Customer:
	 Evolution: Custom Built, 65%
	 Visible: 98%
	 Needs: Online Image Manipulation, Online Photo Storage, Web Site, Print
Online Image Manipulation:
	 Evolution: Custom Built, 10%
	 Visible: 90%
         Needs: Online Photo Storage
Online Photo Storage:
	 Evolution: Custom Built, 50%
	 Visible: 80%
         Needs: Web Site
Print:
	 Evolution: Product, 45%
	 Visible: 80%
         Needs: Web Site
Web Site:
	 Evolution: Product, 55%
	 Visible: 70%
         Needs: CRM, Platform
CRM:
	 Evolution: Product, 65%
	 Visible: 60%
         Needs: Compute
Platform:
	 Evolution: Product, 20%
	 Visible: 50%
         Needs: Compute
Compute:
	 Evolution: Product, 70%
	 Visible: 20%
         Needs: Data Center, Power
Data Center:
	 Evolution: Product, 25%
	 Visible: 15%
         Needs: Power
Power:
	 Evolution: Commodity, 30%
	 Visible: 10%"))

(def max-data-length 2048) ;; TODO

(defn trace [message res]
  (println (str "TRACE: " message res))
  res)

(defn is-node? [line]
  (if (nil? line)
    false
    (if (and
         (not (str/starts-with? line "\t"))
         (str/ends-with? line ":"))
      true
      false)))

(defn merge-other-key [key acc record]
  (let [acc-other (key acc [])
        new-record {:other (into [] (concat (:other acc-other) (:other (key record))))}]
    new-record))

(defn merge-nodes [node-vector]
  (loop [acc {}
         remaining node-vector]
    (if-let [record (first remaining)]
      (let [node (first (keys record))
            new-sub (if (nil? (:other (node record)))
                      (merge (node acc) (node record))
                      (merge (node acc) (merge-other-key node acc record)))
            new-acc (merge acc {node new-sub})]
        (recur new-acc (nthnext remaining 1)))
      acc)))

;; Split incoming text into lines
;; loop through lines, 
;; if its a node,
;;    put it in the map as a key
;;    save node as current key
;; if its not a node
;;    parse line and put it as a value on the current key

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn keywordize-node [line]
  (if (not-empty line)
    (keyword (str/replace (str/trim (str/replace line #":" "")) #" " "-"))
    nil))

(defn labelize-node [node]
  (str/replace (str/replace (name node) "#:" "") #"-" " "))

(def phases [:Genesis :Custom-Built :Product :Commodity])

(defmulti parse-content
  (fn [x] (first (keys x))))

(defmethod parse-content :Evolution [sub-node]
  (let [tokens (str/split (:Evolution sub-node) #", ")
        phase (keywordize-node (first tokens))
        x-axis (str/replace (last tokens) #"%" "")]
    (if (in? phases phase)
      {:Evolution {:phase phase
                   :x-axis x-axis}}
      {:other [(:original sub-node)]})))

(defmethod parse-content :Visible [sub-node]
  (let [y-axis (str/replace (str/trim (:Visible sub-node)) #"%" "")]
    {:Visible {:y-axis y-axis}}))

(defmethod parse-content :Needs [sub-node]
  (let [tokens (str/split (:Needs sub-node) #", ")
        nodes (into [] (remove nil? (map #(keywordize-node (str/trim %)) tokens)))]
    {:Needs {:links nodes}}))

(defmethod parse-content :Evolve [sub-node]
  (let [tokens (str/split (:Evolve sub-node) #", ")
        phase (keywordize-node (first tokens))
        x-axis (str/replace (last tokens) #"%" "")]
    (if (in? phases phase)
      {:Evolve {:phase phase
                   :x-axis x-axis}}
      {:other [(:original sub-node)]})))

(defmethod parse-content :default [sub-node]
  {:other [(:original sub-node)]})


(defn parse-sub-node [line node]
  (let [tokens       (str/split line #":")
        start-token  (keyword (keywordize-node (first tokens)))
        contents     (map str/trim (nthnext tokens 1))
        partial   {start-token (first (vec contents)) :original line}]
    {node (parse-content partial)}))


(defn parse-line [line parse-tree node]
  (let [clean-line (str/trimr line)]
    (if (is-node? clean-line)
      (let [node-keyword (keywordize-node clean-line)
            parse-tree (assoc parse-tree node-keyword {})]
        [parse-tree node-keyword])
      (if (not (empty? clean-line))
        (let [sub-node (parse-sub-node clean-line node)]
          [sub-node node])
        nil))))


(defn pre-process-text [text]
  (let [re-dup-new-lines (str/replace text #"\n\n" "\n" )]
    re-dup-new-lines))

(defn parse [text]
  (let [lines (str/split-lines (pre-process-text text))]
    (loop [acc-trees []
           remaining-lines lines
           node nil]
      (if-let [line (first remaining-lines)]
        (let [full-line-result (parse-line line {} node)
              line-result (first full-line-result)
              new-node (trace "new-node->" (last full-line-result))
              acc-trees (if (and (not (nil? new-node)) 
                                 (not (nil? line-result)))
                          (conj acc-trees line-result)
                          acc-trees)]
          (recur acc-trees (nthnext remaining-lines 1) new-node))
        (merge-nodes (trace "acc-trees -> " acc-trees))))))




(defn decode-qp-text [qp-text]
  (let [json (trace "decode->" (b64/decodeString qp-text))
        map (try
              (js->clj (.parse js/JSON json) :keywordize-keys true)
              (catch js/Error e
                (println "Error occured: " e)
                {:text qp-text}
                )
              )]
    (:text map)))

(defn encode-qp-text [text]
  (let [json (.stringify js/JSON (clj->js {:version "v1" :text text}))
        base64 (b64/encodeString json)]
    base64))


(defn populate-editor [text]
  {:parsed (parse text) :raw text :encoded (encode-qp-text text)})

;; The DB riiight down the bottom.

(def default-db
      {:name "re-frame"
       :editor {:parsed {} :raw ""}
       :window-size {:width js/window.innerWidth, :height js/window.innerHeight}})

(pre-process-text "Evolution: Product, 20%\n\n\t Visible: 50%\n Needs:")