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
# Example 'genrblocal.mk' files:
#
#  * To add an additional converter to the list: 
#    _____________________________________________________
#    |  GENRB_SOURCE_LOCAL =   myLocale.txt ...
#
#  * To REPLACE the default list and only build with a few
#     converters:
#    _____________________________________________________
#    |  GENRB_SOURCE =default.txt index.txt ar.txt ar_AE.txt
#
#

GENRB_SOURCE = root.txt index.txt ar.txt ar_AE.txt\
ar_BH.txt ar_DZ.txt ar_EG.txt ar_IQ.txt ar_JO.txt\
ar_KW.txt ar_LB.txt ar_LY.txt ar_MA.txt ar_OM.txt\
ar_QA.txt ar_SA.txt ar_SD.txt ar_SY.txt ar_TN.txt\
ar_YE.txt be.txt be_BY.txt bg.txt bg_BG.txt\
ca.txt ca_ES.txt ca_ES_EURO.txt cs.txt cs_CZ.txt\
da.txt da_DK.txt de.txt de_AT.txt de_AT_EURO.txt\
de_CH.txt de_DE.txt de_DE_EURO.txt de_LU.txt\
de_LU_EURO.txt el.txt el_GR.txt en.txt en_AU.txt\
en_CA.txt en_BE.txt en_GB.txt en_IE.txt en_IE_EURO.txt\
en_NZ.txt en_US.txt en_ZA.txt es.txt es_AR.txt\
es_BO.txt es_CL.txt es_CO.txt es_CR.txt es_DO.txt\
es_EC.txt es_ES.txt es_ES_EURO.txt es_GT.txt\
es_HN.txt es_MX.txt es_NI.txt es_PA.txt es_PE.txt\
es_PR.txt es_PY.txt es_SV.txt es_UY.txt es_VE.txt\
et.txt et_EE.txt fi.txt fi_FI.txt fi_FI_EURO.txt\
fr.txt fr_BE.txt fr_BE_EURO.txt fr_CA.txt fr_CH.txt\
fr_FR.txt fr_FR_EURO.txt fr_LU.txt fr_LU_EURO.txt\
he.txt he_IL.txt\
hr.txt hr_HR.txt hu.txt hu_HU.txt is.txt\
is_IS.txt it.txt it_CH.txt it_IT.txt it_IT_EURO.txt\
iw.txt iw_IL.txt ja.txt ja_JP.txt ko.txt\
ko_KR.txt lt.txt lt_LT.txt lv.txt lv_LV.txt\
mk.txt mk_MK.txt nl.txt nl_BE.txt nl_BE_EURO.txt\
nl_NL.txt nl_NL_EURO.txt no.txt no_NO.txt no_NO_NY.txt\
pl.txt pl_PL.txt pt.txt pt_BR.txt pt_PT.txt\
pt_PT_EURO.txt ro.txt ro_RO.txt ru.txt ru_RU.txt\
sh.txt sh_YU.txt sk.txt sk_SK.txt sl.txt\
sl_SI.txt sq.txt sq_AL.txt sr.txt sr_YU.txt\
sv.txt sv_SE.txt th.txt th_TH.txt tr.txt\
tr_TR.txt uk.txt uk_UA.txt vi.txt vi_VN.txt\
zh.txt zh_CN.txt zh_HK.txt zh_TW.txt hi.txt hi_IN.txt mt.txt mt_MT.txt eo.txt\
kok.txt kok_IN.txt ta.txt ta_IN.txt mr.txt mr_IN.txt\
sv_FI.txt 

TRANSLIT_SOURCE = fullhalf.txt translit_index.txt kana.txt kbdescl1.txt\
larabic.txt lcyril.txt ldevan.txt\
lgreek.txt lhebrew.txt ljamo.txt\
lkana.txt quotes.txt ucname.txt



