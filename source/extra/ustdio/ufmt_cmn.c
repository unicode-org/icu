/*
******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File ufmt_cmn.c
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen     Creation.
*   03/12/99    stephen     Modified for new C API.
*   03/15/99    stephen     Added defaultCPToUnicode, unicodeToDefaultCP
*   07/19/99    stephen     Fixed bug in defaultCPToUnicode
******************************************************************************
*/

#include "cstring.h"
#include "cmemory.h"
#include "ufmt_cmn.h"
#include "unicode/uchar.h"
#include "unicode/ucnv.h"
#include "ustr_imp.h"

#define DIGIT_0     0x0030
#define DIGIT_9     0x0039
#define LOWERCASE_A 0x0061
#define UPPERCASE_A 0x0041
#define LOWERCASE_Z 0x007A
#define UPPERCASE_Z 0x005A

int
ufmt_digitvalue(UChar c)
{
    if( ((c>=DIGIT_0)&&(c<=DIGIT_9)) ||
        ((c>=LOWERCASE_A)&&(c<=LOWERCASE_Z)) ||
        ((c>=UPPERCASE_A)&&(c<=UPPERCASE_Z))  )
    {
      return c - 0x0030 - (c >= 0x0041 ? (c >= 0x0061 ? 39 : 7) : 0);
    }
    else
    {
      return -1;
    }
}

UBool
ufmt_isdigit(UChar     c,
             int32_t     radix)
{
    int digitVal = ufmt_digitvalue(c);

    return (UBool)(digitVal < radix && digitVal >= 0);
}

#define TO_UC_DIGIT(a) a <= 9 ? (0x0030 + a) : (0x0030 + a + 7)
#define TO_LC_DIGIT(a) a <= 9 ? (0x0030 + a) : (0x0030 + a + 39)

void 
ufmt_ltou(UChar     *buffer, 
          int32_t   *len,
          uint32_t  value, 
          uint32_t  radix,
          UBool     uselower,
          int32_t   minDigits)
{
    int32_t  length = 0;
    uint32_t digit;
    UChar    *left, *right, temp;
    
    do {
        digit = value % radix;
        value = value / radix;
        buffer[length++] = (UChar)(uselower ? TO_LC_DIGIT(digit) 
            : TO_UC_DIGIT(digit));
    } while(value);

    /* pad with zeroes to make it minDigits long */
    if(minDigits != -1 && length < minDigits) {
        while(length < minDigits && length < *len)
            buffer[length++] = 0x0030;  /*zero padding */
    }

    /* reverse the buffer */
    left     = buffer;
    right = buffer + length;
    while(left < --right) {
        temp     = *left;
        *left++     = *right;
        *right     = temp;
    }
    
    *len = length;
}

long
ufmt_utol(const UChar     *buffer, 
          int32_t     *len,
          int32_t     radix)
{
    const UChar     *limit;
    int32_t         count;
    long        result;
    
    
    /* intialize parameters */
    limit     = buffer + *len;
    count     = 0;
    result     = 0;
    
    /* iterate through buffer */
    while(ufmt_isdigit(*buffer, radix) && buffer < limit) {
        
        /* read the next digit */
        result *= radix;
        result += ufmt_digitvalue(*buffer++);
        
        /* increment our count */
        ++count;
    }
    
    *len = count;
    return result;
}

UChar*
ufmt_defaultCPToUnicode(const char *s, int32_t sSize,
                        UChar *target, int32_t tSize)
{
    UChar *alias;
    UErrorCode status = U_ZERO_ERROR;
    UConverter *defConverter = u_getDefaultConverter(&status);
    
    if(U_FAILURE(status) || defConverter == 0)
        return 0;

    if(sSize <= 0) {
        sSize = uprv_strlen(s) + 1;
    }
    
    /* perform the conversion in one swoop */
    if(target != 0) {
        
        alias = target;
        ucnv_toUnicode(defConverter, &alias, alias + tSize, &s, s + sSize - 1, 
            NULL, TRUE, &status);
        
        
        /* add the null terminator */
        *alias = 0x0000;
    }
    
    u_releaseDefaultConverter(defConverter);
    
    return target;
}

char*
ufmt_unicodeToDefaultCP(const UChar *s,
                        int32_t len)
{
    int32_t size;
    char *target, *alias;
    UErrorCode status = U_ZERO_ERROR;
    UConverter *defConverter = u_getDefaultConverter(&status);
    
    if(U_FAILURE(status) || defConverter == 0)
        return 0;
    
    /* perform the conversion in one swoop */
    target = (char*) 
        uprv_malloc((len + 1) * ucnv_getMaxCharSize(defConverter) * sizeof(char));
    size = (len) * ucnv_getMaxCharSize(defConverter) * sizeof(char);
    if(target != 0) {
        
        alias = target;
        ucnv_fromUnicode(defConverter, &alias, alias + size, &s, s + len, 
            NULL, TRUE, &status);
        
        
        /* add the null terminator */
        *alias = 0x00;
    }
    
    u_releaseDefaultConverter(defConverter);
    
    return target;
}



