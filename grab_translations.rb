#!/usr/bin/env ruby

require 'net/http'
require 'uri'

LANGS_URI = 'http://linode/ath/bi/langs/'

langs = Net::HTTP.get(URI.parse(LANGS_URI))

langs.split.each do |lang|
  if lang.length == 2
    dir = 'res/values-' << lang
  else
    dir = 'res/values-' << lang[0,2] << '-r' << lang[2,2]
  end

  if ! Dir.exists?(dir)
    Dir.mkdir(dir)
  end

  fpath = dir + '/strings.xml'

  File.open(fpath, 'w') do |file|
    strings = Net::HTTP.get(URI.parse(LANGS_URI + lang))
    file.write(strings)
    file.sync # Want to be sure `file' command below sees new contents
  end

  puts `file #{fpath}`
end
