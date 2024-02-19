#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2006-2008 IBM All Rights Reserved
#
echo "Testing $1 in all locales"
outfile=$1-locale.txt
echo "" > $outfile
for loc in `locale -a`; do
echo LC_ALL=$loc >> $outfile
LC_ALL=$loc make check $2 >> $outfile 2>&1
done

echo "Done testing $1 in all locales"

