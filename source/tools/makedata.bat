@echo off
rem makedata.bat
rem batch file for Windows for creating the ICU data files
rem parameter:
rem %1 path where the icu folder resides

if "%1"=="" goto :error

if "%ICU_DATA%"=="" set ICU_DATA=%1\icu\data\

rem toolversion: Debug or Release
set toolversion=Release
if not "%2"=="" set toolversion=%2

nmake /f makedata.mak icup=%1 cfg=%2 %3 %4
if not errorlevel 9009 goto :othererror
echo Build tools are not on path! Please make sure that MSVC++ is setup correctly!
goto :error
:othererror
if errorlevel 0 goto :end
echo Nmake has unsuccesfully finished with errorcode %errorlevel%!
goto :end

rem setup mkhelper to read ucmfiles.mk and ucmlocal.mk
set conv=mkhelper\%toolversion%\mkhelper -n UCM_SOURCE -n UCM_SOURCE_LOCAL makeconv\ucmfiles.mk makeconv\ucmlocal.mk

echo create conversion tables
rem delete preexisting files to prevent upper/lowercase file name problems
del "%ICU_DATA%*.cnv"
del "%ICU_DATA%*_cnv.c"
del *_cnv.obj
%conv% -p "$toolversion$\makeconv \"$ICU_DATA$" -s "\"">makeconv\mkcnvtmp.bat
cd makeconv
call mkcnvtmp.bat

echo create locale resource bundles
cd ..\genrb
call genrb %toolversion% %1

echo create binary collation tables
cd ..\gencol
%toolversion%\gencol

cd ..

echo create unames.dat and unames_dat.c from UnicodeData.txt
gennames\%toolversion%\gennames -v- -c- "%ICU_DATA%UnicodeData-3.0.0.txt"
genccode\%toolversion%\genccode "%ICU_DATA%unames.dat"

echo create cnvalias.dat and cnvalias_dat.c from convrtrs.txt
gencnval\%toolversion%\gencnval -c-
genccode\%toolversion%\genccode "%ICU_DATA%cnvalias.dat"

echo create tz.dat from tz.txt
gentz\%toolversion%\gentz -c- gentz\tz.txt
genccode\%toolversion%\genccode "%ICU_DATA%tz.dat"

echo create the converters
%conv% -p "genccode\$toolversion$\genccode \"$ICU_DATA$" -s "\"" -old ".ucm" -new ".cnv">mkconv.bat
call mkconv.bat

echo create the data DLL
type mkobj.cl>mkobj.tmp
echo "%ICU_DATA%unames_dat.c">>mkobj.tmp
echo "%ICU_DATA%cnvalias_dat.c">>mkobj.tmp
echo "%ICU_DATA%tz_dat.c">>mkobj.tmp
%conv% -p "\"$ICU_DATA$" -s "\"" -old ".ucm" -new "_cnv.c">>mkobj.tmp
cl @mkobj.tmp

echo "/out:%ICU_DATA%icudata.dll">mkdll.tmp
echo unames_dat.obj>>mkdll.tmp
echo cnvalias_dat.obj>>mkdll.tmp
echo tz_dat.obj>>mkdll.tmp
%conv% -old ".ucm" -new "_cnv.obj">>mkdll.tmp
type mkdll.lk>>mkdll.tmp
link @mkdll.tmp

echo create the common, memory-mappable file
del "%ICU_DATA%icudata.dat"
echo %ICU_DATA%unames.dat>mkmap.tmp
echo %ICU_DATA%cnvalias.dat>>mkmap.tmp
echo %ICU_DATA%tz.dat>>mkmap.tmp
%conv% -p "$ICU_DATA$" -old ".ucm" -new ".cnv">>mkmap.tmp
gencmn\%toolversion%\gencmn 1000000 mkmap.tmp


goto :end

:error
echo call makedata with the absolute path to the icu directory
echo for example, if the full path is d:\mytools\icu then call
echo makedata d:\mytools
echo a second, optional, parameter can be Debug or Release to specify the tools versions
echo.
echo the current directory must be the icu\source\tools directory with makedata.bat
echo also, the cl compiler and link linker must be on the PATH

:end
