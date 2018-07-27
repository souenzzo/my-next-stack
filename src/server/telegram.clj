(ns server.telegram
  (:require [clojure.data.json :as json]
            [clojure.string :as string]
            [clojure.spec.alpha :as s])
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

(s/fdef encode
        :args (s/cat :s (s/or :string string?
                              :number number?
                              :keyword keyword?))
        :ret string?)

(defn args->query
  [args]
  (if (empty? args)
    ""
    (string/join "&" (for [[k v] args]
                       (string/join "=" (map encode [k v]))))))

(s/def ::query-args
  (s/nilable (s/map-of keyword? (s/or :string string?
                                      :number number?))))

(s/fdef args->query
        :args (s/cat :args ::query-args)
        :ret string?)


(defn method-uri
  ([token method] (method-uri token method nil))
  ([token method args]
   (let [uri (format "https://api.telegram.org/bot%s/%s" token (encode method))]
     (if (empty? args)
       uri
       (format "%s?%s" uri (args->query args))))))

(s/fdef method-uri
        :args (s/cat :token string?
                     :method keyword?
                     :args (s/? ::query-args))
        :ret string?)

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

(s/fdef updates->user-index
        :args (s/cat :updates ::updates)
        :fn (fn [{:keys [args ret]}] (>= (count (:updates args))
                                         (count ret)))
        :ret (s/map-of ::username ::id))

(defn send-two-factor
  [token username text updates]
  (let [username->chat-id (updates->user-index updates)
        chat-id (username->chat-id username)]
    (when chat-id
      (method-uri token :sendMessage {:chat_id chat-id :text text}))))

(s/def ::is_bot boolean?)
(s/def ::username string?)
(s/def ::id number?)
(s/def ::from
  (s/keys :req-un [::username ::id]
          :opt-un [::is_bot]))
(s/def ::message
  (s/keys :req-un [::from]))
(s/def ::update
  (s/keys :req-un [::message]))
(s/def ::updates
  (s/coll-of ::update))

(s/fdef send-two-factor
        :args (s/cat :token string?
                     :username string?
                     :text string?
                     :updates ::updates)
        :ret (s/nilable string?))


(defn send-two-factor!
  [token username text]
  (let [updates (request! (method-uri token :getUpdates))
        send-message-uri (send-two-factor token username text updates)]
    (if-not send-message-uri
      ::fail
      (request! send-message-uri))))
