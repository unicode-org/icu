# ***************************************************************************
# *
# *   Copyright (C) 1997-2005, International Business Machines
# *   Corporation and others.  All Rights Reserved.
# *
# ***************************************************************************

Steps for building ICU data from CLDR:
Developers of CLDR:
1. Check out CLDR from the CVS repository
2. Check out ICU from ICU CVS repository
3. Check out ICU4J from ICU CVS repository
4. Set the required environment variables
        export JAVA_HOME=<path>/java
        export ANT_OPTS="-DCLDR_DTD_CACHE=<path>/temp/cldrdtd"
        export CLDR_DIR=<path>/cldr   
        export CLDR_CLASSES=<path>/cldr/tools/java/classes
        export ICU4C_DIR=/work/icu
        export ICU4J_CLASSES=<path>/icu4j/classes
5. Change directory to <path>/icu/source/data/
6. Enter command
   <path>/ant/bin/ant all

Debugging in Eclipse:
1. From Eclipse select Run > Run from toolbar
2. Click New button
3. Go to Main tab and enter 
    Name: Ant_Launcher
    Project: cldr
    Main class: org.apache.tools.ant.launch.Launcher
4. Go to Arguments tab and enter
    Program Arguments: -buildfile c:\work\cldr\tools\java\build.xml  icu4c
    VM Arguments: -classpath C:\work\apache-ant-1.6.1\lib\ant-launcher.jar 
                  -Dant.home=C:\work\apache-ant-1.6.1 
                  -DCLDR_DTD_CACHE=/work/temp/cldrdtd/
5. Go to Environment tag and create new variables
        CLDR_DIR=<path>/cldr   
        CLDR_CLASSES=<path>/cldr/tools/java/classes
        ICU4C_DIR=/work/icu
        ICU4J_CLASSES=<path>/icu4j/classes
6. Set a break point in CLDRBuild or the tool class that needs to be debugged.

Users of CLDR:
1. Download cldrtools.zip from the CLDR website and unzip in a directory
2. Download cldr.zip from the CLDR website and unzip in cldr directory
3. Check out ICU from ICU CVS repository
4. Set the required environment variables
        export JAVA_HOME=<path>/java
        export ANT_OPTS="-DCLDR_DTD_CACHE=<path>/temp/cldrdtd"
        export CLDR_DIR=<path>/cldr   
        export CLDR_JAR=<path>/cldr.jar
        export ICU4C_DIR=<path>/icu
        export ICU4J_JAR=<path>/icu4j.jar 
        export UTILITIES_JAR=<path>/utilities.jar
5. Change directory to <path>/icu/source/data/
6. Enter command
   <path>/ant/bin/ant all
