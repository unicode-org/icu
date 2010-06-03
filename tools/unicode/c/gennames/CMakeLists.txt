# Copyright (C) 2010, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2010jun03
# created by: Markus W. Scherer
add_executable(gennames gennames.c)
target_link_libraries(gennames libicuuc.so libicutu.so)
