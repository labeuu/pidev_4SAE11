def runMicroservicePipeline(Map cfg) {
    pipeline {
        agent any
        options {
            timestamps()
            disableConcurrentBuilds()
            buildDiscarder(logRotator(numToKeepStr: "25", artifactNumToKeepStr: "10"))
        }
        parameters {
            string(name: "REPO_URL", defaultValue: cfg.repoUrl ?: "https://github.com/YOUR_ORG/YOUR_REPO.git", description: "Git repository URL")
            string(name: "BRANCH", defaultValue: "main", description: "Git branch")
            string(name: "IMAGE_REPO", defaultValue: "docker.io/YOUR_DOCKERHUB_USERNAME", description: "Container image repository prefix")
            string(name: "IMAGE_TAG", defaultValue: "", description: "Image tag override. Empty uses build number")
            booleanParam(name: "PUSH_IMAGE", defaultValue: true, description: "Push image to Docker registry")
            booleanParam(name: "RUN_SONARQUBE", defaultValue: true, description: "Run SonarQube analysis and quality gate")
            booleanParam(name: "TRIGGER_DOWNSTREAM", defaultValue: false, description: "Trigger downstream jobs after success")
            string(name: "DOWNSTREAM_JOBS", defaultValue: cfg.downstreamJobs ?: "", description: "Comma-separated Jenkins jobs to trigger")
        }
        environment {
            SERVICE_PATH = cfg.servicePath
            GITHUB_CREDS_ID = "GithubCredentials"
            DOCKERHUB_CREDS_ID = "DockerHubCredentials"
            SONAR_TOKEN_CREDENTIALS_ID = "SonarQubeToken"
            SONARQUBE_SERVER_NAME = "SonarQube"
            TAG = "${params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER}"
            DOCKER_IMAGE = "${params.IMAGE_REPO}/${cfg.imageName}"
            FULL_IMAGE = "${params.IMAGE_REPO}/${cfg.imageName}:${params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER}"
        }
        stages {
            stage("Checkout") {
                steps {
                    checkout([
                        $class: "GitSCM",
                        branches: [[name: "*/${params.BRANCH}"]],
                        userRemoteConfigs: [[url: params.REPO_URL, credentialsId: env.GITHUB_CREDS_ID]]
                    ])
                }
            }
            stage("Detect Build Tool") {
                steps {
                    script {
                        if (fileExists("${env.SERVICE_PATH}/pom.xml")) {
                            env.BUILD_TOOL = "maven"
                        } else if (fileExists("${env.SERVICE_PATH}/build.gradle") || fileExists("${env.SERVICE_PATH}/build.gradle.kts")) {
                            env.BUILD_TOOL = "gradle"
                        } else if (fileExists("${env.SERVICE_PATH}/package.json")) {
                            env.BUILD_TOOL = "node"
                        } else {
                            error("No supported build tool found in ${env.SERVICE_PATH}")
                        }
                    }
                }
            }
            stage("Build") {
                steps {
                    dir("${env.SERVICE_PATH}") {
                        script {
                            if (env.BUILD_TOOL == "maven") {
                                sh "if [ -f mvnw ]; then chmod +x mvnw && ./mvnw -B -DskipTests clean compile; else mvn -B -DskipTests clean compile; fi"
                            } else if (env.BUILD_TOOL == "gradle") {
                                sh "if [ -f gradlew ]; then chmod +x gradlew && ./gradlew clean assemble -x test; else gradle clean assemble -x test; fi"
                            } else {
                                sh "npm ci"
                                sh "npm run build --if-present"
                            }
                        }
                    }
                }
            }
            stage("Test") {
                steps {
                    dir("${env.SERVICE_PATH}") {
                        script {
                            if (env.BUILD_TOOL == "maven") {
                                sh "if [ -f mvnw ]; then ./mvnw -B test; else mvn -B test; fi"
                            } else if (env.BUILD_TOOL == "gradle") {
                                sh "if [ -f gradlew ]; then ./gradlew test; else gradle test; fi"
                            } else {
                                sh "npm test -- --watch=false || npm test || true"
                            }
                        }
                    }
                }
                post {
                    always {
                        junit allowEmptyResults: true, testResults: "${env.SERVICE_PATH}/target/surefire-reports/*.xml, ${env.SERVICE_PATH}/build/test-results/test/*.xml"
                    }
                }
            }
            stage("Package") {
                steps {
                    dir("${env.SERVICE_PATH}") {
                        script {
                            if (env.BUILD_TOOL == "maven") {
                                sh "if [ -f mvnw ]; then ./mvnw -B -DskipTests package; else mvn -B -DskipTests package; fi"
                            } else if (env.BUILD_TOOL == "gradle") {
                                sh "if [ -f gradlew ]; then ./gradlew bootJar -x test || ./gradlew assemble -x test; else gradle assemble -x test; fi"
                            } else {
                                sh "npm pack >/dev/null 2>&1 || true"
                            }
                        }
                    }
                }
            }
            stage("SonarQube Analysis") {
                when { expression { params.RUN_SONARQUBE } }
                steps {
                    dir("${env.SERVICE_PATH}") {
                        withCredentials([string(credentialsId: env.SONAR_TOKEN_CREDENTIALS_ID, variable: "SONAR_TOKEN")]) {
                            withSonarQubeEnv("${env.SONARQUBE_SERVER_NAME}") {
                                script {
                                    def sonarProjectKey = cfg.imageName.replaceAll("[^a-zA-Z0-9_.:-]", "-")
                                    if (env.BUILD_TOOL == "maven") {
                                        sh "if [ -f mvnw ]; then ./mvnw -B sonar:sonar -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} -Dsonar.token=${SONAR_TOKEN}; else mvn -B sonar:sonar -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} -Dsonar.token=${SONAR_TOKEN}; fi"
                                    } else if (env.BUILD_TOOL == "gradle") {
                                        sh "if [ -f gradlew ]; then ./gradlew sonarqube -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} -Dsonar.token=${SONAR_TOKEN}; else gradle sonarqube -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} -Dsonar.token=${SONAR_TOKEN}; fi"
                                    } else {
                                        sh "npx -y sonar-scanner -Dsonar.projectKey=${sonarProjectKey} -Dsonar.projectName=${cfg.imageName} -Dsonar.sources=. -Dsonar.token=${SONAR_TOKEN}"
                                    }
                                }
                            }
                        }
                    }
                }
            }
            stage("Quality Gate") {
                when { expression { params.RUN_SONARQUBE } }
                steps {
                    timeout(time: 10, unit: "MINUTES") {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
            stage("Build Docker Image") {
                steps {
                    dir("${env.SERVICE_PATH}") {
                        sh "docker build -t ${FULL_IMAGE} -t ${DOCKER_IMAGE}:latest ."
                    }
                }
            }
            stage("Push Docker Image") {
                when { expression { params.PUSH_IMAGE } }
                steps {
                    withCredentials([usernamePassword(credentialsId: env.DOCKERHUB_CREDS_ID, usernameVariable: "DH_USER", passwordVariable: "DH_PASS")]) {
                        sh "echo \"${DH_PASS}\" | docker login -u \"${DH_USER}\" --password-stdin"
                        sh "docker push ${FULL_IMAGE}"
                        sh "docker push ${DOCKER_IMAGE}:latest"
                        sh "docker logout || true"
                    }
                }
            }
            stage("Trigger Downstream") {
                when { expression { params.TRIGGER_DOWNSTREAM && params.DOWNSTREAM_JOBS?.trim() } }
                steps {
                    script {
                        params.DOWNSTREAM_JOBS.split(",").collect { it.trim() }.findAll { it }.each { nextJob ->
                            build job: nextJob, wait: false, parameters: [
                                string(name: "REPO_URL", value: params.REPO_URL),
                                string(name: "BRANCH", value: params.BRANCH),
                                string(name: "IMAGE_REPO", value: params.IMAGE_REPO),
                                string(name: "IMAGE_TAG", value: params.IMAGE_TAG),
                                booleanParam(name: "PUSH_IMAGE", value: params.PUSH_IMAGE),
                                booleanParam(name: "RUN_SONARQUBE", value: params.RUN_SONARQUBE)
                            ]
                        }
                    }
                }
            }
        }
        post {
            always {
                cleanWs(deleteDirs: true, disableDeferredWipeout: true)
            }
        }
    }
}

return this
