#!/bin/bash
echo "Testing $1 in all timezones"
outfile=$1-timezone.txt
echo "" > $outfile
for timezone in `locate /usr/share/zoneinfo/|fgrep -v /right/|fgrep -v /posix/`; do
timezone=${timezone#/usr/share/zoneinfo/}
echo TZ=$timezone >> $outfile
TZ=$timezone make check >> $outfile
done

echo "Done testing $1 in all timezones"

