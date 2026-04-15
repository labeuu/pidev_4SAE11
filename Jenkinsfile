// Global CI pipeline — full stack via Docker Compose (no Kubernetes).
//
// Jenkins: New Item → Pipeline → Definition: Pipeline script from SCM → Script Path: Jenkinsfile
//
// Plugins: Pipeline, Git, Credentials Binding. Agent needs Docker Engine + Compose v2 (`docker compose`).
//
// Credentials (Jenkins → Manage Credentials):
//   - GitHub/SCM credentials ID (default): GithubCredentials
//   - Docker Hub credentials ID (default): DockerHubCrendentials
// Both IDs are configurable as pipeline parameters below.
//
// Optional: `docker compose up` smoke tests are not in this pipeline (slow; needs extra host files
// for Firebase/calendar mounts). Compose now includes Ollama + runtime model bootstrap.
// See docker-compose.yml and firebase-credentials/README.md.

pipeline {
    agent any

    parameters {
        string(
            name: 'GITHUB_CREDENTIALS_ID',
            defaultValue: 'GithubCredentials',
            description: 'Jenkins credentials ID used for Git checkout'
        )
        string(
            name: 'DOCKERHUB_CREDENTIALS_ID',
            defaultValue: 'DockerHubCrendentials',
            description: 'Jenkins credentials ID (username/password) for Docker Hub'
        )
        booleanParam(
            name: 'PUSH_IMAGES',
            defaultValue: false,
            description: 'After a successful build, log in to Docker Hub and push all compose-tagged images'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: scm.branches,
                    doGenerateSubmoduleConfigurations: false,
                    extensions: scm.extensions,
                    userRemoteConfigs: [[
                        url: scm.userRemoteConfigs[0].url,
                        credentialsId: params.GITHUB_CREDENTIALS_ID
                    ]]
                ])
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
                    credentialsId: params.DOCKERHUB_CREDENTIALS_ID,
                    usernameVariable: 'DH_USER',
                    passwordVariable: 'DH_PASS'
                )]) {
                    sh '''
                        export DOCKER_HUB_USERNAME="${DH_USER}"
                        export TAG="${BUILD_NUMBER}"
                        export OLLAMA_MODEL="${OLLAMA_MODEL:-gemma3:4b}"
                        docker compose -f docker-compose.yml config -q
                    '''
                }
            }
        }

        stage('Compose build') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: params.DOCKERHUB_CREDENTIALS_ID,
                    usernameVariable: 'DH_USER',
                    passwordVariable: 'DH_PASS'
                )]) {
                    sh '''
                        export DOCKER_HUB_USERNAME="${DH_USER}"
                        export TAG="${BUILD_NUMBER}"
                        export OLLAMA_MODEL="${OLLAMA_MODEL:-gemma3:4b}"
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
                    credentialsId: params.DOCKERHUB_CREDENTIALS_ID,
                    usernameVariable: 'DH_USER',
                    passwordVariable: 'DH_PASS'
                )]) {
                    sh '''
                        export DOCKER_HUB_USERNAME="${DH_USER}"
                        export TAG="${BUILD_NUMBER}"
                        export OLLAMA_MODEL="${OLLAMA_MODEL:-gemma3:4b}"
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
