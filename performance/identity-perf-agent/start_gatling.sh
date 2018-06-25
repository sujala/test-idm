#!/bin/bash -xe
# data generation
virtualenv env
. env/bin/activate
pip install -r data_generation/requirements.txt
mkdir -p localhost/data/identity
echo "view application.properties"
cat src/test/resources/application.properties
IDM_ENDPOINT=$(cat src/test/resources/application.properties | grep main_internal_auth_url | awk -F'=' '{print $2}')
echo "viewed application.properties"
cd data_generation
echo "create users against $IDM_ENDPOINT"
./create_users.sh $IDM_ENDPOINT 1 10 5 1
echo "create admins"
./generate_files.py -u admins -c admin_file_config.json -o ../localhost/data/identity
./generate_files.py -u users -c file_config.json -o ../localhost/data/identity
./generate_files.py -u default_users -c default_user_file_config.json -o ../localhost/data/identity
#./create_users_in_domain.py -p 10 -n 20 -i users -m 1
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
sbt "gatling:testOnly com.rackspacecloud.simulations.identity.IdentityConstantTputUserDeletions"

mv gatling.log target/gatling/.
echo "copy only simulation log to target/gatling/results"
mv results/identityconstanttputusersrampup* target/gatling/results/
#mv gatling_console.log target/gatling/.
