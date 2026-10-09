#!/usr/bin/env bash
set -euo pipefail
task_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
task_jar="$task_root/apps/backend/target/pet-platform-backend-0.0.0-SNAPSHOT.jar"
if [[ ! -f "$task_jar" ]]; then
  echo '缺少生产JAR；请先在apps/backend执行./mvnw clean verify' >&2
  exit 2
fi
case "${1:-}" in
  bootstrap) task_main='com.pet.platform.identity.infrastructure.bootstrap.BootstrapCommand' ;;
  platform-bootstrap) task_main='com.pet.platform.identity.infrastructure.bootstrap.PlatformBootstrapCommand' ;;
  employee-read-upgrade) task_main='com.pet.platform.identity.infrastructure.bootstrap.EmployeeReadUpgradeCommand' ;;
  migrate) task_main='com.pet.platform.identity.infrastructure.bootstrap.MigrationCommand' ;;
  *) echo '用法：scripts/backend-identity.sh migrate | bootstrap | platform-bootstrap | employee-read-upgrade [选项]' >&2; exit 2 ;;
esac
shift
task_java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$task_java" -Dloader.main="$task_main" -cp "$task_jar" org.springframework.boot.loader.launch.PropertiesLauncher "$@"
