#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2009 IBM Corp. and Others. All Rights Reserved

C_ROOT=$(MULTICU_ROOT)/c
J_ROOT=$(MULTICU_ROOT)/j
M_TMP=$(MULTICU_ROOT)/tmp
C_INS=$(M_TMP)/ins
C_BLD=$(M_TMP)/build
C_SRC=$(M_TMP)/src

# C defs
S_INS=include/unicode/utypes.h
S_SRCDIR=icu/source
S_BLD=$(S_SRCDIR)/config.status
S_SRC=$(S_SRCDIR)/configure

-include $(MULTICU_ROOT)/common/Makefile-local.inc

ICUS=$(MULTICU_ROOT)/packages

TOP=$(shell pwd)

# J defs
