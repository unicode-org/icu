#
# @(#)$RCSfile: GNUmakefile,v $ $Revision: 1.2 $ $Date: 2000/02/26 21:55:43 $
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
		-windowTitle "International Classes for Java" \
		-docTitle "International Classes for Java" \
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

