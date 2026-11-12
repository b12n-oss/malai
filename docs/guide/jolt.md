# Specter on Jolt

This page covers only what is different about running Specter through malai
on [Jolt](https://github.com/jolt-lang/jolt). For Specter itself (navigators,
`select`, `transform`, writing your own navigators) read
[upstream's docs](https://github.com/redplanetlabs/specter/wiki); everything
there applies.

## How paths are cached

On the JVM, Specter compiles the path at each `select`/`transform` call site
once and reuses it. malai does the same on Jolt, with one twist for paths
that use local values.

- **A static path** such as `[MAP-VALS ALL :y ALL even?]` is cached on its
  first call. That costs nothing extra, so there is no reason to wait.
- **A dynamic path**, one that closes over a local such as `(keypath k)`,
  needs an `eval` to compile, and on Jolt that takes about 2 ms. An uncached
  call costs about 10 µs and a cached one about 2 µs, so compiling only pays
  off after roughly 256 calls. So a dynamic call site runs uncached for its
  first 256 calls, then compiles. A CLI tool that hits a path a few times
  never pays for the `eval`, and a long loop or a REPL session gets the
  cached speed.

The cache lives in a table at run time, so it works the same under
`jolt -M` and in a `jolt build` binary, `--closed-world` included. With
`--closed-world` Jolt reports that it skipped tree-shaking, because malai's
code calls `eval`, `resolve` and `intern` at run time. The binary is correct,
and in malai's smoke test it came out about the same size as a default build
(22.8 MB against 22.9 MB).

## Timings

`jolt bench` prints these on your machine. On an Apple M1 Pro, a 3-map
structure, with a Jolt that has the fixes malai needs (the pre-release
`v0.8.26-36-g2337c3fa`):

| | µs per call |
|---|---|
| static path, cached | 7.20 |
| dynamic path `(keypath k)`, cached | 1.99 |
| the same two through `malai.core` | 6.93 and 1.97 |
| static path compiled once by hand (`comp-paths`) | 6.59 |
| hand-written `update`/`mapv` | 1.67 |

Before caching (malai 0.1 on Jolt 0.8.26), the same static path took
18.75 µs and the dynamic one 10.37 µs.

| Cold, after `clear-path-cache!` | ms |
|---|---|
| static path, first call | 0.183 |
| dynamic path, first call | 0.111 |
| a short CLI run: 10 dynamic calls | 0.194 |
| 256 dynamic calls, the last one compiling | 4.826 |

Caching closes the gap to compiling the path yourself. Jolt's navigators
are still slower than the JVM's (Specter 1.1.4 on the JVM ran the static path
in about 3 µs), and malai doesn't try to change that.

## At the REPL

A cached path keeps what it saw when it was compiled. If you redefine a fn
or navigator that a path uses, that call site keeps the old one, as it would
on the JVM. Clear the cache to pick up the new one:

```clojure
(require '[malai.core :as m])
(m/clear-path-cache!)
```

Re-evaluating a `defn` that contains a `select` adds a new cache entry rather
than replacing the old one, and `clear-path-cache!` frees those too. On the
JVM and babashka it does nothing and returns nil.

Two timing details differ from the JVM, both because of the 256-call wait:

- A dynamic call site freezes values at its 256th call, not its first. Up to
  then it reads vars and other values in the path fresh on every call.
- An expression inside a dynamic path, say `(keypath (next-id))`, runs on
  each of the first 256 calls, and once more when the path compiles. On the
  JVM it runs once. Keep side effects out of path expressions.

Set `com.rpl.specter.impl/*DEBUG-INLINE-CACHING*` to true to see a call site
that failed to compile. It stays uncached, and its answers stay correct.

## Which Jolt

malai runs Specter's own code wherever Jolt behaves like the JVM, which
needs a Jolt with fixes for [#1612](https://github.com/jolt-lang/jolt/issues/1612),
[#1613](https://github.com/jolt-lang/jolt/issues/1613),
[#1614](https://github.com/jolt-lang/jolt/issues/1614),
[#1615](https://github.com/jolt-lang/jolt/issues/1615) and
[#1616](https://github.com/jolt-lang/jolt/issues/1616). On an older Jolt,
loading malai fails straight away with a message naming the fixes it lacks,
rather than returning wrong answers:

```
malai needs a Jolt release with fixes for jolt-lang/jolt#1612 to #1616. This Jolt (v0.8.26) is missing: #1612, #1613, #1614, #1615, #1616.
```

At the Jolt REPL that line is printed above Jolt's own "Syntax error
macroexpanding" message ([#1627](https://github.com/jolt-lang/jolt/issues/1627)).
malai 0.2 still runs on Jolt 0.8.26.

With those fixes, malai on Jolt gives the JVM's answers everywhere its tests
and probes look, including collected `vals` built by your own navigators.
