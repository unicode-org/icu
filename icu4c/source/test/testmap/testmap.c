/*
**********************************************************************
* Copyright (C) 1998-2000, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************
*
* File date.c
*
* Modification History:
*
*   Date        Name        Description
*   4/26/2000  srl         created
*******************************************************************************
*/

#include <stdio.h>
#include <string.h>
#include "unicode/udata.h"
#include "unicode/ucnv.h"

#ifdef UDATA_MAP_DLL
extern const uint8_t icudata_dat[]; 
#endif

int
main(int argc,
     char **argv)
{
  UConverter *c;

  int32_t month = -1, year = -1;
  UErrorCode status = U_ZERO_ERROR;
  UErrorCode status2 = U_ZERO_ERROR;

#ifdef UDATA_MAP_DLL
  printf("MAPPED DLL, memory mapped code path present.\n");

  udata_setCommonData(icudata_dat, &status);  

  printf("setCommonData(%p) -> %s\n", icudata_dat, u_errorName(status));

  if(U_FAILURE(status))
  {
    printf("*** FAIL: should have returned U_ZERO_ERROR\n");
    return 1;
  }

#else

#ifdef UDATA_MAP
  printf("memory mapped code path present. Mapped DLL not built.\n"); 
  udata_setCommonData(NULL, &status);
  printf("setCommonData(NULL) -> %s [should fail]\n",  u_errorName(status));

  if(status != U_ILLEGAL_ARGUMENT_ERROR)
  {
    printf("*** FAIL: should have returned U_ILLEGAL_ARGUMENT_ERROR\n");
    return 1;
  }

# else /* non map .. DLL ? */
  
  printf("Mapped DLL not built. Memory mapped code path NOT present.\n");
# ifdef UDATA_DLL
  printf("DLL code path present!\n");
# endif

  udata_setCommonData(NULL, &status);
  printf("setCommonData(NULL) -> %s [should fail]\n",  u_errorName(status));

  if(status != U_UNSUPPORTED_ERROR)
  {
    printf("\n*** FAIL: should have returned U_UNSUPPORTED_ERROR\n");
    return 1;
  }

#endif
  
#endif

  c = ucnv_open("shift_jis", &status);
	  
  printf("ucnv_open(shift_jis)-> %p, err = %s, name=%s\n", c, u_errorName(status), (!c)?"?":ucnv_getName(c,&status2)  );

#ifdef UDATA_MAP_DLL
  if(status != U_ZERO_ERROR)
  {
    printf("\n*** FAIL: should have returned U_ZERO_ERROR;\n");
    return 1;
  }

  udata_setCommonData(icudata_dat, &status);
  printf("setCommonData(%p) -> %s [should fail]\n", icudata_dat, u_errorName(status));
  
  if ( status != U_USING_DEFAULT_ERROR )
  {
    printf("\n*** FAIL: should have returned U_USING_DEFAULT_ERROR\n");
    return 1;
  }

#else  
  if(status == U_ZERO_ERROR)
  {
    printf("\n*** FAIL: should have failed.\n");
    return 1;
  }
#endif


  printf("\n*** PASS PASS PASS, test PASSED!!!!!!!!\n");

  return 0;
}
