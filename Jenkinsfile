library "tesla@v0.8.2"

def gitRepo = 'github.rackspace.com/cloud-identity-dev/cloud-identity'
def githubUrl = "https://${gitRepo}"
def namespace = 'customer-identity-cicd'

properties([
    pipelineTriggers([cron('H * * * *')]),
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
        }
    }

    // Build and publish the artifact to test
    env.IDM_VERSION = buildSteps.publishArtifact(scm)
    env.NAMESPACE_NAME = namespace.toUpperCase().replaceAll('-', '_')
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
    node('java') {
        sh """
        set +x
        source /tmp/secrets/secrets.sh
        curl -X DELETE https://artifacts.rackspace.net/artifactory/identity-maven-local/com/rackspace/idm/${env.IDM_VERSION}/ -H "X-JFrog-Art-Api: \${ARTIFACTORY_PASSWORD}" -v
        """
    }
    throw exc
} finally {
    node('master') {
        cleanWs()
        if (testsPassed) {
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
    }
    // Always build off of master again
    build: "$JOB_NAME"
}
