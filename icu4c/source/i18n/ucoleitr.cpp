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

#include "unicode/ucoleitr.h"
#include "unicode/ustring.h"
#include "unicode/sortkey.h"
#include "ucolimp.h"
#include "cmemory.h"

#define BUFFER_LENGTH 100

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

  if (U_FAILURE(*status))
    return NULL;

  result = (UCollationElements *)uprv_malloc(sizeof(UCollationElements));

  result->collator_ = coll;
  
  /* gets the correct length of the null-terminated string */
  if (textLength == -1)
    textLength = u_strlen(text);

  result->length_ = textLength;
  result->reset_   = TRUE;
  init_collIterate(text, textLength, &result->iteratordata_, FALSE);

  return result;
}

U_CAPI void
ucol_closeElements(UCollationElements *elems)
{
  collIterate *ci = &elems->iteratordata_;
  if (ci->writableBuffer != ci->stackWritableBuffer)
    uprv_free(ci->writableBuffer);
  if (elems->iteratordata_.isWritable && elems->iteratordata_.string != NULL)
    uprv_free(elems->iteratordata_.string);
  uprv_free(elems);
}

U_CAPI void
ucol_reset(UCollationElements *elems)
{
  collIterate *ci = &(elems->iteratordata_);
  elems->reset_   = TRUE;
  ci->pos         = ci->string;
  ci->len         = ci->string + elems->length_;
  ci->CEpos       = ci->toReturn = ci->CEs;
  
  ci->isThai      = TRUE;
  if (ci->stackWritableBuffer != ci->writableBuffer)
  {
    uprv_free(ci->writableBuffer);
    ci->writableBuffer = ci->stackWritableBuffer;
  }
}

U_CAPI int32_t
ucol_next(UCollationElements *elems,
          UErrorCode         *status)
{
  int32_t result;
  if (U_FAILURE(*status)) 
    return UCOL_NULLORDER;

  elems->reset_ = FALSE;
  
  UCOL_GETNEXTCE(result, elems->collator_, elems->iteratordata_, status);

  /* testing
  if ((elems->iteratordata_).CEpos > (elems->iteratordata_).toReturn) 
    {                       
      result = *((elems->iteratordata_).toReturn++);                                      
      if ((elems->iteratordata_).CEpos == (elems->iteratordata_).toReturn)
        (elems->iteratordata_).CEpos = (elems->iteratordata_).toReturn = 
        (elems->iteratordata_).CEs; 
    } 
    else 
      if ((elems->iteratordata_).pos < (elems->iteratordata_).len) 
      {                        
        UChar ch = *(elems->iteratordata_).pos++;     
        if (ch <= 0xFF)
          (result) = (elems->collator_)->latinOneMapping[ch];                                          
        else
          (result) = ucmp32_get((elems->collator_)->mapping, ch);                                      
                                                                                    
        if((result) >= UCOL_NOT_FOUND) 
        {
          (result) = getSpecialCE((elems->collator_), (result), 
                                  &(elems->iteratordata_), (status));        
          if ((result) == UCOL_NOT_FOUND)
            (result) = ucol_getNextUCA(ch, &(elems->iteratordata_), (status));                                                                            
        }                                                                               
      } 
      else
        (result) = UCOL_NO_MORE_CES;
  */
  
  if (result == UCOL_NO_MORE_CES)
    result = UCOL_NULLORDER;
  return result;
}

U_CAPI int32_t
ucol_previous(UCollationElements *elems,
              UErrorCode         *status)
{
  if(U_FAILURE(*status)) 
    return UCOL_NULLORDER;
  else
  {
    int32_t result;

    if (elems->reset_ && 
        (elems->iteratordata_.pos == elems->iteratordata_.string))
      elems->iteratordata_.pos = elems->iteratordata_.len;

    elems->reset_ = FALSE;

    UCOL_GETPREVCE(result, elems->collator_, elems->iteratordata_, 
                   elems->length_, status);

    /* synwee : to be removed, only for testing 
    
    const UCollator   *coll  = elems->collator_;
          collIterate *data  = &(elems->iteratordata_);
          int32_t     length = elems->length_;

    if (data->CEpos > data->CEs) 
    {              
      data->toReturn --;
      (result) = *(data->toReturn);                                           
      if (data->CEs == data->toReturn)                                
        data->CEpos = data->toReturn = data->CEs; 
    }                                                                          
    else 
    {                    
      if (data->pos == data->string || data->pos == data->writableBuffer)
        (result) = UCOL_NO_MORE_CES;                                                                                                                    
      else 
      {                  
        data->pos --;                                 
      
        UChar ch = *(data->pos);
        if (ch <= 0xFF)                                                
          (result) = (coll)->latinOneMapping[ch];                                                                                       
        else
          (result) = ucmp32_get((coll)->mapping, ch);                           
                                                                       
        if ((result) >= UCOL_NOT_FOUND) 
        {
          (result) = getSpecialPrevCE(coll, result, data, length, status);      
          if ((result) == UCOL_NOT_FOUND)
            (result) = ucol_getPrevUCA(ch, data, length, status);                                      
        }                                                                      
      }                                                                        
    } 
    */
    
    if (result == UCOL_NO_MORE_CES)
      result = UCOL_NULLORDER;

    return result;
  }
}

U_CAPI int32_t
ucol_getMaxExpansion(const UCollationElements *elems,
                           int32_t            order)
{
  uint8_t result;
  UCOL_GETMAXEXPANSION(elems->collator_, order, result);
  return result;
}
 
U_CAPI void
ucol_setText(      UCollationElements *elems,
             const UChar              *text,
                   int32_t            textLength,
                   UErrorCode         *status)
{
  if (U_FAILURE(*status)) 
    return;
  
  /* gets the correct length of the null-terminated string */
  if (textLength == -1)
    textLength = u_strlen(text);

  elems->length_ = textLength;

  if (elems->iteratordata_.isWritable && elems->iteratordata_.string != NULL)
    uprv_free(elems->iteratordata_.string);
  init_collIterate(text, textLength, &elems->iteratordata_, FALSE);

  elems->reset_   = TRUE;
}

U_CAPI UTextOffset
ucol_getOffset(const UCollationElements *elems)
{
  const collIterate *ci = &(elems->iteratordata_);
  if (ci->isThai == TRUE)
    return ci->pos - ci->string;

  return ci->pos - ci->writableBuffer;
}

U_CAPI void
ucol_setOffset(UCollationElements    *elems,
               UTextOffset           offset,
               UErrorCode            *status)
{
  if (U_FAILURE(*status)) 
    return;

  collIterate *ci = &(elems->iteratordata_);
  ci->pos         = ci->string + offset;
  ci->CEpos       = ci->toReturn = ci->CEs;
  ci->isThai      = TRUE;
  if (ci->stackWritableBuffer != ci->writableBuffer)
  {
    uprv_free(ci->writableBuffer);
    ci->writableBuffer = ci->stackWritableBuffer;
  }
}





