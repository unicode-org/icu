#!/bin/bash
# © 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

if [[ -z $1 ]]; then
	echo "Pass the current version tag of double-conversion as the first argument to this script";
	exit 1;
fi

filename="$1.tar.gz"
url="https://github.com/google/double-conversion/archive/$1.tar.gz";

echo "Updating upstream to $1";
echo "Will download $url";
read -p "Press any key to continue";

rm -f $filename;
wget $url;
rm -r upstream; # in case any files were deleted in the new version
mkdir upstream;
tar zxf $filename --strip 1 -C upstream;
rm $filename;
echo "upstream updated and $filename removed";
