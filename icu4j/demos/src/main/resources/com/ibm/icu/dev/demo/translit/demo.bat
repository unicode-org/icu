REM /*
REM  *******************************************************************************
REM  * Copyright (C) 2016 and later: Unicode, Inc. and others.
REM  * License & terms of use: http://www.unicode.org/copyright.html
REM  *******************************************************************************
REM  *******************************************************************************
REM  * Copyright (C) 1996-2004, International Business Machines Corporation and    *
REM  * others. All Rights Reserved.                                                *
REM  *******************************************************************************
REM  */
REM For best results, run the demo as an applet inside of Netscape
REM with Bitstream Cyberbit installed.

REM setup your JDK 1.1.x path and classpath here:
call JDK11
set CLASSPATH=../translit.jar;%CLASSPATH%
javaw Demo
