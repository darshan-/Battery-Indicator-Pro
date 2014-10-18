#!/bin/sh

# After initial checkout from svn, you'll want to run this as your first step.

android update project --path . --name "BatteryBot" --target "android-21"
