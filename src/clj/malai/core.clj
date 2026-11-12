(ns malai.core
  "malai's name for Specter's API: every public var of com.rpl.specter
   (navigators, functions and macros) under the same name, so
   (require '[malai.core :as m]) and (require '[com.rpl.specter :as s]) are
   interchangeable. Loading fails with a clear error when the
   com.rpl.specter on the classpath is the original Specter rather than
   malai's fork. malai's version is in malai.version."
  (:require [clojure.java.io :as io]
            [malai.version :as version]))

(defn- clash-error []
  (ex-info (str "malai: com.rpl.specter was loaded from the original Specter, not malai. "
                "Remove com.rpl/specter from your dependencies; malai provides those namespaces.")
           {:malai/version version/version}))

(defn- check-impl-source!
  "Throws when `src`, the text of com/rpl/specter/impl.cljc as found on the
   path, lacks malai's marker. nil (no source on the path, as in an AOT jar)
   passes, and the check after loading decides."
  [src]
  (when (and src (not (.contains ^String src "malai-marker")))
    (throw (clash-error))))

;; Before loading: on Jolt the original Specter fails to load at all (it
;; needs riddley), so this is the only point where a clear error is possible.
(check-impl-source! (some-> (io/resource "com/rpl/specter/impl.cljc") slurp))

(require 'com.rpl.specter)

;; After loading: covers a classpath where the source check couldn't look.
(when-not (resolve 'com.rpl.specter.impl/malai-marker)
  (throw (clash-error)))

(defn- re-export!
  "Interns every public var of `from-ns` into this namespace with the same
   value and metadata. intern copies the symbol's metadata onto the var, so
   :macro keeps macros as macros on the JVM, Jolt and babashka, and
   :direct-nav keeps the inline-cache fast path for pred, terminal-val and
   friends."
  [from-ns]
  (doseq [[sym v] (ns-publics from-ns)]
    (intern 'malai.core
            (with-meta sym (dissoc (meta v) :ns :name))
            (deref v))))

(re-export! 'com.rpl.specter)

(defn clear-path-cache!
  "Drops every cached path, so the next calls rebuild them. On Jolt a cached
   path keeps what it saw on its first call: after redefining a fn or
   navigator that a path uses (at the REPL, say), call this to make call
   sites pick up the new one. Re-evaluating a defn that contains a select
   adds a cache entry rather than replacing one, and this frees those too.
   On the JVM and babashka it does nothing and returns nil, since their
   caches live elsewhere (vars) or don't exist."
  []
  (when-let [cache (resolve 'com.rpl.specter.impl/jolt-path-cache)]
    (reset! (deref cache) {}))
  nil)
