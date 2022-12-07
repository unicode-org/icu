#!/bin/bash
# Copyright (C) 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This command lists all the resource files in a single directory that we want to match on.
# Then we filter the locale resource files, based on the file name, using the same match pattern
# used in the corresponding target in the Ant build.xml configuration.
# Then, we store the locale names in a file in that directory, which ICUResourceBundle.java might look for.
#
# Input argument $1 = directory containing containing *.res files
ls $1/*.res\
 | ruby -lane 'puts "basename "+$F[0]' \
 | sh \
 | ruby -lane 'puts $F[0].match(/^(..\.res|.._.*\.res|...\.res|..._.*\.res|root.res)$/)' \
 | egrep -v "res_index.res|^$" \
 | ruby -lane 'puts $F[0].match(/(.*).res$/)[1]' \
 | sort \
 > $1/fullLocaleNames.lst