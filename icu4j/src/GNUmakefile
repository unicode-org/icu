# Copyright © 1999-2000, International Business Machines Corporation and others. All Rights Reserved

#
# @(#)$RCSfile: GNUmakefile,v $ $Revision: 1.4 $ $Date: 2000/03/09 18:38:20 $
# 

TOPDIR=..
PACKAGE=	

include $(TOPDIR)/src/build/defs.gmk

SUBDIRS=	com

default: classes

DOCDIR=	 	$(TOPDIR)/docs
DOCINDEX=	$(DOCDIR)/index.html

javadoc: FORCE
	rm -rf $(DOCDIR)
	mkdir $(DOCDIR)
	javadoc -d $(DOCDIR)  \
		-nodeprecatedlist \
		-windowTitle "ICU4J" \
		-docTitle "ICU4J" \
		-encoding "iso-8859-1" \
		-docencoding "iso-8859-1" \
		-bottom "<font size=-1>Copyright &copy; 1998-2000 IBM Corporation.</font>" \
	    $(TOPDIR)/src/com/ibm/text/*.java $(TOPDIR)/src/com/ibm/util/*.java


tools: classes toolsinternal

tests: classes testsinternal

demos: classes demosinternal

toolsinternal: FORCE
	cd com/ibm/tools ; $(MAKE) classes ; cd ../../..

testsinternal: FORCE
	cd com/ibm/test ; $(MAKE) classes ; cd ../../..

demosinternal: FORCE
	cd com/ibm/demo ; $(MAKE) classes ; cd ../../..


complete: classes testsinternal toolsinternal demosinternal javadoc

include $(TOPDIR)/src/build/rules.gmk

