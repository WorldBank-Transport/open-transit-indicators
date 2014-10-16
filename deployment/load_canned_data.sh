#!/bin/bash

while read line
do
    IFS=","; set $line; IFS=" "
        echo "Loading city $1..."
        curl -X POST -F "city_name=$1" \
            -F "source_file=@/projects/open-transit-indicators/preload-data/$2" \
            -u $APP_SU_USERNAME:$APP_SU_PASSWORD \
            http://localhost/api/indicators/ 2> /dev/null
        echo  # newline
done < /projects/open-transit-indicators/preload-data/info.txt
