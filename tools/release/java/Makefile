#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (C) 2009-2013 IBM and Others. All Rights Reserved

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
MD_TARGET=APIChangeReport.md

NOTSET=

ifeq ($(OLD_ICU),)
NOTSET:="$(NOTSET) OLD_ICU"
endif

ifeq ($(NEW_ICU),)
NOTSET:="$(NOTSET) NEW_ICU"
endif

ifeq ($(NEW_ICU_BUILD),)
NEW_ICU_BUILD:=$(NEW_ICU)/source
endif

ifeq ($(OLD_ICU_BUILD),)
OLD_ICU_BUILD:=$(OLD_ICU)/source
endif

all: $(TARGET) $(LOCAL_TARGET) $(MD_TARGET)

# auto "build" the "old" icu into OLD_ICU_BUILD
tmp-old: $(OLD_ICU)/source/configure
	mkdir -p $(OLD_ICU_BUILD)/
	( cd $(OLD_ICU_BUILD)/ && $(OLD_ICU)/source/configure )

# auto "build" the "old" icu into OLD_ICU_BUILD
tmp-new: $(NEW_ICU)/source/configure
	mkdir -p $(NEW_ICU_BUILD)/
	( cd $(NEW_ICU_BUILD)/ && $(NEW_ICU)/source/configure )

inplace-old: $(OLD_ICU)/source/configure
	mkdir -p $(OLD_ICU_BUILD)/
	( cd $(OLD_ICU_BUILD)/ && ./configure )

# auto "build" the "old" icu into OLD_ICU_BUILD
inplace-new: $(NEW_ICU)/source/configure
	mkdir -p $(NEW_ICU_BUILD)/
	( cd $(NEW_ICU_BUILD)/ && ./configure )


ifneq ($(NOTSET),)
check-vars:
	@echo "Error: please set $(NOTSET) in Makefile.local or on the command line."
	false
else
check-vars:
endif

.PHONY: check-vars classes clean-docs tmp-old

.PRECIOUS: $(TARGET) $(OLD_ICU_BUILD)/$(DOXYFILE) $(NEW_ICU_BUILD)/$(DOXYFILE)

#######
DOXYFILE=Doxyfile
XML=doc/xml
UVERSIONH=$(XML)/uversion_8h.xml

clean-docs: | check-vars
	-rm -rf "$(OLD_ICU_BUILD)/$(XML)" "$(NEW_ICU_BUILD)/$(XML)"

$(TARGET): check-vars $(OLD_ICU_BUILD)/$(XML) $(NEW_ICU_BUILD)/$(XML)
	echo "Remember to run the non-ascii file detector if you get errors."
	$(ANT) -Dolddir="$(OLD_ICU_BUILD)/$(XML)" -Dnewdir="$(NEW_ICU_BUILD)/$(XML)" $(ANT_TARGET)
	echo "If you get no-changes, see the readme- may need to add xalan/xerces jars."

# check-vars $(OLD_ICU_BUILD)/$(XML) $(NEW_ICU_BUILD)/$(XML)
APIChangeReport.xml: $(OLD_ICU_BUILD)/$(XML) $(NEW_ICU_BUILD)/$(XML)
	echo "Remember to run the non-ascii file detector if you get errors."
	$(ANT) -Dolddir="$(OLD_ICU_BUILD)/$(XML)" -Dnewdir="$(NEW_ICU_BUILD)/$(XML)" $(ANT_TARGET)_xml

APIChangeReport.md: $(OLD_ICU_BUILD)/$(XML) $(NEW_ICU_BUILD)/$(XML)
	echo "Remember to run the non-ascii file detector if you get errors."
	$(ANT) -Dolddir="$(OLD_ICU_BUILD)/$(XML)" -Dnewdir="$(NEW_ICU_BUILD)/$(XML)" $(ANT_TARGET)_md

%/doc/xml:	%/Doxyfile
# don't care what GENERATE_XML is set to previously - set it to yes.
	@echo running doxygen ..
	( cd "$*/" && ( (cat ./Doxyfile | grep -v '^GENERATE_') ;  echo 'GENERATE_XML=YES')  | $(DOXYGEN) -  )
	@echo doxygen done in $*

%/Doxyfile:	%/Makefile
	@echo generating $@
	@$(MAKE) -C  "$*/" Doxyfile || ( echo "could not make Doxyfile in $*  - was doxygen available?" ; false )

#%/source/Makefile: %/source/Makefile.in
#	@echo "$@ does not seem to exist - was configure run?" ; false
