# Copyright (C) 2010-2014, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2010jun03
# created by: Markus W. Scherer
cmake_minimum_required(VERSION 2.6)
project(ICU_UNI_TOOLS)
# This requires an icudefs.txt file with contents like the following:
# Location (--prefix) of where ICU was installed.
#set(ICU_INST_DIR /home/mscherer/svn.icu/trunk/bldinst)
# Location of the ICU source tree.
#set(ICU_SRC_DIR /home/mscherer/svn.icu/trunk/src)
set(CMAKE_BUILD_TYPE Debug)
include(icudefs.txt)
include_directories(
  ${ICU_INST_DIR}/include
  ${ICU_SRC_DIR}/source/common
  ${ICU_SRC_DIR}/source/i18n
  ${ICU_SRC_DIR}/source/tools/toolutil)
link_directories(${ICU_INST_DIR}/lib)
add_subdirectory(genprops)
add_subdirectory(genuca)
add_subdirectory(genuts46)
