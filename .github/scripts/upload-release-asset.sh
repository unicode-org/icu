#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "usage: $0 <release-tag> <file> [file ...]" >&2
  exit 2
fi

tag=$1
shift

repo=${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}

for file in "$@"; do
  if [[ ! -f "$file" ]]; then
    echo "::error::Release asset does not exist: $file"
    exit 1
  fi

  name=$(basename "$file")
  local_digest="sha256:$(sha256sum "$file" | awk '{print $1}')"

  mapfile -t remote_digests < <(
    gh api "repos/$repo/releases/tags/$tag" \
      --jq ".assets[] | select(.name == \"$name\") | .digest"
  )

  if [[ ${#remote_digests[@]} -gt 1 ]]; then
    echo "::error::Release contains multiple assets named $name"
    exit 1
  fi

  if [[ ${#remote_digests[@]} -eq 1 ]]; then
    remote_digest=${remote_digests[0]}
    if [[ -z "$remote_digest" || "$remote_digest" == "null" ]]; then
      echo "::error::Existing release asset $name has no recorded digest; refusing to overwrite it"
      exit 1
    fi
    if [[ "$remote_digest" != "$local_digest" ]]; then
      echo "::error::Existing release asset $name has digest $remote_digest, generated artifact has $local_digest; refusing to overwrite it"
      exit 1
    fi
    echo "$name already exists with digest $local_digest; leaving it unchanged"
    continue
  fi

  gh release upload "$tag" "$file" --repo="$repo"
done
