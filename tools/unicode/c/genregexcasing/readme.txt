#
# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
# Copyright (C) 2014, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2014 May 2
# created by: Andy Heninger

genregexcasing is the tool for generating extended case closure data needed by
regular expressions for case insensitive matching.

The tool generates c++ data declarations that are then manually copied into the file
i18n/regexcmp.cpp.

Edit the Makefile to have the correct directories for your ICU sources and build
(the top two lines.)

A Unix-like system and the clang compiler are assumed.

To build and run the tool, from within this directory, do a plain, unqualified
make

