#!/usr/bin/python2.4
#   Copyright (C) 2010, International Business Machines
#   Corporation and others.  All Rights Reserved.
#
#   file name:  idna2nrm.py
#   encoding:   US-ASCII
#   tab size:   8 (not used)
#   indentation:4
#
#   created on: 2010jan28
#   created by: Markus W. Scherer

"""Turn Unicode IdnaMappingTable.txt into ICU gennorm2 source file format."""

__author__ = "Markus Scherer"

import re

replacements = [
  # Several versions of avoiding circular FFFD>FFFD mappings,
  # depending on the version of the input file.
  (re.compile(r"FFFD          ; disallowed"), "# FFFD (avoid circular mapping)"),
  (re.compile(r"\.\.FFFD"), "..FFFC"),
  (re.compile(r"(FFF[^E])\.\.FFFF"), "\1..FFFC"),
  # Since we switch between checking and not checking for STD3 character
  # restrictions at runtime, checking the non-LDH ASCII characters in code,
  # we treat these values here like their regular siblings.
  (re.compile(r"^([^;]+)  ; disallowed_STD3_valid"), r"# \1disallowed_STD3_valid"),
  (re.compile(r"; disallowed_STD3_mapped +; "), ">"),
  # Normal transformations.
  (re.compile(r"; disallowed"), ">FFFD"),
  (re.compile(r"; ignored"), ">"),
  (re.compile(r"^([^;]+)  ; valid"), r"# \1valid"),
  (re.compile(r"; mapped +; "), ">"),
  (re.compile(r"^([^;]+)  ; deviation +; "), r"# \1deviation >")
]

in_file = open("IdnaMappingTable.txt", "r")
out_file = open("uts46.txt", "w")

out_file.write("# Original file:\n")
for line in in_file:
  orig_line = line
  if line.startswith("# For documentation, see"):
    out_file.write(line)
    out_file.write(r"""
# ================================================
# This file has been reformatted into syntax for the
# gennorm2 Normalizer2 data generator tool.
#
# "valid", "disallowed_STD3_valid" and "deviation" lines are commented out.
# "mapped" and "disallowed_STD3_mapped" are changed to use the ">" mapping syntax.
# "disallowed" lines map to U+FFFD.
# "ignored" lines map to an empty string.
#
# Characters disallowed under STD3 rules are treated as valid or mapped;
# they are handled in code.
# Deviation characters are also handled in code.
#
# Use this file as the second gennorm2 input file after nfc.txt.
# ================================================
""")
    continue
  if line[0] in "#\r\n":
    out_file.write(line)
    continue
  for rep in replacements: line = rep[0].sub(rep[1], line)
  # Align inline comments at column 40.
  comment_pos = line.find("#", 1)
  if comment_pos < 40:
    line = line[:comment_pos] + ((40 - comment_pos) * ' ') + line[comment_pos:]
  elif comment_pos > 40:
    space_pos = comment_pos
    while space_pos > 0 and line[space_pos - 1] == ' ':
      space_pos = space_pos - 1
    if space_pos < 40:
      # Fewer than 40 characters before the comment:
      # Align comments at column 40.
      line = line[:40] + line[comment_pos:]
    else:
      # 40 or more characters before the comment:
      # Keep one space between contents and comment.
      line = line[:space_pos] + " " + line[comment_pos:]
  # Write the modified line.
  out_file.write(line)
  if "..FFFF" in orig_line and "..FFFC" in line:
    out_file.write("FFFE..FFFF    >FFFD\n");
in_file.close()
out_file.close()
