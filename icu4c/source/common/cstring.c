/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
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
#include "utypes.h"
#include "putil.h"
#include "cstring.h"

char*
T_CString_toLowerCase(char* str)
{
    uint32_t i=0;
    while(str[i]) 
        str[i++] = tolower(str[i]);
    return str;
}

char*
T_CString_toUpperCase(char* str)
{
    uint32_t i=0;
    while(str[i]) 
        str[i++] = toupper(str[i]);
    return str;
}

/*Takes a int32_t and     fills in  a char* string with that number "radix"-based*/

void T_CString_integerToString(char* buffer, int32_t i, int32_t radix)
{
  int32_t length=0;
  int32_t num = 0;
  int8_t digit;
  int32_t j;
  char temp;
  
  while (i>radix)
    {
      num = i/radix;
      digit = (int8_t)(i - num*radix);
      buffer[length++] = (T_CString_itosOffset(digit));
      i = num;
    }
  
  buffer[length] = (T_CString_itosOffset(i));
  buffer[length+1] = '\0';
  
  
  /*Reverses the string*/
  for (j=0 ; j<(length/2) + 1 ; j++)
    {
      temp = buffer[length - j];
      buffer[length - j] = buffer[j];
      buffer[j] = temp;
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
                rc=(int)(unsigned char)icu_tolower(c1)-(int)(unsigned char)icu_tolower(c2);
                if(rc!=0) {
                    return rc;
                }
            }
            ++str1;
            ++str2;
        }
    }
}
