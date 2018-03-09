# Copyright (C) 2016 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
# Copyright (C) 2010-2014, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2010jun03
# created by: Markus W. Scherer
cmake_minimum_required(VERSION 2.6)
project(ICU_UNI_TOOLS)
set(CMAKE_CXX_FLAGS "-std=c++11 -fsanitize=bounds")
set(CMAKE_LD_FLAGS "-fsanitize=bounds")
# This requires an icudefs.txt file with contents like the following:
# Location (--prefix) of where ICU was installed.
#set(ICU_INST_DIR /usr/local/google/home/mscherer/svn.icu/trunk/inst/icu4c)
# Location of the ICU4C source tree.
#set(ICU4C_SRC_DIR /usr/local/google/home/mscherer/svn.icu/trunk/src/icu4c)
set(CMAKE_BUILD_TYPE Debug)
include(icudefs.txt)
include_directories(
  ${ICU_INST_DIR}/include
  ${ICU4C_SRC_DIR}/source/common
  ${ICU4C_SRC_DIR}/source/i18n
  ${ICU4C_SRC_DIR}/source/tools/toolutil)
link_directories(${ICU_INST_DIR}/lib)
add_subdirectory(genprops)
add_subdirectory(genuca)
add_subdirectory(genuts46)

