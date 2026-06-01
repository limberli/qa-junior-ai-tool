#!/usr/bin/env bash
#
# Stops the AI JUN stack (both Groq and Ollama compose files).
#
set -euo pipefail
cd "$(dirname "$0")"

for f in docker-compose-groq.yml docker-compose-simple.yml; do
  if docker compose -f "$f" ps -q 2>/dev/null | grep -q .; then
    echo "Останавливаю: $f"
    docker compose -f "$f" down
  fi
done

echo "Остановлено."
