/*
 * Copyright (C) 2008, International Business Machines Corporation and Others.
 * All rights reserved.
 */

#include "unicode/utypes.h"
#include "cmemory.h"
#include "unicode/bms.h"
#include "unicode/unistr.h"
#include "unicode/colldata.h"
#include "unicode/bmsearch.h"

U_CAPI UCD * U_EXPORT2
ucd_open(UCollator *coll)
{
    return (UCD *) CollData::open(coll);
}

U_CAPI void U_EXPORT2
ucd_close(UCD *ucd)
{
    CollData *data = (CollData *) ucd;

    CollData::close(data);
}

U_CAPI void U_EXPORT2
ucd_freeCache()
{
    CollData::freeCollDataCache();
}

struct BMS
{
    BoyerMooreSearch *bms;
    const UnicodeString *targetString;
};

U_CAPI BMS * U_EXPORT2
bms_open(UCD *ucd,
         const UChar *pattern, int32_t patternLength,
         const UChar *target,  int32_t targetLength)
{
    BMS *bms = (BMS *) uprv_malloc(sizeof(BMS));

    CollData *data = (CollData *) ucd;
    UnicodeString patternString(pattern, patternLength);

    if (target != NULL) {
        bms->targetString = new UnicodeString(target, targetLength);
    } else {
        bms->targetString = NULL;
    }

    bms->bms = new BoyerMooreSearch(data, patternString, bms->targetString);

    return bms;
}

U_CAPI void U_EXPORT2
bms_close(BMS *bms)
{
    delete bms->bms;

    delete bms->targetString;

    uprv_free(bms);
}

U_CAPI UBool U_EXPORT2
bms_empty(BMS *bms)
{
    return bms->bms->empty();
}

U_CAPI UBool U_EXPORT2
bms_search(BMS *bms, int32_t offset, int32_t *start, int32_t *end)
{
    return bms->bms->search(offset, *start, *end);
}

U_CAPI void U_EXPORT2
bms_setTargetString(BMS *bms, const UChar *target, int32_t targetLength)
{
    if (bms->targetString != NULL) {
        delete bms->targetString;
    }

    if (target != NULL) {
        bms->targetString = new UnicodeString(target, targetLength);
    } else {
        bms->targetString = NULL;
    }

    bms->bms->setTargetString(bms->targetString);
}
