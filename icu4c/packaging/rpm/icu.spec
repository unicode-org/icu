#
# RPM specification file for ICU.
#
# Yves Arrouye <yves@realnames.com>

# This file can be freely redistributed under the same license as ICU, namely
# the IBM Public License.

# Disclaimer: this is my first and only RPM spec file. Bear with me, and do
# not hesitate to report bugs in this spec file using the ICU bug reporting
# system. There will be a better spec file later, using files -f and other
# nice things like that.

# Additional disclaimer: this file was made for ICU 1.6.0.1 and was not
# checked for completeness for ICU 1.7.

Name: icu
Version: 1.7
Release: 2
Requires: libicu17 >= 1.7
Summary: International Components for Unicode
Copyright: IBM Public License
Group: System Environment/Libraries
Source: icu-1.7.tar.gz
BuildRoot: /var/tmp/%{name}-buildroot
%description
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the runtime libraries for ICU. It does
not contain any of the data files needed at runtime and present in the
`icu' and `icu-locales` packages.

%package -n libicu17
Summary: International Components for Unicode (libraries)
Group: Development/Libraries
%description -n libicu17
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the runtime libraries for ICU. It does
not contain any of the data files needed at runtime and present in the
`icu' and `icu-locales` packages.

%package -n libicu-devel
Summary: International Components for Unicode (development files)
Group: Development/Libraries
Requires: libicu17 = 1.7
%description -n libicu-devel
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the development files for ICU.

%package locales
Summary: Locale data for ICU
Group: System Environment/Libraries
Requires: libicu17 >= 1.7
%description locales
The locale data are used by ICU to provide localization (l10n) and
internationalization (i18n) support to ICU applications. This package
also contains break data for various languages, and transliteration data.

%post icu
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

ICU_DATA=/usr/share/icu/1.7
export ICU_DATA
if test ! -f $ICU_DATA/cnvalias.dat -o /etc/icu/convrtrs.txt -nt $ICU_DATA/cnvalias.dat
then
    echo Compiling converters and aliases list from /etc/icu/convrtrs.txt
    /usr/sbin/gencnval /etc/icu/convrtrs.txt
fi

%preun icu
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n -e '/\/1.7\//d -e 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%post -n libicu17
ldconfig

# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%preun -n libicu17
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n -e '/\/1.7\//d -e 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%prep
%setup -q

%build
cd source
CFLAGS="-O2" CXXFLAGS="-O2" ./configure --prefix=/usr --sysconfdir=/etc --with-data-packaging=files --disable-rpath --enable-shared --enable-static --disable-samples
make RPM_OPT_FLAGS="$RPM_OPT_FLAGS"

%install
rm -rf $RPM_BUILD_ROOT
cd source
make install DESTDIR=$RPM_BUILD_ROOT

%files
%defattr(-,root,root)
%doc readme.html
%doc license.html
%config /etc/icu/convrtrs.txt
/usr/share/icu/1.7/README
/usr/lib/icu/1.7/cns-11643-1992.cnv
/usr/lib/icu/1.7/ebcdic-xml-us.cnv
/usr/lib/icu/1.7/gb18030.cnv
/usr/lib/icu/1.7/gb_2312_80-1.cnv
/usr/lib/icu/1.7/ibm-1038.cnv
/usr/lib/icu/1.7/ibm-1047-s390.cnv
/usr/lib/icu/1.7/ibm-1047.cnv
/usr/lib/icu/1.7/ibm-1051.cnv
/usr/lib/icu/1.7/ibm-1089.cnv
/usr/lib/icu/1.7/ibm-1123.cnv
/usr/lib/icu/1.7/ibm-1140-s390.cnv
/usr/lib/icu/1.7/ibm-1140.cnv
/usr/lib/icu/1.7/ibm-1141.cnv
/usr/lib/icu/1.7/ibm-1142-s390.cnv
/usr/lib/icu/1.7/ibm-1142.cnv
/usr/lib/icu/1.7/ibm-1143-s390.cnv
/usr/lib/icu/1.7/ibm-1143.cnv
/usr/lib/icu/1.7/ibm-1144-s390.cnv
/usr/lib/icu/1.7/ibm-1144.cnv
/usr/lib/icu/1.7/ibm-1145.cnv
/usr/lib/icu/1.7/ibm-1145-s390.cnv
/usr/lib/icu/1.7/ibm-1146-s390.cnv
/usr/lib/icu/1.7/ibm-1146.cnv
/usr/lib/icu/1.7/ibm-1147-s390.cnv
/usr/lib/icu/1.7/ibm-1147.cnv
/usr/lib/icu/1.7/ibm-1148-s390.cnv
/usr/lib/icu/1.7/ibm-1148.cnv
/usr/lib/icu/1.7/ibm-1149-s390.cnv
/usr/lib/icu/1.7/ibm-1149.cnv
/usr/lib/icu/1.7/ibm-1153-s390.cnv
/usr/lib/icu/1.7/ibm-1153.cnv
/usr/lib/icu/1.7/ibm-1154.cnv
/usr/lib/icu/1.7/ibm-1155.cnv
/usr/lib/icu/1.7/ibm-1156.cnv
/usr/lib/icu/1.7/ibm-1157.cnv
/usr/lib/icu/1.7/ibm-1158.cnv
/usr/lib/icu/1.7/ibm-1159.cnv
/usr/lib/icu/1.7/ibm-1160.cnv
/usr/lib/icu/1.7/ibm-1162.cnv
/usr/lib/icu/1.7/ibm-1164.cnv
/usr/lib/icu/1.7/ibm-1250.cnv
/usr/lib/icu/1.7/ibm-1251.cnv
/usr/lib/icu/1.7/ibm-1252.cnv
/usr/lib/icu/1.7/tz.dat
/usr/lib/icu/1.7/ibm-1253.cnv
/usr/lib/icu/1.7/ibm-1254.cnv
/usr/lib/icu/1.7/ibm-1255.cnv
/usr/lib/icu/1.7/ibm-1256.cnv
/usr/lib/icu/1.7/ibm-1257.cnv
/usr/lib/icu/1.7/ibm-1258.cnv
/usr/lib/icu/1.7/ibm-12712-s390.cnv
/usr/lib/icu/1.7/ibm-12712.cnv
/usr/lib/icu/1.7/ibm-1275.cnv
/usr/lib/icu/1.7/ibm-1276.cnv
/usr/lib/icu/1.7/ibm-1277.cnv
/usr/lib/icu/1.7/ibm-1280.cnv
/usr/lib/icu/1.7/ibm-1281.cnv
/usr/lib/icu/1.7/ibm-1282.cnv
/usr/lib/icu/1.7/ibm-1283.cnv
/usr/lib/icu/1.7/ibm-1362.cnv
/usr/lib/icu/1.7/ibm-1363.cnv
/usr/lib/icu/1.7/ibm-1364.cnv
/usr/lib/icu/1.7/ibm-1370.cnv
/usr/lib/icu/1.7/ibm-1371.cnv
/usr/lib/icu/1.7/ibm-1383.cnv
/usr/lib/icu/1.7/ibm-1386.cnv
/usr/lib/icu/1.7/ibm-1388.cnv
/usr/lib/icu/1.7/ibm-1390.cnv
/usr/lib/icu/1.7/ibm-1399.cnv
/usr/lib/icu/1.7/ibm-16684.cnv
/usr/lib/icu/1.7/ibm-16804-s390.cnv
/usr/lib/icu/1.7/ibm-16804.cnv
/usr/lib/icu/1.7/ibm-17248.cnv
/usr/lib/icu/1.7/ibm-21427.cnv
/usr/lib/icu/1.7/ibm-273.cnv
/usr/lib/icu/1.7/ibm-277.cnv
/usr/lib/icu/1.7/ibm-278.cnv
/usr/lib/icu/1.7/ibm-280.cnv
/usr/lib/icu/1.7/ibm-284.cnv
/usr/lib/icu/1.7/ibm-285.cnv
/usr/lib/icu/1.7/ibm-290.cnv
/usr/lib/icu/1.7/ibm-297.cnv
/usr/lib/icu/1.7/ibm-33722.cnv
/usr/lib/icu/1.7/ibm-367.cnv
/usr/lib/icu/1.7/ibm-37-s390.cnv
/usr/lib/icu/1.7/ibm-37.cnv
/usr/lib/icu/1.7/ibm-420.cnv
/usr/lib/icu/1.7/ibm-424.cnv
/usr/lib/icu/1.7/ibm-437.cnv
/usr/lib/icu/1.7/ibm-4899.cnv
/usr/lib/icu/1.7/ibm-4909.cnv
/usr/lib/icu/1.7/ibm-4930.cnv
/usr/lib/icu/1.7/ibm-4971.cnv
/usr/lib/icu/1.7/ibm-500.cnv
/usr/lib/icu/1.7/ibm-5104.cnv
/usr/lib/icu/1.7/ibm-5123.cnv
/usr/lib/icu/1.7/ibm-5210.cnv
/usr/lib/icu/1.7/ibm-5346.cnv
/usr/lib/icu/1.7/ibm-5347.cnv
/usr/lib/icu/1.7/ibm-5349.cnv
/usr/lib/icu/1.7/ibm-5350.cnv
/usr/lib/icu/1.7/ibm-5351.cnv
/usr/lib/icu/1.7/ibm-5352.cnv
/usr/lib/icu/1.7/ibm-5353.cnv
/usr/lib/icu/1.7/ibm-5354.cnv
/usr/lib/icu/1.7/ibm-803.cnv
/usr/lib/icu/1.7/ibm-808.cnv
/usr/lib/icu/1.7/ibm-813.cnv
/usr/lib/icu/1.7/ibm-833.cnv
/usr/lib/icu/1.7/ibm-834.cnv
/usr/lib/icu/1.7/ibm-835.cnv
/usr/lib/icu/1.7/ibm-848.cnv
/usr/lib/icu/1.7/ibm-8482.cnv
/usr/lib/icu/1.7/ibm-849.cnv
/usr/lib/icu/1.7/ibm-850.cnv
/usr/lib/icu/1.7/ibm-851.cnv
/usr/lib/icu/1.7/ibm-852.cnv
/usr/lib/icu/1.7/ibm-855.cnv
/usr/lib/icu/1.7/ibm-856.cnv
/usr/lib/icu/1.7/ibm-857.cnv
/usr/lib/icu/1.7/ibm-858.cnv
/usr/lib/icu/1.7/ibm-859.cnv
/usr/lib/icu/1.7/ibm-860.cnv
/usr/lib/icu/1.7/ibm-861.cnv
/usr/lib/icu/1.7/ibm-862.cnv
/usr/lib/icu/1.7/ibm-863.cnv
/usr/lib/icu/1.7/ibm-864.cnv
/usr/lib/icu/1.7/ibm-865.cnv
/usr/lib/icu/1.7/ibm-866.cnv
/usr/lib/icu/1.7/ibm-867.cnv
/usr/lib/icu/1.7/ibm-868.cnv
/usr/lib/icu/1.7/ibm-869.cnv
/usr/lib/icu/1.7/ibm-871.cnv
/usr/lib/icu/1.7/ibm-872.cnv
/usr/lib/icu/1.7/ibm-874.cnv
/usr/lib/icu/1.7/ibm-878.cnv
/usr/lib/icu/1.7/ibm-901.cnv
/usr/lib/icu/1.7/ibm-902.cnv
/usr/lib/icu/1.7/ibm-9027.cnv
/usr/lib/icu/1.7/ibm-9044.cnv
/usr/lib/icu/1.7/ibm-9049.cnv
/usr/lib/icu/1.7/ibm-9061.cnv
/usr/lib/icu/1.7/ibm-912.cnv
/usr/lib/icu/1.7/ibm-913.cnv
/usr/lib/icu/1.7/ibm-914.cnv
/usr/lib/icu/1.7/ibm-915.cnv
/usr/lib/icu/1.7/ibm-916.cnv
/usr/lib/icu/1.7/ibm-920.cnv
/usr/lib/icu/1.7/ibm-921.cnv
/usr/lib/icu/1.7/ibm-922.cnv
/usr/lib/icu/1.7/ibm-923.cnv
/usr/lib/icu/1.7/ibm-9238.cnv
/usr/lib/icu/1.7/ibm-930.cnv
/usr/lib/icu/1.7/ibm-933.cnv
/usr/lib/icu/1.7/ibm-935.cnv
/usr/lib/icu/1.7/ibm-937.cnv
/usr/lib/icu/1.7/ibm-939.cnv
/usr/lib/icu/1.7/ibm-941.cnv
/usr/lib/icu/1.7/ibm-942.cnv
/usr/lib/icu/1.7/ibm-943.cnv
/usr/lib/icu/1.7/ibm-944.cnv
/usr/lib/icu/1.7/ibm-949.cnv
/usr/lib/icu/1.7/ibm-950.cnv
/usr/lib/icu/1.7/ibm-964.cnv
/usr/lib/icu/1.7/ibm-970.cnv
/usr/lib/icu/1.7/iso-ir-165.cnv
/usr/lib/icu/1.7/jisx-201.cnv
/usr/lib/icu/1.7/jisx-208.cnv
/usr/lib/icu/1.7/jisx-212.cnv
/usr/lib/icu/1.7/ksc_5601_1.cnv
/usr/lib/icu/1.7/lmb-excp.cnv
/usr/lib/icu/1.7/unames.dat
/usr/lib/icu/1.7/uprops.dat

/usr/sbin/genccode
/usr/sbin/gencmn
/usr/sbin/gencnval
/usr/sbin/gennames
/usr/sbin/genprops
/usr/sbin/genrb
/usr/sbin/gentest
/usr/sbin/gentz
/usr/sbin/makeconv
/usr/sbin/pkgdata

/usr/man/man5/convrtrs.txt.5.gz
/usr/man/man5/cnvalias.dat.5.gz
/usr/man/man8/makeconv.8.gz
/usr/man/man8/genrb.8.gz
/usr/man/man8/gencnval.8.gz

%files -n icu-locales
/usr/lib/icu/1.7/ar.res
/usr/lib/icu/1.7/be.res
/usr/lib/icu/1.7/char.brk
/usr/lib/icu/1.7/line.brk
/usr/lib/icu/1.7/line_th.brk
/usr/lib/icu/1.7/sent.brk
/usr/lib/icu/1.7/word.brk
/usr/lib/icu/1.7/word_th.brk
/usr/lib/icu/1.7/root.res
/usr/lib/icu/1.7/index.res
/usr/lib/icu/1.7/ar_AE.res
/usr/lib/icu/1.7/ar_BH.res
/usr/lib/icu/1.7/ar_DZ.res
/usr/lib/icu/1.7/ar_EG.res
/usr/lib/icu/1.7/ar_IQ.res
/usr/lib/icu/1.7/ar_JO.res
/usr/lib/icu/1.7/ar_KW.res
/usr/lib/icu/1.7/ar_LB.res
/usr/lib/icu/1.7/ar_LY.res
/usr/lib/icu/1.7/ar_MA.res
/usr/lib/icu/1.7/ar_OM.res
/usr/lib/icu/1.7/ar_QA.res
/usr/lib/icu/1.7/ar_SA.res
/usr/lib/icu/1.7/ar_SD.res
/usr/lib/icu/1.7/bg.res
/usr/lib/icu/1.7/ar_SY.res
/usr/lib/icu/1.7/ar_TN.res
/usr/lib/icu/1.7/ar_YE.res
/usr/lib/icu/1.7/be_BY.res
/usr/lib/icu/1.7/bg_BG.res
/usr/lib/icu/1.7/ca.res
/usr/lib/icu/1.7/ca_ES.res
/usr/lib/icu/1.7/ca_ES_EURO.res
/usr/lib/icu/1.7/cs.res
/usr/lib/icu/1.7/cs_CZ.res
/usr/lib/icu/1.7/da.res
/usr/lib/icu/1.7/da_DK.res
/usr/lib/icu/1.7/de.res
/usr/lib/icu/1.7/de_AT.res
/usr/lib/icu/1.7/de_AT_EURO.res
/usr/lib/icu/1.7/de_CH.res
/usr/lib/icu/1.7/de_DE.res
/usr/lib/icu/1.7/de_DE_EURO.res
/usr/lib/icu/1.7/de_LU.res
/usr/lib/icu/1.7/de_LU_EURO.res
/usr/lib/icu/1.7/el.res
/usr/lib/icu/1.7/el_GR.res
/usr/lib/icu/1.7/en.res
/usr/lib/icu/1.7/en_AU.res
/usr/lib/icu/1.7/en_CA.res
/usr/lib/icu/1.7/en_BE.res
/usr/lib/icu/1.7/en_GB.res
/usr/lib/icu/1.7/en_IE.res
/usr/lib/icu/1.7/en_IE_EURO.res
/usr/lib/icu/1.7/en_NZ.res
/usr/lib/icu/1.7/en_US.res
/usr/lib/icu/1.7/en_ZA.res
/usr/lib/icu/1.7/es.res
/usr/lib/icu/1.7/es_AR.res
/usr/lib/icu/1.7/es_BO.res
/usr/lib/icu/1.7/es_CL.res
/usr/lib/icu/1.7/es_CO.res
/usr/lib/icu/1.7/es_CR.res
/usr/lib/icu/1.7/es_DO.res
/usr/lib/icu/1.7/es_EC.res
/usr/lib/icu/1.7/es_ES.res
/usr/lib/icu/1.7/es_ES_EURO.res
/usr/lib/icu/1.7/es_GT.res
/usr/lib/icu/1.7/es_HN.res
/usr/lib/icu/1.7/es_MX.res
/usr/lib/icu/1.7/es_NI.res
/usr/lib/icu/1.7/es_PA.res
/usr/lib/icu/1.7/es_PE.res
/usr/lib/icu/1.7/es_PR.res
/usr/lib/icu/1.7/es_PY.res
/usr/lib/icu/1.7/es_SV.res
/usr/lib/icu/1.7/es_UY.res
/usr/lib/icu/1.7/es_VE.res
/usr/lib/icu/1.7/et.res
/usr/lib/icu/1.7/et_EE.res
/usr/lib/icu/1.7/fi.res
/usr/lib/icu/1.7/fi_FI.res
/usr/lib/icu/1.7/fr.res
/usr/lib/icu/1.7/fi_FI_EURO.res
/usr/lib/icu/1.7/fr_BE.res
/usr/lib/icu/1.7/fr_BE_EURO.res
/usr/lib/icu/1.7/fr_CA.res
/usr/lib/icu/1.7/fr_CH.res
/usr/lib/icu/1.7/fr_FR.res
/usr/lib/icu/1.7/fr_FR_EURO.res
/usr/lib/icu/1.7/fr_LU.res
/usr/lib/icu/1.7/fr_LU_EURO.res
/usr/lib/icu/1.7/he.res
/usr/lib/icu/1.7/he_IL.res
/usr/lib/icu/1.7/hr.res
/usr/lib/icu/1.7/hr_HR.res
/usr/lib/icu/1.7/hu.res
/usr/lib/icu/1.7/hu_HU.res
/usr/lib/icu/1.7/is.res
/usr/lib/icu/1.7/is_IS.res
/usr/lib/icu/1.7/it.res
/usr/lib/icu/1.7/it_CH.res
/usr/lib/icu/1.7/it_IT.res
/usr/lib/icu/1.7/it_IT_EURO.res
/usr/lib/icu/1.7/iw.res
/usr/lib/icu/1.7/iw_IL.res
/usr/lib/icu/1.7/ja.res
/usr/lib/icu/1.7/ja_JP.res
/usr/lib/icu/1.7/ko.res
/usr/lib/icu/1.7/ko_KR.res
/usr/lib/icu/1.7/lt.res
/usr/lib/icu/1.7/lt_LT.res
/usr/lib/icu/1.7/lv.res
/usr/lib/icu/1.7/lv_LV.res
/usr/lib/icu/1.7/mk.res
/usr/lib/icu/1.7/mk_MK.res
/usr/lib/icu/1.7/nl.res
/usr/lib/icu/1.7/nl_BE.res
/usr/lib/icu/1.7/nl_BE_EURO.res
/usr/lib/icu/1.7/nl_NL.res
/usr/lib/icu/1.7/nl_NL_EURO.res
/usr/lib/icu/1.7/no.res
/usr/lib/icu/1.7/no_NO.res
/usr/lib/icu/1.7/no_NO_NY.res
/usr/lib/icu/1.7/pl.res
/usr/lib/icu/1.7/pl_PL.res
/usr/lib/icu/1.7/pt.res
/usr/lib/icu/1.7/pt_BR.res
/usr/lib/icu/1.7/ro.res
/usr/lib/icu/1.7/pt_PT.res
/usr/lib/icu/1.7/pt_PT_EURO.res
/usr/lib/icu/1.7/ro_RO.res
/usr/lib/icu/1.7/ru.res
/usr/lib/icu/1.7/ru_RU.res
/usr/lib/icu/1.7/sh.res
/usr/lib/icu/1.7/sk.res
/usr/lib/icu/1.7/sh_YU.res
/usr/lib/icu/1.7/sk_SK.res
/usr/lib/icu/1.7/sl.res
/usr/lib/icu/1.7/sl_SI.res
/usr/lib/icu/1.7/sq.res
/usr/lib/icu/1.7/sq_AL.res
/usr/lib/icu/1.7/sr.res
/usr/lib/icu/1.7/sr_YU.res
/usr/lib/icu/1.7/sv.res
/usr/lib/icu/1.7/sv_SE.res
/usr/lib/icu/1.7/th.res
/usr/lib/icu/1.7/th_TH.res
/usr/lib/icu/1.7/tr.res
/usr/lib/icu/1.7/tr_TR.res
/usr/lib/icu/1.7/uk.res
/usr/lib/icu/1.7/uk_UA.res
/usr/lib/icu/1.7/vi.res
/usr/lib/icu/1.7/vi_VN.res
/usr/lib/icu/1.7/zh.res
/usr/lib/icu/1.7/zh_CN.res
/usr/lib/icu/1.7/hi.res
/usr/lib/icu/1.7/zh_HK.res
/usr/lib/icu/1.7/zh_TW.res
/usr/lib/icu/1.7/hi_IN.res
/usr/lib/icu/1.7/mt.res
/usr/lib/icu/1.7/mt_MT.res
/usr/lib/icu/1.7/eo.res
/usr/lib/icu/1.7/kok.res
/usr/lib/icu/1.7/kok_IN.res
/usr/lib/icu/1.7/ta.res
/usr/lib/icu/1.7/ta_IN.res
/usr/lib/icu/1.7/mr.res
/usr/lib/icu/1.7/mr_IN.res
/usr/lib/icu/1.7/af.res
/usr/lib/icu/1.7/af_ZA.res
/usr/lib/icu/1.7/en_BW.res
/usr/lib/icu/1.7/en_ZW.res
/usr/lib/icu/1.7/es_US.res
/usr/lib/icu/1.7/eu.res
/usr/lib/icu/1.7/eu_ES.res
/usr/lib/icu/1.7/fa.res
/usr/lib/icu/1.7/fa_IR.res
/usr/lib/icu/1.7/fo.res
/usr/lib/icu/1.7/fo_FO.res
/usr/lib/icu/1.7/ga.res
/usr/lib/icu/1.7/ga_IE.res
/usr/lib/icu/1.7/gl.res
/usr/lib/icu/1.7/gl_ES.res
/usr/lib/icu/1.7/gv.res
/usr/lib/icu/1.7/gv_GB.res
/usr/lib/icu/1.7/id.res
/usr/lib/icu/1.7/id_ID.res
/usr/lib/icu/1.7/kl.res
/usr/lib/icu/1.7/kl_GL.res
/usr/lib/icu/1.7/kw.res
/usr/lib/icu/1.7/kw_GB.res
/usr/lib/icu/1.7/ru_UA.res
/usr/lib/icu/1.7/sv_FI.res
/usr/lib/icu/1.7/fullhalf.res
/usr/lib/icu/1.7/translit_index.res
/usr/lib/icu/1.7/kana.res
/usr/lib/icu/1.7/kbdescl1.res
/usr/lib/icu/1.7/larabic.res
/usr/lib/icu/1.7/lcyril.res
/usr/lib/icu/1.7/ldevan.res
/usr/lib/icu/1.7/lgreek.res
/usr/lib/icu/1.7/lhebrew.res
/usr/lib/icu/1.7/ljamo.res
/usr/lib/icu/1.7/lkana.res
/usr/lib/icu/1.7/quotes.res
/usr/lib/icu/1.7/ucname.res

%files -n libicu17
%doc license.html
/usr/lib/libicui18n.so.17
/usr/lib/libicui18n.so.17.0
/usr/lib/libicutoolutil.so.17
/usr/lib/libicutoolutil.so.17.0
/usr/lib/libicuuc.so.17
/usr/lib/libicuuc.so.17.0
/usr/lib/libustdio.so.17
/usr/lib/libustdio.so.17.0

%files -n libicu-devel
%doc readme.html
%doc license.html
/usr/lib/libicui18n.so
/usr/lib/libicui18n.a
/usr/lib/libicuuc.so
/usr/lib/libicuuc.a
/usr/lib/libicutoolutil.so
/usr/lib/libicutoolutil.a
/usr/lib/libustdio.so
/usr/lib/libustdio.a
/usr/include/unicode/bidi.h
/usr/include/unicode/brkiter.h
/usr/include/unicode/calendar.h
/usr/include/unicode/chariter.h
/usr/include/unicode/choicfmt.h
/usr/include/unicode/coleitr.h
/usr/include/unicode/coll.h
/usr/include/unicode/convert.h
/usr/include/unicode/cpdtrans.h
/usr/include/unicode/datefmt.h
/usr/include/unicode/dbbi.h
/usr/include/unicode/dcfmtsym.h
/usr/include/unicode/decimfmt.h
/usr/include/unicode/dtfmtsym.h
/usr/include/unicode/fieldpos.h
/usr/include/unicode/fmtable.h
/usr/include/unicode/format.h
/usr/include/unicode/gregocal.h
/usr/include/unicode/hangjamo.h
/usr/include/unicode/hextouni.h
/usr/include/unicode/jamohang.h
/usr/include/unicode/locid.h
/usr/include/unicode/msgfmt.h
/usr/include/unicode/normlzr.h
/usr/include/unicode/nultrans.h
/usr/include/unicode/numfmt.h
/usr/include/unicode/parseerr.h
/usr/include/unicode/parsepos.h
/usr/include/unicode/platform.h
/usr/include/unicode/pmacos.h
/usr/include/unicode/pos2.h
/usr/include/unicode/pos400.h
/usr/include/unicode/putil.h
/usr/include/unicode/pwin32.h
/usr/include/unicode/rbbi.h
/usr/include/unicode/rbt.h
/usr/include/unicode/rep.h
/usr/include/unicode/resbund.h
/usr/include/unicode/schriter.h
/usr/include/unicode/scsu.h
/usr/include/unicode/simpletz.h
/usr/include/unicode/smpdtfmt.h
/usr/include/unicode/sortkey.h
/usr/include/unicode/tblcoll.h
/usr/include/unicode/timezone.h
/usr/include/unicode/translit.h
/usr/include/unicode/ubidi.h
/usr/include/unicode/ubrk.h
/usr/include/unicode/ucal.h
/usr/include/unicode/uchar.h
/usr/include/unicode/uchriter.h
/usr/include/unicode/ucnv.h
/usr/include/unicode/ucnv_cb.h
/usr/include/unicode/ucnv_err.h
/usr/include/unicode/ucol.h
/usr/include/unicode/udat.h
/usr/include/unicode/udata.h
/usr/include/unicode/uloc.h
/usr/include/unicode/umachine.h
/usr/include/unicode/umisc.h
/usr/include/unicode/umsg.h
/usr/include/unicode/unicode.h
/usr/include/unicode/unifilt.h
/usr/include/unicode/unifltlg.h
/usr/include/unicode/uniset.h
/usr/include/unicode/unistr.h
/usr/include/unicode/unitohex.h
/usr/include/unicode/unum.h
/usr/include/unicode/urep.h
/usr/include/unicode/ures.h
/usr/include/unicode/ushape.h
/usr/include/unicode/ustdio.h
/usr/include/unicode/ustring.h
/usr/include/unicode/utf.h
/usr/include/unicode/utf16.h
/usr/include/unicode/utf32.h
/usr/include/unicode/utf8.h
/usr/include/unicode/utrans.h
/usr/include/unicode/utypes.h
/usr/lib/icu/1.7/Makefile.inc
/usr/lib/icu/Makefile.inc
/usr/share/icu/1.7/config

