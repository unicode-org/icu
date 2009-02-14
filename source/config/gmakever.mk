## -*-makefile-*-
#******************************************************************************
#   Copyright (C) 2008-2009, International Business Machines
#   Corporation and others.  All Rights Reserved.
#******************************************************************************
# Make sure we have the right version of Make.

at_least=3.80
latest_a=$(firstword $(sort $(MAKE_VERSION) $(at_least)))

ifneq ($(at_least),$(latest_a))
err:
	@echo "ERROR: $(MAKE_VERSION) - too old, please upgrade to at least $(at_least)"
	@false
else
ok:
	@echo "$(MAKE_VERSION) (we wanted at least $(at_least))"
	@true
endif

