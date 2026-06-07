#!/usr/bin/env bash
# Usage: ./scripts/infra.sh <command>
#
# Commands:
#   start         Start all infrastructure containers (detached)
#   stop          Stop all containers, keep volumes
#   restart       Stop then start
#   reset         Stop and delete all volumes (clean slate — re-runs init scripts)
#   status        Show running containers and their health
#   logs [svc]    Tail logs for all services, or a specific one:
#                   user-db   → zeno-postgres-db
#                   restaurant-db → zeno-mysql-db
#                   redis     → zeno-redis-cache

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/infra/docker-compose.yaml"
ENV_FILE="$PROJECT_ROOT/infra/.env"

# Colours
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Colour

info()    { echo -e "${GREEN}[infra]${NC} $*"; }
warn()    { echo -e "${YELLOW}[infra]${NC} $*"; }
error()   { echo -e "${RED}[infra]${NC} $*" >&2; exit 1; }

require_env() {
  if [[ ! -f "$ENV_FILE" ]]; then
    error ".env file not found at infra/.env — copy infra/.env.example and fill in values."
  fi
}

compose() {
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" "$@"
}

# Map short service aliases to container names
resolve_service() {
  case "$1" in
    user-db)          echo "zeno-postgres-db" ;;
    restaurant-db)    echo "zeno-mysql-db" ;;
    redis)            echo "zeno-redis-cache" ;;
    *)                echo "$1" ;;
  esac
}

CMD="${1:-help}"

case "$CMD" in

  start)
    require_env
    info "Starting infrastructure..."
    compose up -d
    info "Done. Containers running:"
    compose ps
    ;;

  stop)
    info "Stopping infrastructure (volumes preserved)..."
    compose down
    info "Stopped."
    ;;

  restart)
    require_env
    info "Restarting infrastructure..."
    compose down
    compose up -d
    info "Restarted."
    ;;

  reset)
    warn "This will DELETE all volumes and data. Press Ctrl+C within 5 seconds to cancel."
    sleep 5
    info "Resetting infrastructure..."
    compose down -v
    compose up -d
    info "Reset complete. Init scripts will re-run on first service startup."
    ;;

  status)
    compose ps
    ;;

  logs)
    SERVICE="${2:-}"
    if [[ -n "$SERVICE" ]]; then
      CONTAINER=$(resolve_service "$SERVICE")
      info "Tailing logs for $CONTAINER..."
      compose logs -f "$CONTAINER"
    else
      info "Tailing logs for all services..."
      compose logs -f
    fi
    ;;

  help|*)
    echo ""
    echo "  Usage: ./scripts/infra.sh <command>"
    echo ""
    echo "  Commands:"
    echo "    start              Start all containers (detached)"
    echo "    stop               Stop all containers, keep volumes"
    echo "    restart            Stop then start"
    echo "    reset              Stop + delete volumes (re-runs init scripts)"
    echo "    status             Show container status"
    echo "    logs               Tail logs for all containers"
    echo "    logs user-db       Tail logs for PostgreSQL"
    echo "    logs restaurant-db Tail logs for MariaDB"
    echo "    logs redis         Tail logs for Redis"
    echo ""
    ;;
esac
