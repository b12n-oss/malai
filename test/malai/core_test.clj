(ns malai.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.specter]
            [malai.core :as m]))

(deftest publics-match
  (is (= (set (keys (ns-publics 'com.rpl.specter)))
         (disj (set (keys (ns-publics 'malai.core))) 'version 'specter-base))))

(deftest macros-stay-macros
  (doseq [s '[select transform setval path defnav recursive-path]]
    (testing s
      (is (:macro (meta (ns-resolve 'malai.core s)))))))

(deftest works-through-alias
  (is (= [2] (m/select [m/ALL :a even?] [{:a 1} {:a 2}])))
  (is (= {:x [2 3]} (m/transform [m/MAP-VALS m/ALL] inc {:x [1 2]})))
  (is (= [1 2] (m/setval [m/ALL nil?] m/NONE [1 nil 2]))))

(deftest version-strings
  (is (= "0.1.0" m/version))
  (is (= "1.1.7-SNAPSHOT@6119462" m/specter-base)))

(deftest clash-guard
  ;; Pretend the loaded com.rpl.specter.impl is upstream's: no marker var.
  (ns-unmap 'com.rpl.specter.impl 'malai-marker)
  (try
    (let [e (try (require 'malai.core :reload) nil
                 (catch Exception e e))]
      (is (some? e))
      ;; a load error arrives wrapped (CompilerException on the JVM, a
      ;; syntax-error wrapper on Jolt), so look down the cause chain
      (is (some #(some-> ^Throwable % .getMessage
                         (.startsWith "malai: com.rpl.specter was loaded from the original Specter"))
                (take-while some? (iterate (fn [^Throwable t] (.getCause t)) e)))))
    (finally
      (intern 'com.rpl.specter.impl (with-meta 'malai-marker {:no-doc true}) true)
      (require 'malai.core :reload))))
