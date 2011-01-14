/*
**********************************************************************
* Copyright (C) 1998-2011, International Business Machines Corporation
* and others.  All Rights Reserved.
**********************************************************************
*
* File date.c
*
* Modification History:
*
*   Date        Name        Description
*   2011-Jan-16 srl         Created.
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/uchar.h"

int main()
{
  UErrorCode status = U_ZERO_ERROR;

  printf("u_iscntrl(U+%04X)=%d\n", 0x0009, u_iscntrl(0x0009));
  printf("u_iscntrl(U+%04X)=%d\n", 0x0020, u_iscntrl(0x0020));
  printf("u_tolower(U+%04X)=U+%04X\n", 0x2C1F, u_tolower(0x2C1F));
  printf("u_tolower(U+%04X)=U+%04X\n", 0xA65C, u_tolower(0xA65C));
  printf("Pure C test OK: %s\n", u_errorName(status));
  return status;
}
