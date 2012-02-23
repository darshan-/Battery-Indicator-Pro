#!/usr/bin/env ruby
# encoding: utf-8

project = 'battery-indicator'
user = `cat ~/.netrc | awk '{print $4;}'`.strip
pass = `cat ~/.netrc | awk '{print $6;}'`.strip

version = `grep "android:versionName=" AndroidManifest.xml`.split('"')[1]
apk_location = "BatteryIndicatorPro-#{version}.apk"

system("cp bin/*-release.apk #{apk_location}")
system("./googlecode_upload.py --summary=. --project=#{project} --user=#{user} --password=#{pass} #{apk_location}")
system("rm #{apk_location}")
