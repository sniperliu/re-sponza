(ns babylone.db)

(def default-db
  {:name "re-frame"
   :scene "demo.json"
   :debug? false
   :interactive false
   :active-camera 1
   :loader {:hidden false
            :items-to-stream 78
            :background-opacity 0
            :percentage 0}
   :controls {:hidden true
              :fullscreen {:support-fullscreen? false
                           :fullscreen? false}}
   :effect {:font "Century"
            :font-style ""
            :font-size "3.5vw"
            :font-weight "bold"
            :color "white"
            :opacity "0.8"
            :text "A Babylon.js<br/>Production"}})
