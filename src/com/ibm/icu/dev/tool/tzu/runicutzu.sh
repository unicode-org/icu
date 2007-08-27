#!/bin/sh

# Copyright (c) 2007 International Business Machines Corporation and others.
# All rights reserved

MissingICUTZUJAR() {
  echo "The ICU4J Time Zone Update Utility (icutzu.jar) doesn't exist in $ICUTZU_HOME"
  Failure
}

MissingICU4JJAR() {
  echo "ICU for Java (icu4j.jar) doesn't exist in $ICUTZU_HOME"
  Failure
}

MissingICUTZUENV() {
  echo "runicutzuenv.sh file doesn't exist in $ICUTZU_HOME"
  Failure
}

MissingJAVAHOME() {
  echo "java does not exist in $JAVA_HOME/bin. Please update the JAVA_HOME enviroment variable in runicutzuenv.sh"
  Failure
}

Success() {
  echo
  echo "End of ICU4J Time Zone Update Utility (ICUTZU) completed successfully."
  exit 0
}

Failure() {
  echo
  echo "ICU4J Time Zone Update Utility (ICUTZU) did not complete successfully."
  exit -1
}


echo ""
echo "*********** Welcome to the ICU4J Time Zone Update Utility (ICUTZU) ***********"

# Set ICUTZU_HOME to the current directory.
ICUTZU_HOME=`pwd`
echo "ICUTZU Home: $ICUTZU_HOME"
echo

# Make sure certain files are present.
if [ ! -f "$ICUTZU_HOME/icutzu.jar" ] ; then MissingICUTZUJAR ; fi
if [ ! -f "$ICUTZU_HOME/icu4j.jar" ] ; then MissingICU4JJAR ; fi
if [ ! -f "$ICUTZU_HOME/runicutzuenv.sh" ] ; then MissingICUTZUENV ; fi

# Set environmental variables.
. "$ICUTZU_HOME/runicutzuenv.sh"
if [ ! -f "$JAVA_HOME/bin/java" ] ; then MissingJAVAHOME ; fi


# Create a temporary directory if one doesn't exit already.
if [ ! -d "$ICUTZU_HOME/Temp" ] ; then mkdir "$ICUTZU_HOME/Temp" ; fi

# Run the ICUTZU tool.
echo
echo "Launching the ICU4J Time Zone Update Utility (ICUTZU)..."
echo "\"$JAVA_HOME/bin/java\" -cp \"$ICUTZU_HOME/icutzu.jar\" -Dnogui=$NOGUI -Ddiscoveronly=$DISCOVERONLY -Dsilentpatch=$SILENTPATCH -Doffline=$OFFLINE com.ibm.icu.dev.tool.tzu.ICUTZUMain \"$ICUTZU_HOME/\" DirectorySearch.txt ICUList.txt zoneinfo.res Temp icu.gif"
echo

"$JAVA_HOME/bin/java" -cp "$ICUTZU_HOME/icutzu.jar" -Dnogui=$NOGUI -Ddiscoveronly=$DISCOVERONLY -Dsilentpatch=$SILENTPATCH -Doffline=$OFFLINE com.ibm.icu.dev.tool.tzu.ICUTZUMain "$ICUTZU_HOME/" DirectorySearch.txt ICUList.txt zoneinfo.res Temp icu.gif

# Test the exit code.
if [ $? -eq "0" ] ; then
  Success
else
  Failure
fi
