(ns babylone.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljsjs.babylon]))

(defn loader-view []
  (let [progress (re-frame/subscribe [:loader])]    
    (fn []
      [:div#sponzaLoader (when (:hidden @progress) {:class "hidden"})
       [:div#backgroundImage {:style {:opacity (:background-opacity @progress)}}]
       [:div#loadingDetails
        [:div#loadingDetailsBackground]
        [:div#loadingPercentage
         [:div#loadingTitle "Sponza"]
         [:div#teamText "by Babylon.js"]
         [:div#textPercentage  (str (.toFixed (:percentage @progress)) "%")]
         [:div#progressContainer
          [:progress#loadingProgress {:value (:percentage @progress)
                                      :max "100"}]]
         [:div#streamingText "downloading scene"]]
        [:div#iOSTouchToStart.hidden "Touch to start"]]])))

(defn text-effect-view [effect]
  (fn []
    [:div {:style {:text-align :center
                   :font-family (:font effect)
                   ;; :font-style ""
                   :font-weight (:font-weight effect)
                   :font-size (:font-size effect)
                   :color (:color effect)
                   :opacity (:opacity effect)}
           :dangerouslySetInnerHTML {:__html (:text effect)}}]))

(defn effect-container []
  (let [effect (re-frame/subscribe [:effect])]
    (fn []
      [:div {:style {:position :absolute
                     :left "5%"
                     :right "5%"
                     :top "5%"
                     :bottom "5%"
                     :display :flex
                     :alignItems :center
                     :justifyContent :center
                     :pointerEvents :none}}
       #_[text-effect-view @effect]])))

(defn control-panel []
  (let [controls (re-frame/subscribe [:controls])]
    (fn []
      [:ul#controls (when (:hidden @controls) {:class "hidden"})
       [:li#soundButton.uiButton {;; :on-click (js/switchSound)
                                  :title "Turn off the volume"}]
       [:li#cameraButton.uiButton {;; :on-click (js/switchCamera)
                                   :title "Switch to interactive mode"}]
       [:li#fullscreenButton.uiButton (merge {:on-click #(re-frame/dispatch [:toggle-full-screen])
                                              :title "Switch to fullscreen"}
                                             (when-not (-> @controls :fullscreen :support-fullscreen?)
                                               {:class "hidden"}))]
       [:li#speakersButton.uiButton {;; :on-click (js/switchSpeakerType)
                                     :title "Switch to headphone mode"}]])))

(defn main-view []
  (fn []
    [:div#renderingZone
     [:canvas#renderCanvas {:ref (fn [elem] (when elem (re-frame/dispatch [:install-engine elem])))
                            ;; :touch-action :none
                            }]
     [control-panel]
     [effect-container]]))

(defn not-supported-view []
  (fn []
    [:div#notSupported.hidden "Sorry but your browser does not support WebGL..."]))
;; home

(defn home-panel []
  (fn []
    [:div {:style {:height "100%"
                   :width "100%"}}
     #_[loader-view]
     [main-view]
     [not-supported-view]]))


;; amazing

(defn babylon-inner [data]
  (reagent/create-class
   {:reagent-render (fn [] [:canvas#renderCanvas {:style {:width "100%"
                                                          :height "100%"
                                                          :touch-action :none}}])
    :component-did-mount (fn [this]
                           (let [canvas (reagent/dom-node this)
                                 engine (js/BABYLON.Engine. canvas true)
                                 game-data (clj->js data)
                                 scene (js/BABYLON.Scene. engine)
                                 camera (js/BABYLON.FreeCamera. :camera1 (js/BABYLON.Vector3. 0 5 -10) scene)
                                 light (js/BABYLON.HemisphericLight. :light1 (js/BABYLON.Vector3. 0 1 0) scene)
                                 sphere (js/BABYLON.Mesh.CreateSphere. :sphere1 16 2 scene)
                                 ground (js/BABYLON.Mesh.CreateGround. :ground1 6 6 2 scene)]
                             (println canvas)
                             (.addEventListener js/window "resize" (fn [] (.resize engine)))
                             (.addEventListener js/window "click" (fn [] (.switchFullscreen engine true)))
                             (doto camera
                               (.setTarget (js/BABYLON.Vector3.Zero.))
                               (.attachControl canvas false))
                             (set! sphere.position.y 1)
                             (.runRenderLoop engine (fn [] (.render scene)))))
    #_(comment :component-did-update (fn [this]
                                     (let [[_ data] (reagent/argv this)
                                           game-data (clj->js data)])))}))
;; main

(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :default [] [:div])

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      [show-panel @active-panel])))
