#******************************************************************************
#
#   Copyright (C) 2002-2003, International Business Machines
#   Corporation and others.  All Rights Reserved.
#
#******************************************************************************

ICUDIR=ICUunrenamed
TOP=../..
SO=so

COM=$(ICUDIR)/lib/libicuuc.$(SO)
I18=$(ICUDIR)/lib/libicui18n.$(SO)
LAY=$(ICUDIR)/lib/libicule.$(SO)
LEX=$(ICUDIR)/lib/libiculx.$(SO)
DAT=$(ICUDIR)/stubdata/libicudata.$(SO)
UIO=$(ICUDIR)/lib/libicuio.$(SO)

LIBS=$(COM) $(I18) $(LAY) $(LEX) $(UIO)

all:
	@cat README

clean:
	-rm -rf $(ICUDIR) urename.* *~

# We use config.status to mean we have a valid out of source tree.

$(ICUDIR)/config.status:
	-mv $(ICUDIR) $(ICUDIR)old
	-(rm -rf $(ICUDIR)old &)
	mkdir $(ICUDIR)
	( cd $(ICUDIR) ; CPPFLAGS=-DURENAME_H $(TOP)/../configure --with-data-packaging=archive --enable-layout=yes --enable-tests=no --enable-extras=yes --prefix=`pwd` $(GENREN_CONFIGURE_OPTS) )

# build the libraries
$(DAT): $(ICUDIR)/config.status Makefile
	gmake -C $(ICUDIR)/stubdata libicudata.$(SO)

$(COM): $(DAT) $(ICUDIR)/config.status Makefile
	gmake -C $(ICUDIR)/common libicuuc.$(SO)

$(I18): $(DAT) $(COM) $(ICUDIR)/config.status Makefile
	gmake -C $(ICUDIR)/i18n libicui18n.$(SO)

$(LAY): $(DAT) $(I18) $(COM) $(ICUDIR)/config.status Makefile
	gmake -C $(ICUDIR)/layout libicule.$(SO)

$(LEX): $(DAT) $(I18) $(COM) $(ICUDIR)/config.status Makefile
	gmake -C $(ICUDIR)/layoutex libiculx.$(SO)

$(UIO):	$(DAT) $(I18) $(COM) $(ICUDIR)/config.status Makefile
	gmake -C $(ICUDIR)/extra/ustdio libicuio.$(SO)

# the header itself
urename.h: $(LIBS) genren.pl
	-cp urename.h urename.h.old
	perl ./genren.pl $(LIBS)

sorts:	urename.sort urename.old.sort
	@echo "*** Please check urename.h manually before committing it."
	@echo "Try 'diff --side-by-side urename.old.sort urename.sort'"

urename.sort: urename.h
	sort urename.h > $@

urename.old.sort: $(TOP)/common/unicode/urename.h
	sort  $(TOP)/common/unicode/urename.h > $@

install-header: urename.h sorts
	cp $(TOP)/common/unicode/urename.h $(TOP)/common/unicode/urename.h.old
	cp urename.h $(TOP)/common/unicode/


