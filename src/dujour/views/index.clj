(ns dujour.views.index
  (:require [hiccup.core :as html]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql])
  (:use [dujour.models.db :only (db)]))


(defn make-index []
  (enlive/emit*
    (enlive/at (enlive/html-resource "dujour/views/layout.html")
               [:div#main]
               (enlive/content (enlive/html
                                 [:h1 "Welcome to Dujour Dashboard."]
                                 [:a {:href "https://github.com/puppetlabs/dujour"}
                                  "This is dashboard is currently in development."])))))
