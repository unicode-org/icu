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
* File ustdio.c
*
* Modification History:
*
*   Date        Name        Description
*   11/18/98    stephen	    Creation.
*   03/12/99    stephen     Modified for new C API.
*******************************************************************************
*/

#include "ustdio.h"
#include "ufile.h"
#include "ucnv.h"
#include "ustring.h"
#include "cmemory.h"

static const UChar DELIMITERS [] = { 0x000A, 0x0000 };

#define DELIM_CR 0x000D
#define DELIM_LF 0x000A

#define IS_STRING_DELIMITER(s)	(bool_t)(	(s) == DELIM_CR || \
						(s) == DELIM_LF	)


/* Input/output */

int32_t
u_fputs(const UChar *s,
	UFILE *f,
	UErrorCode *status)
{
  int32_t count;

  if(FAILURE(*status)) return 0;
  
  count = u_file_write(s, u_strlen(s), f, status);
  count += u_file_write(DELIMITERS, u_strlen(DELIMITERS), f, status);
  return count;
}

int32_t
u_fputc(UChar uc,
	UFILE *f,
	UErrorCode *status)
{
  if(FAILURE(*status)) return U_EOF;

  return u_file_write(&uc, 1, f, status) == 1 ? uc : U_EOF;
}

int32_t
u_file_write(const UChar *chars, 
	     int32_t count, 
	     UFILE *f,
	     UErrorCode *status)
{
  /* Set up conversion parameters */
  const UChar		*mySource   	= chars;
  const UChar		*sourceAlias   	= chars;
  const UChar		*mySourceEnd 	= chars + count;
  char			*myTarget 	= f->fCharBuffer;
  int32_t		bufferSize	= UFILE_CHARBUFFER_SIZE;
  int32_t		written		= 0;
 
  if(FAILURE(*status)) return U_EOF;
  
  /* Perform the conversion in a loop */
  do {
    *status 	= ZERO_ERROR;
    sourceAlias = mySource;
    
    ucnv_fromUnicode(f->fConverter,
		     &myTarget, 
		     f->fCharBuffer + bufferSize,
		     &mySource,
		     mySourceEnd,
		     NULL,
		     TRUE,
		     status);

    /* write the converted bytes */
    T_FileStream_write(f->fFile, 
		       f->fCharBuffer, 
		       sizeof(char) * (myTarget - f->fCharBuffer));
    
    written 	+= (myTarget - f->fCharBuffer);
    myTarget 	= f->fCharBuffer;
  }
  while(*status == INDEX_OUTOFBOUNDS_ERROR); 

  /* return # of chars written */
  return written;
}

UChar*
u_fgets(UFILE *f,
	int32_t n,
	UChar *s,
	UErrorCode *status)
{
  int32_t dataSize;
  int32_t read;
  int32_t count;
  UChar *alias;

  if(FAILURE(*status)) return 0;

  /* subtract 1 from n to compensate for the terminator */
  --n;

  /* determine the amount of data in the buffer */
  dataSize = f->fUCLimit - f->fUCPos;

  /* if the buffer contains more data than requested, operate on the buffer */
  if(dataSize > n) {

    /* find the first occurrence of a delimiter character */
    alias = f->fUCPos;
    count = 0;
    while( ! IS_STRING_DELIMITER(*alias) && count < n) {
      ++count;
      ++alias;
    }

    /* copy the characters into the target*/
    icu_memcpy(s, f->fUCPos, count * sizeof(UChar));

    /* add the terminator */
    s[count] = 0x0000;

    /* update the current buffer position */
    f->fUCPos += count;

    /* refill the buffer */
    ufile_fill_uchar_buffer(f, status);
    if(FAILURE(*status)) return 0;
    
    /* skip over any remaining delimiters */
    while(IS_STRING_DELIMITER(*(f->fUCPos)) && f->fUCPos < f->fUCLimit)
      ++(f->fUCPos);
    
    /* return s */
    return s;
  }

  /* otherwise, iteratively fill the buffer and copy */
  read = 0;
  do {
    
    /* refill the buffer */
    ufile_fill_uchar_buffer(f, status);
    if(FAILURE(*status)) return 0;
    
    /* determine the amount of data in the buffer */
    dataSize = f->fUCLimit - f->fUCPos;

    /* find the first occurrence of a delimiter character, if present */
    alias = f->fUCPos;
    count = 0;
    while( ! IS_STRING_DELIMITER(*alias) && alias < f->fUCLimit && count < n) {
      ++count;
      ++alias;
    }
    
    /* copy the current data in the buffer */
    memcpy(s + read, f->fUCPos, count * sizeof(UChar));
    
    /* update number of items read */
    read += count;

    /* update the current buffer position */
    f->fUCPos += count;

    /* if we found a delimiter */
    if(alias < f->fUCLimit) {

      /* refill the buffer */
      ufile_fill_uchar_buffer(f, status);
      if(FAILURE(*status)) return 0;

      /* skip over any remaining delimiters */
      while(IS_STRING_DELIMITER(*(f->fUCPos)) && f->fUCPos < f->fUCLimit)
	++(f->fUCPos);
      
      /* break out */
      break;
    }

  } while(dataSize != 0 && read < n);

  /* if 0 characters were read, return 0 */
  if(read == 0)
    return 0;

  /* add the terminator and return s */
  s[read] = 0x0000;
  return s;
}

UChar
u_fgetc(UFILE *f,
	UErrorCode *status)
{
  if(FAILURE(*status)) return U_EOF;

  /* if we have an available character in the buffer, return it */
  if(f->fUCPos < f->fUCLimit)
    return *(f->fUCPos)++;
  /* otherwise, fill the buffer and return the next character */
  else {
    ufile_fill_uchar_buffer(f, status);
    if(FAILURE(*status)) return U_EOF;
    if(f->fUCPos < f->fUCLimit) {
      return *(f->fUCPos)++;
    }
    /* at EOF */
    else {
      return U_EOF;
    }
  }
}

UChar
u_fungetc(UChar c,
	  UFILE *f,
	  UErrorCode *status)
{
  if(FAILURE(*status)) return U_EOF;

  /* if we're at the beginning of the buffer, sorry! */
  if(f->fUCPos == f->fUCBuffer) {
    *status = BUFFER_OVERFLOW_ERROR;
    return U_EOF;
  }
  /* otherwise, put the character back */
  else {
    *--(f->fUCPos) = c;
    return c;
  }
}

int32_t
u_file_read(UChar *chars, 
	    int32_t count, 
	    UFILE *f,
	    UErrorCode *status)
{
  int32_t dataSize;
  int32_t read;

  if(FAILURE(*status)) return 0;

  /* determine the amount of data in the buffer */
  dataSize = f->fUCLimit - f->fUCPos;

  /* if the buffer contains the amount requested, just copy */
  if(dataSize > count) {
    icu_memcpy(chars, f->fUCPos, count * sizeof(UChar));

    /* update the current buffer position */
    f->fUCPos += count;

    /* return # of chars read */
    return count;
  }

  /* otherwise, iteratively fill the buffer and copy */
  read = 0;
  do {
    
    /* refill the buffer */
    ufile_fill_uchar_buffer(f, status);
    if(FAILURE(*status)) return read;
    
    /* determine the amount of data in the buffer */
    dataSize = f->fUCLimit - f->fUCPos;

    /* copy the current data in the buffer */
    icu_memcpy(chars + read, f->fUCPos, dataSize * sizeof(UChar));
    
    /* update number of items read */
    read += dataSize;

    /* update the current buffer position */
    f->fUCPos += dataSize;

  } while(dataSize != 0 && read < count);
  
  return read;
}





