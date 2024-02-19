# -*- makefile -*-
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (c) 2008-2013 IBM Corp. and Others. All Rights Reserved
include $(MULTICU_ROOT)/common/Makefile-multi.inc
ICUS=$(MULTICU_ROOT)/packages
J_GOOD:=$(shell [ -d $(ICUS) ] && (cd $(ICUS) ;  ls -d icu4j-*.jar | cut -d- -f2 | cut -d. -f1))
#J_GOOD=$(ICU4JVERS:%=$(ICUS)/icu4j-%.jar)


