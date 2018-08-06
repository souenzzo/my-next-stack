(ns client.views
  (:require [re-frame.core :as rf]
            [client.atoms :as a]
            [clojure.string :as string]
            [reagent.core :as r]))
;; utils
(defn index-by
  [f coll]
  (-> (fn [acc v] (assoc! acc (f v) v))
      (reduce (transient {}) coll)
      persistent!))

;; view

(defn ui-todo-item
  [{:keys [text done? on-change]}]
  [a/ListItem
   [a/Checkbox {:on-change #(on-change (-> % .-target .-checked))
                :checked   done?}]
   [a/ListItemText {:primary text}]])

(defn ui-new-todo
  [{:keys [text on-text on-add]}]
  [a/Paper
   [a/TextField {:on-change #(on-text (-> % .-target .-value))
                 :value     text}]
   [a/Button {:on-click #(on-add text)} "+"]])

(defn page-todo
  [{:keys [app/todos todo.new/text]}]
  [a/Paper
   [ui-new-todo {:text    text
                 :on-add  #(rf/dispatch [:add-todo text])
                 :on-text #(rf/dispatch-sync [:todo.new/text %])}]
   [a/Paper
    [a/List
     (for [{:keys [db/id todo/text todo/done?]} todos]
       [ui-todo-item {:key       id
                      :on-change #(rf/dispatch [:done-todo id %])
                      :text      text
                      :done?     done?}])]]])

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
  (fn [{:keys [db]} [_ v]]
    (assoc-in db [:app/auth :user/username] v)))

(rf/reg-event-fx
  :app/send-two-factor
  (fn [{{:keys [app/page]} :db
        :keys              [db]} [_ username]]
    {:fetch {:query `[(app/send-two-factor ~{:user/username username})]
             :then  [:fetch.ok/send-to-factor]}}))

(rf/reg-event-fx
  :fetch.ok/send-to-factor
  (fn [{:keys [db]} _]
    {:db       (assoc-in db [:app/auth :user/two-factor-in-progress?] true)
     :dispatch [:app/toast {:text "Checkout your telegram"}]}))

(rf/reg-event-db
  :app/toast
  (fn [db [_ {:keys [text]}]]
    (let [id (str (gensym "toast"))]
      (-> db
          (assoc-in [:db/by-id id] {:db/id id :toast/text text})
          (update :app/toast (fnil conj []) id)))))

(rf/reg-event-db
  :app.toast/remove
  (fn [db [_ id]]
    (-> db
        (update :db/by-id dissoc id)
        (update :app/toast (partial remove #{id})))))

(rf/reg-event-fx
  :app/login
  (fn [{{:keys [app/page]} :db
        :keys              [db]} [_ username two-factor]]
    {:fetch {:query `[{(app/login ~{:user/username username :user/two-factor two-factor}) [:user/username :user/token]}]
             :then  [:fetch/login]}}))

(rf/reg-event-db
  :fetch/login
  (fn [db [_ {:syms [app/login]}]]
    (update db :app/auth merge login {:user/authed? true})))

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
  :app/auth
  (fn [{:keys [app/auth]} _]
    auth))

(rf/reg-sub
  :app/toast
  (fn [{:keys [app/toast db/by-id]} _]
    (map by-id toast)))

(rf/reg-event-db
  :app/logout
  (fn [db _]
    (dissoc db :app/auth)))

(rf/reg-event-db
  :app/swap-menu
  (fn [db _]
    (update db :app/menu-status not)))

(rf/reg-event-db
  :user/two-factor
  (fn [db [_ v]]
    (assoc-in db [:app/auth :user/two-factor] v)))
(rf/reg-sub
  :app/menu-status
  (fn [db _]
    (boolean (:app/menu-status db))))

;; "main" view

(def pages
  {:page/counter page-counter
   :page/todo    page-todo})

(defn ui-auth
  [{:keys [user/authed?
           user/username
           user/two-factor
           user/two-factor-in-progress?]
    :or   {two-factor ""}}]
  (cond
    authed? [a/Chip
             {:label username :on-delete #(rf/dispatch [:app/logout])}]
    two-factor-in-progress? [a/Paper
                             [a/Input {:value     two-factor
                                       :on-change #(rf/dispatch-sync [:user/two-factor (-> % .-target .-value)])}]
                             [a/Button {:variant  :contained
                                        :disabled (string/blank? two-factor)
                                        :on-click #(rf/dispatch [:app/login username two-factor])
                                        :color    :secondary} "Confirm"]]
    :else [a/Paper
           [a/Input {:value     username
                     :on-change #(rf/dispatch-sync [:user/username (-> % .-target .-value)])}]
           [a/Button {:variant  :contained
                      :disabled (string/blank? username)
                      :on-click #(rf/dispatch [:app/send-two-factor username])
                      :color    :secondary} "Login"]]))


(defn root
  []
  (let [page @(rf/subscribe [:app/page])
        toasts @(rf/subscribe [:app/toast])
        open-menu? @(rf/subscribe [:app/menu-status])
        auth @(rf/subscribe [:app/auth])
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
       [ui-auth auth]]]
     (for [{:keys [db/id toast/text]} toasts
           :let [on-close #(rf/dispatch [:app.toast/remove id])]]
       [a/Snackbar {:key           id
                    :anchor-origin #js {:vertical   "bottom"
                                        :horizontal "left"}
                    :open          true
                    :message       (r/as-component [:span text])
                    :action        #js [(r/as-component [a/IconButton {:on-click on-close} "X"])]
                    :on-close      on-close}])
     [(pages page) data]]))
