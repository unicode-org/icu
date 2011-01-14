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
COMMON_DATA_OBJ=udata.o udatamem.o umapfile.o udataswp.o ucmndata.o
COMMON_TRIE_OBJ=utrie2.o uchar.o ucase.o
COMMON_UTIL_OBJ=putil.o uhash.o ustr_cnv.o ustring.o umutex.o cmemory.o utf_impl.o uinvchar.o ustrfmt.o uenum.o cstring.o ucln_cmn.o uinit.o umath.o icuplug.o uarrsort.o utrace.o utypes.o
COMMON_UCNV_OBJ=ucnv.o ucnv2022.o ucnv_bld.o ucnv_cb.o ucnv_cnv.o ucnv_err.o ucnv_ext.o ucnv_io.o ucnv_lmb.o ucnv_set.o ucnv_u16.o ucnv_u32.o ucnv_u7.o ucnv_u8.o ucnvbocu.o ucnvdisp.o ucnvhz.o ucnvisci.o ucnvlat1.o ucnvmbcs.o ucnvscsu.o
COMMON_OBJ=$(COMMON_UCNV_OBJ) $(COMMON_UTIL_OBJ) $(COMMON_DATA_OBJ) $(COMMON_TRIE_OBJ)
ICU4C0_VERSION=$(VERSION)
endif




icu4c0:
ifeq ($(ICU4C0_VERSION),)
	@echo Error: do not know how to make ICU4C0 for $(VERSION)
	@/bin/false
else
	$(MKINSTALLDIRS) $(NEWLIBDIR)
	@echo Making ICU for C for $(ICU4C0_VERSION) in $(NEWLIBDIR)
	$(MAKE) clean
	$(MAKE) clean ICU4C0=1
	$(MAKE) all ICU4C0=1
	@echo 
	@echo ICU for C for $(ICU4C0_VERSION)
	@echo
	@echo The libraries in $(NEWLIBDIR)
	@echo ' are now for C only. Must do "make clean" in common to rebuild regular ICU.'
endif


ifneq ($(ICU4C0),)
OBJECTS=$(COMMON_OBJ)
#COMMON_SRC=$(COMMON_OBJ:%.o=$(ICU_COMMON)/%.c)

endif
