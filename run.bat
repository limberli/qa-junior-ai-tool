@echo off
REM ============================================================================
REM  AI JUN launcher (Windows). Double-click to run.
REM  Steps: choose language -> choose mode -> (Groq) enter API key -> start UI.
REM ============================================================================
setlocal enabledelayedexpansion
chcp 65001 >nul
cd /d "%~dp0"

set "UI_URL=http://localhost:3000"
set "MODES_URL=http://localhost:8080/api/modes"

REM --- Language --------------------------------------------------------------
echo Выберите язык / Choose language:
echo   1^) Русский
echo   2^) English
set /p lang="[1]: "
if "%lang%"=="2" ( set "L=en" ) else ( set "L=ru" )

if "%L%"=="ru" (
  set "T_NODOCK=Docker не установлен. Поставьте Docker Desktop: https://www.docker.com/products/docker-desktop/"
  set "T_DOWN=Docker не запущен. Запустите Docker Desktop и повторите."
  set "T_PICK=Выберите режим запуска:"
  set "T_G=Groq   - облачный LLM, быстро, без GPU (нужен API-ключ)"
  set "T_O=Ollama - локальная модель phi3:mini (без ключа, медленнее)"
  set "T_CH=Ваш выбор [1]: "
  set "T_BAD=Неизвестный выбор:"
  set "T_NEED=Для Groq нужен API-ключ (https://console.groq.com - API Keys)."
  set "T_PASTE=Вставьте GROQ_API_KEY: "
  set "T_NOKEY=Ключ не введён."
  set "T_SAVED=Ключ сохранён в .env (файл в .gitignore)."
  set "T_HASKEY=Ключ уже сохранён в .env. Использовать его? [Y/n]: "
  set "T_START=Запуск стека"
  set "T_NOTE=- сборка может занять несколько минут при первом запуске..."
  set "T_WAIT=Ожидание готовности сервисов..."
  set "T_TIMEOUT=Сервисы не поднялись за отведённое время. Логи:"
  set "T_READY=Готово! UI:"
  set "T_STOP=Остановить:  stop.bat"
  set "T_FAIL=Ошибка docker compose."
) else (
  set "T_NODOCK=Docker is not installed. Install Docker Desktop: https://www.docker.com/products/docker-desktop/"
  set "T_DOWN=Docker is not running. Start Docker Desktop and try again."
  set "T_PICK=Choose start mode:"
  set "T_G=Groq   - cloud LLM, fast, no GPU (needs API key)"
  set "T_O=Ollama - local phi3:mini model (no key, slower)"
  set "T_CH=Your choice [1]: "
  set "T_BAD=Unknown choice:"
  set "T_NEED=Groq needs an API key (https://console.groq.com - API Keys)."
  set "T_PASTE=Paste GROQ_API_KEY: "
  set "T_NOKEY=No key entered."
  set "T_SAVED=Key saved to .env (git-ignored)."
  set "T_HASKEY=A key is already saved in .env. Use it? [Y/n]: "
  set "T_START=Starting stack"
  set "T_NOTE=- first build may take a few minutes..."
  set "T_WAIT=Waiting for services to become ready..."
  set "T_TIMEOUT=Services did not start in time. Logs:"
  set "T_READY=Ready! UI:"
  set "T_STOP=To stop:  stop.bat"
  set "T_FAIL=docker compose failed."
)

REM --- Docker present and running ---------------------------------------------
where docker >nul 2>&1
if errorlevel 1 ( echo [ERROR] !T_NODOCK! & pause & exit /b 1 )
docker info >nul 2>&1
if errorlevel 1 ( echo [ERROR] !T_DOWN! & pause & exit /b 1 )

REM --- Pick mode -------------------------------------------------------------
echo !T_PICK!
echo   1^) !T_G!
echo   2^) !T_O!
set /p choice="!T_CH!"
set "MODE="
if "!choice!"=="" set "MODE=groq" & set "COMPOSE_FILE=docker-compose-groq.yml"
if "!choice!"=="1" set "MODE=groq" & set "COMPOSE_FILE=docker-compose-groq.yml"
if "!choice!"=="2" set "MODE=ollama" & set "COMPOSE_FILE=docker-compose-simple.yml"
if not defined MODE ( echo [ERROR] !T_BAD! !choice! & pause & exit /b 1 )

REM --- Groq: key entry step (.env, git-ignored) ------------------------------
if /i "%MODE%"=="groq" (
  set "NEEDKEY=1"
  if exist .env findstr /b "GROQ_API_KEY=gsk" .env >nul 2>&1 && (
    set /p usekey="!T_HASKEY!"
    if /i "!usekey!"=="" set "NEEDKEY="
    if /i "!usekey!"=="y" set "NEEDKEY="
  )
  if defined NEEDKEY (
    echo !T_NEED!
    set /p key="!T_PASTE!"
    if "!key!"=="" ( echo [ERROR] !T_NOKEY! & pause & exit /b 1 )
    > .env echo GROQ_API_KEY=!key!
    echo !T_SAVED!
  )
)

REM --- Build & start ---------------------------------------------------------
echo !T_START! (%MODE%) !T_NOTE!
docker compose -f "%COMPOSE_FILE%" up --build -d
if errorlevel 1 ( echo [ERROR] !T_FAIL! & pause & exit /b 1 )

REM --- Wait until ready (UI + orchestrator both answer 200) ------------------
echo !T_WAIT!
set "READY="
for /l %%i in (1,1,90) do (
  if not defined READY (
    for /f %%c in ('curl -s -o nul -w "%%{http_code}" --max-time 3 "%UI_URL%" 2^>nul') do set "UI=%%c"
    for /f %%c in ('curl -s -o nul -w "%%{http_code}" --max-time 3 "%MODES_URL%" 2^>nul') do set "MD=%%c"
    if "!UI!"=="200" if "!MD!"=="200" set "READY=1"
    if not defined READY ( <nul set /p "=." & timeout /t 4 /nobreak >nul )
  )
)
echo.
if not defined READY ( echo [ERROR] !T_TIMEOUT! docker compose -f %COMPOSE_FILE% logs & pause & exit /b 1 )

echo !T_READY! %UI_URL%
start "" "%UI_URL%"
echo.
echo !T_STOP!
pause
