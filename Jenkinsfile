pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        DOCKER_COMPOSE_CMD = "${env.JOB_BASE_NAME.toLowerCase().contains('prod') ? 'docker compose -f docker-compose.prod.yml' : 'docker compose'}"
        COMPOSE_PROJECT_NAME = 'dras'
        DATABASE_PASSWORD = credentials('dras-database-password')
        MAIL_USERNAME     = credentials('dras-mail-username')
        MAIL_PASSWORD     = credentials('dras-mail-password')
    }

    stages {

        // Validate backend with a disposable container and a persistent Maven cache
        stage('Backend Tests') {
            steps {
                sh '''
                    USER_ID=$(id -u)
                    GROUP_ID=$(id -g)
                    docker run --rm \
                        -v "$(pwd)/backend":/app \
                        -v jenkins_maven_cache:/root/.m2 \
                        -w /app \
                        maven:3.9.6-eclipse-temurin-21 \
                        sh -c "mvn -B test; STATUS=\$?; chown -R ${USER_ID}:${GROUP_ID} /app/target 2>/dev/null || true; exit \$STATUS"
                '''
            }
        }

        // Validate frontend with a disposable container and a persistent npm cache
        stage('Frontend Lint') {
            steps {
                sh '''
                    USER_ID=$(id -u)
                    GROUP_ID=$(id -g)
                    docker run --rm \
                        -v "$(pwd)/frontend":/app \
                        -v jenkins_npm_cache:/root/.npm \
                        -w /app \
                        node:20-alpine \
                        sh -c "npm ci && npm run lint; STATUS=\$?; chown -R ${USER_ID}:${GROUP_ID} /app/node_modules 2>/dev/null || true; exit \$STATUS"
                '''
            }
        }

        // Build production Docker images
        stage('Docker Image Build') {
            steps {
                sh "${env.DOCKER_COMPOSE_CMD} build"
            }
        }

        // Deploy the application stack.
        //
        // The frontend_dist named volume must be explicitly deleted before each
        // deployment so that Docker re-initializes it from the newly built
        // frontend artifact image. Without this step, the existing volume
        // persists and nginx would continue serving the old React build.
        stage('Deploy') {
            steps {
                sh "${env.DOCKER_COMPOSE_CMD} down --remove-orphans"
                sh "docker volume rm ${env.COMPOSE_PROJECT_NAME}_frontend_dist || true"
                sh "${env.DOCKER_COMPOSE_CMD} up -d"
            }
        }

        // Verify that the deployed application is responding
        stage('Smoke Test') {
            steps {
                sh 'infrastructure/scripts/smoke-test.sh'
            }
        }

        // Archive deployment logs for troubleshooting
        stage('Archive Logs') {
            steps {
                sh "${env.DOCKER_COMPOSE_CMD} logs > docker_deployment.log"
                archiveArtifacts artifacts: 'docker_deployment.log', allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            // Publish test results to Jenkins so failures are visible in the UI
            junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/TEST-*.xml'
        }
        failure {
            // Actively stop the stack if a deployment fails midway to avoid zombie containers
            echo 'Pipeline failed. Tearing down broken deployment stack...'
            sh "${env.DOCKER_COMPOSE_CMD ?: 'docker compose'} down --remove-orphans || true"
        }
        success {
            echo 'Deployment successful.'
        }
    }
}
