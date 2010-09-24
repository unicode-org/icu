#
# Copyright (C) 2010 IBM Corporation and Others, All Rights Reserved.
#

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
INST_ICU=$(ICU_TARBALLS_VERS:%=$(INST)/%/$(OK))
BUILD_ICU=$(ICU_TARBALLS_VERS:%=$(BUILD)/%/$(OK))
GLOUT_ICU=$(ICU_TARBALLS_VERS:%=$(GLOUT)/%/$(OK))
SRC_ICU=$(ICU_TARBALLS_VERS:%=$(SRC)/%/$(OK))
# gluren.h files for each version
GLUREN_ICU=$(ICU_TARBALLS_VERS:%=$(BUILD)/%/$(GLUREN_H))
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
GLUE_CFLAGS=-I$(INCGLUE)

# icu-config switches for compiling: C and C++
ICU_CONFIG_COMMON=--cppflags
# icu-config switches for C
ICU_CONFIG_CC=--cc $(ICU_CONFIG_COMMON) --cflags 
# icu-config switches for C++
ICU_CONFIG_CXX=--cxx $(ICU_CONFIG_COMMON) --cxxflags

## @build All installed ICUs
ii: $(GLOUT_ICU)

# Don't delete these files
.PRECIOUS: $(INST_ICU) $(BUILD_ICU) $(SRC_ICU) $(ICU_TARBALLS_VERS:%=$(BUILD)/%/config.status) $(ICU_TARBALLS_VERS:%=$(GLOUT)/%/obj-$(OK))  $(ICU_TARBALLS_VERS:%=$(BUILD)/%/config/$(ICU_CONFIG))

# cleanup
clean:
	-rm -rf $(ALLDIRS)
	-mkdir $(ALLDIRS)

# create all empty work dirs
$(ALLDIRS):
	-mkdir -p $(@)

# build the glue objects
$(GLOUT)/%/obj-$(OK): $(GLOUT) glurens $(INST)/%/$(OK)  $(BUILD)/%/config/$(ICU_CONFIG)
	-mkdir $(GLOUT)/$*
#	$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$* -o $(GLOUT)/$*/gl_be_c_$*.o $(GL_BE_C:%.c=$(GLUE)/%.c)
#	$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$* -o $(GLOUT)/$*/gl_be_cxx_$*.o $(GL_BE_CXX:%.cpp=$(GLUE)/%.cpp)
	-rm -rf $(GLOUT)/$*/include
	mkdir -p $(GLOUT)/$*/include/icuglue/include
# for each version..  build all OTHER FE files
	for ver in $(ICU_TARBALLS_VERS) ; \
	do \
		echo building $$ver for $* ; \
		if [ ! "$$ver" = "$*" ]; \
		then \
			echo $(GLOUT)/$*/gl_fe_c_$${ver}_x_$*.o \
			$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -c  -I$(TOP)/$(BUILD)/$$ver/gluren/include -DICUGLUE_VER=$$ver "-DICUGLUE_VER_STR=\"$$ver\"" "-DGLUE_SYM(x)=glue ## x ## $$ver" -o $(GLOUT)/$*/gl_fe_c_$${ver}_x_$*.o $(GL_FE_C:%.c=$(GLUE)/%.c) $(XOPTS) ;  \
			$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -c -I$(TOP)/$(BUILD)/$$ver/gluren/include -DICUGLUE_VER=$$ver "-DICUGLUE_VER_STR=\"$$ver\"" "-DGLUE_SYM(x)=glue ## x ## $$ver" -o $(GLOUT)/$*/gl_fe_cxx_$${ver}_x_$*.o $(GL_FE_CXX:%.cpp=$(GLUE)/%.cpp) $(XOPTS) ; \
			echo " GLUE_VER( $$ver ) " >> $(GLOUT)/$*/include/icuglue/glver.h; \
		fi \
	done
# build 'this version' FE files
	@echo Building FE for $*
	$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -I$(GLOUT)/$*/include -c -o $(GLOUT)/$*/gl_fe_c_$*.o $(GL_FE_C:%.c=$(GLUE)/%.c) $(XOPTS)
	$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -I$(GLOUT)/$*/include -c -o $(GLOUT)/$*/gl_fe_cxx_$*.o $(GL_FE_CXX:%.cpp=$(GLUE)/%.cpp) $(XOPTS)
# build 'this version' BE files (disabled for now)
#	$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CC)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$* -o $(GLOUT)/$*/gl_be_c_$*.o $(GL_BE_C:%.c=$(GLUE)/%.c)
#	$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CXX)) $(GLUE_CFLAGS) -c -DICUGLUE_VER=$* -o $(GLOUT)/$*/gl_be_cxx_$*.o $(GL_BE_CXX:%.cpp=$(GLUE)/%.cpp)
	touch $@

# for now, glue is OK when the objects are OK.
# TODO: replace this with build of a specific lib.
$(GLOUT)/%/$(OK): $(GLOUT)/%/obj-$(OK)
	touch $@

PROVIDER=provider
LIBPROVIDER=lib$(PROVIDER).a

#libs: $(ICU_TARBALLS_VERS:%=$(OUT)/%/lib/$(LIBPROVIDER))

PLUGLIB=4_4_1
PLUGLIB_MAJ=44
PLUGLIB_ICU_CONFIG=$(C_INS)/r$(PLUGLIB)/bin/icu-config
PLUGLIB_SO=$(shell $(PLUGLIB_ICU_CONFIG) --so)
PLUGLIB_NAME=libicuprov.$(PLUGLIB_MAJ).$(PLUGLIB_SO)
#PLUGLIB_MAJ=$(echo $(PLUGLIB) | cut -d_ -f-1 | tr -d _ )
#GLOUT_ICU=$(ICU_TARBALLS_VERS:%=$(GLOUT)/%/$(OK))
PLUGLIB_TARGETS=3_8_1 4_2_0_1
PLUGLIB_TARGET_DATA=$(PLUGLIB_TARGETS:%=$(BUILD)/%/data/out/tmp/*.o)
PLUGLIB_TARGET_STATICS=$(PLUGLIB_TARGETS:%=$(BUILD)/%/[ci]*/*.ao)
PLUGLIB_OUTLIBS=$(PLUGLIB:%=$(OUT)/%/lib/$(PLUGLIB_NAME))



plugs: $(PLUGLIB_OUTLIBS) $(OUT)/$(PLUGLIB_MAJ:%=icuplugins%.txt)

install-plugs: $(C_INS)/r$(PLUGLIB)/lib/$(PLUGLIB_NAME) $(C_INS)/r$(PLUGLIB)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt
	echo "Plugins installed for testing."

#TODO: should be 'install' not 'cp'
$(C_INS)/r$(PLUGLIB)/lib/$(PLUGLIB_NAME) $(C_INS)/r$(PLUGLIB)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt: $(PLUGLIB_OUTLIBS)
	cp $(PLUGLIB_OUTLIBS) $(C_INS)/r$(PLUGLIB)/lib
	echo "$(PLUGLIB_NAME)	coll_provider_plugin" > $(C_INS)/r$(PLUGLIB)/lib/icu/icuplugins$(PLUGLIB_MAJ).txt

$(OUT)/icuplugins$(PLUGLIB_MAJ).txt: $(PLUGLIB_OUTLIBS)
	echo "$^	coll_provider_plugin" > $@

TESTPROG=coldiff

testprog: $(PLUGLIB:%=$(OUT)/%/bin/$(TESTPROG))

check: testprog $(OUT)/icuplugins$(PLUGLIB_MAJ).txt
	ICU_PLUGINS=$(OUT) DYLD_LIBRARY_PATH=$(C_INS)/r$(PLUGLIB)/lib $(C_INS)/r$(PLUGLIB)/bin/icuinfo || ( echo "ICU broken." ; /bin/false )
	ICU_PLUGINS=$(OUT) DYLD_LIBRARY_PATH=$(C_INS)/r$(PLUGLIB)/lib $(C_INS)/r$(PLUGLIB)/bin/icuinfo -L || ( echo "Plugin broken." ; /bin/false )
	ICU_PLUGINS=$(OUT) DYLD_LIBRARY_PATH=$(C_INS)/r$(PLUGLIB)/lib out/$(PLUGLIB)/bin/coldiff || ( echo "coldiff failed." ; /bin/false )

install-check: install-plugs
	DYLD_LIBRARY_PATH=$(C_INS)/r$(PLUGLIB)/lib $(C_INS)/r$(PLUGLIB)/bin/icuinfo -L || ( echo "Plugin broken." ; /bin/false )
	DYLD_LIBRARY_PATH=$(C_INS)/r$(PLUGLIB)/lib out/$(PLUGLIB)/bin/coldiff || ( echo "coldiff failed." ; /bin/false )

#	$(shell $(BUILD)/$*/config/$(ICU_CONFIG) $(ICU_CONFIG_CXX))

$(OUT)/%/bin/$(TESTPROG): $(TESTPROG).cpp
	-mkdir -p $(OUT)/$*/bin
	$(shell $(PLUGLIB_ICU_CONFIG) --cxx --cxxflags --cppflags --ldflags) -o $@ $^

#$(GLOUT)/$%/obj-$(OK)
$(OUT)/%/lib/$(PLUGLIB_NAME): 
	-mkdir -p $(OUT)/$*/lib
	$(shell $(PLUGLIB_ICU_CONFIG) --cxx --cxxflags --cppflags --ldflags) -dynamiclib -exported_symbols_list glue/export.txt -dynamic -o $@ $(GLOUT)/$*/*.o $(PLUGLIB_TARGET_DATA) $(PLUGLIB_TARGET_STATICS)
	cp $@ $(C_INS)/r$*/lib/

$(OUT)/%/lib/$(LIBPROVIDER):  $(GLOUT)/%/obj-$(OK)
	-mkdir -p $(OUT)/$*/lib
	echo copy includes
	ar -rv $@ $(GLOUT)/$*/*.o
	ranlib $@


#	-[ -d $(SRC)/$* ] && rm -rf $(SRC)/$*

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
	( cd $(BUILD)/$* && $(shell $(SUPERCONF) $(TOP)/$(SRC)/$*/$(SOURCE) $*) $(CONFIGURE_OPTS) --
srcdir=$(TOP)/$(SRC)/$*/$(SOURCE) --prefix=$(TOP)/$(INST)/$* )

# build 
$(BUILD)/%/ok: $(BUILD)/%/config.status
	@echo "Building" $*
	( $(MAKE)  $(MOPTS) -C $(BUILD)/$* all $(MAKE_XTRA_OPTS) )
	touch $@

#	( cd $(BUILD_ICU)/% ; echo tar xvfpz $(ICUS)/icu4c-$*-src.tgz )
#	touch $@

# 
help:
	@echo ICU Provider Build
	@echo Available ICU tarballs:  $(ICU_TARBALLS)
	@echo Available ICU versions: $(ICU_TARBALLS_VERS)

# from makefoo:
#EYF+=g++  -g  -dead_strip -o foo glout/4_2_0_1/*.o -Linst/4_2_0_1/lib/ -Lbuild/4_2_0_1/data/out/ -lsicuuc -lsicui18n -licudt42l build/3_8_1/*/*.ao build/4_0_1/*/*.ao build/4_0_1/data/out/tmp/icudt40l_dat.o build/3_8_1/data/out/tmp/icudt38l_dat.o 



# backup
#arch:
#	cd .. ; tar cvfpj MI$$$$.tar.bz2 $(DIR)/Makefile $(DIR)/superconf.sh $(DIR)/glue $(DIR)/makefoo.sh $(DIR)/coldiff.cpp

#-- for the prototype

