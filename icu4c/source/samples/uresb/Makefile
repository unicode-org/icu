# Copyright (c) 2000 IBM, Inc. and others
# conversion sample code $Revision: 1.2 $


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

ICU_DEFAULT_PREFIX=/home/weiv/build/workshop
ICU_DEFAULT_PATH=/home/weiv/dev/icu

ifeq ($(strip $(ICU_PREFIX)),)
  ICU_INC=$(ICU_DEFAULT_PREFIX)/share/icu/Makefile.inc
  GENRB = $(ICU_DEFAULT_PREFIX)/sbin/genrb
else
  ICU_INC=$(ICU_PREFIX)/share/icu/Makefile.inc
  GENRB = $(ICU_PREFIX)/sbin/genrb
endif

ifeq ($(strip $(ICU_PATH)),)
  ICU_PATH=$(ICU_DEFAULT_PATH)
endif

GENRBOPT = -s. -d.

include $(ICU_INC)

# Name of your target
TARGET=uresb
RESOURCES = root.res en.res sr.res

# All object files (C or C++)
OBJECTS=uresb.o

CLEANFILES=*~ $(TARGET).out

DEPS=$(OBJECTS:.o=.d)

all: $(ICU_PATH)/source/tools/toolutil/uoptions.h $(GENRB) $(RESOURCES) $(TARGET) 

$(ICU_PATH)/source/tools/toolutil/uoptions.h:
	@echo "Please read the directions at the top of this file (Makefile)"
	@echo "Can't open $@ - check ICU_PATH"
	@false

CPPFLAGS += -I$(ICU_PATH)/source/tools/toolutil
LDFLAGS += -L$(ICU_PATH)/source/tools/toolutil -licu-toolutil -lustdio

$(GENRB):
	@echo "Please read the directions at the top of this file (Makefile)"
	@echo "Can't open $@ - check ICU_PATH"
	@false

.PHONY: all clean distclean check report

distclean clean:
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)
	-$(RMV) $(OBJECTS) $(TARGET)
	-$(RMV) $(DEPS)

## resources
root.res : root.txt
	@echo "Generating $@"
	$(GENRB) $(GENRBOPT) $?

en.res : en.txt
	@echo "Generating $@"
	$(GENRB) $(GENRBOPT) $?

sr.res : sr.txt
	@echo "Generating $@"
	$(GENRB) $(GENRBOPT) -e cp1251 $?



# Can change this to LINK.c if it is a C only program
# Can add more libraries here. 
$(TARGET): $(OBJECTS)
	$(LINK.cc) -o $@ $^ $(ICULIBS)

# Make check: simply runs the sample, logged to a file
check: $(TARGET)
	$(TARGET) | tee $(TARGET).out

# Make report: creates a 'report file' with both source and sample run
report: $(TARGET).report

$(TARGET).report: check $(TARGET).cpp
	more $(TARGET).cpp $(TARGET).out > $@

$(ICU_INC):
	@echo "Please read the directions at the top of this file (Makefile)"
	@echo "Can't open $(ICU_INC)"
	@false

ifneq ($(MAKECMDGOALS),distclean)
-include $(DEPS)
endif













