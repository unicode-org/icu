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

/* A Boyer-Moore algorithm would be better, but that would require a hashtable
   because UChar is so big. This algorithm doesn't use a lot of extra memory.
 */
U_CAPI UChar * U_EXPORT2
u_strstr(const UChar *s, const UChar *substring) {

  UChar *strItr, *subItr;

  if (*substring == 0) {
    return (UChar *)s;
  }

  do {
    strItr = (UChar *)s;
    subItr = (UChar *)substring;

    /* Only one string iterator needs checking for null terminator */
    while ((*strItr != 0) && (*strItr == *subItr)) {
      strItr++;
	  subItr++;
	}

    if (*subItr == 0) {             /* Was the end of the substring reached? */
      return (UChar *)s;
    }

    s++;
  } while (*strItr != 0);           /* Was the end of the string reached? */

  return NULL;                      /* No match */
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

/*
 returns the minimum of (the length of the null-terminated string) and n.
*/
int32_t u_astrnlen(const char *ucs1, int32_t n)
{
    int32_t len = 0;

    if (ucs1)
    {
        while (*(ucs1++) && n--)
        {
            len++;
        }
    }
    return len;
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
                   s2+u_astrnlen(s2, n),
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

/* u_unescape & support fns ------------------------------------------------- */

static const UChar UNESCAPE_MAP[] = {
    /*"   0x22, 0x22 */
    /*'   0x27, 0x27 */
    /*?   0x3F, 0x3F */
    /*\   0x5C, 0x5C */
    /*a*/ 0x61, 0x07,
    /*b*/ 0x62, 0x08,
    /*f*/ 0x66, 0x0c,
    /*n*/ 0x6E, 0x0a,
    /*r*/ 0x72, 0x0d,
    /*t*/ 0x74, 0x09,
    /*v*/ 0x76, 0x0b
};
enum { UNESCAPE_MAP_LENGTH = sizeof(UNESCAPE_MAP) / sizeof(UNESCAPE_MAP[0]) };

/* Convert one octal digit to a numeric value 0..7, or -1 on failure */
static int8_t _digit8(UChar c) {
    if (c >= 0x0030 && c <= 0x0037) {
        return (int8_t)(c - 0x0030);
    }
    return -1;
}

/* Convert one hex digit to a numeric value 0..F, or -1 on failure */
static int8_t _digit16(UChar c) {
    if (c >= 0x0030 && c <= 0x0039) {
        return (int8_t)(c - 0x0030);
    }
    if (c >= 0x0041 && c <= 0x0046) {
        return (int8_t)(c - (0x0041 - 10));
    }
    if (c >= 0x0061 && c <= 0x0066) {
        return (int8_t)(c - (0x0061 - 10));
    }
    return -1;
}

/* Parse a single escape sequence.  Although this method deals in
 * UChars, it does not use C++ or UnicodeString.  This allows it to
 * be used from C contexts. */
U_CAPI int32_t U_EXPORT2
u_unescapeAt(UNESCAPE_CHAR_AT charAt,
             int32_t *offset,
             int32_t length,
             void *context) {

    int32_t start = *offset;
    UChar c;
    UChar32 result = 0;
    int8_t n = 0;
    int8_t minDig = 0;
    int8_t maxDig = 0;
    int8_t bitsPerDigit = 4; 
    int8_t dig;
    int32_t i;

    /* Check that offset is in range */
    if (*offset < 0 || *offset >= length) {
        goto err;
    }

    /* Fetch first UChar after '\\' */
    c = charAt((*offset)++, context);

    /* Convert hexadecimal and octal escapes */
    switch (c) {
    case 0x0075 /*'u'*/:
        minDig = maxDig = 4;
        break;
    case 0x0055 /*'U'*/:
        minDig = maxDig = 8;
        break;
    case 0x0078 /*'x'*/:
        minDig = 1;
        maxDig = 2;
        break;
    default:
        dig = _digit8(c);
        if (dig >= 0) {
            minDig = 1;
            maxDig = 3;
            n = 1; /* Already have first octal digit */
            bitsPerDigit = 3;
            result = dig;
        }
        break;
    }
    if (minDig != 0) {
        while (*offset < length && n < maxDig) {
            c = charAt(*offset, context);
            dig = (int8_t)((bitsPerDigit == 3) ? _digit8(c) : _digit16(c));
            if (dig < 0) {
                break;
            }
            result = (result << bitsPerDigit) | dig;
            ++(*offset);
            ++n;
        }
        if (n < minDig) {
            goto err;
        }
        return result;
    }

    /* Convert C-style escapes in table */
    for (i=0; i<UNESCAPE_MAP_LENGTH; i+=2) {
        if (c == UNESCAPE_MAP[i]) {
            return UNESCAPE_MAP[i+1];
        } else if (c > UNESCAPE_MAP[i]) {
            break;
        }
    }

    /* If no special forms are recognized, then consider
     * the backslash to generically escape the next character. */
    return c;

 err:
    /* Invalid escape sequence */
    *offset = start; /* Reset to initial value */
    return (UChar32)0xFFFFFFFF;
}

/* u_unescapeAt() callback to return a UChar from a char* */
static UChar _charPtr_charAt(int32_t offset, void *context) {
    UChar c16;
    /* It would be more efficient to access the invariant tables
     * directly but there is no API for that. */
    u_charsToUChars(((char*) context) + offset, &c16, 1);
    return c16;
}

/* Append an escape-free segment of the text; used by u_unescape() */
static void _appendUChars(UChar *dest, int32_t destCapacity,
                          const char *src, int32_t srcLen) {
    if (destCapacity < 0) {
        destCapacity = 0;
    }
    if (srcLen > destCapacity) {
        srcLen = destCapacity;
    }
    u_charsToUChars(src, dest, srcLen);
}

/* Do an invariant conversion of char* -> UChar*, with escape parsing */
U_CAPI int32_t U_EXPORT2
u_unescape(const char *src, UChar *dest, int32_t destCapacity) {
    const char *segment = src;
    UChar *destStart = dest;
    UChar *destLimit;
    char c;

    if (dest == NULL) {
        destCapacity = 0;
    }

    destLimit = dest + destCapacity;

    while ((c=*src) != 0) {
        /* '\\' intentionally written as compiler-specific
         * character constant to correspond to compiler-specific
         * char* constants. */
        if (c == '\\') {
            int32_t lenParsed = 0;
            UChar32 c32;
            if (src != segment) {
                _appendUChars(dest, destLimit - dest,
                              segment, src - segment);
                dest += src - segment;
            }
            ++src; /* advance past '\\' */
            c32 = u_unescapeAt(_charPtr_charAt, &lenParsed, uprv_strlen(src), (void*)src);
            if (lenParsed == 0) {
                goto err;
            }
            src += lenParsed; /* advance past escape seq. */
            if (destStart != NULL) {
                *dest = (UChar) c32;
            }
            dest++;
            segment = src;
        } else {
            ++src;
        }
    }
    if (src != segment) {
        _appendUChars(dest, destLimit - dest,
                      segment, src - segment);
        dest += src - segment;
    }
    if (dest < destLimit) {
        *dest = 0;
    }
    return dest - destStart + 1; /* add 1 for zero term */

 err:
    if (destStart != NULL && destCapacity > 0) {
        *destStart = 0;
    }
    return 0;
}
