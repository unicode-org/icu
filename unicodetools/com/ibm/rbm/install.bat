@echo off

rem *****************************************************************************
rem * Copyright (C) 2000-2002, International Business Machines Corporation and  *
rem * others. All Rights Reserved.                                              *
rem *****************************************************************************

call compile.bat -O

echo * Making Directories

mkdir zip
cd zip
mkdir lib
mkdir docs
cd docs
mkdir Images
cd Images
mkdir Screenshots
cd ..
mkdir Tutorial
mkdir Views
cd ..
mkdir Reports
cd ..

echo * Copying Files

echo ** Copying Batch Files and Properties

copy RBManager.jar zip
copy RBReporter.bat zip
copy rbmanager_scanner.xml zip
copy resources\preferences.properties zip
attrib -r zip/preferences.properties

echo ** Copying Documentation

cd docs
copy *.html ..\zip\docs\
cd Images
copy *.gif ..\..\zip\docs\Images\
copy *.jpg ..\..\zip\docs\Images\
cd Screenshots
copy *.gif ..\..\..\zip\docs\Images\Screenshots\
copy *.jpg ..\..\..\zip\docs\Images\Screenshots\
cd ..\..\Tutorial
copy *.html ..\..\zip\docs\Tutorial\
cd ..\Views
copy *.html ..\..\zip\docs\Views\
cd ..\..

echo ** Copying Jar Files

cd lib
copy *.jar ..\zip\lib\
cd ..

echo ** Copying License
copy ..\..\..\license.html zip

@echo * Directory created: zip
@echo ** Don't forget to modify preferences.properties
pause