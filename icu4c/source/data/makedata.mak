#**********************************************************************
#* Copyright (C) 1999-2001, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak [Debug|Release]
#
#	12/10/1999	weiv	Created

U_ICUDATA_NAME=icudt19
U_ICUDATA_ENDIAN_SUFFIX=l

#  ICUDBLD
#     Must be provided by whoever runs this makefile.
#     Is the directory containing this file (makedata.mak)
#     Is the directory into which most data is built (prior to packaging)
#     Is icu\source\data\build
#
!IF "$(ICUDBLD)"==""
!ERROR Can't find ICUDBLD (ICU Data Build dir, should point to icu\source\data\build\ )!
!ENDIF
!MESSAGE icu data build path is $(ICUDBLD)


#  ICUP
#     The root of the ICU source directory tree
#
ICUP=$(ICUDBLD)\..\..\..


#
#  ICUDATA
#     The source directory.  Contains the source files for the common data to be built.
#     WARNING:  NOT THE SAME AS ICU_DATA environment variable.  Confusing.
ICUDATA=$(ICUP)\data


#
#  DLL_OUTPUT
#      Destination directory for the common data DLL file.
#      This is the same place that all of the other ICU DLLs go (the code-containing DLLs)
#      The lib file for the data DLL goes in $(DLL_OUTPUT)/../lib/
#
DLL_OUTPUT=$(ICUP)\bin

#
#  TESTDATA
#     The source directory for data needed for test programs.
TESTDATA=$(ICUP)\source\test\testdata

#
#   TESTDATAOUT
#      The destination directory for the built test data .dat file
#         When running the tests, ICU_DATA environment variable is set to here
#         so that test data files can be loaded.  (Tests are NOT run from this makefile,
#         only the data is put in place.)
TESTDATAOUT=$(ICUP)\source\data


#
#   ICUTOOLS
#       Directory under which all of the ICU data building tools live.
#
ICUTOOLS=$(ICUP)\source\tools


PATH = $(PATH);$(ICUP)\bin


# We have to prepare params for pkgdata - to help it find the tools
!IF "$(CFG)" == "Debug" || "$(CFG)" == "debug"
!MESSAGE makedata.mak: doing a Debug build.
PKGOPT=D:$(ICUP)
!ELSE
!MESSAGE makedata.mak: doing a Release build.
PKGOPT=R:$(ICUP)
!ENDIF


# Suffixes for data files
.SUFFIXES : .ucm .cnv .dll .dat .res .txt .c

# We're including a list of ucm files. There are two lists, one is essential 'ucmfiles.mk' and
# the other is optional 'ucmlocal.mk'
!IF EXISTS("$(ICUDATA)\ucmfiles.mk")
!INCLUDE "$(ICUDATA)\ucmfiles.mk"
UCM_SOURCE=$(UCM_SOURCE)
!ELSE
!ERROR ERROR: cannot find "ucmfiles.mk"
!ENDIF

!IF EXISTS("$(ICUDATA)\ucmebcdic.mk")
!INCLUDE "$(ICUDATA)\ucmebcdic.mk"
!ELSE
!MESSAGE Warning: cannot find "ucmebcdic.mk".Not building EBCDIC converter files
!ENDIF

!IF EXISTS("$(ICUDATA)\ucmlocal.mk")
!INCLUDE "$(ICUDATA)\ucmlocal.mk"
!IFDEF UCM_SOURCE_LOCAL
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_LOCAL)
!ENDIF
!ELSE
#!MESSAGE Warning: cannot find "ucmlocal.mk"
!ENDIF

# Note that UCM_SOURCE_EBCDIC could be defined in either of ucmlocal.mk or ucmebcdic.mk.
# Note also that subsequent dependency rules fail if there are leading spaces on UCM_SOURCE,
#      hence the contorted logic here.
!IF ("$(UCM_SOURCE_EBCDIC)" != "")
!IF ("$(UCM_SOURCE)" == "")
UCM_SOURCE=$(UCM_SOURCE_EBCDIC)
!ELSE
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_EBCDIC)
!ENDIF
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

#############################################################################
#
# ALL  
#     This target builds all the data files.  The world starts here.
#			Note: we really want the common data dll to go to $(DLL_OUTPUT), not $(ICUBLD).  But specifying
#				that here seems to cause confusion with the building of the stub library of the same name.
#				Building the common dll in $(ICUBLD) unconditionally copies it to $(DLL_OUTPUT) too.
#
#############################################################################
ALL : GODATA  testdata "$(ICUDBLD)\$(U_ICUDATA_NAME).dll" $(TESTDATAOUT)\test1.cnv $(TESTDATAOUT)\test3.cnv $(TESTDATAOUT)\test4.cnv 
	@echo All targets are up to date

#
# testdata - nmake will invoke pkgdata, which will create testdata.dat 
#
testdata: ucadata.dat $(RB_FILES) {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe
	@cd "$(TESTDATA)"
	nmake /nologo /f $(TESTDATA)\testdata.mk TESTDATA=$(TESTDATA) ICUTOOLS=$(ICUTOOLS) PKGOPT=$(PKGOPT) CFG=$(CFG) TESTDATAOUT=$(TESTDATAOUT) ICUDATA=$(ICUDATA)
	@cd "$(ICUDBLD)"


BRK_FILES = "$(ICUDBLD)\sent.brk" "$(ICUDBLD)\char.brk" "$(ICUDBLD)\line.brk" "$(ICUDBLD)\word.brk" "$(ICUDBLD)\line_th.brk" "$(ICUDBLD)\word_th.brk"

#invoke pkgdata for ICU common data
#  pkgdata will drop all output files (.dat, .dll, .lib) into the target (ICUDBLD) directory.
#  move the .dll and .lib files to their final destination afterwards.
#
"$(ICUDBLD)\$(U_ICUDATA_NAME).dll" :  $(CNV_FILES) $(BRK_FILES) uprops.dat unames.dat unorm.dat cnvalias.dat tz.dat ucadata.dat invuca.dat $(ALL_RES) icudata.res $(ICUP)\source\stubdata\stubdatabuilt.txt
	@echo Building icu data
	@cd "$(ICUDBLD)"
 	"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -e $(U_ICUDATA_NAME) -v -m dll -c -p $(U_ICUDATA_NAME) -O "$(PKGOPT)" -d "$(ICUDBLD)" -s . <<pkgdatain.txt
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
	copy "$(U_ICUDATA_NAME).dll" "$(DLL_OUTPUT)"
	-@erase "$(U_ICUDATA_NAME).dll"
	copy "$(U_ICUDATA_NAME).dat" "..\$(U_ICUDATA_NAME)$(U_ICUDATA_ENDIAN_SUFFIX).dat"
	-@erase "$(U_ICUDATA_NAME).dat"




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

# utility target to send us to the right dir
GODATA :
	@cd "$(ICUDBLD)"


# This is to remove all the data files
CLEAN :
	@echo Cleaning up the data files.
	@cd "$(ICUDBLD)"
	-@erase "*.cnv"
	-@erase "*.res"
	-@erase "*.obj"
	-@erase "*.brk"
	-@erase "*.dat"
	@cd "$(TESTDATAOUT)"
	-@erase "*.dat"
	-@erase "*.cnv"
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
$(TESTDATAOUT)\test1.cnv: "$(TESTDATA)\test1.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(TESTDATAOUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

$(TESTDATAOUT)\test3.cnv: "$(TESTDATA)\test3.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(TESTDATAOUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

$(TESTDATAOUT)\test4.cnv: "$(TESTDATA)\test4.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(TESTDATAOUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

# DLL version information
icudata.res: "$(ICUDATA)\icudata.rc"
	@echo Creating data DLL version information from $**
	@rc.exe /i ..\..\..\include\ /r /fo "$@" $**

# Targets for unames.dat
unames.dat: {"$(ICUDATA)"}\unidata\UnicodeData.txt "$(ICUTOOLS)\gennames\$(CFG)\gennames.exe"
	@echo Creating data file for Unicode Names
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gennames\$(CFG)\gennames" -1 $(ICUDATA)\unidata\UnicodeData.txt

# Targets for uprops.dat
uprops.dat: "$(ICUDATA)\unidata\UnicodeData.txt" "$(ICUTOOLS)\genprops\$(CFG)\genprops.exe" "$(DLL_OUTPUT)\$(U_ICUDATA_NAME).dll"
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

{"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe : ucadata.dat uprops.dat unorm.dat

ucadata.dat : uprops.dat unorm.dat

# Dependencies on the tools
convrtrs.txt : {"$(ICUTOOLS)\gencnval\$(CFG)"}gencnval.exe

tz.txt : {"$(ICUTOOLS)\gentz\$(CFG)"}gentz.exe

uprops.dat unames.dat unorm.dat cnvalias.dat tz.dat ucadata.dat invuca.dat: {"$(ICUTOOLS)\genccode\$(CFG)"}genccode.exe


$(TRANSLIT_SOURCE) $(GENRB_SOURCE) : {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe ucadata.dat uprops.dat unorm.dat

$(UCM_SOURCE) : {"$(ICUTOOLS)\makeconv\$(CFG)"}makeconv.exe {"$(ICUTOOLS)\genccode\$(CFG)"}genccode.exe
