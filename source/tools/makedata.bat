rem @echo off
rem makedata.bat
rem batch file for Windows for creating the ICU data files
rem parameter:
rem %1 path where the icu folder resides

if "%1"=="" goto :error

if "%ICU_DATA%"=="" set ICU_DATA=%1\icu\data\
set toolversion=Debug

cd %1\icu\data

rem create conversion tables
call %1\icu\source\tools\makeconv\mkcnvfle %toolversion% %1

rem create locale resource bundles
call %1\icu\source\tools\genrb\genrb %1\icu\source\tools\genrb\%toolversion% %1

rem create binary collation tables
%1\icu\source\tools\gencol\%toolversion%\gencol

goto :end

:error
echo call makedata with the absolute path to the icu directory
echo for example, if the full path is d:\mytools\icu then call
echo makedata d:\mytools
echo the current directory will be changed to d:\mytools\icu\data

:end
