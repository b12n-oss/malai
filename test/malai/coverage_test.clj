(ns malai.coverage-test
  "The Specter operators everyday code leans on, each exercised on made-up
   data. Expected values were checked on the JVM first, so a failure that
   shows up only on Jolt is a malai bug."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.specter :as s :refer :all]))

(def catalogue
  [{:id 1 :tags ["news" "sport"] :body {:blocks [{:type :p :text "Hello"}
                                                 {:type :img :src "a.png"}]}}
   {:id 2 :tags ["opinion"] :body {:blocks [{:type :p :text "World"}]}}
   {:id 3 :tags [] :body {:blocks []}}])

(def blocks (get-in catalogue [0 :body :blocks]))

(deftest core-ops
  (is (= [1 2 3] (select [ALL :id] catalogue)))
  (is (= 1 (select-one [FIRST :id] catalogue)))
  (is (= 2 (select-any [ALL :id even?] catalogue)))
  (is (= [2 3 4] (map :id (transform [ALL :id] inc catalogue))))
  (is (= [[] [] []] (map :tags (setval [ALL :tags] [] catalogue))))
  (is (= {:id 2 :tags ["x"]}
         (select-keys (first (multi-transform
                              [ALL (multi-path [:id (terminal inc)]
                                               [:tags (terminal-val ["x"])])]
                              catalogue))
                      [:id :tags])))
  (is (= [1 2 3] (select (path ALL :id) catalogue)))
  (is (= ["news" "sport" "opinion"] (select (comp-paths ALL :tags ALL) catalogue))))

(deftest navigators
  (is (= 3 (select-one [LAST :id] catalogue)))
  (is (= {:a 2 :b 3} (transform MAP-VALS inc {:a 1 :b 2})))
  (testing "NONE removes from a map and a vector"
    (is (= {:b 2} (setval [MAP-VALS odd?] NONE {:a 1 :b 2})))
    (is (= [2] (setval [ALL odd?] NONE [1 2 3]))))
  (is (= ["Hello"] (select [ALL (must :text)] blocks)))
  (is (= 2 (count (select-one [(keypath :body) (keypath :blocks)] (first catalogue)))))
  (is (= :b (select-one (nthpath 1) [:a :b :c])))
  (is (= [2 2] (transform (nthpath 0) inc [1 2])))
  (is (= {:z 1 :b 2} (setval (map-key :a) :z {:a 1 :b 2})))
  (is (= {:a 1 :b 2 :c 3 :d 4}
         (transform (submap [:a :b]) (fn [m] (assoc m :c 3)) {:a 1 :b 2 :d 4})))
  (is (= [1 3 2 4] (transform (srange 1 3) reverse [1 2 3 4])))
  (is (= [0 2 4] (select-one (filterer even?) (range 6))))
  (is (= [1 2 4 4] (transform [(filterer odd?) LAST] inc [1 2 3 4])))
  (is (= ["1" "22" "333"] (select (regex-nav #"\d+") "a1b22c333")))
  (is (= "a1b2" (transform (regex-nav #"\d+") (fn [d] (str (count d))) "a1b22")))
  (is (= "42" (transform (parser (fn [x] (Long/parseLong x)) str) inc "41")))
  (is (= 3 (select-one (view count) [1 2 3])))
  (is (= [2 3] (select-one (transformed ALL inc) [1 2])))
  (is (= [2 4] (select [ALL (pred even?)] [1 2 3 4]))))

(deftest control
  (is (= ["Hello" "a.png"] (select [ALL (cond-path (must :text) :text (must :src) :src)] blocks)))
  (is (= ["Hello" "a.png"] (select [ALL (if-path (must :text) :text (keypath :src))] blocks)))
  (is (= [[1 "news"] [1 "sport"]] (select [(collect-one :id) :tags ALL] (first catalogue))))
  (is (= [[[1 2 3] 1]] (select [(collect ALL) FIRST] [1 2 3])))
  (is (= [11 12] (transform [(putval 10) ALL] + [1 2])))
  (is (= [1 2 [1 2]] (select (continue-then-stay ALL) [1 2])))
  (is (= [[1 2] 1 2] (select (stay-then-continue ALL) [1 2])))
  (is (= {} (setval [:a (compact :b)] NONE {:a {:b 1}}))))

(def TREE (recursive-path [] p (if-path vector? [ALL p] STAY)))

(deftest recursion
  (is (= [1 2 3 4] (select TREE [1 [2 [3]] 4])))
  (is (= {:a [2 {:b 3}]} (transform (walker number?) inc {:a [1 {:b 2}]})))
  (is (= '(+ (dec 1) 2)
         (transform (codewalker (fn [f] (and (seq? f) (= 'inc (first f)))))
                    (fn [f] (cons 'dec (rest f)))
                    '(+ (inc 1) 2)))))

(defnav second-nav []
  (select* [this structure next-fn]
           (next-fn (second structure)))
  (transform* [this structure next-fn]
              (assoc structure 1 (next-fn (second structure)))))

(declarepath NESTED-NUMS)
(providepath NESTED-NUMS (if-path vector? [ALL NESTED-NUMS] (if-path number? STAY)))

(deftest user-navs
  (is (= [2] (select second-nav [1 2 3])))
  (is (= [1 3 3] (transform second-nav inc [1 2 3])))
  (is (= [1 2 3] (select NESTED-NUMS [1 [2 :x [3]]])))
  (is (= [2 [3 :x [4]]] (transform NESTED-NUMS inc [1 [2 :x [3]]]))))

(defn- pick [m k] (select [ALL (keypath k)] m))
(defn- rev-range [v a b] (transform (srange a b) reverse v))

(deftest closures
  (let [m [{:a 1 :b 2} {:a 3 :b 4}]]
    (is (= [1 3] (pick m :a)))
    (is (= [2 4] (pick m :b)))
    (is (= [nil nil] (pick m :c))))
  (is (= [2 1 3 4 5] (rev-range [1 2 3 4 5] 0 2)))
  (is (= [1 4 3 2 5] (rev-range [1 2 3 4 5] 1 4)))
  (is (= [1 2 3 4 5] (rev-range [1 2 3 4 5] 2 2))))

(defn- nest [inner depth] (reduce (fn [acc _] [acc]) inner (range depth)))

(deftest deep
  (let [d (nest [1] 200)]
    (is (= [1] (select TREE d)))
    (is (= (nest [2] 200) (transform (walker number?) inc d)))))

(defrichnav vals-as [mk]
  (select* [this vals structure next-fn] (next-fn (mk vals) structure))
  (transform* [this vals structure next-fn] (next-fn (mk vals) structure)))

(deftest collected-vals-shapes
  ;; A rich navigator may hand on any vals it likes. Only the shared empty
  ;; vector means "nothing collected", as on the JVM; anything else, a
  ;; freshly built empty vector included, gets the structure conj'd on.
  (is (= [1] (select (vals-as (fn [_] [])) 1)))
  (is (= [1] (select (vals-as (fn [_] (subvec [1] 0 0))) 1)))
  (is (= ['(1)] (select (vals-as (fn [_] nil)) 1)))
  (is (= ['(1)] (select (vals-as (fn [_] ())) 1)))
  (is (= [[1]] (select (vals-as (fn [_] (persistent! (transient [])))) 1)))
  (is (= [[1]] (select (vals-as (fn [v] (filterv odd? v))) 1))))
