/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ustring.h
*
* Modification History:
*
*   Date        Name        Description
*   12/07/98    bertrand    Creation.
*******************************************************************************
*/

#include "unicode/ustring.h"
#include "unicode/utypes.h"
#include "cstring.h"
#include "umutex.h"
#include "unicode/ucnv.h"

/* forward declaractions of definitions for the shared default converter */

static UConverter *fgDefaultConverter = NULL;

static UConverter*
getDefaultConverter(void);

static void
releaseDefaultConverter(UConverter *converter);

/* ANSI string.h - style functions ------------------------------------------ */

#define MAX_STRLEN 0x0FFFFFFF

UChar*
u_strcat(UChar     *dst, 
    const UChar     *src)
{
  UChar *anchor = dst;            /* save a pointer to start of dst */

  while(*dst != 0) {              /* To end of first string          */
    ++dst;
  }
  while((*dst = *src) != 0) {     /* copy string 2 over              */
    ++dst;
    ++src;
  }

  return anchor;
}

UChar* 
u_strncat(UChar     *dst, 
     const UChar     *src, 
     int32_t     n ) 
{
  if(n > 0) {
    UChar *anchor = dst;            /* save a pointer to start of dst */

    while(*dst != 0) {              /* To end of first string          */
      ++dst;
    }
    while((*dst = *src) != 0) {     /* copy string 2 over              */
      ++dst;
      if(--n == 0) {
        *dst = 0;
        break;
      }
      ++src;
    }
  
    return anchor;
  } else {
    return dst;
  }
}

UChar*
u_strchr(const UChar     *s, 
    UChar     c) 
{
  while(*s != 0) {
    if(*s == c) {
      return (UChar *)s;
    }
    ++s;
  }
  return NULL;
}

U_CAPI UChar * U_EXPORT2
u_strstr(const UChar *s, const UChar *substring) {
  if(*substring == 0) {
    return (UChar *)s;
  } else if(substring[1] == 0) {
    return u_strchr(s, *substring);
  } else {
    /* ### TBD */
    return NULL;
  }
}

U_CAPI UChar * U_EXPORT2
u_strchr32(const UChar *s, UChar32 c) {
  if(!UTF_NEED_MULTIPLE_UCHAR(c)) {
    return u_strchr(s, (UChar)c);
  } else {
    UChar buffer[UTF_MAX_CHAR_LENGTH + 1];
    UTextOffset i = 0;
    UTF_APPEND_CHAR_UNSAFE(buffer, i, c);
    buffer[i] = 0;
    return u_strstr(s, buffer);
  }
}

int32_t  
u_strcmp(const UChar *s1, 
    const UChar *s2) 
{
  int32_t rc;
  for(;;) {
    rc = (int32_t)*s1 - (int32_t)*s2;
    if(rc != 0 || *s1 == 0) {
      return rc;
    }
    ++s1;
    ++s2;
  }
}

int32_t  
u_strncmp(const UChar     *s1, 
     const UChar     *s2, 
     int32_t     n) 
{
  if(n > 0) {
    int32_t rc;
    for(;;) {
      rc = (int32_t)*s1 - (int32_t)*s2;
      if(rc != 0 || *s1 == 0 || --n == 0) {
        return rc;
      }
      ++s1;
      ++s2;
    }
  } else {
    return 0;
  }
}

UChar*
u_strcpy(UChar     *dst, 
    const UChar     *src) 
{
  UChar *anchor = dst;            /* save a pointer to start of dst */

  while((*dst = *src) != 0) {     /* copy string 2 over              */
    ++dst;
    ++src;
  }

  return anchor;
}

UChar* 
u_strncpy(UChar     *dst, 
     const UChar     *src, 
     int32_t     n) 
{
  UChar *anchor = dst;            /* save a pointer to start of dst */

  if(n > 0) {
    while((*dst = *src) != 0) {   /* copy string 2 over              */
      ++dst;
      if(--n == 0) {
        *dst = 0;
        break;
      }
      ++src;
    }
  } else {
    *dst = 0;
  }

  return anchor;
}

int32_t  
u_strlen(const UChar *s) 
{
# if U_SIZEOF_WCHAR_T == U_SIZEOF_UCHAR
    return uprv_wcslen(s);
# else
    const UChar *t = s;
    while(*t != 0) {
      ++t;
    }
    return t - s;
#endif
}

/* conversions between char* and UChar* ------------------------------------- */

UChar* u_uastrcpy(UChar *ucs1,
          const char *s2 )
{
  UConverter *cnv = getDefaultConverter();
  if(cnv != NULL) {
    UErrorCode err = U_ZERO_ERROR;
    ucnv_toUChars(cnv,
                    ucs1,
                    MAX_STRLEN,
                    s2,
                    uprv_strlen(s2),
                    &err);
    releaseDefaultConverter(cnv);
    if(U_FAILURE(err)) {
      *ucs1 = 0;
    }
  } else {
    *ucs1 = 0;
  }
  return ucs1;
}

UChar* u_uastrncpy(UChar *ucs1,
           const char *s2 ,
           int32_t n)
{
  UChar *target = ucs1;
  UConverter *cnv = getDefaultConverter();
  if(cnv != NULL) {
    UErrorCode err = U_ZERO_ERROR;
    ucnv_reset(cnv);
    ucnv_toUnicode(cnv,
                   &target,
                   ucs1+n,
                   &s2,
                   s2+uprv_strlen(s2),
                   NULL,
                   TRUE,
                   &err);
    ucnv_reset(cnv); /* be good citizens */
    releaseDefaultConverter(cnv);
    if(U_FAILURE(err) && (err != U_INDEX_OUTOFBOUNDS_ERROR) ) {
      *ucs1 = 0; /* failure */
    }
    if(target < (ucs1+n)) { /* Indexoutofbounds isn't an err, just means no termination will happen. */
      *target = 0;  /* terminate */
    }
  } else {
    *ucs1 = 0;
  }
  return ucs1;
}

char* u_austrcpy(char *s1,
         const UChar *ucs2 )
{
  UConverter *cnv = getDefaultConverter();
  if(cnv != NULL) {
    UErrorCode err = U_ZERO_ERROR;
    int32_t len = ucnv_fromUChars(cnv,
                  s1,
                  MAX_STRLEN,
                  ucs2,
                  -1,
                  &err);
    releaseDefaultConverter(cnv);
    s1[len] = 0;
  } else {
    *s1 = 0;
  }
  return s1;
}

/* mutexed access to a shared default converter ----------------------------- */

/* this is the same implementation as in unistr.cpp */

static UConverter*
getDefaultConverter()
{
  UConverter *converter = NULL;

  if(fgDefaultConverter != NULL) {
    umtx_lock(NULL);

    /* need to check to make sure it wasn't taken out from under us */
    if(fgDefaultConverter != NULL) {
      converter = fgDefaultConverter;
      fgDefaultConverter = NULL;
    }
    umtx_unlock(NULL);
  }

  /* if the cache was empty, create a converter */
  if(converter == NULL) {
    UErrorCode status = U_ZERO_ERROR;
    converter = ucnv_open(NULL, &status);
    if(U_FAILURE(status)) {
      return NULL;
    }
  }

  return converter;
}

static void
releaseDefaultConverter(UConverter *converter)
{
  if(fgDefaultConverter == NULL) {
    umtx_lock(NULL);

    if(fgDefaultConverter == NULL) {
      fgDefaultConverter = converter;
      converter = NULL;
    }
    umtx_unlock(NULL);
  }

  if(converter != NULL) {
    ucnv_close(converter);
  }
}

U_CAPI int32_t U_EXPORT2
u_unescapeChars(const char *s,
                UChar *dest, int32_t destSize) {
  /* ### TBD */
  return 0;
}
