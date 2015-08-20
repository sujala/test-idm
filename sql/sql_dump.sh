#!/bin/bash

DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

USER=keystone
PASSWORD=password
HOST=127.0.0.1
PORT=3306

mysqldump -h $HOST -P $PORT -u $USER --password=$PASSWORD keystone --no-data > $DIR/schema/schema.sql
mysqldump -h $HOST -P $PORT -u $USER --password=$PASSWORD keystone --skip-triggers --skip-add-drop-table --skip-add-locks --skip-disable-keys --skip-set-charset --no-create-info --extended-insert=FALSE --hex-blob > $DIR/data/data.sql
