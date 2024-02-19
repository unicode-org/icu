#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2013 IBM Corp. and Others. All Rights Reserved
if [ ! -x ${1} ];
then
	echo could not exec ${1}
	echo usage: ${0} something.sh
	exit 1
fi
set -x
. "${1}"

${JAVA_HOME}/bin/java -version 2>/dev/null
