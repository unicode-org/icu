# A list of txt's to build
# Note: 
#
#   If you are thinking of modifying this file, READ THIS. 
#
# Instead of changing this file [unless you want to check it back in],
# you should consider creating a 'genrblocal.mk' file in this same directory.
# Then, you can have your local changes remain even if you upgrade or re
# configure the ICU.
#
# Example 'gencollocal.mk' files:
#
#  * To add an additional converter to the list: 
#    _____________________________________________________
#    |  GENCOL_SOURCE_LOCAL =   myLocale.txt ...
#
#  * To REPLACE the default list and only build with a few
#     converters:
#    _____________________________________________________
#    |  GENCOL_SOURCE =default.txt ar.txt ar_AE.txt
#
#

GENCOL_SOURCE = default.txt ar.txt be.txt bg.txt ca.txt cs.txt da.txt el.txt es.txt\
et.txt fi.txt fr.txt hr.txt hu.txt is.txt iw.txt ja.txt ko.txt lt.txt lv.txt\
mk.txt no.txt pl.txt ro.txt ru.txt sh.txt sk.txt sl.txt sq.txt sr.txt\
sv.txt tr.txt uk.txt vi.txt zh.txt zh_TW.txt