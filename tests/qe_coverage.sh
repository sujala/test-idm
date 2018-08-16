#!/bin/bash

mkdir -p ~/.identity/logs
pip install -r tests/api/requirements.txt
cafe-config plugins install http
coverage-opencafe api project::identity nosetests tests/api/.
