(ns emorogue.rotworker
  (:require ["rot-js/dist/rot.min.js" :as ROT]))

(js/console.log "Worker loaded.")

(def api
  {"generate-cellular-map"
   (fn [seed w h r opts]
     (js/console.log "worker: generate-cellular-map" seed w h r opts)
     (ROT/RNG.setSeed (str seed))
     (let [m (ROT/Map.Digger. w h opts)
           map-lookup (atom [])
           doors-lookup (atom [])
           ;map-shape (make-array nil w h)
           _create (.create
                     m
                     (fn [x y v]
                       (when (< v 1)
                         (swap! map-lookup conj [x y]))
                       ;(js/console.log x y v)
                       ;(aset map-shape x y v)
                       
                       ))
           corridors (js->clj
                       (.map (.getCorridors m)
                             (fn [c]
                               (clj->js {:start [(aget c "_startX") (aget c "_startY")]
                                         :end [(aget c "_endX") (aget c "_endY")]
                                         :ends-with-wall (aget c "_endsWithAWall")}))))
           rooms (js->clj
                   (.map (.getRooms m)
                         (fn [c]
                           (let [doors (js->clj
                                         (.map (js/Object.entries (aget c "_doors"))
                                               (fn [[xy v]]
                                                 (js->clj (.split xy ",")))))]
                             (swap! doors-lookup concat doors)
                             (clj->js {:from [(aget c "_x1") (aget c "_y1")]
                                       :to [(aget c "_x2") (aget c "_y2")]
                                       :doors doors})))))]
       {:map @map-lookup
        :doors @doors-lookup
        :corridors corridors
        :rooms rooms}))
   "compute-field-of-view"
   (fn [tiles x y]
     (let [fov (ROT/FOV.RecursiveShadowcasting. (fn [x y] (aget tiles x y)))
           visibility-shape (js/Array.)]
       (.compute fov x y 20 (fn [x y r visibility]
                              (when visibility
                                (.push visibility-shape #js [x y]))))
       visibility-shape))})

(defn init []
  (js/self.addEventListener "message"
                            (fn [^js e]
                              (let [[uid call args] (.. e -data)
                                    result (apply (get api call) args)]
                                (js/console.log "result" result)
                                (js/postMessage (clj->js [uid result])))))
  (js/console.log "worker main")
  (js/postMessage (clj->js [nil "init"])))
