#!/bin/bash

# This script creates a specified number of users in the target identity
# instance, and save information about the users into csv files.
#
# usage: create_users.sh <url> <user_loops> <users_per_loop> <admin_per_loop>
#                        <num_of_processes> <admin_username>
#
#   url                 The URL to the relevant Customer Identity instance.
#   user_loops          How many times to loop.
#   users_per_loop      How many regular user to create per loop.
#   admin_per_loop      How many admin users to create per loop.
#   num_of_processes    Number of parallel sub-processes to spawn.
#   admin_username      The username of the account to use to create the new
#                       users. Default value is "keystone_identity_admin" if
#                       none provided.
#
# For each loop in 1..`user_loops`, this script will make not of the current
# time and use that to construct pathnames for the csv files. It will then
# pass those pathnames and other parameters to the create_user_data.py script
# to actually do the work of creating the users.

url=$1
user_loops=$2
users_per_loop=$3
admins_per_loop=$4
num_of_processes=$5
admin_username=${6:-keystone_identity_admin}
mkdir -p users/
mkdir -p admins/
mkdir -p default_users/
mkdir -p users_in_dom/
for i in `seq 1 $user_loops`;
do
    today=`date '+%Y_%m_%d__%H_%M_%S'`;
    filename="users/${today}_data.csv"
    adminfilename="admins/${today}_data.csv"
    defaultuserfilename="default_users/${today}_data.csv"
    python ./create_user_data.py -p $num_of_processes -u $users_per_loop -o $filename -s $url -f $adminfilename -a $admins_per_loop -d $defaultuserfilename -i ${admin_username} --debug
    echo "******************************"
    echo "finished $i of $1 files."
    echo "******************************"
done
