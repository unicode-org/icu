/*
******************************************************************************
*
*   Copyright (C) 1998-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*  ucnv.c:
*  Implements APIs for the ICU's codeset conversion library;
*  mostly calls through internal functions;
*  created by Bertrand A. Damiba
*
* Modification History:
*
*   Date        Name        Description
*   04/04/99    helena      Fixed internal header inclusion.
*   05/09/00    helena      Added implementation to handle fallback mappings.
*   06/20/2000  helena      OS/400 port changes; mostly typecast.
*/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/ures.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "unicode/uset.h"
#include "cmemory.h"
#include "cstring.h"
#include "uassert.h"
#include "utracimp.h"
#include "ustr_imp.h"
#include "ucnv_imp.h"
#include "ucnv_io.h"
#include "ucnv_cnv.h"
#include "ucnv_bld.h"

/* size of intermediate and preflighting buffers in ucnv_convert() */
#define CHUNK_SIZE 1024

typedef struct UAmbiguousConverter {
    const char *name;
    const UChar variant5c;
} UAmbiguousConverter;

static const UAmbiguousConverter ambiguousConverters[]={
    { "ibm-942_P120-1999", 0xa5 },
    { "ibm-943_P130-1999", 0xa5 },
    { "ibm-33722_P120-1999", 0xa5 },
    { "ibm-949_P110-1999", 0x20a9 },
    { "ibm-1363_P110-1997", 0x20a9 },
    { "ISO_2022,locale=ko,version=0", 0x20a9 }
};

U_CAPI const char*  U_EXPORT2
ucnv_getDefaultName ()
{
    return ucnv_io_getDefaultConverterName();
}

U_CAPI void U_EXPORT2
ucnv_setDefaultName (const char *converterName)
{
  ucnv_io_setDefaultConverterName(converterName);
}
/*Calls through createConverter */
U_CAPI UConverter* U_EXPORT2
ucnv_open (const char *name,
                       UErrorCode * err)
{
    UConverter *r;

    if (err == NULL || U_FAILURE (*err)) {
        return NULL;
    }

    r =  ucnv_createConverter(NULL, name, err);
    return r;
}

U_CAPI UConverter* U_EXPORT2 
ucnv_openPackage   (const char *packageName, const char *converterName, UErrorCode * err)
{
    return ucnv_createConverterFromPackage(packageName, converterName,  err);
}

/*Extracts the UChar* to a char* and calls through createConverter */
U_CAPI UConverter*   U_EXPORT2
ucnv_openU (const UChar * name,
                         UErrorCode * err)
{
    char asciiName[UCNV_MAX_CONVERTER_NAME_LENGTH];

    if (err == NULL || U_FAILURE(*err))
        return NULL;
    if (name == NULL)
        return ucnv_open (NULL, err);
    if (u_strlen(name) >= UCNV_MAX_CONVERTER_NAME_LENGTH)
    {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }
    return ucnv_open(u_austrcpy(asciiName, name), err);
}

/*Assumes a $platform-#codepage.$CONVERTER_FILE_EXTENSION scheme and calls
 *through createConverter*/
U_CAPI UConverter*   U_EXPORT2
ucnv_openCCSID (int32_t codepage,
                UConverterPlatform platform,
                UErrorCode * err)
{
    char myName[UCNV_MAX_CONVERTER_NAME_LENGTH];
    int32_t myNameLen;

    if (err == NULL || U_FAILURE (*err))
        return NULL;

    /* ucnv_copyPlatformString could return "ibm-" or "cp" */
    myNameLen = ucnv_copyPlatformString(myName, platform);
    T_CString_integerToString(myName + myNameLen, codepage, 10);

    return ucnv_createConverter(NULL, myName, err);
}

/* Creating a temporary stack-based object that can be used in one thread, 
and created from a converter that is shared across threads.
*/

U_CAPI UConverter* U_EXPORT2
ucnv_safeClone(const UConverter* cnv, void *stackBuffer, int32_t *pBufferSize, UErrorCode *status)
{
    UConverter *localConverter, *allocatedConverter;
    int32_t bufferSizeNeeded;
    char *stackBufferChars = (char *)stackBuffer;
    UErrorCode cbErr;
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

    UTRACE_ENTRY_OC(UTRACE_UCNV_CLONE);

    if (status == NULL || U_FAILURE(*status)){
        UTRACE_EXIT_STATUS(status? *status: U_ILLEGAL_ARGUMENT_ERROR);
        return 0;
    }

    if (!pBufferSize || !cnv){
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        UTRACE_EXIT_STATUS(*status);
        return 0;
    }

    UTRACE_DATA3(UTRACE_OPEN_CLOSE, "clone converter %s at %p into stackBuffer %p",
                                    ucnv_getName(cnv, status), cnv, stackBuffer);

    if (cnv->sharedData->impl->safeClone != NULL) {
        /* call the custom safeClone function for sizing */
        bufferSizeNeeded = 0;
        cnv->sharedData->impl->safeClone(cnv, NULL, &bufferSizeNeeded, status);
    }
    else
    {
        /* inherent sizing */
        bufferSizeNeeded = sizeof(UConverter);
    }

    if (*pBufferSize <= 0){ /* 'preflighting' request - set needed size into *pBufferSize */
        *pBufferSize = bufferSizeNeeded;
        UTRACE_EXIT_VALUE(bufferSizeNeeded);
        return 0;
    }


    /* Pointers on 64-bit platforms need to be aligned
     * on a 64-bit boundary in memory.
     */
    if (U_ALIGNMENT_OFFSET(stackBuffer) != 0) {
        int32_t offsetUp = (int32_t)U_ALIGNMENT_OFFSET_UP(stackBufferChars);
        if(*pBufferSize > offsetUp) {
            *pBufferSize -= offsetUp;
            stackBufferChars += offsetUp;
        } else {
            /* prevent using the stack buffer but keep the size > 0 so that we do not just preflight */
            *pBufferSize = 1;
        }
    }

    stackBuffer = (void *)stackBufferChars;
    
    /* Now, see if we must allocate any memory */
    if (*pBufferSize < bufferSizeNeeded || stackBuffer == NULL)
    {
        /* allocate one here...*/
        localConverter = allocatedConverter = (UConverter *) uprv_malloc (bufferSizeNeeded);

        if(localConverter == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            UTRACE_EXIT_STATUS(*status);
            return NULL;
        }
        
        if (U_SUCCESS(*status)) {
            *status = U_SAFECLONE_ALLOCATED_WARNING;
        }
        
        /* record the fact that memory was allocated */
        *pBufferSize = bufferSizeNeeded;
    } else {
        /* just use the stack buffer */
        localConverter = (UConverter*) stackBuffer;
        allocatedConverter = NULL;
    }

    uprv_memset(localConverter, 0, bufferSizeNeeded);

    /* Copy initial state */
    uprv_memcpy(localConverter, cnv, sizeof(UConverter));
    localConverter->isCopyLocal = localConverter->isExtraLocal = FALSE;

    /* now either call the safeclone fcn or not */
    if (cnv->sharedData->impl->safeClone != NULL) {
        /* call the custom safeClone function */
        localConverter = cnv->sharedData->impl->safeClone(cnv, localConverter, pBufferSize, status);
    }

    if(localConverter==NULL || U_FAILURE(*status)) {
        uprv_free(allocatedConverter);
        UTRACE_EXIT_STATUS(*status);
        return NULL;
    }

    /* increment refcount of shared data if needed */
    /*
    Checking whether it's an algorithic converter is okay
    in multithreaded applications because the value never changes.
    Don't check referenceCounter for any other value.
    */
    if (cnv->sharedData->referenceCounter != ~0) {
        ucnv_incrementRefCount(cnv->sharedData);
    }

    if(localConverter == (UConverter*)stackBuffer) {
        /* we're using user provided data - set to not destroy */
        localConverter->isCopyLocal = TRUE;
    }

    /* allow callback functions to handle any memory allocation */
    toUArgs.converter = fromUArgs.converter = localConverter;
    cbErr = U_ZERO_ERROR;
    cnv->fromCharErrorBehaviour(cnv->toUContext, &toUArgs, NULL, 0, UCNV_CLONE, &cbErr);
    cbErr = U_ZERO_ERROR;
    cnv->fromUCharErrorBehaviour(cnv->fromUContext, &fromUArgs, NULL, 0, 0, UCNV_CLONE, &cbErr);

    UTRACE_EXIT_PTR_STATUS(localConverter, *status);
    return localConverter;
}



/*Decreases the reference counter in the shared immutable section of the object
 *and frees the mutable part*/

U_CAPI void  U_EXPORT2
ucnv_close (UConverter * converter)
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
    UErrorCode errorCode = U_ZERO_ERROR;

    UTRACE_ENTRY_OC(UTRACE_UCNV_CLOSE);

    if (converter == NULL)
    {
        UTRACE_EXIT();
        return;
    }

    UTRACE_DATA3(UTRACE_OPEN_CLOSE, "close converter %s at %p, isCopyLocal=%b",
        ucnv_getName(converter, &errorCode), converter, converter->isCopyLocal);

    toUArgs.converter = fromUArgs.converter = converter;

    converter->fromCharErrorBehaviour(converter->toUContext, &toUArgs, NULL, 0, UCNV_CLOSE, &errorCode);
    errorCode = U_ZERO_ERROR;
    converter->fromUCharErrorBehaviour(converter->fromUContext, &fromUArgs, NULL, 0, 0, UCNV_CLOSE, &errorCode);

    if (converter->sharedData->impl->close != NULL) {
        converter->sharedData->impl->close(converter);
    }

    /*
    Checking whether it's an algorithic converter is okay
    in multithreaded applications because the value never changes.
    Don't check referenceCounter for any other value.
    */
    if (converter->sharedData->referenceCounter != ~0) {
        ucnv_unloadSharedDataIfReady(converter->sharedData);
    }

    if(!converter->isCopyLocal){
        uprv_free (converter);
    }

    UTRACE_EXIT();
}

/*returns a single Name from the list, will return NULL if out of bounds
 */
U_CAPI const char*   U_EXPORT2
ucnv_getAvailableName (int32_t n)
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

U_CAPI int32_t   U_EXPORT2
ucnv_countAvailable ()
{
    UErrorCode err = U_ZERO_ERROR;
    return ucnv_io_countAvailableConverters(&err);
}

U_CAPI uint16_t U_EXPORT2
ucnv_countAliases(const char *alias, UErrorCode *pErrorCode)
{
    return ucnv_io_countAliases(alias, pErrorCode);
}


U_CAPI const char* U_EXPORT2
ucnv_getAlias(const char *alias, uint16_t n, UErrorCode *pErrorCode)
{
    return ucnv_io_getAlias(alias, n, pErrorCode);
}

U_CAPI void U_EXPORT2
ucnv_getAliases(const char *alias, const char **aliases, UErrorCode *pErrorCode)
{
    ucnv_io_getAliases(alias, 0, aliases, pErrorCode);
}

U_CAPI uint16_t U_EXPORT2
ucnv_countStandards(void)
{
    UErrorCode err = U_ZERO_ERROR;
    return ucnv_io_countStandards(&err);
}

U_CAPI void    U_EXPORT2
ucnv_getSubstChars (const UConverter * converter,
                    char *mySubChar,
                    int8_t * len,
                    UErrorCode * err)
{
    if (U_FAILURE (*err))
        return;

    if (*len < converter->subCharLen) /*not enough space in subChars */
    {
        *err = U_INDEX_OUTOFBOUNDS_ERROR;
        return;
    }

  uprv_memcpy (mySubChar, converter->subChar, converter->subCharLen);   /*fills in the subchars */
  *len = converter->subCharLen; /*store # of bytes copied to buffer */
    uprv_memcpy (mySubChar, converter->subChar, converter->subCharLen);   /*fills in the subchars */
    *len = converter->subCharLen; /*store # of bytes copied to buffer */
}

U_CAPI void    U_EXPORT2
ucnv_setSubstChars (UConverter * converter,
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
    
    uprv_memcpy (converter->subChar, mySubChar, len); /*copies the subchars */
    converter->subCharLen = len;  /*sets the new len */

    /*
    * There is currently (2001Feb) no separate API to set/get subChar1.
    * In order to always have subChar written after it is explicitly set,
    * we set subChar1 to 0.
    */
    converter->subChar1 = 0;
    
    return;
}

U_CAPI int32_t U_EXPORT2
ucnv_getDisplayName(const UConverter *cnv,
                    const char *displayLocale,
                    UChar *displayName, int32_t displayNameCapacity,
                    UErrorCode *pErrorCode) {
    UResourceBundle *rb;
    const UChar *name;
    int32_t length;

    /* check arguments */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if(cnv==NULL || displayNameCapacity<0 || (displayNameCapacity>0 && displayName==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* open the resource bundle and get the display name string */
    rb=ures_open(NULL, displayLocale, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /* use the internal name as the key */
    name=ures_getStringByKey(rb, cnv->sharedData->staticData->name, &length, pErrorCode);
    ures_close(rb);

    if(U_SUCCESS(*pErrorCode)) {
        /* copy the string */
        u_memcpy(displayName, name, uprv_min(length, displayNameCapacity)*U_SIZEOF_UCHAR);
    } else {
        /* convert the internal name into a Unicode string */
        *pErrorCode=U_ZERO_ERROR;
        length=uprv_strlen(cnv->sharedData->staticData->name);
        u_charsToUChars(cnv->sharedData->staticData->name, displayName, uprv_min(length, displayNameCapacity));
    }
    return u_terminateUChars(displayName, displayNameCapacity, length, pErrorCode);
}

/*resets the internal states of a converter
 *goal : have the same behaviour than a freshly created converter
 */
static void _reset(UConverter *converter, UConverterResetChoice choice,
                   UBool callCallback) {
    if(converter == NULL) {
        return;
    }

    if(callCallback) {
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

        toUArgs.converter = fromUArgs.converter = converter;
        if(choice<=UCNV_RESET_TO_UNICODE) {
            errorCode = U_ZERO_ERROR;
            converter->fromCharErrorBehaviour(converter->toUContext, &toUArgs, NULL, 0, UCNV_RESET, &errorCode);
        }
        if(choice!=UCNV_RESET_TO_UNICODE) {
            errorCode = U_ZERO_ERROR;
            converter->fromUCharErrorBehaviour(converter->fromUContext, &fromUArgs, NULL, 0, 0, UCNV_RESET, &errorCode);
        }
    }

    /* now reset the converter itself */
    if(choice<=UCNV_RESET_TO_UNICODE) {
        converter->toUnicodeStatus = converter->sharedData->toUnicodeStatus;
        converter->mode = 0;
        converter->toULength = 0;
        converter->invalidCharLength = converter->UCharErrorBufferLength = 0;
        converter->preToULength = 0;
    }
    if(choice!=UCNV_RESET_TO_UNICODE) {
        converter->fromUnicodeStatus = 0;
        converter->fromUChar32 = 0;
        converter->invalidUCharLength = converter->charErrorBufferLength = 0;
        converter->preFromUFirstCP = U_SENTINEL;
        converter->preFromULength = 0;
    }

    if (converter->sharedData->impl->reset != NULL) {
        /* call the custom reset function */
        converter->sharedData->impl->reset(converter, choice);
    }
}

U_CAPI void  U_EXPORT2
ucnv_reset(UConverter *converter)
{
    _reset(converter, UCNV_RESET_BOTH, TRUE);
}

U_CAPI void  U_EXPORT2
ucnv_resetToUnicode(UConverter *converter)
{
    _reset(converter, UCNV_RESET_TO_UNICODE, TRUE);
}

U_CAPI void  U_EXPORT2
ucnv_resetFromUnicode(UConverter *converter)
{
    _reset(converter, UCNV_RESET_FROM_UNICODE, TRUE);
}

U_CAPI int8_t   U_EXPORT2
ucnv_getMaxCharSize (const UConverter * converter)
{
    return converter->maxBytesPerUChar;
}


U_CAPI int8_t   U_EXPORT2
ucnv_getMinCharSize (const UConverter * converter)
{
    return converter->sharedData->staticData->minBytesPerChar;
}

U_CAPI const char*   U_EXPORT2
ucnv_getName (const UConverter * converter, UErrorCode * err)
     
{
    if (U_FAILURE (*err))
        return NULL;
    if(converter->sharedData->impl->getName){
        const char* temp= converter->sharedData->impl->getName(converter);
        if(temp)
            return temp;
    }
    return converter->sharedData->staticData->name;
}

U_CAPI int32_t   U_EXPORT2
ucnv_getCCSID (const UConverter * converter,
                        UErrorCode * err)
{
    if (U_FAILURE (*err))
        return -1;

    return converter->sharedData->staticData->codepage;
}


U_CAPI UConverterPlatform   U_EXPORT2
ucnv_getPlatform (const UConverter * converter,
                                      UErrorCode * err)
{
    if (U_FAILURE (*err))
        return UCNV_UNKNOWN;

    return (UConverterPlatform)converter->sharedData->staticData->platform;
}

U_CAPI void U_EXPORT2
ucnv_getUnicodeSet(const UConverter *cnv,
                   USet *setFillIn,
                   UConverterUnicodeSet whichSet,
                   UErrorCode *pErrorCode) {
    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }
    if(cnv==NULL || setFillIn==NULL || whichSet<UCNV_ROUNDTRIP_SET || UCNV_SET_COUNT<=whichSet) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /* does this converter support this function? */
    if(cnv->sharedData->impl->getUnicodeSet==NULL) {
        *pErrorCode=U_UNSUPPORTED_ERROR;
        return;
    }

    /* empty the set */
    uset_clear(setFillIn);

    /* call the converter to add the code points it supports */
    cnv->sharedData->impl->getUnicodeSet(cnv, setFillIn, whichSet, pErrorCode);
}

U_CAPI void U_EXPORT2
    ucnv_getToUCallBack (const UConverter * converter,
                         UConverterToUCallback *action,
                         const void **context)
{
    *action = converter->fromCharErrorBehaviour;
    *context = converter->toUContext;
}

U_CAPI void U_EXPORT2
    ucnv_getFromUCallBack (const UConverter * converter,
                           UConverterFromUCallback *action,
                           const void **context)
{
    *action = converter->fromUCharErrorBehaviour;
    *context = converter->fromUContext;
}

U_CAPI void    U_EXPORT2
ucnv_setToUCallBack (UConverter * converter,
                            UConverterToUCallback newAction,
                            const void* newContext,
                            UConverterToUCallback *oldAction,
                            const void** oldContext,
                            UErrorCode * err)
{
    if (U_FAILURE (*err))
        return;
    if (oldAction) *oldAction = converter->fromCharErrorBehaviour;
    converter->fromCharErrorBehaviour = newAction;
    if (oldContext) *oldContext = converter->toUContext;
    converter->toUContext = newContext;
}

U_CAPI void  U_EXPORT2
ucnv_setFromUCallBack (UConverter * converter,
                            UConverterFromUCallback newAction,
                            const void* newContext,
                            UConverterFromUCallback *oldAction,
                            const void** oldContext,
                            UErrorCode * err)
{
    if (U_FAILURE (*err))
        return;
    if (oldAction) *oldAction = converter->fromUCharErrorBehaviour;
    converter->fromUCharErrorBehaviour = newAction;
    if (oldContext) *oldContext = converter->fromUContext;
    converter->fromUContext = newContext;
}

static void
_updateOffsets(int32_t *offsets, int32_t length,
               int32_t sourceIndex, int32_t errorInputLength) {
    int32_t *limit;
    int32_t delta, offset;

    if(sourceIndex>=0) {
        /*
         * adjust each offset by adding the previous sourceIndex
         * minus the length of the input sequence that caused an
         * error, if any
         */
        delta=sourceIndex-errorInputLength;
    } else {
        /*
         * set each offset to -1 because this conversion function
         * does not handle offsets
         */
        delta=-1;
    }

    limit=offsets+length;
    if(delta==0) {
        /* most common case, nothing to do */
    } else if(delta>0) {
        /* add the delta to each offset (but not if the offset is <0) */
        while(offsets<limit) {
            offset=*offsets;
            if(offset>=0) {
                *offsets=offset+delta;
            }
            ++offsets;
        }
    } else /* delta<0 */ {
        /*
         * set each offset to -1 because this conversion function
         * does not handle offsets
         * or the error input sequence started in a previous buffer
         */
        while(offsets<limit) {
            *offsets++=-1;
        }
    }
}

/* ucnv_fromUnicode --------------------------------------------------------- */

/*
 * Implementation note for m:n conversions
 *
 * While collecting source units to find the longest match for m:n conversion,
 * some source units may need to be stored for a partial match.
 * When a second buffer does not yield a match on all of the previously stored
 * source units, then they must be "replayed", i.e., fed back into the converter.
 *
 * The code relies on the fact that replaying will not nest -
 * converting a replay buffer will not result in a replay.
 * This is because a replay is necessary only after the _continuation_ of a
 * partial match failed, but a replay buffer is converted as a whole.
 * It may result in some of its units being stored again for a partial match,
 * but there will not be a continuation _during_ the replay which could fail.
 *
 * It is conceivable that a callback function could call the converter
 * recursively in a way that causes another replay to be stored, but that
 * would be an error in the callback function.
 * Such violations will cause assertion failures in a debug build,
 * and wrong output, but they will not cause a crash.
 */

static void
_fromUnicodeWithCallback(UConverterFromUnicodeArgs *pArgs, UErrorCode *err) {
    UConverterFromUnicode fromUnicode;
    UConverter *cnv;
    const UChar *s;
    char *t;
    int32_t *offsets;
    int32_t sourceIndex;
    int32_t errorInputLength;
    UBool converterSawEndOfInput, calledCallback;

    /* variables for m:n conversion */
    UChar replay[UCNV_EXT_MAX_UCHARS];
    const UChar *realSource, *realSourceLimit;
    int32_t realSourceIndex;
    UBool realFlush;

    cnv=pArgs->converter;
    s=pArgs->source;
    t=pArgs->target;
    offsets=pArgs->offsets;

    /* get the converter implementation function */
    sourceIndex=0;
    if(offsets==NULL) {
        fromUnicode=cnv->sharedData->impl->fromUnicode;
    } else {
        fromUnicode=cnv->sharedData->impl->fromUnicodeWithOffsets;
        if(fromUnicode==NULL) {
            /* there is no WithOffsets implementation */
            fromUnicode=cnv->sharedData->impl->fromUnicode;
            /* we will write -1 for each offset */
            sourceIndex=-1;
        }
    }

    if(cnv->preFromULength>=0) {
        /* normal mode */
        realSource=NULL;

        /* avoid compiler warnings - not otherwise necessary, and the values do not matter */
        realSourceLimit=NULL;
        realFlush=FALSE;
        realSourceIndex=0;
    } else {
        /*
         * Previous m:n conversion stored source units from a partial match
         * and failed to consume all of them.
         * We need to "replay" them from a temporary buffer and convert them first.
         */
        realSource=pArgs->source;
        realSourceLimit=pArgs->sourceLimit;
        realFlush=pArgs->flush;
        realSourceIndex=sourceIndex;

        uprv_memcpy(replay, cnv->preFromU, -cnv->preFromULength*U_SIZEOF_UCHAR);
        pArgs->source=replay;
        pArgs->sourceLimit=replay-cnv->preFromULength;
        pArgs->flush=FALSE;
        sourceIndex=-1;

        cnv->preFromULength=0;
    }

    /*
     * loop for conversion and error handling
     *
     * loop {
     *   convert
     *   loop {
     *     update offsets
     *     handle end of input
     *     handle errors/call callback
     *   }
     * }
     */
    for(;;) {
        /* convert */
        fromUnicode(pArgs, err);

        /*
         * set a flag for whether the converter
         * successfully processed the end of the input
         *
         * need not check cnv->preFromULength==0 because a replay (<0) will cause
         * s<sourceLimit before converterSawEndOfInput is checked
         */
        converterSawEndOfInput=
            (UBool)(U_SUCCESS(*err) &&
                    pArgs->flush && pArgs->source==pArgs->sourceLimit &&
                    cnv->fromUChar32==0);

        /* no callback called yet for this iteration */
        calledCallback=FALSE;

        /* no sourceIndex adjustment for conversion, only for callback output */
        errorInputLength=0;

        /*
         * loop for offsets and error handling
         *
         * iterates at most 3 times:
         * 1. to clean up after the conversion function
         * 2. after the callback
         * 3. after the callback again if there was truncated input
         */
        for(;;) {
            /* update offsets if we write any */
            if(offsets!=NULL) {
                int32_t length=(int32_t)(pArgs->target-t);
                if(length>0) {
                    _updateOffsets(offsets, length, sourceIndex, errorInputLength);

                    /*
                     * if a converter handles offsets and updates the offsets
                     * pointer at the end, then pArgs->offset should not change
                     * here;
                     * however, some converters do not handle offsets at all
                     * (sourceIndex<0) or may not update the offsets pointer
                     */
                    pArgs->offsets=offsets+=length;
                }

                if(sourceIndex>=0) {
                    sourceIndex+=(int32_t)(pArgs->source-s);
                }
            }

            if(cnv->preFromULength<0) {
                /*
                 * switch the source to new replay units (cannot occur while replaying)
                 * after offset handling and before end-of-input and callback handling
                 */
                if(realSource==NULL) {
                    realSource=pArgs->source;
                    realSourceLimit=pArgs->sourceLimit;
                    realFlush=pArgs->flush;
                    realSourceIndex=sourceIndex;

                    uprv_memcpy(replay, cnv->preFromU, -cnv->preFromULength*U_SIZEOF_UCHAR);
                    pArgs->source=replay;
                    pArgs->sourceLimit=replay-cnv->preFromULength;
                    pArgs->flush=FALSE;
                    if((sourceIndex+=cnv->preFromULength)<0) {
                        sourceIndex=-1;
                    }

                    cnv->preFromULength=0;
                } else {
                    /* see implementation note before _fromUnicodeWithCallback() */
                    U_ASSERT(realSource==NULL);
                    *err=U_INTERNAL_PROGRAM_ERROR;
                }
            }

            /* update pointers */
            s=pArgs->source;
            t=pArgs->target;

            if(U_SUCCESS(*err)) {
                if(s<pArgs->sourceLimit) {
                    /*
                     * continue with the conversion loop while there is still input left
                     * (continue converting by breaking out of only the inner loop)
                     */
                    break;
                } else if(realSource!=NULL) {
                    /* switch back from replaying to the real source and continue */
                    pArgs->source=realSource;
                    pArgs->sourceLimit=realSourceLimit;
                    pArgs->flush=realFlush;
                    sourceIndex=realSourceIndex;

                    realSource=NULL;
                    break;
                } else if(pArgs->flush && cnv->fromUChar32!=0) {
                    /*
                     * the entire input stream is consumed
                     * and there is a partial, truncated input sequence left
                     */

                    /* inject an error and continue with callback handling */
                    *err=U_TRUNCATED_CHAR_FOUND;
                    calledCallback=FALSE; /* new error condition */
                } else {
                    /* input consumed */
                    if(pArgs->flush) {
                        /*
                         * return to the conversion loop once more if the flush
                         * flag is set and the conversion function has not
                         * successfully processed the end of the input yet
                         *
                         * (continue converting by breaking out of only the inner loop)
                         */
                        if(!converterSawEndOfInput) {
                            break;
                        }

                        /* reset the converter without calling the callback function */
                        _reset(cnv, UCNV_RESET_FROM_UNICODE, FALSE);
                    }

                    /* done successfully */
                    return;
                }
            }

            /* U_FAILURE(*err) */
            {
                UErrorCode e;

                if( calledCallback ||
                    (e=*err)==U_BUFFER_OVERFLOW_ERROR ||
                    (e!=U_INVALID_CHAR_FOUND &&
                     e!=U_ILLEGAL_CHAR_FOUND &&
                     e!=U_TRUNCATED_CHAR_FOUND)
                ) {
                    /*
                     * the callback did not or cannot resolve the error:
                     * set output pointers and return
                     *
                     * the check for buffer overflow is redundant but it is
                     * a high-runner case and hopefully documents the intent
                     * well
                     *
                     * if we were replaying, then the replay buffer must be
                     * copied back into the UConverter
                     * and the real arguments must be restored
                     */
                    if(realSource!=NULL) {
                        int32_t length;

                        U_ASSERT(cnv->preFromULength==0);

                        length=(int32_t)(pArgs->sourceLimit-pArgs->source);
                        if(length>0) {
                            uprv_memcpy(cnv->preFromU, pArgs->source, length*U_SIZEOF_UCHAR);
                            cnv->preFromULength=(int8_t)-length;
                        }

                        pArgs->source=realSource;
                        pArgs->sourceLimit=realSourceLimit;
                        pArgs->flush=realFlush;
                    }

                    return;
                }
            }

            /* callback handling */
            {
                UChar32 codePoint;

                /* get and write the code point */
                codePoint=cnv->fromUChar32;
                errorInputLength=0;
                U16_APPEND_UNSAFE(cnv->invalidUCharBuffer, errorInputLength, codePoint);
                cnv->invalidUCharLength=(int8_t)errorInputLength;

                /* set the converter state to deal with the next character */
                cnv->fromUChar32=0;

                /* call the callback function */
                cnv->fromUCharErrorBehaviour(cnv->fromUContext, pArgs,
                    cnv->invalidUCharBuffer, errorInputLength, codePoint,
                    *err==U_INVALID_CHAR_FOUND ? UCNV_UNASSIGNED : UCNV_ILLEGAL,
                    err);
            }

            /*
             * loop back to the offset handling
             *
             * this flag will indicate after offset handling
             * that a callback was called;
             * if the callback did not resolve the error, then we return
             */
            calledCallback=TRUE;
        }
    }
}

U_CAPI void U_EXPORT2
ucnv_fromUnicode(UConverter *cnv,
                 char **target, const char *targetLimit,
                 const UChar **source, const UChar *sourceLimit,
                 int32_t *offsets,
                 UBool flush,
                 UErrorCode *err) {
    UConverterFromUnicodeArgs args;
    const UChar *s;
    char *t;

    /* check parameters */
    if(err==NULL || U_FAILURE(*err)) {
        return;
    }

    if(cnv==NULL || target==NULL || source==NULL) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    s=*source;
    t=*target;
    if(sourceLimit<s || targetLimit<t) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /*
     * Make sure that the buffer sizes do not exceed the number range for
     * int32_t because some functions use the size (in units or bytes)
     * rather than comparing pointers, and because offsets are int32_t values.
     *
     * size_t is guaranteed to be unsigned and large enough for the job.
     *
     * Return with an error instead of adjusting the limits because we would
     * not be able to maintain the semantics that either the source must be
     * consumed or the target filled (unless an error occurs).
     * An adjustment would be targetLimit=t+0x7fffffff; for example.
     */
    if(
        ((size_t)(sourceLimit-s)>(size_t)0x3fffffff && sourceLimit>s) ||
        ((size_t)(targetLimit-t)>(size_t)0x7fffffff && targetLimit>t)
    ) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    /* flush the target overflow buffer */
    if(cnv->charErrorBufferLength>0) {
        char *overflow;
        int32_t i, length;

        overflow=(char *)cnv->charErrorBuffer;
        length=cnv->charErrorBufferLength;
        i=0;
        do {
            if(t==targetLimit) {
                /* the overflow buffer contains too much, keep the rest */
                int32_t j=0;

                do {
                    overflow[j++]=overflow[i++];
                } while(i<length);

                cnv->charErrorBufferLength=(int8_t)j;
                *target=t;
                *err=U_BUFFER_OVERFLOW_ERROR;
                return;
            }

            /* copy the overflow contents to the target */
            *t++=overflow[i++];
            if(offsets!=NULL) {
                *offsets++=-1; /* no source index available for old output */
            }
        } while(i<length);

        /* the overflow buffer is completely copied to the target */
        cnv->charErrorBufferLength=0;
    }

    if(!flush && s==sourceLimit && cnv->preFromULength>=0) {
        /* the overflow buffer is emptied and there is no new input: we are done */
        *target=t;
        return;
    }

    /*
     * Do not simply return with a buffer overflow error if
     * !flush && t==targetLimit
     * because it is possible that the source will not generate any output.
     * For example, the skip callback may be called;
     * it does not output anything.
     */

    /* prepare the converter arguments */
    args.converter=cnv;
    args.flush=flush;
    args.offsets=offsets;
    args.source=s;
    args.sourceLimit=sourceLimit;
    args.target=t;
    args.targetLimit=targetLimit;
    args.size=sizeof(args);

    _fromUnicodeWithCallback(&args, err);

    *source=args.source;
    *target=args.target;
}

/* ucnv_toUnicode() --------------------------------------------------------- */

static void
_toUnicodeWithCallback(UConverterToUnicodeArgs *pArgs, UErrorCode *err) {
    UConverterToUnicode toUnicode;
    UConverter *cnv;
    const char *s;
    UChar *t;
    int32_t *offsets;
    int32_t sourceIndex;
    int32_t errorInputLength;
    UBool converterSawEndOfInput, calledCallback;

    /* variables for m:n conversion */
    char replay[UCNV_EXT_MAX_BYTES];
    const char *realSource, *realSourceLimit;
    int32_t realSourceIndex;
    UBool realFlush;

    cnv=pArgs->converter;
    s=pArgs->source;
    t=pArgs->target;
    offsets=pArgs->offsets;

    /* get the converter implementation function */
    sourceIndex=0;
    if(offsets==NULL) {
        toUnicode=cnv->sharedData->impl->toUnicode;
    } else {
        toUnicode=cnv->sharedData->impl->toUnicodeWithOffsets;
        if(toUnicode==NULL) {
            /* there is no WithOffsets implementation */
            toUnicode=cnv->sharedData->impl->toUnicode;
            /* we will write -1 for each offset */
            sourceIndex=-1;
        }
    }

    if(cnv->preToULength>=0) {
        /* normal mode */
        realSource=NULL;

        /* avoid compiler warnings - not otherwise necessary, and the values do not matter */
        realSourceLimit=NULL;
        realFlush=FALSE;
        realSourceIndex=0;
    } else {
        /*
         * Previous m:n conversion stored source units from a partial match
         * and failed to consume all of them.
         * We need to "replay" them from a temporary buffer and convert them first.
         */
        realSource=pArgs->source;
        realSourceLimit=pArgs->sourceLimit;
        realFlush=pArgs->flush;
        realSourceIndex=sourceIndex;

        uprv_memcpy(replay, cnv->preToU, -cnv->preToULength);
        pArgs->source=replay;
        pArgs->sourceLimit=replay-cnv->preToULength;
        pArgs->flush=FALSE;
        sourceIndex=-1;

        cnv->preToULength=0;
    }

    /*
     * loop for conversion and error handling
     *
     * loop {
     *   convert
     *   loop {
     *     update offsets
     *     handle end of input
     *     handle errors/call callback
     *   }
     * }
     */
    for(;;) {
        if(U_SUCCESS(*err)) {
            /* convert */
            toUnicode(pArgs, err);

            /*
             * set a flag for whether the converter
             * successfully processed the end of the input
             *
             * need not check cnv->preToULength==0 because a replay (<0) will cause
             * s<sourceLimit before converterSawEndOfInput is checked
             */
            converterSawEndOfInput=
                (UBool)(U_SUCCESS(*err) &&
                        pArgs->flush && pArgs->source==pArgs->sourceLimit &&
                        cnv->toULength==0);
        } else {
            /* handle error from getNextUChar() */
            converterSawEndOfInput=FALSE;
        }

        /* no callback called yet for this iteration */
        calledCallback=FALSE;

        /* no sourceIndex adjustment for conversion, only for callback output */
        errorInputLength=0;

        /*
         * loop for offsets and error handling
         *
         * iterates at most 3 times:
         * 1. to clean up after the conversion function
         * 2. after the callback
         * 3. after the callback again if there was truncated input
         */
        for(;;) {
            /* update offsets if we write any */
            if(offsets!=NULL) {
                int32_t length=(int32_t)(pArgs->target-t);
                if(length>0) {
                    _updateOffsets(offsets, length, sourceIndex, errorInputLength);

                    /*
                     * if a converter handles offsets and updates the offsets
                     * pointer at the end, then pArgs->offset should not change
                     * here;
                     * however, some converters do not handle offsets at all
                     * (sourceIndex<0) or may not update the offsets pointer
                     */
                    pArgs->offsets=offsets+=length;
                }

                if(sourceIndex>=0) {
                    sourceIndex+=(int32_t)(pArgs->source-s);
                }
            }

            if(cnv->preToULength<0) {
                /*
                 * switch the source to new replay units (cannot occur while replaying)
                 * after offset handling and before end-of-input and callback handling
                 */
                if(realSource==NULL) {
                    realSource=pArgs->source;
                    realSourceLimit=pArgs->sourceLimit;
                    realFlush=pArgs->flush;
                    realSourceIndex=sourceIndex;

                    uprv_memcpy(replay, cnv->preToU, -cnv->preToULength);
                    pArgs->source=replay;
                    pArgs->sourceLimit=replay-cnv->preToULength;
                    pArgs->flush=FALSE;
                    if((sourceIndex+=cnv->preToULength)<0) {
                        sourceIndex=-1;
                    }

                    cnv->preToULength=0;
                } else {
                    /* see implementation note before _fromUnicodeWithCallback() */
                    U_ASSERT(realSource==NULL);
                    *err=U_INTERNAL_PROGRAM_ERROR;
                }
            }

            /* update pointers */
            s=pArgs->source;
            t=pArgs->target;

            if(U_SUCCESS(*err)) {
                if(s<pArgs->sourceLimit) {
                    /*
                     * continue with the conversion loop while there is still input left
                     * (continue converting by breaking out of only the inner loop)
                     */
                    break;
                } else if(realSource!=NULL) {
                    /* switch back from replaying to the real source and continue */
                    pArgs->source=realSource;
                    pArgs->sourceLimit=realSourceLimit;
                    pArgs->flush=realFlush;
                    sourceIndex=realSourceIndex;

                    realSource=NULL;
                    break;
                } else if(pArgs->flush && cnv->toULength>0) {
                    /*
                     * the entire input stream is consumed
                     * and there is a partial, truncated input sequence left
                     */

                    /* inject an error and continue with callback handling */
                    *err=U_TRUNCATED_CHAR_FOUND;
                    calledCallback=FALSE; /* new error condition */
                } else {
                    /* input consumed */
                    if(pArgs->flush) {
                        /*
                         * return to the conversion loop once more if the flush
                         * flag is set and the conversion function has not
                         * successfully processed the end of the input yet
                         *
                         * (continue converting by breaking out of only the inner loop)
                         */
                        if(!converterSawEndOfInput) {
                            break;
                        }

                        /* reset the converter without calling the callback function */
                        _reset(cnv, UCNV_RESET_TO_UNICODE, FALSE);
                    }

                    /* done successfully */
                    return;
                }
            }

            /* U_FAILURE(*err) */
            {
                UErrorCode e;

                if( calledCallback ||
                    (e=*err)==U_BUFFER_OVERFLOW_ERROR ||
                    (e!=U_INVALID_CHAR_FOUND &&
                     e!=U_ILLEGAL_CHAR_FOUND &&
                     e!=U_TRUNCATED_CHAR_FOUND &&
                     e!=U_ILLEGAL_ESCAPE_SEQUENCE &&
                     e!=U_UNSUPPORTED_ESCAPE_SEQUENCE)
                ) {
                    /*
                     * the callback did not or cannot resolve the error:
                     * set output pointers and return
                     *
                     * the check for buffer overflow is redundant but it is
                     * a high-runner case and hopefully documents the intent
                     * well
                     *
                     * if we were replaying, then the replay buffer must be
                     * copied back into the UConverter
                     * and the real arguments must be restored
                     */
                    if(realSource!=NULL) {
                        int32_t length;

                        U_ASSERT(cnv->preToULength==0);

                        length=(int32_t)(pArgs->sourceLimit-pArgs->source);
                        if(length>0) {
                            uprv_memcpy(cnv->preToU, pArgs->source, length);
                            cnv->preToULength=(int8_t)-length;
                        }

                        pArgs->source=realSource;
                        pArgs->sourceLimit=realSourceLimit;
                        pArgs->flush=realFlush;
                    }

                    return;
                }
            }

            /* copy toUBytes[] to invalidCharBuffer[] */
            errorInputLength=cnv->invalidCharLength=cnv->toULength;
            if(errorInputLength>0) {
                uprv_memcpy(cnv->invalidCharBuffer, cnv->toUBytes, errorInputLength);
            }

            /* set the converter state to deal with the next character */
            cnv->toULength=0;

            /* call the callback function */
            cnv->fromCharErrorBehaviour(cnv->toUContext, pArgs,
                cnv->invalidCharBuffer, errorInputLength,
                (*err==U_INVALID_CHAR_FOUND || *err==U_UNSUPPORTED_ESCAPE_SEQUENCE) ?
                    UCNV_UNASSIGNED : UCNV_ILLEGAL,
                err);

            /*
             * loop back to the offset handling
             *
             * this flag will indicate after offset handling
             * that a callback was called;
             * if the callback did not resolve the error, then we return
             */
            calledCallback=TRUE;
        }
    }
}

U_CAPI void U_EXPORT2
ucnv_toUnicode(UConverter *cnv,
               UChar **target, const UChar *targetLimit,
               const char **source, const char *sourceLimit,
               int32_t *offsets,
               UBool flush,
               UErrorCode *err) {
    UConverterToUnicodeArgs args;
    const char *s;
    UChar *t;

    /* check parameters */
    if(err==NULL || U_FAILURE(*err)) {
        return;
    }

    if(cnv==NULL || target==NULL || source==NULL) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    s=*source;
    t=*target;
    if(sourceLimit<s || targetLimit<t) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /*
     * Make sure that the buffer sizes do not exceed the number range for
     * int32_t because some functions use the size (in units or bytes)
     * rather than comparing pointers, and because offsets are int32_t values.
     *
     * size_t is guaranteed to be unsigned and large enough for the job.
     *
     * Return with an error instead of adjusting the limits because we would
     * not be able to maintain the semantics that either the source must be
     * consumed or the target filled (unless an error occurs).
     * An adjustment would be sourceLimit=t+0x7fffffff; for example.
     */
    if(
        ((size_t)(sourceLimit-s)>(size_t)0x7fffffff && sourceLimit>s) ||
        ((size_t)(targetLimit-t)>(size_t)0x3fffffff && targetLimit>t)
    ) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    /* flush the target overflow buffer */
    if(cnv->UCharErrorBufferLength>0) {
        UChar *overflow;
        int32_t i, length;

        overflow=cnv->UCharErrorBuffer;
        length=cnv->UCharErrorBufferLength;
        i=0;
        do {
            if(t==targetLimit) {
                /* the overflow buffer contains too much, keep the rest */
                int32_t j=0;

                do {
                    overflow[j++]=overflow[i++];
                } while(i<length);

                cnv->UCharErrorBufferLength=(int8_t)j;
                *target=t;
                *err=U_BUFFER_OVERFLOW_ERROR;
                return;
            }

            /* copy the overflow contents to the target */
            *t++=overflow[i++];
            if(offsets!=NULL) {
                *offsets++=-1; /* no source index available for old output */
            }
        } while(i<length);

        /* the overflow buffer is completely copied to the target */
        cnv->UCharErrorBufferLength=0;
    }

    if(!flush && s==sourceLimit && cnv->preToULength>=0) {
        /* the overflow buffer is emptied and there is no new input: we are done */
        *target=t;
        return;
    }

    /*
     * Do not simply return with a buffer overflow error if
     * !flush && t==targetLimit
     * because it is possible that the source will not generate any output.
     * For example, the skip callback may be called;
     * it does not output anything.
     */

    /* prepare the converter arguments */
    args.converter=cnv;
    args.flush=flush;
    args.offsets=offsets;
    args.source=s;
    args.sourceLimit=sourceLimit;
    args.target=t;
    args.targetLimit=targetLimit;
    args.size=sizeof(args);

    _toUnicodeWithCallback(&args, err);

    *source=args.source;
    *target=args.target;
}

/* ucnv_to/fromUChars() ----------------------------------------------------- */

U_CAPI int32_t U_EXPORT2
ucnv_fromUChars(UConverter *cnv,
                char *dest, int32_t destCapacity,
                const UChar *src, int32_t srcLength,
                UErrorCode *pErrorCode) {
    const UChar *srcLimit;
    char *originalDest, *destLimit;
    int32_t destLength;

    /* check arguments */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( cnv==NULL ||
        destCapacity<0 || (destCapacity>0 && dest==NULL) ||
        srcLength<-1 || (srcLength!=0 && src==NULL)
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* initialize */
    ucnv_resetFromUnicode(cnv);
    originalDest=dest;
    if(srcLength==-1) {
        srcLength=u_strlen(src);
    }
    if(srcLength>0) {
        srcLimit=src+srcLength;
        destLimit=dest+destCapacity;

        /* pin the destination limit to U_MAX_PTR; NULL check is for OS/400 */
        if(destLimit<dest || (destLimit==NULL && dest!=NULL)) {
            destLimit=(char *)U_MAX_PTR(dest);
        }

        /* perform the conversion */
        ucnv_fromUnicode(cnv, &dest, destLimit, &src, srcLimit, 0, TRUE, pErrorCode);
        destLength=(int32_t)(dest-originalDest);

        /* if an overflow occurs, then get the preflighting length */
        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
            char buffer[1024];

            destLimit=buffer+sizeof(buffer);
            do {
                dest=buffer;
                *pErrorCode=U_ZERO_ERROR;
                ucnv_fromUnicode(cnv, &dest, destLimit, &src, srcLimit, 0, TRUE, pErrorCode);
                destLength+=(int32_t)(dest-buffer);
            } while(*pErrorCode==U_BUFFER_OVERFLOW_ERROR);
        }
    } else {
        destLength=0;
    }

    return u_terminateChars(originalDest, destCapacity, destLength, pErrorCode);
}

U_CAPI int32_t U_EXPORT2
ucnv_toUChars(UConverter *cnv,
              UChar *dest, int32_t destCapacity,
              const char *src, int32_t srcLength,
              UErrorCode *pErrorCode) {
    const char *srcLimit;
    UChar *originalDest, *destLimit;
    int32_t destLength;

    /* check arguments */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( cnv==NULL ||
        destCapacity<0 || (destCapacity>0 && dest==NULL) ||
        srcLength<-1 || (srcLength!=0 && src==NULL))
    {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* initialize */
    ucnv_resetToUnicode(cnv);
    originalDest=dest;
    if(srcLength==-1) {
        srcLength=uprv_strlen(src);
    }
    if(srcLength>0) {
        srcLimit=src+srcLength;
        destLimit=dest+destCapacity;

        /* pin the destination limit to U_MAX_PTR; NULL check is for OS/400 */
        if(destLimit<dest || (destLimit==NULL && dest!=NULL)) {
            destLimit=(UChar *)U_MAX_PTR(dest);
        }

        /* perform the conversion */
        ucnv_toUnicode(cnv, &dest, destLimit, &src, srcLimit, 0, TRUE, pErrorCode);
        destLength=(int32_t)(dest-originalDest);

        /* if an overflow occurs, then get the preflighting length */
        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR)
        {
            UChar buffer[1024];

            destLimit=buffer+sizeof(buffer)/U_SIZEOF_UCHAR;
            do {
                dest=buffer;
                *pErrorCode=U_ZERO_ERROR;
                ucnv_toUnicode(cnv, &dest, destLimit, &src, srcLimit, 0, TRUE, pErrorCode);
                destLength+=(int32_t)(dest-buffer);
            }
            while(*pErrorCode==U_BUFFER_OVERFLOW_ERROR);
        }
    } else {
        destLength=0;
    }

    return u_terminateUChars(originalDest, destCapacity, destLength, pErrorCode);
}

/* ucnv_getNextUChar() ------------------------------------------------------ */

U_CAPI UChar32 U_EXPORT2
ucnv_getNextUChar(UConverter *cnv,
                  const char **source, const char *sourceLimit,
                  UErrorCode *err) {
    UConverterToUnicodeArgs args;
    UChar buffer[U16_MAX_LENGTH];
    const char *s;
    UChar32 c;
    int32_t i, length;

    /* check parameters */
    if(err==NULL || U_FAILURE(*err)) {
        return 0xffff;
    }

    if(cnv==NULL || source==NULL) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return 0xffff;
    }

    s=*source;
    if(sourceLimit<s) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return 0xffff;
    }

    /*
     * Make sure that the buffer sizes do not exceed the number range for
     * int32_t because some functions use the size (in units or bytes)
     * rather than comparing pointers, and because offsets are int32_t values.
     *
     * size_t is guaranteed to be unsigned and large enough for the job.
     *
     * Return with an error instead of adjusting the limits because we would
     * not be able to maintain the semantics that either the source must be
     * consumed or the target filled (unless an error occurs).
     * An adjustment would be sourceLimit=t+0x7fffffff; for example.
     */
    if(((size_t)(sourceLimit-s)>(size_t)0x7fffffff && sourceLimit>s)) {
        *err=U_ILLEGAL_ARGUMENT_ERROR;
        return 0xffff;
    }

    c=U_SENTINEL;

    /* flush the target overflow buffer */
    if(cnv->UCharErrorBufferLength>0) {
        UChar *overflow;

        overflow=cnv->UCharErrorBuffer;
        i=0;
        length=cnv->UCharErrorBufferLength;
        U16_NEXT(overflow, i, length, c);

        /* move the remaining overflow contents up to the beginning */
        if((cnv->UCharErrorBufferLength=(int8_t)(length-i))>0) {
            uprv_memmove(cnv->UCharErrorBuffer, cnv->UCharErrorBuffer+i,
                         cnv->UCharErrorBufferLength*U_SIZEOF_UCHAR);
        }

        if(!U16_IS_LEAD(c) || i<length) {
            return c;
        }
        /*
         * Continue if the overflow buffer contained only a lead surrogate,
         * in case the converter outputs single surrogates from complete
         * input sequences.
         */
    }

    /*
     * flush==TRUE is implied for ucnv_getNextUChar()
     *
     * do not simply return even if s==sourceLimit because the converter may
     * not have seen flush==TRUE before
     */

    /* prepare the converter arguments */
    args.converter=cnv;
    args.flush=TRUE;
    args.offsets=NULL;
    args.source=s;
    args.sourceLimit=sourceLimit;
    args.target=buffer;
    args.targetLimit=buffer+1;
    args.size=sizeof(args);

    if(c<0) {
        /*
         * call the native getNextUChar() implementation if we are
         * at a character boundary (toULength==0)
         *
         * unlike with _toUnicode(), getNextUChar() implementations must set
         * U_TRUNCATED_CHAR_FOUND for truncated input,
         * in addition to setting toULength/toUBytes[]
         */
        if(cnv->toULength==0 && cnv->sharedData->impl->getNextUChar!=NULL) {
            c=cnv->sharedData->impl->getNextUChar(&args, err);
            *source=s=args.source;
            if(*err==U_INDEX_OUTOFBOUNDS_ERROR) {
                /* reset the converter without calling the callback function */
                _reset(cnv, UCNV_RESET_TO_UNICODE, FALSE);
                return 0xffff; /* no output */
            } else if(U_SUCCESS(*err) && c>=0) {
                return c;
            /*
             * else fall through to use _toUnicode() because
             *   UCNV_GET_NEXT_UCHAR_USE_TO_U: the native function did not want to handle it after all
             *   U_FAILURE: call _toUnicode() for callback handling (do not output c)
             */
            }
        }

        /* convert to one UChar in buffer[0], or handle getNextUChar() errors */
        _toUnicodeWithCallback(&args, err);

        if(*err==U_BUFFER_OVERFLOW_ERROR) {
            *err=U_ZERO_ERROR;
        }

        i=0;
        length=(int32_t)(args.target-buffer);
    } else {
        /* write the lead surrogate from the overflow buffer */
        buffer[0]=(UChar)c;
        args.target=buffer+1;
        i=0;
        length=1;
    }

    /* buffer contents starts at i and ends before length */

    if(U_FAILURE(*err)) {
        c=0xffff; /* no output */
    } else if(length==0) {
        /* no input or only state changes */
        *err=U_INDEX_OUTOFBOUNDS_ERROR;
        /* no need to reset explicitly because _toUnicodeWithCallback() did it */
        c=0xffff; /* no output */
    } else {
        c=buffer[0];
        i=1;
        if(!U16_IS_LEAD(c)) {
            /* consume c=buffer[0], done */
        } else {
            /* got a lead surrogate, see if a trail surrogate follows */
            UChar c2;

            if(cnv->UCharErrorBufferLength>0) {
                /* got overflow output from the conversion */
                if(U16_IS_TRAIL(c2=cnv->UCharErrorBuffer[0])) {
                    /* got a trail surrogate, too */
                    c=U16_GET_SUPPLEMENTARY(c, c2);

                    /* move the remaining overflow contents up to the beginning */
                    if((--cnv->UCharErrorBufferLength)>0) {
                        uprv_memmove(cnv->UCharErrorBuffer, cnv->UCharErrorBuffer+1,
                                     cnv->UCharErrorBufferLength*U_SIZEOF_UCHAR);
                    }
                } else {
                    /* c is an unpaired lead surrogate, just return it */
                }
            } else if(args.source<sourceLimit) {
                /* convert once more, to buffer[1] */
                args.targetLimit=buffer+2;
                _toUnicodeWithCallback(&args, err);
                if(*err==U_BUFFER_OVERFLOW_ERROR) {
                    *err=U_ZERO_ERROR;
                }

                length=(int32_t)(args.target-buffer);
                if(U_SUCCESS(*err) && length==2 && U16_IS_TRAIL(c2=buffer[1])) {
                    /* got a trail surrogate, too */
                    c=U16_GET_SUPPLEMENTARY(c, c2);
                    i=2;
                }
            }
        }
    }

    /*
     * move leftover output from buffer[i..length[
     * into the beginning of the overflow buffer
     */
    if(i<length) {
        /* move further overflow back */
        int32_t delta=length-i;
        if((length=cnv->UCharErrorBufferLength)>0) {
            uprv_memmove(cnv->UCharErrorBuffer+delta, cnv->UCharErrorBuffer,
                         length*U_SIZEOF_UCHAR);
        }
        cnv->UCharErrorBufferLength=(int8_t)(length+delta);

        cnv->UCharErrorBuffer[0]=buffer[i++];
        if(delta>1) {
            cnv->UCharErrorBuffer[1]=buffer[i];
        }
    }

    *source=args.source;
    return c;
}

/* ucnv_convert() and siblings ---------------------------------------------- */

U_CAPI void U_EXPORT2
ucnv_convertEx(UConverter *targetCnv, UConverter *sourceCnv,
               char **target, const char *targetLimit,
               const char **source, const char *sourceLimit,
               UChar *pivotStart, UChar **pivotSource,
               UChar **pivotTarget, const UChar *pivotLimit,
               UBool reset, UBool flush,
               UErrorCode *pErrorCode) {
    UChar pivotBuffer[CHUNK_SIZE];
    UChar *myPivotSource, *myPivotTarget;

    /* error checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }

    if( targetCnv==NULL || sourceCnv==NULL ||
        source==NULL || *source==NULL ||
        target==NULL || *target==NULL || targetLimit==NULL
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if(pivotStart==NULL) {
        /* use the stack pivot buffer */
        pivotStart=myPivotSource=myPivotTarget=pivotBuffer;
        pivotSource=&myPivotSource;
        pivotTarget=&myPivotTarget;
        pivotLimit=pivotBuffer+CHUNK_SIZE;
    } else if(  pivotStart>=pivotLimit ||
                pivotSource==NULL || *pivotSource==NULL ||
                pivotTarget==NULL || *pivotTarget==NULL ||
                pivotLimit==NULL
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    if(sourceLimit==NULL) {
        /* get limit of single-byte-NUL-terminated source string */
        sourceLimit=uprv_strchr(*source, 0);
    }

    if(reset) {
        ucnv_resetToUnicode(sourceCnv);
        ucnv_resetFromUnicode(targetCnv);
        *pivotTarget=*pivotSource=pivotStart;
    }

    /* conversion loop */
    for(;;) {
        if(reset) {
            /*
             * if we did a reset in this function, we know that there is nothing
             * to convert to the target yet, so we save a function call
             */
            reset=FALSE;
        } else {
            /*
             * convert to the target first in case the pivot is filled at entry
             * or the targetCnv has some output bytes in its state
             */
            ucnv_fromUnicode(targetCnv,
                             target, targetLimit,
                             (const UChar **)pivotSource, *pivotTarget,
                             NULL,
                             (UBool)(flush && *source==sourceLimit),
                             pErrorCode);
            if(U_FAILURE(*pErrorCode)) {
                break;
            }

            /* ucnv_fromUnicode() must have consumed the pivot contents since it returned with U_SUCCESS() */
            *pivotSource=*pivotTarget=pivotStart;
        }

        /* convert from the source to the pivot */
        ucnv_toUnicode(sourceCnv,
                       pivotTarget, pivotLimit,
                       source, sourceLimit,
                       NULL,
                       flush,
                       pErrorCode);
        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
            /* pivot overflow: continue with the conversion loop */
            *pErrorCode=U_ZERO_ERROR;
        } else if(U_FAILURE(*pErrorCode) || *pivotTarget==pivotStart) {
            /* conversion error, or there was nothing left to convert */
            break;
        }
        /* else ucnv_toUnicode() wrote into the pivot buffer: continue */
    }

    /*
     * The conversion loop is exited when one of the following is true:
     * - the entire source text has been converted successfully to the target buffer
     * - a target buffer overflow occurred
     * - a conversion error occurred
     */

    /* terminate the target buffer if possible */
    if(flush && U_SUCCESS(*pErrorCode)) {
        if(*target!=targetLimit) {
            **target=0;
            if(*pErrorCode==U_STRING_NOT_TERMINATED_WARNING) {
                *pErrorCode=U_ZERO_ERROR;
            }
        } else {
            *pErrorCode=U_STRING_NOT_TERMINATED_WARNING;
        }
    }
}

/* internal implementation of ucnv_convert() etc. with preflighting */
static int32_t
ucnv_internalConvert(UConverter *outConverter, UConverter *inConverter,
                     char *target, int32_t targetCapacity,
                     const char *source, int32_t sourceLength,
                     UErrorCode *pErrorCode) {
    UChar pivotBuffer[CHUNK_SIZE];
    UChar *pivot, *pivot2;

    char *myTarget;
    const char *sourceLimit;
    const char *targetLimit;
    int32_t targetLength=0;

    /* set up */
    if(sourceLength<0) {
        sourceLimit=uprv_strchr(source, 0);
    } else {
        sourceLimit=source+sourceLength;
    }

    /* if there is no input data, we're done */
    if(source==sourceLimit) {
        return u_terminateChars(target, targetCapacity, 0, pErrorCode);
    }

    pivot=pivot2=pivotBuffer;
    myTarget=target;
    targetLength=0;

    if(targetCapacity>0) {
        /* perform real conversion */
        targetLimit=target+targetCapacity;
        ucnv_convertEx(outConverter, inConverter,
                       &myTarget, targetLimit,
                       &source, sourceLimit,
                       pivotBuffer, &pivot, &pivot2, pivotBuffer+CHUNK_SIZE,
                       FALSE,
                       TRUE,
                       pErrorCode);
        targetLength=myTarget-target;
    }

    /*
     * If the output buffer is exhausted (or we are only "preflighting"), we need to stop writing
     * to it but continue the conversion in order to store in targetCapacity
     * the number of bytes that was required.
     */
    if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR || targetCapacity==0)
    {
        char targetBuffer[CHUNK_SIZE];

        targetLimit=targetBuffer+CHUNK_SIZE;
        do {
            *pErrorCode=U_ZERO_ERROR;
            myTarget=targetBuffer;
            ucnv_convertEx(outConverter, inConverter,
                           &myTarget, targetLimit,
                           &source, sourceLimit,
                           pivotBuffer, &pivot, &pivot2, pivotBuffer+CHUNK_SIZE,
                           FALSE,
                           TRUE,
                           pErrorCode);
            targetLength+=(myTarget-targetBuffer);
        } while(*pErrorCode==U_BUFFER_OVERFLOW_ERROR);

        /* done with preflighting, set warnings and errors as appropriate */
        return u_terminateChars(target, targetCapacity, targetLength, pErrorCode);
    }

    /* no need to call u_terminateChars() because ucnv_convertEx() took care of that */
    return targetLength;
}

U_CAPI int32_t U_EXPORT2
ucnv_convert(const char *toConverterName, const char *fromConverterName,
             char *target, int32_t targetCapacity,
             const char *source, int32_t sourceLength,
             UErrorCode *pErrorCode) {
    UConverter in, out; /* stack-allocated */
    UConverter *inConverter, *outConverter;
    int32_t targetLength;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( source==NULL || sourceLength<-1 ||
        targetCapacity<0 || (targetCapacity>0 && target==NULL)
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* if there is no input data, we're done */
    if(sourceLength==0 || (sourceLength<0 && *source==0)) {
        return u_terminateChars(target, targetCapacity, 0, pErrorCode);
    }

    /* create the converters */
    inConverter=ucnv_createConverter(&in, fromConverterName, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    outConverter=ucnv_createConverter(&out, toConverterName, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        ucnv_close(inConverter);
        return 0;
    }

    targetLength=ucnv_internalConvert(outConverter, inConverter,
                                      target, targetCapacity,
                                      source, sourceLength,
                                      pErrorCode);

    ucnv_close(inConverter);
    ucnv_close(outConverter);

    return targetLength;
}

/* @internal */
static int32_t
ucnv_convertAlgorithmic(UBool convertToAlgorithmic,
                        UConverterType algorithmicType,
                        UConverter *cnv,
                        char *target, int32_t targetCapacity,
                        const char *source, int32_t sourceLength,
                        UErrorCode *pErrorCode) {
    UConverter algoConverterStatic; /* stack-allocated */
    UConverter *algoConverter, *to, *from;
    int32_t targetLength;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( cnv==NULL || source==NULL || sourceLength<-1 ||
        targetCapacity<0 || (targetCapacity>0 && target==NULL)
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* if there is no input data, we're done */
    if(sourceLength==0 || (sourceLength<0 && *source==0)) {
        return u_terminateChars(target, targetCapacity, 0, pErrorCode);
    }

    /* create the algorithmic converter */
    algoConverter=ucnv_createAlgorithmicConverter(&algoConverterStatic, algorithmicType,
                                                  "", 0, pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /* reset the other converter */
    if(convertToAlgorithmic) {
        /* cnv->Unicode->algo */
        ucnv_resetToUnicode(cnv);
        to=algoConverter;
        from=cnv;
    } else {
        /* algo->Unicode->cnv */
        ucnv_resetFromUnicode(cnv);
        from=algoConverter;
        to=cnv;
    }

    targetLength=ucnv_internalConvert(to, from,
                                      target, targetCapacity,
                                      source, sourceLength,
                                      pErrorCode);

    ucnv_close(algoConverter);

    return targetLength;
}

U_CAPI int32_t U_EXPORT2
ucnv_toAlgorithmic(UConverterType algorithmicType,
                   UConverter *cnv,
                   char *target, int32_t targetCapacity,
                   const char *source, int32_t sourceLength,
                   UErrorCode *pErrorCode) {
    return ucnv_convertAlgorithmic(TRUE, algorithmicType, cnv,
                                   target, targetCapacity,
                                   source, sourceLength,
                                   pErrorCode);
}

U_CAPI int32_t U_EXPORT2
ucnv_fromAlgorithmic(UConverter *cnv,
                     UConverterType algorithmicType,
                     char *target, int32_t targetCapacity,
                     const char *source, int32_t sourceLength,
                     UErrorCode *pErrorCode) {
    return ucnv_convertAlgorithmic(FALSE, algorithmicType, cnv,
                                   target, targetCapacity,
                                   source, sourceLength,
                                   pErrorCode);
}

U_CAPI UConverterType  U_EXPORT2
ucnv_getType(const UConverter* converter)
{
    int8_t type = converter->sharedData->staticData->conversionType;
#if !UCONFIG_NO_LEGACY_CONVERSION
    if(type == UCNV_MBCS) {
        return _MBCSGetType(converter);
    }
#endif
    return (UConverterType)type;
}

U_CAPI void  U_EXPORT2
ucnv_getStarters(const UConverter* converter, 
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

static const UAmbiguousConverter *ucnv_getAmbiguous(const UConverter *cnv)
{
    UErrorCode errorCode;
    const char *name;
    int32_t i;

    if(cnv==NULL) {
        return NULL;
    }

    errorCode=U_ZERO_ERROR;
    name=ucnv_getName(cnv, &errorCode);
    if(U_FAILURE(errorCode)) {
        return NULL;
    }

    for(i=0; i<(int32_t)(sizeof(ambiguousConverters)/sizeof(UAmbiguousConverter)); ++i)
    {
        if(0==uprv_strcmp(name, ambiguousConverters[i].name))
        {
            return ambiguousConverters+i;
        }
    }

    return NULL;
}

U_CAPI void  U_EXPORT2
ucnv_fixFileSeparator(const UConverter *cnv, 
                      UChar* source, 
                      int32_t sourceLength) {
    const UAmbiguousConverter *a;
    int32_t i;
    UChar variant5c;

    if(cnv==NULL || source==NULL || sourceLength<=0 || (a=ucnv_getAmbiguous(cnv))==NULL)
    {
        return;
    }

    variant5c=a->variant5c;
    for(i=0; i<sourceLength; ++i) {
        if(source[i]==variant5c) {
            source[i]=0x5c;
        }
    }
}

U_CAPI UBool  U_EXPORT2
ucnv_isAmbiguous(const UConverter *cnv) {
    return (UBool)(ucnv_getAmbiguous(cnv)!=NULL);
}

U_CAPI void  U_EXPORT2
ucnv_setFallback(UConverter *cnv, UBool usesFallback)
{
    cnv->useFallback = usesFallback;
}

U_CAPI UBool  U_EXPORT2
ucnv_usesFallback(const UConverter *cnv)
{
    return cnv->useFallback;
}

U_CAPI void  U_EXPORT2
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

U_CAPI void  U_EXPORT2
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

#define SIG_MAX_LEN 5

U_CAPI const char* U_EXPORT2
ucnv_detectUnicodeSignature( const char* source,
                             int32_t sourceLength,
                             int32_t* signatureLength,
                             UErrorCode* pErrorCode) {
    int32_t dummy;

    /* initial 0xa5 bytes: make sure that if we read <SIG_MAX_LEN
     * bytes we don't misdetect something 
     */
    char start[SIG_MAX_LEN]={ '\xa5', '\xa5', '\xa5', '\xa5', '\xa5' };
    int i = 0;

    if((pErrorCode==NULL) || U_FAILURE(*pErrorCode)){
        return NULL;
    }
    
    if(source == NULL || sourceLength < -1){
        *pErrorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    if(signatureLength == NULL) {
        signatureLength = &dummy;
    }

    if(sourceLength==-1){
        sourceLength=uprv_strlen(source);
    }

    
    while(i<sourceLength&& i<SIG_MAX_LEN){
        start[i]=source[i];
        i++;
    }

    if(start[0] == '\xFE' && start[1] == '\xFF') {
        *signatureLength=2;
        return  "UTF-16BE";
    } else if(start[0] == '\xFF' && start[1] == '\xFE') {
        if(start[2] == '\x00' && start[3] =='\x00') {
            *signatureLength=4;
            return "UTF-32LE";
        } else {
            *signatureLength=2;
            return  "UTF-16LE";
        }
    } else if(start[0] == '\xEF' && start[1] == '\xBB' && start[2] == '\xBF') {
        *signatureLength=3;
        return  "UTF-8";
    } else if(start[0] == '\x00' && start[1] == '\x00' && 
              start[2] == '\xFE' && start[3]=='\xFF') {
        *signatureLength=4;
        return  "UTF-32BE";
    } else if(start[0] == '\x0E' && start[1] == '\xFE' && start[2] == '\xFF') {
        *signatureLength=3;
        return "SCSU";
    } else if(start[0] == '\xFB' && start[1] == '\xEE' && start[2] == '\x28') {
        *signatureLength=3;
        return "BOCU-1";
    } else if(start[0] == '\x2B' && start[1] == '\x2F' && start[2] == '\x76') {
        /*
         * UTF-7: Initial U+FEFF is encoded as +/v8  or  +/v9  or  +/v+  or  +/v/
         * depending on the second UTF-16 code unit.
         * Detect the entire, closed Unicode mode sequence +/v8- for only U+FEFF
         * if it occurs.
         *
         * So far we have +/v
         */
        if(start[3] == '\x38' && start[4] == '\x2D') {
            /* 5 bytes +/v8- */
            *signatureLength=5;
            return "UTF-7";
        } else if(start[3] == '\x38' || start[3] == '\x39' || start[3] == '\x2B' || start[3] == '\x2F') {
            /* 4 bytes +/v8  or  +/v9  or  +/v+  or  +/v/ */
            *signatureLength=4;
            return "UTF-7";
        }
    }

    /* no known Unicode signature byte sequence recognized */
    *signatureLength=0;
    return NULL;
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
