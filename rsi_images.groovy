/**
 * This job pushes docker images to artifactory registry based of the provided artifact version.
 *
 * The job is configured with 1 params:
 * APP_REVISION - The identity artifact version
 */
def runBuild(scm) {

    library "tesla@v0.8.2"

    def images = ['ca-directory', 'customer-identity', 'repose', 'active-directory', 'dynamodb']

    stage('Push to artifactory') {
        node('master') {
            cleanWs()
            checkout scm

            openshift.withCluster {
                openshift.withProject('customer-identity-cicd') {
                    env.ARTIFACTORY_ENDPOINT='https://artifacts.rackspace.net'
                    branches = [:]
                    images.each {
                        def name = it
                        branches[name] = {
                            // build configs that push to aritfactory have '-arifactory' append at the end of the image
                            // name
                            def build = openshift.selector('bc', "${name}-artifactory").startBuild(
                                    "--env=identity_version=${env.APP_REVISION}", "--env=artifactory_host=${env.ARTIFACTORY_ENDPOINT}")
                            waitUntil {
                                def status = build.object().status.phase
                                println "build config ${name} is in status ${status}."
                                return status == 'Complete'
                            }
                        }
                    }
                    timeout(20) {
                        parallel branches
                    }
                }
            }
        }

        node('java') {
            images.each {
                def name = it
                // building images only pushes to the latest tag, this copy operation creates a docker images
                // with the APP_REVISION tag
                sh """
                source /tmp/secrets/secrets.sh
                set +x
                curl -X POST ${env.ARTIFACTORY_ENDPOINT}/artifactory/api/copy/identity-docker-local/${name}/latest?to=identity-docker-local/${name}/${env.APP_REVISION} -H "X-JFrog-Art-Api: \${ARTIFACTORY_PASSWORD}"
                """
            }
        }
    }
}

return this
