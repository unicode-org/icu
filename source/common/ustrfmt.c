/*
**********************************************************************
*   Copyright (C) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#include "ustrfmt.h"

U_CAPI double   
uprv_strtod(const char* source, char** end)
{
    return strtod(source,end);
}


U_CAPI char* 
uprv_dtostr(double value, char *buffer, int maximumDigits,UBool fixedPoint)
{
    char *itrPtr = buffer + 1;  /* skip '-' or a number before the decimal */
    char *startPtr;

    sprintf(buffer,"%f",value);

    /* Find the decimal point.
       Some unusal machines use a comma when the system locale changes
    */
    while (isalnum(*itrPtr)) {
        itrPtr++;
    }
    *itrPtr = '.';

    /* truncate trailing zeros, except the one after '.' */
    startPtr = itrPtr + 1;
    itrPtr = uprv_strchr(startPtr, 0);
    while(--itrPtr > startPtr){
        if(*itrPtr == '0'){
            *itrPtr = 0;
        }else{
            break;
        }
    }
    return buffer;
}

/*Takes a int32_t and fills in  a UChar* string with that number "radix"-based
 * and padded with "pad" zeroes
 */
#define MAX_DIGITS 10
U_CAPI int32_t
uprv_itou (UChar * buffer, uint32_t i, uint32_t radix, int32_t pad)
{
    int32_t length = 0;
    int32_t num = 0;
    int digit;
    int32_t j;
    UChar temp;

    do{
        digit = (int)(i % radix);
        buffer[length++]=(UChar)(digit<=9?(0x0030+digit):(0x0030+digit+7));
        i=i/radix;
    } while(i);

    while (length < pad){
        buffer[length++] = (UChar) 0x0030;/*zero padding */
    }
    /* null terminate the buffer */
    if(length<MAX_DIGITS){
        buffer[length] = (UChar) 0x0000;
    }
    num= (pad>=length) ? pad :length;

    /* Reverses the string */
    for (j = 0; j < (num / 2); j++){
        temp = buffer[(length-1) - j];
        buffer[(length-1) - j] = buffer[j];
        buffer[j] = temp;
    }
    return length;
}
