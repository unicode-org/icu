# Copyright (C) 2010, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2010jun03
# created by: Markus W. Scherer
# edited on: 2010jul20
# edited by: Stuart G. Gill
add_executable(genprops genprops.c props2.c store.c)
target_link_libraries(genprops icuuc icutu)
