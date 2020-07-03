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

(defn available-tiles-from-map-tiles
  "make a list of positions with an available tile."
  [map-tiles]
  (vec (filter identity
          (for [row (range (count map-tiles))
                col (range (count (first map-tiles)))]
            (let [t (-> map-tiles (nth row) (nth col))]
              (if (= t 0)
                [col row]
                nil))))))

(defn make-items [rng n available-tiles]
  (let [item-tiles (from-bag rng available-tiles n)
        items (from-bag rng items-table n)]
    (hash-map (map vector item-tiles items))))

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

; ***** events ***** ;

(defn make-level [state]
  (go
    (let [map-tiles (<! (rot-worker/rpc (@state :rw) "generate-cellular-map" [(js/Math.random) 22 80 0.5 {:connected true}]))
          available-tiles (available-tiles-from-map-tiles map-tiles)
          items (make-items rot/RNG 10 available-tiles)]
      (log "got map" map-tiles)
      (log "available tiles" available-tiles)
      (log "items" items)
      (swap! state assoc-in [:game :map] map-tiles)
      (swap! state assoc-in [:game :items] items))))

; ***** views ***** ;

(defn component-game-map [m items]
  (log "game-map-render")
  ; TODO: more efficient to only draw squares with something on them
  [:pre
   (for [row (range (count m)) col (range (count (first m)))]
     [:span {:key (str row "-" col)}
      (let [pos [col row]
            floor (-> m (nth row) (nth col))
            item (-> items (get pos) :name)
            square (if item (str "twa-" item) "twa-black-large-square")]
        [:i.twa.twa-1x {:class (case floor
                                 0 square
                                 1 ""
                                 "")}])
      (when (= col (dec (count (first m)))) "\n")])])

(defn component-title-screen [state]
  [:div#title
   [:h1 "need. coffee. stat."]
   [:p [:i.twa.twa-5x.twa-hot-beverage]]
   [:button.primary {:on-click (partial make-level state)} "Start"]])

(defn component-game [state]
  (let [m (-> @state :game :map)
        items (-> @state :game :items)]
    (if m
      [component-game-map m items]
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

