/********************************************************************
 * Copyright (c) 2016, International Business Machines Corporation
 * and others. All Rights Reserved.
 ********************************************************************/
/* C API TEST FOR DATE INTERVAL FORMAT */

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UCONFIG_NO_BREAK_ITERATION

#include "unicode/ureldatefmt.h"
#include "unicode/unum.h"
#include "unicode/udisplaycontext.h"
#include "unicode/ustring.h"
#include "cintltst.h"
#include "cmemory.h"

static void TestRelDateFmtx(void);

#define LEN(a) (sizeof(a)/sizeof(a[0]))

void addRelativeDateFormatTest(TestNode** root);

#define TESTCASE(x) addTest(root, &x, "tsformat/crelativedateformattest/" #x)

void addRelativeDateFormatTest(TestNode** root)
{
    TESTCASE(TestRelDateFmtx);
}

static const double offsets[] = { -5.0, -2.2, -1.0, -0.7, 0.0, 0.7, 1.0, 2.2, 5.0 };
enum { kNumOffsets = UPRV_LENGTHOF(offsets) };

static const char* en_defNum_long_midSent_week[kNumOffsets*2] = {
/*  text             numeric */
    "5 weeks ago",   "5 weeks ago",
    "2.2 weeks ago", "2.2 weeks ago",
    "last week",     "1 week ago",
    "last week",     "0.7 weeks ago",
    "this week",     "in 0 weeks",
    "next week",     "in 0.7 weeks",
    "next week",     "in 1 week",
    "in 2.2 weeks",  "in 2.2 weeks",
    "in 5 weeks",    "in 5 weeks",
};

typedef struct {
    const char*                         locale;
    int32_t                             decPlaces; /* fixed decimal places; -1 to use default num formatter */
    UDateRelativeDateTimeFormatterStyle width;
    UDisplayContext                     capContext;
    URelativeDateTimeUnit               unit;
    const char **                       expectedResults; /* for the various offsets */
} RelDateTimeFormatTestItem;

static const RelDateTimeFormatTestItem testItems[] = {
    { "en", -1, UDAT_STYLE_LONG, UDISPCTX_CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE, UDAT_REL_UNIT_WEEK, en_defNum_long_midSent_week },
    { NULL, 0, (UDateRelativeDateTimeFormatterStyle)0, (UDisplayContext)0, (URelativeDateTimeUnit)0, NULL } /* terminator */
};

enum { kMaxUBuf = 64 };

static void TestRelDateFmtx()
{
    const RelDateTimeFormatTestItem *itemPtr;
    log_verbose("\nTesting ureldatefmt_open(), ureldatefmt_format(), ureldatefmt_formatNumeric() with various parameters\n");
    for (itemPtr = testItems; itemPtr->locale != NULL; itemPtr++) {
        URelativeDateTimeFormatter *reldatefmt = NULL;
        UNumberFormat* nfToAdopt = NULL;
        UErrorCode status = U_ZERO_ERROR;
        int32_t iOffset;

        if (itemPtr->decPlaces >= 0) {
            nfToAdopt = unum_open(UNUM_DECIMAL, NULL, 0, itemPtr->locale, NULL, &status);
            if ( U_FAILURE(status) ) {
                log_data_err("FAIL: unum_open(UNUM_DECIMAL, ...) for locale %s: %s\n", itemPtr->locale, myErrorName(status));
            }
		    unum_setAttribute(nfToAdopt, UNUM_MIN_FRACTION_DIGITS, itemPtr->decPlaces);
		    unum_setAttribute(nfToAdopt, UNUM_MAX_FRACTION_DIGITS, itemPtr->decPlaces);
		    unum_setAttribute(nfToAdopt, UNUM_ROUNDING_MODE, UNUM_ROUND_DOWN);
        }
        reldatefmt = ureldatefmt_open(itemPtr->locale, nfToAdopt, itemPtr->width, itemPtr->capContext, &status);
        if ( U_FAILURE(status) ) {
            log_data_err("FAIL: ureldatefmt_open() for locale %s: %s\n", itemPtr->locale, myErrorName(status));
        }

        for (iOffset = 0; iOffset < kNumOffsets; iOffset++) {
            UChar ubuf[kMaxUBuf];
            int32_t ulen;

            status = U_ZERO_ERROR;
            ulen = ureldatefmt_format(reldatefmt, offsets[iOffset], itemPtr->unit, ubuf, kMaxUBuf, &status);
            /* check results */

            status = U_ZERO_ERROR;
            ulen = ureldatefmt_formatNumeric(reldatefmt, offsets[iOffset], itemPtr->unit, ubuf, kMaxUBuf, &status);
            /* check results */
        }
        
        ureldatefmt_close(reldatefmt);
    }


/*
log_err("ERROR: udtitvfmt_format for locale %s, skeleton %s, tzid %s, from %.1f, to %.1f: expect %s, get %s\n",
log_err("FAIL: udtitvfmt_format for locale %s, skeleton %s, tzid %s, from %.1f, to %.1f: %s\n",
log_data_err("FAIL: udtitvfmt_open for locale %s, skeleton %s, tzid %s - %s\n",
*/
}

#endif /* #if !UCONFIG_NO_FORMATTING && !UCONFIG_NO_BREAK_ITERATION */
