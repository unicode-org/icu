#
# RPM specification file for ICU.
#
# Neal Probert <nprobert@walid.com> is the current maintainer.
# Yves Arrouye <yves@realnames.com> is the original author.

# This file can be freely redistributed under the same license as ICU.

Name: icu
Version: 2.2
Release: 1
Requires: libicu22 >= 2.2
Summary: International Components for Unicode
Packager: Ian Holsman (CNET Networks) <ianh@cnet.com>
Copyright: IBM Public License
Group: System Environment/Libraries
Source: icu-2.2.tar.gz
BuildRoot: /var/tmp/%{name}
%description
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the runtime libraries for ICU. It does
not contain any of the data files needed at runtime and present in the
`icu' and `icu-locales` packages.

%package -n libicu22
Summary: International Components for Unicode (libraries)
Group: Development/Libraries
%description -n libicu22
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the runtime libraries for ICU. It does
not contain any of the data files needed at runtime and present in the
`icu' and `icu-locales` packages.

%package -n libicu-devel
Summary: International Components for Unicode (development files)
Group: Development/Libraries
Requires: libicu22 = 2.2
%description -n libicu-devel
ICU is a C++ and C library that provides robust and full-featured Unicode
support. This package contains the development files for ICU.

%package locales
Summary: Locale data for ICU
Group: System Environment/Libraries
Requires: libicu22 >= 2.2
%description locales
The locale data are used by ICU to provide localization (l10n) and
internationalization (i18n) support to ICU applications. This package
also contains break data for various languages, and transliteration data.

%post
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

ICU_DATA=/usr/share/icu/2.2
export ICU_DATA
if test ! -f $ICU_DATA/cnvalias.dat -o /etc/icu/convrtrs.txt -nt $ICU_DATA/cnvalias.dat
then
    echo Compiling converters and aliases list from /etc/icu/convrtrs.txt
    /usr/sbin/gencnval /etc/icu/convrtrs.txt
fi

%preun
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n -e '/\/2.2\//d -e 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%post -n libicu22
ldconfig

# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
cd /usr/lib/icu
rm -f current
if test x"$icucurrent" != x
then
    ln -s "$icucurrent" current
fi

%preun -n libicu22
# Adjust the current ICU link in /usr/lib/icu

icucurrent=`2>/dev/null ls -dp /usr/lib/icu/* | sed -n -e '/\/2.2\//d -e 's,.*/\([^/]*\)/$,\1,p'| sort -rn | head -1`
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
chmod a+x ./configure
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
/usr/share/icu/2.2/README
/usr/lib/icu/2.2/*.cnv
/usr/lib/icu/2.2/*.icu

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
/usr/sbin/gennames
/usr/sbin/gennorm
/usr/sbin/genprops
/usr/sbin/gentz
/usr/sbin/genuca

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

%files -n icu-locales
/usr/lib/icu/2.2/*.brk
/usr/lib/icu/2.2/*.res
%files -n libicu22
%doc license.html
/usr/lib/libicui18n.so.22
/usr/lib/libicui18n.so.22.0
/usr/lib/libicutoolutil.so.22
/usr/lib/libicutoolutil.so.22.0
/usr/lib/libicuuc.so.22
/usr/lib/libicuuc.so.22.0
/usr/lib/libustdio.so.22
/usr/lib/libustdio.so.22.0

%files -n libicu-devel
%doc readme.html
%doc license.html
/usr/lib/libicuctestfw.so
/usr/lib/libicuctestfw.a
/usr/lib/libicuctestfw.so.22
/usr/lib/libicuctestfw.so.22.0

/usr/lib/libicui18n.so
/usr/lib/libicui18n.a
/usr/lib/libicuuc.so
/usr/lib/libicuuc.a
/usr/lib/libicutoolutil.so
/usr/lib/libicutoolutil.a
/usr/lib/libustdio.so
/usr/lib/libustdio.a
/usr/include/unicode/*.h
/usr/lib/icu/2.2/Makefile.inc
/usr/lib/icu/Makefile.inc
/usr/share/icu/2.2/config
/usr/share/icu/2.2/README
/usr/share/doc/icu/*

%changelog
* Mon Sep 16 2002 Ian Holsman <ian@holsman.net> 
- update to icu 2.2
