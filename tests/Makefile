#
# cloud-identity/tests/Makefile ---
#
# Commentary:
# * Makefile for building and running tests

.PHONY: help build test test_pytest flake8 test_pytest_no_build flake_no_build

SHELL := /bin/bash

UNAME := $(shell uname)
PWD := $(shell pwd)

VENV=/tmp/.venv
VENV_ACTIVATE=source $(VENV)/bin/activate
VENV_DEACTIVATE=deactivate
CAFE_CONFIG_FILE_PATH=/tmp/.identity/api.conf
CAFE_ROOT_LOG_PATH=/tmp/.identity/logs
CAFE_TEST_LOG_PATH=/tmp/.identity/logs
CAFE_CONFIG_ALL=CAFE_ENGINE_CONFIG_FILE_PATH=$(CAFE_CONFIG_FILE_PATH) CAFE_CONFIG_FILE_PATH=$(CAFE_CONFIG_FILE_PATH) CAFE_ROOT_LOG_PATH=$(CAFE_ROOT_LOG_PATH) CAFE_TEST_LOG_PATH=$(CAFE_TEST_LOG_PATH)

API ?= "api"
TOX_PY_ENV ?= py35

PIP_CONFIG_FILE = $(PWD)/pip.conf
PIP = PIP_CONFIG_FILE=$(PIP_CONFIG_FILE) $(VENV)/bin/pip
TOX = PIP_CONFIG_FILE=$(PIP_CONFIG_FILE) $(VENV)/bin/tox

all: test flake_no_build

help:
	@echo "Targets include the following:"
	@echo "help                 - show help information"
	@echo "flake                - build + run tox flake8"
	@echo "test                 - build + run tox"
	@echo "test_pytest          - build + run pytest"
	@echo "build                - build"
	@echo "flake_no_build       - run tox flake8"
	@echo "test_no_build        - run tox"
	@echo "test_pytest_no_build - run pytest"
	@echo "testenv              - connect to testenv"
	@echo "testenv_flake        - testenv run tox flake8"
	@echo "testenv_test         - testenv run tox"
	@echo "testenv_test_pytest  - testenv run pytest"
	@echo ""

# clean up ../build/libs
# build virtualenv.
# clean up
# Remove .tox directory.
# copy log4j.xml.
# kill all docker instances and start a new one
build: clean
	# build johny environment
	./set_up_johny.sh

flake: build flake_no_build clean

flake_no_build:
	# make sure .venv directory exists prior to creating it
	@test -d $(VENV) || virtualenv $(VENV) --no-site-packages
	# install tox
	$(PIP) install tox
	# run flake8
	$(TOX) -e flake8

test_no_build:
	# make sure .venv directory exists prior to creating it
	@test -d $(VENV) || virtualenv $(VENV) --no-site-packages
	# install tox
	$(PIP) install tox
	# run $(TOX_PY_ENV)
	$(CAFE_CONFIG_ALL) $(TOX) -e $(TOX_PY_ENV)

test: build test_no_build

test_pytest: build test_pytest_no_build clean

test_pytest_no_build:
	# make sure docker is running
	$(eval RUNNING_CONTAINERS = $(shell docker-compose ps | wc -l))
	@if [ "$(RUNNING_CONTAINERS)" = "2" ]; then echo "No containers are running. Run make build.";  exit 1; fi
	# remove virtualenv directory because cloudcafe doesn't like it
	@rm -rf $(VENV)
	@test -d $(VENV) || virtualenv $(VENV) --no-site-packages
	# run pytest and optionally pass in module
	$(PIP) install -r api/requirements.txt
	# create virtualenv and initialize cafe
	$(VENV_ACTIVATE) && cafe-config init && cafe-config plugins install http && $(VENV_DEACTIVATE)
	# create virtualenv and run pytest (optionally pass in module)
	$(VENV_ACTIVATE) && $(CAFE_CONFIG_ALL) pytest $(API)

testenv:
	@docker-compose exec testenv bash

testenv_flake:
	@docker-compose exec testenv make flake_no_build

testenv_test:
	@docker-compose exec testenv make test_no_build

testenv_test_pytest:
	@docker-compose exec testenv make test_pytest_no_build

clean:
	# remove virtualenv directory
	@rm -rf $(VENV)
	# clean up identity libs
	@rm -rf ../build/libs/*
	# remove tox directory
	@rm -rf .tox
	# remove identity directory
	@rm -rf /tmp/.identity
	# clean up docker containers
	docker-compose down --remove-orphans
