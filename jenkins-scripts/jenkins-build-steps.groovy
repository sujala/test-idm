/**
 * This method defines a single Jenkins build step that does two things in parallel:
 *     1) Builds and publishes a snapshot artifact to Artifactory
 *     2) Runs commit tests on the code used to build the artifact
 *
 * @return the artifact version that was published to Artifactory
 */
def publishArtifact(scm) {
    artifactVersion = null
    stage('Build Artifact') {
        parallel(
                test: {
                    node('java') {
                        checkout scm
                        try {
                            sh './gradlew clean build commitCodeCoverage -x nonCommitTest --profile'
                        } finally {
                            junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'
                        }
                    }
                },
                publish: {
                    node('java') {
                        checkout scm
                        sh '''
                    set +x
                    ls -alh /tmp/secrets
                    source /tmp/secrets/secrets.sh
                    echo artifactory_user=$ARTIFACTORY_USER >> gradle.properties
                    echo artifactory_password=$ARTIFACTORY_PASSWORD >> gradle.properties
                    set -x
                    ./gradlew clean build -x test -x nonCommitTest -x commitTest -x commonTest artifactoryPublish --stacktrace
                    '''
                        artifactVersion = sh (
                                script: './gradlew appRevision | head -2 | tail -1',
                                returnStdout: true
                        ).trim()
                    }
                }
        )
    }
    return artifactVersion
}

/**
 * Triggers docker builds for the Identity sandbox environment
 *
 * @param sandboxImageTag - the tag to used for tagging build images in the sandbox environment
 * @param identityVersion - the version of Identity to bake into the images
 * @param branch - the branch to use for building the images from
 * @return
 */
def buildImages(sandboxImageTag, identityVersion, branch = master) {
    stage('Build Images') {
        node('master') {
            openshift.withCluster {
                branches = [:]
                ['ca-directory', 'customer-identity', 'repose', 'active-directory', 'dynamodb'].each {
                    def name = it
                    branches[name] = {
                        def build = openshift.selector('bc', name).startBuild(
                                "--commit=${branch}", "--env=identity_version=${identityVersion}", "--env=artifactory_host=https://artifacts.rackspace.net")
                        waitUntil {
                            def status = build.object().status.phase
                            println "build config ${name} is in status ${status}."
                            return status == 'Complete'
                        }
                        def digest = build.object().status.output.to.imageDigest
                        println "Tagging ${name}@${digest} with tag name ${sandboxImageTag}"
                        openshift.tag("${name}@${digest}", "${name}:${sandboxImageTag}")
                    }
                }
                timeout(20) {
                    parallel branches
                }
            }
        }
    }
}

/**
 * Uses the motor API to install an Identity sandbox environment using the docker
 * images specified with a tag.
 *
 * @param sandboxImageTag - the tag used to identify the docker images to deploy
 * @param sandboxName - a suffix to use for all pod names in the sandbox environment
 * @param namespace - the namespace that the sandbox will be installed in
 * @return returns the release name of the sandbox environment
 */
def createStandboxEnv(sandboxImageTag, sandboxName, namespace = 'customer-identity-cicd') {
    // The chart configuration vars
    sandboxChartName = "customer-identity"
    sandboxChartVersion = "0.1.4"
    sandboxConfig = [
            'customer-identity-deploy': [
                    trigger_tag      : "$sandboxImageTag",
                    name             : "customer-identity-$sandboxName",
                    imageName        : "customer-identity",
                    builder_namespace: namespace,
                    env              : [
                            values: [
                                    [name: 'http_proxy'],
                                    [name: 'HTTP_PROXY'],
                                    [name: 'TOMCAT_HTTP_PORT', value: 8083],
                                    [name: 'CA_DIRECTORY_SERVICE_HOST', value: "\$(CA_DIRECTORY_${sandboxName.toUpperCase().replaceAll('-', '_')}_SERVICE_HOST)"],
                                    [name: 'CA_DIRECTORY_SERVICE_PORT', value: "\$(CA_DIRECTORY_${sandboxName.toUpperCase().replaceAll('-', '_')}_SERVICE_PORT)"],
                                    [name: 'ACTIVE_DIRECTORY_SERVICE_HOST', value: "\$(ACTIVE_DIRECTORY_${sandboxName.toUpperCase().replaceAll('-', '_')}_SERVICE_HOST)"],
                                    [name: 'ACTIVE_DIRECTORY_SERVICE_PORT', value: "\$(ACTIVE_DIRECTORY_${sandboxName.toUpperCase().replaceAll('-', '_')}_SERVICE_PORT)"],
                                    [name: 'DYNAMODB_SERVICE_HOST', value: "\$(DYNAMODB_${sandboxName.toUpperCase().replaceAll('-', '_')}_SERVICE_HOST)"],
                                    [name: 'DYNAMODB_SERVICE_PORT', value: "\$(DYNAMODB_${sandboxName.toUpperCase().replaceAll('-', '_')}_SERVICE_PORT)"]
                            ]
                    ]
            ],
            'repose-deploy'           : [
                    trigger_tag      : "$sandboxImageTag",
                    name             : "repose-$sandboxName",
                    imageName        : "repose",
                    builder_namespace: namespace,
                    env              : [
                            values: [
                                    [name: 'http_proxy'],
                                    [name: 'HTTP_PROXY'],
                                    [name: 'CUSTOMER_IDENTITY_SERVICE_HOST', value: "\$(CUSTOMER_IDENTITY_${sandboxName.toUpperCase().replaceAll('-', '_')}_SERVICE_HOST)"],
                                    [name: 'CUSTOMER_IDENTITY_SERVICE_PORT', value: "\$(CUSTOMER_IDENTITY_${sandboxName.toUpperCase().replaceAll('-', '_')}_SERVICE_PORT)"]
                            ]
                    ]
            ],
            'active-directory-deploy' : [
                    trigger_tag      : "$sandboxImageTag",
                    name             : "active-directory-$sandboxName",
                    imageName        : "active-directory",
                    builder_namespace: namespace,
                    env              : [
                            values: [
                                    [name: 'http_proxy'],
                                    [name: 'HTTP_PROXY'],
                                    [name: 'LDAPS_PORT', value: 636]
                            ]
                    ]
            ],
            'ca-directory-deploy'     : [
                    trigger_tag      : "$sandboxImageTag",
                    name             : "ca-directory-$sandboxName",
                    imageName        : "ca-directory",
                    builder_namespace: namespace,
                    env              : [
                            values: [
                                    [name: 'http_proxy'],
                                    [name: 'HTTP_PROXY'],
                                    [name: 'LDAPS_PORT', value: 636]
                            ]
                    ]
            ],
            'dynamodb-deploy'         : [
                    trigger_tag      : "$sandboxImageTag",
                    name             : "dynamodb-$sandboxName",
                    imageName        : "dynamodb",
                    builder_namespace: namespace,
                    port             : 8083,
                    exposeRoute      : false,
                    env              : [
                            values: [
                                    [name: 'http_proxy'],
                                    [name: 'HTTP_PROXY'],
                                    [name: 'DYNAMODB_PORT', value: 8083]
                            ]
                    ]
            ]
    ]

    println "$sandboxConfig"

    def releaseName, token
    stage('Create Environment') {
        node {
            token = getOAuthToken('https://rsi.rackspace.net', 'snow-service-account')
            releaseName = motor.install(token, sandboxChartName, sandboxChartVersion, [
                    url   : motor.STAGING_URL,
                    values: sandboxConfig
            ])
        }
    }

    return releaseName
}

/**
 * Rolls out the Identity sandbox environment and ensures that each pod is on the latest version
 * and ready to accept traffic.
 *
 * @param releaseName - the release to deploy
 * @return
 */
def deploySandboxEnvironment(releaseName) {
    stage('Deploy') {
        node('master') {
            // Wait for the release to "settle".
            openshift.withCluster {
                def dcSelector = openshift.selector('dc', [release: releaseName])

                // Ensure each DeploymentConfig has been triggered.
                dcSelector.withEach {
                    if (!it.object().status.latestVersion) {
                        it.rollout().latest()
                    }
                }

                // Wait for each DeploymentConfig to have at least one replica available.
                timeout(10) {
                    dcSelector.untilEach {
                        def replicas = it.object().status.availableReplicas
                        return replicas && replicas > 0
                    }
                }
            }

        }
    }

}

def destroySandboxEnv(releaseName) {
    stage('Destroy Environment') {
        node('master') {

            try {
                openshift.withCluster {
                    openshift.selector('pods', [release: releaseName]).withEach {
                        def podSelector = it
                        def pod = it.object()
                        if (pod.spec.containers.size() == 1) {
                            tee("pod-logs/${pod.metadata.name}.log") {
                                podSelector.logs()
                            }
                        } else {
                            pod.spec.containers.each {
                                tee("pod-logs/${pod.metadata.name}-${it.name}.log") {
                                    podSelector.logs('-c', it.name)
                                }
                            }
                        }
                    }
                }
                archiveArtifacts allowEmptyArchive: true, artifacts: 'pod-logs/*.log'
            } catch (exc) {
                echo 'Could not get pod logs'
                echo exc.getMessage()
            }

            def token = getOAuthToken('https://rsi.rackspace.net', 'snow-service-account')
            motor.delete(token, releaseName, [url: motor.STAGING_URL])
            openshift.withCluster {
                openshift.selector('all', [release: releaseName]).delete()
            }
        }
    }
}

def runNonCommitTests(scm) {
    stage('Non-Commit Test') {
        node('java') {
            checkout scm
            try {
                tee('non-commit-test.log') {
                    sh './jenkins-scripts/run-non-commit-tests.sh'
                }
            } finally {
                junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'
            }
        }
    }
}

def runJohnnyTests(scm) {
    stage('Functional Test') {
        node('test-functional') {
            checkout scm
            try {
                tee('functional-test.log') {
                    sh './jenkins-scripts/run_johny_tests.sh'
                }
            } finally {
                // Any cleanup that needs to be done after all API testing is complete goes here. This runs on the API test node.
                // Archive the API test results
                junit allowEmptyResults: true, testResults: '**/nosetests*.xml'
                archiveArtifacts allowEmptyArchive: true, artifacts: 'functional-test.log'
            }
        }
    }
}

return this
