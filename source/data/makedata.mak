#**********************************************************************
#* Copyright (C) 1999-2004, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak icumake=$(ProjectDir)
#
#	12/10/1999	weiv	Created

##############################################################################
# Keep the following in sync with the version - see common/unicode/uversion.h
U_ICUDATA_NAME=icudt30
##############################################################################
U_ICUDATA_ENDIAN_SUFFIX=l
UNICODE_VERSION=4.0.1

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

ICUOUT=$(ICUMAKE)\out

#  the prefix "icudt21_" for use in filenames
ICUPKG=$(U_ICUDATA_NAME)$(U_ICUDATA_ENDIAN_SUFFIX)

# need to nuke \\ for .NET...
ICUOUT=$(ICUOUT:\\=\)

ICUBLD=$(ICUOUT)\build\$(ICUPKG)
ICUTMP=$(ICUOUT)\tmp

#  ICUP
#     The root of the ICU source directory tree
#
ICUP=$(ICUMAKE)\..\..
ICUP=$(ICUP:\source\data\..\..=)
# In case the first one didn't do it, try this one.  .NET would do the second one.
ICUP=$(ICUP:\source\data\\..\..=)
!MESSAGE ICU root path is $(ICUP)


#  ICUSRCDATA
#       The data directory in source
#
ICUSRCDATA=$(ICUP)\source\data
ICUSRCDATA_RELATIVE_PATH=..\..\..\

#  ICUUCM
#       The directory that contains ucmcore.mk files along with *.ucm files
#
ICUUCM=mappings

#  ICULOC
#       The directory that contains resfiles.mk files along with *.txt locale data files
#
ICULOC=locales

#  ICUCOL
#       The directory that contains colfiles.mk files along with *.txt collation data files
#
ICUCOL=coll

#  ICUTRANSLIT
#       The directory that contains trfiles.mk files along with *.txt transliterator files
#
ICUTRNS=translit

#  ICUBRK
#       The directory that contains resfiles.mk files along with *.txt break iterator files
#
ICUBRK=$(ICUP)\source\data\brkitr

#  ICUUNIDATA
#       The directory that contains Unicode data files
#
ICUUNIDATA=$(ICUP)\source\data\unidata


#  ICUMISC
#       The directory that contains miscfiles.mk along with files that are miscelleneous data
#
ICUMISC=$(ICUP)\source\data\misc
ICUMISC2=misc

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

# The current ICU tools need to be in the path first.
PATH = $(ICUP)\bin;$(PATH)

# This variable can be overridden to "-m static" by the project settings,
# if you want a static data library.
!IF "$(ICU_PACKAGE_MODE)"==""
ICU_PACKAGE_MODE=-m dll
!ENDIF


# Suffixes for data files
.SUFFIXES : .ucm .cnv .dll .dat .res .txt .c

# We're including a list of .ucm files.
# There are several lists, they are all optional.

# Always build the mapping files for the EBCDIC fallback codepages
# They are necessary on EBCDIC machines, and
# the following logic is much easier if UCM_SOURCE is never empty.
# (They are small.)
UCM_SOURCE=ibm-37_P100-1995.ucm ibm-1047_P100-1995.ucm

!IF EXISTS("$(ICUSRCDATA)\$(ICUUCM)\ucmcore.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUUCM)\ucmcore.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_CORE)
!ELSE
!MESSAGE Warning: cannot find "ucmcore.mk". Not building core MIME/Unix/Windows converter files.
!ENDIF

!IF EXISTS("$(ICUSRCDATA)\$(ICUUCM)\ucmfiles.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUUCM)\ucmfiles.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_FILES)
!ELSE
!MESSAGE Warning: cannot find "ucmfiles.mk". Not building many converter files.
!ENDIF

!IF EXISTS("$(ICUSRCDATA)\$(ICUUCM)\ucmebcdic.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUUCM)\ucmebcdic.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_EBCDIC)
!ELSE
!MESSAGE Warning: cannot find "ucmebcdic.mk". Not building EBCDIC converter files.
!ENDIF

!IF EXISTS("$(ICUSRCDATA)\$(ICUUCM)\ucmlocal.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUUCM)\ucmlocal.mk"
UCM_SOURCE=$(UCM_SOURCE) $(UCM_SOURCE_LOCAL)
!ELSE
!MESSAGE Information: cannot find "ucmlocal.mk". Not building user-additional converter files.
!ENDIF

CNV_FILES=$(UCM_SOURCE:.ucm=.cnv)

# Read list of locale resource bundle files
!IF EXISTS("$(ICUSRCDATA)\$(ICULOC)\resfiles.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICULOC)\resfiles.mk"
!IF EXISTS("$(ICUSRCDATA)\$(ICULOC)\reslocal.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICULOC)\reslocal.mk"
GENRB_SOURCE=$(GENRB_SOURCE) $(GENRB_SOURCE_LOCAL)
!ELSE
!MESSAGE Information: cannot find "reslocal.mk". Not building user-additional resource bundle files.
!ENDIF
!ELSE
!MESSAGE Warning: cannot find "resfiles.mk"
!ENDIF

RB_FILES = root.res $(GENRB_ALIAS_SOURCE:.txt=.res) $(GENRB_SOURCE:.txt=.res)

# Read list of locale resource bundle files
!IF EXISTS("$(ICUSRCDATA)\$(ICUCOL)\colfiles.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUCOL)\colfiles.mk"
!IF EXISTS("$(ICUSRCDATA)\$(ICUCOL)\collocal.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUCOL)\collocal.mk"
COLLATION_SOURCE=$(COLLATION_SOURCE) $(COLLATION_SOURCE_LOCAL)
!ELSE
!MESSAGE Information: cannot find "collocal.mk". Not building user-additional resource bundle files.
!ENDIF
!ELSE
!MESSAGE Warning: cannot find "colfiles.mk"
!ENDIF

COL_FILES = root.res $(COLLATION_ALIAS_SOURCE:.txt=.res) $(COLLATION_SOURCE:.txt=.res)
COL_COL_FILES=$(COL_FILES:.res=.crs)
# Read list of transliterator resource bundle files
!IF EXISTS("$(ICUSRCDATA)\$(ICUTRNS)\trnsfiles.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUTRNS)\trnsfiles.mk"
!IF EXISTS("$(ICUSRCDATA)\$(ICUTRNS)\trnslocal.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUTRNS)\trnslocal.mk"
TRANLIT_SOURCE=$(TRANSLIT_SOURCE) $(TRANSLIT_SOURCE_LOCAL)
!ELSE
!MESSAGE Information: cannot find "trnslocal.mk". Not building user-additional transliterator files.
!ENDIF
!ELSE
!MESSAGE Warning: cannot find "trnsfiles.mk"
!ENDIF

TRANSLIT_FILES = $(TRANSLIT_SOURCE:.txt=.res)

# Read list of miscellaneous resource bundle files
!IF EXISTS("$(ICUSRCDATA)\$(ICUMISC2)\miscfiles.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUMISC2)\miscfiles.mk"
!IF EXISTS("$(ICUSRCDATA)\$(ICUMISC2)\misclocal.mk")
!INCLUDE "$(ICUSRCDATA)\$(ICUMISC2)\misclocal.mk"
MISC_SOURCE=$(MISC_SOURCE) $(MISC_SOURCE_LOCAL)
!ELSE
!MESSAGE Information: cannot find "misclocal.mk". Not building user-additional miscellaenous files.
!ENDIF
!ELSE
!MESSAGE Warning: cannot find "miscfiles.mk"
!ENDIF

MISC_FILES = $(MISC_SOURCE:.txt=.res)

INDEX_RES_FILES = res_index.res
INDEX_COL_FILES = coll\res_index.res

# don't include COL_FILES
ALL_RES = $(INDEX_RES_FILES) $(RB_FILES) $(TRANSLIT_FILES) $(MISC_FILES)

#############################################################################
#
# ALL
#     This target builds all the data files.  The world starts here.
#			Note: we really want the common data dll to go to $(DLL_OUTPUT), not $(ICUBLD).  But specifying
#				that here seems to cause confusion with the building of the stub library of the same name.
#				Building the common dll in $(ICUBLD) unconditionally copies it to $(DLL_OUTPUT) too.
#
#############################################################################
ALL : GODATA "$(DLL_OUTPUT)\$(U_ICUDATA_NAME).dll" "$(TESTDATAOUT)\testdata.dat"
	@echo All targets are up to date

#
# testdata - nmake will invoke pkgdata, which will create testdata.dat
#
"$(TESTDATAOUT)\testdata.dat": "$(ICUBLD)\ucadata.icu" $(TRANSLIT_FILES) $(MISC_FILES) $(RB_FILES) {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe
	@cd "$(TESTDATA)"
	@echo building testdata...
	nmake /nologo /f "$(TESTDATA)\testdata.mk" TESTDATA=. ICUTOOLS="$(ICUTOOLS)" ICUP="$(ICUP)" CFG=$(CFG) TESTDATAOUT="$(TESTDATAOUT)" ICUDATA="$(ICUDATA)" TESTDATABLD="$(TESTDATABLD)"

#
#  Break iterator data files.
#
BRK_FILES = sent.brk char.brk line.brk word.brk title.brk line_th.brk word_th.brk

#invoke pkgdata for ICU common data
#  pkgdata will drop all output files (.dat, .dll, .lib) into the target (ICUBLD) directory.
#  move the .dll and .lib files to their final destination afterwards.
#  The $(U_ICUDATA_NAME).lib and $(U_ICUDATA_NAME).exp should already be in the right place due to stubdata.
#
"$(DLL_OUTPUT)\$(U_ICUDATA_NAME).dll" : "$(ICUP)\bin\pkgdata.exe" $(CNV_FILES) $(BRK_FILES) "$(ICUBLD)\uprops.icu" "$(ICUBLD)\unames.icu" "$(ICUBLD)\pnames.icu" "$(ICUBLD)\unorm.icu" "$(ICUBLD)\cnvalias.icu" "$(ICUBLD)\ucadata.icu" "$(ICUBLD)\invuca.icu" "$(ICUBLD)\uidna.spp" $(INDEX_COL_FILES) $(COL_COL_FILES) $(ALL_RES) "$(ICUTMP)\icudata.res" "$(ICUP)\source\stubdata\stubdatabuilt.txt"
	echo Building icu data
	cd "$(ICUBLD)"
	@"$(ICUP)\bin\pkgdata" -Z -f -e $(U_ICUDATA_NAME) -v $(ICU_PACKAGE_MODE) -c -p $(ICUPKG) -T "$(ICUTMP)" -L $(U_ICUDATA_NAME) -d "$(ICUBLD)" -s . <<pkgdatain.txt
unorm.icu
uprops.icu
pnames.icu
unames.icu
ucadata.icu
invuca.icu
uidna.spp
cnvalias.icu
$(CNV_FILES:.cnv =.cnv
)
$(ALL_RES:.res =.res
)
$(COL_COL_FILES:.crs =.crs
)
res_index.crs
$(BRK_FILES:.brk =.brk
)
<<KEEP
	copy "$(U_ICUDATA_NAME).dll" "$(DLL_OUTPUT)"
	-@erase "$(U_ICUDATA_NAME).dll"
	copy "$(ICUPKG).dat" "$(ICUOUT)\$(U_ICUDATA_NAME)$(U_ICUDATA_ENDIAN_SUFFIX).dat"
	-@erase "$(ICUPKG).dat"



# RBBI .brk file generation.
#      TODO:  set up an inference rule, so these don't need to be written out one by one...
#

BRKDEPS = "$(ICUBLD)\uprops.icu" "$(ICUBLD)\unames.icu" "$(ICUBLD)\pnames.icu" "$(ICUBLD)\unorm.icu"

char.brk : "$(ICUBRK)\char.txt" $(BRKDEPS)
	genbrk -c -r "$(ICUBRK)\char.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)"

word.brk : "$(ICUBRK)\word.txt" $(BRKDEPS)
	genbrk -c -r "$(ICUBRK)\word.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)"

line.brk : "$(ICUBRK)\line.txt" $(BRKDEPS)
	genbrk -c -r "$(ICUBRK)\line.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)"

sent.brk : "$(ICUBRK)\sent.txt" $(BRKDEPS)
	genbrk -c -r "$(ICUBRK)\sent.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)"

title.brk : "$(ICUBRK)\title.txt" $(BRKDEPS)
	genbrk -c -r "$(ICUBRK)\title.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)"

word_th.brk : "$(ICUBRK)\word_th.txt" $(BRKDEPS)
	genbrk -c -r "$(ICUBRK)\word_th.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)"

line_th.brk : "$(ICUBRK)\line_th.txt" $(BRKDEPS)
	genbrk -c -r "$(ICUBRK)\line_th.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)"


# utility target to send us to the right dir
GODATA :
	@if not exist "$(ICUOUT)\$(NULL)" mkdir "$(ICUOUT)"
	@if not exist "$(ICUTMP)\$(NULL)" mkdir "$(ICUTMP)"
	@if not exist "$(ICUOUT)\build\$(NULL)" mkdir "$(ICUOUT)\build"
	@if not exist "$(ICUBLD)\$(NULL)" mkdir "$(ICUBLD)"
	@if not exist "$(ICUBLD)\coll\$(NULL)" mkdir "$(ICUBLD)\coll"
	@if not exist "$(TESTDATAOUT)\$(NULL)" mkdir "$(TESTDATAOUT)"
	@if not exist "$(TESTDATABLD)\$(NULL)" mkdir "$(TESTDATABLD)"
	@cd "$(ICUBLD)"

# This is to remove all the data files
CLEAN : GODATA
	@echo Cleaning up the data files.
	@cd "$(ICUBLD)"
	-@erase "*.brk"
	-@erase "*.cnv"
	-@erase "*.exp"
	-@erase "*.icu"
	-@erase "*.lib"
	-@erase "*.mak"
	-@erase "*.obj"
	-@erase "*.res"
	-@erase "*.spp"
	-@erase "*.txt"
	@cd "$(ICUOUT)"
	-@erase "*.dat"
	@cd "$(TESTDATABLD)"
	-@erase "*.cnv"
	-@erase "*.icu"
	-@erase "*.mak"
	-@erase "*.res"
	-@erase "*.spp"
	-@erase "*.txt"
	@cd "$(TESTDATAOUT)"
	-@erase "*.dat"
	-@erase "*.typ"
	@cd "$(ICUTMP)"
	-@erase "*.lst"
	-@erase "*.mak"
	@cd "$(ICUBLD)"


# Batch inference rule for creating converters
{$(ICUSRCDATA_RELATIVE_PATH)\$(ICUUCM)}.ucm.cnv::
	@echo Generating converters
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" -c -d"$(ICUBLD)" $<

# Batch inference rule for creating transliterator resource files
{$(ICUSRCDATA_RELATIVE_PATH)\$(ICUTRNS)}.txt.res::
	@echo Making Transliterator Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -d"$(ICUBLD)" $<

# Batch inference rule for creating miscellaneous resource files
# TODO: -q option is specified to squelch the 120+ warnings about
#       empty intvectors and binary elements.  Unfortunately, this may
#       squelch other legitimate warnings.  When there is a better
#       way, remove the -q.
{$(ICUSRCDATA_RELATIVE_PATH)\$(ICUMISC2)}.txt.res::
	@echo Making Miscellaneous Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -q -d"$(ICUBLD)" $<

{$(ICUSRCDATA_RELATIVE_PATH)\coll}.txt.crs::
	@echo Making Collation files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -d"$(ICUBLD)\coll" $<
	ren "$(ICUBLD)\coll\*.res" "*.crs"
	copy "$(ICUBLD)\coll\*.crs" "$(ICUBLD)\"
	ren "$(ICUBLD)\coll\*.crs" "*.res"

# Inference rule for creating resource bundle files
{$(ICUSRCDATA_RELATIVE_PATH)\$(ICULOC)}.txt.res::
	@echo Making Locale Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -d"$(ICUBLD)" $<

$(INDEX_COL_FILES):
	@echo Generating <<coll\res_index.txt
// Warning this file is automatically generated
res_index {
    InstalledLocales {
        $(COLLATION_SOURCE:.txt= {""}
       )
    }
}
<<KEEP
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -d"$(ICUBLD)\coll" .\coll\res_index.txt


$(INDEX_RES_FILES):
	@echo Generating <<res_index.txt
// Warning this file is automatically generated
res_index {
    InstalledLocales {
        $(GENRB_SOURCE:.txt= {""}
       )
    }
}
<<KEEP
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -d"$(ICUBLD)" .\res_index.txt

# Inference rule for creating resource bundle files
{$(ICUSRCDATA_RELATIVE_PATH)\$(ICULOC)}.txt.res::
	@echo Making Locale Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -d"$(ICUBLD)" $<

# DLL version information
# If you modify this, modify winmode.c in pkgdata.
"$(ICUTMP)\icudata.res": "$(ICUMISC)\icudata.rc"
	@echo Creating data DLL version information from $**
	@rc.exe /i "..\..\..\..\..\include" /r /fo $@ $**

# Targets for unames.icu
"$(ICUBLD)\unames.icu": "$(ICUUNIDATA)\*.txt" "$(ICUTOOLS)\gennames\$(CFG)\gennames.exe"
	@echo Creating data file for Unicode Names
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\gennames\$(CFG)\gennames" -1 -u $(UNICODE_VERSION) "$(ICUUNIDATA)\UnicodeData.txt"

# Targets for pnames.icu
# >> Depends on the Unicode data as well as uchar.h and uscript.h <<
"$(ICUBLD)\pnames.icu": "$(ICUUNIDATA)\*.txt" "$(ICUTOOLS)\genpname\$(CFG)\genpname.exe" "$(ICUP)\source\common\unicode\uchar.h" "$(ICUP)\source\common\unicode\uscript.h"
	@echo Creating data file for Unicode Property Names
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\genpname\$(CFG)\genpname" -d "$(ICUBLD)"

# Targets for uprops.icu
"$(ICUBLD)\uprops.icu": "$(ICUUNIDATA)\*.txt" "$(ICUTOOLS)\genprops\$(CFG)\genprops.exe" "$(ICUBLD)\pnames.icu"
	@echo Creating data file for Unicode Character Properties
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\genprops\$(CFG)\genprops" -u $(UNICODE_VERSION) -s "$(ICUUNIDATA)"

# Targets for unorm.icu
"$(ICUBLD)\unorm.icu": "$(ICUUNIDATA)\*.txt" "$(ICUTOOLS)\gennorm\$(CFG)\gennorm.exe"
	@echo Creating data file for Unicode Normalization
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\gennorm\$(CFG)\gennorm" -u $(UNICODE_VERSION) -s "$(ICUUNIDATA)"

# Targets for converters
"$(ICUBLD)\cnvalias.icu" : {"$(ICUSRCDATA)\$(ICUUCM)"}\convrtrs.txt "$(ICUTOOLS)\gencnval\$(CFG)\gencnval.exe"
	@echo Creating data file for Converter Aliases
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\gencnval\$(CFG)\gencnval" "$(ICUSRCDATA)\$(ICUUCM)\convrtrs.txt"

# Targets for ucadata.icu & invuca.icu
"$(ICUBLD)\invuca.icu" "$(ICUBLD)\ucadata.icu": "$(ICUUNIDATA)\FractionalUCA.txt" "$(ICUTOOLS)\genuca\$(CFG)\genuca.exe" "$(ICUBLD)\uprops.icu" "$(ICUBLD)\unorm.icu"
	@echo Creating UCA data files
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\genuca\$(CFG)\genuca" -s "$(ICUUNIDATA)"

# Targets for uidna.spp
"$(ICUBLD)\uidna.spp" : "$(ICUUNIDATA)\*.txt" "$(ICUMISC)\NamePrepProfile.txt"
	gensprep -s "$(ICUMISC)" -d "$(ICUBLD)\\" -b uidna -n "$(ICUUNIDATA)" -k -u 3.2.0 NamePrepProfile.txt

# Dependencies on the tools for the batch inference rules

$(UCM_SOURCE) : {"$(ICUTOOLS)\makeconv\$(CFG)"}makeconv.exe

$(TRANSLIT_SOURCE) $(MISC_SOURCE) $(GENRB_SOURCE) "$(ICUBLD)\root.res" : {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe "$(ICUBLD)\ucadata.icu" "$(ICUBLD)\uprops.icu" "$(ICUBLD)\unorm.icu"

