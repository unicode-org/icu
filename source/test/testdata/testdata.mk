#**********************************************************************
#* Copyright (C) 1999-2000, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
#
#   03/19/2001  weiv, schererm  Created

.SUFFIXES : .res .txt


ALL : "$(TESTDATAOUT)\testdata.dat" 
	@echo Test data is built.

"$(TESTDATAOUT)\testdata.dat" : "$(TESTDATABLD)\ja_data.res" "$(TESTDATABLD)\casing.res" "$(TESTDATABLD)\root.res" "$(TESTDATABLD)\te.res" "$(TESTDATABLD)\te_IN.res" "$(TESTDATABLD)\testaliases.res" "$(TESTDATABLD)\testtypes.res" "$(TESTDATABLD)\testempty.res" "$(TESTDATABLD)\ja_data.res" "$(TESTDATAOUT)\DataDrivenCollationTest.res" $(TESTDATAOUT)test.dat
	@echo Building test data
	copy "$(TESTDATABLD)\te.res" "$(TESTDATAOUT)\testudata_nam.typ"
	@"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -v -m common -c -p testdata -O "$(PKGOPT)" -d "$(TESTDATAOUT)" -T "$(TESTDATABLD)" -s "$(TESTDATABLD)" <<
casing.res
root.res
te.res
te_IN.res
testtypes.res
testempty.res
testaliases.res
ja_data.res
DataDrivenCollationTest.res
test.dat
<<


# Inference rule for creating resource bundles
# Some test data resource bundles are known to have warnings and bad data.
# The -q option is there on purpose, so we don't see it normally.
{$(TESTDATA)}.txt.res:: 
	@echo Making Test Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -q -s"$(TESTDATA)" -d"$(TESTDATABLD)" $<

"$(TESTDATABLD)\ja_data.res":
	@echo Making Test Resource Bundle file with encoding ISO-2022-JP
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -q -s"$(TESTDATA)" -eISO_2022_JP -d"$(TESTDATABLD)" ja_data.bin 

$(TESTDATAOUT)test.dat : {"$(ICUTOOLS)\gentest\$(CFG)"}gentest.exe
	"$(ICUTOOLS)\gentest\$(CFG)\gentest" -d"$(TESTDATABLD)"