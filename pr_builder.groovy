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
            name: 'customer-identity',
            version: '0.1.3',
            values: { branch, name ->
                def custIdentityConfig = [
                        trigger_tag: branch,
                        name: "customer-identity-$name",
                        env: [
                                values: [
                                        [ name: 'http_proxy' ],
                                        [ name: 'HTTP_PROXY' ],
                                        [ name: 'TOMCAT_HTTP_PORT', value: 8083]
                                ]
                        ]
                ]
                def reposeConfig = [
                        trigger_tag: branch,
                        name: "repose-$name",
                        env: [
                                values: [
                                        [ name: 'http_proxy' ],
                                        [ name: 'HTTP_PROXY' ]
                                ]
                        ]
                ]
                def activeDirectoryConfig = [
                        trigger_tag: branch,
                        name: "active-directory-$name",
                        env: [
                                values: [
                                        [ name: 'http_proxy' ],
                                        [ name: 'HTTP_PROXY' ],
                                        [ name: 'LDAPS_PORT', value: 636]
                                ]
                        ]
                ]
                def caDirectoryConfig = [
                        trigger_tag: branch,
                        name: "ca-directory-$name",
                        env: [
                                values: [
                                        [ name: 'http_proxy' ],
                                        [ name: 'HTTP_PROXY' ],
                                        [ name: 'LDAPS_PORT', value: 636]
                                ]
                        ]
                ]

                return [
                        'customer-identity-deploy': custIdentityConfig,
                        'repose-deploy': reposeConfig,
                        'active-directory-deploy': activeDirectoryConfig,
                        'ca-directory-deploy': caDirectoryConfig
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
