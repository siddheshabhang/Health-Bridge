#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

printf 'Checking HIE service-boundary hardening...\n'
if rg -n \
  '@Autowired private (IdentityService|NotificationService|AuditService|HospitalAOPConsultRepository|HospitalBOPConsultRepository|HospitalAPatientRepository|HospitalBPatientRepository|AuthService)' \
  "$ROOT_DIR/services/hie-fhir-exchange-service/src/main/java/com/fhir/hie" \
  "$ROOT_DIR/services/hie-fhir-exchange-service/src/main/java/com/fhir/doctor" \
  "$ROOT_DIR/services/hie-fhir-exchange-service/src/main/java/com/fhir/patient"; then
  printf 'HIE still has direct service/repository coupling. Fix those before Kubernetes.\n' >&2
  exit 1
fi

printf 'Packaging backend reactor...\n'
"$ROOT_DIR/backend/mvnw" -f "$ROOT_DIR/pom.xml" -DskipTests package

printf 'Validating Docker Compose config...\n'
cd "$ROOT_DIR"
docker compose config --quiet

printf 'Building frontends...\n'
for app in frontend frontend-hospital-a frontend-hospital-b; do
  printf '  - %s\n' "$app"
  (cd "$ROOT_DIR/$app" && npm run build)
done

printf 'Pre-Kubernetes readiness checks passed.\n'
