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
	else
		icu_cv_host_frag=$srcdir/config/mh-solaris 
	fi ;;
*-*-mips*)	icu_cv_host_frag=$srcdir/config/mh-irix ;;
*-*-linux*) 	icu_cv_host_frag=$srcdir/config/mh-linux ;;
*-*-freebsd*) 	icu_cv_host_frag=$srcdir/config/mh-freebsd ;;
*-*-aix*) 	icu_cv_host_frag=$srcdir/config/mh-aix ;;
*-*-hpux*)
	case "$CXX" in 
	*aCC)    icu_cv_host_frag=$srcdir/config/mh-hpux-acc ;;
	*CC)     icu_cv_host_frag=$srcdir/config/mh-hpux-cc ;;
	esac;;
*-*-os390*)	icu_cv_host_frag=$srcdir/config/mh-os390 ;;
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
LIBS="$ac_save_LDFLAGS"
])
if eval "test \"`echo $ac_ldflag_pthread`\" = yes"; then
	AC_MSG_RESULT(yes)
        ICU_USE_THREADS=1
else
	AC_MSG_RESULT(no)
fi])
