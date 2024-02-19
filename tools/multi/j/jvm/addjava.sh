#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2013 IBM Corp. and Others. All Rights Reserved
if [ ! -x $1 ];
then
	echo usage: $0 path/to/bin/java
	exit 1
fi

if ! $1 -version > /dev/null ;
then
	echo could not run $1
	exit 2
fi

if ! ( $1 -version 2>&1 | grep -q "java version" );
then
	echo could not get java version from $1
	exit 3
fi

VER=`$1 -version 2>&1 | grep "java version" | head -1 | sed -e 's%^java version "\([^"]*\)\".*$%\1%'`
VERD=`echo $VER | tr . _`

echo $1 = $VER / $VERD

FILE="$VERD.sh"

if [ -f $FILE ];
then
	FILE="${VERD}_$$.sh"
	echo "$VERD.sh existed, renaming to $FILE - rename to something sane if you want"
fi

JHOME=`dirname $1`
JHOME=`dirname $JHOME`

echo "#!/bin/sh" > $FILE
echo "# auto generated for $VER from $1 / addjava.sh" >> $FILE
echo "JAVA_HOME=$JHOME" >> $FILE
echo >> $FILE
echo >> $FILE
$1 -version 2>&1 | sed -e 's%^%#%' >> $FILE

chmod a+rx $FILE

echo 
echo "Created: $FILE"





