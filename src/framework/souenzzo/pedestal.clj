(ns souenzzo.pedestal
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.mime-type :as mime]
            [clojure.string :as string]
            [fulcro.client.dom-server :as dom]
            [io.pedestal.log :as log]
            [cognitect.transit :as transit]
            [clojure.core.async :as async]
            [fulcro.client.impl.protocols :as fcip]
            [fulcro.client.primitives :as fp]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.route :as route])
  (:import (org.eclipse.jetty.server.handler.gzip GzipHandler)
           (org.eclipse.jetty.servlet ServletContextHandler)
           (fulcro.tempid TempId)
           (com.cognitect.transit ReadHandler)
           (fulcro.transit TempIdHandler)))

(defn transit-type
  ([x] (transit-type x false))
  ([x verbose?]
   (when (and (string? x)
              (or (string/starts-with? x "application/transit")
                  (= x "*/*")))
     (cond
       (string/ends-with? x "+msgpack") :msgpack
       verbose? :json-verbose
       :else :json))))

(def transit-write-hanlers
  {TempId (new TempIdHandler)})

(def transit-read-handlers
  {"fulcro/tempid" (reify
                     ReadHandler
                     (fromRep [_ id] (TempId. id)))})

(defn pr-transit
  [type body]
  (fn pr-transit [out]
    (try
      (let [writer (transit/writer out type {:handlers transit-write-hanlers})]
        (transit/write writer body))
      (catch Throwable e
        (log/error :type type :body body :pr-transit e)))))


(def type->conten-type
  {:json         "application/transit+json"
   :json-verbose "application/transit+json"
   :msgpack      "application/transit+msgpack"
   :edn          "application/edn"})



(def write-body
  {:name  ::write-body
   :leave (fn leave-write-body
            [{{::keys           [verbose-transit?]
               {:strs [accept]} :headers} :request
              {:keys [body]}              :response
              :as                         ctx}]
            (cond
              (dom/element? body) (-> ctx
                                      (assoc-in [:response :headers "Content-Type"] "text/html")
                                      (assoc-in [:response :body] (str "<!DOCTYPE html>\n"
                                                                       (dom/render-to-str body)
                                                                       "\n")))
              :else (let [type (transit-type accept verbose-transit?)
                          response-body (if type
                                          (pr-transit type body)
                                          (pr-str body))
                          content-type (type->conten-type (or type :edn))]
                      (-> ctx
                          (assoc-in [:response :body] response-body)
                          (assoc-in [:response :headers "Content-Type"] content-type)))))})

(def index
  {:name  ::index
   :enter (fn [{{:keys [parser ui-index]} :request
                :keys                     [request]
                :as                       context}]
            (let [parser (parser)
                  props (async/<!! (parser request (fp/get-query ui-index)))]
              (assoc context :response {:body   (fcip/render (ui-index props))
                                        :status 200})))})

(def api
  {:name  ::api
   :enter (fn [{{:keys [edn-params transit-params parser]} :request
                :keys                                      [request]
                :as                                        context}]
            (let [query (or edn-params transit-params)
                  parser (parser)
                  result (parser request query)]
              (async/go
                (assoc context :response {:body   (async/<! result)
                                          :status 200}))))})

(def routes
  `#{["/" :get [write-body index]]
     ["/api" :post [write-body api]]})


(defn context-configurator
  "Habilitando gzip nas respostas"
  [^ServletContextHandler context]
  (let [gzip-handler (GzipHandler.)]
    (.addIncludedMethods gzip-handler (into-array ["GET" "POST"]))
    (.setExcludedAgentPatterns gzip-handler (make-array String 0))
    (.setGzipHandler context gzip-handler))
  context)

(defn env-interceptor
  [env]
  (->> (fn [ctx] (update ctx :request #(into env %)))
       (hash-map :name ::env-interceptor :enter)
       (interceptor/interceptor)))

(defn add-env-interceptor
  [env]
  (->> (fn [interceptors]
         (into [(env-interceptor env)]
               interceptors))
       (update env ::http/interceptors)))



(def content-security-policy-settings
  (string/join " " ["script-src"
                    "'self'"
                    "'unsafe-inline'"
                    "'unsafe-eval'"]))


(defn service
  [ctx & {:as opts}]
  (let [{:keys [env]
         :as   ctx} (into ctx opts)
        dev? (= env :dev)]
    (cond-> ctx
            :always (assoc ::http/enable-csrf {:body-params (body-params/default-parser-map :transit-options [{:handlers transit-read-handlers}])}
                           ::http/mime-types mime/default-mime-types
                           ::http/resource-path "public"
                           ::http/file-path "target/public"

                           ::http/container-options {:context-configurator context-configurator}
                           ::http/secure-headers {:content-security-policy-settings content-security-policy-settings}
                           ::http/routes routes)
            :always http/default-interceptors
            :always add-env-interceptor
            dev? (update ::http/routes (fn [routes]
                                         #(route/expand-routes routes)))
            dev? http/dev-interceptors)))
