#!/bin/sh

# After initial checkout from svn, you'll want to run this as your first step.

# API level 5 required for android:required attribute of uses-feature element;
#  that's not available any more, so using 7
android update project --path . --target "android-11"
