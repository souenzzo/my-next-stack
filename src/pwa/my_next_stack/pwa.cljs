(ns my-next-stack.pwa
  (:require [fulcro.client :as fc]
            [fulcro.client.network :as fcn]
            [goog.dom :as gdom]
            [fulcro.client.primitives :as fp]
            [fulcro.client.routing :as fr]
            [goog.object :as gobj]
            [fulcro.client.dom :as dom]
            [fulcro.client.mutations :as fm]))

(fm/defmutation app/login
  [{:keys [username]}]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st))))
  (remote [{:keys [ast state]}]
          (-> ast
              #_(fm/returning state Friends))))

(fp/defsc Login [this {:ui/keys [username]
                       ::keys   [page id]
                       :or      {username ""}}]
  {:query         [::id
                   :ui/username
                   ::page]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {::page ::login
                     ::id   ::login})}
  (dom/form
    {:style    {:display        "flex"
                :flexDirection  "column"
                :justifyContent "center"}
     :onSubmit (fn [e] (.preventDefault e)
                 (fp/transact! this `[(app/login ~{:username username})]))}
    (dom/input {:value    username
                :onChange #(fm/set-value! this :ui/username (-> % .-target .-value))})))


(fp/defsc Loading [this {::keys [page id]}]
  {:query         [::id
                   ::page]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {::page ::loading
                     ::id   ::loading})}
  "Loading")

(fr/defsc-router RootRouter [this {::keys [page id]}]
  {:router-targets {::loading Loading
                    ::login   Login}
   :ident          (fn [] [page id])
   :router-id      ::root-router
   :default-route  Login}
  "404")

(def ui-root-router (fp/factory RootRouter))

(fp/defsc Root [this {::keys [root-router]}]
  {:query         [{::root-router (fp/get-query RootRouter)}]
   :initial-state (fn [_]
                    {::root-router (fp/get-initial-state RootRouter _)})}
  (ui-root-router root-router))

(defn render
  [client]
  (let [target (gdom/getElement "app")]
    (fc/mount client Root target)))

(defonce app (atom nil))

(defn ^:export main
  []
  (let [anti-forgery-token (gobj/getValueByKeys js/document "body" "dataset" "antiForgeryToken")
        client (fc/make-fulcro-client
                 {:networking (fcn/fulcro-http-remote {:url                "/api"
                                                       :request-middleware (-> (fcn/wrap-csrf-token anti-forgery-token)
                                                                               (fcn/wrap-fulcro-request))})})]
    (reset! app (render client))))
