#**********************************************************************
#* Copyright (C) 1999-2000, International Business Machines Corporation 
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak icup=<path_to_icu_instalation> [Debug|Release]
#
#	12/10/1999	weiv	Created

!CMDSWITCHES -D

#If no config, we default to debug
!IF "$(CFG)" == ""
CFG=Debug
!MESSAGE No configuration specified. Defaulting to common - Win32 Debug.
!ENDIF

#Let's see if user has given us a path to ICU
#This could be found according to the path to makefile, but for now it is this way
!IF "$(ICUP)"==""
!ERROR Can't find path!
!ELSE
ICUDATA=$(ICUP)\data
ICD=$(ICUDATA)^\
ICU_DATA=$(ICUDATA)\
DATA_PATH=$(ICUP)\data^\
TEST=..\source\test\testdata^\
TESTDATA=$(ICUP)\source\test\testdata
ICUTOOLS=$(ICUP)\source\tools
!ENDIF

LINK32 = link.exe
LINK32_FLAGS = /out:"$(ICUDATA)/icudata.dll" /DLL /NOENTRY /base:"0x4ad00000" /comment:" Copyright (C) 1999-2000 International Business Machines Corporation and others.  All Rights Reserved. "
#CPP_FLAGS = /I$(ICUP)\include /GD /c
CPP_FLAGS = /I$(ICUP)\include /GD /c /Fo$@

#Here we test if configuration is given
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

!IF "$(CFG)" == "Debug"
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

PATH = $(PATH);$(ICUP)\bin\$(CFG)

# Suffixes for data files
.SUFFIXES : .ucm .cnv .dll .dat .res .txt .c

# We're including a list of ucm files. There are two lists, one is essential 'ucmfiles.mk' and
# the other is optional 'ucmlocal.mk'
!IF EXISTS("$(ICUTOOLS)\makeconv\ucmfiles.mk")
!INCLUDE "$(ICUTOOLS)\makeconv\ucmfiles.mk"
!IF EXISTS("$(ICUTOOLS)\makeconv\ucmlocal.mk")
!INCLUDE "$(ICUTOOLS)\makeconv\ucmlocal.mk"
$(UCM_SOURCE)=$(UCM_SOURCE) $(UCM_SOURCE_LOCAL)
!ELSE
#!MESSAGE Warning: cannot find "ucmlocal.mk"
!ENDIF
!ELSE
!ERROR ERROR: cannot find "ucmfiles.mk"
!ENDIF

# According to the read files, we will generate CNV and C files
CNV_FILES=$(UCM_SOURCE:.ucm=.cnv)
C_CNV_FILES = $(UCM_SOURCE:.ucm=_cnv.c)
OBJ_CNV_FILES = $(C_CNV_FILES:.c=.obj)

# Read list of resource bundle files
!IF EXISTS("$(ICUTOOLS)\genrb\genrbfiles.mk")
!INCLUDE "$(ICUTOOLS)\genrb\genrbfiles.mk"
!IF EXISTS("$(ICUTOOLS)\genrb\genrblocal.mk")
!INCLUDE "$(ICUTOOLS)\genrb\genrblocal.mk"
GENRB_SOURCE=$(GENRB_SOURCE) $(GENRB_SOURCE_LOCAL)
!ELSE
#!MESSAGE Warning: cannot find "genrblocal.mk"
!ENDIF
!ELSE
!ERROR ERROR: cannot find "genrbfiles.mk"
!ENDIF
RB_FILES = $(GENRB_SOURCE:.txt=.res) 
TRANSLIT_FILES = $(TRANSLIT_SOURCE:.txt=.res)
#TRANSLIT_SOURCE = $(TRANSLIT_SOURCE: = translit\)
C_RB_FILES = $(RB_FILES:.res=_res.c) $(TRANSLIT_FILES:.res=_res.c) 
OBJ_RB_FILES = $(C_RB_FILES:.c=.obj)

# This target should build all the data files
ALL : GODATA  test.dat base_test.dat test_dat.dll base_test_dat.dll base_dat.dll $(TESTDATA)\testdata.dll icudata.dll GOBACK #icudata.dat 
	@echo All targets are up to date

BRK_FILES = $(ICUDATA)\sent.brk $(ICUDATA)\char.brk $(ICUDATA)\line.brk $(ICUDATA)\word.brk $(ICUDATA)\line_th.brk $(ICUDATA)\word_th.brk
BRK_CSOURCES = $(BRK_FILES:.brk=_brk.c)

CPP_SOURCES = $(C_CNV_FILES) uprops_dat.c unames_dat.c cnvalias_dat.c tz_dat.c $(BRK_CSOURCES) $(C_RB_FILES)
LINK32_OBJS = $(CPP_SOURCES:.c=.obj)

# target for DLL

LINK32_TEST_FLAGS = /out:"$(ICUDATA)/test_dat.dll" /DLL /NOENTRY 
LINK32_BASE_TEST_FLAGS = /out:"$(ICUDATA)/base_test_dat.dll" /DLL /NOENTRY 
LINK32_BASE_FLAGS = /out:"$(ICUDATA)/base_dat.dll" /DLL /NOENTRY 
LINK32_TESTDATA_FLAGS = /out:"$(TESTDATA)/testdata.dll" /DLL /NOENTRY

#invoke pkgdata
icudata.dll :  $(CNV_FILES) $(BRK_FILES) uprops.dat unames.dat cnvalias.dat tz.dat $(RB_FILES) $(TRANSLIT_FILES)
	@echo Building icu data
 	@$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata -v -m dll -c -p icudata -O $(PKGOPT) -d $(ICUDATA) -s $(ICUDATA) <<
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
$(BRK_FILES:.brk =.brk
)
<<

$(TESTDATA)\testdata.dll :  $(TESTDATA)\root.res $(TESTDATA)\te.res $(TESTDATA)\te_IN.res $(TESTDATA)\testtypes.res
	@echo Building test data
 	@$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata -v -m dll -c -p testdata -O $(PKGOPT) -d $(TESTDATA) -s $(TESTDATA) <<
root.res
te.res
te_IN.res
testtypes.res
<<      


# Targets for test.dat 
test.dat : 
	@echo Creating data file for test
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\gentest\$(CFG)\gentest 
test_dat.c : test.dat
	@echo Creating C source file for test data
        @set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\genccode\$(CFG)\genccode $(ICUDATA)\$?
test_dat.obj : test_dat.c
        @echo creating the obj file for test data
	@cd $(ICUDATA)
	@$(CPP) @<<
$(CPP_FLAGS) $(ICUDATA)\$?
<<


#Targets for base_test.dat
base_test.dat :
	@echo Creating base data file test
	@set ICU_DATA=$(ICUDATA)
	@copy $(ICUDATA)\test.dat $(ICUDATA)\base_test.dat 

# According to the read files, we will generate C files
# Target for test DLL
test_dat.dll : test_dat.obj test.dat
	@echo Creating DLL file
	@cd $(ICUDATA)
	@$(LINK32) @<<
$(LINK32_TEST_FLAGS) test_dat.obj
<<

#Target for base test data DLL
base_test_dat.dll : test_dat.obj test.dat
	@echo Creating DLL file
	@cd $(ICUDATA)
	@$(LINK32) @<<
$(LINK32_BASE_TEST_FLAGS) test_dat.obj
<<

#Target for base data DLL
base_dat.dll : test_dat.obj test.dat
	@echo Creating DLL file
	@cd $(ICUDATA)
	@$(LINK32) @<<
$(LINK32_BASE_FLAGS) test_dat.obj
<<



$(ICUDATA)\sent.brk : $(ICUDATA)\sentLE.brk
    copy $(ICUDATA)\sentLE.brk $(ICUDATA)\sent.brk

$(ICUDATA)\char.brk : $(ICUDATA)\charLE.brk
    copy $(ICUDATA)\charLE.brk $(ICUDATA)\char.brk

$(ICUDATA)\line.brk : $(ICUDATA)\lineLE.brk
    copy $(ICUDATA)\lineLE.brk $(ICUDATA)\line.brk

$(ICUDATA)\word.brk : $(ICUDATA)\wordLE.brk
    copy $(ICUDATA)\wordLE.brk $(ICUDATA)\word.brk

$(ICUDATA)\line_th.brk : $(ICUDATA)\line_thLE.brk
    copy $(ICUDATA)\line_thLE.brk $(ICUDATA)\line_th.brk

$(ICUDATA)\word_th.brk : $(ICUDATA)\word_thLE.brk
    copy $(ICUDATA)\word_thLE.brk $(ICUDATA)\word_th.brk

# utility to send us to the right dir
GODATA :
	@cd $(ICUDATA)

# utility to get us back to the right dir
GOBACK :
	@cd $(ICUTOOLS)

# This is to remove all the data files
CLEAN :
	@cd $(ICUDATA)
	-@erase "*.cnv"
	-@erase "*.res"
	-@erase "$(TRANS)*.res"
	-@erase "uprops*.*"
	-@erase "unames*.*"
	-@erase "cnvalias*.*"
	-@erase "tz*.*"
	-@erase "ibm*_cnv.c"
	-@erase "*_brk.c"
	-@erase "icudata.*"
	-@erase "*.obj"
	-@erase "sent.brk"
	-@erase "char.brk"
	-@erase "line.brk"
	-@erase "word.brk"
	-@erase "line_th.brk"
	-@erase "word_th.brk"
	-@erase "test*.*"
	-@erase "base*.*"
	@cd $(TEST)
	-@erase "*.res"
	@cd $(ICUTOOLS)

# Inference rule for creating resource bundles
.txt.res:
	@echo Making Resource Bundle files
	@echo cd $(ICUDATA)
	@echo set ICU_DATA=$(ICUDATA)
	$(ICUTOOLS)\genrb\$(CFG)\genrb -s$(@D) -d$(@D) $(?F)

# Inference rule for creating converters, with a kludge to create
# c versions of converters at the same time
.ucm.cnv::
	@echo Generating converters and c source files
	@cd $(ICUDATA)
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\makeconv\$(CFG)\makeconv $<

# Inference rule for compiling :)
.c.obj:
	@cd $(ICUDATA)
	@$(CPP) @<<
$(CPP_FLAGS) $?
<<

# Targets for uprops.dat
uprops.dat : unidata\UnicodeData.txt unidata\Mirror.txt $(ICUTOOLS)\genprops\$(CFG)\genprops.exe
	@echo Creating data file for Unicode Character Properties
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\genprops\$(CFG)\genprops -s $(ICUDATA)\unidata

# Targets for unames.dat
unames.dat : unidata\UnicodeData.txt $(ICUTOOLS)\gennames\$(CFG)\gennames.exe
	@echo Creating data file for Unicode Names
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\gennames\$(CFG)\gennames unidata\UnicodeData.txt

# Targets for converters
cnvalias.dat : convrtrs.txt $(ICUTOOLS)\gencnval\$(CFG)\gencnval.exe
	@echo Creating data file for Converter Aliases
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\gencnval\$(CFG)\gencnval

# Targets for tz
tz.dat : {$(ICUTOOLS)\gentz}tz.txt {$(ICUTOOLS)\gentz\$(CFG)}gentz.exe
	@echo Creating data file for Timezones
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\gentz\$(CFG)\gentz $(ICUTOOLS)\gentz\tz.txt

# Dependencies on the tools
convrtrs.txt : {$(ICUTOOLS)\gencnval\$(CFG)}gencnval.exe

tz.txt : {$(ICUTOOLS)\gentz\$(CFG)}gentz.exe

uprops.dat unames.dat cnvalias.dat tz.dat : {$(ICUTOOLS)\genccode\$(CFG)}genccode.exe

$(GENRB_SOURCE) : {$(ICUTOOLS)\genrb\$(CFG)}genrb.exe

$(UCM_SOURCE) : {$(ICUTOOLS)\makeconv\$(CFG)}makeconv.exe {$(ICUTOOLS)\genccode\$(CFG)}genccode.exe

test.dat : {$(ICUTOOLS)\gentest\$(CFG)}gentest.exe

