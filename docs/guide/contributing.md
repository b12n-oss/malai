# Contributing to Malai

Malai is a small fork, and most of its code is Specter's. So before you
change anything, it helps to know which of the two a change belongs to.

- **A change to Specter itself**, such as a new navigator or a bug that
  shows up on the JVM too, belongs upstream in
  [redplanetlabs/specter](https://github.com/redplanetlabs/specter). Malai
  picks it up from there.
- **A change to how Specter runs on Jolt** belongs here. That covers the
  `#?(:jolt ...)` branches, the path cache, `malai.core`, the tests under
  `test/malai` and these docs.

The root `CONTRIBUTING.md` is upstream Specter's, kept unchanged with the
rest of its files, and its contributor agreement is for pull requests to
Specter.

## What you need

- Jolt 0.8.27 or newer on your `PATH`
- Leiningen and a JDK, for the JVM suite
- Node, only for upstream's ClojureScript tests
- Python 3, only for previewing the docs site

The tasks live in `bb.edn` and run as `jolt <task>`; babashka isn't needed.
`jolt info` lists every one of them.

## Running the tests

```sh
jolt verify        # the gate: Jolt, then the JVM
jolt test          # only the Jolt suite
jolt test:cljs     # upstream's ClojureScript tests
jolt smoke:build   # builds a small program with `jolt build`, about 15 s per build
```

Run `jolt verify` before you open a pull request. Both suites run upstream
Specter's tests as well as Malai's, and both have to pass, since a Jolt
change must leave the JVM exactly as it was. CI runs `jolt verify` and
`jolt test:cljs`.

`jolt bench` prints timings, and `jolt metrics` prints a code-metrics report.
Neither ever fails.

## How Jolt changes are written

- Jolt reads a `#?(:jolt ...)` branch first, and otherwise falls back to
  `:clj`. It never reads `:bb`. So every Jolt change is a `:jolt` branch,
  placed right after the matching `:bb` form when there is one.
- Code that a `:jolt` branch replaces stays in the file, word for word,
  under `:default` or its original key, so the JVM, ClojureScript and
  babashka keep running exactly what they ran before.
- If a change works around a Jolt bug, file the bug at
  [jolt-lang/jolt](https://github.com/jolt-lang/jolt/issues) and name the
  issue in a one-line comment next to the branch. When a Jolt release fixes
  it, the branch goes.
- Tests that only make sense on Jolt go in `.jolt` files, which Leiningen
  never loads. Tests for every host go in `.clj` files.

## The docs site

The pages under `docs/guide` build into a site with
[jlt-commons/docs-engine](https://github.com/jlt-commons/docs-engine):

```sh
jolt site:build                            # into _site/
BASE_PATH=/malai bash docs/check-site.sh   # the checks CI runs on it
jolt site:serve                            # preview at /malai/, needs python3
```

The site tasks look for the engine in `$JLT_DOCS_ENGINE`, then
`$DOCS_ENGINE`, then `~/dev/jlt-commons/docs-engine`, then `../docs-engine`.
