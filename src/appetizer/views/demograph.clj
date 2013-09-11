(ns appetizer.views.demograph
  (:require [hiccup.core :as html]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [clojure.string :only (join)]
        [appetizer.models.db :only (db)]))

(defn get-unique-products
  [database]
  (let [sql-fn-keyword (keyword "COUNT(version)")
        sql-query (sql/select [:product :version sql-fn-keyword] :checkins "GROUP BY product, version")]
    (jdbc/query database sql-query)))

(defn format-query-results []
  "This section formats the query data for rendering d3."
  [:script (format "var queryData = [%s]"
                   (->> (get-unique-products (db))
                        (map :count)
                        (join ", ")))])

(defn make-demograph []
  (enlive/emit*
    (enlive/at (enlive/html-resource "appetizer/views/layout.html")
               [:div#main]
               (enlive/content
                (concat
                 (enlive/html (format-query-results))
                 (enlive/html-resource "appetizer/views/demograph.html")))
               [:div#le_javascript]
               (enlive/content
                (concat
                 (enlive/html [:script {:src "/js/demograph.js"}])
                 (enlive/html [:script "$(document).ready(drawDemograph())"]))))))
