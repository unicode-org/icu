#/*
#*******************************************************************************
#* Copyright (C) 1997 - 2001, International Business Machines Corporation and    
#* others. All Rights Reserved.                                           #*******************************************************************************
#*
#* $Source: /xsrl/Nsvn/icu/icu4j/Attic/build.sh,v $ 
#* $Date: 2001/11/29 18:18:10 $ 
#* $Revision: 1.4 $
#*
#*****************************************************************************************
#*/
#!/bin/sh
CLASSPATH=$JAVA_HOME/tools.jar:$ANT_HOME/lib/ant.jar
echo java -classpath $CLASSPATH org.apache.tools.ant.Main $* 
java -classpath $CLASSPATH org.apache.tools.ant.Main $*
