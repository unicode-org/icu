#
# @(#)$RCSfile: makefile,v $ $Revision: 1.6 $ $Date: 2000/02/23 00:35:28 $
# 

TOPDIR=		..
PACKAGE=	

!include $(TOPDIR)/src/build/defs.mak

SUBDIRS=	com

default: classes

DOCDIR=	 	$(TOPDIR)\docs
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
	    com/ibm/text/*.java com/ibm/util/*.java


tools: classes toolsinternal

tests: classes testsinternal

demos: classes demosinternal


toolsinternal: FORCE
	cd com\ibm\test && nmake -nologo /f makefile.nmk classes && cd ..\..\..

testsinternal: FORCE
	cd com\ibm\test && nmake -nologo /f makefile.nmk classes && cd ..\..\..

demosinternal: FORCE
	cd com\ibm\demo && nmake -nologo /f makefile.nmk classes && cd ..\..\..

complete: classes testsinternal toolsinternal demosinternal javadoc

!include $(TOPDIR)/src/build/rules.mak

