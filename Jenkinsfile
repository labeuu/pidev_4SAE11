// Global CI pipeline — full stack via Docker Compose (no Kubernetes).
//
// Jenkins: New Item → Pipeline → Definition: Pipeline script from SCM → Script Path: Jenkinsfile
//
// Plugins: Pipeline, Git, Credentials Binding. Agent needs Docker Engine + Compose v2 (`docker compose`).
//
// Credentials (Jenkins → Manage Credentials):
//   - Git/SCM: configure in the job (e.g. GithubCredentials).
//   - Docker Hub: Username with password, ID must be exactly: DockerHubCrendentials
//     (matches backEnd/Microservices/*/Jenkinsfile). Used for image namespace (build) and optional push.
//
// Optional: `docker compose up` smoke tests are not in this pipeline (slow; needs extra host files
// for Firebase/calendar mounts). See docker-compose.yml and firebase-credentials/README.md.

pipeline {
    agent any

    parameters {
        booleanParam(
            name: 'PUSH_IMAGES',
            defaultValue: false,
            description: 'After a successful build, log in to Docker Hub and push all compose-tagged images'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare .env') {
            steps {
                sh 'cp -f .env.example .env'
            }
        }

        stage('Compose validate') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'DockerHubCrendentials',
                    usernameVariable: 'DH_USER',
                    passwordVariable: 'DH_PASS'
                )]) {
                    sh '''
                        export DOCKER_HUB_USERNAME="${DH_USER}"
                        export TAG="${BUILD_NUMBER}"
                        docker compose -f docker-compose.yml config -q
                    '''
                }
            }
        }

        stage('Compose build') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'DockerHubCrendentials',
                    usernameVariable: 'DH_USER',
                    passwordVariable: 'DH_PASS'
                )]) {
                    sh '''
                        export DOCKER_HUB_USERNAME="${DH_USER}"
                        export TAG="${BUILD_NUMBER}"
                        export DOCKER_BUILDKIT=1
                        docker compose -f docker-compose.yml build --parallel
                    '''
                }
            }
        }

        stage('Compose push') {
            when {
                expression { return params.PUSH_IMAGES }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'DockerHubCrendentials',
                    usernameVariable: 'DH_USER',
                    passwordVariable: 'DH_PASS'
                )]) {
                    sh '''
                        export DOCKER_HUB_USERNAME="${DH_USER}"
                        export TAG="${BUILD_NUMBER}"
                        echo "${DH_PASS}" | docker login -u "${DH_USER}" --password-stdin
                        docker compose -f docker-compose.yml push
                        docker logout || true
                    '''
                }
            }
        }
    }

    post {
        failure {
            echo 'Global compose pipeline failed'
        }
        success {
            echo 'Global compose pipeline succeeded'
        }
    }
}
