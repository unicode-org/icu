#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
#   Copyright (C) 2014, International Business Machines
#   Corporation and others.  All Rights Reserved.

# Edit the following two lines to reflect the location of your ICU sources & build (if out-of-source)
ICU_HOME=$(HOME)/icu/icu/trunk/source
ICU_BUILD=$(ICU_HOME)

data: genregexcasing
	LD_LIBRARY_PATH=$(ICU_BUILD)/lib:$(ICU_BUILD)/stubdata ICU_DATA=$(ICU_BUILD)/data/out ./genregexcasing

genregexcasing: genregexcasing.cpp
	clang++ genregexcasing.cpp -std=c++17 -g -I $(ICU_HOME)/common -I $(ICU_HOME)/i18n -I $(ICU_HOME)/io -L$(ICU_BUILD)/lib -L$(ICU_BUILD)/stubdata -licuuc -licui18n -licudata -o genregexcasing
