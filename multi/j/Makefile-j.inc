# Copyright (c) 2008-2009 IBM Corp. and Others. All Rights Reserved

include $(MULTICU_ROOT)/Makefile-multi.inc
ICUS=$(MULTICU_ROOT)/icus
J_GOOD:=$(shell [ -d $(ICUS) ] && (cd $(ICUS) ;  ls -d icu4j-*.jar | cut -d- -f2 | cut -d. -f1))
#J_GOOD=$(ICU4JVERS:%=$(ICUS)/icu4j-%.jar)


