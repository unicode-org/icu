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
  # Pass through disallowed ASCII characters: Handled in code.
  (re.compile(r"0000..002C    ; disallowed"), "# 0000..002C (allow ASCII)"),
  (re.compile(r"002F          ; disallowed"), "# 002F       (allow ASCII)"),
  (re.compile(r"003A..0040    ; disallowed"), "# 003A..0040 (allow ASCII)"),
  (re.compile(r"005B..0060    ; disallowed"), "# 005B..0060 (allow ASCII)"),
  (re.compile(r"007B..00A0    ; disallowed                                 #"),
   "0080..00A0    >FFFD  # (allow ASCII)"),
  # Normal transformations.
  (re.compile(r"; disallowed   "), ">FFFD"),
  (re.compile(r"; ignored      "), ">"),
  (re.compile(r"^([^;]+)  ; valid"), r"# \1valid"),
  (re.compile(r"; mapped     ; "), ">"),
  (re.compile(r"^([^;]+)  ; deviation"), r"# \1deviation"),
  (re.compile(r"   +(\#  [^\#]+)$"), r"  \1"),
  # Two versions of avoiding circular FFFD>FFFD mappings,
  # depending on the version of the input file.
  (re.compile(r"\.\.FFFD"), "..FFFC"),
  (re.compile(r"(FFF[^E])\.\.FFFF"), "\1..FFFC")
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
# Reformatting via regular expressions:
#   s/; disallowed   />FFFD/
#   s/; ignored      />/
#   s/^([^;]+)  ; valid/# \1valid/
#   s/; mapped     ; />/
#   s/^([^;]+)  ; deviation/# \1deviation/
#   s/   +(\#  [^\#]+)$/  \1/
#
# Except: Disallowed ASCII characters are passed through;
# they are handled in code.
# Deviation characters are also handled in code.
#
# A circular mapping FFFD>FFFD is avoided by rewriting the line that contains
# ..FFFD to contain ..FFFC instead.
#
# Use this file as the second gennorm2 input file after nfc.txt.
# ================================================
""")
    continue
  for rep in replacements: line = rep[0].sub(rep[1], line)
  out_file.write(line)
  if "..FFFF" in orig_line and "..FFFC" in line:
    out_file.write("FFFE..FFFF    >FFFD\n");
in_file.close()
out_file.close()
