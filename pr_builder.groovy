return [
    version: 'v0.8.1',

    unitTest: { scm ->
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
                    source /tmp/secrets/secrets.sh
                    echo artifactory_user=$ARTIFACTORY_USER >> gradle.properties
                    echo artifactory_password=$ARTIFACTORY_PASSWORD >> gradle.properties
                    set -x
                    ./gradlew clean build -x test -x nonCommitTest -x commitTest -x commonTest artifactoryPublish --stacktrace
                    '''
                    env.IDM_Version = sh (
                            script: './gradlew appRevision | head -2 | tail -1',
                            returnStdout: true
                    ).trim()
                    println "idmVersion = ${env.IDM_VERSION}"
                }
            }
        )
        node('master') {
            openshift.withCluster {
                branches = [:]
                ['ca-directory', 'customer-identity', 'repose', 'active-directory'].each {
                    def name = it
                    branches[name] = {
                        def build = openshift.selector('bc', name).startBuild(
                                "--commit=master", "--env=identity_version=${env.IDM_VERSION}", "--env=artifactory_host=https://artifacts.rackspace.net")
                        waitUntil {
                            return build.object().status.phase == 'Complete'
                        }
                        def digest = build.object().status.output.to.imageDigest
                        openshift.tag("${name}@${digest}", "${name}:${env.ghprbSourceBranch}")
                    }
                }
                timeout(15) {
                    parallel branches
                }
            }
        }
    },

    chart: [
            name: 'customer-identity-pr',
            version: '0.1.0',
            values: { branch, name ->
                def deployConfig = [
                        trigger_tag: branch,
                        env: [
                                values: [
                                        [ name: 'http_proxy' ],
                                        [ name: 'HTTP_PROXY' ],
                                        [ name: 'TOMCAT_HTTP_PORT', value: 8280],
                                        [ name: 'DYNAMODB_SERVICE_HOST', value: 'localhost:8000']
                                ]
                        ]
                ]

                return [
                    'name': "customer-identity-pr-$name",
                    'repose': deployConfig,
                    'customer-identity': deployConfig,
                    'active-directory': deployConfig,
                    'ca-directory': deployConfig,
                    'dynamodb': deployConfig
                ]
            }
    ],

    functionalTest: { scm, name ->
        node('test-functional') {
            checkout scm
            try {
                tee('functional-test.log') {
                    sh './run_johny_tests.sh'
                }
            } finally {
                junit allowEmptyResults: true, testResults: '**/nosetests*.xml'
                archiveArtifacts allowEmptyArchive: true, artifacts: 'functional-test.log'
            }
        }
    }
]
