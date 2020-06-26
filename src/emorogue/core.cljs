(ns emorogue.core
  [:require
   [nanoo.nanoo :refer [run add del]]
   ["hyperscript" :as h]])

(defn render []
  (aset 
    (js/document.getElementById "game")
    "innerHTML"
    (aget
      (h "pre" nil
         (h "i.twa.twa-2x.twa-ghost")
         (h "i.twa.twa-2x.twa-white-large-square")
         (h "i.twa.twa-2x.twa-black-large-square")
         "\n"
         (h "i.twa.twa-2x.twa-white-large-square")
         (h "i.twa.twa-2x.twa-white-large-square")
         (h "i.twa.twa-2x.twa-black-large-square"))
      "outerHTML")))

(defn reload! []
  (print "reload")
  (render))

(defn main! []
  (print "main")
  (reload!))

