dnl aclocal.m4 for ICU
dnl Copyright (c) 1999-2000, International Business Machines Corporation and
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
	if test "$ac_cv_prog_gcc" = yes; then	
		icu_cv_host_frag=$srcdir/config/mh-solaris-gcc 
        elif test "$host_cpu" = sparcv9; then         
                icu_cv_host_frag=$srcdir/config/mh-solaris-sparcv9  
	else
		icu_cv_host_frag=$srcdir/config/mh-solaris 
	fi ;;
*-*-mips*)	icu_cv_host_frag=$srcdir/config/mh-irix ;;
*-*-linux*) 	icu_cv_host_frag=$srcdir/config/mh-linux ;;
*-*-freebsd*) 	icu_cv_host_frag=$srcdir/config/mh-freebsd ;;
*-*-aix*) 	icu_cv_host_frag=$srcdir/config/mh-aix ;;
*-sequent-*) 	icu_cv_host_frag=$srcdir/config/mh-ptx ;;
*-*-hpux*)
	case "$CXX" in 
	*aCC)    icu_cv_host_frag=$srcdir/config/mh-hpux-acc ;;
	*CC)     icu_cv_host_frag=$srcdir/config/mh-hpux-cc ;;
	esac;;
*-*-os390*)	icu_cv_host_frag=$srcdir/config/mh-os390 ;;
*-*-os400*)	icu_cv_host_frag=$srcdir/config/mh-os400 ;;
*-apple-rhapsody*)	icu_cv_host_frag=$srcdir/config/mh-rhapsody ;;
*) 		icu_cv_host_frag=$srcdir/config/mh-unknown ;;
esac
		]
	)
])

dnl ICU_CONDITIONAL - Taken from Automake 1.4
AC_DEFUN(ICU_CONDITIONAL,
[AC_SUBST($1_TRUE)
AC_SUBST($1_FALSE)
if $2; then
  $1_TRUE=
  $1_FALSE='#'
else
  $1_TRUE='#'
  $1_FALSE=
fi])

dnl FreeBSD -pthread check - Jonathan McDowell <noodles@earth.li>
AC_DEFUN(AC_PTHREAD_FREEBSD,
[AC_MSG_CHECKING([if we need -pthread for threads])
AC_CACHE_VAL(ac_ldflag_pthread,
[ac_save_LDFLAGS="$LDFLAGS"
LDFLAGS="-pthread $LDFLAGS"
AC_TRY_LINK(
[
char pthread_create();
],
pthread_create();,
eval "ac_ldflag_pthread=yes",
eval "ac_ldflag_pthread=no"),
LDFLAGS="$ac_save_LDFLAGS"
])
if eval "test \"`echo $ac_ldflag_pthread`\" = yes"; then
	AC_MSG_RESULT(yes)
        ICU_USE_THREADS=1
else
	AC_MSG_RESULT(no)
fi])

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
	    CFLAGS="$CFLAGS -Wall -ansi -pedantic -Wshadow -Wpointer-arith -Wmissing-prototypes -Wwrite-strings"
        fi
        if test "$GXX" = yes
        then
	    CXXFLAGS="$CXXFLAGS -Wall -ansi -pedantic -W -Wpointer-arith -Wmissing-prototypes -Wwrite-strings"
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
