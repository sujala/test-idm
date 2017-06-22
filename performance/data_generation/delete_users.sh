#!/bin/bash
url=$1
num_of_processes=30
for i in `pwd`/users/*
do
    if [[ -f $i ]]; then
        #copy stuff ....
        python ./delete_user_data.py -p $num_of_processes -s $url -i $i -o $i.bak
        echo "******************************"
        echo "deleted data from $i."
        echo "******************************"
    fi
done
