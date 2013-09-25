(ns dujour.views.overview
  (:require [hiccup.core :as html]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.models.db :only (db)]))

(defn make-overview []
  (enlive/emit*
    (enlive/at (enlive/html-resource "dujour/views/layout.html")
               [:div#main]
               (enlive/content (enlive/html-resource "dujour/views/overview.html")))))
