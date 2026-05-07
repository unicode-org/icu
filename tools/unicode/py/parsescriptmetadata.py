#!/usr/bin/python3 -B
# -*- coding: utf-8 -*-
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2013-2016 International Business Machines
# Corporation and others. All Rights Reserved.
#
# parsescriptmetadata.py
#
# 2013feb15 Markus W. Scherer
#
# ./parsescriptmetadata.py
#   ~/svn.icu/trunk/src/source/common/unicode/uscript.h
#   ~/svn.cldr/trunk/common/properties/scriptMetadata.txt

"""Parses ICU4C uscript.h & CLDR scriptMetadata.txt,
and writes ICU script data initializers."""

import re
import sys

def main():
  if len(sys.argv) < 3:
    print("Usage: {}  path/to/ICU4C/uscript.h  "
          "path/to/CLDR/scriptMetadata.txt".format(sys.argv[0]))
    return
  (uscript_path, smd_path) = sys.argv[1:3]

  iso_to_icu = {}
  max_icu_num = 0

  # Parse lines like
  #   USCRIPT_ARABIC       =  2,  /* Arab */
  # and extract the ICU numeric script code and the ISO script code.
  script_num_re = re.compile(r" *= *([0-9]+), */\* *([A-Z][a-z]{3}) *\*/")
  with open(uscript_path, "r") as uscript_file:
    for line in uscript_file:
      line = line.strip()
      if not line: continue
      if line.startswith("#"): continue  # whole-line comment
      match = script_num_re.search(line)
      if match:
        icu_num = int(match.group(1))
        iso_to_icu[match.group(2)] = icu_num
        if icu_num > max_icu_num: max_icu_num = icu_num

  icu_data = [None] * (max_icu_num + 1)

  # Parse lines like
  #   Arab; 8; 0628; SA; 1; RECOMMENDED; YES; NO; YES; NO; NO
  # and put the data (as strings) into the icu_data list.
  with open(smd_path, "r") as smd_file:
    for line in smd_file:
      comment_start = line.find("#")
      if comment_start >= 0: line = line[0:comment_start]
      line = line.strip()
      if not line: continue

      fields = line.split(";")
      if not fields or len(fields) < 11: continue
      iso_code = fields[0].strip()
      icu_num = iso_to_icu[iso_code]
      icu_data[icu_num] = (iso_code,
          # sample, usage
          fields[2].strip(), fields[5].strip(),
          # RTL, LB, cased
          fields[6].strip(), fields[7].strip(), fields[10].strip())

  # Print ICU array initializers with the relevant data.
  for t in icu_data:
    if t:
      (iso_code, sample, usage, rtl, lb, cased) = t
      s = "0x" + sample + " | " + usage
      if rtl == "YES": s += " | RTL"
      if lb == "YES": s += " | LB_LETTERS"
      if cased == "YES": s += " | CASED"
      print("    " + s + ",  // " + iso_code)
    else:
      print("    0,")


if __name__ == "__main__":
  main()
