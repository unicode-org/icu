# Copyright (C) 2009 IBM and Others. All Rights Reserved

#
#
#
#Usage:
# make OLD_ICU=/xsrl/E/401m40  NEW_ICU=/xsrl/E/icu4ct42
#
# you can put the OLD_ICU=xx and NEW_ICU=yy in separate lines in Makefile.local 
#

ANT=ant
ANT_TARGET=apireport
DOXYGEN=doxygen

-include Makefile.local

TARGET=APIChangeReport.html

NOTSET=

ifeq ($(OLD_ICU),)
NOTSET:="$(NOTSET) OLD_ICU"
endif

ifeq ($(NEW_ICU),)
NOTSET:="$(NOTSET) NEW_ICU"
endif

all: $(TARGET)

ifneq ($(NOTSET),)
check-vars:
	@echo "Error: please set $(NOTSET) in Makefile.local or on the command line."
	false
else
check-vars:
endif

.PHONY: check-vars classes clean-docs

.PRECIOUS: $(TARGET) $(OLD_ICU)/$(DOXYFILE) $(NEW_ICU)/$(DOXYFILE)

#######
DOXYFILE=source/Doxyfile
XML=source/doc/xml
UVERSIONH=$(XML)/uversion_8h.xml

clean-docs: | check-vars
	-rm -rf "$(OLD_ICU)/$(XML)"

$(TARGET): check-vars $(OLD_ICU)/$(XML) $(NEW_ICU)/$(XML) 
	$(ANT) -Dolddir="$(OLD_ICU)/$(XML)" -Dnewdir="$(NEW_ICU)/$(XML)" $(ANT_TARGET)

%/source/doc/xml:	%/source/Doxyfile
	# don't care what GENERATE_XML is set to previously - set it to yes.
	@echo running doxyugen .. 
	( cd "$*/source/" && ( (cat ./Doxyfile | grep -v '^GENERATE_') ;  echo 'GENERATE_XML=YES')  | $(DOXYGEN) -  )
	@echo doxygen done in $*

%/source/Doxyfile:	%/source/Makefile %/source/Doxyfile.in
	@echo generating $@
	@$(MAKE) -C  "$*/source" Doxyfile || ( echo "could not make Doxyfile in $*  - was doxygen available?" ; false )

%/source/Makefile: %/source/Makefile.in
	@echo "$@ does not seem to exist - was configure run?" ; false
