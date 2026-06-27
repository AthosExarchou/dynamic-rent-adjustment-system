pipeline {
    agent any

    environment {
        DOCKER_COMPOSE_CMD = 'docker compose -f docker-compose.prod.yml'
    }

    stages {
        stage('Backend Build') {
            steps {
                dir('backend') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Backend Tests') {
            steps {
                dir('backend') {
                    sh 'mvn test'
                }
            }
        }

        stage('Frontend Install') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('frontend') {
                    sh 'npm run build'
                }
            }
        }

        stage('Docker Image Build') {
            steps {
                sh "${DOCKER_COMPOSE_CMD} build"
            }
        }

        stage('Docker Compose Deployment') {
            steps {
                sh "${DOCKER_COMPOSE_CMD} up -d"
            }
        }

        stage('Health Check') {
            steps {
                sleep time: 15, unit: 'SECONDS'
                sh "curl -f http://localhost:80/actuator/health"
            }
        }

        stage('Archive Logs') {
            steps {
                sh "${DOCKER_COMPOSE_CMD} logs > docker_deployment.log"
                archiveArtifacts artifacts: 'docker_deployment.log', allowEmptyArchive: true
            }
        }
    }
}
