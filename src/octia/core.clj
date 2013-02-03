(ns octia.core
  (:require [octia.compojure-adapter :as compojure-adapter]
            [octia.endpoint          :as endpoint]
            [octia.doc               :as doc])
  (:refer-clojure :exclude [get]))

(def default-group
  {:path ""
   :opts {}})

(def ^{:dynamic true} *group* default-group)

(defmacro group
  [path {:keys [doc wrappers] :as opts} & body]
  `(reify
     endpoint/Endpoint
       (doc [this] ~doc)
       (path [this] ~path)
       (sub-endpoints [this]
         (binding [*group* {:path ~path
                             :opts ~opts}] (vector ~@body)))
     clojure.lang.IFn
       (invoke [this request#]
         (binding [*group* {:path ~path
                             :opts ~opts}]
            (some #(% request#) (vector ~@body))))))

(defn endpoints->handler
  "Combine several endpoints into one handler"
  [& endpoints]
  (reify
     endpoint/Endpoint
       (sub-endpoints [this] endpoints)
     clojure.lang.IFn
       (invoke [this request]
         (some #(% request) endpoints))))

(defmacro endpoint
  "Generate an endpoint"
  [method path {:keys [doc wrappers] :as opts} args & body]
  `(let [path# (-> *group* :path (str ~path))
         wrappers# (-> *group* :opts :wrappers (or []) (concat ~wrappers))
         route-fn# (compojure-adapter/m-compile-route ~method path# wrappers# ~args ~body)]
     (reify
       endpoint/Endpoint
         (doc [this] ~doc)
         (path [this] path#)
         (method [this] ~method)
         (sub-endpoints [this] [])
       clojure.lang.IFn
         (invoke [this request#] (route-fn# request#)))))

(defmacro get
  [path {:keys [doc wrappers] :as opts} args & body]
  `(endpoint :get ~path ~opts ~args ~@body))

(defmacro post
  [path {:keys [doc wrappers] :as opts} args & body]
  `(endpoint :post ~path ~opts ~args ~@body))

(defmacro put
  [path {:keys [doc wrappers] :as opts} args & body]
  `(endpoint :put ~path ~opts ~args ~@body))

(defmacro delete
  [path {:keys [doc wrappers] :as opts} args & body]
  `(endpoint :delete ~path ~opts ~args ~@body))
