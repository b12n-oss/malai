(ns malai.caching-test
  "Paths whose locals are tricky for inline caching: nested selects inside
   view fns, collected?, loops, self-reference, shadowing and closures.
   Expected values come from the JVM (Specter 1.1.4); each fn is called
   several times so the cached route runs, not just the first call."
  (:require [clojure.test :refer [deftest is]]
            [com.rpl.specter :as s :refer [select transform select-one ALL MAP-VALS
                                           keypath view collected? collect-one must]]))

(def data [{:a 1 :b 2} {:a 3 :b 4}])

(defn- nested-view [outer] (select [ALL (view (fn [m] (select [(keypath outer)] m)))] data))
(defn- with-collected [lim] (select [ALL (collect-one :a) (collected? [a] (> a lim)) :b] data))
(defn- in-loop [] (vec (for [k [:a :b :c]] (select [ALL (keypath k)] data))))
(defn- in-doseq [] (let [acc (atom [])] (doseq [k [:b :a]] (swap! acc conj (select [ALL (must k)] data))) @acc))
(defn- self-ref [n] (if (pos? n) (select-one [(view (fn [_] (self-ref (dec n))))] n) :bottom))
(defn- shadow [] (let [ALL :a] (select [s/ALL ALL] data)))
(defn- closure-fn [f] (transform [ALL MAP-VALS] f data))

(defn- thrice [f] (vec (repeatedly 3 f)))

(deftest nested-view-sees-outer-locals
  (is (= (repeat 3 [[1] [3]]) (thrice #(nested-view :a))))
  (is (= (repeat 3 [[2] [4]]) (thrice #(nested-view :b)))))

(deftest collected-over-a-local
  (is (= (repeat 3 [[3 4]]) (thrice #(with-collected 1))))
  (is (= (repeat 3 [[1 2] [3 4]]) (thrice #(with-collected 0)))))

(deftest paths-in-loops
  (is (= (repeat 3 [[1 3] [2 4] [nil nil]]) (thrice in-loop)))
  (is (= (repeat 3 [[2 4] [1 3]]) (thrice in-doseq))))

(deftest self-reference
  (is (= (repeat 3 :bottom) (thrice #(self-ref 3)))))

(deftest local-shadowing-a-navigator-name
  (is (= (repeat 3 [1 3]) (thrice shadow))))

(deftest transform-fn-passed-in
  (is (= (repeat 3 [{:a 2 :b 3} {:a 4 :b 5}]) (thrice #(closure-fn inc))))
  (is (= (repeat 3 [{:a 0 :b 1} {:a 2 :b 3}]) (thrice #(closure-fn dec)))))
