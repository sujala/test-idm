API Tests
=========

The main purpose of API tests is to validate the API contracts.
These are black box tests that can run against any instance of identity
(dev, test, prod, local instance, containerized instance).

Setting up Conf file & log path
================================

1. Set the following environment variables::

    export CAFE_CONFIG_FILE_PATH=~/.identity/api.conf
    export CAFE_ROOT_LOG_PATH=~/.identity/logs
    export CAFE_TEST_LOG_PATH=~/.identity/logs

2. Make a directory ~/.identity
    mkdir ~/.identity

3. Copy the api.conf file to the path set by CAFE_CONFIG_FILE_PATH::

    $ cp tests/etc/api.conf ~/.identity/api.conf

4. Update the config file in ~/.identity/api.conf with the appropriate values

Running the tests - with tox
============================

1. To run the tests against a py27 env::

    $ cd cloud-identity/tests
    $ tox

2. To run the flake8 checks on the test code::

    $ cd cloud-identity/tests
    $ tox -e flake8

Running the tests - without tox
===============================

1. Create a new virtualenv and install the dependencies::

    NOTE: At the time of this writing opencafe is not compatible with python 3.
          So you will need to create virtualenv with python 2.

    $ pip install -r tests/api/requirements.txt
    $ pip install opencafe
    $ cafe-config init
    $ cafe-config plugins install http

6. Once you are ready to run the tests::

    $ nosetests api (OR)
    $ nosetests --nologcapture api (to avoid the verbose requests log)
