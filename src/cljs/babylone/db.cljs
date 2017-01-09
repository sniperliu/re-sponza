(ns babylone.db)

(def default-db
  {:name "re-frame"
   :scene "demo.json"
   :debug? false
   :interactive false
   :loader {:hidden false}
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
