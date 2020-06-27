(ns emorogue.rotclient)

(defonce callbacks (atom {}))

(defn init [callback]
 (let [w (js/Worker. "js/worker.js")]
   (js/console.log "Opening worker js.")
   (.addEventListener w "message"
                      (fn [e]
                        (js/console.log "rotworker message" e)
                        (let [[uid result] (.-data e)]
                          (if (= result "init")
                            (callback w)
                            (when (@callbacks uid)
                              ((@callbacks uid) (js->clj result))
                              (swap! callbacks dissoc uid))))))
   w))

(defn make-uid []
  (-> (js/Math.random) str (.split ".") .pop int))

(defn rpc [worker call args callback]
  (let [uid (make-uid)]
    (swap! callbacks assoc uid callback)
    (.postMessage worker (clj->js [uid call args]))))

#_ (js/setTimeout (fn []
                 (js/console.log "Triggering map generation")
                 (rpc "generate-cellular-map" [(js/Math.random) 1024 1024 0.5 {:connected true}] (fn [m] (js/console.log "got map:" (clj->js m))))
                 
                 )
               100) 
