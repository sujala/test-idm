#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))
cd $BASEDIR
cd ..
mvn cobertura:cobertura cobertura:check -Dskip.default.surefire=false -Dcobertura.skip.check=false -Didm.properties.location=target/config/JENKINS
