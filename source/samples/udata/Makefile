# Copyright (c) 2000 IBM, Inc. and others
# udata sample code $Revision: 1.2 $


# Usage:
#  - configure and build ICU [see the docs] .. use "--prefix=" something
#     (I  used --prefix=/home/srl/III )
# 
#  - do 'make install' of icu 
#
#  - change ICU_DEFAULT_PREFIX line to point to the $(prefix) that 
#    was used (will look for $(prefix)/share/icu/Makefile.inc )
#      OR 
#   set the variable ICU_PREFIX to point at $(prefix)
#  
#  - change ICU_DEFAULT_PATH line to point to the 'icu' directory
#    containing source, data, config, etc.. 
#      OR 
#   set the variable ICU_PATH to point at that path
#  
#  - do 'make' in this directory

ICU_DEFAULT_PREFIX=/home/srl/II
ICU_DEFAULT_PATH=~/icu

ifeq ($(strip $(ICU_PREFIX)),)
  ICU_INC=$(ICU_DEFAULT_PREFIX)/share/icu/Makefile.inc
else
  ICU_INC=$(ICU_PREFIX)/share/icu/Makefile.inc
endif


ifeq ($(strip $(ICU_PATH)),)
  ICU_PATH=$(ICU_DEFAULT_PATH)
endif

include $(ICU_INC)

# Name of your target
TARGET1=reader
TARGET2=writer

# All object files (C or C++)
OBJECTS1=reader.o
OBJECTS2=writer.o
OBJECTS=$(OBJECTS1) $(OBJECTS2)

CLEANFILES=*~ $(TARGET).out

DEPS=$(OBJECTS:.o=.d)

all: $(ICU_PATH)/source/tools/toolutil/uoptions.h $(TARGET1) $(TARGET2)

$(ICU_PATH)/source/tools/toolutil/uoptions.h:
	@echo "Please read the directions at the top of this file (Makefile)"
	@echo "Can't open $@ - check ICU_PATH"
	@false

# Only the writer needs these, actually.
CPPFLAGS += -I$(ICU_PATH)/source/tools/toolutil
LDFLAGS += -L$(ICU_PATH)/source/tools/toolutil -licu-toolutil


.PHONY: all clean distclean check report

distclean clean:
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)
	-$(RMV) $(OBJECTS) $(TARGET1) $(TARGET2)
	-$(RMV) $(DEPS)

# Can change this to LINK.c if it is a C only program
# Can add more libraries here. 
$(TARGET1): $(OBJECTS1)
	$(LINK.cc) -o $@ $^ $(ICULIBS)

$(TARGET2): $(OBJECTS2)
	$(LINK.cc) -o $@ $^ $(ICULIBS)

# Make check: simply runs the sample, logged to a file
check: $(TARGET1) $(TARGET2)
	$(TARGET1) | tee $(TARGET1).out
	$(TARGET2) | tee $(TARGET2).out

$(ICU_INC):
	@echo "Please read the directions at the top of this file (Makefile)"
	@echo "Can't open $(ICU_INC)"
	@false

ifneq ($(MAKECMDGOALS),distclean)
-include $(DEPS)
endif
