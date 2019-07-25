(ns codegouvfr.server
  (:require [ring.util.response :as response]
            [clojure.java.io :as io]
            [clojure.instant :as inst]
            [org.httpkit.server :as server]
            [ring.middleware.reload :refer [wrap-reload]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [cheshire.core :as json]
            [clj-rss.core :as rss]
            [clj-http.client :as http])
  (:gen-class))

(defn wrap-middleware [handler]
  (-> handler
      wrap-reload))

(defn default-page []
  (assoc
   (response/response
    (io/input-stream
     (io/resource
      "public/index.html")))
   :headers {"Content-Type" "text/html; charset=utf-8"}))

(defn codegouvfr-latest []
  (json/parse-string
   (:body (http/get "https://api-codes-sources-fr.antoine-augusti.fr/api/stats/last_repositories"))
   true))

(defn rss-feed []
  (rss/channel-xml
   {:title       "Derniers dépôts de codes sources publics"
    :link        "https://code.eig-forever.org/latest.xml"
    :description "Derniers dépôts de codes sources publics"}
   (map (fn [item] {:title       (:nom item)
                    :link        (:repertoire_url item)
                    :description (:description item)
                    :author      (:organisation_nom item)
                    :pubDate     (inst/read-instant-date (:derniere_mise_a_jour item))})
        (codegouvfr-latest))))

(defn rss-page []
  (assoc
   (response/response (rss-feed))
   :headers {"Content-Type" "text/xml; charset=utf-8"}))

(defroutes routes
  (GET "/" [] (default-page))
  (GET "/latest.xml" [] (rss-page))
  (resources "/")
  (not-found "Not Found"))

(def app (wrap-middleware #'routes))

(defn -main [& args]
  (let [port 3000] ;; FIXME: make an environment variable
    (def server (server/run-server app {:port port}))))