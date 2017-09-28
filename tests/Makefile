#
# cloud-identity/tests/Makefile ---
#
# Commentary:
# * Makefile for building and running tests

.PHONY: help build test test_nose flake8 test_nose_no_build

SHELL := /bin/bash

UNAME := $(shell uname)

VENV=/tmp/.venv
VENV_ACTIVATE=source $(VENV)/bin/activate
VENV_DEACTIVATE=deactivate
CAFE_CONFIG_FILE_PATH=/tmp/.identity/api.conf
CAFE_ROOT_LOG_PATH=/tmp/.identity/logs
CAFE_TEST_LOG_PATH=/tmp/.identity/logs
CAFE_CONFIG_ALL=CAFE_CONFIG_FILE_PATH=$(CAFE_CONFIG_FILE_PATH) CAFE_ROOT_LOG_PATH=$(CAFE_ROOT_LOG_PATH) CAFE_TEST_LOG_PATH=$(CAFE_TEST_LOG_PATH)

API ?= "api"

all: test flake_no_build

help:
	@echo "Targets include the following:"
	@echo "help                 - show help information"
	@echo "flake                - build + run tox flake8"
	@echo "test                 - build + run tox"
	@echo "test_nose            - build + run nosetests"
	@echo "build                - build"
	@echo "test_nose_no_build   - run nosetests"
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

flake: build flake_no_build

flake_no_build:
	# make sure .venv directory exists prior to creating it
	@test -d $(VENV) || virtualenv $(VENV) --no-site-packages
	# install tox
	$(VENV)/bin/pip install tox
	# run flake8
	$(VENV)/bin/tox -e flake8 || $(MAKE) clean

test: build
	# make sure .venv directory exists prior to creating it
	@test -d $(VENV) || virtualenv $(VENV) --no-site-packages
	# install tox
	$(VENV)/bin/pip install tox
	# run py27
	$(CAFE_CONFIG_ALL) $(VENV)/bin/tox -e py27 || $(MAKE) clean

test_nose: build
	# run test nose
	$(MAKE) API=${API} test_nose_no_build || $(MAKE) clean

test_nose_no_build:
	# make sure docker is running
	$(eval RUNNING_CONTAINERS = $(shell docker-compose ps | wc -l))
	@if [ "$(RUNNING_CONTAINERS)" = "2" ]; then echo "No containers are running. Run make build.";  exit 1; fi
	# remove virtualenv directory because cloudcafe doesn't like it
	@rm -rf $(VENV)
	@test -d $(VENV) || virtualenv $(VENV) --no-site-packages
	# run nosetests stuff and optionally pass in module
	$(VENV)/bin/pip install -r api/requirements.txt
	# install opencafe
	$(VENV)/bin/pip install opencafe
	# create virtualenv and initialize cafe
	$(VENV_ACTIVATE) && cafe-config init && cafe-config plugins install http && $(VENV_DEACTIVATE)
	# create virtualenv and run nose tests (optionally pass in module)
	$(VENV_ACTIVATE) && $(CAFE_CONFIG_ALL) nosetests ${API} || true && $(VENV_DEACTIVATE)

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
	docker-compose down
