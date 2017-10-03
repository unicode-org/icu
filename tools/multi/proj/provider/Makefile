#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
#
# Copyright (C) 2010-2012 IBM Corporation and Others, All Rights Reserved.
#
all: glurens ii plugs testprog

# extra files that need generation.
PLUG_EXTRA_DEPS=
UNAME=$(shell uname)
DOT=../../c
SOBJ=ao
ifeq ($(UNAME),Linux)
LIBPATH_VAR=LD_LIBRARY_PATH
PLUG_EXTRA_LDFLAGS=-fPIC
PBLD_EXTRA_FLAGS=-fPIC
GLUE_EXTRA_CFLAGS= -fPIC
PLUGLIB_SO=so
else
ifeq ($(UNAME),Darwin)
LIBPATH_VAR=DYLD_LIBRARY_PATH
PLUG_EXTRA_LDFLAGS=-exported_symbols_list $(GLUE_EXPORT) -dynamiclib -dynamic
PLUG_EXTRA_DEPS+=$(GLUE_EXPORT)
PLUGLIB_SO=dylib
else
ifeq ($(UNAME),AIX)
LIBPATH_VAR=LIBPATH
#PLUG_EXTRA_LDFLAGS=-exported_symbols_list $(GLUE_EXPORT) -dynamiclib -dynamic
PLUG_EXTRA_DEPS+=$(GLUE_EXPORT)
PLUGLIB_SO=a
SOBJ=o
else
$(warning **********************************)
$(warning this script may not know how to handle your $(UNAME) system)
endif
endif
endif

SUBHEAD="*** ICU"

srcdir=$(shell pwd)

PROVIDER=provider
LIBPROVIDER=lib$(PROVIDER).a

#libs: $(ICU_TARBALLS_VERS:%=$(OUT)/%/lib/$(LIBPROVIDER))

PLUGLIB_INST=$(C_INS)/r$(PLUGLIB)
PLUGLIB_ICU_CONFIG=$(PLUGLIB_INST)/bin/icu-config
#PLUGLIB_SO=$(shell $(PLUGLIB_ICU_CONFIG) --so 2>/dev/null )
PLUGLIB_NAME=libicuprov.$(PLUGLIB_MAJ).$(PLUGLIB_SO)
#PLUGLIB_MAJ=$(echo $(PLUGLIB) | cut -d_ -f-1 | tr -d _ )
#GLOUT_ICU=$(ICU_TARBALLS_VERS:%=$(GLOUT)/%/$(OK))
PLUGLIB_AVAILABLE=$(shell echo $(PROVIDER_AVAILABLE) | tr '.' '_')
PLUGLIB_AVAILABLE_DATA=$(PLUGLIB_AVAILABLE:%=$(BUILD)/%/data/out/tmp/*.o)
PLUGLIB_AVAILABLE_STATICS=$(PLUGLIB_AVAILABLE:%=$(BUILD)/%/[ci]*/*.$(SOBJ))
PLUGLIB_OUTLIBS=$(PLUGLIB:%=$(OUT)/%/lib/$(PLUGLIB_NAME))



## configuration
# Absolute path to this dir. 
# dir holding ICUs to be built
MULTICU_ROOT=../..
include $(MULTICU_ROOT)/c/Makefile-c.inc
ICU_CONFIG=icu-config-nosanity
ICU_CONFIG_ORIG=icu-config

# subdirs...
# prefix to distinguish it from non-provider builds
P_PREFIX=p
BUILD=$(M_TMP)/$(P_PREFIX)bld
INST=$(M_TMP)/$(P_PREFIX)ins
SRC=$(M_TMP)/$(P_PREFIX)src
OUT=$(TOP)/out
SOURCE=$(S_SRCDIR)
GLUE=glue
GLUE_EXPORT=$(GLUE)/export.txt
GLOUT=$(M_TMP)/$(P_PREFIX)glout
# header locations
INCGLUE=$(TOP)/$(GLUE)/include
# relative path to icuglue within glout
GLUREN=gluren/include/icuglue
# relative path to gluren.h
GLUREN_H=$(GLUREN)/gluren.h
# Special 'configure' caller
SUPERCONF=$(TOP)/$(MULTICU_ROOT)/c/superconf.sh
# options to compilation (such as, debug)
XOPTS=$(EXTRA_COMPILE_OPTS)
OK=ok
MOPTS=

# directories that need to be built
ALLDIRS=$(INST) $(BUILD) $(SRC) $(GLOUT) $(OUT) $(GLOUT)/$(PLUGLIB) $(OUT)/$(PLUGLIB)/bin
# The 'ok' flag file for installations
INST_ICU=$(PLUGLIB_AVAILABLE:%=$(INST)/%/$(OK))
BUILD_ICU=$(PLUGLIB_AVAILABLE:%=$(BUILD)/%/$(OK))
# this has to do with the pluglib
GLOUT_ICU=$(PLUGLIB:%=$(GLOUT)/%/obj-$(OK))
#SRC_ICU=$(ICU_TARBALLS_VERS:%=$(SRC)/%/$(OK))
# gluren.h files for each version
GLUREN_ICU=$(PLUGLIB_AVAILABLE:%=$(BUILD)/%/$(GLUREN_H))
#?
CONFIGURE_CMD=
# Options passed to configure
CONFIGURE_OPTS=--enable-static --disable-shared --with-data-packaging=static --disable-extras --disable-icuio --disable-layout --disable-tests --disable-samples  $(EXTRA_CONFIGURE_OPTS) 
# additional options passed to make
MAKE_XTRA_OPTS="PKGDATA_MODE=static"
# additional options passed to make install
MAKE_INSTALL_XTRA_OPTS=$(MAKE_XTRA_OPTS)

# Munged source names: list of C++, O etc files for Frontend
PARTSUFF=_fe
GL_FE_CXX=$(notdir $(wildcard $(GLUE)/*$(PARTSUFF).cpp))
GL_FE_FILES=$(GL_FE_CXX:%.cpp=$(GLUE)/%.cpp)
GL_FE_O=$(GL_FE_CPP:%.cpp=%.o)
GL_PARTS=$(GL_FE_CXX:%$(PARTSUFF).cpp=%)
# C flags used in Glue compilation
GLUE_CFLAGS=-I$(INCGLUE) $(GLUE_EXTRA_CFLAGS)

# icu-config switches for compiling: C and C++
ICU_CONFIG_COMMON=--cppflags
# icu-config switches for C
ICU_CONFIG_CC=--cc $(ICU_CONFIG_COMMON) --cflags 
# icu-config switches for C++
ICU_CONFIG_CXX=--cxx $(ICU_CONFIG_COMMON) --cxxflags

# sigh, include common.
PLUG_EXTRA_FLAGS=-I$(BUILD)/$(PLUGLIB)/common -I$(M_TMP)/build/r$(PLUGLIB)/icu/source/common

include Makefile.local



# Usage:  $(call SILENT_COMPILE,logfile,cmd)
ifndef VERBOSE
	LOG_COMPILE=echo build with VERBOSE=1 to unhide compile ;  ( ($(2) 2>&1) > $(1) || (echo "Compile failed, see logfile in $(1) for details." ;exit 1))
else
	LOG_COMPILE=$(2) 2>&1 | tee $(1)
endif

ECHO_COMPILE=echo "\# ${subst ",\",$(1)}" ; ( $(1) || (  exit 1) )

PLUGLIB=$(shell echo $(PROVIDER_TARGET)  | tr '.' '_' )
PLUGLIB_MAJ=$(shell  ./icu2symver.sh $(PLUGLIB))

## @build All installed ICUs
ii: $(GLOUT_ICU)

# Don't delete these files
.PRECIOUS: (INST_ICU) $(BUILD_ICU) $(SRC_ICU) $(ICU_TARBALLS_VERS:%=$(BUILD)/%/config.status) $(ICU_TARBALLS_VERS:%=$(GLOUT)/%/obj-$(OK))  $(ICU_TARBALLS_VERS:%=$(BUILD)/%/config/$(ICU_CONFIG)) $(PLUGLIB_ICU_CONFIG)

# cleanup
clean:
	-rm -rf $(ALLDIRS)
	-$(MAKE) -C ../.. clean

# create all empty work dirs
$(ALLDIRS):
	-mkdir -p $(@)

# Build r$(PLUGLIB) from ../../c
$(PLUGLIB_ICU_CONFIG):
	@echo "$(SUBHEAD) "$(PLUGLIB) "(plugin) building..."
	$(call LOG_COMPILE,../../c/ricus$(PLUGLIB).log,$(MAKE) MAKE_OPTS=$(MAKE_OPTS) XTRA_RICU_CPPFLAGS=$(XTRA_RICU_CPPFLAGS) -C ../../c ICUVERS=$(PLUGLIB) ricus)

# build the glue objects for TARGET
# used to be %/* instead of $(PLUGLIB) - now, wire it down to pluglib.
$(GLOUT)/$(PLUGLIB)/obj-$(OK): $(GLOUT) glurens  $(PLUGLIB_ICU_CONFIG) $(GL_FE_FILES) $(ALLDIRS)
	@echo $(SUBHEAD) $(PLUGLIB) "(plugin) building glue.."
#	$(shell $(BUILD)/$(PLUGLIB)/config/$(ICU_CONFIG) $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$(PLUGLIB) -o $(GLOUT)/$(PLUGLIB)/gl_be_c_$(PLUGLIB).o $(GL_BE_C:%.c=$(GLUE)/%.c)
#	$(shell $(BUILD)/$(PLUGLIB)/config/$(ICU_CONFIG) $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$(PLUGLIB) -o $(GLOUT)/$(PLUGLIB)/gl_be_cxx_$(PLUGLIB).o $(GL_BE_CXX:%.cpp=$(GLUE)/%.cpp)
	-rm -rf $(GLOUT)/$(PLUGLIB)/include
	mkdir -p $(GLOUT)/$(PLUGLIB)/include/icuglue/include
# for each version..  build all OTHER FE files
# TODO: check is unnecessary, not permitted.
# TODO: change to depend instead of for list. 
	@for ver in $(PLUGLIB_AVAILABLE) ; \
	do \
		echo "*$(SUBHEAD)" $$ver "(provider)" for  $(PLUGLIB) "(plugin)" ; \
		if [ ! "$$ver" = "$(PLUGLIB)" ]; \
		then \
			for part in $(GL_PARTS) ; \
			do \
				echo "**$(SUBHEAD)" $$ver "(provider) backend: ---- " "$$part" "----"  ; \
				$(call ECHO_COMPILE,$(shell $(PLUGLIB_ICU_CONFIG) --noverify $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -c -I$(TOP)/$(BUILD)/$$ver/gluren/include -DICUGLUE_VER=$$ver -DICUGLUE_VER_STR=\"$$ver\" "-DGLUE_SYM(x)=glue ## x ## $$ver" -o $(GLOUT)/$(PLUGLIB)/$${part}_$${ver}_for_$(PLUGLIB).o $(GLUE)/$${part}$(PARTSUFF).cpp $(XOPTS) ) || exit 1 ; \
			done ; \
			echo " GLUE_VER( $$ver ) " >> $(GLOUT)/$(PLUGLIB)/include/icuglue/glver.h; \
		fi \
	done
# build 'this version' FE files
	@for part in $(GL_PARTS) ; \
	do \
		echo "ICU" $(PLUGLIB) "(plugin) building plugin for  ------- $$part -----" ; \
		$(call ECHO_COMPILE,$(shell $(PLUGLIB_ICU_CONFIG) --noverify $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -I$(GLOUT)/$(PLUGLIB)/include -c -o $(GLOUT)/$(PLUGLIB)/$${part}_$(PLUGLIB).o $(GLUE)/$${part}$(PARTSUFF).cpp $(XOPTS) $(PLUG_EXTRA_FLAGS) ) || exit 1 ; \
	done
	touch $@

plugs: $(PLUGLIB_OUTLIBS) $(OUT)/$(PLUGLIB_MAJ:%=icuplugins%.txt)

install-plugs: $(PLUGLIB_INST)/lib/$(PLUGLIB_NAME) $(PLUGLIB_INST)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt
	echo "Plugins installed for testing."

#TODO: should be 'install' not 'cp'
$(PLUGLIB_INST)/lib/$(PLUGLIB_NAME) $(PLUGLIB_INST)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt: $(PLUGLIB_OUTLIBS)
	cp $(PLUGLIB_OUTLIBS) $(PLUGLIB_INST)/lib
	echo "# Generated. " > $(PLUGLIB_INST)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt
	@for part in $(GL_PARTS) ; \
	do \
		echo "$(PLUGLIB_NAME)	$${part}_provider_plugin" >> $(PLUGLIB_INST)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt ; \
		echo " $${part}_provider_plugin" ; \
	done

$(GLUE_EXPORT): Makefile $(GL_FE_FILES)
	echo "# Generated for installation. " > $@
	@for part in $(GL_PARTS) ; \
	do \
		echo "_$${part}_provider_plugin" >> $@ ; \
		echo " _$${part}_provider_plugin" ; \
	done

$(OUT)/icuplugins$(PLUGLIB_MAJ).txt: $(PLUGLIB_OUTLIBS) Makefile
	echo "# Generated for testing." > $@
	@for part in $(GL_PARTS) ; \
	do \
		echo "$(PLUGLIB_NAME)	$${part}_provider_plugin" >> $@ ; \
		echo " $${part}_provider_plugin" ; \
	done

TESTPROG=$(GL_PARTS:%=%diff$(EXEEXT))

testprog: $(PLUGLIB:%=$(OUT)/%/bin/$(TESTPROG)) 

check-icu2symver:
	./check-icu2symver.sh

generate-gdbrc:
	echo "# generated by $@" > .gdbrc
	echo "set environment ICU_PLUGINS=$(OUT)" >> .gdbrc
	echo "set environment $(LIBPATH_VAR)=$(OUT)/../$(PLUGLIB_INST)/lib:$(OUT)/$(PLUGLIB)/lib" >> .gdbrc


check: check-icu2symver all testprog $(OUT)/icuplugins$(PLUGLIB_MAJ).txt
	ICU_PLUGINS=$(OUT) $(LIBPATH_VAR)=$(PLUGLIB_INST)/lib:out/$(PLUGLIB)/lib $(PLUGLIB_INST)/bin/icuinfo || ( echo "ICU broken." ; /bin/false )
	ICU_PLUGINS=$(OUT) $(LIBPATH_VAR)=$(PLUGLIB_INST)/lib:out/$(PLUGLIB)/lib $(PLUGLIB_INST)/bin/icuinfo -L || ( echo "Plugin broken." ; /bin/false )
	@for prog in $(TESTPROG) ; \
	do \
		echo "# ICU_PLUGINS=$(OUT) $(LIBPATH_VAR)=$(PLUGLIB_INST)/lib:out/$(PLUGLIB)/lib out/$(PLUGLIB)/bin/$${prog}" ; \
		ICU_PLUGINS=$(OUT) $(LIBPATH_VAR)=$(PLUGLIB_INST)/lib:out/$(PLUGLIB)/lib out/$(PLUGLIB)/bin/$${prog} || ( echo "$${prog} failed." ; exit 1 ) ; \
	done

install-check: install-plugs
	$(LIBPATH_VAR)=$(PLUGLIB_INST)/lib $(PLUGLIB_INST)/bin/icuinfo -L || ( echo "Plugin broken." ; /bin/false )
	@for prog in $(TESTPROG) ; \
	do \
		echo "# $${prog}" ; \
		$(LIBPATH_VAR)=$(PLUGLIB_INST)/lib out/$(PLUGLIB)/bin/$${prog} || ( echo "$${prog} against installed failed." ; exit 1 ) ; \
	done

$(OUT)/$(PLUGLIB)/bin/$(TESTPROG):  $(GLOUT)/$(PLUGLIB)/obj-$(OK) $(OUT)/$(PLUGLIB)/provider_version.h $(ALLDIRS)
	@for prog in $(TESTPROG) ; \
	do \
		${call ECHO_COMPILE,$(shell $(PLUGLIB_ICU_CONFIG) --cxx --cxxflags --cppflags --ldflags) -o $(OUT)/$(PLUGLIB)/bin/$${prog} -I$(OUT)/$(PLUGLIB) $(TESTPROG_XTRA_OPTS) $(srcdir)/$${prog}.cpp} || exit 1 ; \
	done

$(OUT)/%/lib/$(PLUGLIB_NAME):  $(GLOUT)/%/obj-$(OK) $(PLUG_EXTRA_DEPS)
	-mkdir -p $(OUT)/$*/lib
	$(shell $(PLUGLIB_ICU_CONFIG) --cxx --cxxflags --cppflags --ldflags) $(PLUG_EXTRA_LDFLAGS) -shared -o $@ $(GLOUT)/$*/*.o $(PLUGLIB_AVAILABLE_DATA) $(PLUGLIB_AVAILABLE_STATICS)

#	cp $@ $(C_INS)/r$*/lib/

$(OUT)/%/lib/$(LIBPROVIDER):  $(GLOUT)/%/obj-$(OK)
	-mkdir -p $(OUT)/$*/lib
	echo copy includes
	ar -rv $@ $(GLOUT)/$*/*.o
	ranlib $@

## @@ build glu rename files
glurens: $(GLUREN_ICU)

# build single gluren.h 
$(BUILD)/%/$(GLUREN_H): $(BUILD)/%/ok Makefile makegluren.sh
	-mkdir -p $(BUILD)/$*/$(GLUREN)
	env SRC="$(SRC)" SOURCE="$(SOURCE)" GLUE="$(GLUE)" ./makegluren.sh $@ $*
	@fgrep -q OICU_ $@ || ( echo "Error: $@: no OICU_ symbols found. Removing gluren.h " ; rm -f $@ ; exit 1)

$(BUILD)/%/config/$(ICU_CONFIG): $(BUILD)/%/config/$(ICU_CONFIG_ORIG) icu-config.sed $(BUILD)/%/ok 
	sed -f icu-config.sed < $(BUILD)/$*/config/$(ICU_CONFIG_ORIG) >  $(BUILD)/$*/config/$(ICU_CONFIG) && chmod a+rx  $(BUILD)/$*/config/$(ICU_CONFIG)

# perform installation
$(INST)/%/ok: $(BUILD)/%/ok
	echo $(SUBHEAD) $* "(backend) installing..."
	echo "INST " "[" $* "]"
	mkdir -p $(INST)/$*
	-$(call LOG_COMPILE,$(BUILD)$*/ok.log,$(MAKE) $(MOPTS) -C $(BUILD)/$* install $(MAKE_INSTALL_XTRA_OPTS))
	touch $@

# unpack ICU and patch
$(SRC)/%/ok: $(ICUS)/icu4c-%-src.tgz
	-[ -d $(SRC)/$* ] && rm -rf $(SRC)/$*
	@echo $(SUBHEAD) $* "(backend) unpacking..."
	mkdir -p $(SRC)/$*
	$(call LOG_COMPILE,$(SRC)/$*/unpack.log,( cd $(SRC)/$* && gunzip -d < $(TOP)/$(ICUS)/icu4c-$*-src.tgz | tar xvfp - ))
	([ -f $(DOT)/patch/$* ] && patch -d $(SRC)/$* -p1 < $(DOT)/patch/$*) || true
	touch $@

# config 
$(BUILD)/%/config.status: $(SRC)/%/ok
	@echo $(SUBHEAD) $* "(backend) configuring..."
	-[ -d $(BUILD)/$* ] && rm -rf $(BUILD)/$*
	mkdir -p $(BUILD)/$*
	$(call LOG_COMPILE,$(BUILD)/$*/configure.log,( cd $(BUILD)/$* && env CFLAGS="$(CFLAGS) $(PBLD_EXTRA_FLAGS)" CXXFLAGS="$(CXXFLAGS) $(PBLD_EXTRA_FLAGS)" $(shell $(SUPERCONF) $(TOP)/$(SRC)/$*/$(SOURCE) $*) $(CONFIGURE_OPTS) --srcdir=$(TOP)/$(SRC)/$*/$(SOURCE) --prefix=$(TOP)/$(INST)/$* ))

# build 
# note: regex had renaming problems, and spoof depends on regex. Nuke them.
$(BUILD)/%/ok: $(BUILD)/%/config.status
	@echo $(SUBHEAD) $* "(backend) building.."
	$(call LOG_COMPILE,$(BUILD)/$*/ok2.log,( $(MAKE)  $(MOPTS) -C $(BUILD)/$* all $(MAKE_XTRA_OPTS) ))
	@if [ ! -f $(BUILD)/$*/common/putil.$(SOBJ) ]; \
	then \
		echo re-unpacking $(BUILD)/$*/common ; \
		( cd $(BUILD)/$*/common && ar xf ../lib/libsicuuc.a || echo could not unpack ../lib/libsicuuc.a ) ; \
		echo re-unpacking $(BUILD)/$*/i18n ; \
		( cd $(BUILD)/$*/i18n && ar xf ../lib/libsicui18n.a || echo could not unpack ../lib/libsicui18n.a ) ; \
	fi
	@if [ ! -f $(BUILD)/$*/common/putil.$(SOBJ) ]; \
	then \
		echo ERROR: still could not find $(BUILD)/$*/common/putil.$(SOBJ) - build may fail.; \
	fi
	-rm -f $(BUILD)/$*/i18n/uregex*.* $(BUILD)/$*/i18n/uspoof*.*o*
	touch $@

info help:
	@echo $(SUBHEAD) Provider Build
	@echo "Targetting Provider against ICU $(PROVIDER_TARGET) ($(PLUGLIB), major $(PLUGLIB_MAJ))"
	@echo "Available plugins: $(PROVIDER_AVAILABLE) ($(PLUGLIB_AVAILABLE))"
	@echo "Available keywords:"
	@for ver in $(PROVIDER_AVAILABLE); do \
		echo "   ...@provider=icu"`echo $$ver | ./icu2symver.sh`; \
	done
	@echo "Plugin library will be $(PLUGLIB_NAME)"
	@echo
	@echo Available ICU tarballs:  $(ICU_TARBALLS)
	@echo Available ICU versions: $(shell echo $(ICU_TARBALLS_VERS) | tr '_' '.')

$(OUT)/$(PLUGLIB)/provider_version.h: Makefile.local Makefile $(ALLDIRS)
	@echo $(SUBHEAD) $(PLUGIN) "(plugin)" Generating $@ ..
	@echo "/* Generated file. */" > $@
	@echo "const char *provider_version[] = {" >> $@
	@for ver in $(PROVIDER_AVAILABLE); do \
		sym=`echo $$ver | ./icu2symver.sh` ; \
		echo Version $$ver, Symbol $$sym ; \
		echo "\"$$sym\"",  >> $@ ; \
	done
	@echo " }; " >> $@
	@echo "#define PROVIDER_COUNT (sizeof(provider_version)/sizeof(provider_version[0]))" >> $@
	@echo >> $@
	@echo $(SUBHEAD) $(PLUGIN) "(plugin)" Generated $@

#-- for the prototype
