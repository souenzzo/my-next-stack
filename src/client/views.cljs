(ns client.views
  (:refer-clojure :exclude [list])
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cognitect.transit :as t]
            [material-ui.core :as m]))
;; utils
(defn index-by
  [f coll]
  (persistent!
    (reduce (fn [ret x]
              (let [k (f x)]
                (assoc! ret k x)))
            (transient {}) coll)))


;; ui
(def button (r/adapt-react-class m/Button))
(def list (r/adapt-react-class m/List))
(def list-item (r/adapt-react-class m/ListItem))
(def checkbox (r/adapt-react-class m/Checkbox))
(def list-item-text (r/adapt-react-class m/ListItemText))
(def text-field (r/adapt-react-class m/TextField))

;; IO
(def writer (t/writer :json))
(def reader (t/reader :json))

(def headers #js{:Content-Type "application/transit+json"
                 :Accept       "application/transit+json"})

(def api "http://localhost:8080/api")

(rf/reg-fx
  :fetch
  (fn [{:keys [query then]}]
    (let [fetch (.fetch js/window api #js {:method  "POST"
                                           :headers headers
                                           :body    (t/write writer query)})]
      (.then (.then fetch (fn [res] (.text res))) (fn [text]
                                                    (rf/dispatch-sync (conj then (t/read reader text))))))))

;; view

(defn ui-todo-item
  [{:keys [db/id todo/text todo/done?]}]
  [list-item
   {:key (str id)}
   [checkbox {:onChange #(rf/dispatch [:done-todo id (-> % .-target .-checked)])
              :checked  done?}]
   [list-item-text {:primary text}]])

(defn page-todo
  [{:keys [app/todos todo.new/text]}]
  [:div
   [text-field {:on-change #(rf/dispatch-sync [:todo.new/text (-> % .-target .-value)])
                :value     text}]
   [button {:on-click #(rf/dispatch [:add-todo text])} "+"]
   [:hr]
   [list (map ui-todo-item todos)]])

;; events

(rf/reg-event-db
  :todo.new/text
  (fn [db x]
    (apply assoc db x)))

(rf/reg-event-db
  :fetch/todo
  (fn [db [_ {:keys [app/todos]}]]
    (let [todos-by-id (index-by :db/id todos)]
      (-> db
          (update :db/by-id merge todos-by-id)
          (assoc :app/todos (map :db/id todos))))))

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

;; subs

(rf/reg-sub
  :page/todo
  (fn [{:keys [db/by-id app/todos todo.new/text]} _]
    {:app/todos     (for [id todos]
                      (get by-id id))
     :todo.new/text text}))


;; "main" view

(def pages
  {:page/todo page-todo})

(defn root
  []
  (let [page :page/todo
        data @(rf/subscribe [page])]
    [(pages page) data]))
