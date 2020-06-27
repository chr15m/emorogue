(ns emorogue.core
  (:require
    [nanoo.nanoo :refer [run add del]]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [emorogue.rotclient :as rot]))

(defn component-game [m]
  [:pre
   (for [row (range (count m)) col (range (count (first m)))]
     [:span {:key (str row "-" col)}
      [:i.twa.twa-1x {:class (case (-> m (nth row) (nth col))
                               0 "twa-black-large-square"
                               1 "" ;"twa-black-large-square"
                               "")}]
      (when (= col (dec (count (first m)))) "\n")])])

(defn main []
  (print "main")
  (rot/init (fn [w]
              (rot/rpc w "generate-cellular-map" [(js/Math.random) 48 64 0.5 {:connected true}]
                       (fn [m]
                         (js/console.log "got map:" (clj->js m))
                         (rdom/render [component-game m] (js/document.getElementById "game")))))))

