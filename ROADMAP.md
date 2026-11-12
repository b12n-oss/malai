# Roadmap

| Phase | Status | Scope | Produces |
|---|---|---|---|
| P1 0.1 Jolt port | done | 6 tasks | `:jolt` branches in `com.rpl.specter`, upstream suite green on Jolt, JVM and babashka; `malai.core`, `malai.version`, the clash guard; coverage test; `jolt <task>` tooling and jolt-metrics |
| P2 0.2 inline caching on Jolt | planned | own spike first | `path` caching on Jolt instead of the uncached `comp-paths` fallback, with a test that proves the cache is hit |
| P3 upstream | later, the user's call | 1 PR | offer the `:jolt` branches to redplanetlabs/specter |
| P4 open source | later, the user's call | | make `b12n-oss/malai` public, say so in the Jolt issues it works around |

## Notes

- 0.1 on Jolt builds each path on every call (the same fallback Specter
  already uses under babashka). Results match the JVM; paths are slower.
- P2 starting points: Jolt's `&env` in a macro also lists the enclosing fn's
  own name, which the JVM doesn't; the inline-cache compiler reads `&env` to
  find closed-over locals.
- `malai.core` re-exports five metadata keys per var and drops
  `:direct-nav`, so `pred`, `pred=` and `terminal-val` used through
  `malai.core` lose the JVM's inline-cache fast path. Results are the same.
  Copying the whole metadata would fix it.
- When Jolt fixes jolt-lang/jolt#1612, #1613, #1614 or #1615, the matching
  `:jolt` branch can go. #1612 also closes the one known difference (a
  freshly built empty vector as collected vals; see the README).

## Blocked

Nothing.
