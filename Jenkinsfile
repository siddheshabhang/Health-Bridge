pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 60, unit: 'MINUTES')
    }

    parameters {
        booleanParam(
            name: 'BUILD_DOCKER_IMAGES',
            defaultValue: true,
            description: 'Build all Docker images after Maven and frontend builds.'
        )
        booleanParam(
            name: 'PUSH_TO_DOCKER_HUB',
            defaultValue: false,
            description: 'Tag and push all images to Docker Hub after building. Requires dockerhub-credentials in Jenkins.'
        )
        booleanParam(
            name: 'RUN_COMPOSE_SMOKE',
            defaultValue: false,
            description: 'Start the full Compose stack for a smoke check, then tear it down.'
        )
    }

    environment {
        PATH               = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        SERVICE_MODULES    = 'services/discovery-server,services/api-gateway-service,services/auth-identity-service,services/consent-service,services/notification-audit-service,services/hospital-a-service,services/hospital-b-service,services/hie-fhir-exchange-service,services/admin-reporting-service'
        DOCKER_BUILDKIT    = '1'
        COMPOSE_DOCKER_CLI_BUILD = '1'

        // ── Docker Hub ──────────────────────────────────────────────────────
        // Set DOCKER_HUB_USERNAME in Jenkins → Manage Jenkins → System
        // or override here for your account.
        DOCKER_HUB_USERNAME = "${env.DOCKER_HUB_USERNAME ?: 'siddheshhh'}"
        DOCKER_HUB_REPO     = "${DOCKER_HUB_USERNAME}"

        // Short 8-char commit SHA used as the image tag
        SHORT_SHA = "${env.GIT_COMMIT ? env.GIT_COMMIT.take(8) : 'latest'}"
    }

    stages {
        stage('Tool Versions') {
            steps {
                sh 'java -version'
                sh 'node -v'
                sh 'npm -v'
                sh 'docker --version'
                sh 'docker compose version'
            }
        }

        stage('Backend Package') {
            steps {
                sh './backend/mvnw -f pom.xml -pl "$SERVICE_MODULES" -am -DskipTests package'
            }
        }

        stage('Frontend Build') {
            steps {
                script {
                    ['frontend', 'frontend-hospital-a', 'frontend-hospital-b'].each { app ->
                        dir(app) {
                            sh 'npm install --no-audit --no-fund'
                            sh 'npm run build'
                        }
                    }
                }
            }
        }

        stage('Compose Config') {
            steps {
                sh 'docker compose config --quiet'
            }
        }

        stage('Docker Images') {
            when {
                expression { return params.BUILD_DOCKER_IMAGES }
            }
            steps {
                sh 'docker compose build'
            }
        }

        // ── Phase 8: Docker Hub push ─────────────────────────────────────────
        stage('Docker Hub Push') {
            when {
                allOf {
                    expression { return params.BUILD_DOCKER_IMAGES }
                    expression { return params.PUSH_TO_DOCKER_HUB }
                }
            }
            steps {
                // dockerhub-credentials must be added in:
                //   Jenkins → Manage Jenkins → Credentials → (global) → Add Credentials
                //   Kind: Username with password
                //   ID:   dockerhub-credentials
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'

                    script {
                        // Map: compose service name → local image name (from docker-compose.yml)
                        def images = [
                            'discovery-server'          : 'health-bridge/discovery-server:local',
                            'api-gateway-service'       : 'health-bridge/api-gateway-service:local',
                            'auth-identity-service'     : 'health-bridge/auth-identity-service:local',
                            'consent-service'           : 'health-bridge/consent-service:local',
                            'hospital-a-service'        : 'health-bridge/hospital-a-service:local',
                            'hospital-b-service'        : 'health-bridge/hospital-b-service:local',
                            'hie-fhir-exchange-service' : 'health-bridge/hie-fhir-exchange-service:local',
                            'notification-audit-service': 'health-bridge/notification-audit-service:local',
                            'admin-reporting-service'   : 'health-bridge/admin-reporting-service:local',
                            'frontend'                  : 'health-bridge/frontend:local',
                            'frontend-hospital-a'       : 'health-bridge/frontend-hospital-a:local',
                            'frontend-hospital-b'       : 'health-bridge/frontend-hospital-b:local',
                        ]

                        images.each { svc, localImage ->
                            def hubImage = "${DOCKER_HUB_REPO}/health-bridge-${svc}"
                            // Tag with short SHA
                            sh "docker tag ${localImage} ${hubImage}:${SHORT_SHA}"
                            sh "docker push ${hubImage}:${SHORT_SHA}"
                            // Also tag as latest
                            sh "docker tag ${localImage} ${hubImage}:latest"
                            sh "docker push ${hubImage}:latest"
                            echo "Pushed ${hubImage}:${SHORT_SHA} and ${hubImage}:latest"
                        }
                    }

                    sh 'docker logout'
                }
            }
        }

        // ── Phase 8: Rolling update in Kubernetes ────────────────────────────
        stage('Kubernetes Rollout') {
            when {
                allOf {
                    expression { return params.BUILD_DOCKER_IMAGES }
                    expression { return params.PUSH_TO_DOCKER_HUB }
                }
            }
            steps {
                script {
                    def deployments = [
                        'discovery-server',
                        'api-gateway-service',
                        'auth-identity-service',
                        'consent-service',
                        'hospital-a-service',
                        'hospital-b-service',
                        'hie-fhir-exchange-service',
                        'notification-audit-service',
                        'admin-reporting-service',
                        'frontend',
                        'frontend-hospital-a',
                        'frontend-hospital-b',
                    ]

                    deployments.each { svc ->
                        def hubImage = "${DOCKER_HUB_REPO}/health-bridge-${svc}:${SHORT_SHA}"
                        sh "kubectl set image deployment/${svc} ${svc}=${hubImage} -n health-bridge || echo 'Deployment ${svc} not found, skipping.'"
                    }

                    // Wait for all rollouts to complete
                    deployments.each { svc ->
                        sh "kubectl rollout status deployment/${svc} -n health-bridge --timeout=300s || echo 'Rollout check skipped for ${svc}'"
                    }
                }
            }
        }

        stage('Compose Smoke') {
            when {
                expression { return params.RUN_COMPOSE_SMOKE }
            }
            steps {
                sh 'docker compose up -d'
                sh 'docker compose ps'
            }
            post {
                unsuccessful {
                    sh 'docker compose logs --no-color --tail=200 || true'
                }
                always {
                    sh 'docker compose down --remove-orphans || true'
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts(
                artifacts: 'services/*/target/*.jar,frontend/dist/**,frontend-hospital-a/dist/**,frontend-hospital-b/dist/**',
                allowEmptyArchive: true,
                fingerprint: true
            )
        }
        success {
            echo "✅ Build ${env.BUILD_NUMBER} succeeded — SHA: ${SHORT_SHA}"
        }
        failure {
            echo "❌ Build ${env.BUILD_NUMBER} FAILED — check logs above."
        }
    }
}

