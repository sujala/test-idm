#!/bin/bash

SCRIPT_DIR="$( cd "$( dirname "$0" )" && pwd )"

export PIP_CONFIG_FILE="$SCRIPT_DIR/pip.conf"
mkdir -p ~/.identity/logs
pip install -r $SCRIPT_DIR/api/requirements.txt
cafe-config plugins install http
coverage-opencafe api project::identity pytest $SCRIPT_DIR/api/.
