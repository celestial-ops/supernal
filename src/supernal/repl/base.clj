(ns supernal.repl.base
  (:require
    [clojure.edn :as edn]
    [clojure.string :refer (split)]
    [clojure.tools.trace :as t]
    [clojure.core.async :refer (<!! thread thread-call) :as async]
    [clojure.java.io :refer (reader file)]
    [taoensso.timbre :refer (refer-timbre )]
    [pallet.stevedore.bash]
    [pallet.stevedore :refer (script)]
    [supernal.log :refer (collect-log get-logs gen-uuid)]
    [supernal.sshj :refer (execute)]))

(refer-timbre)

(.bindRoot #'pallet.stevedore/*script-language* :pallet.stevedore.bash/bash)

(defn execute-uuid [auth script host]
  (try
    (let [uuid (gen-uuid)
          code (execute script (merge {:host host} auth) :out-fn (collect-log uuid))]
       {:host host :code code :uuid uuid})
    (catch Throwable e
       {:host host :code :fail :error (.getMessage e)})))

(defn map-async
  "Map functions in seperate theads and merge the results"
  [f ms]
   (<!! (async/into [] (async/merge (map #(thread-call (bound-fn []  (f %))) ms)))))

(defn run-hosts [{:keys [auth hosts]} script]
  (let [results (map-async (partial execute-uuid auth script) hosts)
        grouped (group-by :code results)]
      {:hosts hosts :success (grouped 0) :failure (dissoc grouped 0)}))

(defmacro | [source fun & funs]
  (let [f (first fun) args (rest fun)]
     `(let [[this# res#] ~source]
        (~f this# res# ~@args))))

(defmacro run [f p s & fns]
  (if-not (empty? fns)
    `(run (~p ~f ~s) ~(first fns) ~(second fns) ~@(rest (rest fns)))
    `(~p ~f ~s)))

(defprotocol Shell
  (exec [this script])
  (rm [this target flags])
  (ls [this target flags])
  (grep [this expr flags])
  (cp [this src dest flags]))

(defprotocol Tracing
  (ping [this target]))

(defn zip
  "Collecting output into a hash, must be defined outside protocoal because of var args"
  [this {:keys [success failure] :as res} parent k & ks]
    (let [zipped (fn [{:keys [out] :as m}] (assoc-in m [parent k] (zipmap ks (split out #"\s"))))
          success' (map zipped (get-logs success))
          failure' (into {} (map (fn [[code rs]] [code (get-logs rs)]) failure))]
      [this (assoc (assoc res :success success') :failure failure')]))

(defprotocol Select
  (initialize [this])
  (pick [this m f]))

(defrecord Hosts [auth hosts]
  Shell
  (ls [this target flags]
   [this (run-hosts this (script ("ls" ~target ~flags)))])

  (exec [this script]
    [this (run-hosts this script)])

  Select
   (initialize [this]
    [this hosts])

  (pick [this {:keys [failure success] :as m} f]
    [(Hosts. auth (f success failure hosts)) {}])

  Tracing
  (ping [this target]
    [this (run-hosts this (script ("ping" "-c" 1 ~target)))]))

(defn successful
  "Used for picking successful"
  [success _ hs] (filter (into #{} (map :host success)) hs))

(defn into-hosts
   "builds hosts from an edn file" 
   [f]
   (let [{:keys [auth hosts]} (edn/read-string (slurp (file f)))]
     (Hosts. auth hosts)))

(defn refer-base []
  (require '[supernal.repl.base :as base :refer (run | initialize pick successful ping ls into-hosts exec)]))

