(ns malai.smoke
  "Built into a binary by `jolt smoke:build` to check that inline path
   caching works in jolt build output: static, dynamic and nested paths,
   past the point where dynamic call sites compile."
  (:require [com.rpl.specter :as s]
            [com.rpl.specter.impl :as i]
            [malai.core :as m]))

(def data [{:a 1 :b 2} {:a 3 :b 4}])

(defn- nested-view [outer] (s/select [s/ALL (s/view (fn [m] (s/select [(s/keypath outer)] m)))] data))
(defn- with-collected [lim] (s/select [s/ALL (s/collect-one :a) (s/collected? [a] (> a lim)) :b] data))
(defn- in-loop [] (vec (for [k [:a :b :c]] (s/select [s/ALL (s/keypath k)] data))))
(defn- through-malai [k] (m/select [m/ALL (m/keypath k) (m/pred= 3)] data))

(defn- compiled-dynamic []
  (count (filter #(and (instance? com.rpl.specter.impl.CachedPathInfo %)
                       (i/cached-path-info-dynamic? %))
                 (vals @i/jolt-path-cache))))

(defn -main [& _]
  ;; 300 calls each, so dynamic sites cross jolt-compile-after and compile
  (let [run (fn [f] (last (repeatedly 300 f)))]
    (prn :nested-view (run #(nested-view :a)))
    (prn :collected (run #(with-collected 1)))
    (prn :loop (run in-loop))
    (prn :malai-core (run #(through-malai :a)))
    (prn :compiled-dynamic-sites (compiled-dynamic))))
