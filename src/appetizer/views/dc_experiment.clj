(ns appetizer.views.dc_experiment
  (:require [hiccup.core :as html]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [cheshire.core :as json])
  (:use [clojure.string :only (join)]
        [appetizer.models.db :only (db)]))

(defn get-unique-products
  [database]
  (let [sql-fn-keyword (keyword "COUNT(version)")
        sql-query (sql/select [:product :version sql-fn-keyword] :checkins "GROUP BY product, version")]
    (jdbc/query database sql-query)))

(defn get-checkins
  [database]
  (let [sql-query (sql/select [:product :version :timestamp :ip] :checkins)]
    (jdbc/query database sql-query)))

(defn format-query-results []
  "Formats query for crossfilter in dc.js library."
  (let [results-as-string ()])
  [:script (format "var queryData = %s;"
                   (json/generate-string (get-checkins (db))))])

(defn make-dc-experiment []
  (enlive/emit*
    (enlive/at (enlive/html-resource "appetizer/views/layout.html")
               [:div#main]
               (enlive/content
                (concat
                 (enlive/html (format-query-results))
                 (enlive/html-resource "appetizer/views/dc_experiment.html")))
               [:div#le_javascript]
               (enlive/content
                (concat
                 (enlive/html [:script {:src "/js/dc_experiment.js"} ])
                 (enlive/html [:script "$(document).ready(draw())"]))))))
