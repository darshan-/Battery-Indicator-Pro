#!/bin/sh

scp -q res/values/strings.xml linode:ath/incoming/en.xml

wget --quiet -O /dev/null --ignore-length --post-data="" http://linode/ath/bi/load_new_en

if [ "$?" -ne "0" ]; then
    echo "Sorry, something didn't work!"
    exit 1
fi
