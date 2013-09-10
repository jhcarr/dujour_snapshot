(ns dujour.controllers.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [dujour.query :as query]
            [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            ))

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
