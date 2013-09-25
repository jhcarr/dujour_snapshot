(ns dujour.views.all_products
  (:require [hiccup.core :as html]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.models.db :only (db)]))

(defn get-unique-products
  [database]
  (let [sql-fn-keyword (keyword "COUNT(version)")
        sql-query (sql/select [:product :version sql-fn-keyword] :checkins "GROUP BY product, version")]
    (jdbc/query database sql-query)))

(defn make-table-products []
  [:div {:class "checkin-table"} [:table {:class "table table-striped"}
   [:thead [:th "Product"] [:th "Version"] [:th "Checkins"]]
   [:tbody
    (for [product (get-unique-products (db))]
      [:tr {}
       [:td (product :product)]
       [:td (product :version)]
       [:td (product :count)]])]]])

(defn make-all-products []
  (enlive/emit*
    (enlive/at (enlive/html-resource "dujour/views/layout.html")
               [:div#main]
               (enlive/content (enlive/html (make-table-products))
                               (enlive/html-resource "dujour/views/all_products.html")))))
