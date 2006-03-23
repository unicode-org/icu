# *   Copyright (C) 2006, International Business Machines
# *   Corporation and others.  All Rights Reserved.
# A list of txt's to build
# Note: 
#
#   If you are thinking of modifying this file, READ THIS. 
#
# Instead of changing this file [unless you want to check it back in],
# you should consider creating a 'brslocal.mk' file in this same directory.
# Then, you can have your local changes remain even if you upgrade or
# reconfigure ICU.
#
# Example 'brslocal.mk' files:
#
#  * To add an additional locale to the list: 
#    _____________________________________________________
#    |  BREAKRES_SOURCE_LOCAL =   myLocale.txt ...
#
#  * To REPLACE the default list and only build with a few
#     locale:
#    _____________________________________________________
#    |  BREAKRES_SOURCE = ar.txt ar_AE.txt en.txt de.txt zh.txt
#
#

# Ordinary resources
BREAKRES_SOURCE = ja.txt en.txt en_US.txt en_US_POSIX.txt
