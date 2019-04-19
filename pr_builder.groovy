def build(scm) {

    library "tesla@v0.8.2"
    library "customer-identity@0.1.0"

    try {

        def namespace = 'customer-identity-cicd'

        // Setup the git configuration so it can be passed as an arg to common code
        stage('Setup') {
            node('master') {
                cleanWs()
                checkout scm
                jenkinsBuildSteps.setRsiEndpoints(scm)
            }
        }

        // Build and publish the artifact to test
        env.IDM_VERSION = jenkinsBuildSteps.publishArtifact(scm)
        env.NAMESPACE_NAME = namespace
        println "idmVersion = ${env.IDM_VERSION}"


        // Build images for the sandbox environment
        def sandboxImageTag = env.IDM_VERSION
        jenkinsBuildSteps.buildImages(sandboxImageTag, env.IDM_VERSION, 'master')

        def releaseName = null
        try {
            // Deploy sandbox env
            env.SANDBOX_NAME = "pr-pipeline-jt-${env.BUILD_NUMBER}"
            println "sandboxName = " + env.SANDBOX_NAME
            releaseName = jenkinsBuildSteps.createStandboxEnv(sandboxImageTag, env.SANDBOX_NAME)
            jenkinsBuildSteps.deploySandboxEnvironment(releaseName)

            // Run Johnny tests
            jenkinsBuildSteps.runJohnnyTests(scm)
        } finally {
            jenkinsBuildSteps.destroySandboxEnv(releaseName)
        }

    } catch (exc) {
        throw exc
    } finally {
        node('master') {
            cleanWs()
        }
        if (env.hasProperty('IDM_VERSION')) {
            node('java') {
                sh """
            set +x
            source /tmp/secrets/secrets.sh
            curl -X DELETE https://artifacts.rackspace.net/artifactory/identity-maven-local/com/rackspace/idm/${env.IDM_VERSION}/ -H "X-JFrog-Art-Api: \${ARTIFACTORY_PASSWORD}" -v
            """
            }
        }
    }
}

return this
