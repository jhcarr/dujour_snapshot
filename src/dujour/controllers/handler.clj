(ns dujour.controllers.handler
  (:use [net.cgrand.enlive-html]
        [dujour.views.overview :only (make-overview)]
        [dujour.views.index :only (make-index)]
        [dujour.views.all_products :only (make-all-products)]
        [dujour.views.checkins_by_date :only (make-checkins-by-date)])
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [dujour.query :as query]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [dujour.views.layout :as layout]))

(defn query-app
  [db]
  (routes ;; All the checkins for a product where we do not care about multiple checkins in a day
          (GET "/product/:product" [product]
               (json/generate-string
                (jdbc/query db (query/product-query db product))))
          ;; Same as above but with a version filter as well
          (GET "/product/:product/version/:version" [product version]
               (json/generate-string
                (jdbc/query db (query/version-query db product version))))
          ;; Get the new users for a product between two dates
          (GET "/new/product/:product/between/:start-date/:end-date" [product start-date end-date]
               (json/generate-string
                (jdbc/query db (query/new-users-query db product start-date end-date))))))

(defn default-layout
  ;; Correct syntax for ampersand?
  [body]
  (->> body
       (html)
       (content)
       (at (html-resource "dujour/views/layout.html") [:#main])
       (emit*)))

;;Enlive
(defn dashboard-app
  []
  (routes (GET "/" [] (make-index))
          (GET "/overview" {} (make-overview))
          (GET "/all_products" {} (make-all-products))
          (GET "/checkins_by_date" {} (make-checkins-by-date))
          (GET "/about" [] (default-layout "FIX ME"))
          (GET "/contact" [] (default-layout "MORE FIX ME"))
          (route/resources "")
          (route/not-found "Not Found")))
