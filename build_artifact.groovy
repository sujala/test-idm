/**
 * This job builds an artifact given a commit (sha1 or branch name).
 *
 * The job is configured with 2 params:
 * FORCE_BUILD - If true, the artifact will be built even if it is not tagged with an artifact version.
 *               Otherwise, fail the job if no tags are present.
 * RELEASE - If true, the artifact built will be a release (non-snapshot) artifact
 */
def build(scm) {

    library "tesla@v0.8.2"

    // Setup the git configuration so it can be passed as an arg to common code
    def buildSteps
    node('master') {
        cleanWs()
        checkout scm
        buildSteps = load('jenkins-scripts/jenkins-build-steps.groovy')
    }

    // Verify that the git commit has been tagged as passing tests
    stage('Verify') {
        node('master') {
            checkout scm
            // Search for the first tag that looks like a valid artifact ID. This will be the version we use to build the artifact with.
            env.VERSION_OVERRIDE = sh (
                script: """
                            if [[ -z \$(git tag -l --points-at HEAD) ]]; then
                                echo ''
                            else
                                git tag -l --points-at HEAD | grep -E \'[0-9]+\\.[0-9]+\\.[0-9]+-[0-9]+\' -m 1
                            fi
                        """,
                returnStdout: true
            ).trim()
            echo "env.VERSION_OVERRIDE = " + env.VERSION_OVERRIDE
            env.VERSION_OVERRIDE = env.VERSION_OVERRIDE == null ? '': env.VERSION_OVERRIDE

            // Fail the build if the commit is not tagged with a version tag and we are not forcing a build.
            if (env.FORCE_BUILD == 'false' && env.VERSION_OVERRIDE.length() <= 0) {
                error("The commit is not tagged with a version tag and 'FORCE_BUILD' is not true.")
            }

            if (env.VERSION_OVERRIDE.length() > 0) {
                echo "Found tag ${env.VERSION_OVERRIDE}. Using this tag to override the version of the artifact built."
            } else {
                echo "No version tag found. Not overriding the artifact version."
            }
        }
    }

    stage('Build Artifact') {
        node('java') {
            cleanWs()
            checkout scm
            // Remove the snapshot part of the tag. That is added to the version in the build if it is a snapshot.
            env.VERSION_OVERRIDE = env.VERSION_OVERRIDE.replace('-SNAPSHOT', '')
            sh """
                set +x
                source /tmp/secrets/secrets.sh
                sed -i -e "s|artifactory_user=.*|artifactory_user=\${ARTIFACTORY_USER}|g" gradle.properties
                sed -i -e "s|artifactory_password=.*|artifactory_password=\${ARTIFACTORY_PASSWORD}|g" gradle.properties
                set -x
            """
            if (env.VERSION_OVERRIDE.length() > 0) {
                sh """
                    ./gradlew clean build -x test -x noncommittest -x committest -x commontest artifactorypublish --stacktrace -Pbuild_release=${env.release} -Pversion_override=${env.version_override}
                """
            } else {
                sh """
                    ./gradlew clean build -x test -x noncommittest -x committest -x commontest artifactorypublish --stacktrace -Pbuild_release=${env.release}
                """
            }
        }
    }

}

return this