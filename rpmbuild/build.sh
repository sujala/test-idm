#!/bin/bash

war=$(find ../ -maxdepth 1 -name *.war -print)
version=$(echo $war | grep -oP '\d+\.\d+\.\d+')
release=$(echo $war | grep -oP '\d{4,}')

make VERSION="$version" RELEASE="$release" rpms publish
