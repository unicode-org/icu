#/*
#*******************************************************************************
#* Copyright (C) 1997-2000, International Business Machines Corporation and    *
#* others. All Rights Reserved.                                                *
#*******************************************************************************
#*
#* $Source: /xsrl/Nsvn/icu/icu4j/Attic/build.sh,v $ 
#* $Date: 2000/03/15 00:40:27 $ 
#* $Revision: 1.3 $
#*
#*****************************************************************************************
#*/
#!/bin/sh

ADDL_CLASSPATH=build/icu4jtoolz.zip:jakarta-ant/lib/ant.jar:jakarta-ant/lib/xml.jar

if [ "$CLASSPATH" != "" ] ; then
  CLASSPATH=$CLASSPATH:$ADDL_CLASSPATH
else
 CLASSPATH=$ADDL_CLASSPATH
fi
export CLASSPATH

echo Building with classpath $CLASSPATH

java org.apache.tools.ant.Main $*
