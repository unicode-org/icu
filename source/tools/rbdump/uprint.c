/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998, 1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File date.c
*
* Modification History:
*
*   Date        Name        Description
*   06/14/99    stephen     Creation.
*******************************************************************************
*/

#include "uprint.h"

#include "ucnv.h"
#include "ustring.h"

#define BUF_SIZE 128

/* Print a ustring to the specified FILE* in the default codepage */
void
uprint(const UChar *s,
       FILE *f,
       UErrorCode *status)
{
  /* converter */
  UConverter *converter;
  char buf [BUF_SIZE];
  int32_t sourceLen;
  const UChar *mySource;
  const UChar *mySourceEnd;
  char *myTarget;
  int32_t arraySize;

  if(s == 0) return;

  /* set up the conversion parameters */
  sourceLen    = u_strlen(s);
  mySource     = s;
  mySourceEnd  = mySource + sourceLen;
  myTarget     = buf;
  arraySize    = BUF_SIZE;

  /* open a default converter */
  converter = ucnv_open(0, status);
  
  /* if we failed, clean up and exit */
  if(FAILURE(*status)) goto finish;
  
  /* perform the conversion */
  do {
    /* reset the error code */
    *status = ZERO_ERROR;

    /* perform the conversion */
    ucnv_fromUnicode(converter, &myTarget,  myTarget + arraySize,
		     &mySource, mySourceEnd, NULL,
		     TRUE, status);

    /* Write the converted data to the FILE* */
    fwrite(buf, sizeof(char), myTarget - buf, f);

    /* update the conversion parameters*/
    myTarget     = buf;
    arraySize    = BUF_SIZE;
  }
  while(*status == INDEX_OUTOFBOUNDS_ERROR); 

 finish:
  
  /* close the converter */
  ucnv_close(converter);
}
