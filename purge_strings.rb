#!/usr/bin/env ruby

require 'nokogiri'

string_to_remove = ['pref_cat_keyguard',
                    'pref_cat_keyguard_help',
                    'keyguard_settings',
                    'keyguard_settings_summary',
                    'keyguard_settings_help',
                    'pref_notify_when_kg_disabled',
                    'pref_notify_when_kg_disabled_summary',
                    'pref_notify_when_kg_disabled_help',
                    'pref_confirm_manual_disable',
                    'pref_confirm_manual_disable_summary',
                    'pref_confirm_manual_disable_help',
                    'pref_finish_after_toggle_lock',
                    'pref_finish_after_toggle_lock_summary',
                    'pref_finish_after_toggle_lock_help',
                    'pref_auto_disable',
                    'pref_auto_disable_summary',
                    'pref_auto_disable_help',
                    'pref_disallow_disable_lock_screen',
                    'pref_disallow_disable_lock_screen_summary',
                    'pref_disallow_disable_lock_screen_help',
                    'disable_lock_screen',
                    'reenable_lock_screen',
                    'confirm_disable'
                   ]
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
