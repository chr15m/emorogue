(ns emorogue.rotworker
  (:require ["rot-js/dist/rot.min.js" :as ROT]))

(js/console.log "Worker loaded.")

(def api
  {"generate-cellular-map"
   (fn [seed w h r opts]
     (js/console.log "worker: generate-cellular-map" seed w h r opts)
     (ROT/RNG.setSeed (str seed))
     (let [m (ROT/Map.Rogue. w h opts)
           map-shape (make-array nil w h)]
       (.create m (partial aset map-shape))
       ;(js/console.log map-shape (.getCorridors m) (.getRooms m))
       map-shape))
   "compute-field-of-view"
   (fn [tiles x y]
     (let [fov (ROT/FOV.RecursiveShadowcasting. (fn [x y] (aget tiles x y)))
           visibility-shape (js/Array.)]
       (.compute fov x y 20 (fn [x y r visibility]
                              (when visibility
                                (.push visibility-shape #js [x y]))))))})

(defn init []
  (js/self.addEventListener "message"
                            (fn [^js e]
                              (let [[uid call args] (.. e -data)
                                    result (apply (get api call) args)]
                                (js/console.log "result" result)
                                (js/postMessage (clj->js [uid result])))))
  (js/console.log "worker main")
  (js/postMessage (clj->js [nil "init"])))
