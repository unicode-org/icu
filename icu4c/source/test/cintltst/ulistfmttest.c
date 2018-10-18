// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/********************************************************************
 * Copyright (c) 2015, International Business Machines Corporation
 * and others. All Rights Reserved.
 ********************************************************************/
/* C API TEST for UListFormatter */

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ustring.h"
#include "unicode/ulistformatter.h"
#include "cintltst.h"
#include "cmemory.h"
#include "cstring.h"

static void TestUListFmt(void);
static void TestUListFmtForFields(void);

void addUListFmtTest(TestNode** root);

#define TESTCASE(x) addTest(root, &x, "tsformat/ulistfmttest/" #x)

void addUListFmtTest(TestNode** root)
{
    TESTCASE(TestUListFmt);
    TESTCASE(TestUListFmtForFields);
}

static const UChar str0[] = { 0x41,0 }; /* "A" */
static const UChar str1[] = { 0x42,0x62,0 }; /* "Bb" */
static const UChar str2[] = { 0x43,0x63,0x63,0 }; /* "Ccc" */
static const UChar str3[] = { 0x44,0x64,0x64,0x64,0 }; /* "Dddd" */
static const UChar str4[] = { 0x45,0x65,0x65,0x65,0x65,0 }; /* "Eeeee" */
static const UChar* strings[] =            { str0, str1, str2, str3, str4 };
static const int32_t stringLengths[]  =    {    1,    2,    3,    4,    5 };
static const int32_t stringLengthsNeg[] = {   -1,   -1,   -1,   -1,   -1 };

typedef struct {
    int32_t field;
    int32_t beginPos;
    int32_t endPos;
} FieldsData;

typedef struct {
  const char * locale;
  int32_t stringCount;
  const char *expectedResult; /* invariant chars + escaped Unicode */
  const FieldsData *expectedFields;
} ListFmtTestEntry;

static const FieldsData expectedFields1[] = {
    { ULISTFMT_ELEMENT_FIELD,      0, 1 },   /* "A" */
    { ULISTFMT_ELEMENT_FIELD,      3, 5 },   /* "Bb" */
    { ULISTFMT_ELEMENT_FIELD,      7, 10 },  /* "Ccc" */
    { ULISTFMT_ELEMENT_FIELD,      12, 16 }, /* "Dddd" */
    { ULISTFMT_ELEMENT_FIELD,      22, 27 }, /* "Eeeee" */
    { ULISTFMT_LITERAL_FIELD,      1, 3 },   /* ", " */
    { ULISTFMT_LITERAL_FIELD,      5, 7 },   /* ", " */
    { ULISTFMT_LITERAL_FIELD,      10, 12 }, /* ", " */
    { ULISTFMT_LITERAL_FIELD,      16, 22 }, /* ", and " */
    { -1,                          -1, -1 },
};

static const FieldsData expectedFields2[] = {
    { ULISTFMT_ELEMENT_FIELD,      0, 1 },   /* "A" */
    { ULISTFMT_ELEMENT_FIELD,      6, 8 },   /* "Bb" */
    { ULISTFMT_LITERAL_FIELD,      1, 6 },   /* " and " */
    { -1,                         -1, -1 },
};

static const FieldsData expectedFields3[] = {
    { ULISTFMT_ELEMENT_FIELD,      0, 1 },   /* "A" */
    { ULISTFMT_ELEMENT_FIELD,      3, 5 },   /* "Bb" */
    { ULISTFMT_ELEMENT_FIELD,      7, 10 },  /* "Ccc" */
    { ULISTFMT_ELEMENT_FIELD,      12, 16 }, /* "Dddd" */
    { ULISTFMT_ELEMENT_FIELD,      21, 26 }, /* "Eeeee" */
    { ULISTFMT_LITERAL_FIELD,      1, 3 },   /* ", " */
    { ULISTFMT_LITERAL_FIELD,      5, 7 },   /* ", " */
    { ULISTFMT_LITERAL_FIELD,      10, 12 }, /* ", " */
    { ULISTFMT_LITERAL_FIELD,      16, 21 }, /* " und " */
    { -1,                          -1, -1 },
};

static const FieldsData expectedFields4[] = {
    { ULISTFMT_ELEMENT_FIELD,      0, 1 },   /* "A" */
    { ULISTFMT_ELEMENT_FIELD,      6, 8 },   /* "Bb" */
    { ULISTFMT_LITERAL_FIELD,      1, 6 },   /* " und " */
    { -1,                         -1, -1 },
};

static const FieldsData expectedFields5[] = {
    { ULISTFMT_ELEMENT_FIELD,      0, 1 },   /* "A" */
    { ULISTFMT_ELEMENT_FIELD,      2, 4 },   /* "Bb" */
    { ULISTFMT_ELEMENT_FIELD,      5, 8},  /* "Ccc" */
    { ULISTFMT_ELEMENT_FIELD,      9, 13 }, /* "Dddd" */
    { ULISTFMT_ELEMENT_FIELD,      14, 19 }, /* "Eeeee" */
    { ULISTFMT_LITERAL_FIELD,      1, 2 },   /* "\\u3001" */
    { ULISTFMT_LITERAL_FIELD,      4, 5 },   /* "\\u3001" */
    { ULISTFMT_LITERAL_FIELD,      8, 9 },   /* "\\u3001" */
    { ULISTFMT_LITERAL_FIELD,      13, 14 },   /* "\\u3001" */
    { -1,                          -1, -1 },
};

static const FieldsData expectedFields6[] = {
    { ULISTFMT_ELEMENT_FIELD,      0, 1 },   /* "A" */
    { ULISTFMT_ELEMENT_FIELD,      2, 4 },   /* "Bb" */
    { ULISTFMT_LITERAL_FIELD,      1, 2 },   /* "\\u3001" */
    { -1,                          -1, -1 },
};

static const FieldsData expectedFields7[] = {
    { ULISTFMT_ELEMENT_FIELD,      0, 1 },   /* "A" */
    { ULISTFMT_ELEMENT_FIELD,      2, 4 },   /* "Bb" */
    { ULISTFMT_ELEMENT_FIELD,      5, 8},  /* "Ccc" */
    { ULISTFMT_ELEMENT_FIELD,      9, 13 }, /* "Dddd" */
    { ULISTFMT_ELEMENT_FIELD,      14, 19 }, /* "Eeeee" */
    { ULISTFMT_LITERAL_FIELD,      1, 2 },   /* "\\u3001" */
    { ULISTFMT_LITERAL_FIELD,      4, 5 },   /* "\\u3001" */
    { ULISTFMT_LITERAL_FIELD,      8, 9 },   /* "\\u3001" */
    { ULISTFMT_LITERAL_FIELD,      13, 14 },   /* "\\u3001" */
    { -1,                          -1, -1 },
};

static const FieldsData expectedFields8[] = {
    { ULISTFMT_ELEMENT_FIELD,      0, 1 },   /* "A" */
    { ULISTFMT_ELEMENT_FIELD,      2, 4 },   /* "Bb" */
    { ULISTFMT_LITERAL_FIELD,      1, 2 },   /* "\\u548C" */
    { -1,                          -1, -1 },
};

static ListFmtTestEntry listFmtTestEntries[] = {
    /* locale stringCount expectedResult */
    /*                     0123456789012345678901234567 */
    { "en" ,  5,          "A, Bb, Ccc, Dddd, and Eeeee",                 expectedFields1 },
    { "en" ,  2,          "A and Bb",                                    expectedFields2 },
    { "de" ,  5,          "A, Bb, Ccc, Dddd und Eeeee",                  expectedFields3 },
    { "de" ,  2,          "A und Bb",                                    expectedFields4 },
    /*                     01      234      5678      90123      45678901234567 */
    { "ja" ,  5,          "A\\u3001Bb\\u3001Ccc\\u3001Dddd\\u3001Eeeee", expectedFields5 },
    { "ja" ,  2,          "A\\u3001Bb",                                  expectedFields6 },
    { "zh" ,  5,          "A\\u3001Bb\\u3001Ccc\\u3001Dddd\\u548CEeeee", expectedFields7 },
    { "zh" ,  2,          "A\\u548CBb",                                  expectedFields8 },
    { NULL ,  0,          NULL } /* terminator */
    };

enum {
  kUBufMax = 128,
  kBBufMax = 256
};

static void TestUListFmt() {
    const ListFmtTestEntry * lftep;
    for (lftep = listFmtTestEntries; lftep->locale != NULL ; lftep++ ) {
        UErrorCode status = U_ZERO_ERROR;
        UListFormatter *listfmt = ulistfmt_open(lftep->locale, &status);
        if ( U_FAILURE(status) ) {
            log_data_err("ERROR: ulistfmt_open fails for locale %s, status %s\n", lftep->locale, u_errorName(status));
        } else {
            UChar ubufActual[kUBufMax];
            int32_t ulenActual = ulistfmt_format(listfmt, strings, stringLengths, lftep->stringCount, ubufActual, kUBufMax, &status);
            if ( U_FAILURE(status) ) {
                log_err("ERROR: ulistfmt_format fails for locale %s count %d (real lengths), status %s\n", lftep->locale, lftep->stringCount, u_errorName(status));
            } else {
                UChar ubufExpected[kUBufMax];
                int32_t ulenExpected = u_unescape(lftep->expectedResult, ubufExpected, kUBufMax);
                if (ulenActual != ulenExpected || u_strncmp(ubufActual, ubufExpected, ulenExpected) != 0) {
                    log_err("ERROR: ulistfmt_format for locale %s count %d (real lengths), actual \"%s\" != expected \"%s\"\n", lftep->locale,
                            lftep->stringCount, aescstrdup(ubufActual, ulenActual), aescstrdup(ubufExpected, ulenExpected));
                }
            }
            /* try again with all lengths -1 */
            status = U_ZERO_ERROR;
            ulenActual = ulistfmt_format(listfmt, strings, stringLengthsNeg, lftep->stringCount, ubufActual, kUBufMax, &status);
            if ( U_FAILURE(status) ) {
                log_err("ERROR: ulistfmt_format fails for locale %s count %d (-1 lengths), status %s\n", lftep->locale, lftep->stringCount, u_errorName(status));
            } else {
                UChar ubufExpected[kUBufMax];
                int32_t ulenExpected = u_unescape(lftep->expectedResult, ubufExpected, kUBufMax);
                if (ulenActual != ulenExpected || u_strncmp(ubufActual, ubufExpected, ulenExpected) != 0) {
                    log_err("ERROR: ulistfmt_format for locale %s count %d (-1   lengths), actual \"%s\" != expected \"%s\"\n", lftep->locale,
                            lftep->stringCount, aescstrdup(ubufActual, ulenActual), aescstrdup(ubufExpected, ulenExpected));
                }
            }
            /* try again with NULL lengths */
            status = U_ZERO_ERROR;
            ulenActual = ulistfmt_format(listfmt, strings, NULL, lftep->stringCount, ubufActual, kUBufMax, &status);
            if ( U_FAILURE(status) ) {
                log_err("ERROR: ulistfmt_format fails for locale %s count %d (NULL lengths), status %s\n", lftep->locale, lftep->stringCount, u_errorName(status));
            } else {
                UChar ubufExpected[kUBufMax];
                int32_t ulenExpected = u_unescape(lftep->expectedResult, ubufExpected, kUBufMax);
                if (ulenActual != ulenExpected || u_strncmp(ubufActual, ubufExpected, ulenExpected) != 0) {
                    log_err("ERROR: ulistfmt_format for locale %s count %d (NULL lengths), actual \"%s\" != expected \"%s\"\n", lftep->locale,
                            lftep->stringCount, aescstrdup(ubufActual, ulenActual), aescstrdup(ubufExpected, ulenExpected));
                }
            }
            
            /* try calls that should return error */
            status = U_ZERO_ERROR;
            ulenActual = ulistfmt_format(listfmt, NULL, NULL, lftep->stringCount, ubufActual, kUBufMax, &status);
            if (status != U_ILLEGAL_ARGUMENT_ERROR || ulenActual > 0) {
                log_err("ERROR: ulistfmt_format for locale %s count %d with NULL strings, expected U_ILLEGAL_ARGUMENT_ERROR, got %s, result %d\n", lftep->locale,
                        lftep->stringCount, u_errorName(status), ulenActual);
            }
            status = U_ZERO_ERROR;
            ulenActual = ulistfmt_format(listfmt, strings, NULL, lftep->stringCount, NULL, kUBufMax, &status);
            if (status != U_ILLEGAL_ARGUMENT_ERROR || ulenActual > 0) {
                log_err("ERROR: ulistfmt_format for locale %s count %d with NULL result, expected U_ILLEGAL_ARGUMENT_ERROR, got %s, result %d\n", lftep->locale,
                        lftep->stringCount, u_errorName(status), ulenActual);
            }

            ulistfmt_close(listfmt);
        }
    }
}

static void TestUListFmtForFields() {
    const ListFmtTestEntry * lftep = listFmtTestEntries;
    UErrorCode status = U_ZERO_ERROR;
    for (lftep = listFmtTestEntries; lftep->locale != NULL ; lftep++ ) {
        UListFormatter *listfmt = ulistfmt_open(lftep->locale, &status);
        if ( U_FAILURE(status) ) {
            log_data_err("ERROR: ulistfmt_open fails for locale %s, status %s\n", lftep->locale, u_errorName(status));
            continue;
        }
        UChar ubufActual[kUBufMax];
        UFieldPositionIterator* fpositer = ufieldpositer_open(&status);
        if ( U_FAILURE(status) ) {
            log_err("ufieldpositer_open fails, status %s\n", u_errorName(status));
            continue;
        }
        /* test ulistfmt_formatForFields */
        int32_t ulenActual = ulistfmt_formatForFields(listfmt, strings, stringLengths, lftep->stringCount, ubufActual, kUBufMax, fpositer, &status);
        if ( U_FAILURE(status) ) {
            log_err("ERROR: ulistfmt_formatForFields fails for locale %s count %d (real lengths), status %s\n", lftep->locale, lftep->stringCount, u_errorName(status));
            continue;
        }
        int32_t field, beginPos, endPos;
        const FieldsData * fptr;
        for (fptr = lftep->expectedFields; ; fptr++) {
            field = ufieldpositer_next(fpositer, &beginPos, &endPos);
            if (field != fptr->field || (field >= 0 && (beginPos != fptr->beginPos || endPos != fptr->endPos))) {
                log_err("ulistfmt_formatForFields as \"%s\"; expect field %d range %d-%d, get field %d range %d-%d\n",
                        aescstrdup(ubufActual, ulenActual), fptr->field, fptr->beginPos, fptr->endPos, field, beginPos, endPos);
                break;
            }
            if (field < 0) {
                break;
            }
        }

        field = ufieldpositer_next(fpositer, &beginPos, &endPos);
        UChar ubufExpected[kUBufMax];
        int32_t ulenExpected = u_unescape(lftep->expectedResult, ubufExpected, kUBufMax);
        if (ulenActual != ulenExpected || u_strncmp(ubufActual, ubufExpected, ulenExpected) != 0) {
            log_err("ERROR: ulistfmt_formatForFields for locale %s count %d (real lengths), actual \"%s\" != expected \"%s\"\n", lftep->locale,
                    lftep->stringCount, aescstrdup(ubufActual, ulenActual), aescstrdup(ubufExpected, ulenExpected));
            continue;
        }
        ufieldpositer_close(fpositer);

        /* test ulistfmt_formatForFields again with nullptr as fpositer */
        ulenActual = ulistfmt_formatForFields(listfmt, strings, stringLengths, lftep->stringCount, ubufActual, kUBufMax, NULL, &status);
        if ( U_FAILURE(status) ) {
            log_err("ERROR: ulistfmt_formatForFields fails for locale %s count %d (real lengths), status %s\n", lftep->locale, lftep->stringCount, u_errorName(status));
            continue;
        }
        ulenExpected = u_unescape(lftep->expectedResult, ubufExpected, kUBufMax);
        if (ulenActual != ulenExpected || u_strncmp(ubufActual, ubufExpected, ulenExpected) != 0) {
            log_err("ERROR: ulistfmt_formatForFields for locale %s count %d (real lengths), actual \"%s\" != expected \"%s\"\n", lftep->locale,
                   lftep->stringCount, aescstrdup(ubufActual, ulenActual), aescstrdup(ubufExpected, ulenExpected));
            continue;
        }
        ulistfmt_close(listfmt);
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
