#!/bin/bash
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2006-2008 IBM All Rights Reserved
#
echo "Testing $1 in all timezones"
outfile=$1-timezone.txt
echo "" > $outfile
for timezone in `locate /usr/share/zoneinfo/|fgrep -v /right/|fgrep -v /posix/`; do
timezone=${timezone#/usr/share/zoneinfo/}
echo TZ=$timezone >> $outfile
TZ=$timezone make check $2 >> $outfile 2>&1
done

echo "Done testing $1 in all timezones"

