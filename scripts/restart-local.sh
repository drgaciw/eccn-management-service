#!/usr/bin/env bash
# Reliable local restart for ECCN Management Service (port 8081).
#
# Why this exists:
# - Attaching `mvn spring-boot:run` to an interactive/agent shell gets SIGTERM/SIGKILL
#   when that shell times out or is cancelled — looks like a flaky restart.
# - Ad-hoc `kill`/`pkill` without waiting leaves port 8081 half-bound.
# - Process must be detached (setsid + nohup) so it survives the launching shell.
#
# Usage:
#   ./scripts/restart-local.sh          # stop + start detached
#   ./scripts/restart-local.sh stop
#   ./scripts/restart-local.sh start
#   ./scripts/restart-local.sh status
#
# Auth (local defaults, overridable via env):
#   username: devuser
#   password: devpass123
#   (uses {noop} id so it works with DelegatingPasswordEncoder)

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PORT="${SERVER_PORT:-8081}"
PID_FILE="${ROOT}/.local-backend.pid"
LOG_FILE="${ROOT}/.local-backend.log"

# Prefer {noop} for local so bash does not mangle bcrypt `$` sequences.
# Override with SPRING_SECURITY_USER_* or SECURITY_ADMIN_* as needed.
export SPRING_SECURITY_USER_NAME="${SPRING_SECURITY_USER_NAME:-devuser}"
export SPRING_SECURITY_USER_PASSWORD="${SPRING_SECURITY_USER_PASSWORD:-"{noop}devpass123"}"
# Keep README env names in sync when callers set only SECURITY_ADMIN_*.
export SECURITY_ADMIN_NAME="${SECURITY_ADMIN_NAME:-${SPRING_SECURITY_USER_NAME}}"
export SECURITY_ADMIN_PASSWORD="${SECURITY_ADMIN_PASSWORD:-${SPRING_SECURITY_USER_PASSWORD}}"
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

  # Prefer killing the whole session recorded in the pid file (maven + app).
  if [[ -f "${PID_FILE}" ]]; then
    local starter
    starter="$(cat "${PID_FILE}" 2>/dev/null || true)"
    if [[ -n "${starter}" ]] && kill -0 "${starter}" 2>/dev/null; then
      # Negative PGID = entire process group (setsid made starter the session leader).
      kill -TERM -- "-${starter}" 2>/dev/null || kill -TERM "${starter}" 2>/dev/null || true
    fi
  fi

  local pids
  pids="$(pids_on_port || true)"
  for pid in ${pids}; do
    kill -TERM "${pid}" 2>/dev/null || true
  done
  pkill -f "eccn-management-service.*spring-boot:run" 2>/dev/null || true
  pkill -f "EccnManagementServiceApplication" 2>/dev/null || true

  for _ in $(seq 1 30); do
    if [[ -z "$(pids_on_port || true)" ]]; then
      break
    fi
    sleep 0.5
  done

  if [[ -n "$(pids_on_port || true)" ]]; then
    echo "Still listening; sending SIGKILL ..."
    if [[ -f "${PID_FILE}" ]]; then
      local starter
      starter="$(cat "${PID_FILE}" 2>/dev/null || true)"
      if [[ -n "${starter}" ]]; then
        kill -KILL -- "-${starter}" 2>/dev/null || kill -KILL "${starter}" 2>/dev/null || true
      fi
    fi
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
  # that may later be SIGTERM/SIGKILL'd when the session times out.
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
        echo "Auth: ${SPRING_SECURITY_USER_NAME} / devpass123 (override via SPRING_SECURITY_USER_PASSWORD)"
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
