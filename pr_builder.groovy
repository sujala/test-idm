def build(scm) {

    library "tesla@v0.8.2"

    try {

        def namespace = 'customer-identity-cicd'
        def runNonCommitTests = false

        // Note: This will not work yet. We need to update our base chart to have a regex trigger phrase that allows for this
        if (env?.ghprbCommentBody &&  env.ghprbCommentBody.contains('Run all tests')) {
            runNonCommitTests = true
        }

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
        if (runNonCommitTests) {
            try {
                // Deploy sandbox env
                env.SANDBOX_NAME = "pr-pipeline-nct-${env.BUILD_NUMBER}"
                println "env.SANDBOX_NAME = " + env.SANDBOX_NAME
                releaseName = buildSteps.createStandboxEnv(sandboxImageTag, env.SANDBOX_NAME)
                buildSteps.deploySandboxEnvironment(releaseName)

                // Run non-commit tests
                buildSteps.runNonCommitTests(scm)
            } finally {
                buildSteps.destroySandboxEnv(releaseName)
            }
        }

        try {
            // Deploy sandbox env
            env.SANDBOX_NAME = "pr-pipeline-jt-${env.BUILD_NUMBER}"
            println "sandboxName = " + env.SANDBOX_NAME
            releaseName = buildSteps.createStandboxEnv(sandboxImageTag, env.SANDBOX_NAME)
            buildSteps.deploySandboxEnvironment(releaseName)

            // Run Johnny tests
            buildSteps.runJohnnyTests(scm)
        } finally {
            buildSteps.destroySandboxEnv(releaseName)
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
