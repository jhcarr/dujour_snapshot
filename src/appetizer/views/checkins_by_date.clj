(ns appetizer.views.checkins_by_date
  (:require [hiccup.core :as html]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [cheshire.core :as json])
  (:use [clojure.string :only (join)]
        [appetizer.models.db :only (db)]))

(defn get-unique-products
  [database]
  (let [sql-count-fn (keyword "COUNT(DISTINCT ip)")
        sql-date-fn (keyword "DATE(timestamp)")
        sql-query (sql/select [:product sql-date-fn sql-count-fn]
                              :checkins "WHERE version ~ '^\\d+\\.\\d+\\.\\d+$' GROUP BY product, DATE(timestamp) LIMIT 10000")]
    (jdbc/query database sql-query)))

(defn format-query-results []
  "Formats query for crossfilter in dc.js library."
  (let [results-as-string ()])
  [:script (format "var queryData = %s;"
                   (json/generate-string (get-unique-products (db))))])

(defn make-checkins-by-date []
  (enlive/emit*
    (enlive/at (enlive/html-resource "appetizer/views/layout.html")
               [:div#main]
               (enlive/content
                (concat
                 (enlive/html (format-query-results))
                 (enlive/html-resource "appetizer/views/checkins_by_date.html")))
               [:div#le_javascript]
               (enlive/content
                (concat
                 (enlive/html [:script {:src "/js/charts.js"} ])
                 (enlive/html [:script "$(document).ready(drawCheckins())"]))))))
