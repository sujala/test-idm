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
                ['ca-directory', 'customer-identity', 'repose', 'active-directory', 'dynamodb'].each {
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
                timeout(20) {
                    parallel branches
                }
            }
        }
    },

    chart: [
            name: 'customer-identity',
            version: '0.1.4',
            values: { branch, name ->
                def dynamoDBConfig = [
                        trigger_tag: branch,
                        name: "dynamodb-$name",
                        imageName: "dynamodb",
                        env: [
                                values: [
                                        [ name: 'http_proxy' ],
                                        [ name: 'HTTP_PROXY' ],
                                        [ name: 'TOMCAT_HTTP_PORT', value: 8083]
                                ]
                        ]
                ]
                def custIdentityConfig = [
                        trigger_tag: branch,
                        name: "customer-identity-$name",
                        imageName: "customer-identity",
                        env: [
                                values: [
                                        [ name: 'http_proxy' ],
                                        [ name: 'HTTP_PROXY' ],
                                        [ name: 'TOMCAT_HTTP_PORT', value: 8083],
                                        [ name: 'CA_DIRECTORY_SERVICE_HOST', value: "\$(CA_DIRECTORY_${name.toUpperCase().replaceAll('-','_')}_SERVICE_HOST)"],
                                        [ name: 'CA_DIRECTORY_SERVICE_PORT', value: "\$(CA_DIRECTORY_${name.toUpperCase().replaceAll('-','_')}_SERVICE_PORT)"],
                                        [ name: 'ACTIVE_DIRECTORY_SERVICE_HOST', value: "\$(ACTIVE_DIRECTORY_${name.toUpperCase().replaceAll('-','_')}_SERVICE_HOST)"],
                                        [ name: 'ACTIVE_DIRECTORY_SERVICE_PORT', value: "\$(ACTIVE_DIRECTORY_${name.toUpperCase().replaceAll('-','_')}_SERVICE_PORT)"],
                                        [ name: 'DYNAMODB_SERVICE_HOST', value: "\$(DYNAMODB_${name.toUpperCase().replaceAll('-','_')}_SERVICE_HOST):\$(DYNAMODB_${name.toUpperCase().replaceAll('-','_')}_SERVICE_PORT)"]
                                ]
                        ]
                ]
                def reposeConfig = [
                        trigger_tag: branch,
                        name: "repose-$name",
                        imageName: "repose",
                        env: [
                                values: [
                                        [ name: 'http_proxy' ],
                                        [ name: 'HTTP_PROXY' ],
                                        [ name: 'CUSTOMER_IDENTITY_SERVICE_HOST', value: "\$(CUSTOMER_IDENTITY_${name.toUpperCase().replaceAll('-','_')}_SERVICE_HOST)"],
                                        [ name: 'CUSTOMER_IDENTITY_SERVICE_PORT', value: "\$(CUSTOMER_IDENTITY_${name.toUpperCase().replaceAll('-','_')}_SERVICE_PORT)"]
                                ]
                        ]
                ]
                def activeDirectoryConfig = [
                        trigger_tag: branch,
                        name: "active-directory-$name",
                        imageName: "active-directory",
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
                        imageName: "ca-directory",
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
                        'ca-directory-deploy': caDirectoryConfig,
                        'dynamodb-deploy': dynamoDBConfig
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
