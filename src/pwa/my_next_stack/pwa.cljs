(ns my-next-stack.pwa
  (:require [fulcro.client :as fc]
            [fulcro.client.network :as fcn]
            [goog.dom :as gdom]
            [fulcro.client.primitives :as fp]
            [fulcro.client.routing :as fr]
            [goog.object :as gobj]
            [fulcro.client.dom :as dom]
            [fulcro.client.mutations :as fm]))

(fp/defsc Message [this {:app.message/keys [id body]}]
  {:query [:app.message/id
           :app.message/body]
   :ident [:app.message/id :app.message/id]}
  (dom/li body))


(def ui-messages (fp/factory Message {:keyfn :app.message/id}))

(fp/defsc Chat [this {:app.chat/keys [id title messages]
                      :ui/keys       [body]}]
  {:query [:app.chat/id
           :app.chat/title
           :ui/body
           {:app.chat/messages (fp/get-query Message)}]
   :ident [:app.chat/id :app.chat/id]}
  (fp/fragment
    (dom/p title)
    (map ui-messages messages)
    (dom/form
      {:onSubmit (fn [e] (.preventDefault e)
                   (fp/transact! this `[(app.chat/send ~{:app.chat/id      id
                                                         :app.message/body body})]))}
      (dom/input {:value    body
                  :onChange #(fm/set-value! this :ui/body (-> % .-target .-value))}))))

(def ui-chat (fp/factory Chat))

(fm/defmutation app.user/chat
  [{:keys [app.user/id]}]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st
                             (fr/set-route* ::root-router [::chat id])))))
  (remote [{:keys [ast state]}]
          (-> ast
              (fm/returning state Chat))))

(fp/defsc PageChat [this {::keys   [page id]
                          :ui/keys [chat]}]
  {:query         [::page
                   [::id '_]
                   :ui/chat]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {::page ::chat
                     ::id   (fp/tempid)})}
  (ui-chat chat))

(fp/defsc FriendLi [this {:app.user/keys [id username]}]
  {:query [:app.user/id
           :app.user/username]
   :ident [:app.user/id :app.user/id]}
  (dom/li
    (dom/button {:onClick #(fp/transact! this `[(app.user/chat ~{:app.user/id id})])}
                username)))

(def ui-friend-li (fp/factory FriendLi {:keyfn :app.user/id}))

(fp/defsc FriendsList [this {:keys [app.user/friends]}]
  {:query         [{:app.user/friends (fp/get-query FriendLi)}]
   :ident         (fn [] [::friends ::friends])
   :initial-state (fn [_]
                    {})}
  (dom/ul
    (map ui-friend-li friends)))


(def ui-friends-list (fp/factory FriendsList))
(fm/defmutation app.user/login
  [{:app.user/keys [username]}]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st
                             (fr/set-route* ::root-router [::home ::home])))))
  (remote [{:keys [ast state]}]
          true
          (-> ast
              (fm/returning state FriendsList))))

(fm/defmutation app.session/exit
  [_]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st
                             (fr/set-route* ::root-router [::login ::login])))))
  (remote [_] true))

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
                :justifyContent "center"}
     :onSubmit (fn [e] (.preventDefault e)
                 (fp/transact! this `[(app.user/login ~{:app.user/username username})]))}
    (dom/input {:value    username
                :onChange #(fm/set-value! this :ui/username (-> % .-target .-value))})))

(fp/defsc Home [this {::keys   [page id]
                      :ui/keys [friends-list]}]
  {:query         [::id
                   ::page
                   {:ui/friends-list (fp/get-query FriendsList)}]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {::page           ::home
                     ::id             ::home
                     :ui/friends-list (fp/get-initial-state FriendsList _)})}
  (fp/fragment
    (dom/button {:onClick #(fp/transact! this `[(app.session/exit ~{})])}
                "exit")
    (ui-friends-list friends-list)))

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
                    ::home    Home
                    ::chat    PageChat
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
