# *   Copyright (C) 1997-2001, International Business Machines
# *   Corporation and others.  All Rights Reserved.
# A list of txt's to build
# Note: 
#
#   If you are thinking of modifying this file, READ THIS. 
#
# Instead of changing this file [unless you want to check it back in],
# you should consider creating a 'trnslocal.mk' file in this same directory.
# Then, you can have your local changes remain even if you upgrade or re
# configure the ICU.
#
# Example 'trnslocal.mk' files:
#
#  * To add an additional transliterators to the list: 
#    _____________________________________________________
#    |  TRANSLIT_SOURCE_LOCAL =   myTranslitRules.txt ...
#
#  * To REPLACE the default list and only build with a few
#     transliterators:
#    _____________________________________________________
#    |  TRANLIST_SOURCE = translit_index.txt translit_Any_Publishing.txt
#
#

TRANSLIT_SOURCE=Any_Accents.txt\
Any_Publishing.txt\
Bengali_InterIndic.txt\
Cyrillic_Latin.txt\
Devanagari_InterIndic.txt\
Fullwidth_Halfwidth.txt\
Greek_Latin.txt\
Gujarati_InterIndic.txt\
Gurmukhi_InterIndic.txt\
Hiragana_Katakana.txt\
Hiragana_Latin.txt\
InterIndic_Bengali.txt\
InterIndic_Devanagari.txt\
InterIndic_Gujarati.txt\
InterIndic_Gurmukhi.txt\
InterIndic_Kannada.txt\
InterIndic_Latin.txt\
InterIndic_Malayalam.txt\
InterIndic_Oriya.txt\
InterIndic_Tamil.txt\
InterIndic_Telugu.txt\
Kannada_InterIndic.txt\
Latin_InterIndic.txt\
Latin_Jamo.txt\
Latin_Katakana.txt\
Malayalam_InterIndic.txt\
Oriya_InterIndic.txt\
Tamil_InterIndic.txt\
Telugu_InterIndic.txt\
translit_index.txt