/*
******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
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
#include "ustr_imp.h"
#include "ucnv_imp.h"
#include "ucnv_io.h"
#include "ucnv_cnv.h"
#include "ucnv_bld.h"

#if 0
/* debugging for converters */
# include <stdio.h>
void UCNV_DEBUG_LOG(const char *what, const char *who, const void *p, int l)
{
    static FILE *f = NULL;
    if(f==NULL)
    {
        /* stderr, or open another file */
        f = stderr;
        /*  f = fopen("c:\\UCNV_DEBUG_LOG.txt", "w"); */
    }
    if (!what) {
        what = "(null)";
    }
    if (!who) {
        who = "(null)";
    }
    if (!p) {
        p = "(null)";
    }

    fprintf(f, "%p\t:%d\t%-20s\t%-10s\n",
        p, l, who, what);

    fflush(f);
}


/* dump the contents of a converter */
static void UCNV_DEBUG_CNV(const UConverter *c, int line)
{
    UErrorCode err = U_ZERO_ERROR;
    fprintf(stderr, "%p\t:%d\t", c, line);
    if(c!=NULL) {
        const char *name = ucnv_getName(c, &err);
        if (!name) {
            name = "(null)";
        }
        fprintf(stderr, "%s\t", name);

        fprintf(stderr, "shr=%p, ref=%x\n", 
            c->sharedData,
            c->sharedData->referenceCounter);
    } else { 
        fprintf(stderr, "DEMISED\n");
    }
}

# define UCNV_DEBUG 1
# define UCNV_DEBUG_LOG(x,y,z) UCNV_DEBUG_LOG(x,y,z,__LINE__)
# define UCNV_DEBUG_CNV(c) UCNV_DEBUG_CNV(c, __LINE__)
#else
# define UCNV_DEBUG_LOG(x,y,z)
# define UCNV_DEBUG_CNV(c)
#endif



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
        UCNV_DEBUG_LOG("open", name, NULL);
        return NULL;
    }

    r =  ucnv_createConverter(NULL, name, err);
    UCNV_DEBUG_LOG("open", name, r);
    UCNV_DEBUG_CNV(r);
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

    if (status == NULL || U_FAILURE(*status)){
        return 0;
    }

    if (!pBufferSize || !cnv){
       *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    UCNV_DEBUG_LOG("cloning FROM", ucnv_getName(cnv,status), cnv);
    UCNV_DEBUG_LOG("cloning WITH", "memory", stackBuffer);
    UCNV_DEBUG_CNV(cnv);

    /* Pointers on 64-bit platforms need to be aligned
     * on a 64-bit boundry in memory.
     */
    if (U_ALIGNMENT_OFFSET(stackBuffer) != 0) {
        int32_t offsetUp = (int32_t)U_ALIGNMENT_OFFSET_UP(stackBufferChars);
        *pBufferSize -= offsetUp;
        stackBufferChars += offsetUp;
    }

    stackBuffer = (void *)stackBufferChars;
    
    if (cnv->sharedData->impl->safeClone != NULL) {
        /* call the custom safeClone function for sizing */
        bufferSizeNeeded = 0;
        cnv->sharedData->impl->safeClone(cnv, stackBuffer, &bufferSizeNeeded, status);
    }
    else
    {
        /* inherent sizing */
        bufferSizeNeeded = sizeof(UConverter);
    }

    if (*pBufferSize <= 0){ /* 'preflighting' request - set needed size into *pBufferSize */
        *pBufferSize = bufferSizeNeeded;
        return 0;
    }


    /* Now, see if we must allocate any memory */
    if (*pBufferSize < bufferSizeNeeded || stackBuffer == NULL)
    {
        /* allocate one here...*/
        localConverter = allocatedConverter = (UConverter *) uprv_malloc (bufferSizeNeeded);

        if(localConverter == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
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
#ifdef UCNV_DEBUG
        fprintf(stderr, "%p\t:%d\t\t==stackbuffer %p, isCopyLocal TRUE\n",
                localConverter, __LINE__, stackBuffer);
#endif

    } else {
#ifdef UCNV_DEBUG
        fprintf(stderr, "%p\t:%d\t\t!=stackbuffer %p, isCopyLocal left at %s\n",
                localConverter, __LINE__, stackBuffer,
                localConverter->isCopyLocal?"TRUE":"FALSE");
#endif
    }

    localConverter->isExtraLocal = localConverter->isCopyLocal;

    /* allow callback functions to handle any memory allocation */
    toUArgs.converter = fromUArgs.converter = localConverter;
    cbErr = U_ZERO_ERROR;
    cnv->fromCharErrorBehaviour(cnv->toUContext, &toUArgs, NULL, 0, UCNV_CLONE, &cbErr);
    cbErr = U_ZERO_ERROR;
    cnv->fromUCharErrorBehaviour(cnv->fromUContext, &fromUArgs, NULL, 0, 0, UCNV_CLONE, &cbErr);

    UCNV_DEBUG_LOG("cloning TO", ucnv_getName(localConverter,status), localConverter);
    UCNV_DEBUG_CNV(localConverter);
    UCNV_DEBUG_CNV(cnv);


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

    if (converter == NULL)
    {
        return;
    }

    UCNV_DEBUG_LOG("close", ucnv_getName(converter, &errorCode), converter);
    UCNV_DEBUG_CNV(converter);

    toUArgs.converter = fromUArgs.converter = converter;

    converter->fromCharErrorBehaviour(converter->toUContext, &toUArgs, NULL, 0, UCNV_CLOSE, &errorCode);
    errorCode = U_ZERO_ERROR;
    converter->fromUCharErrorBehaviour(converter->fromUContext, &fromUArgs, NULL, 0, 0, UCNV_CLOSE, &errorCode);

    UCNV_DEBUG_CNV(converter);
        
    if (converter->sharedData->impl->close != NULL) {
        converter->sharedData->impl->close(converter);
    }

#ifdef UCNV_DEBUG
    {
        char c[4];
        c[0]='0'+converter->sharedData->referenceCounter;
        c[1]=0;
        UCNV_DEBUG_LOG("close--", c, converter);
        if((converter->sharedData->referenceCounter == 0)&&(converter->sharedData->sharedDataCached == FALSE)) {
            UCNV_DEBUG_CNV(converter);
            UCNV_DEBUG_LOG("close:delDead", "??", converter);
        }
    }
#endif

    /*
    Checking whether it's an algorithic converter is okay
    in multithreaded applications because the value never changes.
    Don't check referenceCounter for any other value.
    */
    if (converter->sharedData->referenceCounter != ~0) {
        ucnv_unloadSharedDataIfReady(converter->sharedData);
    }

    if(!converter->isCopyLocal){
        UCNV_DEBUG_LOG("close:free", "", converter);
        uprv_free (converter);
    }
    return;
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
static void _reset(UConverter *converter, UConverterResetChoice choice) {
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
    if(choice<=UCNV_RESET_TO_UNICODE) {
        errorCode = U_ZERO_ERROR;
        converter->fromCharErrorBehaviour(converter->toUContext, &toUArgs, NULL, 0, UCNV_RESET, &errorCode);
    }
    if(choice!=UCNV_RESET_TO_UNICODE) {
        errorCode = U_ZERO_ERROR;
        converter->fromUCharErrorBehaviour(converter->fromUContext, &fromUArgs, NULL, 0, 0, UCNV_RESET, &errorCode);
    }

    /* now reset the converter itself */
    if(choice<=UCNV_RESET_TO_UNICODE) {
        converter->toUnicodeStatus = converter->sharedData->toUnicodeStatus;
        converter->toULength = 0;
        converter->invalidCharLength = converter->UCharErrorBufferLength = 0;
    }
    if(choice!=UCNV_RESET_TO_UNICODE) {
        converter->fromUnicodeStatus = 0;
        converter->fromUSurrogateLead = 0;
        converter->invalidUCharLength = converter->charErrorBufferLength = 0;
    }

    if (converter->sharedData->impl->reset != NULL) {
        /* call the custom reset function */
        converter->sharedData->impl->reset(converter, choice);
    } else if(choice<=UCNV_RESET_TO_UNICODE) {
        converter->mode = UCNV_SI;
    }
}

U_CAPI void  U_EXPORT2
ucnv_reset(UConverter *converter)
{
    _reset(converter, UCNV_RESET_BOTH);
}

U_CAPI void  U_EXPORT2
ucnv_resetToUnicode(UConverter *converter)
{
    _reset(converter, UCNV_RESET_TO_UNICODE);
}

U_CAPI void  U_EXPORT2
ucnv_resetFromUnicode(UConverter *converter)
{
    _reset(converter, UCNV_RESET_FROM_UNICODE);
}

U_CAPI int8_t   U_EXPORT2
ucnv_getMaxCharSize (const UConverter * converter)
{
    return converter->sharedData->staticData->maxBytesPerChar;
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
                   USet *set,
                   UConverterUnicodeSet which,
                   UErrorCode *pErrorCode) {
    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return;
    }
    if(cnv==NULL || set==NULL || which<UCNV_ROUNDTRIP_SET || UCNV_SET_COUNT<=which) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /* does this converter support this function? */
    if(cnv->sharedData->impl->getUnicodeSet==NULL) {
        *pErrorCode=U_UNSUPPORTED_ERROR;
        return;
    }

    /* empty the set */
    uset_clear(set);

    /* call the converter to add the code points it supports */
    cnv->sharedData->impl->getUnicodeSet(cnv, set, which, pErrorCode);
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

U_CAPI void  U_EXPORT2
ucnv_fromUnicode (UConverter * _this,
                       char **target,
                       const char *targetLimit,
                       const UChar ** source,
                       const UChar * sourceLimit,
                       int32_t* offsets,
                       UBool flush,
                       UErrorCode * err)
{
    UConverterFromUnicodeArgs args;
    const char *t;

    /*
    * Check parameters in for all conversions
    */
    if (err == NULL || U_FAILURE (*err)) {
        return;
    }

    if (_this == NULL || target == NULL || source == NULL) {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    t = *target;
    if (targetLimit < t || sourceLimit < *source)
    {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /*
    * Make sure that the target buffer size does not exceed the number range for int32_t
    * because some functions use the size rather than comparing pointers.
    * size_t is guaranteed to be unsigned.
    */
    if((size_t)(targetLimit - t) > (size_t)0x7fffffff && targetLimit > t)
    {
        targetLimit = t + 0x7fffffff;
    }
    
    /*
    * Deal with stored carry over data.  This is done in the common location
    * to avoid doing it for each conversion.
    */
    if (_this->charErrorBufferLength > 0)
    {
        int32_t myTargetIndex = 0;
        
        ucnv_flushInternalCharBuffer (_this, 
                (char *)t,
                &myTargetIndex,
                targetLimit - *target,
                offsets?&offsets:NULL,
                err);
        *target += myTargetIndex;
        if (U_FAILURE (*err))
            return;
    }

    if(!flush && *source == sourceLimit) {
        /* the overflow buffer is emptied and there is no new input: we are done */
        return;
    }

    args.converter = _this;
    args.flush = flush;
    args.offsets = offsets;
    args.source = *source;
    args.sourceLimit = sourceLimit;
    args.target = *target;
    args.targetLimit = targetLimit;
    args.size = sizeof(args);
    if (offsets)
    {
        if (_this->sharedData->impl->fromUnicodeWithOffsets != NULL)
        {
            _this->sharedData->impl->fromUnicodeWithOffsets(&args, err);
            *source = args.source;
            *target = args.target;
            return;
        }
        else {
            /* there is no implementation that sets offsets, set them all to -1 */
            int32_t i, targetSize = targetLimit - *target;
            
            for (i=0; i<targetSize; i++) {
                offsets[i] = -1;
            }
        }
    }
    
    /*calls the specific conversion routines */
    _this->sharedData->impl->fromUnicode(&args, err);
    *source = args.source;
    *target = args.target;
}



U_CAPI void    U_EXPORT2
ucnv_toUnicode (UConverter * _this,
                UChar ** target,
                const UChar * targetLimit,
                const char **source,
                const char *sourceLimit,
                int32_t* offsets,
                UBool flush,
                UErrorCode * err)
{
    UConverterToUnicodeArgs args;
    const UChar *t;

    /*
    * Check parameters in for all conversions
    */
    if (err == NULL || U_FAILURE (*err)) {
        return;
    }

    if (_this == NULL || target == NULL || source == NULL) {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    t = *target;
    if (targetLimit < t || sourceLimit < *source) {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /*
    * Make sure that the target buffer size does not exceed the number range for int32_t
    * because some functions use the size rather than comparing pointers.
    * size_t is guaranteed to be unsigned.
    */
    if((size_t)(targetLimit - t) > (size_t)0x3fffffff && targetLimit > t) {
        targetLimit = t + 0x3fffffff;
    }

    /*
    * Deal with stored carry over data.  This is done in the common location
    * to avoid doing it for each conversion.
    */
    if (_this->UCharErrorBufferLength > 0)
    {
        int32_t myTargetIndex = 0;

        ucnv_flushInternalUnicodeBuffer (_this, 
                (UChar *)t,
                &myTargetIndex,
                targetLimit - *target,
                offsets?&offsets:NULL,
                err);
        *target += myTargetIndex;
        if (U_FAILURE (*err))
            return;
    }

    if(!flush && *source == sourceLimit) {
        /* the overflow buffer is emptied and there is no new input: we are done */
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
            /* there is no implementation that sets offsets, set them all to -1 */
            int32_t i, targetSize = targetLimit - *target;
            
            for (i=0; i<targetSize; i++) {
                offsets[i] = -1;
            }
        }
    }

    /*calls the specific conversion routines */
    _this->sharedData->impl->toUnicode(&args, err); 

    *source = args.source;
    *target = args.target;
    return;
}

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

U_CAPI UChar32  U_EXPORT2
ucnv_getNextUChar(UConverter * converter,
                  const char **source,
                  const char *sourceLimit,
                  UErrorCode * err)
{
    UConverterToUnicodeArgs args;
    UChar32 ch;

    if(err == NULL || U_FAILURE(*err)) {
        return 0xffff;
    }

    if(converter == NULL || source == NULL || sourceLimit < *source) {
        *err = U_ILLEGAL_ARGUMENT_ERROR;
        return 0xffff;
    }

    /* In case internal data had been stored
    * we return the first UChar32 in the internal buffer,
    * and update the internal state accordingly
    */
    if (converter->UCharErrorBufferLength > 0)
    {
        int32_t i = 0;
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
    if (converter->sharedData->impl->getNextUChar != NULL)
    {
        ch = converter->sharedData->impl->getNextUChar(&args, err);
    } else {
        /* default implementation */
        ch = ucnv_getNextUCharFromToUImpl(&args, converter->sharedData->impl->toUnicode, FALSE, err);
    }
    *source = args.source;
    return ch;
}

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
