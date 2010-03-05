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
  (re.compile(r"; disallowed   "), ">FFFD"),
  (re.compile(r"; ignored      "), ">"),
  (re.compile(r"^([^;]+)  ; valid"), r"# \1valid"),
  (re.compile(r"; mapped     ; "), ">"),
  (re.compile(r"; deviation  ; "), ">"),
  (re.compile(r"   +(\#  [^\#]+)$"), r"  \1"),
  (re.compile(r"\.\.FFFF"), "..FFFC")
]

in_file = open("IdnaMappingTable.txt", "r")
out_file = open("uts46.txt", "w")

out_file.write("# Original file:\n")
for line in in_file:
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
#   s/; deviation  ; />/
#   s/   +(\#  [^\#]+)$/  \1/
#
# A circular mapping FFFD>FFFD is avoided by rewriting the line that starts with
# FFEF..FFFF to two lines, splitting this range and omitting FFFD.
#
# Use this file as the second gennorm2 input file after nfc.txt.
# ================================================
""")
    continue
  for rep in replacements: line = rep[0].sub(rep[1], line)
  out_file.write(line)
  if "..FFFC" in line:
    out_file.write("FFFE..FFFF    >FFFD\n");
in_file.close()
out_file.close()
