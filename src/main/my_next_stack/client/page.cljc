(ns my-next-stack.client.page
  (:require [fulcro.client.primitives :as fp]
            [my-next-stack.client.organism :as organism]
            [fulcro.client.routing :as fr]
            #?(:clj  [fulcro.client.dom-server :as dom]
               :cljs [fulcro.client.dom :as dom])
            [fulcro.client.mutations :as fm]))


(fp/defsc PageChat [this {:PAGE/keys [page id]
                          :ui/keys   [chat]}]
  {:query         [:PAGE/page
                   :PAGE/id
                   {:ui/chat (fp/get-query organism/Chat)}]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {:PAGE/page :PAGE/chat
                     :PAGE/id   :PAGE/chat
                     :ui/chat   (fp/get-initial-state organism/Chat _)})}
  (fp/fragment
    (dom/button {:onClick #(fp/transact! this `[(fr/set-route {:router :PAGE/root-router
                                                               :target [:PAGE/home :PAGE/home]})])} "<")
    (organism/ui-chat chat)))

(fp/defsc Login [this {:ui/keys   [username]
                       :PAGE/keys [page id]
                       :or        {username ""}}]
  {:query         [:PAGE/id
                   :ui/username
                   :PAGE/page]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {:PAGE/page :PAGE/login
                     :PAGE/id   :PAGE/login})}
  (dom/form
    {:style    {:display        "flex"
                :justifyContent "center"}
     :onSubmit (fn [e] (.preventDefault e)
                 (fp/transact! this `[(app.user/login ~{:app.user/id       (fp/tempid)
                                                        :app.user/username username})]))}
    (dom/input {:value    username
                :onChange #(fm/set-value! this :ui/username (-> % .-target .-value))})))

(fp/defsc Home [this {:PAGE/keys [page id]
                      :>/keys    [friends-list navbar]}]
  {:query         [:PAGE/id
                   :PAGE/page
                   {:>/navbar (fp/get-query organism/Navbar)}
                   {:>/friends-list (fp/get-query organism/FriendsList)}]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {:PAGE/page      :PAGE/home
                     :PAGE/id        :PAGE/home
                     :>/navbar       (fp/get-initial-state organism/FriendsList _)
                     :>/friends-list (fp/get-initial-state organism/FriendsList _)})}
  (fp/fragment
    (organism/ui-navbar navbar)
    (organism/ui-friends-list friends-list)))

(fm/defmutation app.user/login
  [{:app.user/keys [username id]}]
  (action [{:keys [state]}]
          (swap! state (fn [st]
                         (-> st
                             (assoc-in [:app.user/id id] {:app.user/id       id
                                                          :app.user/username username})
                             (fr/set-route* :PAGE/root-router [:PAGE/home :PAGE/home])))))
  (remote [{:keys [ast state]}]
          (-> ast
              (fm/returning state Home))))


(fp/defsc Loading [this {:PAGE/keys [page id]}]
  {:query         [:PAGE/id
                   :PAGE/page]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {:PAGE/page :PAGE/loading
                     :PAGE/id   :PAGE/loading})}
  (dom/div "Loading"))
