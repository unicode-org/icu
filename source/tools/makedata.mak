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
ICUDATA=$(ICUP)\icu\data
ICU_DATA=$(ICUDATA)\
DATA_PATH=$(ICUP)\icu\data^\
TRANS=translit^\
TEST=..\source\test\testdata^\
ICUTOOLS=$(ICUP)\icu\source\tools
!ENDIF

LINK32 = link.exe
LINK32_FLAGS = /out:"$(ICUDATA)/icudata.dll" /DLL /NOENTRY /base:"0x4ad00000" /comment:" Copyright (C) 1999 International Business Machines Corporation and others.  All Rights Reserved. "
CPP_FLAGS = /I$(ICUP)\icu\include /GD /c

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

# This appears in original Microsofts makefiles
!IF "$(OS)" == "Windows_NT"
NULL=
!ELSE
NULL=nul
!ENDIF

PATH = $(PATH);$(ICUP)\icu\bin\$(CFG)

# Suffixes for data files
.SUFFIXES : .ucm .cnv .dll .dat .col .res .txt .c

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

# Read list of resource bundle files for colation
!IF EXISTS("$(ICUTOOLS)\gencol\gencolfiles.mk")
!INCLUDE "$(ICUTOOLS)\gencol\gencolfiles.mk"
!IF EXISTS("$(ICUTOOLS)\gencol\gencollocal.mk")
!INCLUDE "$(ICUTOOLS)\gencol\gencollocal.mk"
GENCOL_SOURCE=$(GENCOL_SOURCE) $(GENCOL_SOURCE_LOCAL)
!ELSE
#!MESSAGE Warning: cannot find "gencollocal.mk"
!ENDIF
!ELSE
!ERROR ERROR: cannot find "gencolfiles.mk"
!ENDIF
COL_FILES = $(GENCOL_SOURCE:.txt=.col)


# This target should build all the data files
ALL : GODATA $(RB_FILES) $(CNV_FILES) $(COL_FILES) icudata.dll icudata.dat GOBACK
	@echo All targets are up to date

BRK_FILES = $(ICUDATA)\sent.brk $(ICUDATA)\char.brk $(ICUDATA)\line.brk $(ICUDATA)\word.brk $(ICUDATA)\line_th.brk $(ICUDATA)\word_th.brk
BRK_CSOURCES = $(BRK_FILES:.brk=_brk.c)

CPP_SOURCES = $(C_CNV_FILES) unames_dat.c cnvalias_dat.c tz_dat.c $(BRK_CSOURCES)
LINK32_OBJS = $(CPP_SOURCES:.c=.obj)

# target for DLL
icudata.dll : $(LINK32_OBJS) $(CNV_FILES)
	@echo Creating DLL file
	@cd $(ICUDATA)
	@$(LINK32) @<<
$(LINK32_FLAGS) $(LINK32_OBJS)
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

# target for memory mapped file
icudata.dat : $(CNV_FILES) unames.dat cnvalias.dat tz.dat
	@echo Creating memory-mapped file
	@cd $(ICUDATA)
 	@$(ICUTOOLS)\gencmn\$(CFG)\gencmn 1000000 <<
$(ICUDATA)\unames.dat
$(ICUDATA)\cnvalias.dat
$(ICUDATA)\tz.dat
$(ICUDATA)\sent.brk
$(ICUDATA)\char.brk
$(ICUDATA)\line.brk
$(ICUDATA)\word.brk
$(ICUDATA)\line_th.brk
$(ICUDATA)\word_th.brk
$(CNV_FILES:.cnv =.cnv
)
<<

# nothing works without this target, but we're making
# these files while creating converters
$(C_CNV_FILES) : $(CNV_FILES)
	@$(ICUTOOLS)\genccode\$(CFG)\genccode $(CNV_FILES)

# nothing works without this target, but we're making
# these files while creating converters
$(BRK_CSOURCES) : $(BRK_FILES)
	@$(ICUTOOLS)\genccode\$(CFG)\genccode $(BRK_FILES)

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
	-@erase "*.col"
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
	@cd $(TEST)
	-@erase "*.res"
	@cd $(ICUTOOLS)

# Inference rule for creating resource bundles
.txt.res::
	@echo Making Resource Bundle files
	@cd $(ICUDATA)
	@$(ICUTOOLS)\genrb\$(CFG)\genrb $<

# Inference rule for creating converters, with a kludge to create
# c versions of converters at the same time
.ucm.cnv::
	@echo Generating converters and c source files
	@cd $(ICUDATA)
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\makeconv\$(CFG)\makeconv $<
#	@$(ICUTOOLS)\genccode\$(CFG)\genccode $(CNV_FILES)

# Inference rule for creating collation files -
# this should be integrated in genrb
.txt.col::
	@echo Making Collation files
	@cd $(ICUDATA)
	@$(ICUTOOLS)\genrb\$(CFG)\genrb $<

# Inference rule for compiling :)
.c.obj::
	@cd $(ICUDATA)
	@$(CPP) @<<
$(CPP_FLAGS) $<
<<

# Targets for unames.dat
unames.dat : UnicodeData-3.0.0.txt
	@echo Creating data file for Unicode Names
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\gennames\$(CFG)\gennames -v- -c- UnicodeData-3.0.0.txt

unames_dat.c : unames.dat
	@echo Creating C source file for Unicode Names
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\genccode\$(CFG)\genccode $(ICUDATA)\$?

# Targets for converters
cnvalias.dat : convrtrs.txt
	@echo Creating data file for Converter Aliases
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\gencnval\$(CFG)\gencnval -c-

cnvalias_dat.c : cnvalias.dat
	@echo Creating C source file for Converter Aliases
	@$(ICUTOOLS)\genccode\$(CFG)\genccode $(ICUDATA)\$?

# Targets for tz
tz.dat : {$(ICUTOOLS)\gentz}tz.txt
	@echo Creating data file for Timezones
	@set ICU_DATA=$(ICUDATA)
	@$(ICUTOOLS)\gentz\$(CFG)\gentz -c- $(ICUTOOLS)\gentz\tz.txt

tz_dat.c : tz.dat
	@echo Creating C source file for Timezones
	@$(ICUTOOLS)\genccode\$(CFG)\genccode $(ICUDATA)\$?

# Dependencies on the tools
UnicodeData-3.0.0.txt : {$(ICUTOOLS)\gennames\$(CFG)}gennames.exe

convrtrs.txt : {$(ICUTOOLS)\gencnval\$(CFG)}gencnval.exe

tz.txt : {$(ICUTOOLS)\gentz\$(CFG)}gentz.exe

unames.dat cnvalias.dat tz.dat : {$(ICUTOOLS)\genccode\$(CFG)}genccode.exe

$(GENRB_SOURCE) $(GENCOL_SOURCE) : {$(ICUTOOLS)\genrb\$(CFG)}genrb.exe

$(UCM_SOURCE) : {$(ICUTOOLS)\makeconv\$(CFG)}makeconv.exe {$(ICUTOOLS)\genccode\$(CFG)}genccode.exe
