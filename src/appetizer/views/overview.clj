(ns appetizer.views.overview
  (:require [hiccup.core :as html]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [appetizer.models.db :only (db)]))

(defn make-overview []
  (enlive/emit*
    (enlive/at (enlive/html-resource "appetizer/views/layout.html")
               [:div#main]
               (enlive/content (enlive/html-resource "appetizer/views/overview.html")))))
