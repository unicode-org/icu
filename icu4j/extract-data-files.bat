@echo off
rem Copyright (C) 2023 and later: Unicode, Inc. and others.
rem License & terms of use: http://www.unicode.org/copyright.html

rem Check that all the .jar files are present

cd %icu4j_root%

set JAR_FILE=main\shared\data\icudata.jar
if exist %JAR_FILE% goto icutzdata
  echo Missing %JAR_FILE%
  goto:EOF

:icutzdata
set JAR_FILE=main\shared\data\icutzdata.jar
if exist %JAR_FILE% goto testdata
  echo Missing %JAR_FILE%
  goto:EOF

:testdata
set JAR_FILE=main\shared\data\testdata.jar
if exist %JAR_FILE% goto alljarsok
  echo Missing %JAR_FILE%
  goto:EOF

:alljarsok

rem Unpack the pre-built .jar files with data only

echo Unpacking icudata.jar
cd %icu4j_root%
rd /s/q main\core\src\main\resources\com\ibm\icu\impl\data\icudata
rem jar cannot extract to folder, and /C does now seem to work on Windows.
rem We have to switch folder explicitly
cd      main\core\src\main\resources
jar xf  %icu4j_root%\main\shared\data\icudata.jar
rd /s/q META-INF
cd %icu4j_root%

echo   Moving charset data
rd /s/q main\charset\src\main\resources\com\ibm\icu\impl\data\icudata
md      main\charset\src\main\resources\com\ibm\icu\impl\data\icudata
move    main\core\src\main\resources\com\ibm\icu\impl\data\icudata\*.cnv        main\charset\src\main\resources\com\ibm\icu\impl\data\icudata\
move    main\core\src\main\resources\com\ibm\icu\impl\data\icudata\cnvalias.icu main\charset\src\main\resources\com\ibm\icu\impl\data\icudata\

echo   Moving currency data
rd /s/q main\currdata\src\main\resources\com\ibm\icu\impl\data\icudata
md      main\currdata\src\main\resources\com\ibm\icu\impl\data\icudata
move    main\core\src\main\resources\com\ibm\icu\impl\data\icudata\curr        main\currdata\src\main\resources\com\ibm\icu\impl\data\icudata\curr

echo   Moving collate data
rd /s/q main\collate\src\main\resources\com\ibm\icu\impl\data\icudata
md      main\collate\src\main\resources\com\ibm\icu\impl\data\icudata
move    main\core\src\main\resources\com\ibm\icu\impl\data\icudata\coll        main\collate\src\main\resources\com\ibm\icu\impl\data\icudata\coll

echo   Moving langdata data
rd /s/q main\langdata\src\main\resources\com\ibm\icu\impl\data\icudata
md      main\langdata\src\main\resources\com\ibm\icu\impl\data\icudata
move    main\core\src\main\resources\com\ibm\icu\impl\data\icudata\lang         main\langdata\src\main\resources\com\ibm\icu\impl\data\icudata\lang

echo   Moving regiondata data
rd /s/q main\regiondata\src\main\resources\com\ibm\icu\impl\data\icudata
md      main\regiondata\src\main\resources\com\ibm\icu\impl\data\icudata
move    main\core\src\main\resources\com\ibm\icu\impl\data\icudata\region       main\regiondata\src\main\resources\com\ibm\icu\impl\data\icudata\region

echo   Moving translit data
rd /s/q main\translit\src\main\resources\com\ibm\icu\impl\data\icudata
md      main\translit\src\main\resources\com\ibm\icu\impl\data\icudata
move    main\core\src\main\resources\com\ibm\icu\impl\data\icudata\translit     main\translit\src\main\resources\com\ibm\icu\impl\data\icudata\translit

echo Unpacking icutzdata.jar
rem This unzips together with other existing core files
rem So we don't remove the folder
cd     %icu4j_root%\main\core\src\main\resources
jar xf %icu4j_root%\main\shared\data\icutzdata.jar
rd /s/q META-INF

echo Unpacking testdata.jar
cd %icu4j_root%
rd /s/q  main\core\src\test\resources\com\ibm\icu\dev\data\testdata
cd       main\core\src\test\resources
jar xf   %icu4j_root%\main\shared\data\testdata.jar
rd /s/q  META-INF
cd %icu4j_root%

echo Removing jar files
echo   icudata.jar
del main\shared\data\icudata.jar
echo   icutzdata.jar
del main\shared\data\icutzdata.jar
echo   testdata.jar
del main\shared\data\testdata.jar

rem remove shared folder, if empty
rd /q main\shared\data
rd /q main\shared

echo DONE
:EOF
