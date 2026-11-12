# Specter on the JVM, babashka and Jolt

The same Specter code runs on all three hosts, and gives the same answers:
upstream Specter's own test suite, plus malai's, passes on the JVM (through
`lein test`), on babashka and on Jolt. What differs is how each host runs it,
and that decides how fast a path is and what it costs to start.

## Side by side

| | JVM | babashka | Jolt (malai) |
|---|---|---|---|
| **What runs your Clojure** | Clojure compiles it to JVM bytecode | SCI interprets it form by form, inside a GraalVM native-image binary | Jolt compiles it to native code through Chez Scheme |
| **Which Specter** | `com.rpl/specter` from Clojars, with riddley and two small compiled Java classes | Upstream Specter from source; its `#?(:bb ...)` branches swap riddley, reducers and the Java classes for plain Clojure | malai, a fork of Specter with `#?(:jolt ...)` branches beside the `:bb` ones |
| **Inline path caching** | Yes: each call site compiles its path on the first call (an `eval` of a fn over the path's locals) | No: under `:bb`, `path` is `comp-paths`, so every call rebuilds its path | Yes: a static path is cached on its first call, a path over locals compiles after 256 calls ([how and why](jolt.md#how-paths-are-cached)) |
| **Seeing locals inside nested paths** | riddley's `macroexpand-all`, which keeps `&env` | Not needed: nothing is cached | Macro calls in a path stay unexpanded and expand inside the compiled fn, where the locals are its parameters |
| **Startup** | A JVM start, then Clojure's | Instant: a native binary | A native binary; a `jolt build` binary caches paths too |
| **At the REPL** | A cached path keeps what it saw on its first call | Nothing goes stale | As on the JVM, plus `malai.core/clear-path-cache!` |

## Timings

`jolt bench` on Jolt, the same benchmark run by `bb` on babashka, and
Specter 1.1.4 on the JVM with the same data and paths. An Apple M1 Pro,
µs per call, after warm-up (Jolt 0.8.26, babashka 1.13.220, Clojure 1.12.3):

| | JVM | babashka | Jolt (malai) |
|---|---|---|---|
| static path `[MAP-VALS ALL :y ALL even?]`, `transform` | 2.58 | 21.48 | 8.38 |
| dynamic path `[MAP-VALS ALL (keypath k)]`, `select` | 0.81 | 15.32 | 2.09 |
| hand-written `update`/`mapv`, same result | 1.06 | 1.95 | 1.90 |

Caching is most of the gap between babashka and Jolt: babashka rebuilds the
path on every call, as malai 0.1 did on Jolt (18.75 µs and 10.37 µs). What
stays between Jolt and the JVM is how fast each host runs the navigators
themselves.

## Which to use

- **On the JVM**, use `com.rpl/specter` from Clojars. It's the fastest of
  the three once warm, and malai adds nothing there.
- **In a babashka script**, Specter loads from source and works as is. Paths
  run uncached, which is fine for scripts that touch them a few hundred times.
- **On Jolt**, use malai. It's the only one of the three that loads there,
  and it caches paths, so a loop or a long REPL session gets close to a path
  compiled by hand.

Never put malai and `com.rpl/specter` in one project: they provide the same
namespaces, and whichever loads first wins.
