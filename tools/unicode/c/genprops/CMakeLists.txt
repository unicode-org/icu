# Copyright (C) 2010-2012, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2010jun03
# created by: Markus W. Scherer
# edited on: 2010jul20
# edited by: Stuart G. Gill
add_executable(genprops genprops.cpp
               pnamesbuilder.cpp corepropsbuilder.cpp
               bidipropsbuilder.cpp casepropsbuilder.cpp
               namespropsbuilder.cpp)
target_link_libraries(genprops icuuc icutu)
