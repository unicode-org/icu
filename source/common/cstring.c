/*
******************************************************************************
*
*   Copyright (C) 1997-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File CSTRING.C
*
* @author       Helena Shih
*
* Modification History:
*
*   Date        Name        Description
*   6/18/98     hshih       Created
*   09/08/98    stephen     Added include for ctype, for Mac Port
*   11/15/99    helena      Integrated S/390 IEEE changes. 
******************************************************************************
*/



#include <stdlib.h>
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"

/*
 * We hardcode case conversion for invariant characters to match our expectation
 * and the compiler execution charset.
 * This prevents problems on systems
 * - with non-default casing behavior, like Turkish system locales where
 *   tolower('I') maps to dotless i and toupper('i') maps to dotted I
 * - where there are no lowercase Latin characters at all, or using different
 *   codes (some old EBCDIC codepages)
 *
 * This works because the compiler usually runs on a platform where the execution
 * charset includes all of the invariant characters at their expected
 * code positions, so that the char * string literals in ICU code match
 * the char literals here.
 *
 * Note that the set of lowercase Latin letters is discontiguous in EBCDIC
 * and the set of uppercase Latin letters is discontiguous as well.
 */

U_CAPI char U_EXPORT2
uprv_toupper(char c) {
#if U_CHARSET_FAMILY==U_EBCDIC_FAMILY
    if(('a'<=c && c<='i') || ('j'<=c && c<='r') || ('s'<=c && c<='z')) {
        c=(char)(c+('A'-'a'));
    }
#else
    if('a'<=c && c<='z') {
        c=(char)(c+('A'-'a'));
    }
#endif
    return c;
}

U_CAPI char U_EXPORT2
uprv_tolower(char c) {
#if U_CHARSET_FAMILY==U_EBCDIC_FAMILY
    if(('A'<=c && c<='I') || ('J'<=c && c<='R') || ('S'<=c && c<='Z')) {
        c=(char)(c+('a'-'A'));
    }
#else
    if('A'<=c && c<='Z') {
        c=(char)(c+('a'-'A'));
    }
#endif
    return c;
}


U_CAPI char* U_EXPORT2
T_CString_toLowerCase(char* str)
{
    char* origPtr = str;

    if (str) {
        do
            *str = (char)uprv_tolower(*str);
        while (*(str++));
    }

    return origPtr;
}

U_CAPI char* U_EXPORT2
T_CString_toUpperCase(char* str)
{
    char* origPtr = str;

    if (str) {
        do
            *str = (char)uprv_toupper(*str);
        while (*(str++));
    }

    return origPtr;
}

/*
 * Takes a int32_t and fills in  a char* string with that number "radix"-based.
 * Does not handle negative values (makes an empty string for them).
 * Writes at most 11 chars ("2147483647" plus NUL).
 * Returns the length of the string.
 */
U_CAPI int32_t U_EXPORT2
T_CString_integerToString(char* buffer, int32_t i, int32_t radix)
{
  int32_t length;
  int32_t num;
  int8_t digit;
  char temp;

  if(i<0) {
    *buffer = 0;
    return 0;
  }

  length = 0;
  while (i>=radix)
    {
      num = i/radix;
      digit = (int8_t)(i - num*radix);
      buffer[length++] = (char)(T_CString_itosOffset(digit));
      i = num;
    }

  buffer[length] = (char)(T_CString_itosOffset(i));
  buffer[++length] = '\0';


  /* Reverses the string, swap digits at buffer[0]..buffer[num] */
  num = length - 1;
  for (i = 0; i < num; ++i, --num) {
    temp = buffer[num];
    buffer[num] = buffer[i];
    buffer[i] = temp;
  }

  return length;
}


U_CAPI int32_t U_EXPORT2
T_CString_stringToInteger(const char *integerString, int32_t radix)
{
    char *end;
    return strtoul(integerString, &end, radix);

}
    
U_CAPI int U_EXPORT2
T_CString_stricmp(const char *str1, const char *str2) {
    if(str1==NULL) {
        if(str2==NULL) {
            return 0;
        } else {
            return -1;
        }
    } else if(str2==NULL) {
        return 1;
    } else {
        /* compare non-NULL strings lexically with lowercase */
        int rc;
        unsigned char c1, c2;

        for(;;) {
            c1=(unsigned char)*str1;
            c2=(unsigned char)*str2;
            if(c1==0) {
                if(c2==0) {
                    return 0;
                } else {
                    return -1;
                }
            } else if(c2==0) {
                return 1;
            } else {
                /* compare non-zero characters with lowercase */
                rc=(int)(unsigned char)uprv_tolower(c1)-(int)(unsigned char)uprv_tolower(c2);
                if(rc!=0) {
                    return rc;
                }
            }
            ++str1;
            ++str2;
        }
    }
}

U_CAPI int U_EXPORT2
T_CString_strnicmp(const char *str1, const char *str2, uint32_t n) {
    if(str1==NULL) {
        if(str2==NULL) {
            return 0;
        } else {
            return -1;
        }
    } else if(str2==NULL) {
        return 1;
    } else {
        /* compare non-NULL strings lexically with lowercase */
        int rc;
        unsigned char c1, c2;

        for(; n--;) {
            c1=(unsigned char)*str1;
            c2=(unsigned char)*str2;
            if(c1==0) {
                if(c2==0) {
                    return 0;
                } else {
                    return -1;
                }
            } else if(c2==0) {
                return 1;
            } else {
                /* compare non-zero characters with lowercase */
                rc=(int)(unsigned char)uprv_tolower(c1)-(int)(unsigned char)uprv_tolower(c2);
                if(rc!=0) {
                    return rc;
                }
            }
            ++str1;
            ++str2;
        }
    }

    return 0;
}

U_CAPI char* U_EXPORT2
uprv_strdup(const char *src) {
    size_t len = strlen(src) + 1;
    char *dup = (char *) uprv_malloc(len);

    if (dup) {
        uprv_memcpy(dup, src, len);
    }

    return dup;
}
