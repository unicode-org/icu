/*
**********************************************************************
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/
/* C++ wrappers for the ICUs Codeset Conversion Routines*/

class Locale;
class UnicodeString;
class Mutex;

#include "unicode/utypes.h"
#include "unicode/resbund.h"
#include "cmemory.h"
#include "mutex.h"
extern "C" {
#include "ucnv_io.h"
#include "unicode/ucnv_bld.h"
#include "unicode/ucnv.h"
}
#include "unicode/convert.h"

/* list of converter and alias names */
const char **UnicodeConverterCPP::availableConverterNames=NULL;
int32_t UnicodeConverterCPP::availableConverterNamesCount=0;

UnicodeConverterCPP::UnicodeConverterCPP()
{
    UErrorCode err = U_ZERO_ERROR;
    myUnicodeConverter = ucnv_open(NULL, &err);
}
UnicodeConverterCPP::UnicodeConverterCPP(const char* name, UErrorCode& err)
{
    myUnicodeConverter = ucnv_open(name, &err);
}

UnicodeConverterCPP::UnicodeConverterCPP(const UnicodeString& name, UErrorCode& err)
{
  char myName[UCNV_MAX_CONVERTER_NAME_LENGTH];
  int i;
  name.extract(0, i = name.length(), myName);
  myName[i]='\0';
  myUnicodeConverter = ucnv_open(myName, &err);
}


UnicodeConverterCPP::UnicodeConverterCPP(int32_t codepageNumber,
                                         UConverterPlatform platform,
                                         UErrorCode& err)
{
    myUnicodeConverter = ucnv_openCCSID(codepageNumber,
                                   platform,
                                   &err);
}

UnicodeConverterCPP&   UnicodeConverterCPP::operator=(const UnicodeConverterCPP&  that)
{
    {
        /*Decrements the overwritten converter's ref count
         *Increments the assigner converter's ref count
         */
      Mutex updateReferenceCounters;
      myUnicodeConverter->sharedData->referenceCounter--;
      that.myUnicodeConverter->sharedData->referenceCounter++;
    }

    *myUnicodeConverter = *(that.myUnicodeConverter);
    return *this;
}

bool_t UnicodeConverterCPP::operator==(const UnicodeConverterCPP& that) const
{
  if ((myUnicodeConverter->sharedData    == that.myUnicodeConverter->sharedData) &&
      (myUnicodeConverter->fromCharErrorBehaviour == that.myUnicodeConverter->fromCharErrorBehaviour) &&
      (myUnicodeConverter->toUnicodeStatus == that.myUnicodeConverter->toUnicodeStatus) &&
      (myUnicodeConverter->subCharLen == that.myUnicodeConverter->subCharLen) &&
      (uprv_memcmp(myUnicodeConverter->subChar, that.myUnicodeConverter->subChar, myUnicodeConverter->subCharLen) == 0) &&
      (myUnicodeConverter->UCharErrorBufferLength == that.myUnicodeConverter->UCharErrorBufferLength) &&
      (myUnicodeConverter->charErrorBufferLength == that.myUnicodeConverter->charErrorBufferLength) &&
      (uprv_memcmp(myUnicodeConverter->UCharErrorBuffer, that.myUnicodeConverter->UCharErrorBuffer, myUnicodeConverter->UCharErrorBufferLength) == 0) &&
      (uprv_memcmp(myUnicodeConverter->charErrorBuffer, that.myUnicodeConverter->charErrorBuffer, myUnicodeConverter->charErrorBufferLength) == 0) &&
      (myUnicodeConverter->fromUCharErrorBehaviour == that.myUnicodeConverter->fromUCharErrorBehaviour))
  return TRUE;
  else return FALSE;
}

bool_t UnicodeConverterCPP::operator!=(const UnicodeConverterCPP& that) const
{
  return !(*this == that);
}

UnicodeConverterCPP::UnicodeConverterCPP(const UnicodeConverterCPP&  that)
{
  /*increments the referenceCounter to let the static table know
   *it has one more client
   */
    myUnicodeConverter = new UConverter;
    {
      Mutex updateReferenceCounter;
      that.myUnicodeConverter->sharedData->referenceCounter++;
    }
    *myUnicodeConverter = *(that.myUnicodeConverter);
}


UnicodeConverterCPP::~UnicodeConverterCPP()
{
    ucnv_close(myUnicodeConverter);
}

 void
UnicodeConverterCPP::fromUnicodeString(char*                    target,
                                       int32_t&                 targetSize,
                                       const UnicodeString&     source,
                                       UErrorCode&               err) const
{
  const UChar* mySource = NULL;
  int32_t mySourceLength = 0;
  UConverter myConverter;
  char *myTarget = NULL;

  if (U_FAILURE(err)) return;

  if ((myUnicodeConverter == NULL) || source.isBogus() || (targetSize <= 0))
    {
      err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }

  /*makes a local copy of the UnicodeConverter*/
  myConverter = *myUnicodeConverter;

  /*Removes all state info on the UnicodeConverter*/
  ucnv_reset(&myConverter);


  mySourceLength = source.length();
  mySource = source.getUChars();
  myTarget = target;
  ucnv_fromUnicode(&myConverter,
                 &myTarget,
                 target + targetSize,
                 &mySource,
                 mySource + mySourceLength,
		   NULL,
		   TRUE,
                 &err);
  targetSize = myTarget - target;

  return;
}

 void
UnicodeConverterCPP::toUnicodeString(UnicodeString&         target,
                                     const char*            source,
                                     int32_t                sourceSize,
                                     UErrorCode&             err) const
{
  const char* mySource = source;
  const char* mySourceLimit = source + sourceSize;
  UChar* myTargetUChars = NULL;
  UChar* myTargetUCharsAlias = NULL;
  int32_t myTargetUCharsLength = 0;
  UConverter myConverter;

  if (U_FAILURE(err)) return;
  if ((myUnicodeConverter == NULL) || target.isBogus() || (sourceSize <= 0))
    {
      err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }

  /*makes a local bitwise copy of the UnicodeConverter*/
  myConverter = *myUnicodeConverter;

  /*Removes all state info on the UnicodeConverter*/
  ucnv_reset(&myConverter);
  /*Allocates the theoritically (Not counting added bytes from the error functions) max buffer
   *on a "normal" call, only one iteration will be necessary.
   */
  myTargetUChars =
    (UChar*)uprv_malloc(sizeof(UChar)*(myTargetUCharsLength = (sourceSize/(int32_t)getMinBytesPerChar())));

  if (myTargetUChars == NULL)
    {
      err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
  /*renders the target clean*/
  target.remove();

  /*Will loop until (re-use the same buffer) until no more memory is requested
   *or an error (other than INDEX_OUTOF_BOUNDS) is encountered
   */
  do
    {
      err = U_ZERO_ERROR;
      myTargetUCharsAlias = myTargetUChars;
      ucnv_toUnicode(&myConverter,
                   &myTargetUCharsAlias,
                   myTargetUChars + myTargetUCharsLength,
                   &mySource,
                   mySourceLimit,
		     NULL,
		     TRUE,
		     &err);

      /*appends what we got thus far to the UnicodeString*/
      target.replace((UTextOffset)target.length(),
             myTargetUCharsAlias - myTargetUChars,
             myTargetUChars,
             myTargetUCharsAlias - myTargetUChars);
      /*Checks for the integrity of target (UnicodeString) as it adds data to it*/
      if (target.isBogus()) err = U_MEMORY_ALLOCATION_ERROR;
    } while (err == U_INDEX_OUTOFBOUNDS_ERROR);


  uprv_free(myTargetUChars);

  return;
}



void
UnicodeConverterCPP::fromUnicode(char*&                 target,
                                 const char*            targetLimit,
                                 const UChar*&        source,
                                 const UChar*         sourceLimit,
				 int32_t *offsets,
				 bool_t                 flush,
                                 UErrorCode&             err)
{
    ucnv_fromUnicode(myUnicodeConverter,
                   &target,
                   targetLimit,
                   &source,
                   sourceLimit,
		     offsets,
                   flush,
                   &err);
}



void
UnicodeConverterCPP::toUnicode(UChar*&           target,
                   const UChar*      targetLimit,
                   const char*&        source,
                   const char*         sourceLimit,
			       int32_t* offsets,
                   bool_t              flush,
                   UErrorCode&          err)
{
    ucnv_toUnicode(myUnicodeConverter,
                 &target,
                 targetLimit,
                 &source,
                 sourceLimit,
		   offsets,
                 flush,
                 &err);
}

const char*
UnicodeConverterCPP::getName(UErrorCode&  err) const
{
  return ucnv_getName(myUnicodeConverter, &err);
}

 int8_t
UnicodeConverterCPP::getMaxBytesPerChar() const
{
    return ucnv_getMaxCharSize(myUnicodeConverter);
}

int8_t
UnicodeConverterCPP::getMinBytesPerChar() const
{
    return ucnv_getMinCharSize(myUnicodeConverter);
}

void
UnicodeConverterCPP::getSubstitutionChars(char*             subChars,
                                          int8_t&           len,
                                          UErrorCode&        err) const
{
    ucnv_getSubstChars(myUnicodeConverter,
                        subChars,
                        &len,
                        &err);
}

void
UnicodeConverterCPP::setSubstitutionChars(const char*       subChars,
                                          int8_t            len,
                                          UErrorCode&        err)
{
    ucnv_setSubstChars(myUnicodeConverter,
                        subChars,
                        len,
                        &err);
}


void
UnicodeConverterCPP::resetState()
{
    ucnv_reset(myUnicodeConverter);
}


int32_t
UnicodeConverterCPP::getCodepage(UErrorCode& err) const
{
    return ucnv_getCCSID(myUnicodeConverter, &err);
}

UConverterToUCallback
UnicodeConverterCPP::getMissingCharAction() const
{
    return ucnv_getToUCallBack(myUnicodeConverter);
}

UConverterFromUCallback
UnicodeConverterCPP::getMissingUnicodeAction() const
{
    return ucnv_getFromUCallBack(myUnicodeConverter);
}


void
UnicodeConverterCPP::setMissingCharAction(UConverterToUCallback  action,
                                          UErrorCode&         err)
{
    ucnv_setToUCallBack(myUnicodeConverter, action, &err);
}

void
UnicodeConverterCPP::setMissingUnicodeAction(UConverterFromUCallback   action,
                                             UErrorCode&             err)
{
    ucnv_setFromUCallBack(myUnicodeConverter, action, &err);
}


void
UnicodeConverterCPP::getDisplayName(const Locale&   displayLocale,
                                    UnicodeString&  displayName) const
{

  UErrorCode err = U_ZERO_ERROR;
  UChar name[UCNV_MAX_CONVERTER_NAME_LENGTH];
  int32_t length = ucnv_getDisplayName(myUnicodeConverter, displayLocale.getName(), name, sizeof(name), &err);
  if (U_SUCCESS(err))
    {
      displayName.replace(0, 0x7fffffff, name, length);
    }

  else
    {
      /*Error While creating the resource bundle use the internal name instead*/
      displayName.remove();
      displayName = getName(err); /*Get the raw ASCII name*/

    }

  return;

}


UConverterPlatform
UnicodeConverterCPP::getCodepagePlatform(UErrorCode &err) const
{
    return ucnv_getPlatform(myUnicodeConverter, &err);
}

UConverterType UnicodeConverterCPP::getType() const
{
  return ucnv_getType(myUnicodeConverter);
}

void UnicodeConverterCPP::getStarters(bool_t starters[256],
				 UErrorCode& err) const
{
  ucnv_getStarters(myUnicodeConverter,
		   starters,
		   &err);
  return;
}

const char* const*
UnicodeConverterCPP::getAvailableNames(int32_t& num, UErrorCode& err)
{
  if(U_FAILURE(err)) {
    num = 0;
    return NULL;
  }
  if (availableConverterNames==NULL) {
    int32_t count = ucnv_io_countAvailableConverters(&err);
    if (count > 0) {
      const char **names = new const char *[count];
      if (names != NULL) {
        ucnv_io_fillAvailableConverters(names, &err);

        /* in the mutex block, set the data for this process */
        umtx_lock(0);
        if (availableConverterNames == NULL) {
          availableConverterNamesCount = count;
          availableConverterNames = names;
          names = 0;
        }
        umtx_unlock(0);

        /* if a different thread set it first, then delete the extra data */
        if (names != 0) {
          delete [] names;
        }
      } else {
        num = 0;
        err = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
      }
    }
  }
  num = availableConverterNamesCount;
  return availableConverterNames;
}

int32_t  UnicodeConverterCPP::flushCache()
{
  return ucnv_flushCache();
}
