# Copyright © 1999-2000, International Business Machines Corporation and others. All Rights Reserved

CLASSDIR=   $(TOPDIR)/classes
SRCDIR=		$(TOPDIR)/src
DOCDIR=		$(TOPDIR)/docs
CLASSPATH=  $(CLASSDIR)

PKGPATH=	$(PACKAGE:.=\)
TARGDIR=	$(CLASSDIR)/$(PKGPATH)

JAVACFLAGS=	-target 1.1
MAKE=		nmake -nologo 
