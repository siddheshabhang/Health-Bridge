pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 45, unit: 'MINUTES')
    }

    parameters {
        booleanParam(
            name: 'BUILD_DOCKER_IMAGES',
            defaultValue: true,
            description: 'Build all Docker images after Maven and frontend builds.'
        )
        booleanParam(
            name: 'RUN_COMPOSE_SMOKE',
            defaultValue: false,
            description: 'Start the full Compose stack for a smoke check, then tear it down.'
        )
    }

    environment {
        PATH = "/opt/homebrew/bin:/usr/local/bin:${env.PATH}"
        SERVICE_MODULES = 'services/discovery-server,services/api-gateway-service,services/auth-identity-service,services/consent-service,services/notification-audit-service,services/hospital-a-service,services/hospital-b-service,services/hie-fhir-exchange-service,services/admin-reporting-service'
        DOCKER_BUILDKIT = '1'
        COMPOSE_DOCKER_CLI_BUILD = '1'
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
    }
}
