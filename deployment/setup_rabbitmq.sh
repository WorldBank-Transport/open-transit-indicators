#!/bin/bash

set -o errexit

echo "Preparing to delete/update users and virtual hosts"

# Parameters (this is designed to be called from another script)
USER_NAME=$1
VHOST_NAME=$2

# Get current vhosts and users for comparison
RABBIT_MQ_USERS=$(rabbitmqctl list_users)
RABBIT_MQ_VHOSTS=$(rabbitmqctl list_vhosts)

# remove default user if in current users
if [[ $RABBIT_MQ_USERS == *guest* ]]
then
    rabbitmqctl delete_user guest
else
    echo "guest user already deleted from rabbitmq. Skipping..."
fi

# Add vhost if not currently added
if [[ $RABBIT_MQ_VHOSTS != *$VHOST_NAME* ]]
then
    rabbitmqctl add_vhost $VHOST_NAME
else
    echo "$VHOST_NAME already added to rabbitmq virtual hosts. Skipping..."
fi

# Add user if not currently added
if [[ $RABBIT_MQ_USERS != *$USER_NAME* ]]
then
    rabbitmqctl add_user $USER_NAME $USER_NAME
    rabbitmqctl set_permissions -p $VHOST_NAME $USER_NAME ".*" ".*" ".*"
else
    echo "$USER_NAME already added to rabbitmq. Skipping..."
fi
