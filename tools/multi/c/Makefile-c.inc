# -*- makefile -*-
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2013 IBM Corp. and Others. All Rights Reserved

include $(MULTICU_ROOT)/common/Makefile-multi.inc

C_GOOD:=$(shell [ -d $(C_INS) ] && (cd $(C_INS) ; ls -d */$(S_INS) 2>/dev/null | cut -d/ -f1))
C_REL:=$(shell [ -d $(C_INS) ] && (cd $(C_INS) ; ls -d r*/$(S_INS) 2>/dev/null | cut -d/ -f1))

C_CLEAN_TARGET=$(C_GOOD:%=$(C_INS)/%/bin/$(TARGET))

SOURCES=$(C_SOURCES) $(CXX_SOURCES)
C_OBJECTS=$(C_SOURCES:%.c=%.o)
CXX_OBJECTS=$(CXX_SOURCES:%.cpp=%.o)
OBJECTS=$(C_OBJECTS) $(CXX_OBJECTS)
#DEBUGOPT=-g

# list of ICUs to be built	
ICU_TARBALLS=$(shell cd $(ICUS) ; ls icu4c-*-src.tgz)
# list of versions ( form "3_8  4_0" etc. )
ICU_TARBALLS_VERS=$(ICU_TARBALLS:icu4c-%-src.tgz=%)


$(C_INS)/%/bin/$(TARGET):	$(SOURCES) $(HEADERS)
	-rm -rf $(C_INS)/$*/tmp
	mkdir $(C_INS)/$*/tmp
ifneq (,$(CXX_OBJECTS))
	@for file in $(CXX_OBJECTS); \
	do \
		what=`basename $$file .o` ; \
		echo tools=$(C_BLD)/$*/icu/source/tools/toolutil ; \
		echo compiling '(C++)' $*/$$what ; \
		echo `$(C_INS)/$*/bin/icu-config --cxx` `$(C_INS)/$*/bin/icu-config --cxxflags` $(DEBUGOPT)  -c -o $(C_INS)/$*/tmp/$$what.o $$what.cpp -I $(C_INS)/$*/include -I $(C_BLD)/$*/icu/source/tools/toolutil ; \
		`$(C_INS)/$*/bin/icu-config --cxx` `$(C_INS)/$*/bin/icu-config --cxxflags` $(DEBUGOPT)  -c -o $(C_INS)/$*/tmp/$$what.o $$what.cpp -I $(C_INS)/$*/include -I $(C_BLD)/$*/icu/source/tools/toolutil  || \
		$(CXX) $(DEBUGOPT) -c -o $(C_INS)/$*/tmp/$$what.o $${what}.cpp -I $(C_INS)/$*/include -I $(C_BLD)/$*/icu/source/tools/toolutil ; \
	done
endif
ifneq (,$(C_OBJECTS))
	@for file in $(C_OBJECTS); \
	do \
		what=`basename $$file .o` ; \
		echo compiling '(C  )' $*/$$what ; \
		$(CC) $(DEBUGOPT) -c -o $(C_INS)/$*/tmp/$$what.o $${what}.c -I $(C_INS)/$*/include -I $(C_BLD)/$*/icu/source/tools/toolutil ; \
	done
endif
	@echo linking $*/$$what
	@echo `$(C_INS)/$*/bin/icu-config --cxx` `$(C_INS)/$*/bin/icu-config --ldflags `  $(DEBUGOPT) -o $@ $(OBJECTS:%.o=$(C_INS)/$*/tmp/%.o)  -licudata -licuuc -licutu -licui18n -licuio -L$(C_INS)/$*/lib
	@`$(C_INS)/$*/bin/icu-config --cxx` -Wl,--verbose `$(C_INS)/$*/bin/icu-config --ldflags `  $(DEBUGOPT) -o $@ $(OBJECTS:%.o=$(C_INS)/$*/tmp/%.o)  -licudata -licuuc -licutu -licui18n -licuio -L$(C_INS)/$*/lib || \
	 $(CXX) $(DEBUGOPT) -o $@ $(OBJECTS:%.o=$(C_INS)/$*/tmp/%.o)  -licudata -licuuc -licutu -licui18n -licuio -L$(C_INS)/$*/lib 


