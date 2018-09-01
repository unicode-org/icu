# Copyright (C) 2017 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
#
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
               layoutpropsbuilder.cpp
               namespropsbuilder.cpp)
target_link_libraries(genprops icuuc icutu)
