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
*   11/15/99    helena      Integrated S/390 IEEE support.
*******************************************************************************
*/

/* include system headers */
#ifdef WIN32
#   include <wtypes.h>
#   include <winnls.h>
#   include "locmap.h"
#elif defined(OS2)
#   define INCL_DOSMISC
#   define INCL_DOSERRORS
#   include <os2.h>
#elif defined(OS400)
#   include <float.h>
#elif defined(XP_MAC)
#   include <Files.h>
#   include <IntlResources.h>
#   include <Script.h>
#elif defined(AIX)
#   include <sys/ldr.h>
#elif defined(SOLARIS) || defined(LINUX)
#   include <dlfcn.h>
#elif defined(HPUX)
#   include <dl.h>
#endif

/* include standard headers */
#include <time.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <locale.h>

/* include ICU headers */
#include "utypes.h"
#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"

/* floating point implementations ------------------------------------------- */

/* We return QNAN rather than SNAN*/
#ifdef IEEE_754
#define NAN_TOP ((int16_t)0x7FF8)
#define INF_TOP ((int16_t)0x7FF0)
#else
#ifdef OS390
#define NAN_TOP ((int16_t)0x7F08)
#define INF_TOP ((int16_t)0x3F00)
#endif
#endif

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
#if defined(_WIN32) || defined(XP_MAC) || defined(OS400) || defined(OS2)
#   undef POSIX
#else
#   define POSIX
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
#ifdef OS390
  uint32_t highBits = *(uint32_t*)u_topNBytesOfDouble(&number,
                            sizeof(uint32_t));
  uint32_t lowBits  = *(uint32_t*)u_bottomNBytesOfDouble(&number,
                           sizeof(uint32_t));

  return ((highBits & 0x7F000000L) == 0x7F000000L) &&
    (((highBits & 0x000FFFFFL) != 0) || (lowBits != 0));
#endif
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
#ifdef OS390
  uint32_t highBits = *(uint32_t*)u_topNBytesOfDouble(&number,
                            sizeof(uint32_t));
  return((highBits & SIGN) && icu_isInfinite(number));
#endif
  return icu_isInfinite(number);
#endif
}

double 
icu_getNaN()
{
#if defined(IEEE_754) || defined(OS390)
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
#ifdef OS390
/* this should work for all flt point w/o NaN and Infpecial cases */
  return (x > y ? x : y);
#else
  return x;
#endif
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
#ifdef OS390
/* this should work for all flt point w/o NaN and Inf special cases */

  return (x > y ? y : x);
#else
  return x;
#endif
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

#if defined(OS400) || defined(XP_MAC)
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
#ifdef OS390
  return _timezone;
#else
  return timezone;
#endif
#endif

#if defined(OS400) || defined(XP_MAC)
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

#if defined(OS400) || defined(XP_MAC)
  return "";
#endif

#if defined(WIN32) || defined(OS2)
  return _tzname[index];
#endif
}

/* Get and set the ICU data directory --------------------------------------- */

static bool_t
gHaveDataDirectory=FALSE;

static char
gDataDirectory[1024];

/*
 * Here, we use a mutex to make sure that setting the data directory
 * is thread-safe; however, reading it after calling u_getDataDirectory()
 * may still occur while it is (re)set and is therefore not thread-safe.
 * The best is to not call it after the initialization.
 */
U_CAPI void U_EXPORT2
u_setDataDirectory(const char *directory) {
    if(directory!=NULL) {
        int length=icu_strlen(directory);

        if(length<sizeof(gDataDirectory)-1) {
            umtx_lock(NULL);
            if(length==0) {
                *gDataDirectory=0;
            } else {
                icu_memcpy(gDataDirectory, directory, length);

                /* terminate the directory with a separator (/ or \) */
                if(gDataDirectory[length-1]!=U_FILE_SEP_CHAR) {
                    gDataDirectory[length++]=U_FILE_SEP_CHAR;
                }

                /* zero-terminate it */
                gDataDirectory[length]=0;
            }
            gHaveDataDirectory=TRUE;
            umtx_unlock(NULL);
        }
    }
}

/*
 * get the system drive or volume path
 * (Windows: e.g. "C:" or "D:")
 * do not terminate with a U_FILE_SEP_CHAR separator
 * return the length of the path, or 0 if none
 */
static int
getSystemPath(char *path, int size) {
#   if defined(XP_MAC)
        int16_t volNum;

        path[0]=0;
        OSErr err=GetVol(path, &volNum);
        if(err!=noErr) {
            int length=(uint8_t)volName[0];
            if(length>0) {
                /* convert the Pascal string to a C string */
                icu_memmove(path, path+1, length);
                path[length]=0;
            }
            return length;
        }
#   elif defined(WIN32)
        if(GetSystemDirectory(path, size)>=2 && path[1]==':') {
            /* remove the rest of the path - "\\winnt\\system32" or similar */
            path[2]=0;
            return 2;
        }
#   elif defined(OS2)
        APIRET rc;
        ULONG bootDrive=0;  /* 1=A, 2=B, 3=C, ... */
    
        rc=DosQuerySysInfo(QSV_BOOT_DRIVE, QSV_BOOT_DRIVE, (PVOID)&bootDrive, sizeof(ULONG));
        if(rc==NO_ERROR) {
            /* convert the numeric boot drive to a string */
            path[0]='A'+bootDrive-1;
            path[1]=':';
            path[2]=0;
            return 2;
        }
#   endif
    return 0;
}

/*
 * get the path to the ICU dynamic library
 * do not terminate with a U_FILE_SEP_CHAR separator
 * return the length of the path, or 0 if none
 */
static int
getLibraryPath(char *path, int size) {
#   ifdef WIN32
        HINSTANCE mod=GetModuleHandle("icuuc.dll");
        if(mod!=NULL) {
            if(GetModuleFileName(mod, path, size)>0) {
                /* remove the basename and the last file separator */
                char *lastSep=icu_strrchr(path, U_FILE_SEP_CHAR);
                if(lastSep!=NULL) {
                    *lastSep=0;
                    return lastSep-path;
                }
            }
        }
#   elif defined(OS2)
#   elif defined(OS390)
#   elif defined(OS400)
#   elif defined(XP_MAC)
#   elif defined(SOLARIS)
        void *handle=dlopen("libicuuc.so", RTLD_LAZY);
        if(handle!=NULL) {
            Link_map *p=NULL;
            char *s;
            int rc, length=0;

            /* get the Link_map list */
            rc=dlinfo(handle, RTLD_DI_LINKMAP, (void *)&p);
            if(rc>=0) {
                /* search for the list item for the library itself */
                while(p!=NULL) {
                    s=icu_strstr(p->l_name, "libicuuc.so");
                    if(s!=NULL) {
                        if(s>p->l_name) {
                            /* copy the path, without the basename and the last separator */
                            length=(s-p->l_name)-1;
                            if(0<length && length<size) {
                                icu_memcpy(path, p->l_name, length);
                                path[length]=0;
                            } else {
                                length=0;
                            }
                        }
                        break;
                    }
                    p=p->l_next;
                }
            }
            dlclose(handle);
            return length;
        }
#   elif defined(LINUX)
#   elif defined(AIX)
        void *handle=load("libicuuc.a", L_LIBPATH_EXEC, ".");
        if(handle!=NULL) {
            uint8_t buffer[4096];
            struct ld_info *p=NULL;
            char *s;
            int rc, length=0;

            /* copy the linked list of loaded libraries into the buffer */
            rc=loadquery(L_GETINFO, buffer, sizeof(buffer));
            if(rc>=0) {
                /* search for the list item for the library itself */
                p=(struct ld_info *)buffer;
                for(;;) {
                    /* advance (ignore the first list item) */
                    if(p->ldinfo_next==0) {
                        break;
                    }
                    p=(struct ld_info *)((uint8_t *)p+p->ldinfo_next);

                    s=icu_strstr(p->ldinfo_filename, "libicuuc.a");
                    if(s!=NULL) {
                        if(s>p->ldinfo_filename) {
                            /* copy the path, without the basename and the last separator */
                            length=(s-p->ldinfo_filename)-1;
                            if(0<length && length<size) {
                                icu_memcpy(path, p->ldinfo_filename, length);
                                path[length]=0;
                            } else {
                                length=0;
                            }
                        }
                        break;
                    }
                    p=p->l_next;
                }
            }
            unload(handle);
            return length;
        }
#   elif defined(HPUX)
        shl_descriptor *p=NULL;
        char *s;
        int i=1, rc, length=0;

        /* walk the list of shared libraries */
        /* search for the list item for the library itself */
        for(;;) {
            rc=shl_get(i, &p);
            if(rc<0) {
                break;
            }

            s=icu_strstr(p->filename, "libicuuc.sl");
            if(s!=NULL) {
                if(s>p->l_name) {
                    /* copy the path, without the basename and the last separator */
                    length=(s-p->l_name)-1;
                    if(0<length && length<size) {
                        icu_memcpy(path, p->l_name, length);
                        path[length]=0;
                    } else {
                        length=0;
                    }
                }
                break;
            }
            ++i;
        }
        return length;
#   elif defined(TANDEM)
#   elif defined(POSIX)
#   endif
    return 0;
}

/*
 * search for the ICU dynamic library and set the path
 * do not terminate with a U_FILE_SEP_CHAR separator
 * return the length of the path, or 0 if none
 */
static int
findLibraryPath(char *path, int size) {
#   ifdef WIN32
#       define LIB_PATH_VAR "PATH"
#       define LIB_FILENAME "icuuc.dll"
#   elif defined(OS2)
#       define LIB_PATH_VAR "LIBPATH"
#       define LIB_FILENAME "icuuc.dll"
#   elif defined(OS390)
#       define LIB_PATH_VAR "LIBPATH"
#       define LIB_FILENAME "libicuuc.a"
#   elif defined(OS400)
#   elif defined(XP_MAC)
#   elif defined(SOLARIS)
#   elif defined(LINUX)
#       define LIB_PATH_VAR "LD_LIBRARY_PATH"
#       define LIB_FILENAME "libicuuc.so"
#   elif defined(AIX)
#   elif defined(HPUX)
#   elif defined(TANDEM)
#       define LIB_PATH_VAR "LIBPATH"
#       define LIB_FILENAME "libicuuc.a"
#   elif defined(POSIX)
#       define LIB_PATH_VAR "LIBPATH"
#       define LIB_FILENAME "libicuuc.so"
#   endif

    /* common implementation for searching the library path */
#   ifdef LIB_FILENAME
        const char *libPath=getenv(LIB_PATH_VAR);

        if(libPath!=NULL) {
            /* loop over all paths */
            FileStream *f;
            const char *end;
            int length;

            for(;;) {
                /* find the end of the path */
                end=libPath;
                while(*end!=0 && *end!=U_PATH_SEP_CHAR) {
                    ++end;
                }

                if(end!=libPath) {
                    /* try this non-empty path */
                    length=end-libPath;

                    /* do not terminate the path */
                    if(*(end-1)==U_FILE_SEP_CHAR) {
                        --length;
                    }

                    /* copy the path and add the library filename */
                    icu_memcpy(path, libPath, length);
                    icu_strcpy(path+length, U_FILE_SEP_STRING LIB_FILENAME);

                    /* does this file exist in this path? */
                    f=T_FileStream_open(path, "rb");
                    if(f!=NULL) {
                        /* yes, clean up and return */
                        T_FileStream_close(f);
                        path[length]=0;
                        return length;
                    }
                }

                if(*end==0) {
                    break;  /* no more path */
                }

                /* *end==U_PATH_SEP_CHAR, go to the next path */
                libPath=end+1;
            }
        }
#   endif
    return 0;
}

/* define a path for fallbacks */
#define FALLBACK_PATH U_FILE_SEP_STRING "share" U_FILE_SEP_STRING "icu" U_FILE_SEP_STRING ICU_VERSION U_FILE_SEP_STRING

U_CAPI const char * U_EXPORT2
u_getDataDirectory(void) {
    /* if we have the directory, then return it immediately */
    if(!gHaveDataDirectory) {
        /* we need to look for it */
        char pathBuffer[1024];
        char *path;
        int length;

#       if !defined(OS400) && !defined(XP_MAC)
            /* first try to get the environment variable */
            path=getenv("ICU_DATA");
#       endif

#       ifdef WIN32
            /* next, try to read the path from the registry */
            if(path==NULL || *path==0) {
                HKEY key;

                if(ERROR_SUCCESS==RegOpenKeyEx(HKEY_LOCAL_MACHINE, "SOFTWARE\\IBM\\Unicode\\Data", 0, KEY_QUERY_VALUE, &key)) {
                    DWORD type=REG_EXPAND_SZ, size=sizeof(pathBuffer);

                    if(ERROR_SUCCESS==RegQueryValueEx(key, "Path", NULL, &type, pathBuffer, &size) && size>1) {
                        if(type==REG_EXPAND_SZ) {
                            /* replace environment variable references by their values */
                            char temporaryPath[1024];

                            /* copy the path with variables to the temporary one */
                            icu_memcpy(temporaryPath, pathBuffer, size);

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

        /* next, try to get the path to the ICU dynamic library */
        if(path==NULL || *path==0) {
            length=getLibraryPath(pathBuffer, sizeof(pathBuffer));
            if(length>0) {
                icu_strcpy(pathBuffer+length, U_FILE_SEP_STRING ".." FALLBACK_PATH);
                path=pathBuffer;
            }
        }

        /* next, search for the ICU dynamic library */
        if(path==NULL || *path==0) {
            length=findLibraryPath(pathBuffer, sizeof(pathBuffer));
            if(length>0) {
                icu_strcpy(pathBuffer+length, U_FILE_SEP_STRING ".." FALLBACK_PATH);
                path=pathBuffer;
            }
        }

        /* last resort: use hardcoded path */
        if(path==NULL || *path==0) {
            /* ICU_DATA_DIR may be set as a compile option */
#           ifdef ICU_DATA_DIR
                path=ICU_DATA_DIR;
#           else
                length=getSystemPath(pathBuffer, sizeof(pathBuffer));
                if(length>0) {
                    icu_strcpy(pathBuffer+length, FALLBACK_PATH);
                    path=pathBuffer;
                } else {
                    path=FALLBACK_PATH;
                }
#           endif
        }

        u_setDataDirectory(path);
    }

    /* we did set the directory if necessary */
    return gDataDirectory;
}

/* Macintosh-specific locale information ------------------------------------ */
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

#ifdef OS400
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
#ifdef OS390
  double last_eps,sum;
#endif
  /* This is the portable implementation...*/
  /* a small coefficient within the precision of the mantissa*/
  static const double smallValue = 1e-10;  
  double epsilon = ((d<0)?-d:d) * smallValue; /* first approximation*/
  if (epsilon == 0) epsilon = smallValue; /* for very small d's*/
  if (!next) epsilon = -epsilon;
  double last_eps = epsilon * 2.0;
  /* avoid higher precision possibly used for temporay values*/
#ifdef OS390
  last_eps = epsilon * 2.0;
  sum = d + epsilon;
#else
  double sum = d + epsilon; 
#endif
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
  return U_IS_BIG_ENDIAN ? (char*)d : (char*)(d + 1) - n;
}

static char* u_bottomNBytesOfDouble(double* d, int n)
{
  return U_IS_BIG_ENDIAN ? (char*)(d + 1) - n : (char*)d;
}

const char* icu_getDefaultCodepage()
{
  /*Lazy evaluates DEFAULT_CONVERTER_NAME*/
  if (DEFAULT_CONVERTER_NAME[0]) return DEFAULT_CONVERTER_NAME;
#if defined(OS400)
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
