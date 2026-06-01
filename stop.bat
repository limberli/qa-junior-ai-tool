@echo off
chcp 65001 >nul
REM Stops the AI JUN stack (both Groq and Ollama compose files).
cd /d "%~dp0"

for %%f in (docker-compose-groq.yml docker-compose-simple.yml) do (
  docker compose -f "%%f" down
)

echo Stopped.
pause
