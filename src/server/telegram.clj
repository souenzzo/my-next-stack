(ns server.telegram
  (:require [clojure.core.async :as async]
            [clojure.data.json :as json])
  (:import (java.nio.charset StandardCharsets)
           (java.net URLEncoder)))

(defn encode
  [s]
  (URLEncoder/encode (str s) StandardCharsets/UTF_8))

(defn send-message
  [{::keys [token chat-id text]}]
  (format "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s"
          token (encode chat-id) (encode text)))

(defn get-updates
  [{::keys [token]}]
  (format "https://api.telegram.org/bot%s/getUpdates" token))

(defn request!
  [uri]
  (-> (slurp uri)
      (json/read-str :key-fn keyword)
      :result))

(defn updates->user-index
  [updates]
  (into {}
        (comp (map :message)
              (map :from)
              (remove :is_bot)
              (map (juxt :username :id)))
        updates))

(defn send-two-factor!
  [token username text]
  (let [updates (request! (get-updates {::token token}))
        username->chat-id (updates->user-index updates)
        chat-id (username->chat-id username)]
    (if-not chat-id
      ::fail
      (request! (send-message {::token token ::chat-id chat-id ::text text})))))
