(ns appetizer.views.index
  (:require [hiccup.core :as html]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [appetizer.models.db :only (db)]))


(defn make-index []
  (enlive/emit*
    (enlive/at (enlive/html-resource "appetizer/views/layout.html")
               [:div#main]
               (enlive/content (enlive/html
                                 [:h1 "Appetizer is the dashboard for Dujour"]
                                 [:p "This is a temporary dashboard."])))))
