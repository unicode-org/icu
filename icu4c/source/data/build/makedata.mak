#**********************************************************************
#* Copyright (C) 1999-2000, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak icup=<path_to_icu_instalation> [Debug|Release]
#
#	12/10/1999	weiv	Created

U_ICUDATA_NAME=icudt17l

#If no config, we default to debug
!IF "$(CFG)" == ""
CFG=Debug
!MESSAGE No configuration specified. Defaulting to common - Win32 Debug.
!ENDIF

#Here we test if a valid configuration is given
!IF "$(CFG)" != "Release" && "$(CFG)" != "release" && "$(CFG)" != "Debug" && "$(CFG)" != "debug"
!MESSAGE Invalid configuration "$(CFG)" specified.
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE
!MESSAGE NMAKE /f "makedata.mak" CFG="Debug"
!MESSAGE
!MESSAGE Possible choices for configuration are:
!MESSAGE
!MESSAGE "Release"
!MESSAGE "Debug"
!MESSAGE
!ERROR An invalid configuration is specified.
!ENDIF

#Let's see if user has given us a path to ICU
#This could be found according to the path to makefile, but for now it is this way
!IF "$(ICUP)"==""
!ERROR Can't find path!
!ENDIF
!MESSAGE icu path is $(ICUP)
ICUDATA=$(ICUP)\data

!IF "$(ICUDBLD)"==""
!ERROR Can't find ICUDBLD (ICU Data Build dir, should point to icu\source\data\build\ )!
!ENDIF
!MESSAGE icu data build path is $(ICUDBLD)

#ICUDBLD=$(ICUP)\source\data\build

TESTDATA=$(ICUP)\source\test\testdata

#If ICU_DATA is not set, we want to output stuff in binary directory
DLL_OUTPUT=$(ICUP)\source\data
TESTDATAOUT=$(TESTDATA)
#TESTDATAOUT=$(DLL_OUTPUT)


ICD=$(ICUDATA)^\
DATA_PATH=$(ICUP)\data^\
TEST=..\source\test\testdata^\
ICUTOOLS=$(ICUP)\source\tools

ICU_DATA=$(ICUDBLD)
!MESSAGE Intermediate files will go in $(ICU_DATA)

# We have to prepare params for pkgdata - to help it find the tools
!IF "$(CFG)" == "Debug" || "$(CFG)" == "debug"
PKGOPT=D:$(ICUP)
!ELSE
PKGOPT=R:$(ICUP)
!ENDIF

# This appears in original Microsofts makefiles
!IF "$(OS)" == "Windows_NT"
NULL=
!ELSE
NULL=nul
!ENDIF

# Adjust the path to find DLLs. If $(U_ICUDATA_NAME).dll really needs to be in $(ICUP)\bin\$(CFG),
# then add $(ICUP)\bin\$(CFG) to this path, as the other DLLs are in $(ICUP)\bin.
PATH = $(PATH);$(ICUP)\bin

# Suffixes for data files
.SUFFIXES : .ucm .cnv .dll .dat .res .txt .c

# We're including a list of ucm files. There are two lists, one is essential 'ucmfiles.mk' and
# the other is optional 'ucmlocal.mk'
!IF EXISTS("$(ICUDATA)\ucmfiles.mk")
!INCLUDE "$(ICUDATA)\ucmfiles.mk"
!IF EXISTS("$(ICUDATA)\ucmlocal.mk")
!INCLUDE "$(ICUDATA)\ucmlocal.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_LOCAL)
!ELSE
#!MESSAGE Warning: cannot find "ucmlocal.mk"
!ENDIF
!ELSE
!ERROR ERROR: cannot find "ucmfiles.mk"
!ENDIF
CNV_FILES=$(UCM_SOURCE:.ucm=.cnv)

# Read list of resource bundle files
!IF EXISTS("$(ICUDATA)\resfiles.mk")
!INCLUDE "$(ICUDATA)\resfiles.mk"
!IF EXISTS("$(ICUDATA)\reslocal.mk")
!INCLUDE "$(ICUDATA)\reslocal.mk"
GENRB_SOURCE=$(GENRB_SOURCE) $(GENRB_SOURCE_LOCAL)
!ELSE
#!MESSAGE Warning: cannot find "reslocal.mk"
!ENDIF
!ELSE
!ERROR ERROR: cannot find "resfiles.mk"
!ENDIF
RB_FILES = $(GENRB_SOURCE:.txt=.res)
TRANSLIT_FILES = $(TRANSLIT_SOURCE:.txt=.res)
ALL_RES = $(RB_FILES) $(TRANSLIT_FILES)

RB_SOURCE_DIR = $(GENRB_SOURCE:$=$)

# This target should build all the data files
ALL : GODATA  test.dat  "$(DLL_OUTPUT)\testdata.dll" "$(DLL_OUTPUT)\$(U_ICUDATA_NAME).dll" $(DLL_OUTPUT)\test1.cnv $(DLL_OUTPUT)\test3.cnv $(DLL_OUTPUT)\test4.cnv GOBACK #$(U_ICUDATA_NAME).dat
	@echo All targets are up to date

BRK_FILES = "$(ICUDATA)\sent.brk" "$(ICUDATA)\char.brk" "$(ICUDATA)\line.brk" "$(ICUDATA)\word.brk" "$(ICUDATA)\line_th.brk" "$(ICUDATA)\word_th.brk"

#invoke pkgdata
"$(DLL_OUTPUT)\$(U_ICUDATA_NAME).dll" :  $(CNV_FILES) $(BRK_FILES) uprops.dat unames.dat cnvalias.dat tz.dat $(ALL_RES) 
	@echo Building icu data
	@cd "$(ICUDBLD)"
 	"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -e icudata -v -T . -m dll -c -p $(U_ICUDATA_NAME) -O "$(PKGOPT)" -d "$(DLL_OUTPUT)" -s . <<pkgdatain.txt
uprops.dat
unames.dat
cnvalias.dat
tz.dat
$(CNV_FILES:.cnv =.cnv
)
$(RB_FILES:.res =.res
)
$(TRANSLIT_FILES:.res =.res
)
$(BRK_FILES:.brk" =.brk"
)
<<KEEP

"$(DLL_OUTPUT)\testdata.dll" :  "$(TESTDATA)\root.res" "$(TESTDATA)\te.res" "$(TESTDATA)\te_IN.res" "$(TESTDATA)\testtypes.res" test.dat
	@echo Building test data
	@copy test.dat $(TESTDATAOUT)
 	@"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -v -m dll -c -p testdata -O "$(PKGOPT)" -d "$(DLL_OUTPUT)" -T "$(TESTDATAOUT)" -s "$(TESTDATA)" <<
root.res
te.res
te_IN.res
testtypes.res
test.dat
<<

# Targets for test.dat
test.dat :
	@echo Creating data file for test: $(ICUDATA) $(ICUP)
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gentest\$(CFG)\gentest"

"$(ICUDATA)\sent.brk" : "$(ICUDATA)\sentLE.brk"
    copy "$(ICUDATA)\sentLE.brk" "$(ICUDATA)\sent.brk"

"$(ICUDATA)\char.brk" : "$(ICUDATA)\charLE.brk"
    copy "$(ICUDATA)\charLE.brk" "$(ICUDATA)\char.brk"

"$(ICUDATA)\line.brk" : "$(ICUDATA)\lineLE.brk"
    copy "$(ICUDATA)\lineLE.brk" "$(ICUDATA)\line.brk"

"$(ICUDATA)\word.brk" : "$(ICUDATA)\wordLE.brk"
    copy "$(ICUDATA)\wordLE.brk" "$(ICUDATA)\word.brk"

"$(ICUDATA)\line_th.brk" : "$(ICUDATA)\line_thLE.brk"
    copy "$(ICUDATA)\line_thLE.brk" "$(ICUDATA)\line_th.brk"

"$(ICUDATA)\word_th.brk" : "$(ICUDATA)\word_thLE.brk"
    copy "$(ICUDATA)\word_thLE.brk" "$(ICUDATA)\word_th.brk"

# utility to send us to the right dir
GODATA :
	@cd "$(ICUDBLD)"

# utility to get us back to the right dir
GOBACK :
	@cd "$(ICUDBLD)"

# This is to remove all the data files
CLEAN :
	@cd "$(ICUDBLD)"
	-@erase "*.cnv"
	-@erase "*.res"
	-@erase "$(TRANS)*.res"
	-@erase "uprops*.*"
	-@erase "unames*.*"
	-@erase "cnvalias*.*"
	-@erase "tz*.*"
	-@erase "ibm*_cnv.c"
	-@erase "*_brk.c"
	-@erase "icudt*.*"
	-@erase "*.obj"
	-@erase "sent.brk"
	-@erase "char.brk"
	-@erase "line.brk"
	-@erase "word.brk"
	-@erase "line_th.brk"
	-@erase "word_th.brk"
	-@erase "test*.*"
	-@erase "base*.*"
	@cd "$(ICUTOOLS)"

#	@cd $(TEST)
#	-@erase "*.res"

$(TESTDATA)\root.res:$(TESTDATA)\root.txt
        @echo Making Special Test Resource Bundle files
        @"$(ICUTOOLS)\genrb\$(CFG)\genrb" -s$(TESTDATA) -d$(TESTDATA) $(?F)
$(TESTDATA)\te.res:$(TESTDATA)\te.txt
        @echo Making Special Test Resource Bundle files
        @"$(ICUTOOLS)\genrb\$(CFG)\genrb" -s$(TESTDATA) -d$(TESTDATA) $(?F)
$(TESTDATA)\te_IN.res:$(TESTDATA)\te_IN.txt
        @echo Making Special Test Resource Bundle files
        @"$(ICUTOOLS)\genrb\$(CFG)\genrb" -s$(TESTDATA) -d$(TESTDATA) $(?F)
$(TESTDATA)\testtypes.res:$(TESTDATA)\testtypes.txt
        @echo Making Special Test Resource Bundle files
        @"$(ICUTOOLS)\genrb\$(CFG)\genrb" -s$(TESTDATA) -d$(TESTDATA) $(?F)


# Inference rule for creating resource bundles
#{$(TESTDATA)}.txt{$(TESTDATA)}.res:
#        @echo Making Test Resource Bundle files
#        @"$(ICUTOOLS)\genrb\$(CFG)\genrb" -s$(TESTDATA) -d$(TESTDATA) $(?F)

{$(ICUDATA)}.txt.res:
	@echo Making Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -s$(ICUDATA) -d$(@D) $(?F)

# Inference rule for creating converters, with a kludge to create
# c versions of converters at the same time
{$(ICUDATA)}.ucm.cnv::
	@echo Generating converters and c source files
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $<

# Targets for test converter data
$(DLL_OUTPUT)\test1.cnv: "$(TESTDATA)\test1.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(DLL_OUTPUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

$(DLL_OUTPUT)\test3.cnv: "$(TESTDATA)\test3.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(DLL_OUTPUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

$(DLL_OUTPUT)\test4.cnv: "$(TESTDATA)\test4.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(DLL_OUTPUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

# Targets for unames.dat
unames.dat: {"$(ICUDATA)"}\unidata\UnicodeData.txt "$(ICUTOOLS)\gennames\$(CFG)\gennames.exe"
	@echo Creating data file for Unicode Names
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gennames\$(CFG)\gennames" $(ICUDATA)\unidata\UnicodeData.txt

# Targets for uprops.dat
uprops.dat: "$(ICUDATA)\unidata\UnicodeData.txt" "$(ICUTOOLS)\genprops\$(CFG)\genprops.exe"
	@echo Creating data file for Unicode Character Properties
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\genprops\$(CFG)\genprops" -s "$(ICUDATA)\unidata"

# Targets for converters
cnvalias.dat : {"$(ICUDATA)"}\convrtrs.txt "$(ICUTOOLS)\gencnval\$(CFG)\gencnval.exe"
	@echo Creating data file for Converter Aliases
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gencnval\$(CFG)\gencnval" $(ICUDATA)\convrtrs.txt

# Targets for tz
tz.dat : {"$(ICUDATA)"}timezone.txt {"$(ICUTOOLS)\gentz\$(CFG)"}gentz.exe
	@echo Creating data file for Timezones
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gentz\$(CFG)\gentz" "$(ICUDATA)\timezone.txt"

# Dependencies on the tools
convrtrs.txt : {"$(ICUTOOLS)\gencnval\$(CFG)"}gencnval.exe

tz.txt : {"$(ICUTOOLS)\gentz\$(CFG)"}gentz.exe

uprops.dat unames.dat cnvalias.dat tz.dat : {"$(ICUTOOLS)\genccode\$(CFG)"}genccode.exe

$(TRANSLIT_SOURCE) $(GENRB_SOURCE) : {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe

$(UCM_SOURCE) : {"$(ICUTOOLS)\makeconv\$(CFG)"}makeconv.exe {"$(ICUTOOLS)\genccode\$(CFG)"}genccode.exe

test.dat : {"$(ICUTOOLS)\gentest\$(CFG)"}gentest.exe

