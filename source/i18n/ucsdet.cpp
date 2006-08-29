/*
 ********************************************************************************
 *   Copyright (C) 2005-2006, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 ********************************************************************************
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_CONVERSION
#include "unicode/ucsdet.h"
#include "csdetect.h"
#include "csmatch.h"

#include "cmemory.h"

#define ARRAY_SIZE(array) (sizeof array / sizeof array[0])

#define NEW_ARRAY(type,count) (type *) uprv_malloc((count) * sizeof(type))
#define DELETE_ARRAY(array) uprv_free((void *) (array))

U_CDECL_BEGIN

U_CAPI UCharsetDetector * U_EXPORT2
ucsdet_open(UErrorCode   *status)
{
    if(U_FAILURE(*status)) {
        return 0;
    }

    CharsetDetector* csd = new CharsetDetector(*status);

    if (U_FAILURE(*status)) {
        delete csd;
        csd = NULL;
    }

    return (UCharsetDetector *) csd;
}

U_CAPI void U_EXPORT2
ucsdet_close(UCharsetDetector *ucsd)
{
    CharsetDetector *csd = (CharsetDetector *) ucsd;
    delete csd;
}

U_CAPI void U_EXPORT2
ucsdet_setText(UCharsetDetector *ucsd, const char *textIn, int32_t len, UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return;
    }

    if (ucsd == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    CharsetDetector *csd = (CharsetDetector *) ucsd;

    csd->setText(textIn, len);
}

U_CAPI const char * U_EXPORT2
ucsdet_getName(const UCharsetMatch *ucsm, UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return NULL;
    }

    if (ucsm == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    CharsetMatch *csm = (CharsetMatch *) ucsm;

    return csm->getName();
}

U_CAPI int32_t U_EXPORT2
ucsdet_getConfidence(const UCharsetMatch *ucsm, UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return 0;
    }

    if (ucsm == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    CharsetMatch *csm = (CharsetMatch *) ucsm;

    return csm->getConfidence();
}

U_CAPI const char * U_EXPORT2
ucsdet_getLanguage(const UCharsetMatch *ucsm, UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return NULL;
    }

    if (ucsm == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    CharsetMatch *csm = (CharsetMatch *) ucsm;

    return csm->getLanguage();
}

U_CAPI const UCharsetMatch * U_EXPORT2
ucsdet_detect(UCharsetDetector *ucsd, UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return NULL;
    }

    if (ucsd == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    CharsetDetector *csd = (CharsetDetector *) ucsd;

    return (const UCharsetMatch *) csd->detect(*status);
}

U_CAPI void U_EXPORT2
ucsdet_setDeclaredEncoding(UCharsetDetector *ucsd, const char *encoding, int32_t length, UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return;
    }

    if (ucsd == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    CharsetDetector *csd = (CharsetDetector *) ucsd;

    csd->setDeclaredEncoding(encoding,length);
}

U_CAPI const UCharsetMatch**
ucsdet_detectAll(UCharsetDetector *ucsd,
                 int32_t *maxMatchesFound, UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return NULL;
    }

    if (ucsd == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    CharsetDetector *csd = (CharsetDetector *) ucsd;

    return (const UCharsetMatch**)csd->detectAll(*maxMatchesFound,*status);
}

// U_CAPI  const char * U_EXPORT2
// ucsdet_getDetectableCharsetName(const UCharsetDetector *csd, int32_t index, UErrorCode *status)
// {
//     if(U_FAILURE(*status)) {
//         return 0;
//     }
//     return csd->getCharsetName(index,*status);
// }

// U_CAPI  int32_t U_EXPORT2
// ucsdet_getDetectableCharsetsCount(const UCharsetDetector *csd, UErrorCode *status)
// {
//     if(U_FAILURE(*status)) {
//         return -1;
//     }
//     return UCharsetDetector::getDetectableCount();
// }

U_CAPI  UBool U_EXPORT2
ucsdet_isInputFilterEnabled(const UCharsetDetector *ucsd)
{
    // todo: could use an error return...
    if (ucsd == NULL) {
        return FALSE;
    }

    CharsetDetector *csd = (CharsetDetector *) ucsd;

    return csd->getStripTagsFlag();
}

U_CAPI  UBool U_EXPORT2
ucsdet_enableInputFilter(UCharsetDetector *ucsd, UBool filter)
{
    // todo: could use an error return...
    if (ucsd == NULL) {
        return FALSE;
    }

    CharsetDetector *csd = (CharsetDetector *) ucsd;
    UBool prev = csd->getStripTagsFlag();

    csd->setStripTagsFlag(filter);

    return prev;
}

U_CAPI  int32_t U_EXPORT2
ucsdet_getUChars(const UCharsetMatch *ucsm,
                 UChar *buf, int32_t cap, UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return 0;
    }

    if (ucsm == NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    CharsetMatch *csm = (CharsetMatch *) ucsm;
    
    return csm->getUChars(buf, cap, status);
}
U_CDECL_END

#endif
