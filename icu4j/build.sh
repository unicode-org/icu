#/*
#*******************************************************************************
#* Copyright (C) 1997-2000, International Business Machines Corporation and    *
#* others. All Rights Reserved.                                                *
#*******************************************************************************
#*
#* $Source: /xsrl/Nsvn/icu/icu4j/Attic/build.sh,v $ 
#* $Date: 2000/03/10 04:17:55 $ 
#* $Revision: 1.2 $
#*
#*****************************************************************************************
#*/
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
