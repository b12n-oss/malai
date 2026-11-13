#!/usr/bin/env bash
# Assertions the documentation build must satisfy. Run by the shared site
# workflow (jlt-commons/ci-builds) against the freshly built _site, with
# BASE_PATH exported. Locally:
#   jolt site:build && BASE_PATH=/malai bash docs/check-site.sh

set -euo pipefail
out=_site
base=${BASE_PATH:-/malai}

test -f "$out/index.html"     || { echo "no homepage generated"; exit 1; }
test -f "$out/404.html"       || { echo "no 404 page generated"; exit 1; }
test -f "$out/css/screen.css" || { echo "static assets missing"; exit 1; }

# The homepage is docs/guide/index.md. Every guide page must render: a page
# that stops rendering would otherwise go unnoticed.
grep -q 'Malai Guide' "$out/index.html" || { echo "homepage has no content"; exit 1; }
for page in docs/guide/*.md; do
  name=$(basename "$page" .md)
  test -f "$out/guide/$name.html" || { echo "guide page $name missing"; exit 1; }
done

# docs/images is copied through :asset-dirs.
for img in logo.svg malai-1880.jpg; do
  test -f "$out/images/$img" || { echo "image $img not copied"; exit 1; }
done

! grep -rq '{{site-base}}' "$out" || { echo "unrendered template variable"; exit 1; }

# Served at <owner>.github.io/malai/, a root-absolute URL outside the base
# path would load the organization site's file instead of this one's.
if grep -rohE '(href|src)="/[^"]*"' "$out" --include='*.html' | grep -v "=\"$base/" | grep -q .; then
  echo "a URL escapes $base:"; grep -rohE '(href|src)="/[^"]*"' "$out" --include='*.html' | grep -v "=\"$base/" | sort -u | head
  exit 1
fi
echo "site checks passed"
