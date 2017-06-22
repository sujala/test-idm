#!/bin/sh

coverage run --source=app,controllers ./run_tests.py

coverage report -m
