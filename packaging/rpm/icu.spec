#   Copyright (C) 2000-2004, International Business Machines
#   Corporation and others.  All Rights Reserved.
#
# RPM specification file for ICU.
#
# Neal Probert <nprobert@walid.com> is the current maintainer.
# Yves Arrouye <yves@realnames.com> is the original author.

# This file can be freely redistributed under the same license as ICU.

Name: icu
Version: 3.0
Release: 1
Requires: libicu30 >= 3.0
Summary: International Components for Unicode
Packager: Ian Holsman (CNET Networks) <ianh@cnet.com>
Copyright: X License
Group: System Environment/Libraries
Source: icu-3.0.tgz
BuildRoot: /var/tmp/%{name}
%description
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the runtime libraries for ICU. It does
not contain any of the data files needed at runtime and present in the
`icu' and `icu-locales` packages.

%package -n libicu30
Summary: International Components for Unicode (libraries)
Group: Development/Libraries
%description -n libicu30
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the runtime libraries for ICU. It does
not contain any of the data files needed at runtime and present in the
`icu' and `icu-locales` packages.

%package -n libicu-devel
Summary: International Components for Unicode (development files)
Group: Development/Libraries
Requires: libicu30 = 3.0
%description -n libicu-devel
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the development files for ICU.

%package locales
Summary: Locale data for ICU
Group: System Environment/Libraries
Requires: libicu30 >= 3.0
%description locales
The locale data are used by ICU to provide localization (l10n) and
internationalization (i18n) support to ICU applications. This package
also contains break data for various languages, and transliteration data.

%post
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f /usr/lib/icu/current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

#ICU_DATA=/usr/lib/icu/3.0
#export ICU_DATA

%preun
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n -e '/\/3.0\//d' -e 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f /usr/lib/icu/current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%post -n libicu30
ldconfig

# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f /usr/lib/icu/current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%preun -n libicu30
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n -e '/\/3.0\//d' -e 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f /usr/lib/icu/current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%prep
%setup -q

%build
cd source
chmod a+x ./configure
CFLAGS="-O3" CXXFLAGS="-O" ./configure --prefix=/usr --sysconfdir=/etc --with-data-packaging=files --enable-shared --enable-static --disable-samples
echo 'CPPFLAGS += -DICU_DATA_DIR=\"/usr/lib/icu/3.0\"' >> icudefs.mk
make RPM_OPT_FLAGS="$RPM_OPT_FLAGS"

%install
rm -rf $RPM_BUILD_ROOT
cd source
make install DESTDIR=$RPM_BUILD_ROOT
# static causes a static icudata lib to be built... - it's not needed, remove it.
##cp stubdata/libicudata.a $RPM_BUILD_ROOT/usr/lib/icu/3.0/
rm -f $RPM_BUILD_ROOT/usr/lib/icu/3.0/libicudata.a


%files
%defattr(-,root,root)
%doc readme.html
%doc license.html
%config /etc/icu/convrtrs.txt
/usr/share/icu/3.0/README
/usr/share/icu/3.0/license.html
/usr/lib/icu/3.0/*.cnv
/usr/lib/icu/3.0/*.icu

/usr/bin/derb
/usr/bin/genbrk
/usr/bin/gencnval
/usr/bin/genrb
/usr/bin/icu-config
/usr/bin/makeconv
/usr/bin/pkgdata
/usr/bin/uconv

/usr/sbin/decmn
/usr/sbin/genccode
/usr/sbin/gencmn
/usr/sbin/gennames
/usr/sbin/gennorm
/usr/sbin/genpname
/usr/sbin/genprops
/usr/sbin/gensprep
/usr/sbin/genuca
/usr/sbin/icuswap
/usr/share/icu/3.0/mkinstalldirs

/usr/man/man1/gencnval.1.*
/usr/man/man1/derb.1.*
/usr/man/man1/genrb.1.*
/usr/man/man1/icu-config.1.*
/usr/man/man1/makeconv.1.*
/usr/man/man1/pkgdata.1.*
/usr/man/man1/uconv.1.*
/usr/man/man8/decmn.8.*
/usr/man/man8/genccode.8.*
/usr/man/man8/gencmn.8.*
/usr/man/man8/gennames.8.*
/usr/man/man8/gennorm.8.*
/usr/man/man8/genprops.8.*
/usr/man/man8/genuca.8.*
/usr/man/man8/genidna.8.*

%files -n icu-locales
/usr/lib/icu/3.0/*.brk
/usr/lib/icu/3.0/*.res
%files -n libicu30
%doc license.html
/usr/lib/libicui18n.so.30
/usr/lib/libicui18n.so.30.0
/usr/lib/libicutu.so.30
/usr/lib/libicutu.so.30.0
/usr/lib/libicuuc.so.30
/usr/lib/libicuuc.so.30.0
/usr/lib/libicudata.so.30
/usr/lib/libicudata.so.30.0
/usr/lib/libicuio.so.30
/usr/lib/libicuio.so.30.0
/usr/lib/libiculx.so.30
/usr/lib/libiculx.so.30.0
/usr/lib/libicule.so.30
/usr/lib/libicule.so.30.0

%files -n libicu-devel
%doc readme.html
%doc license.html
/usr/lib/libicui18n.so
/usr/lib/libsicui18n.a
/usr/lib/libicuuc.so
/usr/lib/libsicuuc.a
/usr/lib/libicutu.so
/usr/lib/libsicutu.a
/usr/lib/libicuio.so
/usr/lib/libsicuio.a
/usr/lib/libicudata.so
/usr/lib/libsicudata.a
/usr/lib/libicule.so
/usr/lib/libsicule.a
/usr/lib/libiculx.so
/usr/lib/libsiculx.a
/usr/include/unicode/*.h
/usr/include/layout/*.h
/usr/lib/icu/3.0/Makefile.inc
/usr/lib/icu/Makefile.inc
/usr/share/icu/3.0/config
/usr/share/icu/3.0/README
/usr/share/doc/icu-3.0/*

%changelog
* Tue Aug 16 2003 Steven Loomis <srl@jtcsv.com>
- update to 2.6.1 - include license
* Thu Jun 05 2003 Steven Loomis <srl@jtcsv.com>
- Update to 2.6
* Fri Dec 27 2002 Steven Loomis <srl@jtcsv.com>
- Update to 2.4 spec
* Fri Sep 27 2002 Steven Loomis <srl@jtcsv.com>
- minor updates to 2.2 spec. Rpath is off by default, don't pass it as an option.
* Mon Sep 16 2002 Ian Holsman <ian@holsman.net> 
- update to icu 2.2

