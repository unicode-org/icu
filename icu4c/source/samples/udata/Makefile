## Makefile for udata example

## Copyright (c) 2000, International Business Machines Corporation and
## others. All Rights Reserved.


## Shell to use
SHELL = /bin/sh
VERSION = 1.6
UNICODE_VERSION = 3.0.0

ICUDATADIR=/home/weiv/dev/icu_linux/source/../data/

## Install directory information
top_builddir=/usr/home/weiv/dev/icu_linux/source
srcdir = /usr/home/weiv/dev/icu_linux/source
top_srcdir = /usr/home/weiv/dev/icu_linux/source
prefix = /usr/local
exec_prefix = ${prefix}

pkgdatadir = $(datadir)/icu
pkglibdir = $(libdir)/icu
pkgincludedir = $(includedir)/icu

## Compiler/tools information
CC = gcc
CXX = g++

## Platform-specific setup

## -*-makefile-*-
## Linux-specific setup
## Copyright (c) 1999-2000, International Business Machines Corporation and
## others. All Rights Reserved.

## Commands to generate dependency files
GEN_DEPS.c=	$(CC) -E -MM $(DEFS) $(CPPFLAGS)
GEN_DEPS.cc=	$(CXX) -E -MM $(DEFS) $(CPPFLAGS)

## Commands to compile
COMPILE.c=	$(CC) $(DEFS) $(CPPFLAGS) $(CFLAGS) -c
COMPILE.cc=	$(CXX) $(DEFS) $(CPPFLAGS) $(CXXFLAGS) -c

## Commands to link
LINK.c=		$(CC) $(DEFS) $(CPPFLAGS) $(CFLAGS) $(LDFLAGS)
LINK.cc=	$(CXX) $(DEFS) $(CPPFLAGS) $(CXXFLAGS) $(LDFLAGS)

## Commands to make a shared library
SHLIB.c=	$(CC) $(DEFS) $(CPPFLAGS) $(CFLAGS) $(LDFLAGS) -shared
SHLIB.cc=	$(CXX) $(DEFS) $(CPPFLAGS) $(CXXFLAGS) $(LDFLAGS) -shared

## Compiler switch to embed a runtime search path
LD_RPATH=	
LD_RPATH_PRE=	-Wl,-rpath,

###  How ICU libraries are named...  ex. $(LIBICU)uc$(SO)
## Prefix for the ICU library names
LIBICU = libicu-
## Shared object suffix
SO=		so

## Link commands to link to ICU libs
LIBICU-UC= 	-L$(top_builddir)/common -licu-uc
LIBICU-I18N= 	-L$(top_builddir)/i18n -licu-i18n
LIBCTESTFW= 	-L$(top_builddir)/tools/ctestfw -lctestfw
LIBICU-TOOLUTIL=-L$(top_builddir)/tools/toolutil -licu-toolutil
LIBUSTDIO=      -L$(top_builddir)/extra/ustdio -lustdio

## Compilation rules
%.o : $(srcdir)/%.c
	$(COMPILE.c) -o $@ $<

%.o : $(srcdir)/%.cpp
	$(COMPILE.cc) -o $@ $<

../data/%.o : ../data/%.c
	$(COMPILE.c) -o $@ $<

## Dependency rules
%.d : $(srcdir)/%.c
	@echo "Generating dependency information for $<"
	@$(SHELL) -ec '$(GEN_DEPS.c) $< \
		| sed '\''s/\($*\)\.o[ :]*/\1.o $@ : /g'\'' > $@; \
		[ -s $@ ] || rm -f $@'

%.d : $(srcdir)/%.cpp
	@echo "Generating dependency information for $<"
	@$(SHELL) -ec '$(GEN_DEPS.cc) $< \
		| sed '\''s/\($*\)\.o[ :]*/\1.o $@ : /g'\'' > $@; \
		[ -s $@ ] || rm -f $@'


## Build directory information
subdir = tools/gentest

## Extra files to remove for 'make clean'
CLEANFILES = *~

## Target information
TARGET = writer

DEFS = -DHAVE_CONFIG_H
CPPFLAGS =  -I. -I$(top_builddir)/common -I$(top_srcdir)/common -I$(srcdir)/../toolutil
CFLAGS = -g -O2
CXXFLAGS = -g -O2
LDFLAGS =  \
$(LD_RPATH)$(LD_RPATH_PRE)$(top_builddir)/common $(LD_RPATH_PRE)$(top_builddir)/tools/toolutil $(LD_RPATH_PRE)$(libdir)
LIBS = $(LIBICU-UC)  -ldl -lpthread -lm    $(LIBICU-TOOLUTIL)

OBJECTS = writer.o reader.o

DEPS = $(OBJECTS:.o=.d)

## Clear suffix list
.SUFFIXES :

## List of standard targets
all: all-local

all-local: $(TARGET) build-data

build-data: $(ICUDATADIR)/test.dat

# we set the ICU_DATA directory here so that test.dat ends up in 
# icu/data rather than PREFIX/share/icu/VERSION/
$(ICUDATADIR)/test.dat: $(TARGET)
	ICU_DATA=$(ICUDATADIR) ./$(TARGET) 

#writer : $(OBJECTS)
#	$(LINK.cc) -o $@ $^ $(LIBS) 

#reader : $(OBJECTS)
#	$(LINK.cc) -o $@ $^ $(LIBS) 

$(TARGET) : $(OBJECTS)
	$(LINK.cc) -o $@ $^ $(LIBS) 

ifeq (,$(MAKECMDGOALS))
-include $(DEPS)
else
ifneq ($(patsubst %clean,,$(MAKECMDGOALS)),)
-include $(DEPS)
endif
endif

















