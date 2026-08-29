pipeline {
    agent any

    environment {
        DOCKER_COMPOSE_CMD = 'docker compose -f docker-compose.prod.yml'
    }

    stages {

        // Validate backend using a disposable Maven container
        stage('Backend Tests') {
            steps {
                sh '''
                    docker run --rm \
                        -v "$(pwd)/backend":/app \
                        -w /app \
                        maven:3.9.6-eclipse-temurin-21 \
                        mvn -B test
                '''
            }
        }

        // Validate frontend using a disposable Node container
        stage('Frontend Lint') {
            steps {
                sh '''
                    docker run --rm \
                        -v "$(pwd)/frontend":/app \
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
        // Using --remove-orphans replaces changed containers in-place without
        // tearing down the entire stack first, minimizing downtime.
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
        failure {
            // Stop the stack if the pipeline fails
            echo 'Pipeline failed. Stopping the deployment stack.'
            sh "${DOCKER_COMPOSE_CMD} down --remove-orphans || true"
        }
        success {
            echo 'Deployment successful.'
        }
    }
}
