#**********************************************************************
#* Copyright (C) 1999-2001, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak [Debug|Release]
#
#	12/10/1999	weiv	Created

U_ICUDATA_NAME=icudt20
U_ICUDATA_ENDIAN_SUFFIX=l
UNICODE_VERSION=3.1.1

#  ICUMAKE
#     Must be provided by whoever runs this makefile.
#     Is the directory containing this file (makedata.mak)
#     Is the directory into which most data is built (prior to packaging)
#     Is icu\source\data\build
#
!IF "$(ICUMAKE)"==""
!ERROR Can't find ICUMAKE (ICU Data Make dir, should point to icu\source\data\ )!
!ENDIF
!MESSAGE ICU data make path is $(ICUMAKE)

ICUDBLD=$(ICUMAKE)\out\build
ICUOUT=$(ICUMAKE)\out


#  ICUP
#     The root of the ICU source directory tree
#
ICUP=$(ICUMAKE)\..\..
ICUP=$(ICUP:\source\data\..\..=)
!MESSAGE ICU root path is $(ICUP)


#  ICUSRCDATA
#       The data directory in source
#
ICUSRCDATA=$(ICUP)\source\data

#  ICUUCM
#       The directory that contains ucmcore.mk files along with *.ucm files
#
ICUUCM=$(ICUP)\source\data\mappings

#  ICULOC
#       The directory that contains resfiles.mk files along with *.txt locale data files
#
ICULOC=$(ICUP)\source\data\locales
!MESSAGE ICULOC is "$(ICULOC)"
#  ICUTRANSLIT
#       The directory that contains trfiles.mk files along with *.txt transliterator files
#
ICUTRNS=$(ICUP)\source\data\translit

#  ICUBRK
#       The directory that contains resfiles.mk files along with *.txt break iterator files
#
ICUBRK=$(ICUP)\source\data\brkitr

#  ICUUNIDATA
#       The directory that contains Unicode data files
#
ICUUNIDATA=$(ICUP)\source\data\unidata


#  ICUMISC
#       The directory that contains files that are miscelleneous data
#
ICUMISC=$(ICUP)\source\data\misc

#
#  ICUDATA
#     The source directory.  Contains the source files for the common data to be built.
#     WARNING:  NOT THE SAME AS ICU_DATA environment variable.  Confusing.
ICUDATA=$(ICUP)\source\data


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
TESTDATAOUT=$(ICUP)\source\test\testdata\out\

#
#   TESTDATABLD
#		The build directory for test data intermidiate files
#		(Tests are NOT run from this makefile,
#         only the data is put in place.)
TESTDATABLD=$(ICUP)\source\test\testdata\out\build

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

# We're including a list of .ucm files.
# There are several lists, they are all optional.

# Always build the mapping files for the EBCDIC fallback codepages
# They are necessary on EBCDIC machines, and
# the following logic is much easier if UCM_SOURCE is never empty.
# (They are small.)
UCM_SOURCE=ibm-37.ucm ibm-1047-s390.ucm

!IF EXISTS("$(ICUUCM)\ucmcore.mk")
!INCLUDE "$(ICUUCM)\ucmcore.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_CORE)
!ELSE
!MESSAGE Warning: cannot find "ucmcore.mk". Not building core MIME/Unix/Windows converter files.
!ENDIF

!IF EXISTS("$(ICUUCM)\ucmfiles.mk")
!INCLUDE "$(ICUUCM)\ucmfiles.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_FILES)
!ELSE
!MESSAGE Warning: cannot find "ucmfiles.mk". Not building many converter files.
!ENDIF

!IF EXISTS("$(ICUUCM)\ucmebcdic.mk")
!INCLUDE "$(ICUUCM)\ucmebcdic.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_EBCDIC)
!ELSE
!MESSAGE Warning: cannot find "ucmebcdic.mk". Not building EBCDIC converter files.
!ENDIF

!IF EXISTS("$(ICUUCM)\ucmlocal.mk")
!INCLUDE "$(ICUUCM)\ucmlocal.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_LOCAL)
!ELSE
!MESSAGE Information: cannot find "ucmlocal.mk". Not building user-additional converter files.
!ENDIF

CNV_FILES=$(UCM_SOURCE:.ucm=.cnv)

# Read list of locale resource bundle files
!IF EXISTS("$(ICULOC)\resfiles.mk")
!INCLUDE "$(ICULOC)\resfiles.mk"
!IF EXISTS("$(ICULOC)\reslocal.mk")
!INCLUDE "$(ICULOC)\reslocal.mk"
GENRB_SOURCE=$(GENRB_SOURCE) $(GENRB_SOURCE_LOCAL)
!ELSE
!MESSAGE Information: cannot find "reslocal.mk". Not building user-additional resource bundle files. 
!ENDIF
!ELSE
!ERROR ERROR: cannot find "resfiles.mk"
!ENDIF

RB_FILES = $(GENRB_SOURCE:.txt=.res)

# Read list of transliterator resource bundle files
!IF EXISTS("$(ICUTRNS)\trnsfiles.mk")
!INCLUDE "$(ICUTRNS)\trnsfiles.mk"
!IF EXISTS("$(ICUTRNS)\trnslocal.mk")
!INCLUDE "$(ICUTRNS)\trnslocal.mk"
TRANLIT_SOURCE=$(TRANSLIT_SOURCE) $(TRANSLIT_SOURCE_LOCAL)
!ELSE
!MESSAGE Information: cannot find "trnslocal.mk". Not building user-additional transliterator files.
!ENDIF
!ELSE
!ERROR ERROR: cannot find "trnsfiles.mk"
!ENDIF

TRANSLIT_FILES = $(TRANSLIT_SOURCE:.txt=.res)

ALL_RES = $(RB_FILES) $(TRANSLIT_FILES)

#############################################################################
#
# ALL  
#     This target builds all the data files.  The world starts here.
#			Note: we really want the common data dll to go to $(DLL_OUTPUT), not $(ICUDBLD).  But specifying
#				that here seems to cause confusion with the building of the stub library of the same name.
#				Building the common dll in $(ICUDBLD) unconditionally copies it to $(DLL_OUTPUT) too.
#
#############################################################################
ALL : GODATA "$(ICUDBLD)\$(U_ICUDATA_NAME).dll" testdata "$(TESTDATAOUT)\test1.cnv" "$(TESTDATAOUT)\test3.cnv" "$(TESTDATAOUT)\test4.cnv"
	@echo All targets are up to date

#
# testdata - nmake will invoke pkgdata, which will create testdata.dat 
#
testdata: ucadata.dat $(TRANSLIT_FILES) $(RB_FILES)  {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe
	@cd "$(TESTDATA)"
	@echo building testdata...
	nmake /nologo /f "$(TESTDATA)\testdata.mk" TESTDATA=. ICUTOOLS="$(ICUTOOLS)" PKGOPT="$(PKGOPT)" CFG=$(CFG) TESTDATAOUT="$(TESTDATAOUT)" ICUDATA="$(ICUDATA)" TESTDATABLD="$(TESTDATABLD)"

BRK_FILES = "$(ICUDBLD)\sent.brk" "$(ICUDBLD)\char.brk" "$(ICUDBLD)\line.brk" "$(ICUDBLD)\word.brk" "$(ICUDBLD)\line_th.brk" "$(ICUDBLD)\word_th.brk"

#invoke pkgdata for ICU common data
#  pkgdata will drop all output files (.dat, .dll, .lib) into the target (ICUDBLD) directory.
#  move the .dll and .lib files to their final destination afterwards.
#
"$(ICUDBLD)\$(U_ICUDATA_NAME).dll" : $(CNV_FILES) $(BRK_FILES) "$(ICUDBLD)\uprops.dat" "$(ICUDBLD)\unames.dat" "$(ICUDBLD)\unorm.dat" "$(ICUDBLD)\cnvalias.dat" "$(ICUDBLD)\tz.dat" "$(ICUDBLD)\ucadata.dat" "$(ICUDBLD)\invuca.dat" $(ALL_RES) "$(ICUDBLD)\icudata.res" "$(ICUP)\source\stubdata\stubdatabuilt.txt"
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
	copy "$(U_ICUDATA_NAME).dat" "$(ICUOUT)\$(U_ICUDATA_NAME)$(U_ICUDATA_ENDIAN_SUFFIX).dat"
	-@erase "$(U_ICUDATA_NAME).dat"

 


"$(ICUDBLD)\sent.brk" : "$(ICUBRK)\sentLE.brk"
    copy "$(ICUBRK)\sentLE.brk" "$(ICUDBLD)\sent.brk"

"$(ICUDBLD)\char.brk" : "$(ICUBRK)\charLE.brk"
    copy "$(ICUBRK)\charLE.brk" "$(ICUDBLD)\char.brk"

"$(ICUDBLD)\line.brk" : "$(ICUBRK)\lineLE.brk"
    copy "$(ICUBRK)\lineLE.brk" "$(ICUDBLD)\line.brk"

"$(ICUDBLD)\word.brk" : "$(ICUBRK)\wordLE.brk"
    copy "$(ICUBRK)\wordLE.brk" "$(ICUDBLD)\word.brk"

"$(ICUDBLD)\line_th.brk" : "$(ICUBRK)\line_thLE.brk"
    copy "$(ICUBRK)\line_thLE.brk" "$(ICUDBLD)\line_th.brk"

"$(ICUDBLD)\word_th.brk" : "$(ICUBRK)\word_thLE.brk"
    copy "$(ICUBRK)\word_thLE.brk" "$(ICUDBLD)\word_th.brk"

# utility target to send us to the right dir
GODATA :
	@if not exist "$(ICUDBLD)\$(NULL)" mkdir "$(ICUDBLD)"
	@if not exist "$(ICUOUT)\$(NULL)" mkdir "$(ICUOUT)"
	@if not exist "$(TESTDATABLD)\$(NULL)" mkdir "$(TESTDATABLD)"
	@if not exist "$(TESTDATAOUT)\$(NULL)" mkdir "$(TESTDATAOUT)"
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
    -@erase "*.dll"
	@cd "$(TESTDATAOUT)"
	-@erase "*.dat"
	-@erase "*.cnv"
	@cd "$(TESTDATABLD)"
	-@erase "*.res"
	-@erase "*.typ"
	@cd "$(ICUTOOLS)"



# Targets for test converter data
"$(TESTDATAOUT)\test1.cnv": "$(TESTDATA)\test1.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(TESTDATAOUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

"$(TESTDATAOUT)\test3.cnv": "$(TESTDATA)\test3.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(TESTDATAOUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

"$(TESTDATAOUT)\test4.cnv": "$(TESTDATA)\test4.ucm"
	@cd "$(ICUDATA)"
	@set ICU_DATA=$(TESTDATAOUT)
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" $**

# Batch inference rule for creating converters
{$(ICUUCM)}.ucm.cnv::
	@echo Generating converters
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" -d"$(ICUDBLD)" $<


# Batch infrence rule for creating transliterator resource files
{$(ICUTRNS)}.txt.res::
	@echo Making Transliterator Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -d"$(ICUDBLD)" $<

# Inference rule for creating resource bundle files
{$(ICULOC)}.txt.res::
	@echo Making Locale Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -d"$(ICUDBLD)" $<

# DLL version information
"$(ICUDBLD)\icudata.res": "$(ICUMISC)\icudata.rc"
	@echo Creating data DLL version information from $**
	@rc.exe /i $(ICUP)\include\ /r /fo "$@" $**

# Targets for unames.dat
"$(ICUDBLD)\unames.dat": "$(ICUUNIDATA)\UnicodeData.txt" "$(ICUTOOLS)\gennames\$(CFG)\gennames.exe"
	@echo Creating data file for Unicode Names
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gennames\$(CFG)\gennames" -1 -u $(UNICODE_VERSION) "$(ICUUNIDATA)\UnicodeData.txt"

# Targets for uprops.dat
"$(ICUDBLD)\uprops.dat": "$(ICUUNIDATA)\UnicodeData.txt" "$(ICUTOOLS)\genprops\$(CFG)\genprops.exe"
	@echo Creating data file for Unicode Character Properties
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\genprops\$(CFG)\genprops" -u $(UNICODE_VERSION) -s "$(ICUUNIDATA)"

# Targets for unorm.dat
"$(ICUDBLD)\unorm.dat": "$(ICUUNIDATA)\UnicodeData.txt" "$(ICUUNIDATA)\DerivedNormalizationProperties.txt" "$(ICUTOOLS)\gennorm\$(CFG)\gennorm.exe"
	@echo Creating data file for Unicode Normalization
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gennorm\$(CFG)\gennorm" -u $(UNICODE_VERSION) -s "$(ICUUNIDATA)"

# Targets for converters
"$(ICUDBLD)\cnvalias.dat" : {"$(ICUUCM)"}\convrtrs.txt "$(ICUTOOLS)\gencnval\$(CFG)\gencnval.exe"
	@echo Creating data file for Converter Aliases
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gencnval\$(CFG)\gencnval" "$(ICUUCM)\convrtrs.txt"

# Targets for tz
"$(ICUDBLD)\tz.dat" : {"$(ICUMISC)"}timezone.txt {"$(ICUTOOLS)\gentz\$(CFG)"}gentz.exe
	@echo Creating data file for Timezones
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\gentz\$(CFG)\gentz" "$(ICUMISC)\timezone.txt"

# Targets for ucadata.dat & invuca.dat
"$(ICUDBLD)\ucadata.dat": "$(ICUUNIDATA)\FractionalUCA.txt" "$(ICUTOOLS)\genuca\$(CFG)\genuca.exe"
	@echo Creating UCA data files
	@set ICU_DATA=$(ICUDBLD)
	@"$(ICUTOOLS)\genuca\$(CFG)\genuca" -s "$(ICUUNIDATA)"

"$(ICUDBLD)\invuca.dat": "$(ICUDBLD)\ucadata.dat"

$(UCM_SOURCE) : {"$(ICUTOOLS)\makeconv\$(CFG)"}makeconv.exe

{"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe : ucadata.dat uprops.dat unorm.dat

ucadata.dat : uprops.dat unorm.dat

# Dependencies on the tools
convrtrs.txt : {"$(ICUTOOLS)\gencnval\$(CFG)"}gencnval.exe

tz.txt : {"$(ICUTOOLS)\gentz\$(CFG)"}gentz.exe

uprops.dat unames.dat unorm.dat cnvalias.dat tz.dat ucadata.dat invuca.dat: GODATA {"$(ICUTOOLS)\genccode\$(CFG)"}genccode.exe


$(TRANSLIT_SOURCE) $(GENRB_SOURCE) : {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe ucadata.dat uprops.dat unorm.dat

