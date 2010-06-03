# Copyright (C) 2010, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2010jun03
# created by: Markus W. Scherer
add_executable(genuts46 genuts46.cpp)
target_link_libraries(genuts46 libicuuc.so libicutu.so)
