#**********************************************************************
#* Copyright (C) 1999-2003, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
#
#   03/19/2001  weiv, schererm  Created

.SUFFIXES : .res .txt

TESTPKG=testdata
TESTDT=$(TESTPKG)_


ALL : "$(TESTDATAOUT)\testdata.dat" 
	@echo Test data is built.

"$(TESTDATAOUT)\testdata.dat" : "$(TESTDATABLD)\casing.res" "$(TESTDATABLD)\mc.res" "$(TESTDATABLD)\root.res" "$(TESTDATABLD)\te.res" "$(TESTDATABLD)\te_IN.res" "$(TESTDATABLD)\testaliases.res" "$(TESTDATABLD)\testtypes.res" "$(TESTDATABLD)\testempty.res" "$(TESTDATABLD)\$(TESTDT)iscii.res" "$(TESTDATABLD)\$(TESTDT)idna_rules.res" "$(TESTDATABLD)\DataDrivenCollationTest.res" $(TESTDATABLD)\$(TESTDT)test.icu "$(TESTDATABLD)\$(TESTDT)test1.cnv" "$(TESTDATABLD)\$(TESTDT)test3.cnv" "$(TESTDATABLD)\$(TESTDT)test4.cnv" "$(TESTDATABLD)\$(TESTDT)ibm9027.cnv"
	@echo Building test data
	@copy "$(TESTDATABLD)\$(TESTDT)te.res" "$(TESTDATAOUT)\$(TESTDT)nam.typ"
	@"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -f -v -m common -c -p"$(TESTPKG)"  -O "$(PKGOPT)" -d "$(TESTDATAOUT)" -T "$(TESTDATABLD)" -s "$(TESTDATABLD)" <<
$(TESTDT)casing.res
$(TESTDT)mc.res
$(TESTDT)root.res
$(TESTDT)te.res
$(TESTDT)te_IN.res
$(TESTDT)testtypes.res
$(TESTDT)testempty.res
$(TESTDT)testaliases.res
$(TESTDT)iscii.res
$(TESTDT)DataDrivenCollationTest.res
$(TESTDT)test.icu
$(TESTDT)test1.cnv
$(TESTDT)test3.cnv
$(TESTDT)test4.cnv
$(TESTDT)ibm9027.cnv
$(TESTDT)idna_rules.res
<<


# Inference rule for creating resource bundles
# Some test data resource bundles are known to have warnings and bad data.
# The -q option is there on purpose, so we don't see it normally.
{$(TESTDATA)}.txt.res:: 
	@echo Making Test Resource Bundle files $<
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -t -p"$(TESTPKG)" -q -s"$(TESTDATA)" -d"$(TESTDATABLD)" $<

"$(TESTDATABLD)\$(TESTDT)iscii.res": "$(TESTDATA)\iscii.bin"
	@echo Making Test Resource Bundle file with encoding ISCII,version=0
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -p"$(TESTPKG)" -q -s"$(TESTDATA)" -eISCII,version=0 -d"$(TESTDATABLD)" iscii.bin

"$(TESTDATABLD)\$(TESTDT)idna_rules.res": "$(TESTDATA)\idna_rules.txt"
	@echo Making Test Resource Bundle file for IDNA reference implementation
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -p"$(TESTPKG)" -q -s"$(TESTDATA)" -d"$(TESTDATABLD)" idna_rules.txt


$(TESTDATABLD)\$(TESTDT)test.icu : {"$(ICUTOOLS)\gentest\$(CFG)"}gentest.exe
	"$(ICUTOOLS)\gentest\$(CFG)\gentest" -d"$(TESTDATABLD)"



# Targets for test converter data
"$(TESTDATABLD)\$(TESTDT)test1.cnv": "$(TESTDATA)\test1.ucm"
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" -d"$(TESTDATABLD)" -p $(TESTPKG) $**

"$(TESTDATABLD)\$(TESTDT)test3.cnv": "$(TESTDATA)\test3.ucm"
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" -d"$(TESTDATABLD)" -p $(TESTPKG) $**

"$(TESTDATABLD)\$(TESTDT)test4.cnv": "$(TESTDATA)\test4.ucm"
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" -d"$(TESTDATABLD)" -p $(TESTPKG) $**

"$(TESTDATABLD)\$(TESTDT)ibm9027.cnv": "$(TESTDATA)\ibm9027.ucm"
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" -d"$(TESTDATABLD)" -p $(TESTPKG) $**

