/*
*******************************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
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
*****************************************************************************************
*/



#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"

char*
T_CString_toLowerCase(char* str)
{
    uint32_t i=0;
    while(str[i]) 
        str[i++] = (char)tolower(str[i]);
    return str;
}

char*
T_CString_toUpperCase(char* str)
{
    uint32_t i=0;
    while(str[i]) 
        str[i++] = (char)toupper(str[i]);
    return str;
}

/*Takes a int32_t and     fills in  a char* string with that number "radix"-based*/

void T_CString_integerToString(char* buffer, int32_t i, int32_t radix)
{
  int32_t length=0;
  int32_t num = 0;
  int8_t digit;
  char temp;

  while (i>=radix)
    {
      num = i/radix;
      digit = (int8_t)(i - num*radix);
      buffer[length++] = (char)(T_CString_itosOffset(digit));
      i = num;
    }

  buffer[length] = (char)(T_CString_itosOffset(i));
  buffer[length+1] = '\0';


  /*Reverses the string*/
  for (i = 0; i < length; ++i, --length) {
    temp = buffer[length];
    buffer[length] = buffer[i];
    buffer[i] = temp;
  }

  return;
}

#include <stdio.h>

int32_t
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

U_CAPI char *uprv_strdup(const char *src) {
    size_t len = strlen(src) + 1;
    char *dup = (char *) malloc(len);

    if (dup) {
        uprv_memcpy(dup, src, len);
    }

    return dup;
}

#if !U_HAVE_WCHAR_H
U_CAPI wchar_t *uprv_wcscat(wchar_t *dst, const wchar_t *src) {
    wchar_t *start=dst;
    while(*dst!=0) {
        ++dst;
    }
    while((*dst=*src)!=0) {
        ++dst;
        ++src;
    }
    return start;
}

U_CAPI wchar_t *uprv_wcscpy(wchar_t *dst, const wchar_t *src) {
    wchar_t *start=dst;
    while((*dst=*src)!=0) {
        ++dst;
        ++src;
    }
    return start;
}

U_CAPI size_t uprv_wcslen(const wchar_t *src) {
    const wchar_t *start=src;
    while(*src!=0) {
        ++src;
    }
    return src-start;
}
#endif
