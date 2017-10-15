(ns frontend.core
  (:require-macros [frontend.macro :refer [foobar]])
  (:require [reagent.core :as r]
            [common.hello :refer [foo-cljc]]
            [foo.bar]
            [taoensso.sente :as sente]))

;; Reagent application state
;; Defonce used to that the state is kept between reloads
(defonce app-state (r/atom {:y 2017}))

(defn join [])
(defn shoot [])

(defn main []
  [:div
   [:canvas#canvas {:width "640" :height "480"}]
   [:button {:on-click join} "Join"]
   [:button {:on-click shoot} "Shoot"]
   [:h1 (foo-cljc (:y @app-state))]
   [:div.btn-toolbar
    [:button.btn.btn-danger
     {:type "button"
      :on-click #(swap! app-state update :y inc)} "+"]
    [:button.btn.btn-success
     {:type "button"
      :on-click #(swap! app-state update :y dec)} "-"]
    [:button.btn.btn-default
     {:type "button"
      :on-click #(js/console.log @app-state)}
     "Console.log"]]])

(defn start! []
  (js/console.log "Starting the app")
  (r/render-component [main] (js/document.getElementById "app")))

;; When this namespace is (re)loaded the Reagent app is mounted to DOM
(start!)

;; Macro test
(foobar :abc 3)

;; Example of interop call to plain JS in src/cljs/foo.js
(js/foo)

;; WS
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" ; Note the same path as before
                                  {:type :auto ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
  )

(comment
  (println "foo"))