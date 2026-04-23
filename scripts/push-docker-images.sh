#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"

if [[ -f "$repo_root/.env" ]]; then
  set -a
  # Load local registry credentials and tag overrides if present.
  # shellcheck disable=SC1090
  source "$repo_root/.env"
  set +a
fi

if [[ -z "${DOCKER_HUB_USERNAME:-}" || "$DOCKER_HUB_USERNAME" == "your-dockerhub-username" ]]; then
  echo "ERROR: set DOCKER_HUB_USERNAME in .env or export it before running this script." >&2
  exit 1
fi

compose_cmd=""
if docker compose version >/dev/null 2>&1; then
  compose_cmd=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  compose_cmd=(docker-compose)
else
  echo "ERROR: Docker Compose is not installed." >&2
  exit 1
fi

cd "$repo_root"

"${compose_cmd[@]}" build

image_refs=()
while IFS= read -r image_ref; do
  image_refs+=("$image_ref")
done < <("${compose_cmd[@]}" config --images | grep "^${DOCKER_HUB_USERNAME}/" | sort -u)

for image_ref in "${image_refs[@]}"; do
  echo "Pushing ${image_ref}..."
  docker push "$image_ref"
done