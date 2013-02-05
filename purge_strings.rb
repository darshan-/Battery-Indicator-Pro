#!/usr/bin/env ruby

require 'nokogiri'

string_to_remove = ['pref_ac_charge_time',
                    'pref_ac_charge_time_help',
                    'pref_ac_charge_time_summary',
                    'pref_cat_notification_time',
                    'pref_cat_status_duration',
                    'pref_cat_time_remaining_estimates',
                    'pref_cat_time_remaining_estimates_help',
                    'pref_constant_usage_time',
                    'pref_constant_usage_time_help',
                    'pref_constant_usage_time_summary',
                    'pref_heavy_usage_time',
                    'pref_heavy_usage_time_help',
                    'pref_heavy_usage_time_summary',
                    'pref_light_usage_time',
                    'pref_light_usage_time_help',
                    'pref_light_usage_time_summary',
                    'pref_normal_usage_time',
                    'pref_normal_usage_time_help',
                    'pref_normal_usage_time_summary',
                    'pref_show_charge_time',
                    'pref_show_charge_time_help',
                    'pref_show_charge_time_summary',
                    'pref_show_constant_usage',
                    'pref_show_constant_usage_help',
                    'pref_show_constant_usage_summary',
                    'pref_show_heavy_usage',
                    'pref_show_heavy_usage_help',
                    'pref_show_heavy_usage_summary',
                    'pref_show_light_usage',
                    'pref_show_light_usage_help',
                    'pref_show_light_usage_summary',
                    'pref_show_normal_usage',
                    'pref_show_normal_usage_help',
                    'pref_show_normal_usage_summary',
                    'pref_show_notification_time',
                    'pref_show_notification_time_help',
                    'pref_show_notification_time_summary',
                    'pref_status_dur_est',
                    'pref_status_dur_est_help',
                    'pref_status_dur_est_summary',
                    'pref_usb_charge_time',
                    'pref_usb_charge_time_help',
                    'pref_usb_charge_time_summary',
                    'time_settings',
                    'time_settings_help',
                    'time_settings_summary']
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
