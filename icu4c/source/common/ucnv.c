/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
*  ucnv.c:
*  Implements APIs for the ICU's codeset conversion library
*  mostly calls through internal functions created and maintained 
*  by Bertrand A. Damiba
*
* Modification History:
*
*   Date        Name        Description
*   04/04/99    helena      Fixed internal header inclusion.
*   05/09/00    helena      Added implementation to handle fallback mappings.
*   06/20/2000  helena      OS/400 port changes; mostly typecast.
*/
#include "umutex.h"
#include "unicode/ures.h"
#include "uhash.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "ucnv_io.h"
#include "unicode/ucnv_err.h"
#include "ucnv_cnv.h"
#include "ucnv_imp.h"
#include "unicode/ucnv.h"
#include "cmemory.h"
#include "cstring.h"
#include "unicode/ustring.h"
#include "unicode/uloc.h"
#include "ucnv_bld.h"

#define CHUNK_SIZE 5*1024

/* Internal function : begin */
static int32_t ucnv_getAmbiguousCCSID (const UConverter* cnv);
/* Internal function : end */

static void T_UConverter_fromCodepageToCodepage (UConverter * outConverter,
						 UConverter * inConverter,
						 char **target,
						 const char *targetLimit,
						 const char **source,
						 const char *sourceLimit,
						 int32_t* offsets,
						 UBool flush,
						 UErrorCode * err);


const char* ucnv_getDefaultName ()
{
  return ucnv_io_getDefaultConverterName();
}

void   ucnv_setDefaultName (const char *converterName)
{
  ucnv_io_setDefaultConverterName(converterName);
}
/*Calls through createConverter */
UConverter* ucnv_open (const char *name,
		       UErrorCode * err)
{
  if (err == NULL || U_FAILURE (*err)) {
    return NULL;
  }

  return createConverter (name, err);
}

/*Extracts the UChar* to a char* and calls through createConverter */
UConverter*  ucnv_openU (const UChar * name,
			 UErrorCode * err)
{
  char asciiName[UCNV_MAX_CONVERTER_NAME_LENGTH];
  
  if (U_FAILURE (*err))
    return NULL;
  if (name == NULL)
    return ucnv_open (NULL, err);
  if (u_strlen (name) > UCNV_MAX_CONVERTER_NAME_LENGTH)
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return NULL;
    }
  return ucnv_open (u_austrcpy (asciiName, name), err);
}

/*Assumes a $platform-#codepage.$CONVERTER_FILE_EXTENSION scheme and calls
 *through createConverter*/
UConverter*  ucnv_openCCSID (int32_t codepage,
			     UConverterPlatform platform,
			     UErrorCode * err)
{
  char myName[UCNV_MAX_CONVERTER_NAME_LENGTH];

  if (U_FAILURE (*err))
    return NULL;

  copyPlatformString (myName, platform);
  uprv_strcat (myName, "-");
  T_CString_integerToString (myName + uprv_strlen (myName), codepage, 10);


  return createConverter (myName, err);
}

/*Decreases the reference counter in the shared immutable section of the object
 *and frees the mutable part*/

void ucnv_close (UConverter * converter)
{
  /* first, notify the callback functions that the converter is closed */
  UConverterToUnicodeArgs toUArgs = {
    sizeof(UConverterToUnicodeArgs),
    TRUE,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
  };
  UConverterFromUnicodeArgs fromUArgs = {
    sizeof(UConverterFromUnicodeArgs),
    TRUE,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
  };
  UErrorCode errorCode;

  if (converter == NULL)
  {
    return;
  }

  toUArgs.converter = fromUArgs.converter = converter;
  errorCode = U_ZERO_ERROR;
  converter->fromCharErrorBehaviour(converter->toUContext, &toUArgs, NULL, 0, UCNV_CLOSE, &errorCode);
  errorCode = U_ZERO_ERROR;
  converter->fromUCharErrorBehaviour(converter->fromUContext, &fromUArgs, NULL, 0, 0, UCNV_CLOSE, &errorCode);

  if (converter->sharedData->impl->close != NULL) {
    converter->sharedData->impl->close(converter);
  }

  if (converter->sharedData->referenceCounter != ~0) {
    umtx_lock (NULL);
    if (converter->sharedData->referenceCounter != 0) {
      converter->sharedData->referenceCounter--;
    }
    umtx_unlock (NULL);
  }
  uprv_free (converter);

  return;
}

/*Frees all shared immutable objects that aren't referred to (reference count = 0)
 */
int32_t  ucnv_flushCache ()
{
  UConverterSharedData *mySharedData = NULL;
  int32_t pos = -1;
  int32_t tableDeletedNum = 0;
  const UHashElement *e;

  /*if shared data hasn't even been lazy evaluated yet
   * return 0
   */
  if (SHARED_DATA_HASHTABLE == NULL)
    return 0;

  /*creates an enumeration to iterate through every element in the
   *table
   */
  umtx_lock (NULL);
  while ((e = uhash_nextElement (SHARED_DATA_HASHTABLE, &pos)) != NULL)
    {
      mySharedData = (UConverterSharedData *) e->value;
      /*deletes only if reference counter == 0 */
      if (mySharedData->referenceCounter == 0)
        {
          tableDeletedNum++;

          uhash_removeElement(SHARED_DATA_HASHTABLE, e);
          deleteSharedConverterData (mySharedData);
        }
    }
  umtx_unlock (NULL);

  return tableDeletedNum;
}

/*returns a single Name from the list, will return NULL if out of bounds
 */
const char*  ucnv_getAvailableName (int32_t n)
{
  if (0 <= n && n <= 0xffff) {
    UErrorCode err = U_ZERO_ERROR;
    const char *name = ucnv_io_getAvailableConverter((uint16_t)n, &err);
    if (U_SUCCESS(err)) {
      return name;
    }
  }
  return NULL;
}

int32_t  ucnv_countAvailable ()
{
  UErrorCode err = U_ZERO_ERROR;
  return ucnv_io_countAvailableConverters(&err);
}

U_CAPI uint16_t
ucnv_countAliases(const char *alias, UErrorCode *pErrorCode) {
    const char *p;
    return ucnv_io_getAliases(alias, &p, pErrorCode);
}


U_CAPI const char *
ucnv_getAlias(const char *alias, uint16_t n, UErrorCode *pErrorCode) {
    return ucnv_io_getAlias(alias, n, pErrorCode);
}

U_CAPI void
ucnv_getAliases(const char *alias, const char **aliases, UErrorCode *pErrorCode) {
    const char *p;
    uint16_t count=ucnv_io_getAliases(alias, &p, pErrorCode);
    while(count>0) {
        *aliases++=p;
        /* skip a name, first the canonical converter name */
        p+=uprv_strlen(p)+1;
        --count;
    }
}

U_CAPI uint16_t
ucnv_countStandards(void) {
    UErrorCode err = U_ZERO_ERROR;
    return ucnv_io_countStandards(&err);
}

U_CAPI const char *
ucnv_getStandard(uint16_t n, UErrorCode *pErrorCode) {
    return ucnv_io_getStandard(n, pErrorCode);
}

U_CAPI const char *
ucnv_getStandardName(const char *name, const char *standard, UErrorCode *pErrorCode) {
    return ucnv_io_getStandardName(name, standard, pErrorCode);
}

void   ucnv_getSubstChars (const UConverter * converter,
			   char *mySubChar,
			   int8_t * len,
			   UErrorCode * err)
{
  if (U_FAILURE (*err))
    return;

  if (*len < converter->subCharLen)	/*not enough space in subChars */
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return;
    }

  uprv_memcpy (mySubChar, converter->subChar, converter->subCharLen);	/*fills in the subchars */
  *len = converter->subCharLen;	/*store # of bytes copied to buffer */

  return;
}

void   ucnv_setSubstChars (UConverter * converter,
			   const char *mySubChar,
			   int8_t len,
			   UErrorCode * err)
{
  if (U_FAILURE (*err))
    return;

  /*Makes sure that the subChar is within the codepages char length boundaries */
  if ((len > converter->sharedData->staticData->maxBytesPerChar)
      || (len < converter->sharedData->staticData->minBytesPerChar))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }

  uprv_memcpy (converter->subChar, mySubChar, len);	/*copies the subchars */
  converter->subCharLen = len;	/*sets the new len */

  return;
}




int32_t  ucnv_getDisplayName (const UConverter * converter,
			      const char *displayLocale,
			      UChar * displayName,
			      int32_t displayNameCapacity,
			      UErrorCode * err)
{
  UChar stringToWriteBuffer[UCNV_MAX_CONVERTER_NAME_LENGTH];
  UChar const *stringToWrite;
  int32_t stringToWriteLength;
  UResourceBundle *rb = NULL;

  if (U_FAILURE (*err))
    return 0;

  /*create an RB, init the fill-in string, gets it from the RB */
  rb = ures_open (NULL, displayLocale, err);

  stringToWrite = ures_getStringByKey(rb,
			    converter->sharedData->staticData->name,
                &stringToWriteLength,
			    err);
  if (rb)
    ures_close (rb);

  if(U_FAILURE(*err))
    {
      /*Error While creating or getting resource from the resource bundle
       *use the internal name instead
       *
       *sets stringToWriteLength (which accounts for a NULL terminator)
       *and stringToWrite
       */
      stringToWriteLength = uprv_strlen (converter->sharedData->staticData->name) + 1;
      stringToWrite = u_uastrcpy (stringToWriteBuffer, converter->sharedData->staticData->name);

      /*Hides the fallback to the internal name from the user */
      if (*err == U_MISSING_RESOURCE_ERROR)
	*err = U_ZERO_ERROR;
    }

  /*At this point we have a displayName and its length
   *we want to see if it fits in the user provided params
   */

  if (stringToWriteLength <= displayNameCapacity)
    {
      /*it fits */
      u_strcpy (displayName, stringToWrite);
    }
  else
    {
      /*it doesn't fit */
      *err = U_BUFFER_OVERFLOW_ERROR;

      u_strncpy (displayName,
		 stringToWrite,
		 displayNameCapacity);
      /*Zero terminates the string */
      if (displayNameCapacity > 0)
	displayName[displayNameCapacity - 1] = 0x0000;
    }

  /*if the user provided us with a with an outputLength
   *buffer we'll store in it the theoretical size of the
   *displayString
   */
  return stringToWriteLength;
}


/*resets the internal states of a converter
 *goal : have the same behaviour than a freshly created converter
 */
void  ucnv_reset (UConverter * converter)
{
  /* first, notify the callback functions that the converter is reset */
  UConverterToUnicodeArgs toUArgs = {
    sizeof(UConverterToUnicodeArgs),
    TRUE,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
  };
  UConverterFromUnicodeArgs fromUArgs = {
    sizeof(UConverterFromUnicodeArgs),
    TRUE,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
  };
  UErrorCode errorCode;

  if(converter == NULL) {
    return;
  }
  toUArgs.converter = fromUArgs.converter = converter;
  errorCode = U_ZERO_ERROR;
  converter->fromCharErrorBehaviour(converter->toUContext, &toUArgs, NULL, 0, UCNV_RESET, &errorCode);
  errorCode = U_ZERO_ERROR;
  converter->fromUCharErrorBehaviour(converter->fromUContext, &fromUArgs, NULL, 0, 0, UCNV_RESET, &errorCode);

  /* now reset the converter itself */
  converter->toUnicodeStatus = converter->sharedData->toUnicodeStatus;
  converter->fromUnicodeStatus = 0;
  converter->UCharErrorBufferLength = 0;
  converter->charErrorBufferLength = 0;
  if (converter->sharedData->impl->reset != NULL) {
    /* call the custom reset function */
    converter->sharedData->impl->reset(converter);
  } else {
    converter->mode = UCNV_SI;
  }

  return;
}

int8_t  ucnv_getMaxCharSize (const UConverter * converter)
{
  return converter->sharedData->staticData->maxBytesPerChar;
}


int8_t  ucnv_getMinCharSize (const UConverter * converter)
{
  return converter->sharedData->staticData->minBytesPerChar;
}

const char*  ucnv_getName (const UConverter * converter, UErrorCode * err)
     
{
  if (U_FAILURE (*err))
    return NULL;

  return converter->sharedData->staticData->name;
}

int32_t  ucnv_getCCSID (const UConverter * converter,
			UErrorCode * err)
{
  if (U_FAILURE (*err))
    return -1;

  return converter->sharedData->staticData->codepage;
}


UConverterPlatform  ucnv_getPlatform (const UConverter * converter,
				 UErrorCode * err)
{
  if (U_FAILURE (*err))
    return UCNV_UNKNOWN;
  
  return (UConverterPlatform)converter->sharedData->staticData->platform;
}

U_CAPI void U_EXPORT2
    ucnv_getToUCallBack (const UConverter * converter,
                         UConverterToUCallback *action,
                         void **context)
{
  *action = converter->fromCharErrorBehaviour;
  *context = converter->toUContext;
}

U_CAPI void U_EXPORT2
    ucnv_getFromUCallBack (const UConverter * converter,
                           UConverterFromUCallback *action,
                           void **context)
{
  *action = converter->fromUCharErrorBehaviour;
  *context = converter->fromUContext;
}

void   ucnv_setToUCallBack (UConverter * converter,
					UConverterToUCallback newAction,
					void* newContext,
					UConverterToUCallback *oldAction,
					void** oldContext,
					UErrorCode * err)
{
  if (U_FAILURE (*err))
    return;
  *oldAction = converter->fromCharErrorBehaviour;
  converter->fromCharErrorBehaviour = newAction;
  *oldContext = converter->toUContext;
  converter->toUContext = newContext;
}

void   ucnv_setFromUCallBack (UConverter * converter,
					UConverterFromUCallback newAction,
					void* newContext,
					UConverterFromUCallback *oldAction,
					void** oldContext,
					UErrorCode * err)
{
  
  if (U_FAILURE (*err))
    return;
  *oldAction = converter->fromUCharErrorBehaviour;
  converter->fromUCharErrorBehaviour = newAction;
  *oldContext = converter->fromUContext;
  converter->fromUContext = newContext;
}
void   ucnv_fromUnicode (UConverter * _this,
			 char **target,
			 const char *targetLimit,
			 const UChar ** source,
			 const UChar * sourceLimit,
			 int32_t* offsets,
			 UBool flush,
			 UErrorCode * err)
{
  UConverterFromUnicodeArgs args;
  /*
   * Check parameters in for all conversions
   */
  if (U_FAILURE (*err))   return;
  if ((_this == NULL) || ((char *) targetLimit < *target) || (sourceLimit < *source))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
  

  /*
   * Deal with stored carry over data.  This is done in the common location
   * to avoid doing it for each conversion.
   */
  if (_this->charErrorBufferLength > 0)
    {
      int32_t myTargetIndex = 0;

      flushInternalCharBuffer (_this, 
			       (char *) *target,
			       &myTargetIndex,
			       targetLimit - *target,
			       offsets?&offsets:NULL,
			       err);
      *target += myTargetIndex;
      if (U_FAILURE (*err)) return;
    }

  args.converter = _this;
  args.flush = flush;
  args.offsets = offsets;
  args.source = *source;
  args.sourceLimit = sourceLimit;
  args.target = *target;
  args.targetLimit = targetLimit;
  args.size = sizeof(args);
  if (offsets) {
    if (_this->sharedData->impl->fromUnicodeWithOffsets != NULL) {
	   _this->sharedData->impl->fromUnicodeWithOffsets(&args, err);
       *source = args.source;
       *target = args.target;
       return;
    } else {
      /* all code points are of the same length */
      int32_t targetSize = targetLimit - *target;
      int32_t i, bytesPerChar = _this->sharedData->staticData->maxBytesPerChar;

      if(bytesPerChar == 1) {
        for (i=0; i<targetSize; i++) {
          args.offsets[i] = i;
        }
      } else if(bytesPerChar == 2) {
        for (i=0; i<targetSize; i++) {
          args.offsets[i] = i>>1;
        }
      } else {
        int32_t j = 0, k = bytesPerChar;

        for (i=0; i<targetSize; i++) {
          /* offsets[i] = i/bytesPerChar; -- without division */
          args.offsets[i] = j;
          if(--k == 0) {
            k = bytesPerChar;
            ++j;
          }
        }
      }
    }
  }

  /*calls the specific conversion routines */
  _this->sharedData->impl->fromUnicode(&args, err);
  *source = args.source;
  *target = args.target;
  return;
}



void   ucnv_toUnicode (UConverter * _this,
		       UChar ** target,
		       const UChar * targetLimit,
		       const char **source,
		       const char *sourceLimit,
		       int32_t* offsets,
		       UBool flush,
		       UErrorCode * err)
{
  UConverterToUnicodeArgs args;
  /*
   * Check parameters in for all conversions
   */
  if (U_FAILURE (*err))   return;
  if ((_this == NULL) || ((UChar *) targetLimit < *target) || (sourceLimit < *source))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }

  /*
   * Deal with stored carry over data.  This is done in the common location
   * to avoid doing it for each conversion.
   */
  if (_this->UCharErrorBufferLength > 0)
    {
      int32_t myTargetIndex = 0;

      flushInternalUnicodeBuffer (_this, 
				  *target,
				  &myTargetIndex,
				  targetLimit - *target,
				  offsets?&offsets:NULL,
				  err);
      *target += myTargetIndex;
      if (U_FAILURE (*err))
	return;
    }

  args.converter = _this;
  args.flush = flush;
  args.offsets = offsets;
  args.source = (char *) *source;
  args.sourceLimit = sourceLimit;
  args.target =  *target;
  args.targetLimit = targetLimit;
  args.size = sizeof(args);
  if (offsets) {
    if (_this->sharedData->impl->toUnicodeWithOffsets != NULL) {
	  _this->sharedData->impl->toUnicodeWithOffsets(&args, err);
      *source = args.source;
      *target = args.target;
	  return;
    } else {
      /* all code points are of the same length */
      int32_t targetSize = targetLimit - *target;
      int32_t i, bytesPerChar = _this->sharedData->staticData->maxBytesPerChar;

      if(bytesPerChar == 1) {
        for (i=0; i<targetSize; i++) {
          offsets[i] = i;
        }
      } else if(bytesPerChar == 2) {
        for (i=0; i<targetSize; i++) {
          offsets[i] = i<<1;
        }
      } else {
        for (i=0; i<targetSize; i++) {
          offsets[i] = i*bytesPerChar;
        }
      }
    }
  }

  /*calls the specific conversion routines */
  _this->sharedData->impl->toUnicode(&args, err); 
  
  *source = args.source;
  *target = args.target;
  return;
}

int32_t   ucnv_fromUChars (const UConverter * converter,
			   char *target,
			   int32_t targetSize,
			   const UChar * source,
               int32_t sourceSize,
			   UErrorCode * err)
{
  const UChar *mySource_limit;
  int32_t mySourceLength = sourceSize;
  UConverter myConverter;
  char *myTarget_limit;
  int32_t targetCapacity = 0;
  UConverterFromUnicodeArgs args;

  if (U_FAILURE (*err))
    return 0;

  if ((converter == NULL) || (targetSize < 0))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return 0;
    }

  /*makes a local copy of the UConverter */
  myConverter = *converter;


  /*Removes all state info on the UConverter */
  ucnv_reset (&myConverter);

  /*if the source is empty we return immediately */
  if (sourceSize == -1) {
    mySourceLength = u_strlen (source);
  } 
  if (mySourceLength == 0)
    {
      /*for consistency we still need to
       *store 0 in the targetCapacity
       *if the user requires it
       */
      return 0;
    }

  mySource_limit = source + mySourceLength;
  myTarget_limit = target + targetSize;

  /* Pin the limit to U_MAX_PTR.  NULL check is for AS/400. */
  if((myTarget_limit < target) || (myTarget_limit == NULL)) {
    myTarget_limit = (char *)U_MAX_PTR;
  }

  args.converter = &myConverter;
  args.flush = TRUE;
  args.offsets = NULL;
  args.source = source;
  args.sourceLimit = mySource_limit;
  args.target = target;
  args.targetLimit = myTarget_limit;
  args.size = sizeof(args);
  if (targetSize > 0)
    {
      /*calls the specific conversion routines */
      args.converter->sharedData->impl->fromUnicode(&args, err); 
  
      targetCapacity = args.target - target;
    }

  /*Updates targetCapacity to contain the number of bytes written to target */

  if (targetSize == 0)
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }

  /* If the output buffer is exhausted, we need to stop writing
   * to it but continue the conversion in order to store in targetSize
   * the number of bytes that was required*/
  if (*err == U_INDEX_OUTOFBOUNDS_ERROR)
    {
      char target2[CHUNK_SIZE];
      const char *target2_limit = target2 + CHUNK_SIZE;

      /*We use a stack allocated buffer around which we loop
       *(in case the output is greater than CHUNK_SIZE)
       */

      while (*err == U_INDEX_OUTOFBOUNDS_ERROR)
	{
	  *err = U_ZERO_ERROR;
      args.target = target2;
      args.targetLimit = target2_limit;
      args.converter->sharedData->impl->fromUnicode(&args, err); 

      /*updates the output parameter to contain the number of char required */
      targetCapacity += (args.target - target2) + 1;	}
      /*We will set the erro code to U_BUFFER_OVERFLOW_ERROR only if
       *nothing graver happened in the previous loop*/
      (targetCapacity)--;
      if (U_SUCCESS (*err))
	*err = U_BUFFER_OVERFLOW_ERROR;
    }

  return targetCapacity;
}

int32_t ucnv_toUChars (const UConverter * converter,
		       UChar * target,
		       int32_t targetSize,
		       const char *source,
		       int32_t sourceSize,
		       UErrorCode * err)
{
  const char *mySource_limit = source + sourceSize;
  UConverter myConverter;
  UChar *myTarget_limit;
  int32_t targetCapacity;
  UConverterToUnicodeArgs args;

  if (U_FAILURE (*err))
    return 0;

  if ((converter == NULL) || (targetSize < 0) || (sourceSize < 0))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return 0;
    }
  /*Means there is no work to be done */
  if (sourceSize == 0)
    {
      /*for consistency we still need to
       *store 0 in the targetCapacity
       *if the user requires it
       */
      if (targetSize >= 1)
	{
	  target[0] = 0x0000;
	  return 1;
	}
      else
	return 0;
    }

  /*makes a local copy of the UConverter */
  myConverter = *converter;

  /*Removes all state info on the UConverter */
  ucnv_reset (&myConverter);

  args.converter = &myConverter;
  args.flush = TRUE;
  args.offsets = NULL;
  args.source = source;
  args.sourceLimit = mySource_limit;
  args.target = target;
  args.size = sizeof(args);
  if (targetSize > 0)
  {
      myTarget_limit = target + targetSize - 1;

      /* Pin the limit to U_MAX_PTR.  NULL check is for AS/400. */
      if ((myTarget_limit == NULL) || (myTarget_limit < target)) {
          myTarget_limit = ((UChar*)U_MAX_PTR) - 1; 
      }

      /*Not in pure pre-flight mode */

      args.targetLimit = myTarget_limit;
      args.converter->sharedData->impl->toUnicode(&args, err); 

      /*Null terminates the string */
      *(args.target) = 0x0000;
    }


  /*Rigs targetCapacity to have at least one cell for zero termination */
  /*Updates targetCapacity to contain the number of bytes written to target */
  targetCapacity = 1;
  targetCapacity += args.target - target;
  if (targetSize == 0)
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
  /* If the output buffer is exhausted, we need to stop writing
   * to it but if the input buffer is not exhausted,
   * we need to continue the conversion in order to store in targetSize
   * the number of bytes that was required
   */
  if (*err == U_INDEX_OUTOFBOUNDS_ERROR)
    {
      UChar target2[CHUNK_SIZE];
      const UChar *target2_limit = target2 + CHUNK_SIZE;

      /*We use a stack allocated buffer around which we loop
         (in case the output is greater than CHUNK_SIZE) */

      while (*err == U_INDEX_OUTOFBOUNDS_ERROR)
	{
	  *err = U_ZERO_ERROR;
      args.target = target2;
      args.targetLimit = target2_limit;
      args.converter->sharedData->impl->toUnicode(&args, err); 

	  /*updates the output parameter to contain the number of char required */
	  targetCapacity += args.target - target2 + 1;
	}
      (targetCapacity)--;	/*adjust for last one */
      if (U_SUCCESS (*err))
	*err = U_BUFFER_OVERFLOW_ERROR;
    }

  return targetCapacity;
}

UChar32 ucnv_getNextUChar (UConverter * converter,
			 const char **source,
			 const char *sourceLimit,
			 UErrorCode * err)
{
  UConverterToUnicodeArgs args;
  UChar32 ch;

  if(err == NULL || U_FAILURE(*err)) {
      return 0xffff;
  }

  /* In case internal data had been stored
   * we return the first UChar32 in the internal buffer,
   * and update the internal state accordingly
   */
  if (converter->UCharErrorBufferLength > 0)
    {
      UTextOffset i = 0;
      UChar32 myUChar;
      UTF_NEXT_CHAR(converter->UCharErrorBuffer, i, sizeof(converter->UCharErrorBuffer), myUChar);
      /*In this memmove we update the internal buffer by
       *popping the first character.
         *Note that in the call itself we decrement
         *UCharErrorBufferLength
       */
      uprv_memmove (converter->UCharErrorBuffer,
		   converter->UCharErrorBuffer + i,
		   (converter->UCharErrorBufferLength - i) * sizeof (UChar));
      converter->UCharErrorBufferLength -= (int8_t)i;
      return myUChar;
    }
  /*calls the specific conversion routines */
  /*as dictated in a code review, avoids a switch statement */
  args.converter = converter;
  args.flush = TRUE;
  args.offsets = NULL;
  args.source = *source;
  args.sourceLimit = sourceLimit;
  args.target = NULL;
  args.targetLimit = NULL;
  args.size = sizeof(args);
  ch = converter->sharedData->impl->getNextUChar(&args,
										  err);
  *source = args.source;
  return ch;
}



/**************************
* Will convert a sequence of bytes from one codepage to another.
* @param toConverterName: The name of the converter that will be used to encode the output buffer
* @param fromConverterName: The name of the converter that will be used to decode the input buffer
* @param target: Pointer to the output buffer* written
* @param targetLength: on input contains the capacity of target, on output the number of bytes copied to target
* @param source: Pointer to the input buffer
* @param sourceLength: on input contains the capacity of source, on output the number of bytes processed in "source"
* @param internal: used internally to store store state data across calls
* @param err: fills in an error status
*/
void 
T_UConverter_fromCodepageToCodepage (UConverter * outConverter,
				     UConverter * inConverter,
				     char **target,
				     const char *targetLimit,
				     const char **source,
				     const char *sourceLimit,
				     int32_t* offsets,
				     UBool flush,
				     UErrorCode * err)
{

  UChar out_chunk[CHUNK_SIZE];
  const UChar *out_chunk_limit = out_chunk + CHUNK_SIZE;
  UChar *out_chunk_alias;
  UChar const *out_chunk_alias2;


  if (U_FAILURE (*err))    return;


  /*loops until the input buffer is completely consumed
   *or if an error has be encountered
   *first we convert from inConverter codepage to Unicode
   *then from Unicode to outConverter codepage
   */
  while ((*source != sourceLimit) && U_SUCCESS (*err))
    {
      out_chunk_alias = out_chunk;
      ucnv_toUnicode (inConverter,
		      &out_chunk_alias,
		      out_chunk_limit,
		      source,
		      sourceLimit,
		      NULL,
		      flush,
		      err);

      /*U_INDEX_OUTOFBOUNDS_ERROR means that the output "CHUNK" is full
       *we will require at least another loop (it's a recoverable error)
       */

      if (U_SUCCESS (*err) || (*err == U_INDEX_OUTOFBOUNDS_ERROR))
	{
	  *err = U_ZERO_ERROR;
	  out_chunk_alias2 = out_chunk;

	  while ((out_chunk_alias2 != out_chunk_alias) && U_SUCCESS (*err))
	    {
	      ucnv_fromUnicode (outConverter,
				target,
				targetLimit,
				&out_chunk_alias2,
				out_chunk_alias,
				NULL,
				TRUE,
				err);

	    }
	}
      else
	break;
    }

  return;
}

int32_t  ucnv_convert(const char *toConverterName,
		      const char *fromConverterName,
		      char *target,
		      int32_t targetSize,
		      const char *source,
		      int32_t sourceSize,
		      UErrorCode * err)
{
  const char *mySource = source;
  const char *mySource_limit = source + sourceSize;
  UConverter *inConverter;
  UConverter *outConverter;
  char *myTarget = target;
  int32_t targetCapacity = 0;

  if (U_FAILURE (*err))
    return 0;

  if ((targetSize < 0) || (sourceSize < 0))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return 0;
    }

  /*if there is no input data, we're done */
  if (sourceSize == 0)
    {
      /*in case the caller passed an output ptr
       *we update it
       */
      return 0;
    }

  /*create the converters */
  inConverter = ucnv_open (fromConverterName, err);
  if (U_FAILURE (*err)) return 0;
  outConverter = ucnv_open (toConverterName, err);
  if (U_FAILURE (*err))
    {
      ucnv_close (inConverter);
      return 0;
    }


  if (targetSize > 0)
    {
      T_UConverter_fromCodepageToCodepage (outConverter,
					   inConverter,
					   &myTarget,
					   target + targetSize,
					   &mySource,
					   mySource_limit,
					   NULL,
					   TRUE,
					   err);
    }


  /*Updates targetCapacity to contain the number of bytes written to target */
  targetCapacity = myTarget - target;
  if (targetSize == 0)
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }

  /* If the output buffer is exhausted, we need to stop writing
   * to it but continue the conversion in order to store in targetSize
   * the number of bytes that was required*/
  if (*err == U_INDEX_OUTOFBOUNDS_ERROR)
    {
      char target2[CHUNK_SIZE];
      char *target2_alias = target2;
      const char *target2_limit = target2 + CHUNK_SIZE;

      /*We use a stack allocated buffer around which we loop
       *(in case the output is greater than CHUNK_SIZE)
       */

      while (*err == U_INDEX_OUTOFBOUNDS_ERROR)
	{
	  *err = U_ZERO_ERROR;
	  target2_alias = target2;
	  T_UConverter_fromCodepageToCodepage (outConverter,
					       inConverter,
					       &target2_alias,
					       target2_limit,
					       &mySource,
					       mySource_limit,
					       NULL,
					       TRUE,
					       err);

	  /*updates the output parameter to contain the number of char required */
	  targetCapacity += (target2_alias - target2) + 1;
	}
      /*We will set the erro code to U_BUFFER_OVERFLOW_ERROR only if
       *nothing graver happened in the previous loop*/
      (targetCapacity)--;
      if (U_SUCCESS (*err))
	*err = U_BUFFER_OVERFLOW_ERROR;
    }

  ucnv_close (inConverter);
  ucnv_close (outConverter);

  return targetCapacity;
}

UConverterType ucnv_getType(const UConverter* converter)
{
  return (UConverterType)converter->sharedData->staticData->conversionType;
}

void ucnv_getStarters(const UConverter* converter, 
		      UBool starters[256],
		      UErrorCode* err)
{
    if (err == NULL || U_FAILURE(*err)) {
        return;
    }

    if(converter->sharedData->impl->getStarters != NULL) {
        converter->sharedData->impl->getStarters(converter, starters, err);
    } else {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
    }
}

int32_t ucnv_getAmbiguousCCSID(const UConverter *cnv)
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t i = 0;
    int32_t ccsid = 0;
    if (cnv == NULL) 
    {
        return -1;
    }
    ccsid = ucnv_getCCSID(cnv, &status);
    if (U_FAILURE(status)) 
    {
        return -1;
    }
    for (i = 0; i < UCNV_MAX_AMBIGUOUSCCSIDS; i++) {
        if (ccsid == UCNV_AMBIGUOUSCONVERTERS[i].ccsid) 
        {
            return i;
        }
    }
    return -1;
}

void ucnv_fixFileSeparator(const UConverter *cnv, 
                           UChar* source, 
                           int32_t sourceLength)
{
    int32_t i = 0;
    int32_t offset = 0;
    if ((source == NULL) || (cnv == NULL))
    {
        return;
    }
    if ((offset = ucnv_getAmbiguousCCSID(cnv)) != -1)
    {
        for (i = 0; i < sourceLength; i++) 
        {
            if (source[i] == UCNV_AMBIGUOUSCONVERTERS[offset].mismapped)
            {
                source[i] = UCNV_AMBIGUOUSCONVERTERS[offset].replacement;
            }
        }
    }
}

UBool ucnv_isAmbiguous(const UConverter *cnv)
{
    return (ucnv_getAmbiguousCCSID(cnv) == -1 ? FALSE : TRUE);
}

void ucnv_setFallback(UConverter *cnv, UBool usesFallback)
{
    cnv->useFallback = usesFallback;
}

UBool ucnv_usesFallback(const UConverter *cnv)
{
    return cnv->useFallback;
}

void 
ucnv_getInvalidChars (const UConverter * converter,
			  char *errBytes,
			  int8_t * len,
			  UErrorCode * err)
{
    if (err == NULL || U_FAILURE(*err))
    {
        return;
    }
    if (len == NULL || errBytes == NULL || converter == NULL)
    {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if (*len < converter->invalidCharLength)
    {
        *err = U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }
    if ((*len = converter->invalidCharLength) > 0)
    {
        uprv_memcpy (errBytes, converter->invalidCharBuffer, *len);
    }
}

void 
ucnv_getInvalidUChars (const UConverter * converter,
			   UChar *errChars,
			   int8_t * len,
			   UErrorCode * err)
{
    if (err == NULL || U_FAILURE(*err))
    {
        return;
    }
    if (len == NULL || errChars == NULL || converter == NULL)
    {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    if (*len < converter->invalidUCharLength)
    {
        *err = U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }
    if ((*len = converter->invalidUCharLength) > 0)
    {
        uprv_memcpy (errChars, converter->invalidUCharBuffer, sizeof(UChar) * (*len));
    }
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */

