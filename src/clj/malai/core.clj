(ns malai.core
  "malai's name for Specter's API: every public var of com.rpl.specter
   (navigators, functions and macros) under the same name, so
   (require '[malai.core :as m]) and (require '[com.rpl.specter :as s]) are
   interchangeable. Loading fails with a clear error when the
   com.rpl.specter on the classpath is the original Specter rather than
   malai's fork."
  (:require [com.rpl.specter]))

(def version "malai's version." "0.1.0")

(def specter-base
  "The upstream Specter version and commit malai is based on."
  "1.1.7-SNAPSHOT@6119462")

(when-not (resolve 'com.rpl.specter.impl/malai-marker)
  (throw (ex-info (str "malai: com.rpl.specter was loaded from the original Specter, not malai. "
                       "Remove com.rpl/specter from your dependencies; malai provides those namespaces.")
                  {:malai/version "0.1.0"})))

(defn- re-export!
  "Interns every public var of `from-ns` into this namespace with the same
   value and metadata. intern copies the symbol's metadata onto the var, so
   :macro true keeps macros as macros on the JVM, Jolt and babashka."
  [from-ns]
  (doseq [[sym v] (ns-publics from-ns)]
    (intern 'malai.core
            (with-meta sym (select-keys (meta v) [:doc :arglists :macro :added :deprecated]))
            (deref v))))

(re-export! 'com.rpl.specter)
