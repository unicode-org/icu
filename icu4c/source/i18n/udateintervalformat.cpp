/*
*****************************************************************************************
* Copyright (C) 2010-2011, International Business Machines
* Corporation and others. All Rights Reserved.
*****************************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/udateintervalformat.h"
#include "unicode/dtitvfmt.h"
#include "unicode/dtintrv.h"
#include "unicode/timezone.h"
#include "unicode/locid.h"
#include "unicode/unistr.h"

U_NAMESPACE_USE


U_CAPI UDateIntervalFormat* U_EXPORT2
udtitvfmt_open(const char*  locale,
              const UChar* skeleton,
              int32_t      skeletonLength,
              const UChar* tzID,
              int32_t      tzIDLength,
              UErrorCode*  status)
{
    if (U_FAILURE(*status)) {
        return 0;
    }
    UnicodeString skel((UBool)(skeletonLength == -1), skeleton, skeletonLength);
    DateIntervalFormat* formatter = DateIntervalFormat::createInstance(skel, Locale(locale), *status);
    if (formatter == 0) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return 0;
    }
    if(tzID != 0) {
        TimeZone *zone = TimeZone::createTimeZone(UnicodeString((UBool)(tzIDLength == -1), tzID, tzIDLength));
        if(zone == 0) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            delete formatter;
            return 0;
        }
        formatter->adoptTimeZone(zone);
    }
    return (UDateIntervalFormat*)formatter;
}


U_CAPI void U_EXPORT2
udtitvfmt_close(UDateIntervalFormat *formatter)
{
    delete (DateIntervalFormat*)formatter;
}


U_CAPI int32_t U_EXPORT2
udtitvfmt_format(const UDateIntervalFormat* formatter,
                UDate           fromDate,
                UDate           toDate,
                UChar*          result,
                int32_t         resultCapacity,
                UFieldPosition* position,
                UErrorCode*     status)
{
    if (U_FAILURE(*status)) {
        return -1;
    }
    UnicodeString res;
    if (!(result==NULL && resultCapacity==0)) {
        // NULL destination for pure preflighting: empty dummy string
        // otherwise, alias the destination buffer (copied from udat_format)
        res.setTo(result, 0, resultCapacity);
    }
    FieldPosition fp;
    if (position != 0) {
        fp.setField(position->field);
    }

    DateInterval interval = DateInterval(fromDate,toDate);
    ((const DateIntervalFormat*)formatter)->format( &interval, res, fp, *status );
    if (U_FAILURE(*status)) {
        return -1;
    }
    if (position != 0) {
        position->beginIndex = fp.getBeginIndex();
        position->endIndex = fp.getEndIndex();
    }

    return res.extract(result, resultCapacity, *status);
}


#endif /* #if !UCONFIG_NO_FORMATTING */
