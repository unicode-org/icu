/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1998     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
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
*******************************************************************************
*/

#include <time.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <locale.h>

#include "utypes.h"

#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"

#ifdef AS400
#include <float.h>
#endif

#ifdef XP_MAC
#include "Files.h"
#include "IntlResources.h"
#include "Script.h"
#endif


#ifdef WIN32
#include "locmap.h"
#include <wtypes.h>
#include <winnls.h>
#endif

/* We return QNAN rather than SNAN*/
#define NAN_TOP ((int16_t)0x7FF8)
#define INF_TOP ((int16_t)0x7FF0)

#define SIGN 0x80000000L

static char DEFAULT_CONVERTER_NAME[60] = "";
static char tempString[10] = "";

/* statics */
static bool_t fgNaNInitialized = FALSE;
static double fgNan;
static bool_t fgInfInitialized = FALSE;
static double fgInf;

/* protos */
static char* u_topNBytesOfDouble(double* d, int n);
static char* u_bottomNBytesOfDouble(double* d, int n);


/*---------------------------------------------------------------------------
  Platform utilities
  Our general strategy is to assume we're on a POSIX platform.  Platforms which
  are non-POSIX must declare themselves so.  The default POSIX implementation
  will sometimes work for non-POSIX platforms as well (e.g., the NaN-related
  functions).
  ---------------------------------------------------------------------------*/

/* Assume POSIX, and modify as necessary below*/
#define POSIX
#if defined(_WIN32) || defined(XP_MAC) || defined(AS400) || defined(OS2)
#undef POSIX
#endif

/*---------------------------------------------------------------------------
  Universal Implementations
  These are designed to work on all platforms.  Try these, and if they don't
  work on your platform, then special case your platform with new
  implementations.
  ---------------------------------------------------------------------------*/

/* Get UTC (GMT) time measured in seconds since 0:00 on 1/1/70.*/
int32_t
icu_getUTCtime()
{
#ifdef XP_MAC
  time_t t, t1, t2;
  struct tm tmrec;
  
  memset( &tmrec, 0, sizeof(tmrec) );
  tmrec.tm_year = 70;
  tmrec.tm_mon = 0;
  tmrec.tm_mday = 1;
  t1 = mktime(&tmrec);    /* seconds of 1/1/1970*/
  
  time(&t);
  memcpy( &tmrec, gmtime(&t), sizeof(tmrec) );
  t2 = mktime(&tmrec);    /* seconds of current GMT*/
  return t2 - t1;         /* GMT (or UTC) in seconds since 1970*/
#else
  time_t epochtime;
  time(&epochtime);
  return epochtime;
#endif
}

bool_t
icu_isBigEndian()
{
  union
  {
    int16_t     fShort;
    int8_t      fChars[2];
  } testPattern;
    
  testPattern.fShort = 0x1234;
  return (testPattern.fChars[0] == 0x12);
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

bool_t 
icu_isNaN(double number)
{
#ifdef IEEE_754
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

  return ((highBits & 0x7FF00000L) == 0x7FF00000L) && 
    (((highBits & 0x000FFFFFL) != 0) || (lowBits != 0));
#else
  /* If your platform doesn't support IEEE 754 but *does* have an NaN value,*/
  /* you'll need to replace this default implementation with what's correct*/
  /* for your platform.*/
  return number != number;
#endif
}

bool_t
icu_isInfinite(double number)
{
#ifdef IEEE_754
  /* We know the top bit is the sign bit, so we mask that off in a copy of */
  /* the number and compare against infinity. [LIU]*/
  /* The following approach doesn't work for some reason, so we go ahead and */
  /* scrutinize the pattern itself. */
  /*  double a = number; */
  /*  *(int8_t*)u_topNBytesOfDouble(&a, 1) &= 0x7F;*/
  /*  return a == icu_getInfinity();*/
  /* Instead, We want to see either:*/
  
  /*   7FF0 0000 0000 0000*/
  /*   FFF0 0000 0000 0000*/
  
  uint32_t highBits = *(uint32_t*)u_topNBytesOfDouble(&number, 
                              sizeof(uint32_t));
  uint32_t lowBits  = *(uint32_t*)u_bottomNBytesOfDouble(&number, 
                             sizeof(uint32_t));
  
  return ((highBits  & ~SIGN) == 0x7FF00000L) && (lowBits == 0x00000000L);
#else
  /* If your platform doesn't support IEEE 754 but *does* have an infinity*/
  /* value, you'll need to replace this default implementation with what's*/
  /* correct for your platform.*/
  return number == (2.0 * number);
#endif
}

bool_t   
icu_isPositiveInfinity(double number)
{
#ifdef IEEE_754
  return (number > 0 && icu_isInfinite(number));
#else
  return icu_isInfinite(number);
#endif
}

bool_t   
icu_isNegativeInfinity(double number)
{
#ifdef IEEE_754
  return (number < 0 && icu_isInfinite(number));
#else
  return icu_isInfinite(number);
#endif
}

double 
icu_getNaN()
{
#ifdef IEEE_754  
  if( ! fgNaNInitialized) {
    umtx_lock(NULL);
    if( ! fgNaNInitialized) {
      int i;
      int8_t* p = (int8_t*)&fgNan;
      for(i = 0; i < sizeof(double); ++i) 
    *p++ = 0;
      *(int16_t*)u_topNBytesOfDouble(&fgNan, sizeof(NAN_TOP)) = NAN_TOP;
      fgNaNInitialized = TRUE;
    }
    umtx_unlock(NULL);
  }
  return fgNan;
#else
  /* If your platform doesn't support IEEE 754 but *does* have an NaN value,*/
  /* you'll need to replace this default implementation with what's correct*/
  /* for your platform.*/
  return 0.0;
#endif
}

double 
icu_getInfinity()
{
#ifdef IEEE_754  
  if (!fgInfInitialized)
    {
      int i;
      int8_t* p = (int8_t*)&fgInf;
      for(i = 0; i < sizeof(double); ++i) 
    *p++ = 0;
      *(int16_t*)u_topNBytesOfDouble(&fgInf, sizeof(INF_TOP)) = INF_TOP;
      fgInfInitialized = TRUE;
    }
  return fgInf;
#else
  /* If your platform doesn't support IEEE 754 but *does* have an infinity*/
  /* value, you'll need to replace this default implementation with what's*/
  /* correct for your platform.*/
  return 0.0;
#endif
}

double 
icu_floor(double x)
{
  return floor(x);
}

double 
icu_ceil(double x)
{
  return ceil(x);
}

double 
icu_fabs(double x)
{
  return fabs(x);
}

double 
icu_modf(double x, double* y)
{
  return modf(x, y);
}

double 
icu_fmod(double x, double y)
{
  return fmod(x, y);
}

double
icu_pow10(int32_t x)
{
#ifdef XP_MAC
  return pow(10.0, (double)x);
#else
  return pow(10.0, x);
#endif
}

double 
icu_IEEEremainder(double x, double p)
{
#ifdef IEEE_754
  int32_t hx, hp;
  uint32_t sx, lx, lp;
  double p_half;
  
  hx = *(int32_t*)u_topNBytesOfDouble(&x, sizeof(int32_t));
  lx = *(uint32_t*)u_bottomNBytesOfDouble(&x, sizeof(uint32_t));
  
  hp = *(int32_t*)u_topNBytesOfDouble(&p, sizeof(int32_t));
  lp = *(uint32_t*)u_bottomNBytesOfDouble(&p, sizeof(uint32_t));
  
  sx = hx & SIGN;
  
  hp &= 0x7fffffff;
  hx &= 0x7fffffff;
  
  /* purge off exception values */
  if((hp|lp) == 0) 
    return (x*p) / (x*p);     /* p = 0 */
  if((hx >= 0x7ff00000)||        /* x not finite */
     ((hp>=0x7ff00000) &&    /* p is NaN */
      (((hp-0x7ff00000)|lp) != 0)))
    return (x*p) / (x*p);
  
  
  if(hp <= 0x7fdfffff) 
    x = icu_fmod(x, p + p);    /* now x < 2p */
  if(((hx-hp)|(lx-lp)) == 0) 
    return 0.0 * x;
  x  = icu_fabs(x);
  p  = icu_fabs(p);
  if (hp < 0x00200000) {
    if(x + x > p) {
      x -= p;
      if(x + x >= p) 
    x -= p;
    }
  } 
  else {
    p_half = 0.5 * p;
    if(x > p_half) {
      x -= p;
      if(x >= p_half) 
    x -= p;
    }
  }
  
  *(int32_t*)u_topNBytesOfDouble(&x, sizeof(int32_t)) ^= sx;
  
  return x;
#else
  /* {sfb} need to fix this*/
  return icu_fmod(x, p);
#endif
}

double 
icu_fmax(double x, double y)
{
#ifdef IEEE_754
  int32_t lowBits;
  
  /* first handle NaN*/
  if(icu_isNaN(x) || icu_isNaN(y))
    return icu_getNaN();
  
  /* check for -0 and 0*/
  lowBits = *(uint32_t*) u_bottomNBytesOfDouble(&x, sizeof(uint32_t));
  if(x == 0.0 && y == 0.0 && (lowBits & SIGN))
    return y; 
  
  return (x > y ? x : y);
#else
  /* {sfb} fix this*/
  return x;
#endif
}

int32_t 
icu_max(int32_t x, int32_t y)
{
  return (x > y ? x : y);
}

double 
icu_fmin(double x, double y)
{
#ifdef IEEE_754
  int32_t lowBits;

  /* first handle NaN*/
  if(icu_isNaN(x) || icu_isNaN(y))
    return icu_getNaN();
  
  /* check for -0 and 0*/
  lowBits = *(uint32_t*) u_bottomNBytesOfDouble(&y, sizeof(uint32_t));
  if(x == 0.0 && y == 0.0 && (lowBits & SIGN))
    return y; 
  
  return (x > y ? y : x);
#else
  /* {sfb} fix this*/
  return x;
#endif
}

int32_t 
icu_min(int32_t x, int32_t y)
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
double 
icu_trunc(double d)
{
#ifdef IEEE_754

  int32_t lowBits;
  
  /* handle error cases*/
  if(icu_isNaN(d))        return icu_getNaN();
  if(icu_isInfinite(d))        return icu_getInfinity();
  
  lowBits = *(uint32_t*) u_bottomNBytesOfDouble(&d, sizeof(uint32_t));
  if( (d == 0.0 && (lowBits & SIGN)) || d < 0)
    return ceil(d);
  else
    return floor(d);
#else
  return d >= 0 ? floor(d) : ceil(d);
#endif
}

void        
icu_longBitsFromDouble(double d, int32_t *hi, uint32_t *lo)
{
  *hi = *(int32_t*)u_topNBytesOfDouble(&d, sizeof(int32_t));
  *lo = *(uint32_t*)u_bottomNBytesOfDouble(&d, sizeof(uint32_t));
}
    

/**
 * Return the floor of the log base 10 of a given double.
 * This method compensates for inaccuracies which arise naturally when
 * computing logs, and always give the correct value.  The parameter
 * must be positive and finite.
 * (Thanks to Alan Liu for supplying this function.)
 */
int16_t 
icu_log10(double d)
{
  /* The reason this routine is needed is that simply taking the*/
  /* log and dividing by log10 yields a result which may be off*/
  /* by 1 due to rounding errors.  For example, the naive log10*/
  /* of 1.0e300 taken this way is 299, rather than 300.*/
  double log10 = log(d) / log(10.0);
  int16_t ilog10 = (int16_t)floor(log10);
  
  /* Positive logs could be too small, e.g. 0.99 instead of 1.0*/
  if (log10 > 0 && d >= pow(10.0, ilog10 + 1))
    ++ilog10;
  
  /* Negative logs could be too big, e.g. -0.99 instead of -1.0*/
  else if (log10 < 0 && d < pow(10.0, ilog10))
    --ilog10;
  
  return ilog10;
}

int32_t 
icu_digitsAfterDecimal(double x)
{
  char buffer[20];
  int16_t numDigits;
  char *p;
  int16_t ptPos, exponent;

  /* negative numbers throw off the calculations*/
  x = fabs(x);
  
  /* cheat and use the string-format routine to get a string representation*/
  /* (it handles mathematical inaccuracy better than we can), then find out */
  /* many characters are to the right of the decimal point */
  sprintf(buffer, "%.9g", x);
  p = icu_strchr(buffer, '.');
  if (p == 0)
    return 0;
  
  ptPos = p - buffer;
  numDigits = strlen(buffer) - ptPos - 1;
  
  /* if the number's string representation is in scientific notation, find */
  /* the exponent and take it into account*/
  exponent = 0;
  p = icu_strchr(buffer, 'e');
  if (p != 0) {
    int16_t expPos = p - buffer;
    numDigits -= strlen(buffer) - expPos;
    exponent = atoi(p + 1);
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
  return numDigits;
}

/*---------------------------------------------------------------------------
  Platform-specific Implementations
  Try these, and if they don't work on your platform, then special case your
  platform with new implementations.
  ---------------------------------------------------------------------------*/

/* Time zone utilities */
void 
icu_tzset()
{
#ifdef POSIX
  tzset();
#endif

#if defined(AS400) || defined(XP_MAC)
  /* no initialization*/
#endif

#if defined(WIN32) || defined(OS2)
  _tzset();
#endif
}

int32_t 
icu_timezone()
{
#ifdef POSIX
  return timezone;
#endif

#if defined(AS400) || defined(XP_MAC)
  time_t t, t1, t2;
  struct tm tmrec;
  bool_t dst_checked;
  
  time(&t);
  memcpy( &tmrec, localtime(&t), sizeof(tmrec) );
  dst_checked = (tmrec.tm_isdst != 0); /* daylight savings time is checked*/
  t1 = mktime(&tmrec);                 /* local time in seconds*/
  memcpy( &tmrec, gmtime(&t), sizeof(tmrec) );
  t2 = mktime(&tmrec);                 /* GMT (or UTC) in seconds*/
  int32_t tdiff = t2 - t1;
  /* imitate NT behaviour, which returns same timezone offset to GMT for 
     winter and summer*/
  if (dst_checked) tdiff += 3600;
  return tdiff;
#endif

#if defined(WIN32) || defined(OS2)
  return _timezone;
#endif
}

char* 
icu_tzname(int index)
{
#ifdef POSIX
  return tzname[index];
#endif

#if defined(AS400) || defined(XP_MAC)
  return "";
#endif

#if defined(WIN32) || defined(OS2)
  return _tzname[index];
#endif
}

const char* 
icu_getDefaultDataDirectory()
{
#ifdef POSIX
  static char *PATH = 0;
  if(PATH == 0) {
    umtx_lock(NULL);
    if(PATH == 0) {
      /* Normally, the locale and converter data will be installed in
         the same tree as the ICU libraries - typically /usr/local/lib
         for the libraries, /usr/local/include for the headers, and
         /usr/local/share for the binary data.  However, the directory
         where the ICU looks for the binary data can be overridden by
         setting the environment variable ICU_DATA */
      char *dir = getenv("ICU_DATA");

      /* If the environment variable is set, use it */
      if(dir != 0) {
        PATH = dir;
      }
      /* Otherwise, use the compiled in default */
      else {
        PATH = ICU_DATA_DIR;
      }
    }
    umtx_unlock(NULL);
  }
  return PATH;
#endif

#ifdef AS400
  return "/icu/data/";
#endif

#ifdef XP_MAC
  static char path[256];
  char* mainDir;
  char* relPath = ":icu:data:";
  
  Str255 volName;
  int16_t volNum;
  OSErr err = GetVol( volName, &volNum );
  if (err != noErr) 
    volName[0] = 0;
  mainDir = (char*) &(volName[1]);
  mainDir[volName[0]] = 0;
  int32_t lenMainDir = strlen( mainDir );
  int32_t lenRelPath = strlen( relPath );
  if (sizeof(path) < lenMainDir + lenRelPath + 2) { 
    path[0] = 0; 
    return path; 
  }
  icu_strcpy( path, mainDir );
  icu_strcat( path, relPath );
  
  return path;
#endif

#ifdef WIN32
  return "\\icu\\data\\";
#endif

#ifdef OS2
  char * dpath;
  dpath = getenv("ICUPATH");
  if (!dpath || !*dpath)
      return "\\icu\\data\\";
  return dpath;
#endif


}

/* Macintosh-specific locale information */
#ifdef XP_MAC

struct mac_lc_rec {
  int32_t script;
  int32_t region;
  int32_t lang;
  int32_t date_region;
  char* posixID;
};
/* To do: This will be updated with a newer version from www.unicode.org web
   page when it's available.*/
#define MAC_LC_MAGIC_NUMBER -5
#define MAC_LC_INIT_NUMBER -9

mac_lc_rec mac_lc_recs[] = {
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
  MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 20, "EL_GR", 
  /* Greece*/
  MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 21, "is_IS", 
  /* Iceland ===*/
  /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 22, "", 
    // Malta ===*/
  /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 23, "", 
    // Cyprus ===*/
  MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 24, "tr_TR", 
  /* Turkey ===*/
  MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 25, "sh_YU", 
  /* Croatian system for Yugoslavia*/
  /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 33, "", 
    // Hindi system for India*/
  /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 34, "", 
    // Pakistan*/
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
  /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 46, "", 
    // Lapland  [Ask Rich for the data. HS]*/
  /*MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, MAC_LC_MAGIC_NUMBER, 47, "", 
    // Faeroe Islands*/
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

const char* 
icu_getDefaultLocaleID()
{
#ifdef POSIX
  char* posixID = getenv("LC_ALL");
  if (posixID == 0) posixID = getenv("LANG");
  if (posixID == 0) posixID = setlocale(LC_ALL, NULL);
  if (icu_strcmp("C", posixID) == 0) posixID = "en_US";
  return posixID;
#endif

#ifdef AS400
  /* TBD */
  return "";
#endif

#ifdef XP_MAC
  int32_t script = MAC_LC_INIT_NUMBER; 
  /* = IntlScript(); or GetScriptManagerVariable(smSysScript);*/
  int32_t region = MAC_LC_INIT_NUMBER; 
  /* = GetScriptManagerVariable(smRegionCode);*/
  int32_t lang = MAC_LC_INIT_NUMBER;   
  /* = GetScriptManagerVariable(smScriptLang);*/
  int32_t date_region = MAC_LC_INIT_NUMBER;
  char* posixID = 0;
  Intl1Hndl ih;
  
  ih = (Intl1Hndl) GetIntlResource(1);
  if (ih) date_region = ((uint16_t)(*ih)->intl1Vers) >> 8;
  
  int32_t count = sizeof(mac_lc_recs) / sizeof(mac_lc_rec);
  for (int32_t i = 0; i < count; i++) {
    if ( ((mac_lc_recs[i].script == MAC_LC_MAGIC_NUMBER)      
      || (mac_lc_recs[i].script == script))
     && ((mac_lc_recs[i].region == MAC_LC_MAGIC_NUMBER)
         || (mac_lc_recs[i].region == region))
     && ((mac_lc_recs[i].lang == MAC_LC_MAGIC_NUMBER)
         || (mac_lc_recs[i].lang == lang))
     && ((mac_lc_recs[i].date_region == MAC_LC_MAGIC_NUMBER) 
         || (mac_lc_recs[i].date_region == date_region))
     ) {
      posixID = mac_lc_recs[i].posixID;
      break;
    }
  }
  
  return posixID;
#endif

#ifdef WIN32
  LCID id = GetThreadLocale();
  return T_convertToPosix(id);
#endif

#ifdef OS2
    char * locID;

    locID = getenv("LC_ALL");
    if (!locID || !*locID)
        locID = getenv("LANG");
    if (!locID || !*locID) {
        locID = "C";
    }
    if (!stricmp(locID, "c") || !stricmp(locID, "posix") ||
        !stricmp(locID, "univ"))
        locID = "en_US";
    return locID;
#endif

}

/* end of platform-specific implementation */

double 
icu_nextDouble(double d, bool_t next)
{
#ifdef IEEE_754
  int32_t highBits;
  uint32_t lowBits;
  int32_t highMagnitude;
  uint32_t lowMagnitude;
  double result;
  uint32_t *highResult, *lowResult;
  uint32_t signBit;

  /* filter out NaN's */
  if (icu_isNaN(d)) {
    return d;
  }
  
  /* zero's are also a special case */
  if (d == 0.0) {
    double smallestPositiveDouble = 0.0;
    uint32_t *lowBits = 
      (uint32_t *)u_bottomNBytesOfDouble(&smallestPositiveDouble, 
                     sizeof(uint32_t));
    
    *lowBits = 1;
    
    if (next) {
      return smallestPositiveDouble;
    } else {
      return -smallestPositiveDouble;
    }
  }
  
  /* if we get here, d is a nonzero value */
  
  /* hold all bits for later use */
  highBits = *(int32_t*)u_topNBytesOfDouble(&d, sizeof(uint32_t));
  lowBits = *(uint32_t*)u_bottomNBytesOfDouble(&d, sizeof(uint32_t));
  
  /* strip off the sign bit */
  highMagnitude = highBits & ~SIGN;
  lowMagnitude = lowBits;
  
  /* if next double away from zero, increase magnitude */
  if ((highBits >= 0) == next) {
    if (highMagnitude != 0x7FF00000L || lowMagnitude != 0x00000000L) {
      lowMagnitude += 1;
      if (lowMagnitude == 0) {
    highMagnitude += 1;
      }
    }
  }
  /* else decrease magnitude */
  else {
    lowMagnitude -= 1;
    if (lowMagnitude > lowBits) {
      highMagnitude -= 1;
    }
  }
  
  /* construct result and return */
  signBit = highBits & SIGN;
  highResult = (uint32_t *)u_topNBytesOfDouble(&result, sizeof(uint32_t));
  lowResult  = (uint32_t *)u_bottomNBytesOfDouble(&result, sizeof(uint32_t));
  
  *highResult = signBit | highMagnitude;
  *lowResult  = lowMagnitude;
  return result;
#else
  /* This is the portable implementation...*/
  /* a small coefficient within the precision of the mantissa*/
  static const double smallValue = 1e-10;  
  double epsilon = ((d<0)?-d:d) * smallValue; /* first approximation*/
  if (epsilon == 0) epsilon = smallValue; /* for very small d's*/
  if (!next) epsilon = -epsilon;
  double last_eps = epsilon * 2.0;
  /* avoid higher precision possibly used for temporay values*/
  double sum = d + epsilon; 
  while ((sum != d) && (epsilon != last_eps)) {
    last_eps = epsilon;
    epsilon /= 2.0;
    sum = d + epsilon;
  }
  return d + last_eps;
#endif
}

static char*
u_topNBytesOfDouble(double* d, int n)
{
  return icu_isBigEndian() ? (char*)d : (char*)(d + 1) - n;
}

static char* u_bottomNBytesOfDouble(double* d, int n)
{
  return icu_isBigEndian() ? (char*)(d + 1) - n : (char*)d;
}

const char* icu_getDefaultCodepage()
{
  /*Lazy evaluates DEFAULT_CONVERTER_NAME*/
  if (DEFAULT_CONVERTER_NAME[0]) return DEFAULT_CONVERTER_NAME;
#if defined(AS400)
  /* Currently TBD 
     in the future should use thread specific CP
  */
#elif defined(OS390)
  icu_strcpy(DEFAULT_CONVERTER_NAME, "ibm-1047");
#elif defined(XP_MAC)
  /* TBD */
#elif defined(WIN32) 
  icu_strcpy(DEFAULT_CONVERTER_NAME, "cp");
  icu_strcat(DEFAULT_CONVERTER_NAME, _itoa(GetACP(), tempString, 10));
#elif defined(POSIX)
  icu_strcpy(DEFAULT_CONVERTER_NAME, "LATIN_1");

#else 
  icu_strcpy(DEFAULT_CONVERTER_NAME, "LATIN_1");
#endif
  
  return DEFAULT_CONVERTER_NAME;
}
