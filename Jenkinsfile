pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'Hello World'
            }
        }

        stage('Test') {
            steps {
                echo 'Hello World'
            }
        }
    }

    post {
        success {
            setBuildStatus("Build succeeded", "SUCCESS")
        }
        failure {
            setBuildStatus("Build failed", "FAILURE")
        }
    }
}

void setBuildStatus(String message, String state) {
    step([
        $class: "GitHubCommitStatusSetter",
        reposSource: [$class: "ManuallyEnteredRepositorySource", url: "${env.GIT_URL}"],
        commitShaSource: [$class: "ManuallyEnteredShaSource", sha: "${env.GIT_COMMIT}"],
        credentialsId: "MYPURECLOUD_GITHUB_TOKEN",
        errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
        statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
    ])
}