#!/usr/bin/env ruby

# Just requiring the file with its relative path didn't work, because then its requires
#  using relative paths failed.  Not sure if there's a cleaner way to do this...
wd = Dir.getwd
Dir.chdir('../translationhelper')
require './s3storage.rb'
Dir.chdir(wd)

storage = S3Storage.new

storage.get_langs.each do |lang|
  if lang.length == 2
    dir = 'res/values-' << lang
  else
    dir = 'res/values-' << lang[0,2] << '-r' << lang[2,2]
  end

  if ! Dir.exists?(dir)
    Dir.mkdir(dir)
  end

  File.open(dir << '/strings.xml', 'w') do |file|
    file.write(storage.get_strings(lang))
  end
end
