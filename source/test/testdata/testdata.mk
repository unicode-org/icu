#**********************************************************************
#* Copyright (C) 1999-2000, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak icup=<path_to_icu_instalation> [Debug|Release]
#
#   03/19/2001  weiv, schererm  Created

.SUFFIXES : .res .txt

ALL : "$(TESTDATAOUT)\testdata.dat" 
	@echo Test data is built.

"$(TESTDATAOUT)\testdata.dat" :  "$(TESTDATAOUT)\root.res" "$(TESTDATAOUT)\te.res" "$(TESTDATAOUT)\te_IN.res" "$(TESTDATAOUT)\testtypes.res" "$(TESTDATAOUT)\testempty.res" "$(TESTDATAOUT)\empty.res" $(TESTDATAOUT)test.dat
	@echo Building test data
	@"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -v -m common -c -p testdata -O "$(PKGOPT)" -d "$(TESTDATAOUT)" -T "$(TESTDATAOUT)" -s "$(TESTDATAOUT)" <<
root.res
te.res
te_IN.res
testtypes.res
empty.res
test.dat
<<

# Inference rule for creating resource bundles
# Some test data resource bundles are known to have warnings and bad data.
# The -q option is there on purpose, so we don't see it normally.
{$(TESTDATA)}.txt.res:
	@echo Making Test Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -q -s$(TESTDATA) -d$(TESTDATAOUT) $(?F)

$(TESTDATAOUT)test.dat : {"$(ICUTOOLS)\gentest\$(CFG)"}gentest.exe
	"$(ICUTOOLS)\gentest\$(CFG)\gentest" -d$(TESTDATAOUT)