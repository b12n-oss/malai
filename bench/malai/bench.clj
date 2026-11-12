(ns malai.bench
  "jolt bench: how long Specter paths take on this runtime, cached and not.
   Report only; it never fails."
  (:require [com.rpl.specter :as s]
            [malai.core :as m]))

(def data {:a [{:x 1 :y [1 2 3]} {:x 2 :y [4 5 6]}] :b [{:x 3 :y [7 8 9]}]})

(defn- static-site [] (s/transform [s/MAP-VALS s/ALL :y s/ALL even?] inc data))
(defn- dynamic-site [k] (s/select [s/MAP-VALS s/ALL (s/keypath k)] data))
(defn- malai-static [] (m/transform [m/MAP-VALS m/ALL :y m/ALL even?] inc data))
(defn- malai-dynamic [k] (m/select [m/MAP-VALS m/ALL (m/keypath k)] data))
(def ^:private compiled-once (s/comp-paths s/MAP-VALS s/ALL :y s/ALL even?))
(defn- hand-written []
  (update-vals data (fn [v] (mapv (fn [mm] (update mm :y (fn [ys] (mapv (fn [y] (if (even? y) (inc y) y)) ys)))) v))))

(defn- us-per-op [f n]
  (dotimes [_ 2000] (f))
  (let [t0 (System/nanoTime)]
    (dotimes [_ n] (f))
    (/ (- (System/nanoTime) t0) 1000.0 n)))

(defn- ms [f]
  (let [t0 (System/nanoTime)]
    (f)
    (/ (- (System/nanoTime) t0) 1e6)))

(defn -main [& _]
  (println "Warm, µs per call (2000 warm-up calls, so dynamic sites have compiled)")
  (doseq [[label f] [["static path" static-site]
                     ["dynamic path (keypath k)" #(dynamic-site :x)]
                     ["static, via malai.core" malai-static]
                     ["dynamic, via malai.core" #(malai-dynamic :x)]
                     ["compiled once by hand (comp-paths)" #(s/compiled-transform compiled-once inc data)]
                     ["hand-written update/mapv" hand-written]]]
    (println (format "  %-36s %8.2f" label (us-per-op f 50000))))
  (m/clear-path-cache!)
  (println "Cold, ms (after clear-path-cache!)")
  (println (format "  %-36s %8.3f" "static path, first call" (ms static-site)))
  (println (format "  %-36s %8.3f" "dynamic path, first call" (ms #(dynamic-site :x))))
  (m/clear-path-cache!)
  (println (format "  %-36s %8.3f" "short CLI run: 10 dynamic calls" (ms #(dotimes [_ 10] (dynamic-site :x)))))
  (m/clear-path-cache!)
  (println (format "  %-36s %8.3f" "256 dynamic calls (the compile one)" (ms #(dotimes [_ 256] (dynamic-site :x))))))
