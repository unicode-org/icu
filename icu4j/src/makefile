#
# @(#)$RCSfile: makefile,v $ $Revision: 1.4 $ $Date: 2000/02/16 19:37:33 $
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


tools: classes
	cd com\ibm\test && nmake -nologo classes && cd ..\..\..

tests: classes
	cd com\ibm\test && nmake -nologo classes && cd ..\..\..

demos: classes
	cd com\ibm\demo && nmake -nologo classes && cd ..\..\..


toolsinternal: FORCE
	cd com\ibm\test && nmake -nologo classes && cd ..\..\..

testsinternal: FORCE
	cd com\ibm\test && nmake -nologo classes && cd ..\..\..

demosinternal: FORCE
	cd com\ibm\demo && nmake -nologo classes && cd ..\..\..



complete: classes testsinternal toolsinternal demosinternal javadoc

!include $(TOPDIR)/src/build/rules.mak

