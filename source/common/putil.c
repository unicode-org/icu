/*
******************************************************************************
*
*   Copyright (C) 1997-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*  FILE NAME : putil.c (previously putil.cpp and ptypes.cpp)
*
*   Date        Name        Description
*   04/14/97    aliu        Creation.
*   04/24/97    aliu        Added getDefaultDataDirectory() and
*                            getDefaultLocaleID().
*   04/28/97    aliu        Rewritten to assume Unix and apply general methods
*                            for assumed case.  Non-UNIX platforms must be
*                            special-cased.  Rewrote numeric methods dealing
*                            with NaN and Infinity to be platform independent
*                             over all IEEE 754 platforms.
*   05/13/97    aliu        Restored sign of timezone
*                            (semantics are hours West of GMT)
*   06/16/98    erm         Added IEEE_754 stuff, cleaned up isInfinite, isNan,
*                             nextDouble..
*   07/22/98    stephen     Added remainder, max, min, trunc
*   08/13/98    stephen     Added isNegativeInfinity, isPositiveInfinity
*   08/24/98    stephen     Added longBitsFromDouble
*   09/08/98    stephen     Minor changes for Mac Port
*   03/02/99    stephen     Removed openFile().  Added AS400 support.
*                            Fixed EBCDIC tables
*   04/15/99    stephen     Converted to C.
*   06/28/99    stephen     Removed mutex locking in u_isBigEndian().
*   08/04/99    jeffrey R.  Added OS/2 changes
*   11/15/99    helena      Integrated S/390 IEEE support.
*   04/26/01    Barry N.    OS/400 support for uprv_getDefaultLocaleID
*   08/15/01    Steven H.   OS/400 support for uprv_getDefaultCodepage
******************************************************************************
*/

#ifdef _AIX
#    include<sys/types.h>
#endif

#ifndef PTX

/* Define _XOPEN_SOURCE for Solaris and friends. */
/* NetBSD needs it to be >= 4 */
#ifndef _XOPEN_SOURCE
#define _XOPEN_SOURCE 4
#endif

/* Define __USE_POSIX and __USE_XOPEN for Linux and glibc. */
#ifndef __USE_POSIX
#define __USE_POSIX
#endif
#ifndef __USE_XOPEN
#define __USE_XOPEN
#endif

#endif /* PTX */

/* include ICU headers */
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "unicode/ustring.h"
#include "uassert.h"
#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "locmap.h"
#include "ucln_cmn.h"
#include "udataswp.h"

/* Include standard headers. */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <locale.h>
#include <time.h>
#include <float.h>

/* include system headers */
#ifdef WIN32
#   define WIN32_LEAN_AND_MEAN
#   define NOGDI
#   define NOUSER
#   define NOSERVICE
#   define NOIME
#   define NOMCX
#   include <windows.h>
#elif defined(OS2)
#   define INCL_DOSMISC
#   define INCL_DOSERRORS
#   define INCL_DOSMODULEMGR
#   include <os2.h>
#elif defined(OS400)
#   include <float.h>
#   include <qusec.h>       /* error code structure */
#   include <qusrjobi.h>
#   include <qliept.h>      /* EPT_CALL macro  - this include must be after all other "QSYSINCs" */
#elif defined(XP_MAC)
#   include <Files.h>
#   include <IntlResources.h>
#   include <Script.h>
#   include <Folders.h>
#   include <MacTypes.h>
#   include <TextUtils.h>
#elif defined(OS390)
#include "unicode/ucnv.h"   /* Needed for UCNV_SWAP_LFNL_OPTION_STRING */
#elif defined(U_AIX)
/*
#   include <sys/ldr.h>
*/
#elif defined(U_SOLARIS) || defined(U_LINUX)
/*
#   include <dlfcn.h>
#   include <link.h>
*/
#elif defined(U_HPUX)
/*
#   include <dl.h>
*/
#elif defined(U_DARWIN)
#include <sys/file.h>
#include <sys/param.h>
#elif defined(U_QNX)
#include <sys/neutrino.h>
#endif

/* Define the extension for data files, again... */
#define DATA_TYPE "dat"

/* Leave this copyright notice here! */
static const char copyright[] = U_COPYRIGHT_STRING;

/* floating point implementations ------------------------------------------- */

/* We return QNAN rather than SNAN*/
#define SIGN 0x80000000U
#if defined(__GNUC__)
/*
    This is an optimization for when u_topNBytesOfDouble
    and u_bottomNBytesOfDouble can't be properly optimized by the compiler.
*/
#define USE_64BIT_DOUBLE_OPTIMIZATION 1
#else
#define USE_64BIT_DOUBLE_OPTIMIZATION 0
#endif

#if USE_64BIT_DOUBLE_OPTIMIZATION
/* gcc 3.2 has an optimization bug */
static const int64_t gNan64 = 0x7FF8000000000000LL;
static const int64_t gInf64 = 0x7FF0000000000000LL;
static const double * const fgNan = (const double *)(&gNan64);
static const double * const fgInf = (const double *)(&gInf64);
#else

#if IEEE_754
#define NAN_TOP ((int16_t)0x7FF8)
#define INF_TOP ((int16_t)0x7FF0)
#elif defined(OS390)
#define NAN_TOP ((int16_t)0x7F08)
#define INF_TOP ((int16_t)0x3F00)
#endif

/* statics */
static UBool fgNaNInitialized = FALSE;
static UBool fgInfInitialized = FALSE;
static double gNan;
static double gInf;
static double * const fgNan = &gNan;
static double * const fgInf = &gInf;
#endif

/*---------------------------------------------------------------------------
  Platform utilities
  Our general strategy is to assume we're on a POSIX platform.  Platforms which
  are non-POSIX must declare themselves so.  The default POSIX implementation
  will sometimes work for non-POSIX platforms as well (e.g., the NaN-related
  functions).
  ---------------------------------------------------------------------------*/

#if defined(_WIN32) || defined(XP_MAC) || defined(OS400) || defined(OS2)
#   undef U_POSIX_LOCALE
#else
#   define U_POSIX_LOCALE    1
#endif

/*
 * Only include langinfo.h if we have a way to get the codeset. If we later
 * depend on more feature, we can test on U_HAVE_NL_LANGINFO.
 *
 */

#if U_HAVE_NL_LANGINFO_CODESET
#include <langinfo.h>
#endif

/* Utilities to get the bits from a double */
static char*
u_topNBytesOfDouble(double* d, int n)
{
#if U_IS_BIG_ENDIAN
    return (char*)d;
#else
    return (char*)(d + 1) - n;
#endif
}

static char*
u_bottomNBytesOfDouble(double* d, int n)
{
#if U_IS_BIG_ENDIAN
    return (char*)(d + 1) - n;
#else
    return (char*)d;
#endif
}

/*---------------------------------------------------------------------------
  Universal Implementations
  These are designed to work on all platforms.  Try these, and if they don't
  work on your platform, then special case your platform with new
  implementations.
  ---------------------------------------------------------------------------*/

/* Get UTC (GMT) time measured in seconds since 0:00 on 1/1/70.*/
U_CAPI int32_t U_EXPORT2
uprv_getUTCtime()
{
#ifdef XP_MAC
    time_t t, t1, t2;
    struct tm tmrec;

    uprv_memset( &tmrec, 0, sizeof(tmrec) );
    tmrec.tm_year = 70;
    tmrec.tm_mon = 0;
    tmrec.tm_mday = 1;
    t1 = mktime(&tmrec);    /* seconds of 1/1/1970*/

    time(&t);
    uprv_memcpy( &tmrec, gmtime(&t), sizeof(tmrec) );
    t2 = mktime(&tmrec);    /* seconds of current GMT*/
    return t2 - t1;         /* GMT (or UTC) in seconds since 1970*/
#else
    time_t epochtime;
    time(&epochtime);
    return epochtime;
#endif
}

/*-----------------------------------------------------------------------------
  IEEE 754
  These methods detect and return NaN and infinity values for doubles
  conforming to IEEE 754.  Platforms which support this standard include X86,
  Mac 680x0, Mac PowerPC, AIX RS/6000, and most others.
  If this doesn't work on your platform, you have non-IEEE floating-point, and
  will need to code your own versions.  A naive implementation is to return 0.0
  for getNaN and getInfinity, and false for isNaN and isInfinite.
  ---------------------------------------------------------------------------*/

U_CAPI UBool U_EXPORT2
uprv_isNaN(double number)
{
#if IEEE_754
#if USE_64BIT_DOUBLE_OPTIMIZATION
    /* gcc 3.2 has an optimization bug */
    /* Infinity is 0x7FF0000000000000U. Anything greater than that is a NaN */
    return (UBool)(((*((int64_t *)&number)) & U_INT64_MAX) > gInf64);

#else
    /* This should work in theory, but it doesn't, so we resort to the more*/
    /* complicated method below.*/
    /*  return number != number;*/

    /* You can't return number == getNaN() because, by definition, NaN != x for*/
    /* all x, including NaN (that is, NaN != NaN).  So instead, we compare*/
    /* against the known bit pattern.  We must be careful of endianism here.*/
    /* The pattern we are looking for id:*/

    /*   7FFy yyyy yyyy yyyy  (some y non-zero)*/

    /* There are two different kinds of NaN, but we ignore the distinction*/
    /* here.  Note that the y value must be non-zero; if it is zero, then we*/
    /* have infinity.*/

    uint32_t highBits = *(uint32_t*)u_topNBytesOfDouble(&number,
                              sizeof(uint32_t));
    uint32_t lowBits  = *(uint32_t*)u_bottomNBytesOfDouble(&number,
                             sizeof(uint32_t));

    return (UBool)(((highBits & 0x7FF00000L) == 0x7FF00000L) &&
      (((highBits & 0x000FFFFFL) != 0) || (lowBits != 0)));
#endif

#elif defined(OS390)
    uint32_t highBits = *(uint32_t*)u_topNBytesOfDouble(&number,
                        sizeof(uint32_t));
    uint32_t lowBits  = *(uint32_t*)u_bottomNBytesOfDouble(&number,
                        sizeof(uint32_t));

    return ((highBits & 0x7F080000L) == 0x7F080000L) &&
      (lowBits == 0x00000000L);

#else
    /* If your platform doesn't support IEEE 754 but *does* have an NaN value,*/
    /* you'll need to replace this default implementation with what's correct*/
    /* for your platform.*/
    return number != number;
#endif
}

U_CAPI UBool U_EXPORT2
uprv_isInfinite(double number)
{
#if IEEE_754
#if USE_64BIT_DOUBLE_OPTIMIZATION
    /* gcc 3.2 has an optimization bug */
    return (UBool)(((*((int64_t *)&number)) & U_INT64_MAX) == gInf64);
#else

    /* We know the top bit is the sign bit, so we mask that off in a copy of */
    /* the number and compare against infinity. [LIU]*/
    /* The following approach doesn't work for some reason, so we go ahead and */
    /* scrutinize the pattern itself. */
    /*  double a = number; */
    /*  *(int8_t*)u_topNBytesOfDouble(&a, 1) &= 0x7F;*/
    /*  return a == uprv_getInfinity();*/
    /* Instead, We want to see either:*/

    /*   7FF0 0000 0000 0000*/
    /*   FFF0 0000 0000 0000*/

    uint32_t highBits = *(uint32_t*)u_topNBytesOfDouble(&number,
                        sizeof(uint32_t));
    uint32_t lowBits  = *(uint32_t*)u_bottomNBytesOfDouble(&number,
                        sizeof(uint32_t));

    return (UBool)(((highBits  & ~SIGN) == 0x7FF00000U) &&
      (lowBits == 0x00000000U));
#endif

#elif defined(OS390)
    uint32_t highBits = *(uint32_t*)u_topNBytesOfDouble(&number,
                        sizeof(uint32_t));
    uint32_t lowBits  = *(uint32_t*)u_bottomNBytesOfDouble(&number,
                        sizeof(uint32_t));

    return ((highBits  & ~SIGN) == 0x70FF0000L) && (lowBits == 0x00000000L);

#else
    /* If your platform doesn't support IEEE 754 but *does* have an infinity*/
    /* value, you'll need to replace this default implementation with what's*/
    /* correct for your platform.*/
    return number == (2.0 * number);
#endif
}

U_CAPI UBool U_EXPORT2
uprv_isPositiveInfinity(double number)
{
#if IEEE_754 || defined(OS390)
    return (UBool)(number > 0 && uprv_isInfinite(number));
#else
    return uprv_isInfinite(number);
#endif
}

U_CAPI UBool U_EXPORT2
uprv_isNegativeInfinity(double number)
{
#if IEEE_754 || defined(OS390)
    return (UBool)(number < 0 && uprv_isInfinite(number));

#else
    uint32_t highBits = *(uint32_t*)u_topNBytesOfDouble(&number,
                        sizeof(uint32_t));
    return((highBits & SIGN) && uprv_isInfinite(number));

#endif
}

U_CAPI double U_EXPORT2
uprv_getNaN()
{
#if IEEE_754 || defined(OS390)
#if !USE_64BIT_DOUBLE_OPTIMIZATION
    if (!fgNaNInitialized) {
        /* This variable is always initialized with the same value,
        so a mutex isn't needed. */
        int i;
        int8_t* p = (int8_t*)fgNan;
        for(i = 0; i < sizeof(double); ++i)
            *p++ = 0;
        *(int16_t*)u_topNBytesOfDouble(fgNan, sizeof(NAN_TOP)) = NAN_TOP;
        fgNaNInitialized = TRUE;
    }
#endif
    return *fgNan;
#else
    /* If your platform doesn't support IEEE 754 but *does* have an NaN value,*/
    /* you'll need to replace this default implementation with what's correct*/
    /* for your platform.*/
    return 0.0;
#endif
}

U_CAPI double U_EXPORT2
uprv_getInfinity()
{
#if IEEE_754 || defined(OS390)
#if !USE_64BIT_DOUBLE_OPTIMIZATION
    if (!fgInfInitialized)
    {
        /* This variable is always initialized with the same value,
        so a mutex isn't needed. */
        int i;
        int8_t* p = (int8_t*)fgInf;
        for(i = 0; i < sizeof(double); ++i)
            *p++ = 0;
        *(int16_t*)u_topNBytesOfDouble(fgInf, sizeof(INF_TOP)) = INF_TOP;
        fgInfInitialized = TRUE;
    }
#endif
    return *fgInf;
#else
    /* If your platform doesn't support IEEE 754 but *does* have an infinity*/
    /* value, you'll need to replace this default implementation with what's*/
    /* correct for your platform.*/
    return 0.0;
#endif
}

U_CAPI double U_EXPORT2
uprv_floor(double x)
{
    return floor(x);
}

U_CAPI double U_EXPORT2
uprv_ceil(double x)
{
    return ceil(x);
}

U_CAPI double U_EXPORT2
uprv_round(double x)
{
    return uprv_floor(x + 0.5);
}

U_CAPI double U_EXPORT2
uprv_fabs(double x)
{
    return fabs(x);
}

U_CAPI double U_EXPORT2
uprv_modf(double x, double* y)
{
    return modf(x, y);
}

U_CAPI double U_EXPORT2
uprv_fmod(double x, double y)
{
    return fmod(x, y);
}

U_CAPI double U_EXPORT2
uprv_pow(double x, double y)
{
    /* This is declared as "double pow(double x, double y)" */
    return pow(x, y);
}

U_CAPI double U_EXPORT2
uprv_pow10(int32_t x)
{
    return pow(10.0, (double)x);
}

U_CAPI double U_EXPORT2
uprv_fmax(double x, double y)
{
#if IEEE_754
    int32_t lowBits;

    /* first handle NaN*/
    if(uprv_isNaN(x) || uprv_isNaN(y))
        return uprv_getNaN();

    /* check for -0 and 0*/
    lowBits = *(uint32_t*) u_bottomNBytesOfDouble(&x, sizeof(uint32_t));
    if(x == 0.0 && y == 0.0 && (lowBits & SIGN))
        return y;

#endif

    /* this should work for all flt point w/o NaN and Infpecial cases */
    return (x > y ? x : y);
}

U_CAPI int32_t U_EXPORT2
uprv_max(int32_t x, int32_t y)
{
    return (x > y ? x : y);
}

U_CAPI double U_EXPORT2
uprv_fmin(double x, double y)
{
#if IEEE_754
    int32_t lowBits;

    /* first handle NaN*/
    if(uprv_isNaN(x) || uprv_isNaN(y))
        return uprv_getNaN();

    /* check for -0 and 0*/
    lowBits = *(uint32_t*) u_bottomNBytesOfDouble(&y, sizeof(uint32_t));
    if(x == 0.0 && y == 0.0 && (lowBits & SIGN))
        return y;

#endif

    /* this should work for all flt point w/o NaN and Inf special cases */
    return (x > y ? y : x);
}

U_CAPI int32_t U_EXPORT2
uprv_min(int32_t x, int32_t y)
{
    return (x > y ? y : x);
}

/**
 * Truncates the given double.
 * trunc(3.3) = 3.0, trunc (-3.3) = -3.0
 * This is different than calling floor() or ceil():
 * floor(3.3) = 3, floor(-3.3) = -4
 * ceil(3.3) = 4, ceil(-3.3) = -3
 */
U_CAPI double U_EXPORT2
uprv_trunc(double d)
{
#if IEEE_754
    int32_t lowBits;

    /* handle error cases*/
    if(uprv_isNaN(d))
        return uprv_getNaN();
    if(uprv_isInfinite(d))
        return uprv_getInfinity();

    lowBits = *(uint32_t*) u_bottomNBytesOfDouble(&d, sizeof(uint32_t));
    if( (d == 0.0 && (lowBits & SIGN)) || d < 0)
        return ceil(d);
    else
        return floor(d);

#else
    return d >= 0 ? floor(d) : ceil(d);

#endif
}

/**
 * Return the largest positive number that can be represented by an integer
 * type of arbitrary bit length.
 */
U_CAPI double U_EXPORT2
uprv_maxMantissa(void)
{
    return pow(2.0, DBL_MANT_DIG + 1.0) - 1.0;
}

/**
 * Return the floor of the log base 10 of a given double.
 * This method compensates for inaccuracies which arise naturally when
 * computing logs, and always give the correct value.  The parameter
 * must be positive and finite.
 * (Thanks to Alan Liu for supplying this function.)
 */
U_CAPI int16_t U_EXPORT2
uprv_log10(double d)
{
#ifdef OS400
    /* We don't use the normal implementation because you can't underflow */
    /* a double otherwise an underflow exception occurs */
    return log10(d);
#else
    /* The reason this routine is needed is that simply taking the*/
    /* log and dividing by log10 yields a result which may be off*/
    /* by 1 due to rounding errors.  For example, the naive log10*/
    /* of 1.0e300 taken this way is 299, rather than 300.*/
    double alog10 = log(d) / log(10.0);
    int16_t ailog10 = (int16_t) floor(alog10);

    /* Positive logs could be too small, e.g. 0.99 instead of 1.0*/
    if (alog10 > 0 && d >= pow(10.0, (double)(ailog10 + 1)))
        ++ailog10;

    /* Negative logs could be too big, e.g. -0.99 instead of -1.0*/
    else if (alog10 < 0 && d < pow(10.0, (double)(ailog10)))
        --ailog10;

    return ailog10;
#endif
}

U_CAPI double U_EXPORT2
uprv_log(double d)
{
    return log(d);
}

#if 0
/* This isn't used. If it's readded, readd putiltst.c tests */
U_CAPI int32_t U_EXPORT2
uprv_digitsAfterDecimal(double x)
{
    char buffer[20];
    int32_t numDigits, bytesWritten;
    char *p = buffer;
    int32_t ptPos, exponent;

    /* cheat and use the string-format routine to get a string representation*/
    /* (it handles mathematical inaccuracy better than we can), then find out */
    /* many characters are to the right of the decimal point */
    bytesWritten = sprintf(buffer, "%+.9g", x);
    while (isdigit(*(++p))) {
    }

    ptPos = (int32_t)(p - buffer);
    numDigits = (int32_t)(bytesWritten - ptPos - 1);

    /* if the number's string representation is in scientific notation, find */
    /* the exponent and take it into account*/
    exponent = 0;
    p = uprv_strchr(buffer, 'e');
    if (p != 0) {
        int16_t expPos = (int16_t)(p - buffer);
        numDigits -= bytesWritten - expPos;
        exponent = (int32_t)(atol(p + 1));
    }

    /* the string representation may still have spurious decimal digits in it, */
    /* so we cut off at the ninth digit to the right of the decimal, and have */
    /* to search backward from there to the first non-zero digit*/
    if (numDigits > 9) {
        numDigits = 9;
        while (numDigits > 0 && buffer[ptPos + numDigits] == '0')
            --numDigits;
    }
    numDigits -= exponent;
    if (numDigits < 0) {
        return 0;
    }
    return numDigits;
}
#endif

/*---------------------------------------------------------------------------
  Platform-specific Implementations
  Try these, and if they don't work on your platform, then special case your
  platform with new implementations.
  ---------------------------------------------------------------------------*/

/* Win32 time zone detection ------------------------------------------------ */

#ifdef WIN32

/*
  This code attempts to detect the Windows time zone, as set in the
  Windows Date and Time control panel.  It attempts to work on
  multiple flavors of Windows (9x, Me, NT, 2000, XP) and on localized
  installs.  It works by directly interrogating the registry and
  comparing the data there with the data returned by the
  GetTimeZoneInformation API, along with some other strategies.  The
  registry contains time zone data under one of two keys (depending on
  the flavor of Windows):

    HKLM\SOFTWARE\Microsoft\Windows\CurrentVersion\Time Zones\
    HKLM\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Time Zones\

  Under this key are several subkeys, one for each time zone.  These
  subkeys are named "Pacific" on Win9x/Me and "Pacific Standard Time"
  on WinNT/2k/XP.  There are some other wrinkles; see the code for
  details.  The subkey name is NOT LOCALIZED, allowing us to support
  localized installs.

  Under the subkey are data values.  We care about:

    Std   Standard time display name, localized
    TZI   Binary block of data

  The TZI data is of particular interest.  It contains the offset, two
  more offsets for standard and daylight time, and the start and end
  rules.  This is the same data returned by the GetTimeZoneInformation
  API.  The API may modify the data on the way out, so we have to be
  careful, but essentially we do a binary comparison against the TZI
  blocks of various registry keys.  When we find a match, we know what
  time zone Windows is set to.  Since the registry key is not
  localized, we can then translate the key through a simple table
  lookup into the corresponding ICU time zone.

  This strategy doesn't always work because there are zones which
  share an offset and rules, so more than one TZI block will match.
  For example, both Tokyo and Seoul are at GMT+9 with no DST rules;
  their TZI blocks are identical.  For these cases, we fall back to a
  name lookup.  We attempt to match the display name as stored in the
  registry for the current zone to the display name stored in the
  registry for various Windows zones.  By comparing the registry data
  directly we avoid conversion complications.

  Author: Alan Liu
  Since: ICU 2.6
  Based on original code by Carl Brown <cbrown@xnetinc.com>
*/

static LONG openTZRegKey(HKEY* hkey, const char* winid, int winType);

/**
 * Layout of the binary registry data under the "TZI" key.
 */
typedef struct {
   LONG       Bias;
   LONG       StandardBias;
   LONG       DaylightBias; /* Tweaked by GetTimeZoneInformation */
   SYSTEMTIME StandardDate;
   SYSTEMTIME DaylightDate;
} TZI;

typedef struct {
    const char* icuid;
    const char* winid;
} WindowsICUMap;

/**
 * Mapping between Windows zone IDs and ICU zone IDs.  This list has
 * been mechanically checked; all zone offsets match (most important)
 * and city names match the display city names (where possible).  The
 * presence or absence of DST differs in some cases, but this is
 * acceptable as long as the zone is semantically the same (which has
 * been manually checked).
 *
 * Windows 9x/Me zone IDs are listed as "Pacific" rather than "Pacific
 * Standard Time", which is seen in NT/2k/XP.  This is fixed-up at
 * runtime as needed.  The one exception is "Mexico Standard Time 2",
 * which is not present on Windows 9x/Me.
 *
 * Zones that are not unique under Offset+Rules should be grouped
 * together for efficiency (see code below).  In addition, rules MUST
 * be grouped so that all zones of a single offset are together.
 *
 * Comments list S(tandard) or D(aylight), as declared by Windows,
 * followed by the display name (data from Windows XP).
 *
 * NOTE: Etc/GMT+12 is CORRECT for offset GMT-12:00.  Consult
 * documentation elsewhere for an explanation.
 */
static const WindowsICUMap ZONE_MAP[] = {
    "Etc/GMT+12",           "Dateline", /* S (GMT-12:00) International Date Line West */

    "Pacific/Apia",         "Samoa", /* S (GMT-11:00) Midway Island, Samoa */

    "Pacific/Honolulu",     "Hawaiian", /* S (GMT-10:00) Hawaii */

    "America/Anchorage",    "Alaskan", /* D (GMT-09:00) Alaska */

    "America/Los_Angeles",  "Pacific", /* D (GMT-08:00) Pacific Time (US & Canada); Tijuana */

    "America/Phoenix",      "US Mountain", /* S (GMT-07:00) Arizona */
    "America/Denver",       "Mountain", /* D (GMT-07:00) Mountain Time (US & Canada) */
    "America/Chihuahua",    "Mexico Standard Time 2", /* D (GMT-07:00) Chihuahua, La Paz, Mazatlan */

    "America/Managua",      "Central America", /* S (GMT-06:00) Central America */
    "America/Regina",       "Canada Central", /* S (GMT-06:00) Saskatchewan */
    "America/Mexico_City",  "Mexico", /* D (GMT-06:00) Guadalajara, Mexico City, Monterrey */
    "America/Chicago",      "Central", /* D (GMT-06:00) Central Time (US & Canada) */

    "America/Indianapolis", "US Eastern", /* S (GMT-05:00) Indiana (East) */
    "America/Bogota",       "SA Pacific", /* S (GMT-05:00) Bogota, Lima, Quito */
    "America/New_York",     "Eastern", /* D (GMT-05:00) Eastern Time (US & Canada) */

    "America/Caracas",      "SA Western", /* S (GMT-04:00) Caracas, La Paz */
    "America/Santiago",     "Pacific SA", /* D (GMT-04:00) Santiago */
    "America/Halifax",      "Atlantic", /* D (GMT-04:00) Atlantic Time (Canada) */

    "America/St_Johns",     "Newfoundland", /* D (GMT-03:30) Newfoundland */

    "America/Buenos_Aires", "SA Eastern", /* S (GMT-03:00) Buenos Aires, Georgetown */
    "America/Godthab",      "Greenland", /* D (GMT-03:00) Greenland */
    "America/Sao_Paulo",    "E. South America", /* D (GMT-03:00) Brasilia */

    "America/Noronha",      "Mid-Atlantic", /* D (GMT-02:00) Mid-Atlantic */

    "Atlantic/Cape_Verde",  "Cape Verde", /* S (GMT-01:00) Cape Verde Is. */
    "Atlantic/Azores",      "Azores", /* D (GMT-01:00) Azores */

    "Africa/Casablanca",    "Greenwich", /* S (GMT) Casablanca, Monrovia */
    "Europe/London",        "GMT", /* D (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London */

    "Africa/Lagos",         "W. Central Africa", /* S (GMT+01:00) West Central Africa */
    "Europe/Berlin",        "W. Europe", /* D (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna */
    "Europe/Paris",         "Romance", /* D (GMT+01:00) Brussels, Copenhagen, Madrid, Paris */
    "Europe/Sarajevo",      "Central European", /* D (GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb */
    "Europe/Belgrade",      "Central Europe", /* D (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague */

    "Africa/Johannesburg",  "South Africa", /* S (GMT+02:00) Harare, Pretoria */
    "Asia/Jerusalem",       "Israel", /* S (GMT+02:00) Jerusalem */
    "Europe/Istanbul",      "GTB", /* D (GMT+02:00) Athens, Istanbul, Minsk */
    "Europe/Helsinki",      "FLE", /* D (GMT+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius */
    "Africa/Cairo",         "Egypt", /* D (GMT+02:00) Cairo */
    "Europe/Bucharest",     "E. Europe", /* D (GMT+02:00) Bucharest */

    "Africa/Nairobi",       "E. Africa", /* S (GMT+03:00) Nairobi */
    "Asia/Riyadh",          "Arab", /* S (GMT+03:00) Kuwait, Riyadh */
    "Europe/Moscow",        "Russian", /* D (GMT+03:00) Moscow, St. Petersburg, Volgograd */
    "Asia/Baghdad",         "Arabic", /* D (GMT+03:00) Baghdad */

    "Asia/Tehran",          "Iran", /* D (GMT+03:30) Tehran */

    "Asia/Muscat",          "Arabian", /* S (GMT+04:00) Abu Dhabi, Muscat */
    "Asia/Tbilisi",         "Caucasus", /* D (GMT+04:00) Baku, Tbilisi, Yerevan */

    "Asia/Kabul",           "Afghanistan", /* S (GMT+04:30) Kabul */

    "Asia/Karachi",         "West Asia", /* S (GMT+05:00) Islamabad, Karachi, Tashkent */
    "Asia/Yekaterinburg",   "Ekaterinburg", /* D (GMT+05:00) Ekaterinburg */

    "Asia/Calcutta",        "India", /* S (GMT+05:30) Chennai, Kolkata, Mumbai, New Delhi */

    "Asia/Katmandu",        "Nepal", /* S (GMT+05:45) Kathmandu */

    "Asia/Colombo",         "Sri Lanka", /* S (GMT+06:00) Sri Jayawardenepura */
    "Asia/Dhaka",           "Central Asia", /* S (GMT+06:00) Astana, Dhaka */
    "Asia/Novosibirsk",     "N. Central Asia", /* D (GMT+06:00) Almaty, Novosibirsk */

    "Asia/Rangoon",         "Myanmar", /* S (GMT+06:30) Rangoon */

    "Asia/Bangkok",         "SE Asia", /* S (GMT+07:00) Bangkok, Hanoi, Jakarta */
    "Asia/Krasnoyarsk",     "North Asia", /* D (GMT+07:00) Krasnoyarsk */

    "Australia/Perth",      "W. Australia", /* S (GMT+08:00) Perth */
    "Asia/Taipei",          "Taipei", /* S (GMT+08:00) Taipei */
    "Asia/Singapore",       "Singapore", /* S (GMT+08:00) Kuala Lumpur, Singapore */
    "Asia/Hong_Kong",       "China", /* S (GMT+08:00) Beijing, Chongqing, Hong Kong, Urumqi */
    "Asia/Irkutsk",         "North Asia East", /* D (GMT+08:00) Irkutsk, Ulaan Bataar */

    "Asia/Tokyo",           "Tokyo", /* S (GMT+09:00) Osaka, Sapporo, Tokyo */
    "Asia/Seoul",           "Korea", /* S (GMT+09:00) Seoul */
    "Asia/Yakutsk",         "Yakutsk", /* D (GMT+09:00) Yakutsk */

    "Australia/Darwin",     "AUS Central", /* S (GMT+09:30) Darwin */
    "Australia/Adelaide",   "Cen. Australia", /* D (GMT+09:30) Adelaide */

    "Pacific/Guam",         "West Pacific", /* S (GMT+10:00) Guam, Port Moresby */
    "Australia/Brisbane",   "E. Australia", /* S (GMT+10:00) Brisbane */
    "Asia/Vladivostok",     "Vladivostok", /* D (GMT+10:00) Vladivostok */
    "Australia/Hobart",     "Tasmania", /* D (GMT+10:00) Hobart */
    "Australia/Sydney",     "AUS Eastern", /* D (GMT+10:00) Canberra, Melbourne, Sydney */

    "Asia/Magadan",         "Central Pacific", /* S (GMT+11:00) Magadan, Solomon Is., New Caledonia */

    "Pacific/Fiji",         "Fiji", /* S (GMT+12:00) Fiji, Kamchatka, Marshall Is. */
    "Pacific/Auckland",     "New Zealand", /* D (GMT+12:00) Auckland, Wellington */

    "Pacific/Tongatapu",    "Tonga", /* S (GMT+13:00) Nuku'alofa */
    NULL,                   NULL
};

typedef struct {
    const char* winid;
    const char* altwinid;
} WindowsZoneRemap;

/**
 * If a lookup fails, we attempt to remap certain Windows ids to
 * alternate Windows ids.  If the alternate listed here begins with
 * '-', we use it as is (without the '-').  If it begins with '+', we
 * append a " Standard Time" if appropriate.
 */
static const WindowsZoneRemap ZONE_REMAP[] = {
    "Central European",     "-Warsaw",
    "Central Europe",       "-Prague Bratislava",
    "China",                "-Beijing",
                                               
    "Greenwich",            "+GMT",
    "GTB",                  "+GFT",
    "Arab",                 "+Saudi Arabia",
    "SE Asia",              "+Bangkok",
    "AUS Eastern",          "+Sydney",
    NULL,                   NULL,
};

/**
 * Various registry keys and key fragments.
 */
static const char CURRENT_ZONE_REGKEY[] = "SYSTEM\\CurrentControlSet\\Control\\TimeZoneInformation\\";
static const char STANDARD_NAME_REGKEY[] = "StandardName";
static const char STANDARD_TIME_REGKEY[] = " Standard Time";
static const char TZI_REGKEY[] = "TZI";
static const char STD_REGKEY[] = "Std";

/**
 * HKLM subkeys used to probe for the flavor of Windows.  Note that we
 * specifically check for the "GMT" zone subkey; this is present on
 * NT, but on XP has become "GMT Standard Time".  We need to
 * discriminate between these cases.
 */
static const char* const WIN_TYPE_PROBE_REGKEY[] = {
    /* WIN_9X_ME_TYPE */
    "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Time Zones",

    /* WIN_NT_TYPE */
    "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Time Zones\\GMT"

    /* otherwise: WIN_2K_XP_TYPE */
};

/**
 * The time zone root subkeys (under HKLM) for different flavors of
 * Windows.
 */
static const char* const TZ_REGKEY[] = {
    /* WIN_9X_ME_TYPE */
    "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Time Zones\\",

    /* WIN_NT_TYPE | WIN_2K_XP_TYPE */
    "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Time Zones\\"
};

/**
 * Flavor of Windows, from our perspective.  Not a real OS version,
 * but rather the flavor of the layout of the time zone information in
 * the registry.
 */
enum {
    WIN_9X_ME_TYPE = 0,
    WIN_NT_TYPE = 1,
    WIN_2K_XP_TYPE = 2
};

/**
 * Main Windows time zone detection function.  Returns the Windows
 * time zone, translated to an ICU time zone, or NULL upon failure.
 */
static const char* detectWindowsTimeZone() {
    int winType;
    LONG result;
    HKEY hkey;
    TZI tziKey;
    TZI tziReg;
    DWORD cbData = sizeof(TZI);
    TIME_ZONE_INFORMATION apiTZI;
    char stdName[32];
    DWORD stdNameSize;
    char stdRegName[64];
    DWORD stdRegNameSize;
    int firstMatch, lastMatch;
    int j;

    /* Detect the version of windows by trying to open a sequence of
       probe keys.  We don't use the OS version API because what we
       really want to know is how the registry is laid out.
       Specifically, is it 9x/Me or not, and is it "GMT" or "GMT
       Standard Time". */
    for (winType=0; winType<2; ++winType) {
        result = RegOpenKeyEx(HKEY_LOCAL_MACHINE,
                              WIN_TYPE_PROBE_REGKEY[winType],
                              0,
                              KEY_QUERY_VALUE,
                              &hkey);
        RegCloseKey(hkey);
        if (result == ERROR_SUCCESS) {
            break;
        }
    }

    /* Obtain TIME_ZONE_INFORMATION from the API, and then convert it
       to TZI.  We could also interrogate the registry directly; we do
       this below if needed. */
    uprv_memset(&apiTZI, 0, sizeof(apiTZI));
    GetTimeZoneInformation(&apiTZI);
    tziKey.Bias = apiTZI.Bias;
    uprv_memcpy((char *)&tziKey.StandardDate, (char*)&apiTZI.StandardDate,
           sizeof(apiTZI.StandardDate));
    uprv_memcpy((char *)&tziKey.DaylightDate, (char*)&apiTZI.DaylightDate,
           sizeof(apiTZI.DaylightDate));

    /* For each zone that can be identified by Offset+Rules, see if we
       have a match.  Continue scanning after finding a match,
       recording the index of the first and the last match.  We have
       to do this because some zones are not unique under
       Offset+Rules. */
    firstMatch = lastMatch = -1;
    for (j=0; ZONE_MAP[j].icuid; j++) {
        result = openTZRegKey(&hkey, ZONE_MAP[j].winid, winType);
        if (result == ERROR_SUCCESS) {
            result = RegQueryValueEx(hkey,
                                     TZI_REGKEY,
                                     NULL,
                                     NULL,
                                     (LPBYTE)&tziReg,
                                     &cbData);
        }
        RegCloseKey(hkey);
        if (result == ERROR_SUCCESS) {
            /* Assume that offsets are grouped together, and bail out
               when we've scanned everything with a matching
               offset. */
            if (firstMatch >= 0 && tziKey.Bias != tziReg.Bias) {
                break;
            }
            /* Windows alters the DaylightBias in some situations.
               Using the bias and the rules suffices, so overwrite
               these unreliable fields. */
            tziKey.StandardBias = tziReg.StandardBias;
            tziKey.DaylightBias = tziReg.DaylightBias;
            if (uprv_memcmp((char *)&tziKey, (char*)&tziReg,
                       sizeof(tziKey)) == 0) {
                if (firstMatch < 0) {
                    firstMatch = j;
                }
                lastMatch = j;
            }
        }
    }

    /* This should never happen; if it does it means our table doesn't
       match Windows AT ALL, perhaps because this is post-XP? */
    if (firstMatch < 0) {
        return NULL;
    }
    
    if (firstMatch != lastMatch) {
        /* Offset+Rules lookup yielded >= 2 matches.  Try to match the
           localized display name.  Get the name from the registry
           (not the API). This avoids conversion issues.  Use the
           standard name, since Windows modifies the daylight name to
           match the standard name if there is no DST. */
        result = RegOpenKeyEx(HKEY_LOCAL_MACHINE,
                              CURRENT_ZONE_REGKEY,
                              0,
                              KEY_QUERY_VALUE,
                              &hkey);
        if (result == ERROR_SUCCESS) {
            stdNameSize = sizeof(stdName);
            result = RegQueryValueEx(hkey,
                                     (LPTSTR)STANDARD_NAME_REGKEY,
                                     NULL,
                                     NULL,
                                     (LPBYTE)stdName,
                                     &stdNameSize);
            RegCloseKey(hkey);

            /* Scan through the Windows time zone data in the registry
               again (just the range of zones with matching TZIs) and
               look for a standard display name match. */
            for (j=firstMatch; j<=lastMatch; j++) {
                result = openTZRegKey(&hkey, ZONE_MAP[j].winid, winType);
                if (result == ERROR_SUCCESS) {
                    stdRegNameSize = sizeof(stdRegName);
                    result = RegQueryValueEx(hkey,
                                             (LPTSTR)STD_REGKEY,
                                             NULL,
                                             NULL,
                                             (LPBYTE)stdRegName,
                                             &stdRegNameSize);
                }
                RegCloseKey(hkey);
                if (result == ERROR_SUCCESS &&
                    stdRegNameSize == stdNameSize &&
                    uprv_memcmp(stdName, stdRegName, stdNameSize) == 0) {
                    firstMatch = j; /* record the match */
                    break;
                }
            }
        } else {
            RegCloseKey(hkey); /* should never get here */
        }
    }

    return ZONE_MAP[firstMatch].icuid;
}

/**
 * Auxiliary Windows time zone function.  Attempts to open the given
 * Windows time zone ID as a registry key.  Returns ERROR_SUCCESS if
 * successful.  Caller must close the registry key.  Handles
 * variations in the resource layout in different flavors of Windows.
 *
 * @param hkey output parameter to receive opened registry key
 * @param winid Windows zone ID, e.g., "Pacific", without the
 * " Standard Time" suffix (if any).  Special case "Mexico Standard Time 2"
 * allowed.
 * @param winType Windows flavor (WIN_9X_ME_TYPE, etc.)
 * @return ERROR_SUCCESS upon success
 */
static LONG openTZRegKey(HKEY *hkey, const char* winid, int winType) {
    LONG result;
    char subKeyName[96];
    char* name;
    int i;

    uprv_strcpy(subKeyName, TZ_REGKEY[(winType == WIN_9X_ME_TYPE) ? 0 : 1]);
    name = &subKeyName[strlen(subKeyName)];
    uprv_strcat(subKeyName, winid);
    if (winType != WIN_9X_ME_TYPE) {
        /* Don't modify "Mexico Standard Time 2", which does not occur
           on WIN_9X_ME_TYPE.  Also, if the type is WIN_NT_TYPE, then
           in practice this means the GMT key is not followed by
           " Standard Time", so don't append in that case. */
        int isMexico2 = (winid[uprv_strlen(winid)- 1] == '2');
        if (!isMexico2 &&
            !(winType == WIN_NT_TYPE && uprv_strcmp(winid, "GMT") == 0)) {
            uprv_strcat(subKeyName, STANDARD_TIME_REGKEY);
        }
    }
    result = RegOpenKeyEx(HKEY_LOCAL_MACHINE,
                          subKeyName,
                          0,
                          KEY_QUERY_VALUE,
                          hkey);

    if (result != ERROR_SUCCESS) {
        /* If the primary lookup fails, try to remap the Windows zone
           ID, according to the remapping table. */
        for (i=0; ZONE_REMAP[i].winid; ++i) {
            if (uprv_strcmp(winid, ZONE_REMAP[i].winid) == 0) {
                uprv_strcpy(name, ZONE_REMAP[i].altwinid + 1);
                if (*(ZONE_REMAP[i].altwinid) == '+' &&
                    winType != WIN_9X_ME_TYPE) {
                    uprv_strcat(subKeyName, STANDARD_TIME_REGKEY);                
                }
                result = RegOpenKeyEx(HKEY_LOCAL_MACHINE,
                                      subKeyName,
                                      0,
                                      KEY_QUERY_VALUE,
                                      hkey);
                break;
            }
        }
    }

    return result;
}

#endif /*WIN32*/

/* Generic time zone layer -------------------------------------------------- */

/* Time zone utilities */
U_CAPI void U_EXPORT2
uprv_tzset()
{
#ifdef U_TZSET
    U_TZSET();
#else
    /* no initialization*/
#endif
}

U_CAPI int32_t U_EXPORT2
uprv_timezone()
{
#if U_HAVE_TIMEZONE
    return U_TIMEZONE;
#else
    time_t t, t1, t2;
    struct tm tmrec;
    UBool dst_checked;
    int32_t tdiff = 0;

    time(&t);
    uprv_memcpy( &tmrec, localtime(&t), sizeof(tmrec) );
    dst_checked = (tmrec.tm_isdst != 0); /* daylight savings time is checked*/
    t1 = mktime(&tmrec);                 /* local time in seconds*/
    uprv_memcpy( &tmrec, gmtime(&t), sizeof(tmrec) );
    t2 = mktime(&tmrec);                 /* GMT (or UTC) in seconds*/
    tdiff = t2 - t1;
    /* imitate NT behaviour, which returns same timezone offset to GMT for
       winter and summer*/
    if (dst_checked)
        tdiff += 3600;
    return tdiff;
#endif
}

/* Note that U_TZNAME does *not* have to be tzname, but if it is,
   some platforms need to have it declared here. */ 

#if defined(U_IRIX) || defined(U_DARWIN) /* For SGI or Mac OS X.  */
extern char *tzname[]; /* RS6000 and others reject char **tzname.  */
#elif defined(U_CYGWIN)
extern U_IMPORT char *_tzname[2]; 
#endif

#if defined(U_DARWIN)	/* For Mac OS X */
#define TZZONELINK	"/etc/localtime"
#define TZZONEINFO	"/usr/share/zoneinfo/"
static char *gTimeZoneBuffer = NULL; /* Heap allocated */
#endif

#include <stdio.h>

U_CAPI char* U_EXPORT2
uprv_tzname(int n)
{
#ifdef WIN32
    char* id = (char*) detectWindowsTimeZone();
    if (id != NULL) {
        return id;
    }
#endif

#if defined(U_DARWIN)
    int ret;

    char *tzenv;

    tzenv = getenv("TZFILE");
    if (tzenv != NULL) {
    	return tzenv;
    }

#if 0
    /* TZ is often set to "PST8PDT" or similar, so we cannot use it. Alan */
    tzenv = getenv("TZ");
    if (tzenv != NULL) {
    	return tzenv;
    }
#endif
    
    /* Caller must handle threading issues */
    if (gTimeZoneBuffer == NULL) {
    	gTimeZoneBuffer = (char *) uprv_malloc(MAXPATHLEN + 2);

        ret = readlink(TZZONELINK, gTimeZoneBuffer, MAXPATHLEN + 2);
        if (0 < ret) {
            gTimeZoneBuffer[ret] = '\0';
            if (uprv_strncmp(gTimeZoneBuffer, TZZONEINFO, sizeof(TZZONEINFO) - 1) == 0) {
                return (gTimeZoneBuffer += sizeof(TZZONEINFO) - 1);
            }
        }

        uprv_free(gTimeZoneBuffer);
        gTimeZoneBuffer = NULL;
    }
#endif

#ifdef U_TZNAME
    return U_TZNAME[n];
#else
    return "";
#endif
}

/* Get and set the ICU data directory --------------------------------------- */

static char *gDataDirectory = NULL;
#if U_POSIX_LOCALE
 static char *gCorrectedPOSIXLocale = NULL; /* Heap allocated */
#endif

UBool putil_cleanup(void)
{
    if (gDataDirectory) {
        uprv_free(gDataDirectory);
        gDataDirectory = NULL;
    }
#if U_POSIX_LOCALE
    if (gCorrectedPOSIXLocale) {
        uprv_free(gCorrectedPOSIXLocale);
        gCorrectedPOSIXLocale = NULL;
    }
#endif
    return TRUE;
}

/*
 * Set the data directory.
 *    Make a copy of the passed string, and set the global data dir to point to it.
 *    TODO:  see bug #2849, regarding thread safety.
 */
U_CAPI void U_EXPORT2
u_setDataDirectory(const char *directory) {
    char *newDataDir;
#if (U_FILE_SEP_CHAR != U_FILE_ALT_SEP_CHAR)
    char *p;
#endif
    int32_t length;

    if(directory==NULL) {
        directory = "";
    }
    length=(int32_t)uprv_strlen(directory);
    newDataDir = (char *)uprv_malloc(length + 2);
    uprv_strcpy(newDataDir, directory);

#if (U_FILE_SEP_CHAR != U_FILE_ALT_SEP_CHAR)
    while(p = uprv_strchr(newDataDir, U_FILE_ALT_SEP_CHAR)) {
       *p = U_FILE_SEP_CHAR;
    }
#endif

    umtx_lock(NULL);
    if (gDataDirectory) {
        uprv_free(gDataDirectory);
    }
    gDataDirectory = newDataDir;
    umtx_unlock(NULL);
}

U_CAPI UBool U_EXPORT2
uprv_pathIsAbsolute(const char *path) 
{
  if(!path || !*path) { 
    return FALSE; 
  }

  if(*path == U_FILE_SEP_CHAR) {
    return TRUE;
  }

#if (U_FILE_SEP_CHAR != U_FILE_ALT_SEP_CHAR)
  if(*path == U_FILE_ALT_SEP_CHAR) {
    return TRUE;
  }
#endif

#if defined(WIN32)
  if( (((path[0] >= 'A') && (path[0] <= 'Z')) ||
       ((path[0] >= 'a') && (path[0] <= 'z'))) &&
      path[1] == ':' ) {
    return TRUE;
  }
#endif

  return FALSE;
}

U_CAPI const char * U_EXPORT2
u_getDataDirectory(void) {
    const char *path = NULL;
    char pathBuffer[1024];
    const char *dataDir;

    /* if we have the directory, then return it immediately */
    umtx_lock(NULL);
    dataDir = gDataDirectory;
    umtx_unlock(NULL);

    if(dataDir) {
        return dataDir;
    }

    /* we need to look for it */
    pathBuffer[0] = 0;                     /* Shuts up compiler warnings about unreferenced */
                                           /*   variables when the code using it is ifdefed out */
#   if !defined(XP_MAC)
    /* first try to get the environment variable */
    path=getenv("ICU_DATA");
#   else    /* XP_MAC */
    {
        OSErr myErr;
        short vRef;
        long  dir,newDir;
        int16_t volNum;
        Str255 xpath;
        FSSpec spec;
        short  len;
        Handle full;

        xpath[0]=0;

        myErr = HGetVol(xpath, &volNum, &dir);

        if(myErr == noErr) {
            myErr = FindFolder(volNum, kApplicationSupportFolderType, TRUE, &vRef, &dir);
            newDir=-1;
            if (myErr == noErr) {
                myErr = DirCreate(volNum,
                    dir,
                    "\pICU",
                    &newDir);
                if( (myErr == noErr) || (myErr == dupFNErr) ) {
                    spec.vRefNum = volNum;
                    spec.parID = dir;
                    uprv_memcpy(spec.name, "\pICU", 4);

                    myErr = FSpGetFullPath(&spec, &len, &full);
                    if(full != NULL)
                    {
                        HLock(full);
                        uprv_memcpy(pathBuffer,  ((char*)(*full)), len);
                        pathBuffer[len] = 0;
                        path = pathBuffer;
                        DisposeHandle(full);
                    }
                }
            }
        }
    }
#       endif


#       if defined WIN32 && defined ICU_ENABLE_DEPRECATED_WIN_REGISTRY
    /* next, try to read the path from the registry */
    if(path==NULL || *path==0) {
        HKEY key;

        if(ERROR_SUCCESS==RegOpenKeyEx(HKEY_LOCAL_MACHINE, "SOFTWARE\\ICU\\Unicode\\Data", 0, KEY_QUERY_VALUE, &key)) {
            DWORD type=REG_EXPAND_SZ, size=sizeof(pathBuffer);

            if(ERROR_SUCCESS==RegQueryValueEx(key, "Path", NULL, &type, (unsigned char *)pathBuffer, &size) && size>1) {
                if(type==REG_EXPAND_SZ) {
                    /* replace environment variable references by their values */
                    char temporaryPath[1024];

                    /* copy the path with variables to the temporary one */
                    uprv_memcpy(temporaryPath, pathBuffer, size);

                    /* do the replacement and store it in the pathBuffer */
                    size=ExpandEnvironmentStrings(temporaryPath, pathBuffer, sizeof(pathBuffer));
                    if(size>0 && size<sizeof(pathBuffer)) {
                        path=pathBuffer;
                    }
                } else if(type==REG_SZ) {
                    path=pathBuffer;
                }
            }
            RegCloseKey(key);
        }
    }
#       endif

    /* ICU_DATA_DIR may be set as a compile option */
#   ifdef ICU_DATA_DIR
    if(path==NULL || *path==0) {
        path=ICU_DATA_DIR;
    }
#   endif

    if(path==NULL) {
        /* It looks really bad, set it to something. */
        path = "";
    }

    u_setDataDirectory(path);
    return gDataDirectory;
}





/* Macintosh-specific locale information ------------------------------------ */
#ifdef XP_MAC

typedef struct {
    int32_t script;
    int32_t region;
    int32_t lang;
    int32_t date_region;
    const char* posixID;
} mac_lc_rec;

/* Todo: This will be updated with a newer version from www.unicode.org web
   page when it's available.*/
#define MAC_LC_MAGIC_NUMBER -5
#define MAC_LC_INIT_NUMBER -9

static const mac_lc_rec mac_lc_recs[] = {
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 0, "en_US",
    /* United States*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 1, "fr_FR",
    /* France*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 2, "en_GB",
    /* Great Britain*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 3, "de_DE",
    /* Germany*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 4, "it_IT",
    /* Italy*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 5, "nl_NL",
    /* Metherlands*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 6, "fr_BE",
    /* French for Belgium or Lxembourg*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 7, "sv_SE",
    /* Sweden*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 9, "da_DK",
    /* Denmark*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 10, "pt_PT",
    /* Portugal*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 11, "fr_CA",
    /* French Canada*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 13, "is_IS",
    /* Israel*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 14, "ja_JP",
    /* Japan*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 15, "en_AU",
    /* Australia*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 16, "ar_AE",
    /* the Arabic world (?)*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 17, "fi_FI",
    /* Finland*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 18, "fr_CH",
    /* French for Switzerland*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 19, "de_CH",
    /* German for Switzerland*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 20, "el_GR",
    /* Greece*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 21, "is_IS",
    /* Iceland ===*/
    /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 22, "",*/
    /* Malta ===*/
    /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 23, "",*/
    /* Cyprus ===*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 24, "tr_TR",
    /* Turkey ===*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 25, "sh_YU",
    /* Croatian system for Yugoslavia*/
    /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 33, "",*/
    /* Hindi system for India*/
    /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 34, "",*/
    /* Pakistan*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 41, "lt_LT",
    /* Lithuania*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 42, "pl_PL",
    /* Poland*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 43, "hu_HU",
    /* Hungary*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 44, "et_EE",
    /* Estonia*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 45, "lv_LV",
    /* Latvia*/
    /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 46, "",*/
    /* Lapland  [Ask Rich for the data. HS]*/
    /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 47, "",*/
    /* Faeroe Islands*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 48, "fa_IR",
    /* Iran*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 49, "ru_RU",
    /* Russia*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 50, "en_IE",
    /* Ireland*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 51, "ko_KR",
    /* Korea*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 52, "zh_CN",
    /* People's Republic of China*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 53, "zh_TW",
    /* Taiwan*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 54, "th_TH",
    /* Thailand*/

    /* fallback is en_US*/
    MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER,
    MAC_LC_MAGIC_NUMBER, "en_US"
};

#endif

#if U_POSIX_LOCALE
/* Return just the POSIX id, whatever happens to be in it */
static const char *uprv_getPOSIXID(void)
{
    static const char* posixID = NULL;
    if (posixID == 0) {
        posixID = getenv("LC_ALL");
        if (posixID == 0) {
            posixID = getenv("LANG");
            if (posixID == 0) {
                /*
                * On Solaris two different calls to setlocale can result in 
                * different values. Only get this value once.
                */
                posixID = setlocale(LC_ALL, NULL);
            }
        }
    }

    if (posixID==0)
    {
        /* Nothing worked.  Give it a nice value. */
        posixID = "en_US";
    }
    else if ((uprv_strcmp("C", posixID) == 0)
        || (uprv_strchr(posixID, ' ') != NULL)
        || (uprv_strchr(posixID, '/') != NULL))
    {   /* HPUX returns 'C C C C C C C' */
        /* Solaris can return /en_US/C/C/C/C/C on the second try. */
        /* Maybe we got some garbage.  Give it a nice value. */
        posixID = "en_US_POSIX";
    }
    return posixID;
}
#endif

/* NOTE: The caller should handle thread safety */
U_CAPI const char* U_EXPORT2
uprv_getDefaultLocaleID()
{
#if U_POSIX_LOCALE
/*
  Note that:  (a '!' means the ID is improper somehow)
     LC_ALL  ---->     default_loc          codepage
--------------------------------------------------------
     ab.CD             ab                   CD
     ab@CD             ab__CD               -
     ab@CD.EF          ab__CD               EF

     ab_CD.EF@GH       ab_CD_GH             EF

Some 'improper' ways to do the same as above:
  !  ab_CD@GH.EF       ab_CD_GH             EF
  !  ab_CD.EF@GH.IJ    ab_CD_GH             EF
  !  ab_CD@ZZ.EF@GH.IJ ab_CD_GH             EF

     _CD@GH            _CD_GH               -
     _CD.EF@GH         _CD_GH               EF

The variant cannot have dots in it.
The 'rightmost' variant (@xxx) wins.
The leftmost codepage (.xxx) wins.
*/
    char *correctedPOSIXLocale = 0;
    const char* posixID = uprv_getPOSIXID();
    const char *p;
    const char *q;
    int32_t len;

    /* Format: (no spaces)
    ll [ _CC ] [ . MM ] [ @ VV]

      l = lang, C = ctry, M = charmap, V = variant
    */

    if (gCorrectedPOSIXLocale != NULL) {
        return gCorrectedPOSIXLocale; 
    }

    if ((p = uprv_strchr(posixID, '.')) != NULL) {
        /* assume new locale can't be larger than old one? */
        correctedPOSIXLocale = uprv_malloc(uprv_strlen(posixID));
        uprv_strncpy(correctedPOSIXLocale, posixID, p-posixID);
        correctedPOSIXLocale[p-posixID] = 0;

        /* do not copy after the @ */
        if ((p = uprv_strchr(correctedPOSIXLocale, '@')) != NULL) {
            correctedPOSIXLocale[p-correctedPOSIXLocale] = 0;
        }
    }

    /* Note that we scan the *uncorrected* ID. */
    if ((p = uprv_strrchr(posixID, '@')) != NULL) {
        if (correctedPOSIXLocale == NULL) {
            correctedPOSIXLocale = uprv_malloc(uprv_strlen(posixID));
            uprv_strncpy(correctedPOSIXLocale, posixID, p-posixID);
            correctedPOSIXLocale[p-posixID] = 0;
        }
        p++;

        /* Take care of any special cases here.. */
        if (!uprv_strcmp(p, "nynorsk")) {
            p = "NY";

            /*      Should we assume no_NO_NY instead of possible no__NY?
            * if (!uprv_strcmp(correctedPOSIXLocale, "no")) {
            *     uprv_strcpy(correctedPOSIXLocale, "no_NO");
            * }
            */
        }

        if (uprv_strchr(correctedPOSIXLocale,'_') == NULL) {
            uprv_strcat(correctedPOSIXLocale, "__"); /* aa@b -> aa__b */
        }
        else {
            uprv_strcat(correctedPOSIXLocale, "_"); /* aa_CC@b -> aa_CC_b */
        }

        if ((q = uprv_strchr(p, '.')) != NULL) {
            /* How big will the resulting string be? */
            len = (int32_t)(uprv_strlen(correctedPOSIXLocale) + (q-p));
            uprv_strncat(correctedPOSIXLocale, p, q-p);
            correctedPOSIXLocale[len] = 0;
        }
        else {
            /* Anything following the @ sign */
            uprv_strcat(correctedPOSIXLocale, p);
        }

        /* Should there be a map from 'no@nynorsk' -> no_NO_NY here?
         * How about 'russian' -> 'ru'?
         */
    }

    /* Was a correction made? */
    if (correctedPOSIXLocale != NULL) {
        posixID = correctedPOSIXLocale;
    }
    else {
        /* copy it, just in case the original pointer goes away.  See j2395 */
        correctedPOSIXLocale = (char *)uprv_malloc(uprv_strlen(posixID) + 1);
        posixID = uprv_strcpy(correctedPOSIXLocale, posixID);
    }

    if (gCorrectedPOSIXLocale == NULL) {
        gCorrectedPOSIXLocale = correctedPOSIXLocale;
        correctedPOSIXLocale = NULL;
    }

    if (correctedPOSIXLocale != NULL) {  /* Was already set - clean up. */
        uprv_free(correctedPOSIXLocale); 
    }

    return posixID;

#elif defined(WIN32)
    UErrorCode status = U_ZERO_ERROR;
    LCID id = GetThreadLocale();
    const char* locID = uprv_convertToPosix(id, &status);

    if (U_FAILURE(status)) {
        locID = "en_US";
    }
    return locID;

#elif defined(XP_MAC)
    int32_t script = MAC_LC_INIT_NUMBER;
    /* = IntlScript(); or GetScriptManagerVariable(smSysScript);*/
    int32_t region = MAC_LC_INIT_NUMBER;
    /* = GetScriptManagerVariable(smRegionCode);*/
    int32_t lang = MAC_LC_INIT_NUMBER;
    /* = GetScriptManagerVariable(smScriptLang);*/
    int32_t date_region = MAC_LC_INIT_NUMBER;
    const char* posixID = 0;
    int32_t count = sizeof(mac_lc_recs) / sizeof(mac_lc_rec);
    int32_t i;
    Intl1Hndl ih;

    ih = (Intl1Hndl) GetIntlResource(1);
    if (ih)
        date_region = ((uint16_t)(*ih)->intl1Vers) >> 8;

    for (i = 0; i < count; i++) {
        if (   ((mac_lc_recs[i].script == MAC_LC_MAGIC_NUMBER)
             || (mac_lc_recs[i].script == script))
            && ((mac_lc_recs[i].region == MAC_LC_MAGIC_NUMBER)
             || (mac_lc_recs[i].region == region))
            && ((mac_lc_recs[i].lang == MAC_LC_MAGIC_NUMBER)
             || (mac_lc_recs[i].lang == lang))
            && ((mac_lc_recs[i].date_region == MAC_LC_MAGIC_NUMBER)
             || (mac_lc_recs[i].date_region == date_region))
            )
        {
            posixID = mac_lc_recs[i].posixID;
            break;
        }
    }

    return posixID;

#elif defined(OS2)
    char * locID;

    locID = getenv("LC_ALL");
    if (!locID || !*locID)
        locID = getenv("LANG");
    if (!locID || !*locID) {
        locID = "en_US";
    }
    if (!stricmp(locID, "c") || !stricmp(locID, "posix") ||
        !stricmp(locID, "univ"))
        locID = "en_US_POSIX";
    return locID;

#elif defined(OS400)
    /* locales are process scoped and are by definition thread safe */
    static char correctedLocale[64];
    const  char *localeID = getenv("LC_ALL");
           char *p;

    if (localeID == NULL)
        localeID = getenv("LANG");
    if (localeID == NULL)
        localeID = setlocale(LC_ALL, NULL);
    /* Make sure we have something... */
    if (localeID == NULL)
        return "en_US_POSIX";

    /* Extract the locale name from the path. */
    if((p = uprv_strrchr(localeID, '/')) != NULL)
    {
        /* Increment p to start of locale name. */
        p++;
        localeID = p;
    }

    /* Copy to work location. */
    uprv_strcpy(correctedLocale, localeID);

    /* Strip off the '.locale' extension. */
    if((p = uprv_strchr(correctedLocale, '.')) != NULL) {
        *p = 0;
    }

    /* Upper case the locale name. */
    T_CString_toUpperCase(correctedLocale);

    /* See if we are using the POSIX locale.  Any of the
    * following are equivalent and use the same QLGPGCMA
    * (POSIX) locale.
    */
    if ((uprv_strcmp("C", correctedLocale) == 0) ||
        (uprv_strcmp("POSIX", correctedLocale) == 0) ||
        (uprv_strcmp("QLGPGCMA", correctedLocale) == 0))
    {
        uprv_strcpy(correctedLocale, "en_US_POSIX");
    }
    else
    {
        int16_t LocaleLen;

        /* Lower case the lang portion. */
        for(p = correctedLocale; *p != 0 && *p != '_'; p++)
        {
            *p = uprv_tolower(*p);
        }

        /* Adjust for Euro.  After '_E' add 'URO'. */
        LocaleLen = uprv_strlen(correctedLocale);
        if (correctedLocale[LocaleLen - 2] == '_' &&
            correctedLocale[LocaleLen - 1] == 'E')
        {
            uprv_strcat(correctedLocale, "URO");
        }

        /* If using Lotus-based locale then convert to
         * equivalent non Lotus.
         */
        else if (correctedLocale[LocaleLen - 2] == '_' &&
            correctedLocale[LocaleLen - 1] == 'L')
        {
            correctedLocale[LocaleLen - 2] = 0;
        }

        /* There are separate simplified and traditional
         * locales called zh_HK_S and zh_HK_T.
         */
        else if (uprv_strncmp(correctedLocale, "zh_HK", 5) == 0)
        {
            uprv_strcpy(correctedLocale, "zh_HK");
        }

        /* A special zh_CN_GBK locale...
        */
        else if (uprv_strcmp(correctedLocale, "zh_CN_GBK") == 0)
        {
            uprv_strcpy(correctedLocale, "zh_CN");
        }

    }

    return correctedLocale;
#endif

}


static const char*  
int_getDefaultCodepage()
{
#if defined(OS400)
    uint32_t ccsid = 37; /* Default to ibm-37 */
    static char codepage[64];
    Qwc_JOBI0400_t jobinfo;
    Qus_EC_t error = { sizeof(Qus_EC_t) }; /* SPI error code */

    EPT_CALL(QUSRJOBI)(&jobinfo, sizeof(jobinfo), "JOBI0400",
        "*                         ", "                ", &error);

    if (error.Bytes_Available == 0) {
        if (jobinfo.Coded_Char_Set_ID != 0xFFFF) {
            ccsid = (uint32_t)jobinfo.Coded_Char_Set_ID;
        }
        else if (jobinfo.Default_Coded_Char_Set_Id != 0xFFFF) {
            ccsid = (uint32_t)jobinfo.Default_Coded_Char_Set_Id;
        }
        /* else use the default */
    }
    sprintf(codepage,"ibm-%d", ccsid);
    return codepage;

#elif defined(OS390)
    static char codepage[64];
    sprintf(codepage,"%s" UCNV_SWAP_LFNL_OPTION_STRING, nl_langinfo(CODESET));
    return codepage;

#elif defined(XP_MAC)
    return "ibm-1275"; /* TODO: Macintosh Roman. There must be a better way. fixme! */

#elif defined(WIN32)
    static char codepage[64];
    sprintf(codepage, "windows-%d", GetACP());
    return codepage;

#elif U_POSIX_LOCALE
    static char codesetName[100];
    char *name = NULL;
    char *euro = NULL;
    const char *localeName = NULL;
    const char *defaultTable = NULL;

    uprv_memset(codesetName, 0, sizeof(codesetName));
    localeName = uprv_getPOSIXID();
    if (localeName != NULL && (name = (uprv_strchr(localeName, (int)'.'))) != NULL)
    {
        /* strip the locale name and look at the suffix only */
        name = uprv_strncpy(codesetName, name+1, sizeof(codesetName));
        codesetName[sizeof(codesetName)-1] = 0;
        if ((euro = (uprv_strchr(name, (int)'@'))) != NULL)
        {
           *euro = 0;
        }
        /* if we can find the codset name, return that. */
        if (*name)
        {
            return name;
        }
    }

    /* otherwise, try CTYPE */
    if (*codesetName)
    {
        uprv_memset(codesetName, 0, sizeof(codesetName));
    }
    localeName = setlocale(LC_CTYPE, NULL);
    if (localeName != NULL && (name = (uprv_strchr(localeName, (int)'.'))) != NULL)
    {
        /* strip the locale name and look at the suffix only */
        name = uprv_strncpy(codesetName, name+1, sizeof(codesetName));
        codesetName[sizeof(codesetName)-1] = 0;
        if ((euro = (uprv_strchr(name, (int)'@'))) != NULL)
        {
           *euro = 0;
        }
        /* if we can find the codset name from setlocale, return that. */
        if (*name)
        {
            return name;
        }
    }

    if (*codesetName)
    {
        uprv_memset(codesetName, 0, sizeof(codesetName));
    }
#if U_HAVE_NL_LANGINFO_CODESET
    {
        const char *codeset = nl_langinfo(U_NL_LANGINFO_CODESET);
        if (codeset != NULL) {
            uprv_strncpy(codesetName, codeset, sizeof(codesetName));
            codesetName[sizeof(codesetName)-1] = 0;
        }
    }
#endif
    if (*codesetName == 0)
    {
        /* look up in srl's table */
        defaultTable = uprv_defaultCodePageForLocale(localeName);
        if (defaultTable != NULL)
        {
            uprv_strcpy(codesetName, defaultTable);
        }
        else
        {
            /* if the table lookup failed, return US ASCII (ISO 646). */
            uprv_strcpy(codesetName, "US-ASCII");
        }
    }
    return codesetName;
#else
    return "US-ASCII";
#endif
}


U_CAPI const char*  U_EXPORT2
uprv_getDefaultCodepage()
{
    static char const  *name = NULL;
    umtx_lock(NULL);
    if (name == NULL) {
        name = int_getDefaultCodepage();
    }
    umtx_unlock(NULL);
    return name;
}



/* invariant-character handling --------------------------------------------- */

/*
 * These maps for ASCII to/from EBCDIC map invariant characters (see utypes.h)
 * appropriately for most EBCDIC codepages.
 *
 * They currently also map most other ASCII graphic characters,
 * appropriately for codepages 37 and 1047.
 * Exceptions: The characters for []^ have different codes in 37 & 1047.
 * Both versions are mapped to ASCII.
 *
 *    ASCII 37 1047
 * [     5B BA   AD
 * ]     5D BB   BD
 * ^     5E B0   5F
 *
 * There are no mappings for variant characters from Unicode to EBCDIC.
 *
 * Currently, C0 control codes are also included in these maps.
 * Exceptions: S/390 Open Edition swaps LF and NEL codes compared with other
 * EBCDIC platforms; both codes (15 and 25) are mapped to ASCII LF (0A),
 * but there is no mapping for ASCII LF back to EBCDIC.
 *
 *    ASCII EBCDIC S/390-OE
 * LF    0A     25       15
 * NEL   85     15       25
 *
 * The maps below explicitly exclude the variant
 * control and graphical characters that are in ASCII-based
 * codepages at 0x80 and above.
 * "No mapping" is expressed by mapping to a 00 byte.
 *
 * These tables do not establish a converter or a codepage.
 */

static const uint8_t asciiFromEbcdic[256]={
    0x00, 0x01, 0x02, 0x03, 0x00, 0x09, 0x00, 0x7f, 0x00, 0x00, 0x00, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
    0x10, 0x11, 0x12, 0x13, 0x00, 0x0a, 0x08, 0x00, 0x18, 0x19, 0x00, 0x00, 0x1c, 0x1d, 0x1e, 0x1f,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x17, 0x1b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x06, 0x07,
    0x00, 0x00, 0x16, 0x00, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x14, 0x15, 0x00, 0x1a,

    0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2e, 0x3c, 0x28, 0x2b, 0x7c,
    0x26, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x21, 0x24, 0x2a, 0x29, 0x3b, 0x5e,
    0x2d, 0x2f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x25, 0x5f, 0x3e, 0x3f,
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x60, 0x3a, 0x23, 0x40, 0x27, 0x3d, 0x22,

    0x00, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f, 0x70, 0x71, 0x72, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x00, 0x7e, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x00, 0x00, 0x00, 0x5b, 0x00, 0x00,
    0x5e, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x5b, 0x5d, 0x00, 0x5d, 0x00, 0x00,

    0x7b, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x7d, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, 0x50, 0x51, 0x52, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x5c, 0x00, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

static const uint8_t ebcdicFromAscii[256]={
    0x00, 0x01, 0x02, 0x03, 0x37, 0x2d, 0x2e, 0x2f, 0x16, 0x05, 0x00, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
    0x10, 0x11, 0x12, 0x13, 0x3c, 0x3d, 0x32, 0x26, 0x18, 0x19, 0x3f, 0x27, 0x1c, 0x1d, 0x1e, 0x1f,
    0x40, 0x00, 0x7f, 0x00, 0x00, 0x6c, 0x50, 0x7d, 0x4d, 0x5d, 0x5c, 0x4e, 0x6b, 0x60, 0x4b, 0x61,
    0xf0, 0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0x7a, 0x5e, 0x4c, 0x7e, 0x6e, 0x6f,

    0x00, 0xc1, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xd1, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6,
    0xd7, 0xd8, 0xd9, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0x00, 0x00, 0x00, 0x00, 0x6d,
    0x00, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96,
    0x97, 0x98, 0x99, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0x00, 0x00, 0x00, 0x00, 0x07,

    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
};

/*
 * Bit sets indicating which characters of the ASCII repertoire
 * (by ASCII/Unicode code) are "invariant".
 * See utypes.h for more details.
 *
 * As invariant are considered the characters of the ASCII repertoire except
 * for the following:
 * 21  '!' <exclamation mark>
 * 23  '#' <number sign>
 * 24  '$' <dollar sign>
 *
 * 40  '@' <commercial at>
 *
 * 5b  '[' <left bracket>
 * 5c  '\' <backslash>
 * 5d  ']' <right bracket>
 * 5e  '^' <circumflex>
 *
 * 60  '`' <grave accent>
 *
 * 7b  '{' <left brace>
 * 7c  '|' <vertical line>
 * 7d  '}' <right brace>
 * 7e  '~' <tilde>
 */
static const uint32_t invariantChars[4]={
    0xfffffbff, /* 00..1f but not 0a */
    0xffffffe5, /* 20..3f but not 21 23 24 */
    0x87fffffe, /* 40..5f but not 40 5b..5e */
    0x87fffffe  /* 60..7f but not 60 7b..7e */
};

/*
 * test unsigned types (or values known to be non-negative) for invariant characters,
 * tests ASCII-family character values
 */
#define UCHAR_IS_INVARIANT(c) (((c)<=0x7f) && (invariantChars[(c)>>5]&((uint32_t)1<<((c)&0x1f)))!=0)

/* test signed types for invariant characters, adds test for positive values */
#define SCHAR_IS_INVARIANT(c) ((0<=(c)) && UCHAR_IS_INVARIANT(c))

U_CAPI void U_EXPORT2
u_charsToUChars(const char *cs, UChar *us, int32_t length) {
    UChar u;
    uint8_t c;
    UBool onlyInvariantChars;

    /*
     * Allow the entire ASCII repertoire to be mapped _to_ Unicode.
     * For EBCDIC systems, this works for characters with codes from
     * codepages 37 and 1047 or compatible.
     */
    onlyInvariantChars=TRUE;
    while(length>0) {
        c=(uint8_t)(*cs++);
#if U_CHARSET_FAMILY==U_ASCII_FAMILY
        u=(UChar)c;
#elif U_CHARSET_FAMILY==U_EBCDIC_FAMILY
        u=(UChar)asciiFromEbcdic[c];
#else
#   error U_CHARSET_FAMILY is not valid
#endif
        if(u==0 && c!=0) {
            onlyInvariantChars=FALSE;
        }
        *us++=u;
        --length;
    }
    U_ASSERT(onlyInvariantChars); /* only invariant chars? */
}

U_CAPI void U_EXPORT2
u_UCharsToChars(const UChar *us, char *cs, int32_t length) {
    UChar u;
    UBool onlyInvariantChars;

    onlyInvariantChars=TRUE;
    while(length>0) {
        u=*us++;
        if(!UCHAR_IS_INVARIANT(u)) {
            onlyInvariantChars=FALSE;
            u=0;
        }
#if U_CHARSET_FAMILY==U_ASCII_FAMILY
        *cs++=(char)u;
#elif U_CHARSET_FAMILY==U_EBCDIC_FAMILY
        *cs++=(char)ebcdicFromAscii[u];
#else
#   error U_CHARSET_FAMILY is not valid
#endif
        --length;
    }
    U_ASSERT(onlyInvariantChars); /* only invariant chars? */
}

U_CAPI UBool U_EXPORT2
uprv_isInvariantString(const char *s, int32_t length) {
    uint8_t c;

    for(;;) {
        if(length<0) {
            /* NUL-terminated */
            c=(uint8_t)*s++;
            if(c==0) {
                break;
            }
        } else {
            /* count length */
            if(length==0) {
                break;
            }
            --length;
            c=(uint8_t)*s++;
            if(c==0) {
                continue; /* NUL is invariant */
            }
        }
        /* c!=0 now, one branch below checks c==0 for variant characters */

        /*
         * no assertions here because these functions are legitimately called
         * for strings with variant characters
         */
#if U_CHARSET_FAMILY==U_ASCII_FAMILY
        if(!UCHAR_IS_INVARIANT(c)) {
            return FALSE; /* found a variant char */
        }
#elif U_CHARSET_FAMILY==U_EBCDIC_FAMILY
        c=asciiFromEbcdic[c];
        if(c==0 || !UCHAR_IS_INVARIANT(c)) {
            return FALSE; /* found a variant char */
        }
#else
#   error U_CHARSET_FAMILY is not valid
#endif
    }
    return TRUE;
}

U_CAPI UBool U_EXPORT2
uprv_isInvariantUString(const UChar *s, int32_t length) {
    UChar c;

    for(;;) {
        if(length<0) {
            /* NUL-terminated */
            c=*s++;
            if(c==0) {
                break;
            }
        } else {
            /* count length */
            if(length==0) {
                break;
            }
            --length;
            c=*s++;
        }

        /*
         * no assertions here because these functions are legitimately called
         * for strings with variant characters
         */
        if(!UCHAR_IS_INVARIANT(c)) {
            return FALSE; /* found a variant char */
        }
    }
    return TRUE;
}

/* UDataSwapFn implementations used in udataswp.c ------- */

/* convert ASCII to EBCDIC and verify that all characters are invariant */
U_CFUNC int32_t
uprv_ebcdicFromAscii(const UDataSwapper *ds,
                     const void *inData, int32_t length, void *outData,
                     UErrorCode *pErrorCode) {
    const uint8_t *s;
    uint8_t *t;
    uint8_t c;

    int32_t count;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || length<0 || (length>0 && outData==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* setup and swapping */
    s=(const uint8_t *)inData;
    t=(uint8_t *)outData;
    count=length;
    while(count>0) {
        c=*s++;
        if(!UCHAR_IS_INVARIANT(c)) {
            udata_printError(ds, "uprv_ebcdicFromAscii() string[%d] contains a variant character in position %d\n",
                             length, length-count);
            *pErrorCode=U_INVALID_CHAR_FOUND;
            return 0;
        }
        *t++=ebcdicFromAscii[c];
        --count;
    }

    return length;
}

/* this function only checks and copies ASCII strings without conversion */
U_CFUNC int32_t
uprv_copyAscii(const UDataSwapper *ds,
               const void *inData, int32_t length, void *outData,
               UErrorCode *pErrorCode) {
    const uint8_t *s;
    uint8_t c;

    int32_t count;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || length<0 || (length>0 && outData==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* setup and checking */
    s=(const uint8_t *)inData;
    count=length;
    while(count>0) {
        c=*s++;
        if(!UCHAR_IS_INVARIANT(c)) {
            udata_printError(ds, "uprv_copyFromAscii() string[%d] contains a variant character in position %d\n",
                             length, length-count);
            *pErrorCode=U_INVALID_CHAR_FOUND;
            return 0;
        }
        --count;
    }

    if(length>0 && inData!=outData) {
        uprv_memcpy(outData, inData, length);
    }

    return length;
}

/* convert EBCDIC to ASCII and verify that all characters are invariant */
U_CFUNC int32_t
uprv_asciiFromEbcdic(const UDataSwapper *ds,
                     const void *inData, int32_t length, void *outData,
                     UErrorCode *pErrorCode) {
    const uint8_t *s;
    uint8_t *t;
    uint8_t c;

    int32_t count;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || length<0 ||  (length>0 && outData==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* setup and swapping */
    s=(const uint8_t *)inData;
    t=(uint8_t *)outData;
    count=length;
    while(count>0) {
        c=*s++;
        if(c!=0 && ((c=asciiFromEbcdic[c])==0 || !UCHAR_IS_INVARIANT(c))) {
            udata_printError(ds, "uprv_asciiFromEbcdic() string[%d] contains a variant character in position %d\n",
                             length, length-count);
            *pErrorCode=U_INVALID_CHAR_FOUND;
            return 0;
        }
        *t++=c;
        --count;
    }

    return length;
}

/* this function only checks and copies EBCDIC strings without conversion */
U_CFUNC int32_t
uprv_copyEbcdic(const UDataSwapper *ds,
                const void *inData, int32_t length, void *outData,
                UErrorCode *pErrorCode) {
    const uint8_t *s;
    uint8_t c;

    int32_t count;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || length<0 || (length>0 && outData==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* setup and checking */
    s=(const uint8_t *)inData;
    count=length;
    while(count>0) {
        c=*s++;
        if(c!=0 && ((c=asciiFromEbcdic[c])==0 || !UCHAR_IS_INVARIANT(c))) {
            udata_printError(ds, "uprv_copyEbcdic() string[%] contains a variant character in position %d\n",
                             length, length-count);
            *pErrorCode=U_INVALID_CHAR_FOUND;
            return 0;
        }
        --count;
    }

    if(length>0 && inData!=outData) {
        uprv_memcpy(outData, inData, length);
    }

    return length;
}

/* compare invariant strings; variant characters compare less than others and unlike each other */
U_CFUNC int32_t
uprv_compareInvAscii(const UDataSwapper *ds,
                     const char *outString, int32_t outLength,
                     const UChar *localString, int32_t localLength) {
    int32_t minLength;
    UChar32 c1, c2;
    uint8_t c;

    if(outString==NULL || outLength<-1 || localString==NULL || localLength<-1) {
        return 0;
    }

    if(outLength<0) {
        outLength=(int32_t)uprv_strlen(outString);
    }
    if(localLength<0) {
        localLength=u_strlen(localString);
    }

    minLength= outLength<localLength ? outLength : localLength;

    while(minLength>0) {
        c=(uint8_t)*outString++;
        if(UCHAR_IS_INVARIANT(c)) {
            c1=c;
        } else {
            c1=-1;
        }

        c2=*localString++;
        if(!UCHAR_IS_INVARIANT(c2)) {
            c1=-2;
        }

        if((c1-=c2)!=0) {
            return c1;
        }

        --minLength;
    }

    /* strings start with same prefix, compare lengths */
    return outLength-localLength;
}

U_CFUNC int32_t
uprv_compareInvEbcdic(const UDataSwapper *ds,
                      const char *outString, int32_t outLength,
                      const UChar *localString, int32_t localLength) {
    int32_t minLength;
    UChar32 c1, c2;
    uint8_t c;

    if(outString==NULL || outLength<-1 || localString==NULL || localLength<-1) {
        return 0;
    }

    if(outLength<0) {
        outLength=(int32_t)uprv_strlen(outString);
    }
    if(localLength<0) {
        localLength=u_strlen(localString);
    }

    minLength= outLength<localLength ? outLength : localLength;

    while(minLength>0) {
        c=(uint8_t)*outString++;
        if(c==0) {
            c1=0;
        } else if((c1=asciiFromEbcdic[c])!=0 && UCHAR_IS_INVARIANT(c1)) {
            /* c1 is set */
        } else {
            c1=-1;
        }

        c2=*localString++;
        if(!UCHAR_IS_INVARIANT(c2)) {
            c1=-2;
        }

        if((c1-=c2)!=0) {
            return c1;
        }

        --minLength;
    }

    /* strings start with same prefix, compare lengths */
    return outLength-localLength;
}

/* end of platform-specific implementation -------------- */

/* version handling --------------------------------------------------------- */

U_CAPI void U_EXPORT2
u_versionFromString(UVersionInfo versionArray, const char *versionString) {
    char *end;
    uint16_t part=0;

    if(versionArray==NULL) {
        return;
    }

    if(versionString!=NULL) {
        for(;;) {
            versionArray[part]=(uint8_t)uprv_strtoul(versionString, &end, 10);
            if(end==versionString || ++part==U_MAX_VERSION_LENGTH || *end!=U_VERSION_DELIMITER) {
                break;
            }
            versionString=end+1;
        }
    }

    while(part<U_MAX_VERSION_LENGTH) {
        versionArray[part++]=0;
    }
}

U_CAPI void U_EXPORT2
u_versionToString(UVersionInfo versionArray, char *versionString) {
    uint16_t count, part;
    uint8_t field;

    if(versionString==NULL) {
        return;
    }

    if(versionArray==NULL) {
        versionString[0]=0;
        return;
    }

    /* count how many fields need to be written */
    for(count=4; count>0 && versionArray[count-1]==0; --count) {
    }

    if(count <= 1) {
        count = 2;
    }

    /* write the first part */
    /* write the decimal field value */
    field=versionArray[0];
    if(field>=100) {
        *versionString++=(char)('0'+field/100);
        field%=100;
    }
    if(field>=10) {
        *versionString++=(char)('0'+field/10);
        field%=10;
    }
    *versionString++=(char)('0'+field);

    /* write the following parts */
    for(part=1; part<count; ++part) {
        /* write a dot first */
        *versionString++=U_VERSION_DELIMITER;

        /* write the decimal field value */
        field=versionArray[part];
        if(field>=100) {
            *versionString++=(char)('0'+field/100);
            field%=100;
        }
        if(field>=10) {
            *versionString++=(char)('0'+field/10);
            field%=10;
        }
        *versionString++=(char)('0'+field);
    }

    /* NUL-terminate */
    *versionString=0;
}

U_CAPI void U_EXPORT2
u_getVersion(UVersionInfo versionArray) {
    u_versionFromString(versionArray, U_ICU_VERSION);
}

/* u_errorName() ------------------------------------------------------------ */

static const char * const
_uErrorInfoName[U_ERROR_WARNING_LIMIT-U_ERROR_WARNING_START]={
    "U_USING_FALLBACK_WARNING",
    "U_USING_DEFAULT_WARNING",
    "U_SAFECLONE_ALLOCATED_WARNING",
    "U_STATE_OLD_WARNING",
    "U_STRING_NOT_TERMINATED_WARNING",
    "U_SORT_KEY_TOO_SHORT_WARNING",
    "U_AMBIGUOUS_ALIAS_WARNING",
    "U_DIFFERENT_UCA_VERSION"
};

static const char * const
_uTransErrorName[U_PARSE_ERROR_LIMIT - U_PARSE_ERROR_START]={
    "U_BAD_VARIABLE_DEFINITION",
    "U_MALFORMED_RULE",
    "U_MALFORMED_SET",
    "U_MALFORMED_SYMBOL_REFERENCE",
    "U_MALFORMED_UNICODE_ESCAPE",
    "U_MALFORMED_VARIABLE_DEFINITION",
    "U_MALFORMED_VARIABLE_REFERENCE",
    "U_MISMATCHED_SEGMENT_DELIMITERS",
    "U_MISPLACED_ANCHOR_START",
    "U_MISPLACED_CURSOR_OFFSET",
    "U_MISPLACED_QUANTIFIER",
    "U_MISSING_OPERATOR",
    "U_MISSING_SEGMENT_CLOSE",
    "U_MULTIPLE_ANTE_CONTEXTS",
    "U_MULTIPLE_CURSORS",
    "U_MULTIPLE_POST_CONTEXTS",
    "U_TRAILING_BACKSLASH",
    "U_UNDEFINED_SEGMENT_REFERENCE",
    "U_UNDEFINED_VARIABLE",
    "U_UNQUOTED_SPECIAL",
    "U_UNTERMINATED_QUOTE",
    "U_RULE_MASK_ERROR",
    "U_MISPLACED_COMPOUND_FILTER",
    "U_MULTIPLE_COMPOUND_FILTERS",
    "U_INVALID_RBT_SYNTAX",
    "U_INVALID_PROPERTY_PATTERN",
    "U_MALFORMED_PRAGMA",
    "U_UNCLOSED_SEGMENT",
    "U_ILLEGAL_CHAR_IN_SEGMENT",
    "U_VARIABLE_RANGE_EXHAUSTED",
    "U_VARIABLE_RANGE_OVERLAP",
    "U_ILLEGAL_CHARACTER",
    "U_INTERNAL_TRANSLITERATOR_ERROR",
    "U_INVALID_ID",
    "U_INVALID_FUNCTION"
};

static const char * const
_uErrorName[U_STANDARD_ERROR_LIMIT]={
    "U_ZERO_ERROR",

    "U_ILLEGAL_ARGUMENT_ERROR",
    "U_MISSING_RESOURCE_ERROR",
    "U_INVALID_FORMAT_ERROR",
    "U_FILE_ACCESS_ERROR",
    "U_INTERNAL_PROGRAM_ERROR",
    "U_MESSAGE_PARSE_ERROR",
    "U_MEMORY_ALLOCATION_ERROR",
    "U_INDEX_OUTOFBOUNDS_ERROR",
    "U_PARSE_ERROR",
    "U_INVALID_CHAR_FOUND",
    "U_TRUNCATED_CHAR_FOUND",
    "U_ILLEGAL_CHAR_FOUND",
    "U_INVALID_TABLE_FORMAT",
    "U_INVALID_TABLE_FILE",
    "U_BUFFER_OVERFLOW_ERROR",
    "U_UNSUPPORTED_ERROR",
    "U_RESOURCE_TYPE_MISMATCH",
    "U_ILLEGAL_ESCAPE_SEQUENCE",
    "U_UNSUPPORTED_ESCAPE_SEQUENCE",
    "U_NO_SPACE_AVAILABLE",
    "U_CE_NOT_FOUND_ERROR",
    "U_PRIMARY_TOO_LONG_ERROR",
    "U_STATE_TOO_OLD_ERROR",
    "U_TOO_MANY_ALIASES_ERROR",
    "U_ENUM_OUT_OF_SYNC_ERROR",
    "U_INVARIANT_CONVERSION_ERROR",
    "U_INVALID_STATE_ERROR"
};
static const char * const
_uFmtErrorName[U_FMT_PARSE_ERROR_LIMIT - U_FMT_PARSE_ERROR_START] = {
    "U_UNEXPECTED_TOKEN",
    "U_MULTIPLE_DECIMAL_SEPARATORS",
    "U_MULTIPLE_EXPONENTIAL_SYMBOLS",
    "U_MALFORMED_EXPONENTIAL_PATTERN",
    "U_MULTIPLE_PERCENT_SYMBOLS",
    "U_MULTIPLE_PERMILL_SYMBOLS",
    "U_MULTIPLE_PAD_SPECIFIERS",
    "U_PATTERN_SYNTAX_ERROR",
    "U_ILLEGAL_PAD_POSITION",
    "U_UNMATCHED_BRACES",
    "U_UNSUPPORTED_PROPERTY",
    "U_UNSUPPORTED_ATTRIBUTE"
};

static const char * const
_uBrkErrorName[U_BRK_ERROR_LIMIT - U_BRK_ERROR_START] = {
    "U_BRK_ERROR_START",
    "U_BRK_INTERNAL_ERROR",
    "U_BRK_HEX_DIGITS_EXPECTED",
    "U_BRK_SEMICOLON_EXPECTED",
    "U_BRK_RULE_SYNTAX",
    "U_BRK_UNCLOSED_SET",
    "U_BRK_ASSIGN_ERROR",
    "U_BRK_VARIABLE_REDFINITION",
    "U_BRK_MISMATCHED_PAREN",
    "U_BRK_NEW_LINE_IN_QUOTED_STRING",
    "U_BRK_UNDEFINED_VARIABLE",
    "U_BRK_INIT_ERROR",
    "U_BRK_RULE_EMPTY_SET",
    "U_BRK_UNRECOGNIZED_OPTION",
    "U_BRK_MALFORMED_RULE_TAG"
};

static const char * const
_uRegexErrorName[U_REGEX_ERROR_LIMIT - U_REGEX_ERROR_START] = {
    "U_REGEX_ERROR_START",
    "U_REGEX_INTERNAL_ERROR",
    "U_REGEX_RULE_SYNTAX",
    "U_REGEX_INVALID_STATE",
    "U_REGEX_BAD_ESCAPE_SEQUENCE",
    "U_REGEX_PROPERTY_SYNTAX",
    "U_REGEX_UNIMPLEMENTED",
    "U_REGEX_MISMATCHED_PAREN",
    "U_REGEX_NUMBER_TOO_BIG",
    "U_REGEX_BAD_INTERVAL",
    "U_REGEX_MAX_LT_MIN",
    "U_REGEX_INVALID_BACK_REF",
    "U_REGEX_INVALID_FLAG",
    "U_REGEX_LOOK_BEHIND_LIMIT",
    "U_REGEX_SET_CONTAINS_STRING"
};

static const char * const
_uIDNAErrorName[U_IDNA_ERROR_LIMIT - U_IDNA_ERROR_START] = {
      "U_IDNA_ERROR_START",
      "U_IDNA_PROHIBITED_ERROR",
      "U_IDNA_UNASSIGNED_ERROR",
      "U_IDNA_CHECK_BIDI_ERROR",
      "U_IDNA_STD3_ASCII_RULES_ERROR",
      "U_IDNA_ACE_PREFIX_ERROR",
      "U_IDNA_VERIFICATION_ERROR",
      "U_IDNA_LABEL_TOO_LONG_ERROR"
};

U_CAPI const char * U_EXPORT2
u_errorName(UErrorCode code) {
    if(U_ZERO_ERROR <= code && code < U_STANDARD_ERROR_LIMIT) {
        return _uErrorName[code];
    } else if(U_ERROR_WARNING_START <= code && code < U_ERROR_WARNING_LIMIT) {
        return _uErrorInfoName[code - U_ERROR_WARNING_START];
    } else if(U_PARSE_ERROR_START <= code && code < U_PARSE_ERROR_LIMIT){
        return _uTransErrorName[code - U_PARSE_ERROR_START];
    } else if(U_FMT_PARSE_ERROR_START <= code && code < U_FMT_PARSE_ERROR_LIMIT){
        return _uFmtErrorName[code - U_FMT_PARSE_ERROR_START];
    } else if (U_BRK_ERROR_START <= code  && code < U_BRK_ERROR_LIMIT){
        return _uBrkErrorName[code - U_BRK_ERROR_START];
    } else if (U_REGEX_ERROR_START <= code && code < U_REGEX_ERROR_LIMIT) {
        return _uRegexErrorName[code - U_REGEX_ERROR_START];
    } else if( U_IDNA_ERROR_START <= code && code <= U_IDNA_ERROR_LIMIT) {
        return _uIDNAErrorName[code - U_IDNA_ERROR_START];
    } else {
        return "[BOGUS UErrorCode]";
    }
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
