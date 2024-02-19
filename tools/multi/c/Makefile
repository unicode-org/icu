#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2009-2013 IBM Corp. and Others. All Rights Reserved
# multicu/c makefile

all:
	@echo To build all ICUs:  $(MAKE) all-icus
	@echo "Using the -k option may be a good idea if some are not building properly."
	@echo ICUVERS: $(ICUVERS)

info:

#ICUCONF=sh ./configure
ICUCONF=$(MULTICU_ROOT)/c/superconf.sh
DOT=$(shell pwd)
MULTICU_ROOT=$(DOT)/..
include $(MULTICU_ROOT)/common/Makefile-multi.inc
-include Makefile.local

ICUVERS:=$(shell cd $(ICUS); ls icu4c*.tgz | cut -d- -f2 | sort | uniq | fgrep -v -f $(MULTICU_ROOT)/c/old-icu.txt)
ICU4JVERS:=$(shell cd $(ICUS); ls icu4j*.jar | cut -d- -f2 | cut -d. -f1 |  sort | uniq | fgrep -v -f $(MULTICU_ROOT)/j/old-icu4j.txt)

BLD=$(C_BLD)
INS=$(C_INS)
SRC=$(C_SRC)

ALLICUS=$(ICUVERS:%=$(BLD)/%/$(S_BLD)) $(ICUVERS:%=$(BLD)/r%/$(S_BLD))
RICUS=$(ICUVERS:%=$(INS)/r%/$(S_INS))
IICUS=$(ICUVERS:%=$(INS)/%/$(S_INS)) $(RICUS)


APRECIOUS=$(ICUVERS:%=$(BLD)/%/$(S_SRC)) $(ICUVERS:%=$(BLD)/r%/$(S_SRC)) $(ICUVERS:%=$(BLD)/%/$(S_BLD)) $(ICUVERS:%=$(INS)/%/$(S_INS)) $(ICUVERS:%=$(BLD)/r%/$(S_BLD)) $(ICUVERS:%=$(INS)/r%/$(S_INS))

.PRECIOUS: $(APRECIOUS)

tehprecious:
	echo $(APRECIOUS)

$(M_TMP) $(BLD) $(INS):
	mkdir $(M_TMP)
	mkdir $(BLD) $(SRC)  $(INS)

$(INS)/%/$(S_INS): $(BLD)/%/$(S_BLD)
	@if [ -f $(BLD)/$*/build.err ]; then echo '###' Error for $* stored in $(BLD)/$*/build.err - remove if you want me to retry; false;  fi
	( make $(MAKE_OPTS) -C $(BLD)/$*/icu/source 2>&1 all install | tee $(BLD)/$*/build.log ) || ( mv $(BLD)/$*/build.log $(BLD)/$*/build.err ; false )
	touch -c $@

$(INS)/r%/$(S_INS): $(BLD)/r%/$(S_BLD)
	@if [ -f $(BLD)/r$*/build.err ]; then echo '###' Error for $* stored in $(BLD)/r$*/build.err - remove if you want me to retry; false;  fi
	( make $(MAKE_OPTS) -C $(BLD)/r$*/icu/source 2>&1 all install | tee $(BLD)/r$*/build.log ) || ( mv $(BLD)/r$*/build.log $(BLD)/r$*/build.err ; false )
	touch -c $@

$(BLD)/r%/$(S_SRC): $(ICUS)/icu4c-%-src.tgz
	-mv $(BLD)/r$* $(BLD)/r$*.old
	-( rm -rf ./$(BLD)/r$*.old& )
	mkdir -p $(BLD)/r$*
	( cd $(BLD)/r$* ; gunzip -d < $^ | tar xfp - )
	([ -f $(DOT)/patch/$* ] && patch -d $(BLD)/r$* -p1 < $(DOT)/patch/$*) || true
	touch -c $@

$(BLD)/%/$(S_SRC): $(ICUS)/icu4c-%-src.tgz
	-mv $(BLD)/$* $(BLD)/$*.old
	-( rm -rf ./$(BLD)/$*.old& )
	mkdir -p $(BLD)/$*
	( cd $(BLD)/$* ; gunzip -d < $^ | tar xfp - )
	([ -f $(DOT)/patch/$* ] && patch -d $(BLD)/$* -p1 < $(DOT)/patch/$*) || true
	touch -c $@


$(BLD)/r%/$(S_BLD): $(BLD)/r%/$(S_SRC)
	-mkdir -p $(INS)
	-chmod a+rx $(BLD)/r$*/icu/source/configure $(BLD)/r$*/icu/source/runConfigureICU
	( cd $(BLD)/r$*/icu/source ; env CPPFLAGS="$(XTRA_RICU_CPPFLAGS)" `$(ICUCONF) "$(BLD)/r$*/icu/source" "$*"` --enable-renaming --enable-shared --disable-static --enable-release --disable-debug --prefix=$(INS)/r$* )
	touch -c $@

$(BLD)/%/$(S_BLD): $(BLD)/%/$(S_SRC)
	-mkdir -p $(INS)
	-chmod a+rx $(BLD)/$*/icu/source/configure $(BLD)/$*/icu/source/runConfigureICU
	( cd $(BLD)/$*/icu/source ; env CPPFLAGS="$(XTRA_ICU_CPPFLAGS)" `$(ICUCONF) "$(BLD)/$*/icu/source" "$*"` --disable-renaming --enable-shared --disable-static --enable-release --disable-debug --prefix=$(INS)/$* )
	touch -c $@


$(INS)/%/bin/$(TARGET): $(INS)/%/$(S_INS) $(SRCS)
# 	Don't delete the built version.
#	-rm -rf ./$(BLD)/$*/tmp
	mkdir ./$(BLD)/$*/tmp
	@for file in $(OBJECTS); \
	do \
		what=`basename $$file .o` ; \
		echo compiling $*/$$what ; \
		echo $(CXX) -c -o ./$(BLD)/$*/tmp/$$what.o $$what.cpp -I $(INS)/$*/include -I $(BLD)/$*/icu/source/tools/toolutil ; \
		$(CXX) -c -o ./$(BLD)/$*/tmp/$$what.o $$what.cpp -I $(INS)/$*/include -I $(BLD)/$*/icu/source/tools/toolutil ; \
	done
	$(CXX) -o $@ $(OBJECTS:%.o=./$(BLD)/$*/tmp/%.o)  -licudata -licuuc -licutu -licui18n -L$(INS)/$*/lib 

foo:
	echo $(ALLICUS)

allicus: $(BLD) $(ALLICUS)


all-icus: iicus

iicus: $(BLD) $(IICUS)

ricus: $(RICUS)
