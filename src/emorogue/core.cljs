(ns emorogue.core
  (:require
    [nanoo.nanoo :refer [run add del]]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [emorogue.rotclient :as rot-worker]
    ["rot-js/dist/rot.min.js" :as rot]))

; ***** functions ***** ;

; https://stackoverflow.com/questions/1394991/clojure-remove-item-from-vector-at-a-specified-location#1395274
(defn vec-remove
  "remove elem in coll"
  [pos coll]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn from-bag [rng bag quantity]
  (loop [chosen []
         b bag
         n quantity]
    (let [index (js/Math.floor (* (.getUniform rng) (count b)))]
      (if (and (> n 0) (> (count b) 0))
        (recur (conj chosen (nth b index)) (vec-remove index b) (dec n))
        chosen))))

; ***** events ***** ;

(defn start-game [state]
  (rot-worker/rpc (@state :rw) "generate-cellular-map" [(js/Math.random) 48 64 0.5 {:connected true}]
                  (fn [m]
                    (js/console.log "got map:" (clj->js m))
                    (swap! state assoc-in [:game :map] m))))

; ***** views ***** ;

(defn component-game-map [m]
  [:pre
       (for [row (range (count m)) col (range (count (first m)))]
         [:span {:key (str row "-" col)}
          [:i.twa.twa-2x {:class (case (-> m (nth row) (nth col))
                                   0 "twa-black-large-square"
                                   1 "" ;"twa-black-large-square"
                                   "")}]
          (when (= col (dec (count (first m)))) "\n")])])

(defn component-title-screen [state]
  [:div#title
   [:h1 "emorogue"]
   [:p [:i.twa.twa-5x.twa-skull]]
   [:button.primary {:on-click (partial start-game state)} "Start"]])

(defn component-game [state]
  (let [m (-> @state :game :map)]
    (if m
      [component-game-map m]
      [component-title-screen state])))

; ***** launch ***** ;

(defonce state (r/atom {}))

(defn ^:dev/after-load start []
  (js/console.log "start")
  (rdom/render [component-game state] (js/document.getElementById "game")))

(defn main []
  (print "main")
  (rot-worker/init
    (fn [rw]
      (swap! state assoc :rw rw)
      (start))))

