#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))
cd $BASEDIR
mvn cobertura:cobertura cobertura:check -Dskip.default.surefire=false -Dcobertura.skip.check=false
