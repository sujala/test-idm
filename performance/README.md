# Performance testing for Cloud Identity

## Why

Cloud Identity is at the center of all applications within the Rackspace platform solution.  We want to validate that we did not introduce any regressions to the code/configuration.  We also want the ability to automate the performance suite as well as provide the team with a way to validate against their own branches.

## What

This directory will contain performance environment load generation tooling, scripts, data configurations, and local environment configurations.

## Local setup

In order for us to validate any performance characteristics of the target identity environment, we first need to run tests locally.  For that, we can utilize the following tooling:

* IntelliJ
* Docker
* Docker-compose

### Steps:

1. Set up all of identity and its dependencies: `PWD=$(pwd) docker-compose up -d`
2. Copy over `identity-saml-generator-*-.jar` from `tests/resources` to `data_generation` directory: `cp ../tests/resources/identity-saml-generator-* data_generation/'
3. Generate fed_origin cert: `pushd data_generation && jar xf identity-saml-generator-* sample_keys/fed-origin.crt && popd`
4. Generate sample data:

   4.1. Generate users in temporary directories (positional values are: ip, loops, normal users per loop, and admin users per loop): `pushd data_generation && ./create_users.sh http://${docker_ip}:8082/idm/cloud 1 10 5 && popd`.  This will create loops * (normal users per loop) users and loops * (admin users per loop) admins.
   4.2. Generate admin user data: `pushd data_generation && ./generate_files.py -u admins -c admin_file_config.json -o ../identity-perf-agent/localhost/data/identity  && popd`
   4.3. Generate regular user data: `pushd data_generation && ./generate_files.py -u users -c file_config.json -o ../identity-perf-agent/localhost/data/identity && popd`
5. Set up `identity-perf-agent/src/test/resources/application.properties` to the values you want to run with.  While there are many values, you can figure out the ones you need from your Simulation.  An example would be in `com.rackspacecloud.simulations.identity.IdentityDemo`:

        val conf = ConfigFactory.load()

        // V20 Authenticate
        // conf maps to application.properties value
        val V20_AUTHENTICATE_APIKEY_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_authenticate_apikey.users_per_sec")
        // set to Identity scenario
        val v20_apikey_auth_scn = Identity.v20_apikey_auth

        // V20 Validate
        // conf maps to application.properties value
        val V20_VALIDATE_USERS_PER_SEC : Double =  conf.getDouble("soa.v20_validate.users_per_sec")
        // set to Identity scenario
        val v20_token_validate_scn = Identity.v20_token_validate

        // set max duration (overall)
        val MAX_DURATION_SECS: Int = conf.getInt("simulation.max_duration_secs")
        // set external url
        val OS_MAIN_EXTERNAL_AUTH_URL = conf.getString("main_external_auth_url")

        // set up http protocol to use in tests
        val httpMainExternalConf = http
            .baseURL(OS_MAIN_EXTERNAL_AUTH_URL)
            .acceptHeader("application/json")
            .acceptEncodingHeader("gzip, deflate, compress")
            .contentTypeHeader("application/json; charset=utf-8")
            .userAgentHeader( """QEPerf/1.0.0""")
            .shareConnections

        // add scenarios to scenario list
        def list_scns(): List[PopulationBuilder] = {
            return List(
                scn_wrapper(v20_apikey_auth_scn, V20_AUTHENTICATE_APIKEY_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf),
                scn_wrapper(v20_token_validate_scn, V20_VALIDATE_USERS_PER_SEC, MAX_DURATION_SECS, httpMainExternalConf)
            )
        }

6. Set `identity-perf-agent/src/test/scala/Engine.scala` `props.simulationClass("yoursimulation")`
7. Run `Engine` class.  The results will appear in `results` directory.
8. (Optional) clean up identity: `pushd data_generation && ./delete_users.sh http://$docker_ip:8082/idm/cloud && popd`.  This will remove all files and back up the data to a .bak file.  If you'd like, you can then manually wipe those files out after validating that all data has been properly removed.