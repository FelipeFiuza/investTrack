#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

if [ ! -f "$ROOT/.env" ]; then
  echo "Arquivo .env não encontrado em $ROOT" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source "$ROOT/.env"
set +a

cd "$ROOT/bezkoder-app"
echo "Iniciando Spring Boot. O log segue neste terminal. Ctrl+C encerra."
exec mvn spring-boot:run
