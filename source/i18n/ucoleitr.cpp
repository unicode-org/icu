/*
******************************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
******************************************************************************
*
* File ucoleitr.cpp
*
* Modification History:
*
* Date        Name        Description
* 02/15/2001  synwee      Modified all methods to process its own function 
*                         instead of calling the equivalent c++ api (coleitr.h)
******************************************************************************/

#include <stdio.h>
#include "unicode/ucoleitr.h"
#include "unicode/ustring.h"
#include "unicode/sortkey.h"
#include "ucol_imp.h"
#include "cmemory.h"

#define BUFFER_LENGTH             100

typedef struct collIterate collIterator;

/* public methods ---------------------------------------------------- */

/**
* Since this is going to be deprecated, I'll leave it as it is
*/
U_CAPI int32_t
ucol_keyHashCode(const uint8_t *key, 
                       int32_t  length)
{
  CollationKey newKey(key, length);
  return newKey.hashCode();
}


UCollationElements*
ucol_openElements(const UCollator  *coll,
                  const UChar      *text,
                        int32_t    textLength,
                        UErrorCode *status)
{
  UCollationElements *result;

  if (U_FAILURE(*status)) {
    return NULL;
  }

  result = (UCollationElements *)uprv_malloc(sizeof(UCollationElements));

  result->reset_   = TRUE;
  result->normalization_ = UNORM_DEFAULT;

  result->isWritable = FALSE;
  init_collIterate(coll, text, textLength, &result->iteratordata_);

  return result;
}

U_CAPI void
ucol_closeElements(UCollationElements *elems)
{
  collIterate *ci = &elems->iteratordata_;
  if (ci->writableBuffer != ci->stackWritableBuffer) {
    uprv_free(ci->writableBuffer);
  }
  if (elems->isWritable && elems->iteratordata_.string != NULL)
  {
    uprv_free(elems->iteratordata_.string);
  }
  uprv_free(elems);
}

U_CAPI void
ucol_reset(UCollationElements *elems)
{
  collIterate *ci = &(elems->iteratordata_);
  elems->reset_   = TRUE;
  ci->pos         = ci->string;
  if (ci->endp == NULL) {
    ci->endp      = ci->string + u_strlen(ci->string);
  }
  ci->CEpos       = ci->toReturn = ci->CEs;
  ci->flags       = UCOL_ITER_HASLEN;
  if (ci->coll->normalizationMode == UCOL_ON) {
    ci->flags |= UCOL_ITER_NORM;
  }
  
  if (ci->stackWritableBuffer != ci->writableBuffer) {
    uprv_free(ci->writableBuffer);
    ci->writableBuffer = ci->stackWritableBuffer;
    ci->writableBufSize = UCOL_WRITABLE_BUFFER_SIZE;
  }
}

U_CAPI int32_t
ucol_next(UCollationElements *elems,
          UErrorCode         *status)
{
  uint32_t result;
  if (U_FAILURE(*status)) {
    return UCOL_NULLORDER;
  }

  elems->reset_ = FALSE;

  result = ucol_getNextCE(elems->iteratordata_.coll, &elems->iteratordata_, 
                          status);
  
  if (result == UCOL_NO_MORE_CES) {
    result = UCOL_NULLORDER;
  }
  return result;
}

U_CAPI int32_t
ucol_previous(UCollationElements *elems,
              UErrorCode         *status)
{
  if(U_FAILURE(*status)) {
    return UCOL_NULLORDER;
  }
  else
  {
    uint32_t result;

    if (elems->reset_ && 
        (elems->iteratordata_.pos == elems->iteratordata_.string)) {
        if (elems->iteratordata_.endp == NULL) {
            elems->iteratordata_.endp = elems->iteratordata_.string + 
                                        u_strlen(elems->iteratordata_.string);
            elems->iteratordata_.flags |= UCOL_ITER_HASLEN;
        }
        elems->iteratordata_.pos = elems->iteratordata_.endp;
    }

    elems->reset_ = FALSE;

    result = ucol_getPrevCE(elems->iteratordata_.coll, &(elems->iteratordata_), 
                            status);
    
    if (result == UCOL_NO_MORE_CES) {
      result = UCOL_NULLORDER;
    }

    return result;
  }
}

U_CAPI int32_t
ucol_getMaxExpansion(const UCollationElements *elems,
                           int32_t            order)
{
  uint8_t result;
  UCOL_GETMAXEXPANSION(elems->iteratordata_.coll, (uint32_t)order, result);
  return result;
}
 
U_CAPI void
ucol_setText(      UCollationElements *elems,
             const UChar              *text,
                   int32_t            textLength,
                   UErrorCode         *status)
{
  if (U_FAILURE(*status)) {
    return;
  }
  
  /* gets the correct length of the null-terminated string */
  if (textLength == -1) {
    textLength = u_strlen(text);
  }

  if (elems->isWritable && elems->iteratordata_.string != NULL)
  {
    uprv_free(elems->iteratordata_.string);
  }
 
  elems->isWritable = FALSE;
  init_collIterate(elems->iteratordata_.coll, text, textLength, &elems->iteratordata_);

  elems->reset_   = TRUE;
}

U_CAPI UTextOffset
ucol_getOffset(const UCollationElements *elems)
{
  const collIterate *ci = &(elems->iteratordata_);
  return ci->pos - ci->string;
}

U_CAPI void
ucol_setOffset(UCollationElements    *elems,
               UTextOffset           offset,
               UErrorCode            *status)
{
  if (U_FAILURE(*status)) {
    return;
  }

  /* this methods will clean up any use of the writable buffer and points to the
     original string */
  collIterate *ci = &(elems->iteratordata_);
  ci->pos         = ci->string + offset;
  ci->CEpos       = ci->toReturn = ci->CEs;
  ci->flags       = UCOL_ITER_HASLEN;
  if (ci->coll->normalizationMode == UCOL_ON) {
    ci->flags |= UCOL_ITER_NORM;
  }
  if (ci->stackWritableBuffer != ci->writableBuffer)
  {
    uprv_free(ci->writableBuffer);
    /* reseting UCOL_ITER_INNORMBUF */
    ci->writableBuffer = ci->stackWritableBuffer;
  }
}





