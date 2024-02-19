#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2013 IBM Corp. and Others. All Rights Reserved
VER=/System/Library/Frameworks/JavaVM.framework/Versions
if [ ! -d "${VER}" ];
then
	echo err, cant find ${VER}   - are you really on a mac?
	exit 1
fi

VERS=`cd ${VER};ls -d [0-9]*`

for aver in ${VERS};
do
	if [ ! -x ${VER}/${aver}/Home/bin/java ]; then
		continue
	fi
	if ! ${VER}/${aver}/Home/bin/java -version 2> /dev/null; then
		continue
	fi
	VERNUM=`echo "${aver}" | tr '.' '_'`
	F=${VERNUM}.jvm.sh
	echo "# ${F} from ${VER}/${aver}"
	cat > ${F} <<EOF
JAVA_HOME=${VER}/${aver}/Home
EOF
	chmod a+rx ${F}
	
	if ! ./test.sh ${F}; then	
		rm ${F}
		echo "# Deleted: ${F} due to failure"
	fi
done
