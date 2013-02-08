#!/usr/bin/env ruby

require 'nokogiri'

string_to_remove = ['menu_logs']
string_files = []

Dir.glob('res/values*').each do |d|
  file = d << "/strings.xml"
  string_files << file if File.exists?(file)
end
string_files.sort!

string_files.each do |file|
  puts "Editing " << file

  strings_xml = ""
  File.open(file) {|f| strings_xml = f.read()}

  doc = Nokogiri::XML(strings_xml)

  doc.xpath('//string').each do |str_el|
    if string_to_remove.include?(str_el.attr('name')) then
      puts " - Deleting " << str_el.attr('name')
      str_el.remove()
    end
  end

  s = doc.to_xml(:encoding => 'utf-8')
  s = s.gsub(/(\n\s+\n)+/, "\n")

  File.open(file, 'w') {|f| f.write(s)}
end
