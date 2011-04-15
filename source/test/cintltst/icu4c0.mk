#
# Copyright (C) 2010-2011 IBM Corp
#
# version 1 1/13/2011
#
#
# Usage: 
#
#  1. Build ICU normally
#
#  2. as the LAST STEP,  cd into 'common/' and run 'make icu4c0'
#
#  4. Now, the libicuuc* libraries are C only, a subset of ICU.
#
#



include $(top_srcdir)/icu4c0.mk

ifeq ($(VERSION),4.4.2)
CINTLTST_OBJ=cintltst.o calltest.o cnormtst.o ccolltst.o
ICU4C0_VERSION=$(VERSION)
endif




icu4c0:
ifeq ($(ICU4C0_VERSION),)
	@echo Error: do not know how to make ICU4C0 for $(VERSION)
	@/bin/false
else
	$(MKINSTALLDIRS) $(NEWLIBDIR)
	@echo Making ICU for C for $(ICU4C0_VERSION) in $(NEWLIBDIR)
#	$(MAKE) clean
	$(MAKE) clean ICU4C0=1
	$(MAKE) all ICU4C0=1
	@echo 
	@echo ICU for C for $(ICU4C0_VERSION)
	@echo
	@echo The libraries in $(NEWLIBDIR)
	@echo ' are now for C only. Must do "make clean" in common to rebuild regular ICU.'
endif


ifneq ($(ICU4C0),)
OBJECTS=$(CINTLTST_OBJ)
#LIBS := $(LIBICUTOOLUTIL) $(LIBICUI18N) $(LIBICUUC) $(DEFAULT_LIBS)
LIBS =  $(LIBICUUC)  $(LIBCTESTFW) $(DEFAULT_LIBS)
#LIBS = $(LIBCTESTFW) $(LIBICUI18N) $(LIBICUTOOLUTIL) $(LIBICUUC) $(DEFAULT_LIBS) $(LIB_M)

#COMMON_SRC=$(COMMON_OBJ:%.o=$(ICU_COMMON)/%.c)

endif

-include icu4c0.local
