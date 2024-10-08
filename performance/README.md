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

2. Run `cd identity-perf-agent` and then copy over `saml-generator-*-.jar` from `tests/resources` to `data_generation` directory: `cp ../../tests/resources/saml-generator-* data_generation/'

3. Generate fed_origin cert: `pushd data_generation && jar xf saml-generator-* sample_keys/fed-origin.crt && popd`

4. Generate sample data:

   4.0. Set `docker_ip` variable to IP that your docker is running under.  Usually localhost on OSs that run native docker and 192.168.x.x on those that run in docker-machine.

   4.1. There are python scripts.  You should run under latest Python 3.x version (use pyenv to set the version and virtualenv to not install packages in your system)

   4.2. Run `pip install -r data_generation/requirements.txt`

   4.3. Run `mkdir -p localhost/data/identity`. Please wipe out any any old csv files in admins, users or default_users directories.

   4.4. Generate users in temporary directories (positional values are: ip, loops, normal users per loop, and admin users per loop): `pushd data_generation && ./create_users.sh http://${docker_ip}:8082/idm/cloud 1 10 5 1 && popd`.  This will create loops * (normal users per loop) users and loops * (admin users per loop) admins.

   4.5. Generate admin user data: `pushd data_generation && ./generate_files.py -u admins -c admin_file_config.json -o ../localhost/data/identity  && popd`

   4.6. Generate regular user data: `pushd data_generation && ./generate_files.py -u users -c file_config.json -o ../localhost/data/identity && popd`

   4.7. Generate default user data: `pushd data_generation && ./generate_files.py -u default_users -c default_user_file_config.json -o ../localhost/data/identity && popd`

   The next two sub-steps need to be run only if you want to test `list users in domain` call for memory leak testing.

   4.8. If you want to test `users in a domain` call, you need to create default users in the domain: `pushd data_generation && ./create_users_in_domain.py -p 10 -n 20 -i users -m 1 && popd`. This will also let you specify more than 1 domain, so that you can create users in multiple domains & then the api call will be made for all those domains.

   4.9. Generate users in domain data: `pushd data_generation && ./generate_files.py -u users_in_dom -c users_in_domain.json -o ../localhost/data/identity -i true && popd`

   4.10. If you need to do federation tests. You need to run this before running Engine.scala : `pushd data_generation && python create_idp_data.py -s http://localhost:8082/idm/cloud -f ../localhost/data/identity/dom_users_for_fed.dat && popd`.
         If one wants to update attribute mapping policy for the idps for any testing, s/he can pass another optional argument `-l <path to mapping policy file>` to `create_idp_data.py`

   4.11 If you want to run delegation tests, you need to run 'python data_generation/add_rcn_to_domain.py -i data_generation/users/<file_name>'. This will add RCN attribute to each domain in that file.

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

6. *NOTE* in real execution environment, we wrap this but locally you have to execute this:

   * Update `identity-perf-agent/src/test/scala/Engine.scala` to include `props.simulationClass("com.rackspacecloud.simulations.identity.IdentityConstantTputGenerateTokens")`

   * Run `Engine` class.

   * This will generate tokens for our test

7. Set `identity-perf-agent/src/test/scala/Engine.scala` `props.simulationClass("yoursimulation")`

8. Run `Engine` class.  The results will appear in `results` directory.

9. (Optional) clean up identity: `pushd data_generation && ./delete_users.sh http://$docker_ip:8082/idm/cloud && popd`.  This will remove all files and back up the data to a .bak file.  If you'd like, you can then manually wipe those files out after validating that all data has been properly removed.

## Running in Ollie

Running the perf tests in Ollie is about the same process as you would use for Ollie in general, with a few caveats due to the particular layout of this repo.
To run the perf tests in Ollie, you'll need an available Ollie instance. Setting up such an instance and interacting with it are beyond the scope of this README, but there is one that usually available at `https://scheduler-identity-perf.devapps.rsi.rackspace.net/`

To run the tests, do the following:
1. Create a Test (See [`example-ollie-test.json`](./example-ollie-test.json))
   1. Set the `context_dir` to `performance/identity-perf-agent`.
   2. Set the `run_test_command` to `sh start_gatling.sh <admin user name>`.
   3. If you want to specify a mapping policy file, add it to the end of the `run_test_command`, like so: `sh start_gatling.sh <admin user name> <path to mapping policy>`. If the path is not absolute (which, in general, it shouldn't be), it will be interpreted as relative to the `performance/identity-perf-agent/data_generation` folder.
   4. Set everything else in the usual fashion.
2. Create an Execution with a `test_id` of the test you just created. (See [`example-ollie-execution.json`](./example-ollie-execution.json)).
