(ns text-to-wardley.db
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pp]
   [clojure.set :as set]
   ))

(def starting-editor-value
  (str
   "Customer:\n"
   "\t Evolution: Custom Built, 65%\n"
   "\t Visible: 100%\n"
   "\t Needs: Online Image Manipulation, Online Photo Storage\n"
   "\n\n"
   "Online Image Manipulation:\n"
   "\n\n"
   "Online Photo Storage:\n"
   "\n\n"
   "Print:\n"
   "\n\n"
   "Web Site:\n"
   "\n\n"
   "CRM:\n"
   "\n\n"
   "Platform:\n"
   "\n\n"
   "Compute:\n"
   "\n\n"
   "Data Centre:\n"
   "\n\n"
   "Power:\n"))


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
            new-acc (merge acc {node new-sub})
            ]
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

(defmulti parse-content
  (fn [x] (first (keys x))))

(defmethod parse-content :Evolution [sub-node]
  (let [tokens (str/split (:Evolution sub-node) #", ")
        phase (first tokens)
        x-axis (last tokens)]
    {:Evolution {:phase "custom build"
                 :x-axis "50%"}}))

(defmethod parse-content :default [sub-node]
  {:other [(:original sub-node)]})

(defn keywordize-node [line]
  (keyword (str/replace (str/trim (str/replace line #":" "")) #" " "-")))


(defn parse-sub-node [line node]
  (let [tokens       (str/split line #":")
        start-token  (keyword (keywordize-node (first tokens)))
        contents     (map str/trim (nthnext tokens 1))
        partial   {start-token (first (vec contents)) :original line}
        ]
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


(defn parse [text]
  (let [lines (str/split-lines text)]
    (loop [acc-trees []
           remaining-lines lines
           node nil
           ]
      (if-let [line (first remaining-lines)]
        (let [full-line-result (parse-line line {} node)
              line-result (first full-line-result)
              new-node (last full-line-result)
              acc-trees (if (not (nil? line-result))
                          (conj acc-trees line-result)
                          acc-trees)]
          (recur acc-trees (nthnext remaining-lines 1) new-node))
        (let [_ (println acc-trees)]
          (merge-nodes acc-trees))))))

(defn populate-editor [text]
  {:parsed (parse text) :raw text})


;; The DB riiight down the bottom.

(def default-db
  {:name "re-frame"
   :editor (populate-editor starting-editor-value)})