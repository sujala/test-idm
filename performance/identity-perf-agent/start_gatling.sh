#!/bin/bash -xe
# data generation
admin_username=${1:-keystone_identity_admin}
virtualenv env
. env/bin/activate
pip install -r data_generation/requirements.txt
mkdir -p localhost/data/identity
echo "view application.properties"
cat src/test/resources/application.properties
IDM_ENDPOINT=$(cat src/test/resources/application.properties | grep main_internal_auth_url | awk -F'=' '{print $2}')
echo "viewed application.properties"
mkdir -p lib
cd data_generation
cp ../../../tests/resources/saml-generator-* ../lib/.
mkdir -p sample_keys
jar xf ../lib/saml-generator-* sample_keys/fed-origin.crt
echo "update password in json file"
./add_password_to_request.py
echo "create users against $IDM_ENDPOINT"
./create_users.sh $IDM_ENDPOINT 1 10 5 1 ${admin_username}
echo "create admins"
./generate_files.py -u admins -c admin_file_config.json -o ../localhost/data/identity
./generate_files.py -u users -c file_config.json -o ../localhost/data/identity
./generate_files.py -u default_users -c default_user_file_config.json -o ../localhost/data/identity
USERS_FILE_NAME=$(ls users)
./add_rcn_to_domain.py -i users/${USERS_FILE_NAME} -s ${IDM_ENDPOINT}
./create_users_in_domain.py -p 10 -n 20 -i users -m 1 -s ${IDM_ENDPOINT}
./create_idp_data.py -f ../localhost/data/identity/dom_users_for_fed.dat -s ${IDM_ENDPOINT}
#./generate_files.py -u users_in_dom -c users_in_domain.json -o ../localhost/data/identity -i true
cd ..
#rm -rf target/*
#rm -f data/identity/users_tokens.dat
#rm -f data/identity/admin_users_tokens.dat
./compile_sbt.sh
sbt "gatling:testOnly com.rackspacecloud.simulations.identity.IdentityConstantTputGenerateTokens"
# echo "token,username,apikey,ipaddress" |cat - $DATA_DIR/users_tokens.dat > /tmp/out && mv /tmp/out $DATA_DIR/users_tokens.dat
# echo "admin_token,user_name,apikey,password,ipaddress" |cat - $DATA_DIR/admin_users_tokens.dat > /tmp/out && mv /tmp/out $DATA_DIR/admin_users_tokens.dat

# Run tests
sbt "gatling:testOnly com.rackspacecloud.simulations.identity.IdentityConstantTputUsersRampUp"

# echo "admin_token,user_id,username" |cat - $DATA_DIR/created_users.dat > /tmp/out && mv /tmp/out $DATA_DIR/created_users.dat

# Delete users
# sbt "gatling:testOnly com.rackspacecloud.simulations.identity.IdentityConstantTputUserDeletions"

[ -f gatling.log ] && mv gatling.log target/gatling/.
echo "copy only simulation log to target/gatling/results"
mv results/identityconstanttputusersrampup* target/gatling/results/
#mv gatling_console.log target/gatling/.

cd data_generation
./remove_idps.py -s ${IDM_ENDPOINT}