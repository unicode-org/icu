#/*
#************************************************************************
#* Copyright (C) 1997 - 2002, International Business Machines Corporation and
#* others. All Rights Reserved.
#************************************************************************
#*
#* $Source: /xsrl/Nsvn/icu/icu4j/Attic/build.sh,v $ 
#* $Date: 2002/03/18 22:11:04 $ 
#* $Revision: 1.5 $
#*
#*************************************************************************
#*/
#!/bin/sh
CLASSPATH=$JAVA_HOME/tools.jar:$ANT_HOME/lib/ant.jar
echo java -classpath $CLASSPATH org.apache.tools.ant.Main $* 
java -classpath $CLASSPATH org.apache.tools.ant.Main $*
