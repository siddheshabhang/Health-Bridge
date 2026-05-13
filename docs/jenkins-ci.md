# Jenkins CI

This repository includes a root `Jenkinsfile` for a local or self-hosted Jenkins job.

## Required Jenkins Agent Tools

- Java 21
- Node.js 22
- npm
- Docker CLI with the Compose v2 plugin
- Permission for the Jenkins agent user to run Docker

The pipeline uses the repository Maven wrapper at `backend/mvnw`, so a global Maven
installation is not required.

On macOS with Homebrew, Jenkins may not load the same shell profile as your terminal.
The `Jenkinsfile` sets `PATH` to include `/opt/homebrew/bin` and `/usr/local/bin`
so Homebrew-installed `node`, `npm`, and `docker` can be found by the Jenkins shell.

## Pipeline Stages

1. Package backend service JARs with Maven.
2. Build the three React frontends.
3. Validate `docker-compose.yml`.
4. Optionally build Docker images.
5. Optionally start the full Compose stack for a smoke check.

## Parameters

- `BUILD_DOCKER_IMAGES`: defaults to `true`.
- `RUN_COMPOSE_SMOKE`: defaults to `false` because it starts the full local stack.

The Compose smoke stage always runs `docker compose down --remove-orphans` afterward.
