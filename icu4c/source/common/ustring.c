/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
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

#include "ustring.h"
#include "utypes.h"
#include "cstring.h"
#include "ucnv.h"


static UConverter* _defaultConverter = NULL;
static UErrorCode gErr = U_ZERO_ERROR;

#define MAX_STRLEN 0x0FFFFFFF

/*Lazy evaluating macro for the default converter*/
#define defaultConverter (_defaultConverter==NULL)?_defaultConverter=ucnv_open(NULL, &gErr):_defaultConverter

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
  if(U_SIZEOF_WCHAR_T == sizeof(UChar)) {
    return icu_wcslen(s);
  } else {
    const UChar *t = s;
    while(*t != 0) {
      ++t;
    }
    return t - s;
  }
}


UChar* u_uastrcpy(UChar *ucs1,
          const char *s2 )
{
  UErrorCode err = U_ZERO_ERROR;
  ucnv_toUChars(defaultConverter,
                  ucs1,
                  MAX_STRLEN,
                  s2,
                  icu_strlen(s2),
                  &err);
  if(U_FAILURE(err)) {
    *ucs1 = 0;
  }
  return ucs1;
}

UChar* u_uastrncpy(UChar *ucs1,
           const char *s2 ,
           int32_t n)
{
  UErrorCode err = U_ZERO_ERROR;
  ucnv_toUChars(defaultConverter,
                  ucs1,
                  n,
                  s2,
                  icu_strlen(s2),
                  &err);

  if(U_FAILURE(err)) {
    *ucs1 = 0;
  }
  return ucs1;
}

char* u_austrcpy(char *s1,
         const UChar *ucs2 )
{
  UErrorCode err = U_ZERO_ERROR;
  int32_t len = ucnv_fromUChars(defaultConverter,
                s1,
                MAX_STRLEN,
                ucs2,
                &err);

  s1[len] = 0;
  return s1;
}
