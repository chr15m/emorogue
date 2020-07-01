(ns emorogue.rotclient
  (:require [clojure.core.async :refer [go chan put!]]))

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

(defn rpc [worker call args]
  (let [uid (make-uid)
        c (chan)]
    (go
      (swap! callbacks assoc uid #(put! c %))
      (.postMessage worker (clj->js [uid call args])))
    c))
