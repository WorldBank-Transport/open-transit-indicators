#!/bin/bash

current_feeds=`curl -u $APP_SU_USERNAME:$APP_SU_PASSWORD http://localhost/api/gtfs-feeds/`

rm /tmp/gtfs_provision.zip 2> /dev/null || true
while read line
do IFS=","
    set $line
    if grep -q -v \"city_name\":\ \"$1\" <<< $current_feeds
    then
        echo "Loading city $1..."
        wget -O /tmp/gtfs_provision.zip $2
        curl -X POST -F "city_name=$1" -F "source_file=@/tmp/gtfs_provision.zip" \
           -u $APP_SU_USERNAME:$APP_SU_PASSWORD \
           http://localhost/api/gtfs-feeds/
        echo # print newline
        rm /tmp/gtfs_provision.zip
    else
        echo "City $1 is already loaded"
    fi
done < city_data.txt
