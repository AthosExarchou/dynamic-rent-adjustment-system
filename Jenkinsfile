pipeline {
    agent any

    environment {
        DOCKER_COMPOSE_CMD = 'docker compose -f docker-compose.prod.yml'
    }

    stages {

        // Validate backend with a disposable container and a persistent Maven cache
        stage('Backend Tests') {
            steps {
                sh '''
                    docker run --rm \
                        -v "$(pwd)/backend":/app \
                        -v jenkins_maven_cache:/root/.m2 \
                        -w /app \
                        maven:3.9.6-eclipse-temurin-21 \
                        mvn -B test
                '''
            }
        }

        // Validate frontend with a disposable container and a persistent npm cache
        stage('Frontend Lint') {
            steps {
                sh '''
                    docker run --rm \
                        -v "$(pwd)/frontend":/app \
                        -v jenkins_npm_cache:/root/.npm \
                        -w /app \
                        node:20-alpine \
                        sh -c "npm ci && npm run lint"
                '''
            }
        }

        // Build production Docker images
        stage('Docker Image Build') {
            steps {
                sh "${DOCKER_COMPOSE_CMD} build"
            }
        }

        // Deploy the application stack
        stage('Deploy') {
            steps {
                sh "${DOCKER_COMPOSE_CMD} up -d --remove-orphans"
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
                sh "${DOCKER_COMPOSE_CMD} logs > docker_deployment.log"
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
            // Actively stop the stack if a deployment fails midway to avoid zombie containers.
            echo 'Pipeline failed. Tearing down broken deployment stack...'
            sh "${DOCKER_COMPOSE_CMD} down --remove-orphans || true"
        }
        success {
            echo 'Deployment successful.'
        }
    }
}
