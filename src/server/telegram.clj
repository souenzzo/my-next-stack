(ns server.telegram
  (:require [clojure.data.json :as json]
            [clojure.string :as string])
  (:import (clojure.lang Keyword)
           (java.net URLEncoder)
           (java.nio.charset StandardCharsets)))

(defprotocol IEncode
  (encode [this]))

(extend-protocol IEncode
  Number
  (encode [s]
    (encode (str s)))
  String
  (encode [s]
    (URLEncoder/encode s StandardCharsets/UTF_8))
  Keyword
  (encode [s]
    (encode (name s))))

(defn args->query
  [args]
  (if (empty? args)
    ""
    (string/join "&" (for [[k v] args]
                       (string/join "=" (map encode [k v]))))))

(defn method-uri
  ([token method] (method-uri token method nil))
  ([token method args]
   (let [uri (format "https://api.telegram.org/bot%s/%s" token (encode method))]
     (if (empty? args)
       uri
       (format "%s?%s" uri (args->query args))))))

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

(defn send-two-factor
  [token username text updates]
  (let [username->chat-id (updates->user-index updates)
        chat-id (username->chat-id username)]
    (when chat-id
      (method-uri token :sendMessage {:chat_id chat-id :text text}))))


(defn send-two-factor!
  [token username text]
  (let [updates (request! (method-uri token :getUpdates))
        send-message-uri (send-two-factor token username text updates)]
    (if-not send-message-uri
      ::fail
      (request! send-message-uri))))
