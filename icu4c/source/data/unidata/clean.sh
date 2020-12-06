#!/bin/bash

# © 2021 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This script deletes files that generate.sh tries to regenerate.

# Required environment variables:
#   - ICU_SRC - the root directory of ICU source. This directory contains the
#               `icu4c` directory.


rm $ICU_SRC/icu4c/source/common/norm2_nfc_data.h

rm $ICU_SRC/icu4c/source/common/uchar_props_data.h
rm $ICU_SRC/icu4c/source/data/in/uprops.icu

