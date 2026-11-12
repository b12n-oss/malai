# malai changes

Upstream Specter's history is in `CHANGES.md`, unchanged.

## 0.1.0 (unreleased)

- Specter runs on Jolt 0.8.26 or newer: `#?(:jolt ...)` branches beside the
  existing `:bb` ones, and workarounds for jolt-lang/jolt#1612, #1613, #1614
  and #1615. Upstream's suite passes on Jolt, the JVM and babashka.
- No inline path caching on Jolt yet; paths are built on every call.
- `malai.core` re-exports every public var of `com.rpl.specter`, and
  refuses to load (with a malai error) when the `com.rpl.specter` on the
  path is the original Specter.
- `malai.version` holds `version` and `specter-base`.
- `deps.edn` for Jolt users; `bb.edn` tasks run as `jolt <task>` (`test`,
  `test:clj`, `test:bb`, `verify`, `metrics`, `info`).
- A coverage test of the operators everyday code uses, checked on the JVM
  and run on all three hosts.
