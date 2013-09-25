(ns dujour.views.layout
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.sql :as sql]
            [net.cgrand.enlive-html :as html]
            )
  (:use compojure.core
        [hiccup.core :only (html)]))

