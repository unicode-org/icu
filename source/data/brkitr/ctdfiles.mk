# *   Copyright (C) 2006, International Business Machines
# *   Corporation and others.  All Rights Reserved.
# A list of txt's to build
# Note: 
#
#   If you are thinking of modifying this file, READ THIS. 
#
# Instead of changing this file [unless you want to check it back in],
# you should consider creating a 'ctdlocal.mk' file in this same directory.
# Then, you can have your local changes remain even if you upgrade or
# reconfigure ICU.
#
# Example 'ctdlocal.mk' files:
#
#  * To add an additional dictionary to the list: 
#    _____________________________________________________
#    |  CTD_SOURCE_LOCAL =   myDict.txt ...
#
#  * To REPLACE the default list and only build with a different
#     dictionary:
#    _____________________________________________________
#    |  CTD_SOURCE = myDict.txt
#
#

CTD_SOURCE = \
thaidict.txt
