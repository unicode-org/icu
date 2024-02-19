#!/bin/sh
#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
#*****************************************************************************
#
#   Copyright (C) 2006-2008, International Business Machines
#   Corporation and others.  All Rights Reserved.
#
#*****************************************************************************
for loc in `locale -a`; do echo ; echo LC_ALL=$loc ; LC_ALL=$loc ./displayLocaleConv ; done
