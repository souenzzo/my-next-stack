(ns my-next-stack.client.ui
  (:require [fulcro.client.primitives :as fp]
            [fulcro.client.routing :as fr]
            [my-next-stack.client.page :as page]
            #?(:clj  [fulcro.client.dom-server :as dom]
               :cljs [fulcro.client.dom :as dom])))

(fr/defsc-router RootRouter [this {:PAGE/keys [page id]}]
  {:router-targets {:PAGE/loading page/Loading
                    :PAGE/home    page/Home
                    :PAGE/chat    page/PageChat
                    :PAGE/login   page/Login}
   :ident          (fn [] [page id])
   :router-id      :PAGE/root-router
   :default-route  page/Login}
  (dom/div "404"))

(def ui-root-router (fp/factory RootRouter))

(fp/defsc Root [this {:PAGE/keys [root-router]}]
  {:query         [{:PAGE/root-router (fp/get-query RootRouter)}]
   :initial-state (fn [_]
                    {:PAGE/root-router (fp/get-initial-state RootRouter _)})}
  (ui-root-router root-router))
