/*
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
 *  ucnv_cb.c:
 *  External APIs for the ICU's codeset conversion library
 *  Helena Shih
 * 
 * Modification History:
 *
 *   Date        Name        Description 
 *   7/28/2000   srl         Implementation
 */

/**
 * @name Character Conversion C API
 *
 */

#include "unicode/utypes.h"
#include "unicode/ucnv_cb.h"
#include "ucnv_bld.h"
#include "cmemory.h"

/* need to update the offsets when the target moves. */
/* Note: Recursion may occur in the cb functions, be sure to update the offsets correctly
if you don't use ucnv_cbXXX functions.  Make sure you don't use the same callback within
the same call stack if the complexity arises. */
void ucnv_cbFromUWriteBytes (UConverterFromUnicodeArgs *args,
                       const char* source,
                       int32_t length,
                       int32_t offsetIndex,
                       UErrorCode * err)
{
  int32_t togo;
  int8_t toerr;
  int32_t i;

  if((args->targetLimit - args->target) >= length) /* If the buffer fits.. */
  {
    uprv_memcpy(args->target, source, length);
    args->target += length;
    if(args->offsets) /* set all the offsets to the same # */
    {
      for(i=0;i<length;i++)
      {
        *(args->offsets++) = offsetIndex;
      }
    }
  }
  else
  {
    togo = args->targetLimit - args->target;

    uprv_memcpy(args->target, source, togo);
    args->target += togo;
    
    if(args->offsets)
    {
      for(i=0;i<togo;i++)
      {
        *(args->offsets++) = offsetIndex;
      }
    }

    /* Now, copy the remainder into the errbuff */
    source += togo;
    toerr = (int8_t)(length - togo);
    
    uprv_memcpy(args->converter->charErrorBuffer + 
                args->converter->charErrorBufferLength, 
                source,
                toerr * sizeof(source[0]));
    args->converter->charErrorBufferLength += toerr;

    *err = U_INDEX_OUTOFBOUNDS_ERROR;

  }
}

void ucnv_cbFromUWriteUChars(UConverterFromUnicodeArgs *args,
                             const UChar** source,
                             const UChar*  sourceLimit,
                             int32_t offsetIndex,
                             UErrorCode * err)
{
  /*
    This is a fun one.  Recursion can occur - we're basically going to 
    just retry shoving data through the same converter. Note, if you got 
    here through some kind of invalid sequence, you maybe should emit a 
    reset sequence of some kind and/or call ucnv_reset().  Since this
    IS an actual conversion, take care that you've changed the callback
    or the data, or you'll get an infinite loop.
    
    Please set the err value to something reasonable before calling
    into this.
  */

  char *oldTarget;

  if(U_FAILURE(*err))
  {
    return;
  }

  oldTarget = args->target;

  ucnv_fromUnicode(args->converter,
                   &args->target,
                   args->targetLimit,
                   source,
                   sourceLimit,
                   NULL, /* no offsets */
                   FALSE, /* no flush */
                   err);

  if(args->offsets) 
  {
    while (args->target != oldTarget)  /* if it moved at all.. */
    {
      *(args->offsets)++ = offsetIndex;
      oldTarget++;
    }
  }

  /* Note, if you did something like used a Stop subcallback, things would get interesting.  
     In fact, here's where we want to return the partially consumed in-source! 
  */
  if(*err == U_INDEX_OUTOFBOUNDS_ERROR)
         /* && (*source < sourceLimit && args->target >= args->targetLimit) 
                    -- S. Hrcek */
  {
    /* Overflowed the target.  Now, we'll write into the charErrorBuffer.
       It's a fixed size. If we overflow it... Hmm */
    char *newTarget;
    const char *newTargetLimit;
    UErrorCode err2 = U_ZERO_ERROR;

    int8_t errBuffLen;

    errBuffLen  = args->converter->charErrorBufferLength;

    /* start the new target at the first free slot in the errbuff.. */
    newTarget = (char *)(args->converter->charErrorBuffer + errBuffLen);
      
    newTargetLimit = (char *)(args->converter->charErrorBuffer +
      sizeof(args->converter->charErrorBuffer));

    if(newTarget >= newTargetLimit) 
    {
      *err = U_INTERNAL_PROGRAM_ERROR;
      return;
    }

    /* We're going to tell the converter that the errbuff len is empty.
       This prevents the existing errbuff from being 'flushed' out onto
       itself.  If the errbuff is needed by the converter this time, 
       we're hosed - we're out of space! */

    args->converter->charErrorBufferLength = 0;
    
    ucnv_fromUnicode(args->converter,
                     &newTarget, 
                     newTargetLimit,
                     source,
                     sourceLimit,
                     NULL,
                     FALSE,
                     &err2);

    /* We can go ahead and overwrite the  length here. We know just how
       to recalculate it. */

    args->converter->charErrorBufferLength = (int8_t)(
      newTarget - (char*)args->converter->charErrorBuffer);

    if((newTarget >= newTargetLimit) || (err2 == U_INDEX_OUTOFBOUNDS_ERROR))
    {
      /* now we're REALLY in trouble.
         Internal program error - callback oughtn't to have written this much
         data!
      */
      *err = U_INTERNAL_PROGRAM_ERROR;
      return;
    }
    else
    {
      /* sub errs could be invalid/truncated/illegal chars or w/e.
         These might want to be passed on up.. But the problem is, we already
         need to pass U_INDEXOUTOFBOUNDS_ERROR. That has to override these 
         other errs.. */

      /*
          if(U_FAILURE(err2))
                ??
      */
    }
  }
}

void ucnv_cbFromUWriteSub (UConverterFromUnicodeArgs *args,
                           int32_t offsetIndex,
                       UErrorCode * err)
{
    char togo[5];
    int32_t togoLen;

    if(U_FAILURE(*err))
    {
      return;
    }

    /*In case we're dealing with a modal converter a la UCNV_EBCDIC_STATEFUL,
    we need to make sure that the emitting of the substitution charater in the right mode*/
    uprv_memcpy(togo, args->converter->subChar, togoLen = args->converter->subCharLen);
    if (ucnv_getType(args->converter) == UCNV_EBCDIC_STATEFUL)
    {
        if ((args->converter->fromUnicodeStatus)&&(togoLen != 2))
        {
            togo[0] = UCNV_SI;
            togo[1] = args->converter->subChar[0];
            togo[2] = UCNV_SO;
            togoLen = 3;
        }
        else if (!(args->converter->fromUnicodeStatus)&&(togoLen != 1))
        {
            togo[0] = UCNV_SO;
            togo[1] = args->converter->subChar[0];
            togo[2] = args->converter->subChar[1];
            togo[3] = UCNV_SI;
            togoLen = 4;
        }
    }

    /*if we have enough space on the output buffer we just copy
    the subchar there and update the pointer */  
    ucnv_cbFromUWriteBytes(args, togo, togoLen, offsetIndex, err);


    return;
}

void ucnv_cbToUWriteUChars (UConverterToUnicodeArgs *args,
                            const UChar* source,
                            int32_t length,
                            int32_t offsetIndex,
                            UErrorCode * err)
{
  int32_t togo;
  int8_t toerr;
  int32_t i;
  
  if(U_FAILURE(*err))
  {
    return;
  }


  if((args->targetLimit - args->target) >= length) /* If the buffer fits.. */
  {
    uprv_memcpy(args->target, source, length * sizeof(args->target[0]) );
    args->target += length;
    if(args->offsets) /* set all the offsets to the same # */
    {
      for(i=0;i<length;i++)
      {
        *(args->offsets++) = offsetIndex;
      }
    }
  }
  else
  {
    togo = args->targetLimit - args->target;

    uprv_memcpy(args->target, source, togo * sizeof(args->target[0])  );
    args->target += togo;
    
    if(args->offsets) 
    {
      for(i=0;i<togo;i++)
      {
        *(args->offsets++) = offsetIndex;
      }
    }

    /* Now, copy the remainder into the errbuff */
    source += togo;
    toerr = (int8_t)(length - togo);
    
    uprv_memcpy(args->converter->UCharErrorBuffer + 
                args->converter->UCharErrorBufferLength, 
                source,
                toerr * sizeof(source[0]));
    args->converter->UCharErrorBufferLength += toerr;

    *err = U_INDEX_OUTOFBOUNDS_ERROR;
  }
}

void ucnv_cbToUWriteSub (UConverterToUnicodeArgs *args,
                         int32_t offsetIndex,
                       UErrorCode * err)
{
  static const UChar kSubstituteChar  = 0xFFFD ;

  /* could optimize this case, just one uchar */
  ucnv_cbToUWriteUChars(args, &kSubstituteChar, 1, offsetIndex, err);
}
