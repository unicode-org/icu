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
*    09/08/98    stephen        Added include for ctype, for Mac Port
*****************************************************************************************
*/



#ifndef _CSTRING
#include "cstring.h"
#endif

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>

char T_CString_toffst(char a) 
{
    if (a>0x39) return 0x37;
    else return 0x30;
}


#define T_CString_itosOffset(a) a<=9?(0x30+a):(0x30+a+7)

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
    
  /*    int32_t     integer = 0;
    int8_t      sign = (integerString[0]=='-')?(1):0;
    uint32_t    i   = sign;
    int8_t digit;*/
    char *end;
    return strtoul(integerString, &end, radix);
      /*    for (;integerString[i];i++)
    {
        digit = toupper(integerString[i]) - T_CString_toffst((char)toupper(integerString[i]));
        if ((digit<0)||(digit>=radix))  {
            return 0;
        }
        integer = integer*radix + digit;
    }
    
    if (sign) return (-1)*integer;
    else return integer;*/
}
    

