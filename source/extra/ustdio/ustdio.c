/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File ustdio.c
*
* Modification History:
*
*   Date        Name        Description
*   11/18/98    stephen     Creation.
*   03/12/99    stephen     Modified for new C API.
*   07/19/99    stephen     Fixed read() and gets()
*******************************************************************************
*/

#include "unicode/ustdio.h"
#include "ufile.h"
#include "ufmt_cmn.h"
#include "unicode/ucnv.h"
#include "unicode/ustring.h"

#include <string.h>

static const UChar DELIMITERS [] = { 0x000A, 0x0000 };

#define DELIM_CR 0x000D
#define DELIM_LF 0x000A

#define IS_STRING_DELIMITER(s)    (UBool)(    (s) == DELIM_CR || \
                        (s) == DELIM_LF    )


/* Input/output */

int32_t
u_fputs(const UChar    *s,
    UFILE        *f)
{
  int32_t count = u_file_write(s, u_strlen(s), f);
  count += u_file_write(DELIMITERS, u_strlen(DELIMITERS), f);
  return count;
}

int32_t
u_fputc(UChar        uc,
    UFILE        *f)
{
  return u_file_write(&uc, 1, f) == 1 ? uc : EOF;
}

int32_t
u_file_write(    const UChar     *chars, 
        int32_t        count, 
        UFILE         *f)
{
  /* Set up conversion parameters */
  UErrorCode         status        = U_ZERO_ERROR;
  const UChar        *mySource       = chars;
  const UChar        *sourceAlias       = chars;
  const UChar        *mySourceEnd     = chars + count;
  char            *myTarget     = f->fCharBuffer;
  int32_t        bufferSize    = UFILE_CHARBUFFER_SIZE;
  int32_t        written        = 0;
 
  /* Perform the conversion in a loop */
  do {
    status     = U_ZERO_ERROR;
    sourceAlias = mySource;
    if(f->fConverter != NULL) { /* We have a valid converter */
        ucnv_fromUnicode(f->fConverter,
                 &myTarget, 
                 f->fCharBuffer + bufferSize,
                 &mySource,
                 mySourceEnd,
                 NULL,
                 TRUE,
                 &status);
      } else { /*weiv: do the invariant conversion */
          u_UCharsToChars(mySource, myTarget, count);
          myTarget += count;
      }

    /* write the converted bytes */
    fwrite(f->fCharBuffer, 
       sizeof(char), 
       myTarget - f->fCharBuffer, 
       f->fFile);
    
    written     += (myTarget - f->fCharBuffer);
    myTarget     = f->fCharBuffer;
  }
  while(status == U_INDEX_OUTOFBOUNDS_ERROR); 

  /* return # of chars written */
  return written;
}

/* private function used for buffering input */
void
ufile_fill_uchar_buffer(UFILE *f)
{
  UErrorCode         status;
  const char        *mySource;
  const char        *mySourceEnd;
  UChar            *myTarget;
  int32_t        bufferSize;
  int32_t        maxCPBytes;
  int32_t        bytesRead;
  int32_t        availLength;
  int32_t        dataSize;


  /* shift the buffer if it isn't empty */
  dataSize = f->fUCLimit - f->fUCPos;
  if(dataSize != 0) {
    memmove(f->fUCBuffer, 
        f->fUCPos, 
        dataSize * sizeof(UChar));
  }
  
  /* record how much buffer space is available */
  availLength = UFILE_UCHARBUFFER_SIZE - dataSize;
  
  /* Determine the # of codepage bytes needed to fill our UChar buffer */
  /* weiv: if converter is NULL, we use invariant converter with charwidth = 1)*/
  maxCPBytes = availLength * (f->fConverter!=NULL?ucnv_getMaxCharSize(f->fConverter):1);
  
  /* Read in the data to convert */
  bytesRead = fread(f->fCharBuffer, 
            sizeof(char), 
            ufmt_min(maxCPBytes, UFILE_CHARBUFFER_SIZE), 
            f->fFile);
  
  /* Set up conversion parameters */
  status    = U_ZERO_ERROR;
  mySource       = f->fCharBuffer;
  mySourceEnd     = f->fCharBuffer + bytesRead;
  myTarget     = f->fUCBuffer + dataSize;
  bufferSize    = UFILE_UCHARBUFFER_SIZE;

  if(f->fConverter != NULL) { /* We have a valid converter */
      /* Perform the conversion */
      ucnv_toUnicode(f->fConverter,
             &myTarget, 
             f->fUCBuffer + bufferSize,
             &mySource,
             mySourceEnd,
		     NULL,
             TRUE,
             &status);
  } else { /*weiv: do the invariant conversion */
      u_charsToUChars(mySource, myTarget, bytesRead);
      myTarget += bytesRead;
  }
  
  /* update the pointers into our array */
  f->fUCPos    = f->fUCBuffer;
  f->fUCLimit     = myTarget;
}

UChar*
u_fgets(UFILE        *f,
    int32_t        n,
    UChar        *s)
{
  int32_t dataSize;
  int32_t read;
  int32_t count;
  UChar *alias;

    
  /* fill the buffer */
  ufile_fill_uchar_buffer(f);

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
      *alias++;
    }

    /* copy the characters into the target*/
    memcpy(s, f->fUCPos, count * sizeof(UChar));

    /* add the terminator */
    s[count] = 0x0000;

    /* update the current buffer position */
    f->fUCPos += count;

    /* refill the buffer */
    ufile_fill_uchar_buffer(f);
    
    /* skip over any remaining delimiters */
    while(IS_STRING_DELIMITER(*(f->fUCPos)) && f->fUCPos < f->fUCLimit)
      *(f->fUCPos)++;
    
    /* return s */
    return s;
  }

  /* otherwise, iteratively fill the buffer and copy */
  read = 0;
  do {
        
    /* determine the amount of data in the buffer */
    dataSize = f->fUCLimit - f->fUCPos;

    /* find the first occurrence of a delimiter character, if present */
    alias = f->fUCPos;
    count = 0;
    while( ! IS_STRING_DELIMITER(*alias) && alias < f->fUCLimit && count < n) {
      ++count;
      *alias++;
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
      ufile_fill_uchar_buffer(f);
      
      /* skip over any remaining delimiters */
      while(IS_STRING_DELIMITER(*(f->fUCPos)) && f->fUCPos < f->fUCLimit)
	*(f->fUCPos)++;
      
      /* break out */
      break;
    }

    /* refill the buffer */
    ufile_fill_uchar_buffer(f);

  } while(dataSize != 0 && read < n);

  /* if 0 characters were read, return 0 */
  if(read == 0)
    return 0;

  /* add the terminator and return s */
  s[read] = 0x0000;
  return s;
}

UChar
u_fgetc(UFILE        *f)
{
  /* if we have an available character in the buffer, return it */
  if(f->fUCPos < f->fUCLimit)
    return *(f->fUCPos)++;
  /* otherwise, fill the buffer and return the next character */
  else {
    ufile_fill_uchar_buffer(f);
    if(f->fUCPos < f->fUCLimit)
      return *(f->fUCPos)++;
    else
      return 0xFFFF;
  }
}

UChar
u_fungetc(UChar        c,
      UFILE        *f)
{
  /* if we're at the beginning of the buffer, sorry! */
  if(f->fUCPos == f->fUCBuffer)
    return 0xFFFF;
  /* otherwise, put the character back */
  else {
    *--(f->fUCPos) = c;
    return c;
  }
}

int32_t
u_file_read(    UChar        *chars, 
        int32_t        count, 
        UFILE         *f)
{
  int32_t dataSize;
  int32_t read;

  /* fill the buffer */
  ufile_fill_uchar_buffer(f);

  /* determine the amount of data in the buffer */
  dataSize = f->fUCLimit - f->fUCPos;

  /* if the buffer contains the amount requested, just copy */
  if(dataSize > count) {
    memcpy(chars, f->fUCPos, count * sizeof(UChar));

    /* update the current buffer position */
    f->fUCPos += count;

    /* return # of chars read */
    return count;
  }

  /* otherwise, iteratively fill the buffer and copy */
  read = 0;
  do {
    
    /* determine the amount of data in the buffer */
    dataSize = f->fUCLimit - f->fUCPos;

    /* copy the current data in the buffer */
    memcpy(chars + read, f->fUCPos, dataSize * sizeof(UChar));
    
    /* update number of items read */
    read += dataSize;

    /* update the current buffer position */
    f->fUCPos += dataSize;

    /* refill the buffer */
    ufile_fill_uchar_buffer(f);

  } while(dataSize != 0 && read < count);
  
  return read;
}
