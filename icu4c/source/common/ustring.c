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

#define MAX_STRLEN 0x00FFFFFF

/*Lazy evaluating macro for the default converter*/
#define defaultConverter (_defaultConverter==NULL)?_defaultConverter=ucnv_open(NULL, &gErr):_defaultConverter

UChar*
u_strcat(UChar     *dst, 
    const UChar     *src)
{
  UChar *anchor = dst;       /* save a pointer to start of dst */
 
  while(*dst++);               /* To end of first string          */
  dst--;                       /* Return to the null              */
  while(*dst++ = *src++);      /* copy string 2 over              */
  return anchor;
}

UChar* 
u_strncat(UChar     *dst, 
     const UChar     *src, 
     int32_t     n ) 
{
  UChar *anchor = dst;       /* save a pointer to start of dst */
  
  if (!n) return dst;
  while(*dst++);               /* To end of first string          */
  dst--;                       /* Return to the null              */
  while((*dst++ = *src++) && --n);    /* copy string 2 over              */
  *dst = 0x0000;
  
  return anchor;
}

UChar*
u_strchr(const UChar     *s, 
    UChar     c) 
{
  while((*s != c) && *s) 
    s++;
  
  if(*s == c)
    return (UChar*) s;
  return NULL;
}

int32_t  
u_strcmp(const UChar *s1, 
    const UChar *s2) 
{
  while((*s1 == *s2) && *s1) {
    s1++;
    s2++;
  }

  return (int32_t)*s1 - (int32_t)*s2;
}

int32_t  
u_strncmp(const UChar     *s1, 
     const UChar     *s2, 
     int32_t     n) 
{
  if (!n) return 0;
  while((*s1 == *s2) && *s1 && --n) {
    s1++;
    s2++;
  }
  return  (int32_t)*s1 - (int32_t)*s2;
}

UChar*
u_strcpy(UChar     *dst, 
    const UChar     *src) 
{
  UChar *anchor = dst;     /* save the start of result string */
  
  while(*dst++ = *src++);
  return anchor;
}

UChar* 
u_strncpy(UChar     *dst, 
     const UChar     *src, 
     int32_t     n) 
{
  UChar *anchor = dst;     /* save the start of result string */
  
  if (!n) return dst;
  while((*dst++ = *src++) && --n);
  *dst = 0x0000;
  return anchor;
}

int32_t  
u_strlen(const UChar *s) 
{
  int32_t  i = 0;
  
  while(*s++)
    i++;
  return  i;
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
  
  return ucs1;
}

UChar* u_uastrncpy(UChar *ucs1,
           const char *s2 ,
           int32_t n)
{
  UErrorCode err = U_ZERO_ERROR;
  int32_t end = ucnv_toUChars(defaultConverter,
                  ucs1,
                  n,
                  s2,
                  icu_strlen(s2),
                  &err);
  
  ucs1[icu_min(end,n)] = 0x0000;
  return ucs1;
}

char* u_austrcpy(char *s1,
         const UChar *ucs2 )
{
  char * anchor = s1;     /* save the start of result string */
  UErrorCode err = U_ZERO_ERROR;
  int32_t len = ucnv_fromUChars(defaultConverter,
                s1,
                MAX_STRLEN,
                ucs2,
                &err);
  
  s1[len] = '\0';
  return s1;
  
}




