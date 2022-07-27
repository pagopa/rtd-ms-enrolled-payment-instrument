#!/bin/bash

# start mongo
/usr/bin/mongod --bind_ip_all --replSet rs0 --fork --logpath /var/log/mongodb/mongod.log

# wait start
sleep 5

# enable replica set config
mongo <<EOF
var config = {
    "_id": "rs0",
    "version": 2,
    "members": [
        {
            "_id": 1,
            "host": "localhost:27017",
        },
    ]
};
rs.initiate(config, { force: true });
rs.status();
EOF

# log
tail -f var/log/mongodb/mongod.log
