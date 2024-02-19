#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
#*****************************************************************************
#
#   Copyright (C) 2006-2008, International Business Machines
#   Corporation and others.  All Rights Reserved.
#
#*****************************************************************************
#
#  FILE NAME : Makefile
#
#   Date        Name        Description
#   03/02/2006  grhoten     Creation.
#*****************************************************************************

## List of phony targets
.PHONY : all all-local install install-local clean clean-local	\
distclean distclean-local install-library install-headers dist	\
dist-local check check-local

## Clear suffix list
.SUFFIXES :

## List of standard targets
all: all-local
install: install-local
clean: clean-local
distclean : distclean-local
dist: dist-local
check: all check-local

ALL_TARGETS = displayLocaleConv testTimezone
#ICU_BASE = /home/build/Build_ICU_HP11iACC
#ICU_BASE = /home/build/Build_ICU_Solaris9CC
ICU_BASE = /home/build/Build_ICU_LinuxRHEL4.2
#ICU_BASE = /home/build/Build_ICU_MacOSX
#ICU_BASE = /home/build/Build_ICU_AIX5.2VA6

all-local: $(ALL_TARGETS)

displayLocaleConv: displayLocaleConv.c
	#aCC +DD64 -I$(ICU_BASE)/icu/source/common/ -L$(ICU_BASE)/icu/source/lib/ -licuuc -licudata -o $@ $<
	#CC -xtarget=ultra -xarch=v9 -I$(ICU_BASE)/icu/source/common/ -L$(ICU_BASE)/icu/source/lib/ -licuuc -licudata -o $@ $<
	gcc -I $(ICU_BASE)/icu/source/common/ -L$(ICU_BASE)/icu/source/lib/ -licuuc -licudata -o $@ $<
	#gcc -I $(ICU_BASE)/icu/source/common/ -L$(ICU_BASE)/icu/source/lib/ -licuuc -licudata -o $@ $<
	#xlc -q64 -I $(ICU_BASE)/icu/source/common/ -L$(ICU_BASE)/icu/source/lib/ -licuuc35 -licudata35 -o $@ $<

testTimezone: testTimezone.c
	#aCC +DD64 -I$(ICU_BASE)/icu/source/common/ -I$(ICU_BASE)/icu/source/i18n/ -L$(ICU_BASE)/icu/source/lib/ -licuuc -licudata -o $@ $<
	#CC -xtarget=ultra -xarch=v9 -I$(ICU_BASE)/icu/source/common/ -I$(ICU_BASE)/icu/source/i18n/ -L$(ICU_BASE)/icu/source/lib/ -licuuc -licudata -o $@ $<
	gcc -I $(ICU_BASE)/icu/source/common/ -I$(ICU_BASE)/icu/source/i18n/ -L$(ICU_BASE)/icu/source/lib/ -licuuc -licudata -o $@ $<
	#gcc -I $(ICU_BASE)/icu/source/common/ -I$(ICU_BASE)/icu/source/i18n/ -L$(ICU_BASE)/icu/source/lib/ -licuuc -licudata -o $@ $<
	#xlc -q64 -I $(ICU_BASE)/icu/source/common/ -I$(ICU_BASE)/icu/source/i18n/ -L$(ICU_BASE)/icu/source/lib/ -licuuc35 -licudata35 -o $@ $<


install-local: install-headers install-library

install-library: all-local

install-headers:

dist-local:

clean-local:
#	test -z "$(CLEANFILES)" || $(RMV) $(CLEANFILES)
	rm $(OBJECTS) $(ALL_TARGETS) $(SO_VERSION_DATA)

distclean-local: clean-local

check-local:


