# Copyright (c) 2000 IBM, Inc. and others
# conversion sample code $Revision: 1.6 $


# Usage:
#  - configure, build, install ICU (make install)
#  - make sure "icu-config" (in the ICU installed bin directory) is on
#     the path
#  - do 'make' in this directory

# Name of your target
TARGET=convsamp

# All object files (C or C++)
OBJECTS=convsamp.o flagcb.o

CLEANFILES=*~ $(TARGET).out

####################################################################
# Load ICU information. You can copy this to other makefiles #######
####################################################################
CC=$(shell icu-config --cc)
CXX=$(shell icu-config --cxx)
CPPFLAGS=$(shell icu-config --cppflags)
CFLAGS=$(shell icu-config --cflags)
CXXFLAGS=$(shell icu-config --cxxflags)
LDFLAGS =$(shell icu-config --ldflags)
####################################################################

# turn on super warnings
#CPPFLAGS += -Wall

$(TARGET): $(OBJECTS)

all: $(TARGET)

.PHONY: all clean distclean check report

distclean clean:
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)
	-rm $(OBJECTS) $(TARGET)

# Make check: simply runs the sample, logged to a file
check: $(TARGET)
	./$(TARGET) | tee $(TARGET).out

# Make report: creates a 'report file' with both source and sample run
report: $(TARGET).report

$(TARGET).report: check $(TARGET).cpp
	more $(TARGET).cpp $(TARGET).out > $@



