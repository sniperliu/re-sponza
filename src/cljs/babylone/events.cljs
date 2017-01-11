(ns babylone.events
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljs.core.async :as async :refer [>! <! put! chan]]
            [babylone.db :as db]))

(re-frame/reg-cofx
 :now
 (fn [coeffects _]
   (assoc coeffects :now (js/Date.))))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/reg-fx
 :register-listener
 (fn [handlers]
   (doseq [[event handler] handlers]
     (.addEventListener js/window (name event) handler))))

(re-frame/reg-event-fx
 :install-engine
 [re-frame/debug re-frame/trim-v]
 (fn [{:keys [db]} [canvas]]
   (when-not (:engine db)
     (let [engine (js/BABYLON.Engine. canvas true #js { "limitDeviceRatio" 2 } true)]
       (println db)
       #_(set! engine.enableOfflineSupport true)
       {:db (-> db
                (assoc :engine engine))
        :dispatch [:load-config (:scene db)]
        ;;      :deregister-event-handler :install-engine
        :register-listener {:resize (fn [] (.resize engine))
                            :keydown (fn [evt] (re-frame/dispatch [:key-down evt]))}}))))



(re-frame/reg-event-fx
 :tick
 [(re-frame/inject-cofx :now)]
 (fn [{:keys [db now]} _]
   #_(println "Clock: " (mod (- now (:start db)) 1000) " - " (js/Math.round (/ (- now (:start db)) 1000)))
   {:db (-> db
            (update :ticks inc))}))

(defn load-config
  "load config file async, return a channel"
  [file-name]
  (let [out (async/chan)]
    (js/BABYLON.Tools.LoadFile
     file-name
     #(put! out (-> % (js/JSON.parse) (js->clj))))
    out))

(defn load-scene
  "load scene for engine on scene-path, return a triple with
   scene chan, progress chan and error channel."
  [engine scene-path scene-name]
  (let [scene-ch (chan)
        progress-ch (chan)
        error-ch (chan)]
    (.Load js/BABYLON.SceneLoader
           scene-path
           scene-name
           engine
           #(put! scene-ch %)
           #(put! progress-ch %)
           #(put! error-ch %))))

(re-frame/reg-event-fx
 :load-config
 (fn [cofx [_ config]]
   (let [out (load-config config)]
     {:db (:db cofx)
      :dispatch [:create-scene {:engine (-> cofx :db :engine)
                                :config out}]})))

(defn re-trigger-timer
  [handler] (reagent/next-tick handler))

;; refer later https://github.com/Day8/re-frame/wiki/Alternative-dispatch,-routing-&-handling
(re-frame/reg-event-db
 :update-progress
 (fn [db [_ {:keys [scene evt]}]]
   ;; FIXME use another handler to register/de-register dynamic event handler
   (cond
     scene (let [remaining (.getWaitingItemsCount scene)
                 percentage (+ 50 (* 50 (/ (- 78 remaining) 78)))]
             (println "percentage " percentage)
             (when (< percentage 100)
               (re-trigger-timer (fn [] (re-frame/dispatch [:update-progress {:scene scene}]))))
             (-> db
                 (assoc-in [:loader :percentage] percentage)
                 (assoc-in [:loader :background-opacity] (str (double (/ percentage 100))))))
     evt (let [percentage (if (.-lengthComputable evt)
                            (/ (* 50 (.-loaded evt)) (.-total evt))
                            (/ (* 50 (.-loaded evt)) (* 1024 1024 29.351353645324707)))]
           (println "evt percentage " percentage)
           (-> db
               (assoc-in [:loader :percentage] percentage)
               (assoc-in [:loader :background-opacity] (str (double (/ percentage 100)))))))))

;; pipe
(defn create-scene [engine config-in scene-out]
  (let [config (go (async/<! config-in))
          [scene-ch progress-ch error-ch]
          (load-scene engine (config "scenePath") (config "sceneName"))]
      (go (>! scene-out (<! scene-ch)))
      (go (re-frame/dispatch [:update-progress {:evt (<! progress-ch)}]))
      (go (println (str "error" (<! error-ch))))))

;; pipe
(defn start-scene [db scene-in ready-out]
  (go
    (let [scene (async/<! scene-in)]
      (println "start scene " scene)
      (println "cameras " (.-cameras scene))
      (re-frame/dispatch [:update-progress {:scene scene}])
      (set! (.-activeCamera scene) (aget (.-cameras scene) (:active-camera db)))
      (.executeWhenReady scene #(go (async/>! ready-out scene))))))

(re-frame/reg-event-fx
 :create-scene
 (fn [cofx [_ {:keys [engine config] :as input}]]
   (let [out (async/chan)]
     (create-scene engine config out)
     {:db (:db cofx)
      :dispatch [:before-render out]})))

(re-frame/reg-event-fx
 :before-render
 (fn [{:keys [db]} [_ scene]]
   (println "before render")
   (let [out (async/chan)]
     (start-scene db scene out)
     {:db db
      :dispatch [:render out]})))

(re-frame/reg-event-db
 :render
 (fn [{engine :engine :as db} [_ ch-scene]]
   (println "render")
   (go (let [scene (async/<! ch-scene)]
         (.runRenderLoop engine #(.render scene))
         (re-frame/dispatch [:start-game])))
    db))

(defn support-fullscreen? [canvas]
  (not (nil? (or (.-requestFullscreen canvas)
                 (.-mozRequestFullScreen canvas)
                 (.-webkitRequestFullscreen canvas)
                 (.-msRequestFullscreen canvas)))))

(re-frame/reg-event-fx
 :start-game
 [(re-frame/inject-cofx :now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (assoc :start now)
            (assoc :ticks 0)
            (assoc :clock (js/setInterval
                           #(re-frame/dispatch [:tick]) 1000))
            (update-in [:loader :hidden] (constantly true))
            (update-in [:controls :hidden] (constantly false))
            (assoc-in [:controls :fullscreen :support-fullscreen?] (-> db
                                                                       :engine
                                                                       (.getRenderingCanvas)
                                                                       support-fullscreen?)))}))

;; scene control
(re-frame/reg-fx
 :enter-fullscreen
 (fn [zone]
   (js/BABYLON.Tools.RequestFullscreen zone)))
(re-frame/reg-fx
 :exit-fullscreen
 (fn [_]
   (js/BABYLON.Tools.ExitFullscreen)))
(re-frame/reg-fx
 :show-debugger
 (fn [scene]
   (.show (.-debugLayer scene))))
(re-frame/reg-fx
 :hide-debugger
 (fn [scene]
   (.hide (.-debugLayer scene))))

(re-frame/reg-event-fx
 :key-down
 (fn [{:keys [db]} [_ evt]]
   (cond
     (and (.-ctrlKey evt)
          (.-shiftKey evt)
          (= (.-keyCode evt) 68)) {:db db
                                   :dispatch [:toggle-debug-layer]}
     :else {:db db})))

(re-frame/reg-event-fx
 :toggle-debug-layer
 (fn [{:keys [db]} _]
   (let [engine (:engine db)
         debug? (:debug? db)
         scene (aget (.-scenes engine) 0)]
     (cond-> {:db (update db :debug? not)}
       debug? (assoc :hide-debugger scene)
       (not debug?) (assoc :show-debugger scene)))))

(re-frame/reg-event-fx
 :toggle-full-screen
 (fn [{:keys [db]} _]
   (let [engine (:engine db)
         canvas (-> db :engine (.getRenderingCanvas))
         zone (.-parentNode canvas)
         fullscreen? (get-in db [:controls :fullscreen :fullscreen?])
         fx (cond-> {:db (update-in db [:controls :fullscreen :fullscreen?] not)}
              (not fullscreen?) (assoc :enter-fullscreen zone)
              fullscreen? (assoc :exit-fullscreen nil))]
     #_(.switchFullscreen engine true)
     fx)))
