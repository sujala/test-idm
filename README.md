Cloud-Identity
--------------

The Rackspace Cloud Customer and Racker Identity System.

More info: [Cloud Identity One Rack Home](https://one.rackspace.com/display/auth/Home)

Team
-------------
* Werner Mendizabal
* Jorge Munoz
* Timothy Cline
* Robert Jacoby
* Aseem Jain

Documentation
--------------

[API Developer Guide, v2.0](https://developer.rackspace.com/docs/cloud-identity/v2)

[API Admin Developer Guide, v2.0 ](https://pages.github.rackspace.com/ServiceAPIContracts/global-auth-keystone-extensions)

[API Developer Guide, v1.1 (Deprecated, in PDF format)](https://6266fae112c61ca2a24b-0b7d389aeec8162360b1800f389138d1.ssl.cf1.rackcdn.com/auth-client-devguide-internal-deprecated.pdf)

[API Admin Developer Guide, v1.1 (i)(Deprecated, in PDF format)](https://6266fae112c61ca2a24b-0b7d389aeec8162360b1800f389138d1.ssl.cf1.rackcdn.com/auth-1.1-admin-devguide-internal-deprecated.pdf)

[Getting Started](https://one.rackspace.com/display/auth/Getting+Started)

[Development Quick-Start](https://one.rackspace.com/display/auth/Development+Quick-Start)

[Updating Identity's Documentation](https://one.rackspace.com/display/auth/Updating+Identity%27s+Documentation)

Dev Environment Setup
------------

[Setting up Global Auth on your laptop](https://one.rackspace.com/display/auth/Setting+up+Global+Auth+on+your+Laptop)

Johny (integration) Tests
-------------------------

Pre-requisites: docker, python2.7, pip

CD into `tests/` directory

Get all operations: `make help`

Run flake8: `make flake8`

Run tests via tox: `make test`

Run specific modules: `make API=api/v2/federation test_pytest`

Run multiple times without cleaning up after every iteration: `make build && make API=api/v2/federation test_pytest_no_build && ... && make clean`

Contributing
------------

1. Fork it.
2. Create a branch (`git checkout -b mybranch`)
3. Commit your changes (`git commit -am "Added Stuffs"`)
4. Push to the branch (`git push origin mybranch`)
5. Open a Pull Request
6. Enjoy a refreshing Diet Coke and wait to get PR comments / approvals.
7. PR can be merged once it gets 1 QE's approval and 2 DEV's approval.
8. PR with single commit can be merged with "Rebase and merge" option, however PR with multiple commits can be merged with "Squash and Merge" option.

Continuous Integration
----------------------

We are moving our PR pipeline to RSI. The workflow is the following:

1. Follow steps 1-5 in #Contributing section
2. Jenkins pull request pipeline will pick up the flow and run commit tests and publish task in parallel. Publish task will publish jars and config zip files to artifactory and save the published version to `IDENTITY_VERSION` environment variable.
3. If tests pass, Jenkins will build configurations listed in https://github.rackspace.com/cloud-identity-dev/cloud-identity/blob/master/pr_builder.groovy with the identity version specified in `IDENTITY_VERSION` environment variable. Currently, they are: `repose`, `ca-directory`, `customer-identity`, `dynamodb`, and `active-directory`. These images will be published with the branch name to https://rsi.rackspace.net/console/project/identity-test/browse/images
4. If images are successfully built and pushed, Jenkins will create a new release using `customer-identity` chart. The chart will be discussed in depth in a different section.
5. If deployment is successful, functional test is kicked off. It runs all johny tests with setup found in https://github.rackspace.com/cloud-identity-dev/cloud-identity/blob/master/run_johny_tests.sh.
6. Once pipeline ends (pass or fail), the release is cleaned up. An example could be found here: https://jenkins-identity-test.devapps.rsi.rackspace.net/job/PR-Builder-pull-request-pipeline/9/. You can find pod logs and functional test logs attached to each job build as build artifacts.

Customer Identity CICD Chart
--------------------------

The values used in the helm install commands can be found in the following passwordsafe projects.
- https://passwordsafe.corp.rackspace.com/projects/19983
    - Access to this passwordsafe project is managed by the Customer Identity team.
- https://passwordsafe.corp.rackspace.com/projects/19811
    - Access to this passwordsafe project is managed by the QE Security team. You can request access to this project in the #codescan channel on Slack.

Customer Identity CICD chart runs Jenkins PR flow. It is located [here](https://github.rackspace.com/tesla/charts/tree/master/app/customer-identity-cicd). To install the PR flow in the new namespace, run:

```bash
# install a chart
helm install  app/customer-identity-cicd \
    --set github-secret.username=cid-rsi-dev-svc,\
    github-secret.token=<CID-RSI-DEV-SVC-GITHUB-PERSONAL-ACCESS-TOKEN>,\
    jenkins-base.snowUsername=cid-rsi-dev-svc,\
    jenkins-base.snowPassword=<CID-RSI-DEV-SVC-PASSWORD>,\
    passwordsafe.password=<CID-RSI-DEV-SVC-PASSWORD>,\
    docker-secret.password=<CID-RSI-DEV-SVC-PASSWORD>,\
    jenkins-base.checkmarxUsername=ci-compliance,\
    jenkins-base.checkmarxPassword=<CI-COMPLIANCE-PASSWORD>
```

Next, in this repo, add the [webhook](https://github.rackspace.com/cloud-identity-dev/cloud-identity/settings/hooks). In payload URL, add https://jenkins-identity-test.devapps.rsi.rackspace.net/ghprbhook/, select `issue comments` and `pull requests` individual events.

If you need to update the current namespace, run

```bash
helm upgrade <CHART_NAME>  app/customer-identity-cicd \
    --set github-secret.username=cid-rsi-dev-svc,\
    github-secret.token=<CID-RSI-DEV-SVC-PAT-TOKEN>,\
    jenkins-base.snowUsername=cid-rsi-dev-svc,\
    jenkins-base.snowPassword=<CID-RSI-DEV-SVC-PASSWORD>,\
    passwordsafe.password=<CID-RSI-DEV-SVC-PASSWORD>,\
    docker-secret.password=<CID-RSI-DEV-SVC-PASSWORD>,\
    jenkins-base.checkmarxUsername=ci-compliance,\
    jenkins-base.checkmarxPassword=<CI-COMPLIANCE-PASSWORD>
```

Customer Identity PR Chart
--------------------------

Customer Identity PR chart creates a sandbox identity environment. It is located [here](https://github.rackspace.com/tesla/charts/tree/master/app/customer-identity). It is installed as part of the pr flow found here: https://github.rackspace.com/cloud-identity-dev/cloud-identity/blob/master/pr_builder.groovy. It deploys `repose`, `customer-identity`, `ca-directory`, `dynamodb`, and `active-directory` container in their own pods. It exposes `repose` port as a service. The name of the service will have the following template: `repose-pr-<PR_ID>`.
