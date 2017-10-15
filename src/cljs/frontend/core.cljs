(ns frontend.core
  (:require-macros [frontend.macro :refer [foobar]])
  (:require [reagent.core :as r]
            [common.hello :refer [foo-cljc]]
            [foo.bar]
            [taoensso.sente :as sente]
            [taoensso.encore :as encore :refer-macros (have have?)]))

(enable-console-print!)

;; WS
(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk"                   ; Note the same path as before
                                  {:type :auto              ; e/o #{:auto :ajax :ws}
                                   })]
  (def chsk chsk)
  (def ch-chsk ch-recv)                                     ; ChannelSocket's receive channel
  (def chsk-send! send-fn)                                  ; ChannelSocket's send API fn
  (def chsk-state state)                                    ; Watchable, read-only atom
  )

;; Reagent application state
;; Defonce used to that the state is kept between reloads
(defonce app-state (r/atom {:y 2017}))

(defn join []
  (println "Sending osmus/join")
  (chsk-send! [:osmus/join {:had-a-callback? "nope"}]))

(defn shoot [])

(defn main []
  [:div
   [:canvas#canvas {:width "640" :height "480"}]
   [:button {:on-click join} "Join"]
   [:button {:on-click shoot} "Shoot"]
   [:h1 (foo-cljc (:y @app-state))]
   [:div.btn-toolbar
    [:button.btn.btn-danger
     {:type     "button"
      :on-click #(swap! app-state update :y inc)} "+"]
    [:button.btn.btn-success
     {:type     "button"
      :on-click #(swap! app-state update :y dec)} "-"]
    [:button.btn.btn-default
     {:type     "button"
      :on-click #(js/console.log @app-state)}
     "Console.log"]]])

(defmulti -event-msg-handler
          "Multimethod to handle Sente `event-msg`s"
          :id                                               ; Dispatch on event-id
          )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default                                                  ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (println "Unhandled event:" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (let [[old-state-map new-state-map] (have vector? ?data)]
    (if (:first-open? new-state-map)
      (println "Channel socket successfully established!:" new-state-map)
      (println "Channel socket state change:" new-state-map))))

(defmethod -event-msg-handler :chsk/recv
  [ev-msg]
  (println "Push event from server:" ev-msg))

(defmethod -event-msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}])

(defmethod -event-msg-handler :osmus/state
  [{:as ev-msg :keys [?data]}]
  (println "Got state msg:" ev-msg)
  (let [[?uid ?csrf-token ?handshake-data] ?data]))

(defmethod -event-msg-handler :osmus/test
  [ev-msg]
  (println "Got test msg:" ev-msg))

(defonce ws-router_ (atom nil))
(defn stop-ws-router! [] (when-let [stop-f @ws-router_] (stop-f)))
(defn start-ws-router! []
  (stop-ws-router!)
  (reset! ws-router_
          (sente/start-client-chsk-router!
            ch-chsk event-msg-handler)))

(defn start! []
  (js/console.log "Starting the app")
  (r/render-component [main] (js/document.getElementById "app")))

;; When this namespace is (re)loaded the Reagent app is mounted to DOM
(start!)

(start-ws-router!)

;; Macro test
(foobar :abc 3)

;; Example of interop call to plain JS in src/cljs/foo.js
(js/foo)

(comment
  (println "foo"))