#
# @(#)$RCSfile: makefile,v $ $Revision: 1.3 $ $Date: 2000/02/16 19:04:29 $
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


# tests: 

# demos:

complete: classes javadoc

!include $(TOPDIR)/src/build/rules.mak

