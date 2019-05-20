(ns my-next-stack.client.organism
  (:require [fulcro.client.primitives :as fp]
            #?(:clj  [fulcro.client.dom-server :as dom]
               :cljs [fulcro.client.dom :as dom])
            [fulcro.client.mutations :as fm]))

(fp/defsc Message [this {:app.message/keys [id body author-name]}]
  {:query [:app.message/id
           :app.message/author-name
           :app.message/body]
   :ident [:app.message/id :app.message/id]}
  (dom/li (pr-str [author-name body])))


(def ui-messages (fp/factory Message {:keyfn :app.message/id}))

(fp/defsc Chat [this {:app.chat/keys [id title messages]
                      :ui/keys       [body new-title]
                      :or            {body ""}}]
  {:query [:app.chat/id
           :app.chat/title
           :ui/new-title
           :ui/body
           {:app.chat/messages (fp/get-query Message)}]
   :ident [:app.chat/id :app.chat/id]}
  (fp/fragment
    (if new-title
      (let [on-submit (fn [e] (.preventDefault e)
                        (fp/transact! this `[(app.chat/new-title ~{:app.chat/id id :app.chat/title new-title})]))]
        (dom/form
          {:onSubmit on-submit}
          (dom/input {:value    new-title
                      :onBlur   on-submit
                      :onChange #(fm/set-value! this :ui/new-title (-> % .-target .-value))})))
      (dom/p {:onClick #(fm/set-value! this :ui/new-title title)} title))
    (map ui-messages messages)
    (dom/form
      {:onSubmit (fn [e] (.preventDefault e)
                   (fp/transact! this `[(app.message/send ~{:app.chat/id      id
                                                            :app.message/id   (fp/tempid)
                                                            :app.message/body body})]))}
      (dom/input {:value    body
                  :onChange #(fm/set-value! this :ui/body (-> % .-target .-value))}))))

(def ui-chat (fp/factory Chat))

(fp/defsc FriendLi [this {:app.user/keys [id username me?]}]
  {:query [:app.user/id
           :app.user/me?
           :app.user/username]
   :ident [:app.user/id :app.user/id]}
  (dom/li
    (dom/button {:disabled (boolean me?)
                 :onClick  #(fp/transact! this `[(app.chat/chat-with ~{:app.chat/id (fp/tempid)
                                                                       :app.user/id id})])}
                (str username))))

(def ui-friend-li (fp/factory FriendLi {:keyfn :app.user/id}))

(fp/defsc Navbar [this {:keys [app.user/username]}]
  {:query         [:app.user/username]
   :ident         (fn [] [::navbar ::navbar])
   :initial-state (fn [_] {})}
  (fp/fragment
    (dom/code username)
    (dom/button {:onClick #(fp/transact! this `[(app.session/exit ~{})])}
                "exit")))

(def ui-navbar (fp/factory Navbar))


(fp/defsc FriendsList [this {:keys [app.user/friends]}]
  {:query         [{:app.user/friends (fp/get-query FriendLi)}]
   :ident         (fn [] [::friends ::friends])
   :initial-state (fn [_]
                    {})}
  (dom/ul
    (map ui-friend-li friends)))


(def ui-friends-list (fp/factory FriendsList))
