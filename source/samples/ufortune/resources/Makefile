
# Copyright (c) 2001 IBM, Inc. and others

#
# File
#    icu/source/samples/ufortune/resources/Makefile
#
# Usage:
#    See the instructions in the parent Makefile,
#          icu/source/samples/ufortune/Makefile.
#          This subproject builds the ICU resource files for ufortune.
#          It is normally invoked from the parent directory,
#            although the resources can be built from here.
#
#    Two ICU tools are run from this makefile:
#      genrb    compiles a resource source file (.txt) into
#               a binary .res file.
#      pkgdata  combines all of the .res files into a single
#               shared library that can then be linked with the
#               main application.
#
#               pkgdata will recursively generate and run yet
#               another makefile, which in turn runs two more
#               icu tools.  gencmn combines the individual .res
#               files, and genccode emits the data as C source
#               code that can then be built into a .so
#
ICU_DEFAULT_PREFIX=/export/andy/icui

ifeq ($(strip $(ICU_PREFIX)),)
  ICU_PREFIX=$(ICU_DEFAULT_PREFIX)
endif

ICU_INC=$(ICU_PREFIX)/lib/icu/Makefile.inc
ICUPATH=

include $(ICU_INC)

# Name of your target
TARGET=libfortune_resources.$(SO)

# Add implicit rule for .txt to .res
# ICU resources are compiled into .res files by the genrb tool
#
%.res: %.txt
	$(INVOKE) $(sbindir)/genrb -d . $<

# Resource files.  Add new ones for additional locales here.
RESFILES=root.res es.res


# All object files (C or C++)
OBJECTS=

CLEANFILES=*~ $(TARGET).out

DEPS=$(OBJECTS:.o=.d)

all: $(TARGET)

.PHONY: all clean distclean check report

distclean clean:
	-test -z "$(CLEANFILES)" || rm -f $(CLEANFILES)
	-$(RMV) $(OBJECTS) $(TARGET) $(RESFILES)
	-$(RMV) $(DEPS)

$(TARGET): $(RESFILES)
	$(INVOKE) $(sbindir)/pkgdata --name fortune_resources -v -O $(ICU_INC) --mode dll -d .  res-file-list.txt
	cp $(TARGET) ..

# Make check: simply runs the sample, logged to a file
check: $(TARGET)

# Make report: creates a 'report file' with both source and sample run
report: 


$(ICU_INC):
	@echo "Please read the directions at the top of this file (Makefile)"
	@echo "Can't open $(ICU_INC)"
	@false

ifneq ($(MAKECMDGOALS),distclean)
# -include $(DEPS)
endif
