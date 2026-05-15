# Pre-Kubernetes Readiness

This checkpoint captures the work that should be true before creating Kubernetes manifests.

## Current Status

- Maven multi-module backend compiles.
- API Gateway routes preserve the frontend-facing paths.
- Docker Compose config is valid.
- HIE now calls Auth/Identity, Hospital A, Hospital B, and Notification/Audit through HTTP clients instead of directly autowiring their services or repositories.
- Internal service endpoints use `X-Internal-Service-Token` for service-to-service calls.
- HIE component scanning is narrowed to HIE, doctor/patient orchestration, and shared security/error handling packages so copied transition controllers are not exposed at runtime.

## Hardened Internal Routes

- Auth/Identity:
  - `GET /internal/identity/patients/{abhaId}/exists`
  - `GET /internal/identity/patients/{abhaId}`
  - `POST /internal/identity/patient-links`
- Hospital A:
  - `POST /internal/hospitalA/fhir-bundle`
  - `GET /internal/hospitalA/patients/{identifier}`
  - `POST /internal/hospitalA/patients`
  - `GET /internal/hospitalA/patients/{abhaId}/consults/exists`
  - `GET /internal/hospitalA/patients/{abhaId}/consultations`
- Hospital B:
  - `POST /internal/hospitalB/fhir-bundle`
  - `GET /internal/hospitalB/patients/{identifier}`
  - `POST /internal/hospitalB/patients`
  - `GET /internal/hospitalB/patients/{abhaId}/consults/exists`
  - `GET /internal/hospitalB/patients/{abhaId}/consultations`
- Notification/Audit:
  - `POST /internal/notifications/consent-request`
  - `POST /internal/audit/transfers/pending`
  - `PATCH /internal/audit/transfers/{id}/success`
  - `PATCH /internal/audit/transfers/{id}/failure`

## Verification

Run:

```bash
scripts/verify-pre-k8s-readiness.sh
```

This packages the backend, validates Compose, builds all frontends, and fails if the main HIE orchestration code regresses to direct platform/hospital repository coupling.

## Remaining Before Kubernetes

- Run the full Compose stack and perform an end-to-end smoke test:
  - login
  - consent request and approval
  - Hospital A native consult
  - HIE pull
  - Hospital B Mongo persistence
  - audit and notification visibility
- Remove copied transition packages from HIE after the Compose smoke path is green.
- Replace Hospital A/B direct `ConsentStore` use in patient-push flows with an internal Consent client.
- Only then create Kubernetes Deployments, Services, ConfigMaps, Secrets, Ingress, and observability manifests.
