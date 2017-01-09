(ns babylone.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [babylone.events]
              [babylone.subs]
              [babylone.routes :as routes]
              [babylone.views :as views]
              [babylone.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/app-routes)
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
