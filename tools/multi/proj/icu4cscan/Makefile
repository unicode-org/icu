#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved.
MULTICU_ROOT=../../
include $(MULTICU_ROOT)/common/Makefile-multi.inc

XML=./xml
JXML=./jxml
JVMXML=./jvmxml

COOKED=cooked
INPUT=input
INTROXMLS=$(wildcard $(INPUT)/*-introduction.xml)
FEATXMLS=$(INTROXMLS:$(INPUT)/%-introduction.xml=$(COOKED)/%-cooked.xml) $(COOKED)/characters.xml
ALLCOOKED=cooked/features.xml

# java stuff
MergerName=Merger
PropagateName=PropagateFeatures
PropagateCharsName=PropagateCharacters
MergerPackage=com.ibm.icu.dev.meta
MergerPackageSlash=$(subst .,/,$(MergerPackage))
## ICUMETA=/xsrl/E/data/meta  (in Makefile.local)
MergerLib=$(ICUMETA)/lib
MergerJar=$(ICUMETA)/icumeta.jar
#was: $(ICUMETA)/classes
MergerClassPath=$(shell ls $(MergerLib)/*.jar | tr '\n' ':')$(MergerJar):$(OTHERJARS)
include Makefile.local
MergerBuild=$(ICUMETA)/build.xml
#MergerClass=$(ICUMETA)/classes/$(MergerPackageSlash)/$(MergerName).class
#MergerJava=$(ICUMETA)/src/$(MergerPackageSlash)/$(MergerName).java
#BASEXMLS=$(ICUMETA)/xml/icumeta.xml
BASEXMLS=$(ICUMETA)/input/versions.xml


APRECIOUS=$(ICUVERS:%=$(BLD)/%/$(S_SRC)) $(ICUVERS:%=$(BLD)/%/$(S_BLD)) $(ICUVERS:%=$(INS)/%/$(S_INS))

all: icucaps.xml

.PRECIOUS: $(APRECIOUS)

tehprecious:
	echo $(APRECIOUS)

$(MergerJar):
	( cd $(ICUMETA)  ; ant jar )

$(BLD):
	-mkdir $(BLD)

$(XML):
	-mkdir $(XML)

$(JXML):
	-mkdir $(JXML)

$(JVMXML):
	-mkdir $(JXML)

ifeq ($(JVMSH),)
JVMSH_WARN=warn-jvmsh
warn-jvmsh:
	@echo "Note: No $(JVM)/*$(JVM_CFG) files found, see $(JVM)/readme.txt"
endif


DTDCACHE=$(shell pwd)/dtdcache
DTDTMP=$(shell pwd)/dtdtmp
DTDOVR=$(ICUMETA)/dtd
JAVADEBUG=-DCLDR_DTD_CACHE_DEBUG=n -DCLDR_DTD_CACHE_ADEBUG=n 
JAVAPROPS= -DCLDR_DTD_CACHE=$(DTDCACHE) -DCLDR_DTD_OVERRIDE=$(DTDOVR) -DICU_DTD_CACHE=$(DTDCACHE) -DICU_DTD_OVERRIDE=$DTDOVR 

#//$(DTD)/Makefile: $(ICUMETA)/dtd/Makfile
#//	ln -s $^ $@
#//
#//dtd/icucaps.dtd: $(DTD)/Makefile dtd/icucaps.rnc
#//	make -C $(DTD)

ICUMETA_XML=$(ICUMETA)/xml/icumeta.xml
BASE_VERSION=$(COOKED)/allversions.xml
BASE_SOURCE=$(BASEXMLS) $(INPUT)/java.xml


#$(ICUMETA)/xml/icumeta.xml: $(MergerBuild)
#	( cd $(ICUMETA) ; ant xml )	

ALLXMLS=$(wildcard $(XML)/*.xml $(JXML)/*.xml $(JVMXML)/*.xml)

allxmls: $(XML) $(JXML) $(JVMXML)
	$(MAKE) -f Makefile_c outfiles
	$(MAKE) -f Makefile_j outfiles


featxmls: $(FEATXMLS)

allcooked: $(ALLCOOKED)

$(COOKED):
	-mkdir $(COOKED)

$(BASE_VERSION): $(BASE_SOURCE) $(MergerJar) | $(COOKED)
	-rm -rf ./dtdcache
	mkdir dtdcache
	java $(JAVAPROPS) -classpath $(MergerClassPath) $(MergerPackage).$(MergerName) -v  -o:$@ $(BASE_SOURCE)

$(ALLCOOKED): $(FEATXMLS) $(MergerJar)
	-rm -rf ./dtdcache
	mkdir dtdcache
	java $(JAVAPROPS) -classpath $(MergerClassPath) $(MergerPackage).$(MergerName) -v  -o:$@ $(FEATXMLS)

$(COOKED)/%-cooked.xml: $(BASE_VERSION) $(INPUT)/%-introduction.xml $(MergerJar) | $(COOKED)
	-rm -rf ./dtdcache
	mkdir dtdcache
	java $(JAVAPROPS) -classpath $(MergerClassPath) $(MergerPackage).$(PropagateName) $(BASE_VERSION) $(INPUT)/$*-introduction.xml > $(COOKED)/$*-cooked.xml

$(COOKED)/characters.xml: $(ALLXMLS) $(COOKED)/junicode-cooked.xml $(MergerJar)
	java $(JAVAPROPS) -classpath $(MergerClassPath) $(MergerPackage).$(PropagateCharsName) > $@ $(ALLXMLS)  $(COOKED)/junicode-cooked.xml  || (rm ./$@ ; false)

 
icucaps.xml: allxmls $(BASE_VERSION)  $(ALLXMLS) $(MergerJar) $(ALLCOOKED)
	-rm -rf ./dtdcache
	mkdir dtdcache
#	-rm -rf ./dtdtmp
#	mkdir dtdtmp
#	ln -s $(ICUMETA)/dtd/*.dtd ./dtdtmp/
#	ln -s ./dtd/*.dtd ./dtdtmp/
	java $(JAVAPROPS) -classpath $(MergerClassPath) $(MergerPackage).$(MergerName) -v  -o:$@ $(BASE_VERSION)  $(ALLXMLS) $(ALLJXMLS) $(ALLCOOKED)

