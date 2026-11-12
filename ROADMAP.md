# Roadmap

| Phase | Status | Scope | Produces |
|---|---|---|---|
| P1 0.1 Jolt port | done | 6 tasks | `:jolt` branches in `com.rpl.specter`, upstream suite green on Jolt, JVM and babashka; `malai.core`, `malai.version`, the clash guard; coverage test; `jolt <task>` tooling and jolt-metrics |
| P2 0.2 inline caching on Jolt | done | 6 tasks | per-call-site path cache on Jolt (static from call 1, dynamic after 256 calls), `clear-path-cache!`, full metadata in `malai.core`, `jolt bench`, `jolt smoke:build`, `docs/guide/jolt.md` |
| P2b drop the Jolt workarounds | waiting on Jolt | small | once a Jolt release ships the fixes for jolt-lang/jolt#1612 to #1616 (PRs #1617, #1618, #1619, #1621, #1622), raise `:jolt/min-version`, remove those `:jolt` branches, and go back to `identical?` for collected vals |
| P3 upstream | later, the user's call | 1 PR | offer the `:jolt` branches to redplanetlabs/specter |
| P4 open source | later, the user's call | | make `b12n-oss/malai` public, say so in the Jolt issues it works around |

## Notes

- Caching on Jolt: static paths are cached on the first call; a dynamic call
  site runs uncached for 256 calls (its first compile, an `eval`, costs about
  2 ms), then compiles. Details and timings in `docs/guide/jolt.md`.
- Known small costs left from the P2 review, none affecting answers: every
  thread that crosses a call site's 256th call at the same moment compiles
  it (a `swap-vals!` claim would make it one); a `clear-path-cache!` racing
  an in-flight compile can write back a pre-clear entry; a key cleared
  mid-update can come back as a nil entry; the #1616 wrapper adds an `apply`
  per call to direct-nav fns until P2b; `malai.core` would copy `:dynamic`
  metadata if upstream ever adds a public dynamic var.
- Jolt-stack (`~/.local/bin/jolt-stack`, all the fixes stacked) passes the
  whole suite with the workarounds still in place.

## Blocked

Nothing.
