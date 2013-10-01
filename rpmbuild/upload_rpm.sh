#!/bin/bash

ROOTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ $# -ne 1 ]
then
    echo "usage: $0 <server>"
    exit
fi

RPM_SERVER=$1

scp rpmbuild/RPMS/noarch/*.rpm root@$RPM_SERVER:/var/www/html/centos/6/idm/x86_64/
ssh root@$RPM_SERVER 'createrepo /var/www/html/centos/6/idm/x86_64/'
