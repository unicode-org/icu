#**********************************************************************
#* Copyright (C) 1999-2000, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
#
#   03/19/2001  weiv, schererm  Created

.SUFFIXES : .res .txt

TESTPKG=testdata
TESTDT=$(TESTPKG)_


ALL : "$(TESTDATAOUT)\testdata.dat" 
	@echo Test data is built.

"$(TESTDATAOUT)\testdata.dat" : "$(TESTDATABLD)\casing.res" "$(TESTDATABLD)\root.res" "$(TESTDATABLD)\te.res" "$(TESTDATABLD)\te_IN.res" "$(TESTDATABLD)\testaliases.res" "$(TESTDATABLD)\testtypes.res" "$(TESTDATABLD)\testempty.res" "$(TESTDATABLD)\iscii.res" "$(TESTDATABLD)\DataDrivenCollationTest.res" $(TESTDATABLD)\testdata_test.icu "$(TESTDATABLD)\testdata_test1.cnv" "$(TESTDATABLD)\testdata_test3.cnv" "$(TESTDATABLD)\testdata_test4.cnv" 
	@echo Building test data
	@copy "$(TESTDATABLD)\testdata_te.res" "$(TESTDATAOUT)\testdata_nam.typ"
	@"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -f -v -m common -c -p"$(TESTPKG)"  -O "$(PKGOPT)" -d "$(TESTDATAOUT)" -T "$(TESTDATABLD)" -s "$(TESTDATABLD)" <<
testdata_casing.res
testdata_root.res
testdata_te.res
testdata_te_IN.res
testdata_testtypes.res
testdata_testempty.res
testdata_testaliases.res
testdata_iscii.res
testdata_DataDrivenCollationTest.res
testdata_test.icu
testdata_test1.cnv
testdata_test3.cnv
testdata_test4.cnv
<<


# Inference rule for creating resource bundles
# Some test data resource bundles are known to have warnings and bad data.
# The -q option is there on purpose, so we don't see it normally.
{$(TESTDATA)}.txt.res:: 
	@echo Making Test Resource Bundle files $<
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -t -p"$(TESTPKG)" -q -s"$(TESTDATA)" -d"$(TESTDATABLD)" $<

"$(TESTDATABLD)\iscii.res":
	@echo Making Test Resource Bundle file with encoding x-iscii-de
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -t -p"$(TESTPKG)" -q -s"$(TESTDATA)" -ex-iscii-de -d"$(TESTDATABLD)" iscii.bin 

$(TESTDATABLD)\testdata_test.icu : {"$(ICUTOOLS)\gentest\$(CFG)"}gentest.exe
	"$(ICUTOOLS)\gentest\$(CFG)\gentest" -d"$(TESTDATABLD)"



# Targets for test converter data
"$(TESTDATABLD)\testdata_test1.cnv": "$(TESTDATA)\test1.ucm"
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" -d"$(TESTDATABLD)" -t -p testdata  $**

"$(TESTDATABLD)\testdata_test3.cnv": "$(TESTDATA)\test3.ucm"
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv"  -d"$(TESTDATABLD)" -t -p testdata  $**

"$(TESTDATABLD)\testdata_test4.cnv": "$(TESTDATA)\test4.ucm"
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv"  -d"$(TESTDATABLD)" -t -p testdata $**
