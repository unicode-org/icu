# *****************************************************************************
# * Copyright (C) 2000-2004, International Business Machines Corporation and  *
# * others. All Rights Reserved.                                              *
# *****************************************************************************

cd ../../../
echo compiling source code $1
javac -d . -classpath com/ibm/rbm/lib/xerces.jar -deprecation $1 com/ibm/rbm/*.java com/ibm/rbm/gui/*.java
#if errorlevel 1 goto error

echo creating jar file
rm com/ibm/rbm/RBManager.jar
jar cfm com/ibm/rbm/RBManager.jar com/ibm/rbm/manifest.stub com/ibm/rbm/*.class com/ibm/rbm/gui/*.class com/ibm/rbm/gui/images/*.gif com/ibm/rbm/resources/RBManager*.properties
#if errorlevel 1 goto error

echo cleaning up class files
cd com/ibm/rbm
rm -rf *.class gui/*.class

