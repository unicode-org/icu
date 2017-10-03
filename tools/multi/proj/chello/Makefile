#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2009 IBM Corp. and Others. All Rights Reserved
# Makefile for multicu/sample/chello
# C/C++ hello

## Name of the app
TARGET=chello

## Sources list (C and C++ just to be difficult)
C_SOURCES=date.c
CXX_SOURCES=uprint.cpp
SRCS=$(C_SOURCES) $(CXX_SOURCES)
HEADERS=uprint.h

all:
	@echo To build and list "'" $(OUTFILES) "'"  in "'" $(C_GOOD) "'" use "$(MAKE) check"

## Setup multicu
MULTICU_ROOT=../../
include $(MULTICU_ROOT)/c/Makefile-c.inc

OUT=out

$(OUT):
	mkdir $(OUT)

## The output files.  Will result in:   out/3_8.txt  out/4_0.txt  etc.
OUTFILES=$(C_GOOD:%=$(OUT)/%.txt)

.PRECIOUS: $(C_CLEAN_TARGET)

## Generate a file
$(OUT)/%.txt: $(OUT) $(C_INS)/%/bin/$(TARGET)
	$(shell $(C_INS)/$*/bin/icu-config --invoke) $(C_INS)/$*/bin/$(TARGET)  > $@

## clean
clean:
	-rm -f $(C_CLEAN_TARGET)
	-rm -f ./$(OUT)/*
	-rmdir $(OUT)

## Just generate
outfiles: $(OUTFILES)

## Test: generate out files, and print them.
check: $(OUTFILES)
	@for file in $(OUTFILES); \
	do \
		echo; \
		sed -e "s%^%$$file: %g" < $$file; \
	done

