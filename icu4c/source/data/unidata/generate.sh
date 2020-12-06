#!/bin/bash

# © 2021 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html

# This script runs Bazel to create (generate) header files from data that are
# needed for bootstrapping the ICU4C build to integrate the data.

# Required environment variables:
#   - ICU_SRC - the root directory of ICU source. This directory contains the
#               `icu4c` directory.


bazelisk build //icu4c/source/common:normalizer2
cp $ICU_SRC/bazel-bin/icu4c/source/common/norm2_nfc_data.h $ICU_SRC/icu4c/source/common/norm2_nfc_data.h

bazelisk run //tools/unicode/c/genprops $ICU_SRC/icu4c/
