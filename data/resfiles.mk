# *   Copyright (C) 1997-2001, International Business Machines
# *   Corporation and others.  All Rights Reserved.
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

GENRB_SOURCE = root.txt index.txt\
af.txt af_ZA.txt\
ar.txt ar_AE.txt ar_BH.txt ar_DZ.txt ar_EG.txt\
ar_IQ.txt ar_JO.txt ar_KW.txt ar_LB.txt ar_LY.txt\
ar_MA.txt ar_OM.txt ar_QA.txt ar_SA.txt ar_SD.txt\
ar_SY.txt ar_TN.txt ar_YE.txt\
be.txt be_BY.txt\
bg.txt bg_BG.txt\
ca.txt ca_ES.txt ca_ES_EURO.txt\
cs.txt cs_CZ.txt\
da.txt da_DK.txt\
de.txt de_AT.txt de_AT_EURO.txt de_CH.txt\
de_DE.txt de_DE_EURO.txt de_LU.txt de_LU_EURO.txt\
el.txt el_GR.txt el_GR_EURO.txt\
en.txt en_AS.txt en_AU.txt en_BE.txt en_BE_EURO.txt en_BW.txt en_CA.txt en_GB.txt en_GB_EURO.txt\
en_GU.txt en_HK.txt en_IE.txt en_IE_EURO.txt en_IN.txt en_MH.txt en_MP.txt en_NZ.txt en_PH.txt en_SG.txt\
en_UM.txt en_US.txt en_US_POSIX.txt en_VI.txt en_ZA.txt en_ZW.txt\
eo.txt\
es.txt es_AR.txt es_BO.txt es_CL.txt es_CO.txt\
es_CR.txt es_DO.txt es_EC.txt es_ES.txt es_ES_EURO.txt\
es_GT.txt es_HN.txt es_MX.txt es_NI.txt es_PA.txt\
es_PE.txt es_PR.txt es_PY.txt es_SV.txt es_US.txt\
es_UY.txt es_VE.txt\
et.txt et_EE.txt\
eu.txt eu_ES.txt eu_ES_EURO.txt\
fa.txt fa_IR.txt\
fi.txt fi_FI.txt fi_FI_EURO.txt\
fo.txt fo_FO.txt\
fr.txt fr_BE.txt fr_BE_EURO.txt fr_CA.txt fr_CH.txt\
fr_FR.txt fr_FR_EURO.txt fr_LU.txt fr_LU_EURO.txt\
ga.txt ga_IE.txt ga_IE_EURO.txt\
gl.txt gl_ES.txt gl_ES_EURO.txt\
gv.txt gv_GB.txt\
he.txt he_IL.txt\
hi.txt hi_IN.txt\
hr.txt hr_HR.txt\
hu.txt hu_HU.txt\
id.txt id_ID.txt\
is.txt is_IS.txt\
it.txt it_CH.txt it_IT.txt it_IT_EURO.txt\
iw.txt iw_IL.txt\
ja.txt ja_JP.txt\
kl.txt kl_GL.txt\
ko.txt ko__LOTUS.txt ko_KR.txt\
kok.txt kok_IN.txt\
kw.txt kw_GB.txt\
lt.txt lt_LT.txt\
lv.txt lv_LV.txt\
mk.txt mk_MK.txt\
mr.txt mr_IN.txt\
mt.txt mt_MT.txt\
nb.txt nb_NO.txt\
nl.txt nl_BE.txt nl_BE_EURO.txt nl_NL.txt nl_NL_EURO.txt\
nn.txt nn_NO.txt\
no.txt no_NO.txt no_NO_NY.txt\
pl.txt pl_PL.txt\
pt.txt pt_BR.txt pt_PT.txt pt_PT_EURO.txt\
ro.txt ro_RO.txt\
ru.txt ru_RU.txt ru_UA.txt\
sh.txt sh_YU.txt\
sk.txt sk_SK.txt\
sl.txt sl_SI.txt\
sq.txt sq_AL.txt\
sr.txt sr_YU.txt\
sv.txt sv_FI.txt sv_FI_AL.txt sv_SE.txt\
sw.txt sw_KE.txt sw_TZ.txt\
ta.txt ta_IN.txt\
te.txt te_IN.txt\
th.txt th_TH.txt\
tr.txt tr_TR.txt\
uk.txt uk_UA.txt\
vi.txt vi_VN.txt\
zh.txt zh__PINYIN.txt zh_CN.txt zh_HK.txt zh_SG.txt zh_TW.txt zh_TW_STROKE.txt

TRANSLIT_SOURCE=fullhalf.txt translit_index.txt kana.txt kbdescl1.txt\
larabic.txt lcyril.txt ldevan.txt\
lgreek.txt lhebrew.txt ljamo.txt\
lkana.txt quotes.txt ucname.txt\
Bengali_InterIndic.txt\
Devanagari_InterIndic.txt\
Gujarati_InterIndic.txt\
Gurmukhi_InterIndic.txt\
Kannada_InterIndic.txt\
Malayalam_InterIndic.txt\
Oriya_InterIndic.txt\
Tamil_InterIndic.txt\
Telugu_InterIndic.txt\
InterIndic_Bengali.txt\
InterIndic_Devanagari.txt\
InterIndic_Gujarati.txt\
InterIndic_Gurmukhi.txt\
InterIndic_Kannada.txt\
InterIndic_Malayalam.txt\
InterIndic_Oriya.txt\
InterIndic_Tamil.txt\
InterIndic_Telugu.txt

