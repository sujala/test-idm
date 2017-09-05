#
# cloud-identity/tests/Makefile ---
#
# Commentary:
# * Makefile for building and running tests

.PHONY: help build test test_nose flake8

SHELL := /bin/bash

UNAME := $(shell uname)

VENV=.venv
VENV_ACTIVATE=source $(VENV)/bin/activate
VENV_DEACTIVATE=deactivate

API ?= "api"

help:
	@echo "Targets include the following:"
	@echo "help        - show help information"
	@echo "flake       - virtualenv + run tox flake8"
	@echo "test        - virtualenv + run tox"
	@echo "test_nose   - virtualenv + run nosetests"
	@echo "build       - virtualenv + requirements"
	@echo ""

# clean up ../build/libs
# build virtualenv.
# clean up 
# Remove .tox directory.
# copy log4j.xml.
# kill all docker instances and start a new one
build: clean
	# build johny environment
	# make sure .venv directory exists prior to creating it
	@test -d $(VENV) || virtualenv $(VENV) --no-site-packages
	# install tox
	@./$(VENV)/bin/pip install tox
	@./set_up_johny.sh

flake: build
	# run flake8
	@./$(VENV)/bin/tox -e flake8

test: build
	# run flake8
	@./$(VENV)/bin/tox -e py27

test_nose: build
	# run nosetests stuff and optionally pass in module
	@./$(VENV)/bin/pip install -r api/requirements.txt
	# install opencafe
	@./$(VENV)/bin/pip install opencafe
	# create virtualenv and initialize cafe
	$(VENV_ACTIVATE) && cafe-config init && cafe-config plugins install http && $(VENV_DEACTIVATE)
	# create virtualenv and run nose tests (optionally pass in module)
	$(VENV_ACTIVATE) && nosetests ${API} || true && $(VENV_DEACTIVATE)

clean:
	# remove virtualenv directory
	@rm -rf $(VENV)
	# clean up identity libs
	@rm -rf ../build/libs/*
	# remove tox directory
	@rm -rf .tox
	# clean up docker containers
	docker-compose down
