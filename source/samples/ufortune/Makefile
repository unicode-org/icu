# Copyright (c) 2001 IBM, Inc. and others

#
# File
#    icu/source/samples/ufortune/Makefile
#
# Usage:
#  - configure and build ICU [see the docs] .. use "--prefix=" something
#     (I  used --prefix=/home/srl/III )
# 
#  - do 'make install' of icu 
#
#  - change the following line to point to the $(prefix) that 
#    was used (will look for $(prefix)/lib/icu/Makefile.inc )
#      OR 
#   set the variable ICU_PREFIX to point at $(prefix)
#  
#  - do 'gmake' in this directory
#  - do 'gmake check' to run the sample.
#

ICU_DEFAULT_PREFIX=/export/andy/icui

ifeq ($(strip $(ICU_PREFIX)),)
  ICU_PREFIX=$(ICU_DEFAULT_PREFIX)
endif

ICU_INC=$(ICU_PREFIX)/lib/icu/Makefile.inc
include $(ICU_INC)

# Name of your target
TARGET=ufortune

# All object files (C or C++)
OBJECTS=ufortune.o

SUBDIRS = resources

CLEANFILES=*~ $(TARGET).out

DEPS=$(OBJECTS:.o=.d)

# For ufortune, INVOKE needs current directory on the lib path to pick up
#  the resources .so.  (For Solaris.  Not needed for Linux.  Others unknown.)
UFINVOKE = $(LDLIBRARYPATH_ENVVAR)=$(libdir):.:$$$(LDLIBRARYPATH_ENVVAR)

all: subdirs $(TARGET) 

.PHONY: all clean distclean check report subdirs $(SUBDIRS)

subdirs: $(SUBDIRS)
	
$(SUBDIRS):
	$(MAKE) -C $@

distclean clean: subdirs
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)
	-$(RMV) $(OBJECTS) $(TARGET)
	-$(RMV) $(DEPS)



$(TARGET): $(OBJECTS)
	$(LINK.c) -o $@ $(OBJECTS) $(ICULIBS) -L. -lfortune_resources$(ICULIBSUFFIX)


# Make check: simply runs the sample, logged to a file
check: $(TARGET)
	$(UFINVOKE) $(TARGET) | tee $(TARGET).out

# Make report: creates a 'report file' with both source and sample run
report: $(TARGET).report

$(TARGET).report: check $(TARGET).c
	more $(TARGET).c $(TARGET).out > $@

$(ICU_INC):
	@echo "Please read the directions at the top of this file (Makefile)"
	@echo "Can't open $(ICU_INC)"
	@false

ifneq ($(MAKECMDGOALS),distclean)
-include $(DEPS)
endif
