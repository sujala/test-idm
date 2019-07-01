#!/bin/bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

CID=$(docker ps | grep ca-directory | cut -d ' ' -f1)

cat << EOF | docker exec -i $CID sh -c 'cat > /tmp/dump.sh'
. /opt/CA/Directory/dxserver/install/.dxprofile
dxserver stop all
dxdumpdb -f /tmp/base.ldif a-ldap1-virt
dxserver start all
EOF

docker exec $CID chmod u+x /tmp/dump.sh
docker exec $CID bash -c /tmp/dump.sh
docker exec $CID cat /tmp/base.ldif > $DIR/ldif/temp-base.ldif

sed -e '/^creatorsName/d' \
    -e '/^createTimestamp/d' \
    -e '/^modifyTimestamp/d' \
    -e '/^modifiersName/d' \
    -e '/^dxUpdatedByDisp/d' \
    -e '/^rsLoadedDate/d' \
    -e '/^dxPwdHistory/d' \
    -e '/^dxPwdFailedAttempts/d' \
    -e '/^dxPwdFailedTime/d' $DIR/ldif/temp-base.ldif > $DIR/ldif/base.ldif

rm $DIR/ldif/temp-base.ldif
