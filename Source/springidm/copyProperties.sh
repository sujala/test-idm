if [ $# -ne 2 ]; then
    echo "usage: copyProperties.sh <host> <environment>"
else
    export HOST=$1
    export ENV=$2
    scp ./src/main/config/$ENV/idm.properties rack@$HOST:/etc/idm/
    scp ./src/main/config/$ENV/base.idm.properties rack@$HOST:/etc/idm/
    scp ./src/main/config/$ENV/idm.secrets.properties rack@$HOST:/etc/idm/
fi