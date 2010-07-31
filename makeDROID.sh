#!/bin/bash

SRC="src/com/darshancomputing/BatteryIndicator"
DSRC="../DROID/src/com/darshancomputing/BatteryIndicator"

rm -rf ../DROID
mkdir ../DROID

cp *.xml *.properties my-release-key.keystore ../DROID/

mkdir -p ../DROID/res/layout
cp res/layout/*.xml ../DROID/res/layout

mkdir -p ../DROID/res/values
cp res/values/*.xml ../DROID/res/values

mkdir -p ../DROID/res/xml
cp res/xml/*.xml ../DROID/res/xml

DD="../DROID/res/drawable"
mkdir -p $DD
cp res/drawable-hdpi/*.png $DD
pushd $DD >/dev/null
rm ???[1-46-9].png
rm ??[2-9]5.png
popd >/dev/null

mkdir -p $DSRC
cp $SRC/*.java $DSRC
for f in $DSRC/*.java; do sed --in-place 's/BatteryIndicatorPro/BatteryIndicatorProDROID/' $f; done
