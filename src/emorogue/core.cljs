(ns emorogue.core
  (:require
    [nanoo.nanoo :refer [run add del]]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [emorogue.rotclient :as rot]))

(defonce state (r/atom {}))

(defn component-game-map [m]
  [:pre
       (for [row (range (count m)) col (range (count (first m)))]
         [:span {:key (str row "-" col)}
          [:i.twa.twa-1x {:class (case (-> m (nth row) (nth col))
                                   0 "twa-black-large-square"
                                   1 "" ;"twa-black-large-square"
                                   "")}]
          (when (= col (dec (count (first m)))) "\n")])])

(defn component-title-screen []
  [:div#title
   [:h1 "emorogue"]
   [:p [:i.twa.twa-5x.twa-skull]]
   [:button.primary {:on-click identity} "Start"]])

(defn component-game [state]
  (let [m (-> @state :game :map)]
    (if m
      [component-game-map m]
      [component-title-screen])))

(defn main []
  (print "main")
  (rdom/render [component-game state] (js/document.getElementById "game"))

  #_ (rot/init (fn [w]
              (rot/rpc w "generate-cellular-map" [(js/Math.random) 48 64 0.5 {:connected true}]
                       (fn [m]
                         (js/console.log "got map:" (clj->js m))
                         (swap! state assoc-in [:game :map] m))))))

