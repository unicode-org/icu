@echo off
REM  ********************************************************************
REM  * COPYRIGHT:
REM  * Copyright (c) 2010, International Business Machines Corporation
REM  * and others. All Rights Reserved.
REM  ********************************************************************

set ICU_ARCH=%1
set ICU_DBRL=%2

if "%1" == "" (
echo Usage: %0 "x86 or x64"  "Debug or Release"
exit /b 1
)

if "%2" == "" (
echo Usage: %0 %1 "Debug or Release"
exit /b 1
)

set ICU_OPATH=%PATH%

set ICU_ICUDIR=%~f0\..\..\..
set ICU_BINDIR=%ICU_ICUDIR%\bin
set PATH=%ICU_BINDIR%;%PATH%

echo testing ICU in %ICU_ICUDIR%  arch=%ICU_ARCH% type=%ICU_DBRL%
pushd %ICU_ICUDIR%

echo "====" icuinfo
%ICU_ICUDIR%\source\tools\icuinfo\%ICU_ARCH%\%ICU_DBRL%\icuinfo.exe %ICUINFO_OPTS%

echo "====" intltest
cd %ICU_ICUDIR%\source\test\intltest
%ICU_ICUDIR%\source\test\intltest\%ICU_ARCH%\%ICU_DBRL%\intltest.exe %INTLTEST_OPTS%

echo "====" iotest
cd %ICU_ICUDIR%\source\test\iotest
%ICU_ICUDIR%\source\test\iotest\%ICU_ARCH%\%ICU_DBRL%\iotest.exe %IOTEST_OPTS%

echo "====" cintltst
cd %ICU_ICUDIR%\source\test\cintltst
%ICU_ICUDIR%\source\test\cintltst\%ICU_ARCH%\%ICU_DBRL%\cintltst.exe %CINTLTST_OPTS%

echo "====" letest
cd %ICU_ICUDIR%\source\test\letest
%ICU_ICUDIR%\source\test\letest\%ICU_ARCH%\%ICU_DBRL%\letest.exe %LETEST_OPTS%

REM clean up
set PATH=%ICU_OPATH%
REM unset ICU_OPATH
popd

@REM done
