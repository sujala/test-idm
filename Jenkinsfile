library "tesla@v0.8.2"

def gitRepo = 'github.rackspace.com/cloud-identity-dev/cloud-identity'
def githubUrl = "https://${gitRepo}"
def namespace = 'customer-identity-cicd'

properties([
    pipelineTriggers([cron('@daily')]),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: githubUrl],
    buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')),
    disableConcurrentBuilds()
])

def scm = [
    $class: 'GitSCM',
    branches: [[name: 'master']],
    userRemoteConfigs: [[
        url: githubUrl,
        name: 'origin',
        credentialsId: 'github-service-account'
    ]]
]

def testsPassed = false
try {

    // Setup the git configuration so it can be passed as an arg to common code
    stage('Setup') {
        node('master') {
            cleanWs()
            checkout scm
            buildSteps = load('jenkins-scripts/jenkins-build-steps.groovy')
            buildSteps.setRsiEndpoints(scm)
        }
    }

    // Build and publish the artifact to test
    env.IDM_VERSION = buildSteps.publishArtifact(scm)
    env.NAMESPACE_NAME = namespace
    println "idmVersion = ${env.IDM_VERSION}"


    // Build images for the sandbox environment
    def sandboxImageTag = env.IDM_VERSION
    buildSteps.buildImages(sandboxImageTag, env.IDM_VERSION, 'master')

    def releaseName = null
    try {
        // Deploy sandbox env
        env.SANDBOX_NAME = "pipeline-nct-${env.BUILD_NUMBER}"
        println "env.SANDBOX_NAME = " + env.SANDBOX_NAME
        releaseName = buildSteps.createStandboxEnv(sandboxImageTag, env.SANDBOX_NAME)
        buildSteps.deploySandboxEnvironment(releaseName)

        // Run non-commit tests
        buildSteps.runNonCommitTests(scm)
    } finally {
        buildSteps.destroySandboxEnv(releaseName)
    }

    try {
        // Deploy sandbox env
        env.SANDBOX_NAME = "pipeline-jt-${env.BUILD_NUMBER}"
        println "env.SANDBOX_NAME = " + env.SANDBOX_NAME
        releaseName = buildSteps.createStandboxEnv(sandboxImageTag, env.SANDBOX_NAME)
        buildSteps.deploySandboxEnvironment(releaseName)

        // Run Johnny tests
        buildSteps.runJohnnyTests(scm)
    } finally {
        buildSteps.destroySandboxEnv(releaseName)
    }

    testsPassed = true
} catch (exc) {
    // If tests fail, delete the artifact from artifactory
    try {
        node('java') {
            sh """
            set +x
            source /tmp/secrets/secrets.sh
            curl -X DELETE https://artifacts.rackspace.net/artifactory/identity-maven-local/com/rackspace/idm/${env.IDM_VERSION}/ -H "X-JFrog-Art-Api: \${ARTIFACTORY_PASSWORD}" -v
            """
        }
    } finally {
        // trigger failed job
        build env.JOB_NAME
    }
    throw exc
} finally {
    if(testsPassed) {
        node('master') {
            cleanWs()
                checkout scm
                sh """
                    set +x
                    git config --global user.email "identity@rackspace.com"
                    git config --global user.name "cid-rsi-dev-svc"
                    git remote remove origin
                    git remote add origin https://${env.GITHUB_ENTERPRISE_USERNAME}:${env.GITHUB_ENTERPRISE_PASSWORD}@${gitRepo}.git
                    set -x
                    git tag -a ${env.IDM_VERSION} -m "Version ${env.IDM_VERSION}"
                    git push origin ${env.IDM_VERSION}
                """
        }

        // trigger push images to Artifactory
        build job: 'rsi-images', parameters: [string(name: 'APP_REVISION', value: env.IDM_VERSION)], wait: false
    }
}
