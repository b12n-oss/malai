# Malai Guide

**Malai** is [Specter](https://github.com/redplanetlabs/specter) running on
[Jolt](https://github.com/jolt-lang/jolt), the native Clojure on Chez Scheme.
It's a fork that keeps Specter's own namespaces, so code that requires
`com.rpl.specter` runs on Jolt with no change to its `require` lines, and it
adds `malai.core`, which re-exports the same API under Malai's name.

This guide covers only what Malai adds. For Specter itself (navigators,
paths, `select`, `transform`, writing your own navigators) read
[Specter's wiki](https://github.com/redplanetlabs/specter/wiki); all of it
applies on Jolt.

## A first look

```clojure
(require '[com.rpl.specter :as s])

(s/transform [s/MAP-VALS s/ALL even?] inc {:a [1 2 3] :b [4 5]})
;; => {:a [1 3 3], :b [5 5]}

(require '[malai.core :as m])

(m/select [m/ALL :x odd?] [{:x 1} {:x 2} {:x 3}])
;; => [1 3]
```

## Pages

- [Specter on the JVM, babashka and Jolt](comparison.md): how each host runs
  Specter, side by side, with timings, and which one to use where.
- [Specter on Jolt](jolt.md): how paths are cached on Jolt, timings from
  `jolt bench`, using it at the REPL, `jolt build` binaries, and which Jolt
  Malai needs.

## Using Malai

- On Jolt, add Malai as a `deps.edn` git dependency. On the JVM keep using
  `com.rpl/specter` from Clojars, and never put both in one project: they
  provide the same namespaces, and whichever loads first wins. `malai.core`
  refuses to load over the original Specter with a clear error.
- Malai is private for now and licensed under the Apache License 2.0, the
  same as Specter. Specter is Nathan Marz's library, from Red Planet Labs.
