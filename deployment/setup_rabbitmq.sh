#!/bin/bash
# Parameters (this is designed to be called from another script)
USER_NAME=$1
VHOST_NAME=$2

# remove default user and add our own
rabbitmqctl delete_user guest
rabbitmqctl add_user $USER_NAME $USER_NAME
rabbitmqctl add_vhost $VHOST_NAME
rabbitmqctl set_permissions -p $VHOST_NAME $USER_NAME ".*" ".*" ".*"
