dnl aclocal.m4 for ICU
dnl Copyright (c) 1999-2004, International Business Machines Corporation and
dnl others. All Rights Reserved.
dnl Stephen F. Booth

dnl @TOP@

dnl ICU_CHECK_MH_FRAG
AC_DEFUN(ICU_CHECK_MH_FRAG, [
	AC_CACHE_CHECK(
		[which Makefile fragment to use],
		[icu_cv_host_frag],
		[
case "${host}" in
*-*-solaris*)
	if test "$GCC" = yes; then	
		icu_cv_host_frag=mh-solaris-gcc 
	else
		icu_cv_host_frag=mh-solaris
	fi ;;
alpha*-*-linux-gnu)
	if test "$GCC" = yes; then
		icu_cv_host_frag=mh-alpha-linux-gcc
	else  
		icu_cv_host_frag=mh-alpha-linux-cc
	fi ;;
*-*-linux*) icu_cv_host_frag=mh-linux ;;
*-*-cygwin)
	if test "$ac_cv_prog_gcc" = yes; then
	  	icu_cv_host_frag=mh-cygwin
	else
	  	icu_cv_host_frag=mh-cygwin-msvc
	fi ;;
*-*-*bsd*) 	icu_cv_host_frag=mh-bsd-gcc ;;
*-*-aix*)
	if test "$GCC" = yes; then
		icu_cv_host_frag=mh-aix-gcc
	else
		if test -n "`$CXX --help 2>&1 | grep 'IBM C and C++ Compilers$'`"; then
			icu_cv_host_frag=mh-aix
		else
			icu_cv_host_frag=mh-aix-va
		fi
	fi ;;
*-*-hpux*)
	if test "$GCC" = yes; then
		icu_cv_host_frag=mh-hpux-gcc
	else
		case "$CXX" in
		*aCC)    icu_cv_host_frag=mh-hpux-acc ;;
		*CC)     icu_cv_host_frag=mh-hpux-cc ;;
		esac
	fi ;;
*-*ibm-openedition*|*-*-os390*)	icu_cv_host_frag=mh-os390 ;;
*-*-os400*)	icu_cv_host_frag=mh-os400 ;;
*-apple-rhapsody*)	icu_cv_host_frag=mh-darwin ;;
*-apple-darwin*)	icu_cv_host_frag=mh-darwin ;;
*-*-beos)	icu_cv_host_frag=mh-beos ;;
*-*-irix*)	icu_cv_host_frag=mh-irix ;;
*-dec-osf*) icu_cv_host_frag=mh-alpha-osf ;;
*-*-nto*)	icu_cv_host_frag=mh-qnx ;;
*-ncr-*)	icu_cv_host_frag=mh-mpras ;;
*-sequent-*) 	icu_cv_host_frag=mh-ptx ;;
*) 		icu_cv_host_frag=mh-unknown ;;
esac
		]
	)
])

dnl ICU_CONDITIONAL - similar example taken from Automake 1.4
AC_DEFUN(ICU_CONDITIONAL,
[AC_SUBST($1_TRUE)
if $2; then
  $1_TRUE=
else
  $1_TRUE='#'
fi])

dnl AC_SEARCH_LIBS_FIRST(FUNCTION, SEARCH-LIBS [, ACTION-IF-FOUND
dnl            [, ACTION-IF-NOT-FOUND [, OTHER-LIBRARIES]]])
dnl Search for a library defining FUNC, then see if it's not already available.
 
AC_DEFUN(AC_SEARCH_LIBS_FIRST,
[AC_PREREQ([2.13])
AC_CACHE_CHECK([for library containing $1], [ac_cv_search_$1],
[ac_func_search_save_LIBS="$LIBS"
ac_cv_search_$1="no"
for i in $2; do
LIBS="-l$i $5 $ac_func_search_save_LIBS"
AC_TRY_LINK_FUNC([$1],
[ac_cv_search_$1="-l$i"
break])
done
if test "$ac_cv_search_$1" = "no"; then
AC_TRY_LINK_FUNC([$1], [ac_cv_search_$1="none required"])
fi
LIBS="$ac_func_search_save_LIBS"])
if test "$ac_cv_search_$1" != "no"; then
  test "$ac_cv_search_$1" = "none required" || LIBS="$ac_cv_search_$1 $LIBS"
  $3
else :
  $4
fi])

dnl Check if we can build and use 64-bit libraries
AC_DEFUN(AC_CHECK_64BIT_LIBS,
[
    AC_ARG_ENABLE(64bit-libs,
        [  --enable-64bit-libs     build 64-bit libraries [default=yes]],
        [ENABLE_64BIT_LIBS=${enableval}],
        [ENABLE_64BIT_LIBS=yes]
    )
    dnl These results can't be cached because is sets compiler flags.
    AC_MSG_CHECKING([for 64-bit executable support])
    if test "$ENABLE_64BIT_LIBS" = no; then
        case "${host}" in
        *-*-hpux*)
#            case "${CXX}" in
#            *CC)
#                CFLAGS="${CFLAGS} +DAportable"
#                CXXFLAGS="${CXXFLAGS} +DAportable"
#                ;;
#            esac;;
        esac
    else
        case "${host}" in
        *-*-solaris*)
            if test "$GCC" = no; then
                SOL64=`$CXX -xarch=v9 2>&1 && $CC -xarch=v9 2>&1 | grep -v usage:`
                SPARCV9=`isainfo -n 2>&1 | grep sparcv9`
                if test -z "$SOL64" && test -n "$SPARCV9"; then
                    CFLAGS="${CFLAGS} -xtarget=ultra -xarch=v9"
                    CXXFLAGS="${CXXFLAGS} -xtarget=ultra -xarch=v9"
                    LDFLAGS="${LDFLAGS} -xtarget=ultra -xarch=v9"
                    ENABLE_64BIT_LIBS=yes
                else
                    ENABLE_64BIT_LIBS=no
                fi
            else
                ENABLE_64BIT_LIBS=no
            fi
            ;;
        ia64-*-linux*)
            if test "$GCC" = yes; then
                # gcc compiler support
                if test -n "`$CXX -dumpspecs 2>&1 && $CC -dumpspecs 2>&1 | grep -v __LP64__`"; then
                    ENABLE_64BIT_LIBS=yes
                else
                    ENABLE_64BIT_LIBS=no
                fi
            else
                # check for ecc/ecpc compiler support
                if test -n "`$CXX --help 2>&1 && $CC --help 2>&1 | grep -v Intel`"; then
                    if test -n "`$CXX --help 2>&1 && $CC --help 2>&1 | grep -v Itanium`"; then
                        ENABLE_64BIT_LIBS=yes
                    else
                        ENABLE_64BIT_LIBS=no
                    fi
                else
                    # unknown
                    ENABLE_64BIT_LIBS=no
                fi
            fi
            ;;
        x86_64-*-linux*)
            if test "$GCC" = yes; then
                if test -n "`$CXX -dumpspecs 2>&1 && $CC -dumpspecs 2>&1 | grep -v __LP64__`"; then
                    ENABLE_64BIT_LIBS=yes
                else
                    ENABLE_64BIT_LIBS=no
                fi
            else
                # unknown
                ENABLE_64BIT_LIBS=no
            fi
            ;;
        *-*-aix*)
            if test "$ac_cv_prog_gcc" = no; then
                # Note: Have not tested 64-bitness with gcc.
                # Maybe the flag "-maix64" could be used with gcc?
                OLD_CFLAGS="${CFLAGS}"
                OLD_CXXFLAGS="${CXXFLAGS}"
                OLD_LDFLAGS="${LDFLAGS}"
                CFLAGS="${CFLAGS} -q64"
                CXXFLAGS="${CXXFLAGS} -q64"
                LDFLAGS="${LDFLAGS} -q64"
                AC_TRY_RUN(int main(void) {return 0;},
                   ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no, ENABLE_64BIT_LIBS=no)
                if test "$ENABLE_64BIT_LIBS" = no; then
                    CFLAGS="${OLD_CFLAGS}"
                    CXXFLAGS="${OLD_CXXFLAGS}"
                    LDFLAGS="${OLD_LDFLAGS}"
                else
                    ARFLAGS="${ARFLAGS} -X64"
                fi
            fi
            ;;
        *-*-hpux*)
            dnl First we try the newer +DD64, if that doesn't work,
            dnl try other options.

            OLD_CFLAGS="${CFLAGS}"
            OLD_CXXFLAGS="${CXXFLAGS}"
            CFLAGS="${CFLAGS} +DD64"
            CXXFLAGS="${CXXFLAGS} +DD64"
            AC_TRY_RUN(int main(void) {return 0;},
                ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no, ENABLE_64BIT_LIBS=no)
            if test "$ENABLE_64BIT_LIBS" = no; then
                CFLAGS="${OLD_CFLAGS}"
                CXXFLAGS="${OLD_CXXFLAGS}"
                CFLAGS="${CFLAGS} +DA2.0W"
                CXXFLAGS="${CXXFLAGS} +DA2.0W"
                AC_TRY_RUN(int main(void) {return 0;},
                    ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no, ENABLE_64BIT_LIBS=no)
                if test "$ENABLE_64BIT_LIBS" = no; then
                    CFLAGS="${OLD_CFLAGS}"
                    CXXFLAGS="${OLD_CXXFLAGS}"
                fi
            fi
            ;;
        *)
            ENABLE_64BIT_LIBS=no
            ;;
        esac
    fi
    dnl Individual tests that fail should reset their own flags.
    AC_MSG_RESULT($ENABLE_64BIT_LIBS)
])

dnl Strict compilation options.
AC_DEFUN(AC_CHECK_STRICT_COMPILE,
[
    AC_MSG_CHECKING([whether strict compiling is on])
    AC_ARG_ENABLE(strict,[  --enable-strict         compile with strict compiler options [default=no]], [
        if test "$enableval" = no
        then
            ac_use_strict_options=no
        else
            ac_use_strict_options=yes
        fi
      ], [ac_use_strict_options=no])
    AC_MSG_RESULT($ac_use_strict_options)

    if test "$ac_use_strict_options" = yes
    then
        if test "$GCC" = yes
        then
            CFLAGS="$CFLAGS -Wall -ansi -pedantic -Wshadow -Wpointer-arith -Wmissing-prototypes -Wwrite-strings -Winline -Wno-long-long"
            case "${host}" in
            *-*-solaris*)
                CFLAGS="$CFLAGS -D__STDC__=0";;
            esac
        else
            case "${host}" in
            *-*-cygwin)
                if test "`$CC /help 2>&1 | head -c9`" = "Microsoft"
                then
                    CFLAGS="$CFLAGS /W4"
                fi
            esac
        fi
        if test "$GXX" = yes
        then
            CXXFLAGS="$CXXFLAGS -W -Wall -ansi -pedantic -Wpointer-arith -Wmissing-prototypes -Wwrite-strings -Winline -Wno-long-long"
            case "${host}" in
            *-*-solaris*)
                CXXFLAGS="$CXXFLAGS -D__STDC__=0";;
            esac
        else
            case "${host}" in
            *-*-cygwin)
                if test "`$CXX /help 2>&1 | head -c9`" = "Microsoft"
                then
                    CXXFLAGS="$CXXFLAGS /W4"
                fi
            esac
        fi
    fi
])

dnl Define a sizeof checking macro that is a bit better than autoconf's
dnl builtin (and heavily based on it, of course). The new macro is
dnl AC_DO_CHECK_SIZEOF(TYPE [, CROSS_SIZE [, INCLUDES])
AC_DEFUN(AC_DO_CHECK_SIZEOF,
[changequote(<<, >>)dnl
dnl The name to #define.
define(<<AC_TYPE_NAME>>, translit(sizeof_$1, [a-z *], [A-Z_P]))dnl
dnl The cache variable name.
define(<<AC_CV_NAME>>, translit(ac_cv_sizeof_$1, [ *], [_p]))dnl
changequote([, ])dnl
AC_MSG_CHECKING(size of $1)
AC_CACHE_VAL(AC_CV_NAME,
[AC_TRY_RUN($3
[#include <stdio.h>
main()
{
  FILE *f=fopen("conftestval", "w");
  if (!f) exit(1);
  fprintf(f, "%d\n", sizeof($1));
  exit(0);
}], AC_CV_NAME=`cat conftestval`, AC_CV_NAME=0, ifelse([$2], , , AC_CV_NAME=$2))])dnl
AC_MSG_RESULT($AC_CV_NAME)
AC_DEFINE_UNQUOTED(AC_TYPE_NAME, $AC_CV_NAME)
undefine([AC_TYPE_NAME])dnl
undefine([AC_CV_NAME])dnl
])

