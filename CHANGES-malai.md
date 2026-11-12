# Malai changes

Upstream Specter's history is in `CHANGES.md`, unchanged.

## 0.2.0 (unreleased)

- Inline path caching on Jolt. Static paths are cached on the first call; a
  path that closes over locals compiles after 256 calls at its call site,
  so short CLI runs never pay the compile. Works in `jolt build` binaries,
  `--closed-world` included.
- `malai.core/clear-path-cache!` drops every cached path (for the REPL); a
  no-op on the JVM and babashka.
- `malai.core` keeps each var's full metadata, so `pred`, `pred=` and
  `terminal-val` get the inline-cache fast path through it too.
- Works around jolt-lang/jolt#1616 (`with-meta` on a fn changes it in place),
  which hung every dynamic path once caching was on.
- `jolt bench` and `jolt smoke:build` tasks; `docs/guide/jolt.md`.

## 0.1.0 (unreleased)

- Specter runs on Jolt 0.8.26 or newer: `#?(:jolt ...)` branches beside the
  existing `:bb` ones, and workarounds for jolt-lang/jolt#1612, #1613, #1614
  and #1615. Upstream's suite passes on Jolt, the JVM and babashka.
- No inline path caching on Jolt yet; paths are built on every call.
- `malai.core` re-exports every public var of `com.rpl.specter`, and
  refuses to load (with a Malai error) when the `com.rpl.specter` on the
  path is the original Specter.
- `malai.version` holds `version` and `specter-base`.
- `deps.edn` for Jolt users; `bb.edn` tasks run as `jolt <task>` (`test`,
  `test:clj`, `test:bb`, `verify`, `metrics`, `info`).
- A coverage test of the operators everyday code uses, checked on the JVM
  and run on all three hosts.
