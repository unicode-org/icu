# Copyright (c) 2002 IBM, Inc. and others
# Sample code makefile definitions 

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
INVOKE=$(shell icu-config --invoke)
####################################################################
### Project independent things (common) 
### We depend on gmake for the bulk of the work 

# link with C++ compiler
LINK.o=$(LINK.cc)
