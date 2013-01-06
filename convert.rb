#!/usr/bin/env ruby
# encoding: utf-8

require 'nokogiri'

# read in convert.xml to see which how to convert the string-arrays
# for each strings.xml in res/values res/values-?? res/values-??-*
#   find the string-arrays that match the ones in convert.xml and for each
#     remove opening and closing string-array tag
#     change each item to a string with the name from the corresponding convert.xml item

# e.g., this:
#   <string-array name="statuses">
#     <item>Unplugged</item>
#     <item>Unknown</item>
#     <item>Charging</item>
#     <item>Discharging</item>
#     <item>Not Charging</item>
#     <item>Fully Charged</item>
#   </string-array>

# based on this in convert.xml:
#   <string-array name="statuses">
#     <item>status_unplugged</item>
#     <item>status_unknown</item>
#     <item>status_charging</item>
#     <item>status_discharging</item>
#     <item>status_not_charging</item>
#     <item>status_fully_charged</item>
#   </string-array>

# becomes:
#   <string formatted="false" name="status_unplugged">Unplugged</string>
#   etc.

def parse_string(element)
  return nil if element.nil?

  s = ''

  # element.text strips HTML like <b> and/or <i> that we want to keep, so we loop over the children
  #  taking each child's to_xml to preserve them.  Manually setting encoding seems to be necessary
  #  to preserve multi-byte characters.
  element.children.each do |c|
    s << c.to_xml(:encoding => 'utf-8')
  end

  s
end

conversions = {}

conv_doc = Nokogiri::XML(IO.read('./conversions.xml'))

conv_doc.xpath('//string-array').each do |sa_el|
  a = []

  sa_el.element_children.each_with_index do |item_el, i|
    a[i] = parse_string(item_el)
  end

  conversions[sa_el.attr('name')] = a
end

str_xml_files = `echo -n res/values/strings.xml res/values-??/strings.xml res/values-??-*/strings.xml`.split

str_xml_files.each do |str_xml_file|
  old_content = IO.read(str_xml_file)
  new_content = ""
  existing = {}

  in_array = false
  i = -1

  old_content.each_line do |line|
    if in_array
      if i >= conversions[in_array].length
        in_array = false
        next
      end

      name = conversions[in_array][i]
      value = line.split("<item>")[1].split("</item>")[0]

      if existing.has_key?(name)
        puts "Name #{name} already exists with content: #{existing[name]}"
        if existing[name] == value
          puts "  (Skipped) new content is the same: #{value}"
        else
          puts "  (Skipped) new content is different: #{value}"
        end
      else
        existing[name] = value
        new_content << %Q{  <string formatted="false" name="#{name}">#{value}</string>\n}
      end

      i += 1
      next
    end

    if not line.start_with?("  <string-array ")
      new_content << line
      next
    end

    if line.count('"') != 2
      new_content << line
      next
    end

    a_name = line.split('"')[1]

    if not conversions.has_key?(a_name)
      new_content << line
      next
    end

    in_array = a_name
    i = 0
  end

  IO.write(str_xml_file, new_content)
end
