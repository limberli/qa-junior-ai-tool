#!/usr/bin/env bash
#
# AI JUN launcher (macOS / Linux).
# Asks for language, then start mode (Groq / local Ollama), starts the stack,
# waits until it's ready, and opens the UI.
#
set -euo pipefail
cd "$(dirname "$0")"

UI_URL="http://localhost:3000"
MODES_URL="http://localhost:8080/api/modes"

c_err()  { printf "\033[31m%s\033[0m\n" "$*" >&2; }
c_info() { printf "\033[36m%s\033[0m\n" "$*"; }
c_ok()   { printf "\033[32m%s\033[0m\n" "$*"; }

# ── Language ─────────────────────────────────────────────────────────────────
echo "Выберите язык / Choose language:"
echo "  1) Русский"
echo "  2) English"
printf "[1]: "
read -r lang
case "${lang:-1}" in
  2) LANG_SEL="en" ;;
  *) LANG_SEL="ru" ;;
esac

# Localized strings
if [ "$LANG_SEL" = "ru" ]; then
  T_NO_DOCKER="Docker не установлен. Поставьте Docker Desktop: https://www.docker.com/products/docker-desktop/"
  T_DOCKER_DOWN="Docker не запущен. Запустите Docker Desktop и повторите."
  T_PICK="Выберите режим запуска:"
  T_OPT_GROQ="Groq — облачный LLM, быстро, без GPU (нужен API-ключ)"
  T_OPT_OLLAMA="Ollama — локальная модель phi3:mini (без ключа, медленнее)"
  T_CHOICE="Ваш выбор [1]: "
  T_BADCHOICE="Неизвестный выбор:"
  T_NEEDKEY="Для Groq нужен API-ключ (https://console.groq.com → API Keys)."
  T_PASTEKEY="Вставьте GROQ_API_KEY: "
  T_NOKEY="Ключ не введён."
  T_KEYSAVED="Ключ сохранён в .env (файл в .gitignore)."
  T_HASKEY="Ключ уже сохранён в .env. Использовать его? [Y/n]: "
  T_STARTING="Запуск стека"
  T_BUILDNOTE="— сборка может занять несколько минут при первом запуске…"
  T_WAITING="Ожидание готовности сервисов…"
  T_TIMEOUT="Сервисы не поднялись за отведённое время. Логи:"
  T_READY="Готово! UI:"
  T_STOPHINT="Остановить:  ./stop.sh"
else
  T_NO_DOCKER="Docker is not installed. Install Docker Desktop: https://www.docker.com/products/docker-desktop/"
  T_DOCKER_DOWN="Docker is not running. Start Docker Desktop and try again."
  T_PICK="Choose start mode:"
  T_OPT_GROQ="Groq   — cloud LLM, fast, no GPU (needs API key)"
  T_OPT_OLLAMA="Ollama — local phi3:mini model (no key, slower)"
  T_CHOICE="Your choice [1]: "
  T_BADCHOICE="Unknown choice:"
  T_NEEDKEY="Groq needs an API key (https://console.groq.com → API Keys)."
  T_PASTEKEY="Paste GROQ_API_KEY: "
  T_NOKEY="No key entered."
  T_KEYSAVED="Key saved to .env (git-ignored)."
  T_HASKEY="A key is already saved in .env. Use it? [Y/n]: "
  T_STARTING="Starting stack"
  T_BUILDNOTE="— first build may take a few minutes…"
  T_WAITING="Waiting for services to become ready…"
  T_TIMEOUT="Services did not start in time. Logs:"
  T_READY="Ready! UI:"
  T_STOPHINT="To stop:  ./stop.sh"
fi

# ── Docker present and running ────────────────────────────────────────────────
if ! command -v docker >/dev/null 2>&1; then c_err "$T_NO_DOCKER"; exit 1; fi
if ! docker info >/dev/null 2>&1; then c_err "$T_DOCKER_DOWN"; exit 1; fi

# ── Pick mode ────────────────────────────────────────────────────────────────
echo "$T_PICK"
echo "  1) $T_OPT_GROQ"
echo "  2) $T_OPT_OLLAMA"
printf "%s" "$T_CHOICE"
read -r choice
case "${choice:-1}" in
  1|"") MODE="groq";   COMPOSE_FILE="docker-compose-groq.yml" ;;
  2)    MODE="ollama"; COMPOSE_FILE="docker-compose-simple.yml" ;;
  *)    c_err "$T_BADCHOICE $choice"; exit 1 ;;
esac

# ── Groq needs an API key (.env, git-ignored) ────────────────────────────────
if [ "$MODE" = "groq" ]; then
  need_key="yes"
  if [ -f .env ] && grep -q '^GROQ_API_KEY=gsk' .env 2>/dev/null; then
    printf "%s" "$T_HASKEY"
    read -r usekey
    case "${usekey:-y}" in [Nn]*) need_key="yes" ;; *) need_key="" ;; esac
  fi
  if [ -n "$need_key" ]; then
    c_info "$T_NEEDKEY"
    printf "%s" "$T_PASTEKEY"
    read -r key
    if [ -z "${key:-}" ]; then c_err "$T_NOKEY"; exit 1; fi
    echo "GROQ_API_KEY=$key" > .env
    c_ok "$T_KEYSAVED"
  fi
fi

# ── Build & start ────────────────────────────────────────────────────────────
c_info "$T_STARTING ($MODE) $T_BUILDNOTE"
docker compose -f "$COMPOSE_FILE" up --build -d

# ── Wait until ready (UI + orchestrator schema both answer 200) ─────────────
c_info "$T_WAITING"
ready=""
for i in $(seq 1 90); do
  ui=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$UI_URL" 2>/dev/null || echo 000)
  md=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$MODES_URL" 2>/dev/null || echo 000)
  if [ "$ui" = "200" ] && [ "$md" = "200" ]; then ready="yes"; break; fi
  printf "."
  sleep 4
done
echo

if [ -z "$ready" ]; then
  c_err "$T_TIMEOUT docker compose -f $COMPOSE_FILE logs"
  exit 1
fi
c_ok "$T_READY $UI_URL"

# ── Open the browser ─────────────────────────────────────────────────────────
if command -v open >/dev/null 2>&1; then
  open "$UI_URL"
elif command -v xdg-open >/dev/null 2>&1; then
  xdg-open "$UI_URL" >/dev/null 2>&1 &
fi

echo
echo "$T_STOPHINT"
