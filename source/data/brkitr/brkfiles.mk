# *   Copyright (C) 1997-2004, International Business Machines
# *   Corporation and others.  All Rights Reserved.
# A list of txt's to build
# Note: 
#
#   If you are thinking of modifying this file, READ THIS. 
#
# Instead of changing this file [unless you want to check it back in],
# you should consider creating a 'brklocal.mk' file in this same directory.
# Then, you can have your local changes remain even if you upgrade or
# reconfigure ICU.
#
# Example 'brklocal.mk' files:
#
#  * To add an additional locale to the list: 
#    _____________________________________________________
#    |  BRK_SOURCE_LOCAL =   myLocale.txt ...
#
#  * To REPLACE the default list and only build with a few
#     locale:
#    _____________________________________________________
#    |  BRK_SOURCE = ar.txt ar_AE.txt en.txt de.txt zh.txt
#
#


# don't include thaidict.brk - it goes into a resource bundle - plus it isn't deleted.
# char.txt, title.txt and word.txt are not included so that more tests pass by default,
# and so that the makefile rules are simplier.
BRK_SOURCE = \
line.txt sent.txt line_th.txt word_th.txt word_ja.txt word_POSIX.txt
