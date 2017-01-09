(ns babylone.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 :game
 (fn [db]
   (:game db)))

(re-frame/reg-sub
 :loader
 (fn [db _]
   (:loader db)))

(re-frame/reg-sub
 :controls
 (fn [db _]
   (:controls db)))

(re-frame/reg-sub
 :effect
 (fn [db _]
   (:effect db)))

(when babylone.config/debug?
  (re-frame/reg-sub
   :db
   (fn [db _]
     db)))
