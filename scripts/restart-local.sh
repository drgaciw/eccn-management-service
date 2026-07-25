#!/usr/bin/env bash
# Reliable local restart for ECCN Management Service (port 8081).
#
# Why this exists:
# - Attaching `mvn spring-boot:run` to an interactive/agent shell gets SIGKILL
#   when that shell times out or is cancelled — looks like a flaky restart.
# - Ad-hoc `kill`/`pkill` without waiting leaves port 8081 half-bound.
# - Credentials must be bcrypt (or {noop} with DelegatingPasswordEncoder);
#   a bare BCryptPasswordEncoder bean cannot verify `{noop}admin`.
#
# Usage:
#   ./scripts/restart-local.sh          # stop + start detached
#   ./scripts/restart-local.sh stop
#   ./scripts/restart-local.sh start
#   ./scripts/restart-local.sh status

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PORT="${SERVER_PORT:-8081}"
PID_FILE="${ROOT}/.local-backend.pid"
LOG_FILE="${ROOT}/.local-backend.log"

# bcrypt hash of "devpass123" (matches prior UAT/SIT env). Override with env.
export SPRING_SECURITY_USER_NAME="${SPRING_SECURITY_USER_NAME:-devuser}"
export SPRING_SECURITY_USER_PASSWORD="${SPRING_SECURITY_USER_PASSWORD:-\$2a\$10\$qYnnIc04L.NVWGJVFDCvcegL3qUSTMU0BvC4XarsjiSh9a.koP3fe}"
# Avoid GenAI autoconfig failures when keys are missing
export SPRING_AUTOCONFIGURE_EXCLUDE="${SPRING_AUTOCONFIGURE_EXCLUDE:-org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration}"
# Silence noisy OTLP export errors when no collector is running locally
export MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED="${MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED:-false}"
export MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED="${MANAGEMENT_OTLP_TRACING_EXPORT_ENABLED:-false}"

pids_on_port() {
  ss -tlnp 2>/dev/null | awk -v p=":${PORT}" '$4 ~ p"$" {print}' \
    | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | sort -u
}

status() {
  local pids
  pids="$(pids_on_port || true)"
  if [[ -n "${pids}" ]]; then
    echo "UP on :${PORT} (pid(s): ${pids})"
    curl -sS -m 3 "http://127.0.0.1:${PORT}/actuator/health" || true
    echo
    return 0
  fi
  echo "DOWN (:${PORT} free)"
  return 1
}

stop() {
  echo "Stopping anything on :${PORT} ..."
  local pids
  pids="$(pids_on_port || true)"
  if [[ -z "${pids}" ]]; then
    # Also clear stale spring-boot:run wrappers for this project
    pkill -f "eccn-management-service.*spring-boot:run" 2>/dev/null || true
    rm -f "${PID_FILE}"
    echo "Already stopped."
    return 0
  fi

  for pid in ${pids}; do
    # Prefer killing the Java process; also walk parent if it's mvn wrapper
    kill -TERM "${pid}" 2>/dev/null || true
  done
  pkill -f "eccn-management-service.*spring-boot:run" 2>/dev/null || true

  for _ in $(seq 1 30); do
    if [[ -z "$(pids_on_port || true)" ]]; then
      break
    fi
    sleep 0.5
  done

  if [[ -n "$(pids_on_port || true)" ]]; then
    echo "Still listening; sending SIGKILL ..."
    for pid in $(pids_on_port); do
      kill -KILL "${pid}" 2>/dev/null || true
    done
    sleep 1
  fi

  rm -f "${PID_FILE}"
  if [[ -n "$(pids_on_port || true)" ]]; then
    echo "ERROR: port ${PORT} still in use: $(pids_on_port)" >&2
    return 1
  fi
  echo "Stopped."
}

start() {
  if [[ -n "$(pids_on_port || true)" ]]; then
    echo "Already running:"
    status
    return 0
  fi

  echo "Compiling..."
  ./mvnw -q -DskipTests compile

  echo "Starting detached on :${PORT} (log: ${LOG_FILE}) ..."
  # nohup + setsid so the process is not tied to an agent/interactive shell
  # that may later be SIGKILL'd when the session times out.
  : > "${LOG_FILE}"
  setsid nohup ./mvnw spring-boot:run \
    -Dspring-boot.run.arguments="--server.port=${PORT}" \
    >> "${LOG_FILE}" 2>&1 &
  echo $! > "${PID_FILE}"

  echo "Waiting for health..."
  for i in $(seq 1 90); do
    if curl -sS -m 2 -o /dev/null -w '' "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null; then
      code="$(curl -sS -m 2 -o /dev/null -w '%{http_code}' "http://127.0.0.1:${PORT}/actuator/health" || true)"
      if [[ "${code}" == "200" ]]; then
        echo "UP after ${i}s"
        status
        echo "Auth: ${SPRING_SECURITY_USER_NAME} / (password from SPRING_SECURITY_USER_PASSWORD)"
        return 0
      fi
    fi
    # Fail fast if Maven already exited
    if [[ -f "${PID_FILE}" ]] && ! kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
      echo "ERROR: starter process exited early. Last log lines:" >&2
      tail -40 "${LOG_FILE}" >&2 || true
      return 1
    fi
    sleep 1
  done

  echo "ERROR: timed out waiting for health. Last log lines:" >&2
  tail -60 "${LOG_FILE}" >&2 || true
  return 1
}

cmd="${1:-restart}"
case "${cmd}" in
  start) start ;;
  stop) stop ;;
  status) status ;;
  restart) stop; start ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}" >&2
    exit 2
    ;;
esac
