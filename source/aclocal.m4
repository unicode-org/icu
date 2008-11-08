# aclocal.m4 for ICU
# Copyright (c) 1999-2008, International Business Machines Corporation and
# others. All Rights Reserved.
# Stephen F. Booth

# @TOP@

# ICU_CHECK_MH_FRAG
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
powerpc*-*-linux*)
	if test "$GCC" = yes; then
		icu_cv_host_frag=mh-linux
	else
		icu_cv_host_frag=mh-linux-va
	fi ;;
*-*-linux*|*-pc-gnu) icu_cv_host_frag=mh-linux ;;
*-*-cygwin|*-*-mingw32)
	if test "$GCC" = yes; then
		AC_TRY_COMPILE([
#ifndef __MINGW32__
#error This is not MinGW
#endif], [], icu_cv_host_frag=mh-mingw, icu_cv_host_frag=mh-cygwin)
	else
		icu_cv_host_frag=mh-cygwin-msvc
	fi ;;
*-*-*bsd*|*-*-dragonfly*) 	icu_cv_host_frag=mh-bsd-gcc ;;
*-*-aix*)
	if test "$GCC" = yes; then
		icu_cv_host_frag=mh-aix-gcc
	else
		icu_cv_host_frag=mh-aix-va
	fi ;;
*-*-hpux*)
	if test "$GCC" = yes; then
		icu_cv_host_frag=mh-hpux-gcc
	else
		case "$CXX" in
		*aCC)    icu_cv_host_frag=mh-hpux-acc ;;
		esac
	fi ;;
*-*ibm-openedition*|*-*-os390*)	icu_cv_host_frag=mh-os390 ;;
*-*-os400*)	icu_cv_host_frag=mh-os400 ;;
*-apple-rhapsody*)	icu_cv_host_frag=mh-darwin ;;
*-apple-darwin*)	icu_cv_host_frag=mh-darwin ;;
*-*-beos|*-*-haiku)	icu_cv_host_frag=mh-beos ;;
*-*-irix*)	icu_cv_host_frag=mh-irix ;;
*-dec-osf*) icu_cv_host_frag=mh-alpha-osf ;;
*-*-nto*)	icu_cv_host_frag=mh-qnx ;;
*-ncr-*)	icu_cv_host_frag=mh-mpras ;;
*) 		icu_cv_host_frag=mh-unknown ;;
esac
		]
	)
])

# ICU_CONDITIONAL - similar example taken from Automake 1.4
AC_DEFUN(ICU_CONDITIONAL,
[AC_SUBST($1_TRUE)
if $2; then
  $1_TRUE=
else
  $1_TRUE='#'
fi])

# ICU_PROG_LINK - Make sure that the linker is usable
AC_DEFUN(ICU_PROG_LINK,
[
case "${host}" in
    *-*-cygwin*|*-*-mingw*)
        if test "$GCC" != yes && test -n "`link --version 2>&1 | grep 'GNU coreutils'`"; then
            AC_MSG_ERROR([link.exe is not a valid linker. Your PATH is incorrect.
                  Please follow the directions in ICU's readme.])
        fi;;
    *);;
esac])

# AC_SEARCH_LIBS_FIRST(FUNCTION, SEARCH-LIBS [, ACTION-IF-FOUND
#            [, ACTION-IF-NOT-FOUND [, OTHER-LIBRARIES]]])
# Search for a library defining FUNC, then see if it's not already available.

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



# Check if we can build and use 64-bit libraries
AC_DEFUN(AC_CHECK_64BIT_LIBS,
[
    BITS_REQ=nochange
    ENABLE_64BIT_LIBS=unknown
    ## revisit this for cross-compile.
    
    AC_ARG_ENABLE(64bit-libs,
        [  --enable-64bit-libs     (deprecated, use --with-library-bits) build 64-bit libraries [default= platform default]],
        [echo "note, use --with-library-bits instead of --*-64bit-libs"
         case "${withval}" in
            no|false|32) BITS_REQ=32 ;;
            yes|true|64) BITS_REQ=64 ;;
            nochange) BITS_REQ=nochange ;;
            *) AC_MSG_ERROR(bad value ${withval} for --with-library-bits) ;;
            esac]    )
    

    AC_ARG_WITH(library-bits,
        [  --with-library-bits=bits specify how many bits to use for the library (32, 64, 64else32, nochange) [default=nochange]],
        [case "${withval}" in
            nochange) BITS_REQ=$withval ;;
            32|64|64else32) BITS_REQ=$withval ;;
            *) AC_MSG_ERROR(bad value ${withval} for --with-library-bits) ;;
            esac])
    DEFAULT_64BIT=no
    AC_MSG_CHECKING([whether 64 bit binaries are built by default])
    AC_RUN_IFELSE(int main(void) {return (sizeof(void*)*8==64)?0:1;},
       DEFAULT_64BIT=yes, DEFAULT_64BIT=no, DEFAULT_64BIT=unknown)
    BITS_GOT=unknown
    BITS_OK=yes
    if test "$DEFAULT_64BIT" = "yes"; then
        BITS_GOT=64
        case "$BITS_REQ" in
            32) BITS_OK=no;;
            nochange) ;;
            *) ;;
        esac
    elif test "$DEFAULT_64BIT" = "no"; then
        BITS_GOT=32
        case "$BITS_REQ" in
            64|64else32) BITS_OK=no;;
            nochange) ;;
            *) ;;
        esac
    elif test "$DEFAULT_64BIT" = "unknown"; then
        BITS_GOT=unknown
        case "$BITS_REQ" in
            64|64else32) BITS_OK=no;;
            32) BITS_OK=no;;
            nochange) ;;
            *) ;;
        esac
    fi
            
    AC_MSG_RESULT($DEFAULT_64BIT);
    #AC_MSG_RESULT($DEFAULT_64BIT - got $BITS_GOT wanted $BITS_REQ okness $BITS_OK);
    if test "$BITS_OK" != "yes"; then
        # These results can't be cached because is sets compiler flags.
        if test "$BITS_REQ" = "64" -o "$BITS_REQ" = "64else32"; then
            AC_MSG_CHECKING([how to build 64-bit executables])
            if test "$GCC" = yes; then
                #DONOTUSE# This test is wrong.  If it's GCC, just test m64
                #DONOTUSE#if test -n "`$CXX -dumpspecs 2>&1 && $CC -dumpspecs 2>&1 | grep -v __LP64__`"; then
                OLD_CFLAGS="${CFLAGS}"
                OLD_CXXFLAGS="${CXXFLAGS}"
                CFLAGS="${CFLAGS} -m64"
                CXXFLAGS="${CXXFLAGS} -m64"
                AC_COMPILE_IFELSE(int main(void) {return (sizeof(void*)*8==64)?0:1;},
                   ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no)
                if test "$ENABLE_64BIT_LIBS" != yes; then
                    # Nope. back out changes.
                    CFLAGS="${OLD_CFLAGS}"
                    CXXFLAGS="${OLD_CXXFLAGS}"
                fi
            else
                case "${host}" in
                sparc*-*-solaris*)
                    # 0. save old flags
                    OLD_CFLAGS="${CFLAGS}"
                    OLD_CXXFLAGS="${CXXFLAGS}"
                    # 1. try -m64
                    CFLAGS="${CFLAGS} -m64"
                    CXXFLAGS="${CXXFLAGS} -m64"
                    AC_COMPILE_IFELSE(int main(void) {return (sizeof(void*)*8==64)?0:1;},
                       ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no)
                    if test "$ENABLE_64BIT_LIBS" != yes; then
                        # Nope. back out changes.
                        CFLAGS="${OLD_CFLAGS}"
                        CXXFLAGS="${OLD_CXXFLAGS}"
                        # 2. try xarch=v9 [deprecated]
                        ## TODO: cross compile: the following won't work.
                        SPARCV9=`isainfo -n 2>&1 | grep sparcv9`
                        SOL64=`$CXX -xarch=v9 2>&1 && $CC -xarch=v9 2>&1 | grep -v usage:`
                        # "Warning: -xarch=v9 is deprecated, use -m64 to create 64-bit programs"
                        if test -z "$SOL64" && test -n "$SPARCV9"; then
                            CFLAGS="${CFLAGS} -xtarget=ultra -xarch=v9"
                            CXXFLAGS="${CXXFLAGS} -xtarget=ultra -xarch=v9"
                            LDFLAGS="${LDFLAGS} -xtarget=ultra -xarch=v9"
                            ENABLE_64BIT_LIBS=yes
                        else
                            ENABLE_64BIT_LIBS=no
                        fi
                    fi
                    ;;
                i386-*-solaris*)
                    # 0. save old flags
                    OLD_CFLAGS="${CFLAGS}"
                    OLD_CXXFLAGS="${CXXFLAGS}"
                    # 1. try -m64
                    CFLAGS="${CFLAGS} -m64"
                    CXXFLAGS="${CXXFLAGS} -m64"
                    AC_COMPILE_IFELSE(int main(void) {return (sizeof(void*)*8==64)?0:1;},
                       ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no)
                    if test "$ENABLE_64BIT_LIBS" != yes; then
                        # Nope. back out changes.
                        CFLAGS="${OLD_CFLAGS}"
                        CXXFLAGS="${OLD_CXXFLAGS}"
                        # 2. try the older compiler option
                        ## TODO: cross compile problem
                        SOL64=`$CXX -xtarget=generic64 2>&1 && $CC -xtarget=generic64 2>&1 | grep -v usage:`
                        if test -z "$SOL64" && test -n "$AMD64"; then
                            CFLAGS="${CFLAGS} -xtarget=generic64"
                            CXXFLAGS="${CXXFLAGS} -xtarget=generic64"
                            ENABLE_64BIT_LIBS=yes
                        else
                            ENABLE_64BIT_LIBS=no
                        fi
                    fi
                    ;;
                ia64-*-linux*)
                    # check for ecc/ecpc compiler support
                    ## TODO: cross compiler problem
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
                    ;;
                *-*-cygwin)
                    # vcvarsamd64.bat should have been used to enable 64-bit builds.
                    # We only do this check to display the correct answer.
                    ## TODO: cross compiler problem
                    if test -n "`$CXX -help 2>&1 | grep 'for x64'`"; then
                        ENABLE_64BIT_LIBS=yes
                    else
                        # unknown
                        ENABLE_64BIT_LIBS=no
                    fi
                    ;;
                *-*-aix*|powerpc64-*-linux*)
                    OLD_CFLAGS="${CFLAGS}"
                    OLD_CXXFLAGS="${CXXFLAGS}"
                    OLD_LDFLAGS="${LDFLAGS}"
                    CFLAGS="${CFLAGS} -q64"
                    CXXFLAGS="${CXXFLAGS} -q64"
                    LDFLAGS="${LDFLAGS} -q64"
                    AC_COMPILE_IFELSE(int main(void) {return (sizeof(void*)*8==64)?0:1;},
                       ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no)
                    if test "$ENABLE_64BIT_LIBS" != yes; then
                        CFLAGS="${OLD_CFLAGS}"
                        CXXFLAGS="${OLD_CXXFLAGS}"
                        LDFLAGS="${OLD_LDFLAGS}"
                    else
                        case "${host}" in
                        *-*-aix*)
                            ARFLAGS="${ARFLAGS} -X64"
                        esac
                    fi
                    ;;
                *-*-hpux*)
                    # First we try the newer +DD64, if that doesn't work,
                    # try other options.

                    OLD_CFLAGS="${CFLAGS}"
                    OLD_CXXFLAGS="${CXXFLAGS}"
                    CFLAGS="${CFLAGS} +DD64"
                    CXXFLAGS="${CXXFLAGS} +DD64"
                    AC_COMPILE_IFELSE(int main(void) {return (sizeof(void*)*8==64)?0:1;},
                        ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no)
                    if test "$ENABLE_64BIT_LIBS" != yes; then
                        CFLAGS="${OLD_CFLAGS}"
                        CXXFLAGS="${OLD_CXXFLAGS}"
                        CFLAGS="${CFLAGS} +DA2.0W"
                        CXXFLAGS="${CXXFLAGS} +DA2.0W"
                        AC_COMPILE_IFELSE(int main(void) {return (sizeof(void*)*8==64)?0:1;},
                            ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no)
                        if test "$ENABLE_64BIT_LIBS" != yes; then
                            CFLAGS="${OLD_CFLAGS}"
                            CXXFLAGS="${OLD_CXXFLAGS}"
                        fi
                    fi
                    ;;
                *-*ibm-openedition*|*-*-os390*)
                    OLD_CFLAGS="${CFLAGS}"
                    OLD_CXXFLAGS="${CXXFLAGS}"
                    OLD_LDFLAGS="${LDFLAGS}"
                    CFLAGS="${CFLAGS} -Wc,lp64"
                    CXXFLAGS="${CXXFLAGS} -Wc,lp64"
                    LDFLAGS="${LDFLAGS} -Wl,lp64"
                    AC_COMPILE_IFELSE(int main(void) {return (sizeof(void*)*8==64)?0:1;},
                       ENABLE_64BIT_LIBS=yes, ENABLE_64BIT_LIBS=no)
                    if test "$ENABLE_64BIT_LIBS" != yes; then
                        CFLAGS="${OLD_CFLAGS}"
                        CXXFLAGS="${OLD_CXXFLAGS}"
                        LDFLAGS="${OLD_LDFLAGS}"
                    fi
                    ;;
                *)
                    ENABLE_64BIT_LIBS=no
                    ;;
                esac
            fi
            AC_MSG_RESULT($ENABLE_64BIT_LIBS)
        elif test "$BITS_REQ" = "32"; then
            AC_MSG_CHECKING([how to build 32-bit executables])
            if test "$GCC" = yes; then
                OLD_CFLAGS="${CFLAGS}"
                OLD_CXXFLAGS="${CXXFLAGS}"
                CFLAGS="${CFLAGS} -m32"
                CXXFLAGS="${CXXFLAGS} -m32"
                AC_COMPILE_IFELSE(int main(void) {return (sizeof(void*)*8==32)?0:1;},
                   ENABLE_64BIT_LIBS=no, ENABLE_64BIT_LIBS=yes)
                if test "$ENABLE_64BIT_LIBS" != no; then
                    CFLAGS="${OLD_CFLAGS}"
                    CXXFLAGS="${OLD_CXXFLAGS}"
                fi
            else
                echo " Note: not sure how to build 32 bit executables on this platform."
            fi
            # 'How to build 32 bit...' will be opposite of 64 bit
            if test "$ENABLE_64BIT_LIBS" = yes; then
                AC_MSG_RESULT(no)
            else
                if test "$ENABLE_64BIT_LIBS" = no; then
                    AC_MSG_RESULT(yes)
                else
                    AC_MSG_RESULT(unknown)
                fi
            fi
        fi
        # Individual tests that fail should reset their own flags.
        NOW_64BIT=no
        NOW_32BIT=no
        AC_MSG_CHECKING([whether runnable 64-bit binaries are being built ])
        AC_TRY_RUN(int main(void) {return (sizeof(void*)*8==64)?0:1;},
           NOW_64BIT=yes, NOW_64BIT=no, NOW_64BIT=unknown)
        AC_MSG_RESULT($NOW_64BIT);
        AC_MSG_CHECKING([whether runnable 32-bit binaries are being built ])
        AC_TRY_RUN(int main(void) {return (sizeof(void*)*8==32)?0:1;},
           NOW_32BIT=yes, NOW_32BIT=no, NOW_32BIT=unknown)
        AC_MSG_RESULT($NOW_32BIT);
        
        if test "$BITS_REQ" = "32" -a "$NOW_64BIT" = "yes"; then
            AC_MSG_ERROR([Requested $BITS_REQ but got 64 bit binaries])
        elif test "$BITS_REQ" = "64" -a "$NOW_32BIT" = "yes"; then
            AC_MSG_ERROR([Requested $BITS_REQ but got 32 bit binaries])
        elif test "$NOW_32BIT" != "yes" -a "$NOW_64BIT" != "yes"; then 
            echo "*** Note: Cannot determine bitness - if configure fails later, try --with-library-bits=nochange"
        fi
    fi
])

# Strict compilation options.
AC_DEFUN(AC_CHECK_STRICT_COMPILE,
[
    AC_MSG_CHECKING([whether strict compiling is on])
    AC_ARG_ENABLE(strict,[  --enable-strict         compile with strict compiler options [default=yes]], [
        if test "$enableval" = no
        then
            ac_use_strict_options=no
        else
            ac_use_strict_options=yes
        fi
      ], [ac_use_strict_options=yes])
    AC_MSG_RESULT($ac_use_strict_options)

    if test "$ac_use_strict_options" = yes
    then
        if test "$GCC" = yes
        then
            CFLAGS="$CFLAGS -Wall -ansi -pedantic -Wshadow -Wpointer-arith -Wmissing-prototypes -Wwrite-strings -Wno-long-long"
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
            CXXFLAGS="$CXXFLAGS -W -Wall -ansi -pedantic -Wpointer-arith -Wwrite-strings -Wno-long-long"
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


