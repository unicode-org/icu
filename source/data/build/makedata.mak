#**********************************************************************
#* Copyright (C) 1999-2001, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak icup=<path_to_icu_instalation> [Debug|Release]
#
#	12/10/1999	weiv	Created

U_ICUDATA_NAME=icudt18l

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
UCM_SOURCE=$(UCM_SOURCE)
!IF EXISTS("$(ICUDATA)\ucmebcdic.mk")
!INCLUDE "$(ICUDATA)\ucmebcdic.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_EBCDIC) 
!IF EXISTS("$(ICUDATA)\ucmlocal.mk")
!INCLUDE "$(ICUDATA)\ucmlocal.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_EBCDIC) $(UCM_SOURCE_LOCAL)
!ELSE
#!MESSAGE Warning: cannot find "ucmlocal.mk"
!ENDIF
!ELSE
!MESSAGE Warning: cannot find "ucmebcdic.mk".Not building EBCDIC converter files
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
ALL : GODATA  testdata "$(DLL_OUTPUT)\$(U_ICUDATA_NAME).dll" $(DLL_OUTPUT)\test1.cnv $(DLL_OUTPUT)\test3.cnv $(DLL_OUTPUT)\test4.cnv GOBACK #$(U_ICUDATA_NAME).dat
	@echo All targets are up to date

testdata: ucadata.dat $(RB_FILES) {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe
	@cd "$(TESTDATA)"
	nmake /nologo /f $(TESTDATA)\testdata.mk TESTDATA=$(TESTDATA) ICUTOOLS=$(ICUTOOLS) PKGOPT=$(PKGOPT) CFG=$(CFG) DLL_OUTPUT=$(DLL_OUTPUT) TESTDATAOUT=$(TESTDATAOUT)
	@cd "$(ICUDBLD)"


BRK_FILES = "$(ICUDBLD)\sent.brk" "$(ICUDBLD)\char.brk" "$(ICUDBLD)\line.brk" "$(ICUDBLD)\word.brk" "$(ICUDBLD)\line_th.brk" "$(ICUDBLD)\word_th.brk"

#invoke pkgdata
"$(DLL_OUTPUT)\$(U_ICUDATA_NAME).dll" :  $(CNV_FILES) $(BRK_FILES) qchk.dat fchk.dat uprops.dat unames.dat unorm.dat cnvalias.dat tz.dat ucadata.dat invuca.dat $(ALL_RES) icudata.res
	@echo Building icu data
	@cd "$(ICUDBLD)"
 	"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -e icudata -v -T . -m dll -c -p $(U_ICUDATA_NAME) -O "$(PKGOPT)" -d "$(DLL_OUTPUT)" -s . <<pkgdatain.txt
qchk.dat
fchk.dat
uprops.dat
unames.dat
unorm.dat
cnvalias.dat
tz.dat
ucadata.dat
invuca.dat
$(CNV_FILES:.cnv =.cnv
)
$(RB_FILES:.res =.res
)
$(TRANSLIT_FILES:.res =.res
)
$(BRK_FILES:.brk" =.brk"
)
<<KEEP


"$(ICUDBLD)\sent.brk" : "$(ICUDATA)\sentLE.brk"
    copy "$(ICUDATA)\sentLE.brk" "$(ICUDBLD)\sent.brk"

"$(ICUDBLD)\char.brk" : "$(ICUDATA)\charLE.brk"
    copy "$(ICUDATA)\charLE.brk" "$(ICUDBLD)\char.brk"

"$(ICUDBLD)\line.brk" : "$(ICUDATA)\lineLE.brk"
    copy "$(ICUDATA)\lineLE.brk" "$(ICUDBLD)\line.brk"

"$(ICUDBLD)\word.brk" : "$(ICUDATA)\wordLE.brk"
    copy "$(ICUDATA)\wordLE.brk" "$(ICUDBLD)\word.brk"

"$(ICUDBLD)\line_th.brk" : "$(ICUDATA)\line_thLE.brk"
    copy "$(ICUDATA)\line_thLE.brk" "$(ICUDBLD)\line_th.brk"

"$(ICUDBLD)\word_th.brk" : "$(ICUDATA)\word_thLE.brk"
    copy "$(ICUDATA)\word_thLE.brk" "$(ICUDBLD)\word_th.brk"

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
	-@erase "qchk*.*"
	-@erase "fchk*.*"
	-@erase "uprops*.*"
	-@erase "unames*.*"
	-@erase "unorm*.*"
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
	@cd "$(DLL_OUTPUT)"
	-@erase "*.cnv"
	@cd "$(TESTDATA)"
	-@erase "*.res"
	@cd "$(ICUTOOLS)"


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

# DLL version information
icudata.res: "$(ICUDATA)\icudata.rc"
	@echo Creating data DLL version information from $**
	@rc.exe /i ..\..\..\include\ /r /fo "$@" $**

# Targets for qchk.dat
qchk.dat: "$(ICUDATA)\unidata\QuickCheck.txt" "$(ICUTOOLS)\genqchk\$(CFG)\genqchk.exe"
	@echo Creating data file for Quick Check Properties
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\genqchk\$(CFG)\genqchk" -s "$(ICUDATA)\unidata"

# Targets for fchk.dat
fchk.dat: "$(ICUDATA)\unidata\FCDCheck.txt" "$(ICUTOOLS)\genfchk\$(CFG)\genfchk.exe"
	@echo Creating data file for FCD Check Properties
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\genfchk\$(CFG)\genfchk" -s "$(ICUDATA)\unidata"

# Targets for unames.dat
unames.dat: {"$(ICUDATA)"}\unidata\UnicodeData.txt "$(ICUTOOLS)\gennames\$(CFG)\gennames.exe"
	@echo Creating data file for Unicode Names
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gennames\$(CFG)\gennames" -1 $(ICUDATA)\unidata\UnicodeData.txt

# Targets for uprops.dat
uprops.dat: "$(ICUDATA)\unidata\UnicodeData.txt" "$(ICUTOOLS)\genprops\$(CFG)\genprops.exe"
	@echo Creating data file for Unicode Character Properties
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\genprops\$(CFG)\genprops" -s "$(ICUDATA)\unidata"

# Targets for unorm.dat
unorm.dat: "$(ICUDATA)\unidata\UnicodeData.txt" "$(ICUDATA)\unidata\DerivedNormalizationProperties.txt" "$(ICUTOOLS)\gennorm\$(CFG)\gennorm.exe"
	@echo Creating data file for Unicode Normalization
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gennorm\$(CFG)\gennorm" -s "$(ICUDATA)\unidata"

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

# Targets for ucadata.dat & invuca.dat
ucadata.dat: "$(ICUDATA)\unidata\FractionalUCA.txt" "$(ICUTOOLS)\genuca\$(CFG)\genuca.exe"
	@echo Creating UCA data files
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\genuca\$(CFG)\genuca" -s "$(ICUDATA)\unidata"

invuca.dat: ucadata.dat

{"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe : ucadata.dat qchk.dat fchk.dat uprops.dat unorm.dat

ucadata.dat : uprops.dat qchk.dat fchk.dat unorm.dat

# Dependencies on the tools
convrtrs.txt : {"$(ICUTOOLS)\gencnval\$(CFG)"}gencnval.exe

tz.txt : {"$(ICUTOOLS)\gentz\$(CFG)"}gentz.exe

uprops.dat unames.dat unorm.dat cnvalias.dat tz.dat ucadata.dat invuca.dat: {"$(ICUTOOLS)\genccode\$(CFG)"}genccode.exe


$(TRANSLIT_SOURCE) $(GENRB_SOURCE) : {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe ucadata.dat qchk.dat fchk.dat uprops.dat unorm.dat

$(UCM_SOURCE) : {"$(ICUTOOLS)\makeconv\$(CFG)"}makeconv.exe {"$(ICUTOOLS)\genccode\$(CFG)"}genccode.exe
