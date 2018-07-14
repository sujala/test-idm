#!/bin/bash

./gradlew clean build commitCodeCoverage -x nonCommitTest --profile
