#**********************************************************************
#* Copyright (C) 1999-2003, International Business Machines Corporation
#* and others.  All Rights Reserved.
#**********************************************************************
# nmake file for creating data files on win32
# invoke with
# nmake /f makedata.mak [Debug|Release]
#
#	12/10/1999	weiv	Created

##############################################################################
# Keep the following in sync with the version - see common/unicode/uversion.h
U_ICUDATA_NAME=icudt26
##############################################################################
U_ICUDATA_ENDIAN_SUFFIX=l
UNICODE_VERSION=4

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

# need to nuke \\ for .NET...
ICUOUT=$(ICUOUT:\\=\)

ICUBLD=$(ICUOUT)\build

# ICUDT
#  the prefix "icudt21_" for use in filenames
ICUPKG=$(U_ICUDATA_NAME)$(U_ICUDATA_ENDIAN_SUFFIX)
ICUDT=$(ICUPKG)_

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
ICUSRCDATA_RELATIVE_PATH=..\..\

#  ICUUCM
#       The directory that contains ucmcore.mk files along with *.ucm files
#
ICUUCM=mappings

#  ICULOC
#       The directory that contains resfiles.mk files along with *.txt locale data files
#
ICULOC=locales

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

INDEX_RES_FILES = res_index.res

ALL_RES = $(INDEX_RES_FILES) $(RB_FILES) $(TRANSLIT_FILES)

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
"$(TESTDATAOUT)\testdata.dat": $(ICUDT)ucadata.icu $(TRANSLIT_FILES) $(RB_FILES)  {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe
	@cd "$(TESTDATA)"
	@echo building testdata...
	nmake /nologo /f "$(TESTDATA)\testdata.mk" TESTDATA=. ICUTOOLS="$(ICUTOOLS)" PKGOPT="$(PKGOPT)" CFG=$(CFG) TESTDATAOUT="$(TESTDATAOUT)" ICUDATA="$(ICUDATA)" TESTDATABLD="$(TESTDATABLD)"

#
#  Break iterator data files.
#
BRK_FILES = $(ICUDT)sent.brk $(ICUDT)char.brk $(ICUDT)line.brk $(ICUDT)word.brk $(ICUDT)title.brk $(ICUDT)line_th.brk $(ICUDT)word_th.brk

#invoke pkgdata for ICU common data
#  pkgdata will drop all output files (.dat, .dll, .lib) into the target (ICUBLD) directory.
#  move the .dll and .lib files to their final destination afterwards.
#  The $(U_ICUDATA_NAME).lib and $(U_ICUDATA_NAME).exp should already be in the right place due to stubdata.
#
"$(DLL_OUTPUT)\$(U_ICUDATA_NAME).dll" : "$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata.exe" $(CNV_FILES) $(BRK_FILES) "$(ICUBLD)\$(ICUDT)uprops.icu" "$(ICUBLD)\$(ICUDT)unames.icu" "$(ICUBLD)\$(ICUDT)pnames.icu" "$(ICUBLD)\$(ICUDT)unorm.icu" "$(ICUBLD)\$(ICUDT)cnvalias.icu" "$(ICUBLD)\$(ICUDT)tz.icu" "$(ICUBLD)\$(ICUDT)ucadata.icu" "$(ICUBLD)\$(ICUDT)invuca.icu" "$(ICUBLD)\$(ICUDT)uidna.icu" $(ALL_RES) "$(ICUBLD)\$(ICUDT)icudata.res" "$(ICUP)\source\stubdata\stubdatabuilt.txt"
	@echo Building icu data
	@cd "$(ICUBLD)"
 	@"$(ICUTOOLS)\pkgdata\$(CFG)\pkgdata" -f -e $(U_ICUDATA_NAME) -v -m dll -c -p $(ICUPKG) -O "$(PKGOPT)" -d "$(ICUBLD)" -s . <<pkgdatain.txt
$(ICUDT)unorm.icu
$(ICUDT)uprops.icu
$(ICUDT)pnames.icu
$(ICUDT)unames.icu
$(ICUDT)ucadata.icu
$(ICUDT)invuca.icu
$(ICUDT)uidna.icu
$(ICUDT)tz.icu
$(ICUDT)cnvalias.icu
$(CNV_FILES:.cnv =.cnv
)
$(ALL_RES:.res =.res
)
$(BRK_FILES:.brk =.brk
)
<<KEEP
	copy "$(ICUPKG).dll" "$(DLL_OUTPUT)"
	-@erase "$(ICUPKG).dll"
	copy "$(ICUPKG).dat" "$(ICUOUT)\$(U_ICUDATA_NAME)$(U_ICUDATA_ENDIAN_SUFFIX).dat"
	-@erase "$(ICUPKG).dat"

 

# RBBI .brk file generation.
#      TODO:  set up an inference rule, so these don't need to be written out one by one...
#

BRKDEPS = "$(ICUBLD)\$(ICUDT)uprops.icu" "$(ICUBLD)\$(ICUDT)unames.icu" "$(ICUBLD)\$(ICUDT)pnames.icu" "$(ICUBLD)\$(ICUDT)unorm.icu"

$(ICUDT)char.brk : "$(ICUBRK)\char.txt" $(BRKDEPS)
	genbrk -r "$(ICUBRK)\char.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)\\"

$(ICUDT)word.brk : "$(ICUBRK)\word.txt" $(BRKDEPS)
	genbrk -r "$(ICUBRK)\word.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)\\"

$(ICUDT)line.brk : "$(ICUBRK)\line.txt" $(BRKDEPS)
	genbrk -r "$(ICUBRK)\line.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)\\"

$(ICUDT)sent.brk : "$(ICUBRK)\sent.txt" $(BRKDEPS)
	genbrk -r "$(ICUBRK)\sent.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)\\"

$(ICUDT)title.brk : "$(ICUBRK)\title.txt" $(BRKDEPS)
	genbrk -r "$(ICUBRK)\title.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)\\"

$(ICUDT)word_th.brk : "$(ICUBRK)\word_th.txt" $(BRKDEPS)
	genbrk -r "$(ICUBRK)\word_th.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)\\"

$(ICUDT)line_th.brk : "$(ICUBRK)\line_th.txt" $(BRKDEPS)
	genbrk -r "$(ICUBRK)\line_th.txt" -o $@ -d"$(ICUBLD)" -i "$(ICUBLD)\\"


# utility target to send us to the right dir
GODATA :
	@if not exist "$(ICUOUT)\$(NULL)" mkdir "$(ICUOUT)"
	@if not exist "$(ICUBLD)\$(NULL)" mkdir "$(ICUBLD)"
	@if not exist "$(TESTDATAOUT)\$(NULL)" mkdir "$(TESTDATAOUT)"
	@if not exist "$(TESTDATABLD)\$(NULL)" mkdir "$(TESTDATABLD)"
	@cd "$(ICUBLD)"

# This is to remove all the data files
CLEAN : GODATA
	@echo Cleaning up the data files.
	@cd "$(ICUBLD)"
	-@erase "*.brk"
	-@erase "*.cnv"
	-@erase "*.dat"
	-@erase "*.exp"
	-@erase "*.obj"
	-@erase "*.icu"
	-@erase "*.lib"
	-@erase "*.mak"
	-@erase "*.res"
	-@erase "*.txt"
	@cd "$(ICUOUT)"
	-@erase "*.dat"
	@cd "$(TESTDATABLD)"
	-@erase "*.res"
	-@erase "*.dat"
	-@erase "*.mak"
	@cd "$(TESTDATAOUT)"
	-@erase "*.dat"
	-@erase "*.cnv"
	@cd "$(ICUBLD)"


# Batch inference rule for creating converters
{$(ICUSRCDATA_RELATIVE_PATH)\$(ICUUCM)}.ucm.cnv::
	@echo Generating converters
	@"$(ICUTOOLS)\makeconv\$(CFG)\makeconv" -t -p"$(ICUPKG)" -d"$(ICUBLD)" $<

# Batch infrence rule for creating transliterator resource files
{$(ICUSRCDATA_RELATIVE_PATH)\$(ICUTRNS)}.txt.res::
	@echo Making Transliterator Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -t -p"$(ICUPKG)" -d"$(ICUBLD)" $<

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
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -t -p"$(ICUPKG)" -d"$(ICUBLD)" .\res_index.txt

# Inference rule for creating resource bundle files
{$(ICUSRCDATA_RELATIVE_PATH)\$(ICULOC)}.txt.res::
	@echo Making Locale Resource Bundle files
	@"$(ICUTOOLS)\genrb\$(CFG)\genrb" -k -t -p"$(ICUPKG)" -d"$(ICUBLD)" $<

# DLL version information
"$(ICUBLD)\$(ICUDT)icudata.res": "$(ICUMISC)\icudata.rc"
	@echo Creating data DLL version information from $**
	@rc.exe /i "..\..\..\..\include" /r /fo $@ $**

# Targets for unames.icu
"$(ICUBLD)\$(ICUDT)unames.icu": "$(ICUUNIDATA)\*.txt" "$(ICUTOOLS)\gennames\$(CFG)\gennames.exe"
	@echo Creating data file for Unicode Names
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\gennames\$(CFG)\gennames" -1 -u $(UNICODE_VERSION) "$(ICUUNIDATA)\UnicodeData.txt"

# Targets for pnames.icu
# >> Depends on the Unicode data as well as uchar.h and uscript.h <<
"$(ICUBLD)\$(ICUDT)pnames.icu": "$(ICUUNIDATA)\*.txt" "$(ICUTOOLS)\genpname\$(CFG)\genpname.exe" "$(ICUP)\source\common\unicode\uchar.h" "$(ICUP)\source\common\unicode\uscript.h"
	@echo Creating data file for Unicode Property Names
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\genpname\$(CFG)\genpname" -d "$(ICUBLD)"

# Targets for uprops.icu
"$(ICUBLD)\$(ICUDT)uprops.icu": "$(ICUUNIDATA)\*.txt" "$(ICUTOOLS)\genprops\$(CFG)\genprops.exe" "$(ICUBLD)\$(ICUDT)pnames.icu"
	@echo Creating data file for Unicode Character Properties
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\genprops\$(CFG)\genprops" -u $(UNICODE_VERSION) -s "$(ICUUNIDATA)"

# Targets for unorm.icu
"$(ICUBLD)\$(ICUDT)unorm.icu": "$(ICUUNIDATA)\*.txt" "$(ICUTOOLS)\gennorm\$(CFG)\gennorm.exe"
	@echo Creating data file for Unicode Normalization
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\gennorm\$(CFG)\gennorm" -u $(UNICODE_VERSION) -s "$(ICUUNIDATA)"

# Targets for converters
"$(ICUBLD)\$(ICUDT)cnvalias.icu" : {"$(ICUSRCDATA)\$(ICUUCM)"}\convrtrs.txt "$(ICUTOOLS)\gencnval\$(CFG)\gencnval.exe"
	@echo Creating data file for Converter Aliases
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\gencnval\$(CFG)\gencnval" "$(ICUSRCDATA)\$(ICUUCM)\convrtrs.txt"

# Targets for tz
"$(ICUBLD)\$(ICUDT)tz.icu" : {"$(ICUMISC)"}timezone.txt {"$(ICUTOOLS)\gentz\$(CFG)"}gentz.exe
	@echo Creating data file for Timezones
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\gentz\$(CFG)\gentz" "$(ICUMISC)\timezone.txt"

# Targets for ucadata.icu & invuca.icu
"$(ICUBLD)\$(ICUDT)invuca.icu" "$(ICUBLD)\$(ICUDT)ucadata.icu": "$(ICUUNIDATA)\FractionalUCA.txt" "$(ICUTOOLS)\genuca\$(CFG)\genuca.exe" $(ICUDT)uprops.icu $(ICUDT)unorm.icu
	@echo Creating UCA data files
	@set ICU_DATA=$(ICUBLD)
	@"$(ICUTOOLS)\genuca\$(CFG)\genuca" -s "$(ICUUNIDATA)"

# Targets for uidna.icu
"$(ICUBLD)\$(ICUDT)uidna.icu" : "$(ICUUNIDATA)\*.txt" "$(ICUMISC)\*.txt"
	genidna -s "$(ICUDATA)" -d "$(ICUBLD)\\"

# Dependencies on the tools for the batch inference rules

$(UCM_SOURCE) : {"$(ICUTOOLS)\makeconv\$(CFG)"}makeconv.exe

$(TRANSLIT_SOURCE) $(GENRB_SOURCE) "$(ICUBLD)\$(ICUDT)root.res" : {"$(ICUTOOLS)\genrb\$(CFG)"}genrb.exe "$(ICUBLD)\$(ICUDT)ucadata.icu" "$(ICUBLD)\$(ICUDT)uprops.icu" "$(ICUBLD)\$(ICUDT)unorm.icu"

