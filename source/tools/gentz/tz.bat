@echo off
REM Copyright (C) 1999, International Business Machines
REM Corporation and others.  All Rights Reserved.

REM This script is a Windows launcher for the tz.pl script.  For this
REM to work, the perl executable must be on the path.  We recommend
REM the ActiveState build; see http://www.activestate.com.  See the
REM tz.pl script itself for more documentation.

if "%OS%" == "Windows_NT" goto WinNT
perl -w -x -S "tz.pl" %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end
:WinNT
perl -w -x -S "tz.pl" %*
if NOT "%COMSPEC%" == "%SystemRoot%\system32\cmd.exe" goto end
if %errorlevel% == 9009 echo You do not have Perl in your PATH.
:end
