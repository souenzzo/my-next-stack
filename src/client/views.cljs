(ns client.views
  (:refer-clojure :exclude [list])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cognitect.transit :as t]
            [client.atoms :as a]))
;; utils
(defn index-by
  [f coll]
  (persistent!
    (reduce (fn [ret x]
              (let [k (f x)]
                (assoc! ret k x)))
            (transient {}) coll)))

;; view

(defn ui-todo-item
  [{:keys [db/id todo/text todo/done?]}]
  [a/ListItem
   {:key (str id)}
   [a/Checkbox {:onChange #(rf/dispatch [:done-todo id (-> % .-target .-checked)])
                :checked  done?}]
   [a/ListItemText {:primary text}]])

(defn page-todo
  [{:keys [app/todos todo.new/text]}]
  [a/Paper
   [a/Paper
    [a/TextField {:on-change #(rf/dispatch-sync [:todo.new/text (-> % .-target .-value)])
                  :value     text}]
    [a/Button {:on-click #(rf/dispatch [:add-todo text])} "+"]]
   [a/Paper
    [a/List (map ui-todo-item todos)]]])

(defn page-counter
  [{:keys [app/counter]}]
  [a/Paper {} (str counter) [a/Button {:on-click #(rf/dispatch [:app.counter/inc])} "+"]])

;; events

(rf/reg-event-db
  :todo.new/text
  (fn [db x]
    (apply assoc db x)))

(rf/reg-event-fx
  :app/page
  (fn [{:keys [db]} [_ page]]
    {:db       (assoc db :app/page page)
     :dispatch [page]}))

(rf/reg-event-db
  :fetch/todo
  (fn [db [_ {:keys [app/todos]}]]
    (let [todos-by-id (index-by :db/id todos)]
      (-> db
          (update :db/by-id merge todos-by-id)
          (assoc :app/todos (map :db/id todos))))))

(rf/reg-event-db
  :fetch/counter
  (fn [db [_ {:keys [app/counter]}]]
    (assoc db :app/counter counter)))

(rf/reg-event-fx
  :app.counter/inc
  (fn [{:keys [db]} _]
    {:db    (update db :app/counter (fnil inc 0))
     :fetch {:query `[(app.counter/inc)]
             :then  [:page/counter]}}))

(rf/reg-event-fx
  :done-todo
  (fn [{:keys [db]} [_ id done?]]
    {:db    (assoc-in db [:db/by-id id :todo/done?] done?)
     :fetch {:query `[(app/done-todo ~{:db/id      id
                                       :todo/done? done?})]
             :then  [:page/todo]}}))

(rf/reg-event-fx
  :add-todo
  (fn [{:keys [db]} [_ text]]
    (let []
      {:fetch {:query `[(app/new-todo ~{:todo/text text})]
               :then  [:page/todo]}})))

(rf/reg-event-fx
  :page/todo
  (fn [{:keys [db]} _]
    {:db    (assoc db :todo.new/text "")
     :fetch {:query [{:app/todos [:db/id :todo/text :todo/done?]}]
             :then  [:fetch/todo]}}))

(rf/reg-event-fx
  :page/counter
  (fn [{:keys [db]} _]
    {:fetch {:query [:app/counter]
             :then  [:fetch/counter]}}))

(rf/reg-event-db
  :user/username
  (fn [{:keys [db]} kv]
    (apply assoc db kv)))

(rf/reg-event-fx
  :app/login
  (fn [{{:keys [app/page]} :db
        :keys              [db]} [_ username]]
    {:fetch {:query `[{(app/login ~{:user/username username}) [:user/username :user/token]}]
             :then  [:fetch/login]}}))

(rf/reg-event-db
  :fetch/login
  (fn [db [_ {:syms [app/login]}]]
    (merge db login)))

;; subs

(rf/reg-sub
  :page/todo
  (fn [{:keys [db/by-id app/todos todo.new/text]} _]
    {:app/todos     (for [id todos]
                      (get by-id id))
     :todo.new/text text}))

(rf/reg-sub
  :page/counter
  (fn [{:keys [app/counter]} _]
    {:app/counter counter}))

(rf/reg-sub
  :app/page
  (fn [{:keys [app/page]
        :or   {page :page/todo}} _]
    page))
(rf/reg-sub
  :user/username
  (fn [{:keys [user/username]
        :or   {username ""}} _]
    username))
(rf/reg-sub
  :user/token
  (fn [{:keys [user/token]} _]
    token))

(rf/reg-event-db
  :app/logout
  (fn [db _]
    (dissoc db :user/token)))

(rf/reg-event-db
  :app/swap-menu
  (fn [db _]
    (update db :app/menu-status not)))


(rf/reg-sub
  :app/menu-status
  (fn [db _]
    (boolean (:app/menu-status db))))

;; "main" view

(def pages
  {:page/counter page-counter
   :page/todo    page-todo})

(defn root
  []
  (let [page @(rf/subscribe [:app/page])
        open-menu? @(rf/subscribe [:app/menu-status])
        username @(rf/subscribe [:user/username])
        token @(rf/subscribe [:user/token])
        data @(rf/subscribe [page])]
    [a/Paper
     [a/AppBar {:position :static}
      [a/Toolbar
       [a/IconButton
        {:on-click #(rf/dispatch [:app/swap-menu])}
        [a/MenuIcon]]
       [a/Select
        {:style     {:display :none}
         :open      open-menu?
         :on-close  #(rf/dispatch [:app/swap-menu])
         :value     (name page)
         :on-change #(rf/dispatch [:app/page (->> % .-target .-value (keyword "page"))])}
        (for [[k _] pages]
          [a/MenuItem {:key   (name k)
                       :value (name k)} (name k)])]
       [a/Typography {:style   {:flexGrow 1}
                      :variant :title
                      :color   :inherit} (name page)]
       (if token
         [a/Chip
          {:label username :on-delete #(rf/dispatch [:app/logout])}]
         [a/Paper
          [a/Input {:value     username
                    :on-change #(rf/dispatch-sync [:user/username (-> % .-target .-value)])}]
          [a/Button {:variant  :contained
                     :on-click #(rf/dispatch [:app/login username])
                     :color    :secondary} "Login"]])]]
     [(pages page) data]]))
