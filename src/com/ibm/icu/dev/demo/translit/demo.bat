REM /*
REM *******************************************************************************
REM  * Copyright (C) 1996-2000, International Business Machines Corporation and    *
REM  * others. All Rights Reserved.                                                *
REM  *******************************************************************************
REM  *
REM  * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/demo/translit/demo.bat,v $ 
REM  * $Date: 2000/03/10 03:47:44 $ 
REM  * $Revision: 1.2 $
REM  *
REM  *****************************************************************************************
REM  */
REM For best results, run the demo as an applet inside of Netscape
REM with Bitstream Cyberbit installed.

REM setup your JDK 1.1.x path and classpath here:
call JDK11
set CLASSPATH=../translit.jar;%CLASSPATH%
javaw Demo
