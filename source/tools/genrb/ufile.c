/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File ufile.c
*
* Modification History:
*
*   Date        Name        Description
*   11/19/98    stephen	    Creation.
*   03/12/99    stephen     Modified for new C API.
*******************************************************************************
*/

#include <stdio.h>
#include "ustdio.h"
#include "ufile.h"
#include "cmemory.h"

#define MIN(a,b) (a < b ? a : b)
#define MAX(a,b) (a > b ? a : b)

UFILE*
u_finit(FileStream *f,
	UErrorCode *status)
{
  UFILE *result	= (UFILE*) icu_malloc(sizeof(UFILE));
  if(result == 0) {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return 0;
  }

  result->fFile 	= f;
  result->fOwnFile 	= FALSE;
  result->fUCPos 	= result->fUCBuffer;
  result->fUCLimit 	= result->fUCBuffer;
  
  result->fConverter = ucnv_open(0, status);
  if(U_FAILURE(*status) || result->fConverter == 0) {
    T_FileStream_close(result->fFile);
    icu_free(result);
    return 0;
  }

  return result;
}

void
u_fclose(UFILE *file)
{
  if(file->fOwnFile)
    T_FileStream_close(file->fFile);

  ucnv_close(file->fConverter);
  icu_free(file);
}

/* private function used for buffering input */
void
ufile_fill_uchar_buffer(UFILE *f,
			UErrorCode *status)
{
  const char		*mySource;
  const char		*mySourceEnd;
  UChar			*myTarget;
  int32_t		bufferSize;
  int32_t		maxCPBytes;
  int32_t		bytesRead;
  int32_t		availLength;
  int32_t		dataSize;

  if(U_FAILURE(*status)) return;

  /* shift the buffer if it isn't empty */
  dataSize = f->fUCLimit - f->fUCPos;
  if(dataSize != 0) {
    icu_memmove(f->fUCBuffer, 
		f->fUCPos, 
		dataSize * sizeof(UChar));
  }
  
  /* record how much buffer space is available */
  availLength = UFILE_UCHARBUFFER_SIZE - dataSize;
  
  /* Determine the # of codepage bytes needed to fill our UChar buffer */
  maxCPBytes = availLength * ucnv_getMaxCharSize(f->fConverter);
  
  /* Read in the data to convert */
  bytesRead = T_FileStream_read(f->fFile,f->fCharBuffer, 
		    MIN(maxCPBytes, UFILE_CHARBUFFER_SIZE));
  
  /* Set up conversion parameters */
  *status	= U_ZERO_ERROR;
  mySource   	= f->fCharBuffer;
  mySourceEnd 	= f->fCharBuffer + bytesRead;
  myTarget 	= f->fUCBuffer + dataSize;
  bufferSize	= UFILE_UCHARBUFFER_SIZE;

  /* Perform the conversion */
  ucnv_toUnicode(f->fConverter,
		 &myTarget, 
		 f->fUCBuffer + bufferSize,
		 &mySource,
		 mySourceEnd,
		 NULL,
		 TRUE,
		 status);
  
  /* update the pointers into our array */
  f->fUCPos	= f->fUCBuffer;
  f->fUCLimit 	= myTarget;
}
