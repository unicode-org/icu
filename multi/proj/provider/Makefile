#
# Copyright (C) 2010 IBM Corporation and Others, All Rights Reserved.
#
all: glurens ii plugs testprog

UNAME=$(shell uname)

ifeq ($(UNAME),Linux)
LIBPATH_VAR=LD_LIBRARY_PATH
PLUG_EXTRA_LDFLAGS=-fPIC
PBLD_EXTRA_FLAGS=-fPIC
GLUE_EXTRA_CFLAGS= -fPIC
PLUGLIB_SO=so
else
ifeq ($(UNAME),Darwin)
LIBPATH_VAR=DYLD_LIBRARY_PATH
PLUG_EXTRA_LDFLAGS=-exported_symbols_list glue/export.txt -dynamiclib -dynamic
PLUGLIB_SO=dylib
else
$(warning **********************************)
$(warning this script may not know how to handle your $(UNAME) system)
endif
endif


PROVIDER=provider
LIBPROVIDER=lib$(PROVIDER).a

#libs: $(ICU_TARBALLS_VERS:%=$(OUT)/%/lib/$(LIBPROVIDER))

PLUGLIB=$(shell echo $(PROVIDER_TARGET)  | tr '.' '_' )
PLUGLIB_MAJ=$(shell echo $(PROVIDER_TARGET) | cut -d. -f1-2 | tr -d '.' )
PLUGLIB_ICU_CONFIG=$(C_INS)/r$(PLUGLIB)/bin/icu-config
#PLUGLIB_SO=$(shell $(PLUGLIB_ICU_CONFIG) --so 2>/dev/null )
PLUGLIB_NAME=libicuprov.$(PLUGLIB_MAJ).$(PLUGLIB_SO)
#PLUGLIB_MAJ=$(echo $(PLUGLIB) | cut -d_ -f-1 | tr -d _ )
#GLOUT_ICU=$(ICU_TARBALLS_VERS:%=$(GLOUT)/%/$(OK))
PLUGLIB_AVAILABLE=$(shell echo $(PROVIDER_AVAILABLE) | tr '.' '_')
PLUGLIB_AVAILABLE_DATA=$(PLUGLIB_AVAILABLE:%=$(BUILD)/%/data/out/tmp/*.o)
PLUGLIB_AVAILABLE_STATICS=$(PLUGLIB_AVAILABLE:%=$(BUILD)/%/[ci]*/*.ao)
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
ALLDIRS=$(INST) $(BUILD) $(SRC) $(GLOUT) $(OUT)
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

# Munged source names: list of C, C++, O etc files for Backend
GL_BE_C=$(shell cd $(GLUE) ; ls *_be.c)
GL_BE_CXX=$(shell cd $(GLUE) ; ls *_be.cpp)
GL_BE_O=$(GL_BE_C:%.c=%.o) $(GL_BE_CPP:%.cpp=%.o)
# Munged source names: list of C, C++, O etc files for Frontend
GL_FE_C=$(shell cd $(GLUE) ; ls *_fe.c)
GL_FE_CXX=$(shell cd $(GLUE) ; ls *_fe.cpp)
GL_FE_O=$(GL_FE_C:%.c=%.o) $(GL_FE_CPP:%.cpp=%.o)
# C flags used in Glue compilation
GLUE_CFLAGS=-I$(INCGLUE) $(GLUE_EXTRA_CFLAGS)


# icu-config switches for compiling: C and C++
ICU_CONFIG_COMMON=--cppflags
# icu-config switches for C
ICU_CONFIG_CC=--cc $(ICU_CONFIG_COMMON) --cflags 
# icu-config switches for C++
ICU_CONFIG_CXX=--cxx $(ICU_CONFIG_COMMON) --cxxflags

include Makefile.local


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
	$(MAKE) -C ../../c ICUVERS=$(PLUGLIB) ricus

# build the glue objects for TARGET
# used to be %/* instead of $(PLUGLIB) - now, wire it down to pluglib.
$(GLOUT)/$(PLUGLIB)/obj-$(OK): $(GLOUT) glurens  $(PLUGLIB_ICU_CONFIG)
	-mkdir $(GLOUT)/$(PLUGLIB)
#	$(shell $(BUILD)/$(PLUGLIB)/config/$(ICU_CONFIG) $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$(PLUGLIB) -o $(GLOUT)/$(PLUGLIB)/gl_be_c_$(PLUGLIB).o $(GL_BE_C:%.c=$(GLUE)/%.c)
#	$(shell $(BUILD)/$(PLUGLIB)/config/$(ICU_CONFIG) $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$(PLUGLIB) -o $(GLOUT)/$(PLUGLIB)/gl_be_cxx_$(PLUGLIB).o $(GL_BE_CXX:%.cpp=$(GLUE)/%.cpp)
	-rm -rf $(GLOUT)/$(PLUGLIB)/include
	mkdir -p $(GLOUT)/$(PLUGLIB)/include/icuglue/include
# for each version..  build all OTHER FE files
# TODO: check is unnecessary, not permitted.
# TODO: change to depend instead of for list. 
	for ver in $(PLUGLIB_AVAILABLE) ; \
	do \
		echo building $$ver for $(PLUGLIB) ; \
		if [ ! "$$ver" = "$(PLUGLIB)" ]; \
		then \
			echo $(GLOUT)/$(PLUGLIB)/gl_fe_c_$${ver}_x_$(PLUGLIB).o \
			$(shell $(PLUGLIB_ICU_CONFIG) --noverify $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -c -I$(TOP)/$(BUILD)/$$ver/gluren/include -DICUGLUE_VER=$$ver "-DICUGLUE_VER_STR=\"$$ver\"" "-DGLUE_SYM(x)=glue ## x ## $$ver" -o $(GLOUT)/$(PLUGLIB)/gl_fe_c_$${ver}_x_$(PLUGLIB).o $(GL_FE_C:%.c=$(GLUE)/%.c) $(XOPTS) ;  \
			$(shell $(PLUGLIB_ICU_CONFIG) --noverify $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -c -I$(TOP)/$(BUILD)/$$ver/gluren/include -DICUGLUE_VER=$$ver "-DICUGLUE_VER_STR=\"$$ver\"" "-DGLUE_SYM(x)=glue ## x ## $$ver" -o $(GLOUT)/$(PLUGLIB)/gl_fe_cxx_$${ver}_x_$(PLUGLIB).o $(GL_FE_CXX:%.cpp=$(GLUE)/%.cpp) $(XOPTS) ; \
			echo " GLUE_VER( $$ver ) " >> $(GLOUT)/$(PLUGLIB)/include/icuglue/glver.h; \
		fi \
	done
# build 'this version' FE files
	@echo Building FE for $(PLUGLIB)
	$(shell $(PLUGLIB_ICU_CONFIG) --noverify $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -I$(GLOUT)/$(PLUGLIB)/include -c -o $(GLOUT)/$(PLUGLIB)/gl_fe_c_$(PLUGLIB).o $(GL_FE_C:%.c=$(GLUE)/%.c) $(XOPTS)
	$(shell $(PLUGLIB_ICU_CONFIG) --noverify $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -I$(GLOUT)/$(PLUGLIB)/include -c -o $(GLOUT)/$(PLUGLIB)/gl_fe_cxx_$(PLUGLIB).o $(GL_FE_CXX:%.cpp=$(GLUE)/%.cpp) $(XOPTS)
# build 'this version' BE files (disabled for now)
#	$(shell $(BUILD)/$(PLUGLIB)/config/$(ICU_CONFIG) $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$(PLUGLIB) -o $(GLOUT)/$(PLUGLIB)/gl_be_c_$(PLUGLIB).o $(GL_BE_C:%.c=$(GLUE)/%.c)
#	$(shell $(BUILD)/$(PLUGLIB)/config/$(ICU_CONFIG) $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$(PLUGLIB) -o $(GLOUT)/$(PLUGLIB)/gl_be_cxx_$(PLUGLIB).o $(GL_BE_CXX:%.cpp=$(GLUE)/%.cpp)
	touch $@

plugs: $(PLUGLIB_OUTLIBS) $(OUT)/$(PLUGLIB_MAJ:%=icuplugins%.txt)

install-plugs: $(C_INS)/r$(PLUGLIB)/lib/$(PLUGLIB_NAME) $(C_INS)/r$(PLUGLIB)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt
	echo "Plugins installed for testing."

#TODO: should be 'install' not 'cp'
$(C_INS)/r$(PLUGLIB)/lib/$(PLUGLIB_NAME) $(C_INS)/r$(PLUGLIB)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt: $(PLUGLIB_OUTLIBS)
	cp $(PLUGLIB_OUTLIBS) $(C_INS)/r$(PLUGLIB)/lib
	echo "$(PLUGLIB_NAME)	coll_provider_plugin" > $(C_INS)/r$(PLUGLIB)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt

$(OUT)/icuplugins$(PLUGLIB_MAJ).txt: $(PLUGLIB_OUTLIBS)
	echo "$(PLUGLIB_NAME)	coll_provider_plugin" > $@

TESTPROG=coldiff

testprog: $(PLUGLIB:%=$(OUT)/%/bin/$(TESTPROG)) 

check: all testprog $(OUT)/icuplugins$(PLUGLIB_MAJ).txt
	ICU_PLUGINS=$(OUT) $(LIBPATH_VAR)=$(C_INS)/r$(PLUGLIB)/lib:out/$(PLUGLIB)/lib $(C_INS)/r$(PLUGLIB)/bin/icuinfo || ( echo "ICU broken." ; /bin/false )
	ICU_PLUGINS=$(OUT) $(LIBPATH_VAR)=$(C_INS)/r$(PLUGLIB)/lib:out/$(PLUGLIB)/lib $(C_INS)/r$(PLUGLIB)/bin/icuinfo -L || ( echo "Plugin broken." ; /bin/false )
	ICU_PLUGINS=$(OUT) $(LIBPATH_VAR)=$(C_INS)/r$(PLUGLIB)/lib:out/$(PLUGLIB)/lib out/$(PLUGLIB)/bin/coldiff || ( echo "coldiff failed." ; /bin/false )

install-check: install-plugs
	$(LIBPATH_VAR)=$(C_INS)/r$(PLUGLIB)/lib $(C_INS)/r$(PLUGLIB)/bin/icuinfo -L || ( echo "Plugin broken." ; /bin/false )
	$(LIBPATH_VAR)=$(C_INS)/r$(PLUGLIB)/lib out/$(PLUGLIB)/bin/coldiff || ( echo "coldiff failed." ; /bin/false )

$(OUT)/%/bin/$(TESTPROG): $(TESTPROG).cpp $(OUT)/$(PLUGLIB)/provider_version.h
	-mkdir -p $(OUT)/$*/bin
	$(shell $(PLUGLIB_ICU_CONFIG) --cxx --cxxflags --cppflags --ldflags) -o $@ -I$(OUT)/$(PLUGLIB) $(TESTPROG).cpp

$(OUT)/%/lib/$(PLUGLIB_NAME):  $(GLOUT)/%/obj-$(OK)
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
$(BUILD)/%/$(GLUREN_H): $(BUILD)/%/ok Makefile
	-mkdir -p $(BUILD)/$*/$(GLUREN)
	cat $(GLUE)/gluren-top.h > $@
	echo "#define GLUREN_VER" $* >> $@
	echo "#define GLUREN_TINY" $(shell echo $* | cut -d_ -f1-2) >> $@
	echo >> $@
	echo >> $@
	echo >> $@
	grep "^#define.*$(shell echo $* | cut -d_ -f1-2)$$" $(SRC)/$*/$(SOURCE)/common/unicode/urename.h   | fgrep -v '*' | \
		sed -e "s@^#define \([^ ]*\) \([^ ]*\)@#define OICU_\1 \2@" >> $@
	cat $(GLUE)/gluren-bottom.h >> $@


$(BUILD)/%/config/$(ICU_CONFIG): $(BUILD)/%/config/$(ICU_CONFIG_ORIG) icu-config.sed $(BUILD)/%/ok 
	sed -f icu-config.sed < $(BUILD)/$*/config/$(ICU_CONFIG_ORIG) >  $(BUILD)/$*/config/$(ICU_CONFIG) && chmod a+rx  $(BUILD)/$*/config/$(ICU_CONFIG)

# smiling now.
# perform installation
$(INST)/%/ok: $(BUILD)/%/ok
	echo "INST " "[" $* "]"
	mkdir -p $(INST)/$*
	-$(MAKE) $(MOPTS) -C $(BUILD)/$* install $(MAKE_INSTALL_XTRA_OPTS) 
	touch $@

# unpack ICU and patch
$(SRC)/%/ok: $(ICUS)/icu4c-%-src.tgz
	-[ -d $(SRC)/$* ] && rm -rf $(SRC)/$*
	@echo "Unpacking" $*
	mkdir -p $(SRC)/$*
	( cd $(SRC)/$* && tar xfpz $(TOP)/$(ICUS)/icu4c-$*-src.tgz )
	@echo Should patch $* here
	touch $@

# config 
$(BUILD)/%/config.status: $(SRC)/%/ok
	@echo "Configging" $*
	-[ -d $(BUILD)/$* ] && rm -rf $(BUILD)/$*
	mkdir -p $(BUILD)/$*
	( cd $(BUILD)/$* && env CFLAGS=$(PBLD_EXTRA_FLAGS) CXXFLAGS=$(PBLD_EXTRA_FLAGS) $(shell $(SUPERCONF) $(TOP)/$(SRC)/$*/$(SOURCE) $*) $(CONFIGURE_OPTS) --srcdir=$(TOP)/$(SRC)/$*/$(SOURCE) --prefix=$(TOP)/$(INST)/$* )

# build 
# note: regex had renaming problems, and spoof depends on regex. Nuke them.
$(BUILD)/%/ok: $(BUILD)/%/config.status
	@echo "Building" $*
	( $(MAKE)  $(MOPTS) -C $(BUILD)/$* all $(MAKE_XTRA_OPTS) )
	@if [ ! -f $(BUILD)/$*/common/putil.ao ]; \
	then \
		echo re-unpacking $(BUILD)/$*/common ; \
		( cd $(BUILD)/$*/common && ar xf ../lib/libsicuuc.a || echo could not unpack ../lib/libsicuuc.a ) ; \
		echo re-unpacking $(BUILD)/$*/i18n ; \
		( cd $(BUILD)/$*/i18n && ar xf ../lib/libsicui18n.a || echo could not unpack ../lib/libsicui18n.a ) ; \
	fi
	@if [ ! -f $(BUILD)/$*/common/putil.ao ]; \
	then \
		echo ERROR: still could not find $(BUILD)/$*/common/putil.ao - build may fail.; \
	fi
	-rm -f $(BUILD)/$*/i18n/uregex*.* $(BUILD)/$*/i18n/uspoof*.*o*
	touch $@

info help:
	@echo ICU Provider Build
	@echo "Targetting Provider against ICU $(PROVIDER_TARGET) ($(PLUGLIB), major $(PLUGLIB_MAJ))"
	@echo "Available plugins: $(PROVIDER_AVAILABLE) ($(PLUGLIB_AVAILABLE))"
	@echo "Available keywords:"
	@for ver in $(PROVIDER_AVAILABLE); do \
		echo "   ...@provider=icu"`echo $$ver | cut -d. -f1-2 | tr -d .`; \
	done
	@echo "Plugin library will be $(PLUGLIB_NAME)"
	@echo
	@echo Available ICU tarballs:  $(ICU_TARBALLS)
	@echo Available ICU versions: $(shell echo $(ICU_TARBALLS_VERS) | tr '_' '.')

$(OUT)/$(PLUGLIB)/provider_version.h: Makefile.local Makefile
	@echo Generating $@ ..
	@echo "/* Generated file. */" > $@
	@echo "const char *provider_version[] = {" >> $@
	@for ver in $(PROVIDER_AVAILABLE); do \
		echo '"'`echo $$ver | cut -d. -f1-2 | tr -d .`'",' >> $@ ; \
	done
	@echo " }; " >> $@
	@echo "#define PROVIDER_COUNT (sizeof(provider_version)/sizeof(provider_version[0]))" >> $@
	@echo >> $@

#-- for the prototype
