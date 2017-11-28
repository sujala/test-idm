#!/bin/bash
url=$1
user_loops=$2
users_per_loop=$3
admins_per_loop=$4
num_of_processes=30
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
    python ./create_user_data.py -p $num_of_processes -u $users_per_loop -o $filename -s $url -f $adminfilename -a $admins_per_loop -d $defaultuserfilename
    echo "******************************"
    echo "finished $i of $1 files."
    echo "******************************"
done
