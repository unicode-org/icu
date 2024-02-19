#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2013 IBM Corp. and Others. All Rights Reserved
if [ ! -x ${1} ];
then
	echo could not exec ${1}
	echo usage: ${0} something.jvm.sh  blah
	exit 1
fi
#JAVA_HOME=/somewhere/1_6
JAVA=java
#CLASSPATH=foo/bar.jar:/baz
#VM_OPTS=-Xmx265

. "${1}"


set -x
${JAVA_HOME}/bin/${JAVA} ${VM_OPTS} -classpath ${CLASSPATH}:${2} ${3}
