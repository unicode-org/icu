#!/bin/sh
echo "Testing $1 in all locales"
outfile=$1-locale.txt
echo "" > $outfile
for loc in `locale -a`; do
echo LC_ALL=$loc >> $outfile
LC_ALL=$loc make check >> $outfile
done

echo "Done testing $1 in all locales"

