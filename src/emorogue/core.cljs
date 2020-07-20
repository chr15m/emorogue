(ns emorogue.core
  (:require
    [nanoo.nanoo :refer [run add del]]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [emorogue.rotclient :as rot-worker]
    [emorogue.tables :refer [items-table sentence-table]]
    ["rot-js/dist/rot.min.js" :as rot]
    [clojure.core.async :refer [go <!]]))

; ***** functions ***** ;

(defn log [& args]
  (apply js/console.log (clj->js args)))

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
        (recur (conj chosen (nth b index))
               (if (= (count b) 1)
                 bag
                 (vec-remove index b))
               (dec n))
        chosen))))

(defn make-items [rng n available-tiles]
  (let [item-tiles (from-bag rng available-tiles n)
        items (from-bag rng items-table n)]
    (into {} (map vector item-tiles items))))

(defn make-sentences [rng n]
  (let [[verbs adjectives nouns subjects] (map #(from-bag rng (sentence-table %) n)
                                               [:verb :adjective :noun :subject])]
    (map (fn [i]
           (let [adjective (nth adjectives i)]
             (str
               (nth verbs i)
               (if (>= (.indexOf "aeiou" (first adjective)) 0)
                 " an "
                 " a ")
               adjective
               " "
               (nth nouns i)
               " on "
               (nth subjects i))))
         (range n))))

(defn move-to [pos & [attrs]]
  (let [x (* (second pos) 40)
        y (* (first pos) 40)]
    (assoc-in (or attrs {})
              [:style :transform]
              (str "translate(" x "px," y "px)"))))

; ***** events ***** ;

(defn make-level [state]
  (go
    (let [dungeon-map (<! (rot-worker/rpc (@state :rw) "generate-cellular-map" [(js/Math.random) 80 22 0.5 {:connected true}]))
          available-tiles (get dungeon-map "map")
          items (make-items rot/RNG 10 available-tiles)]
      (log "got map" dungeon-map)
      (log "available tiles" available-tiles)
      (log "items" items)
      (swap! state assoc-in [:game :map] dungeon-map)
      (swap! state assoc-in [:game :items] items))))

; ***** views ***** ;

(defn component-game-map [state m items]
  (log "game-map-render")
  [:div (move-to [-40 -11] {:on-click #(reset! state {})})
   (for [pos (get m "map")]
     [:i.twa.twa-1x (move-to pos {:class "tile twa-black-large-square"
                                  :key ["floor" pos]})])
   (for [[pos item] items]
     [:i.twa.twa-1x (move-to pos {:class (str "tile twa-" (item :name))
                                  :key ["item" pos]})])])

(defn component-title-screen [state]
  [:div#title
   [:h1 "need. coffee. stat."]
   [:p [:i.twa.twa-5x.twa-hot-beverage]]
   [:button.primary {:on-click (partial make-level state)} "Start"]])

(defn component-game [state]
  (let [m (-> @state :game :map)
        items (-> @state :game :items)]
    (if m
      [component-game-map state m items]
      [component-title-screen state])))

; ***** launch ***** ;

(defonce state (r/atom {}))

(defn ^:dev/after-load start []
  (log "start")
  (log (make-sentences rot/RNG 10))
  (rdom/render [component-game state] (js/document.getElementById "game")))

(defn main []
  (print "main")
  (rot-worker/init
    (fn [rw]
      (swap! state assoc :rw rw)
      (start))))

