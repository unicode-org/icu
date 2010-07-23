# Copyright (C) 2010, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# Basic definitions for building ICU Unicode data.
# Sourced from makeprops.sh for example.
UNICODE_VERSION=6.0
# Assume that there are parallel src & bld trees with the Unicode tools
# source files and the out-of-source build files.
# Assume that the current folder is some/path/src/unicode
UNITOOLS_BLD=../../bld/unicode
# The sourcing script must define ICU_SRC and ICU_BLD for the ICU library
# source files and the out-of-source build files.
UNIDATA=$ICU_SRC/source/data/unidata
COMMON=$ICU_SRC/source/common
SRC_DATA_IN=$ICU_SRC/source/data/in
BLD_DATA_FILES=$ICU_BLD/data/out/build/icudt45l
