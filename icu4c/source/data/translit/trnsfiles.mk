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

TRANSLIT_SOURCE=t_Any_Accents.txt\
t_Any_Publishing.txt\
t_Arab_Latn.txt\
t_Beng_InterIndic.txt\
t_Cyrl_Latn.txt\
t_Deva_InterIndic.txt\
t_FWidth_HWidth.txt\
t_Grek_Latn.txt\
t_Grek_Latn_UNGEGN.txt\
t_Gujr_InterIndic.txt\
t_Guru_InterIndic.txt\
t_Hani_Latn.txt\
t_Hebr_Latn.txt\
t_Hira_Kana.txt\
t_Hira_Latn.txt\
t_InterIndic_Beng.txt\
t_InterIndic_Deva.txt\
t_InterIndic_Gujr.txt\
t_InterIndic_Guru.txt\
t_InterIndic_Knda.txt\
t_InterIndic_Latn.txt\
t_InterIndic_Mlym.txt\
t_InterIndic_Orya.txt\
t_InterIndic_Taml.txt\
t_InterIndic_Telu.txt\
t_Knda_InterIndic.txt\
t_Latn_InterIndic.txt\
t_Latn_Jamo.txt\
t_Latn_Kana.txt\
t_Mlym_InterIndic.txt\
t_Orya_InterIndic.txt\
t_Taml_InterIndic.txt\
t_Telu_InterIndic.txt\
translit_index.txt
