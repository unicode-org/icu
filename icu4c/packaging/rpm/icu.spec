#   Copyright (C) 2000-2003, International Business Machines
#   Corporation and others.  All Rights Reserved.
#
# RPM specification file for ICU.
#
# Neal Probert <nprobert@walid.com> is the current maintainer.
# Yves Arrouye <yves@realnames.com> is the original author.

# This file can be freely redistributed under the same license as ICU.

Name: icu
Version: 2.6.1
Release: 1
Requires: libicu26 >= 2.6.1
Summary: International Components for Unicode
Packager: Ian Holsman (CNET Networks) <ianh@cnet.com>
Copyright: X License
Group: System Environment/Libraries
Source: icu-2.6.1.tar.gz
BuildRoot: /var/tmp/%{name}
%description
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the runtime libraries for ICU. It does
not contain any of the data files needed at runtime and present in the
`icu' and `icu-locales` packages.

%package -n libicu26
Summary: International Components for Unicode (libraries)
Group: Development/Libraries
%description -n libicu26
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the runtime libraries for ICU. It does
not contain any of the data files needed at runtime and present in the
`icu' and `icu-locales` packages.

%package -n libicu-devel
Summary: International Components for Unicode (development files)
Group: Development/Libraries
Requires: libicu26 = 2.6.1
%description -n libicu-devel
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the development files for ICU.

%package locales
Summary: Locale data for ICU
Group: System Environment/Libraries
Requires: libicu26 >= 2.6.1
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

ICU_DATA=/usr/lib/icu/2.6.1
export ICU_DATA
if test ! -f $ICU_DATA/cnvalias.dat -o /etc/icu/convrtrs.txt -nt $ICU_DATA/cnvalias.dat
then
    echo Compiling converters and aliases list from /etc/icu/convrtrs.txt
    /usr/bin/gencnval /etc/icu/convrtrs.txt
fi

%preun
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n -e '/\/2.6.1\//d' -e 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f /usr/lib/icu/current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%post -n libicu26
ldconfig

# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f /usr/lib/icu/current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%preun -n libicu26
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n -e '/\/2.6.1\//d' -e 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
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
CFLAGS="-O3" CXXFLAGS="-O" ./configure --prefix=/usr --sysconfdir=/etc --with-data-packaging=files  --enable-shared --enable-static --disable-samples
echo 'CPPFLAGS += -DICU_DATA_DIR=\"/usr/lib/icu/2.6.1\"' >> icudefs.mk
make RPM_OPT_FLAGS="$RPM_OPT_FLAGS"

%install
rm -rf $RPM_BUILD_ROOT
cd source
make install DESTDIR=$RPM_BUILD_ROOT
# static causes a static icudata lib to be built... - it's not needed, remove it.
##cp stubdata/libicudata.a $RPM_BUILD_ROOT/usr/lib/icu/2.6.1/
rm -f $RPM_BUILD_ROOT/usr/lib/icu/2.6.1/libicudata.a


%files
%defattr(-,root,root)
%doc readme.html
%doc license.html
%config /etc/icu/convrtrs.txt
/usr/share/icu/2.6.1/README
/usr/share/icu/2.6.1/license.html
/usr/lib/icu/2.6.1/*.cnv
/usr/lib/icu/2.6.1/*.icu

/usr/bin/derb
/usr/bin/gencnval
/usr/bin/genbrk
/usr/bin/genrb
/usr/bin/makeconv
/usr/bin/icu-config
/usr/bin/pkgdata
/usr/bin/uconv

/usr/sbin/decmn
/usr/sbin/genccode
/usr/sbin/gencmn
/usr/sbin/genpname
/usr/sbin/gennames
/usr/sbin/gennorm
/usr/sbin/genprops
/usr/sbin/gentz
/usr/sbin/genuca
/usr/sbin/genidna
/usr/share/icu/2.6.1/mkinstalldirs

/usr/man/man1/gencnval.1.gz
/usr/man/man1/genrb.1.gz
/usr/man/man1/icu-config.1.gz
/usr/man/man1/makeconv.1.gz
/usr/man/man1/pkgdata.1.gz
/usr/man/man1/uconv.1.gz
/usr/man/man5/convrtrs.txt.5.gz
/usr/man/man5/cnvalias.dat.5.gz
/usr/man/man8/decmn.8.gz
/usr/man/man8/genccode.8.gz
/usr/man/man8/gencmn.8.gz
/usr/man/man8/gennames.8.gz
/usr/man/man8/gennorm.8.gz
/usr/man/man8/genprops.8.gz
/usr/man/man8/genuca.8.gz
/usr/man/man8/genidna.8.gz

%files -n icu-locales
/usr/lib/icu/2.6.1/*.brk
/usr/lib/icu/2.6.1/*.res
%files -n libicu26
%doc license.html
/usr/lib/libicui18n.so.26
/usr/lib/libicui18n.so.26.1
/usr/lib/libicutoolutil.so.26
/usr/lib/libicutoolutil.so.26.1
/usr/lib/libicuuc.so.26
/usr/lib/libicuuc.so.26.1
/usr/lib/libicudata.so.26
/usr/lib/libicudata.so.26.1
/usr/lib/libicuio.so.26
/usr/lib/libicuio.so.26.1
/usr/lib/libiculx.so.26
/usr/lib/libiculx.so.26.1
/usr/lib/libicule.so.26
/usr/lib/libicule.so.26.1

%files -n libicu-devel
%doc readme.html
%doc license.html
/usr/lib/libicui18n.so
/usr/lib/libicui18n.a
/usr/lib/libicuuc.so
/usr/lib/libicuuc.a
/usr/lib/libicutoolutil.so
/usr/lib/libicutoolutil.a
/usr/lib/libicuio.so
/usr/lib/libicuio.a
/usr/lib/libicudata.so
/usr/lib/libicudata.a
/usr/lib/libicule.so
/usr/lib/libiculx.so
/usr/lib/libicule.a
/usr/lib/libiculx.a
/usr/include/unicode/*.h
/usr/include/layout/*.h
/usr/lib/icu/2.6.1/Makefile.inc
/usr/lib/icu/Makefile.inc
/usr/share/icu/2.6.1/config
/usr/share/icu/2.6.1/README
/usr/share/doc/icu-2.6.1/*

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

