#**********************************************************************
#* Copyright (C) 1999-2000, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak icup=<path_to_icu_instalation> [Debug|Release]
#
#	03/19/2001	weiv, schererm	Created

.SUFFIXES : .res .txt


ALL : "$(DLL_OUTPUT)\testdata.dll" 
	@echo Test data is built.

"$(DLL_OUTPUT)\testdata.dll" :  "$(TESTDATA)\root.res" "$(TESTDATA)\te.res" "$(TESTDATA)\te_IN.res" "$(TESTDATA)\testtypes.res" test.dat
	@echo Building test data
 	@"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -v -m dll -c -p testdata -O "$(PKGOPT)" -d "$(DLL_OUTPUT)" -T "$(TESTDATAOUT)" -s "$(TESTDATA)" <<
root.res
te.res
te_IN.res
testtypes.res
test.dat
<<

# Inference rule for creating resource bundles
{$(TESTDATA)}.txt.res:
        @echo Making Test Resource Bundle files
        @"$(ICUTOOLS)\genrb\$(CFG)\genrb" -s$(TESTDATA) -d$(TESTDATA) $(?F)

test.dat : {"$(ICUTOOLS)\gentest\$(CFG)"}gentest.exe
	"$(ICUTOOLS)\gentest\$(CFG)\gentest" -d$(TESTDATA)