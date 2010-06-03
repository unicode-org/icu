# Copyright (C) 2010, International Business Machines
# Corporation and others.  All Rights Reserved.
#
# created on: 2010jun03
# created by: Markus W. Scherer
add_executable(genuca genuca.cpp)
target_link_libraries(genuca libicuuc.so libicui18n.so libicutu.so)
