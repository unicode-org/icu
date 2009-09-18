# Copyright (c) 2008-2009 IBM Corp. and Others. All Rights Reserved

C_ROOT=$(MULTICU_ROOT)/c
J_ROOT=$(MULTICU_ROOT)/j
M_TMP=$(MULTICU_ROOT)/tmp
C_INS=$(M_TMP)/ins
C_BLD=$(M_TMP)/build
C_SRC=$(M_TMP)/src

# C defs
S_INS=include/unicode/utypes.h
S_BLD=icu/source/config.status
S_SRC=icu/source/configure

-include $(MULTICU_ROOT)/common/Makefile-local.inc

ICUS=$(MULTICU_ROOT)/packages


# J defs
