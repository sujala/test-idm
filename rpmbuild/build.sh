#!/bin/bash

ROOTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

WAR=$(find $ROOTDIR/../build | grep \\.war$)

BUILD_VERSION=$(echo $WAR | sed -n "s/.*-\([0-9]\+\.[0-9]\+\.[0-9]\+\)-.*/\1/p")
BUILD_RELEASE=$(echo $WAR | sed -n "s/.*-\([0-9]\+\).war/\1/p")

rm -r $ROOTDIR/rpmbuild

mkdir -p $ROOTDIR/rpmbuild/{BUILD,RPMS,S{OURCE,PEC,RPM}S}

mkdir -p $ROOTDIR/cloud-identity-$BUILD_VERSION

cp $WAR cloud-identity-$BUILD_VERSION/
cp $ROOTDIR/../build/config/VAGRANT/*  cloud-identity-$BUILD_VERSION/

tar zcvf $ROOTDIR/rpmbuild/SOURCES/cloud-identity-$BUILD_VERSION.tar.gz cloud-identity-$BUILD_VERSION/

rm -r $ROOTDIR/cloud-identity-$BUILD_VERSION

cp $ROOTDIR/cloud-identity.spec $ROOTDIR/rpmbuild/SPECS/

sed -i -e "s/BUILD_VERSION/$BUILD_VERSION/g" -e "s/BUILD_RELEASE/$BUILD_RELEASE/g" $ROOTDIR/rpmbuild/SPECS/cloud-identity.spec

rpmbuild --define "_topdir $ROOTDIR/rpmbuild" -ba $ROOTDIR/rpmbuild/SPECS/cloud-identity.spec
