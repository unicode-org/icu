#!/bin/sh

ADDL_CLASSPATH=build/ant.jar:build/projectx-tr2.jar:build/javac.jar

if [ "$CLASSPATH" != "" ] ; then
  CLASSPATH=$CLASSPATH:$ADDL_CLASSPATH
else
 CLASSPATH=$ADDL_CLASSPATH
fi
export CLASSPATH

echo Building with classpath $CLASSPATH

java org.apache.tools.ant.Main $*
