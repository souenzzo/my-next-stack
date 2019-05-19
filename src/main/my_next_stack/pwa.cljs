(ns my-next-stack.pwa
  (:require [fulcro.client :as fc]
            [fulcro.client.network :as fcn]
            [goog.dom :as gdom]
            [fulcro.client.routing :as fr]
            [my-next-stack.client.ui :as ui]
            [goog.object :as gobj]
            [my-next-stack.client.organism :as organism]
            [fulcro.client.mutations :as fm]))

(fm/defmutation app.message/send
  [{tempid :app.message/id
    :keys  [app.message/body
            app.chat/id]}]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st
                             (assoc-in [:app.message/id tempid] {:app.message/id   tempid
                                                                 :app.message/body body})
                             (update-in [:app.chat/id id :app.chat/messages] conj [:app.message/id tempid])

                             (assoc-in [:app.chat/id id :ui/body] "")))))
  (remote [{:keys [ast state]}]
          (-> ast
              (fm/returning state organism/Message))))

(fm/defmutation app.chat/new-title
  [{:keys [app.chat/id]}]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st
                             (assoc-in [:app.chat/id id :ui/new-title] nil)))))
  (remote [{:keys [ast state]}]
          (-> ast
              (fm/returning state organism/Chat))))

(fm/defmutation app.chat/chat-with
  [{:keys [app.chat/id]}]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st
                             (assoc-in [:app.chat/id id] {:app.chat/id id})
                             (assoc-in [:PAGE/chat :PAGE/chat :ui/chat] [:app.chat/id id])
                             (fr/set-route* :PAGE/root-router [:PAGE/chat :PAGE/chat])))))
  (remote [{:keys [ast state]}]
          (-> ast
              (fm/returning state organism/Chat))))

(fm/defmutation app.session/exit
  [_]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st
                             (fr/set-route* :PAGE/root-router [:PAGE/login :PAGE/login])))))
  (remote [_] true))

(defn render
  [client]
  (let [target (gdom/getElement "app")]
    (fc/mount client ui/Root target)))

(defonce app (atom nil))

(defn ^:export main
  []
  (let [anti-forgery-token (gobj/getValueByKeys js/document "body" "dataset" "antiForgeryToken")
        request-middleware (-> (fcn/wrap-csrf-token anti-forgery-token)
                               (fcn/wrap-fulcro-request))
        networking (fcn/fulcro-http-remote {:request-middleware request-middleware})
        client (fc/make-fulcro-client {:networking networking})]
    (reset! app (render client))))

(gobj/set js/window "onload" main)
