REM /**
REM *******************************************************************************
REM * Copyright (C) 2002-2002, International Business Machines Corporation and    *
REM * others. All Rights Reserved.                                                *
REM *******************************************************************************
REM *
REM * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/translit/indic.bat,v $
REM * $Date: 2002/12/18 03:56:41 $
REM * $Revision: 1.2 $
REM *
REM *******************************************************************************
REM */

@echo off
REM This script is a Windows launcher for the indic.pl script.  For this
REM to work, the perl executable must be on the path.  We recommend
REM the ActiveState build; see http://www.activestate.com.  See the
REM tz.pl script itself for more documentation.

if "%OS%" == "Windows_NT" goto WinNT
perl -w -x indic.pl %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end
:WinNT
perl -w -x indic.pl %*
if NOT "%COMSPEC%" == "%SystemRoot%\system32\cmd.exe" goto end
if %errorlevel% == 9009 echo You do not have Perl in your PATH.
:end
