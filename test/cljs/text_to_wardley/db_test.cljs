(ns text-to-wardley.db-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test.check.clojure-test :as pt :include-macros true]
            [text-to-wardley.db :as db]
             [goog.crypt.base64 :as b64]
            ))

(enable-console-print!)


(pt/defspec test-encode-decode
  (prop/for-all [text gen/string]
                (let [encoded (db/encode-qp-text text)
                      decoded (db/decode-qp-text encoded)]
                  (= text decoded))))

;; (deftest encode-decode
;;   (testing "simple encode decode"
;;     (let [text "foo"
;;           encoded (db/encode-qp-text text)
;;           decoded (db/decode-qp-text encoded)
;;           ]
;;       (is (= text decoded)))))

(deftest online-photo-shop
  (testing "no starting node"
    (let [text "bar"
          output (db/populate-editor text)
          ]
      
      )
    )
  (testing "online photo shop happy path"
    (let [text db/starting-editor-value
          output (db/populate-editor text)
          parsed {:Power 
                  {:Evolution {:phase :Commodity, :x-axis "30"}, 
                   :Visible {:y-axis "10"}}, 
                  :CRM 
                  {:Evolution {:phase :Product, :x-axis "65"}, 
                   :Visible {:y-axis "60"}, 
                   :Needs {:links [:Compute]}}, 
                  :Compute 
                  {:Evolution {:phase :Product, :x-axis "70"}, 
                   :Visible {:y-axis "20"}, 
                   :Needs {:links [:Data-Center :Power]}}, 
                  :Data-Center 
                  {:Evolution {:phase :Product, :x-axis "25"}, 
                   :Visible {:y-axis "15"}, 
                   :Needs {:links [:Power]}}, 
                  :Online-Image-Manipulation 
                  {:Evolution {:phase :Custom-Built, :x-axis "10"}, 
                   :Visible {:y-axis "90"}, 
                   :Needs {:links [:Online-Photo-Storage]}}, 
                  :Customer 
                  {:Evolution {:phase :Custom-Built, :x-axis "65"}, 
                   :Visible {:y-axis "98"}, 
                   :Needs {:links [:Online-Image-Manipulation :Online-Photo-Storage :Web-Site :Print]}}, 
                  :Platform 
                  {:Evolution {:phase :Product, :x-axis "20"}, 
                   :Visible {:y-axis "50"}, 
                   :Needs {:links [:Compute]}}, 
                  :Online-Photo-Storage 
                  {:Evolution {:phase :Custom-Built, :x-axis "50"}, 
                   :Visible {:y-axis "80"}, 
                   :Needs {:links [:Web-Site]}}, 
                  :Web-Site 
                  {:Evolution {:phase :Product, :x-axis "55"}, 
                   :Visible {:y-axis "70"}, 
                   :Needs {:links [:CRM :Platform]}}, 
                  :Print 
                  {:Evolution {:phase :Product, :x-axis "45"}, 
                   :Visible {:y-axis "80"}, 
                   :Needs {:links [:Web-Site]}}
                  }
                  ]
      (is (= (:raw output) text))
      (is (= (:parsed output) parsed))
      )))


