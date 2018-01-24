// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// trie3test.c (modified from trie2test.c)
// created: 2017dec29 Markus W. Scherer

#include <stdio.h>
#include "unicode/utypes.h"
#include "unicode/utf.h"
#include "unicode/utf16.h"
#include "unicode/utf8.h"
#include "utrie.h"
#include "utrie3.h"
#include "utrie3builder.h"
#include "cstring.h"
#include "cmemory.h"
#include "udataswp.h"
#include "cintltst.h"

void addTrie3Test(TestNode** root);

/* Values for setting possibly overlapping, out-of-order ranges of values */
typedef struct SetRange {
    UChar32 start, limit;
    uint32_t value;
    UBool overwrite;
} SetRange;

/*
 * Values for testing:
 * value is set from the previous boundary's limit to before
 * this boundary's limit
 *
 * There must be an entry with limit 0 and the intialValue.
 * It may be preceded by an entry with negative limit and the errorValue.
 */
typedef struct CheckRange {
    UChar32 limit;
    uint32_t value;
} CheckRange;

static int32_t
skipSpecialValues(const CheckRange checkRanges[], int32_t countCheckRanges) {
    int32_t i;
    for(i=0; i<countCheckRanges && checkRanges[i].limit<=0; ++i) {}
    return i;
}

static int32_t
getSpecialValues(const CheckRange checkRanges[], int32_t countCheckRanges,
                 uint32_t *pInitialValue, uint32_t *pErrorValue) {
    int32_t i=0;
    if(i<countCheckRanges && checkRanges[i].limit<0) {
        *pErrorValue=checkRanges[i++].value;
    } else {
        *pErrorValue=0xbad;
    }
    if(i<countCheckRanges && checkRanges[i].limit==0) {
        *pInitialValue=checkRanges[i++].value;
    } else {
        *pInitialValue=0;
    }
    return i;
}

/* utrie3_enum() callback, modifies a value */
static uint32_t U_CALLCONV
testHandleValue(const void *context, uint32_t value) {
    return value ^ 0x5555;
}

static UBool
doCheckRange(const char *name, const char *variant,
             UChar32 start, UChar32 end, uint32_t value,
             UChar32 expEnd, uint32_t expValue) {
    if (end < 0) {
        if (expEnd >= 0) {
            log_err("error: %s getRanges (%s) fails to deliver range [U+%04lx..U+%04lx].0x%lx\n",
                    name, variant, (long)start, (long)expEnd, (long)expValue);
        }
        return FALSE;
    }
    if (expEnd < 0) {
        log_err("error: %s getRanges (%s) delivers unexpected range [U+%04lx..U+%04lx].0x%lx\n",
                name, variant, (long)start, (long)end, (long)value);
        return FALSE;
    }
    if (end != expEnd || value != expValue) {
        log_err("error: %s getRanges (%s) delivers wrong range [U+%04lx..U+%04lx].0x%lx "
                "instead of [U+%04lx..U+%04lx].0x%lx\n",
                name, variant, (long)start, (long)end, (long)value,
                (long)start, (long)expEnd, (long)expValue);
        return FALSE;
    }
    return TRUE;
}

static UChar32 iterStarts[] = { 0, 0xdddd, 0x10000, 0x12345, 0x110000 };

static void
testTrieGetRanges(const char *testName, const UTrie3 *trie,
                  const CheckRange checkRanges[], int32_t countCheckRanges) {
    UBool isFrozen = utrie3bld_isFrozen(trie);
    const char *const typeName = isFrozen ? "frozen trie" : "newTrie";
    char name[80];
    int32_t s;
    for (s = 0; s < UPRV_LENGTHOF(iterStarts); ++s) {
        UChar32 start = iterStarts[s];
        int32_t i, i0;
        UChar32 end, expEnd;
        uint32_t value, expValue;

        sprintf(name, "%s(%s) min=U+%04lx", typeName, testName, (long)start);

        // Skip over special values and low ranges.
        for (i = 0; i < countCheckRanges && checkRanges[i].limit <= start; ++i) {}
        i0 = i;
        // without value handler
        for (;; ++i, start = end + 1) {
            if (i < countCheckRanges) {
                expEnd = checkRanges[i].limit - 1;
                expValue = checkRanges[i].value;
            } else {
                expEnd = -1;
                expValue = value = 0x5005;
            }
            end = isFrozen ? utrie3_getRange(trie, start, NULL, NULL, &value) :
                utrie3bld_getRange(trie, start, NULL, NULL, &value);
            if (!doCheckRange(name, "without value handler", start, end, value, expEnd, expValue)) {
                break;
            }
        }
        // with value handler
        for (i = i0, start = iterStarts[s];; ++i, start = end + 1) {
            if (i < countCheckRanges) {
                expEnd = checkRanges[i].limit - 1;
                expValue = checkRanges[i].value ^ 0x5555;
            } else {
                expEnd = -1;
                expValue = value = 0x5005;
            }
            end = isFrozen ? utrie3_getRange(trie, start, testHandleValue, NULL, &value) :
                utrie3bld_getRange(trie, start, testHandleValue, NULL, &value);
            if (!doCheckRange(name, "with value handler", start, end, value, expEnd, expValue)) {
                break;
            }
        }
        // without value
        for (i = i0, start = iterStarts[s];; ++i, start = end + 1) {
            if (i < countCheckRanges) {
                expEnd = checkRanges[i].limit - 1;
            } else {
                expEnd = -1;
            }
            end = isFrozen ? utrie3_getRange(trie, start, NULL, NULL, NULL) :
                utrie3bld_getRange(trie, start, NULL, NULL, NULL);
            if (!doCheckRange(name, "without value", start, end, 0, expEnd, 0)) {
                break;
            }
        }
    }
}

/* verify all expected values via UTRIE3_GETxx() */
static void
testTrieGetters(const char *testName,
                const UTrie3 *trie, UTrie3ValueBits valueBits,
                const CheckRange checkRanges[], int32_t countCheckRanges) {
    uint32_t initialValue, errorValue;
    uint32_t value, value2;
    UChar32 start, limit;
    int32_t i, countSpecials;
    int32_t countErrors=0;

    UBool isFrozen=utrie3bld_isFrozen(trie);
    const char *const typeName= isFrozen ? "frozen trie" : "newTrie";

    countSpecials=getSpecialValues(checkRanges, countCheckRanges, &initialValue, &errorValue);

    start=0;
    for(i=countSpecials; i<countCheckRanges; ++i) {
        limit=checkRanges[i].limit;
        value=checkRanges[i].value;

        while(start<limit) {
            if(isFrozen) {
                if(start<=0xffff) {
                    if(valueBits==UTRIE3_16_VALUE_BITS) {
                        value2=UTRIE3_GET16_FROM_BMP(trie, start);
                    } else {
                        value2=UTRIE3_GET32_FROM_BMP(trie, start);
                    }
                    if(value!=value2) {
                        log_err("error: %s(%s).fromBMP(U+%04lx)==0x%lx instead of 0x%lx\n",
                                typeName, testName, (long)start, (long)value2, (long)value);
                        ++countErrors;
                    }
                } else {
                    if(valueBits==UTRIE3_16_VALUE_BITS) {
                        UTRIE3_GET16_FROM_SUPP(trie, start, value2);
                    } else {
                        UTRIE3_GET32_FROM_SUPP(trie, start, value2);
                    }
                    if(value!=value2) {
                        log_err("error: %s(%s).fromSupp(U+%04lx)==0x%lx instead of 0x%lx\n",
                                typeName, testName, (long)start, (long)value2, (long)value);
                        ++countErrors;
                    }
                }
                if(valueBits==UTRIE3_16_VALUE_BITS) {
                    UTRIE3_GET16(trie, start, value2);
                } else {
                    UTRIE3_GET32(trie, start, value2);
                }
                if(value!=value2) {
                    log_err("error: %s(%s).get(U+%04lx)==0x%lx instead of 0x%lx\n",
                            typeName, testName, (long)start, (long)value2, (long)value);
                    ++countErrors;
                }
                value2=utrie3_get(trie, start);
                if(value!=value2) {
                    log_err("error: %s(%s).get(U+%04lx)==0x%lx instead of 0x%lx\n",
                            typeName, testName, (long)start, (long)value2, (long)value);
                    ++countErrors;
                }
            } else {
                value2=utrie3bld_get(trie, start);
                if(value!=value2) {
                    log_err("error: %s(%s).get(U+%04lx)==0x%lx instead of 0x%lx\n",
                            typeName, testName, (long)start, (long)value2, (long)value);
                    ++countErrors;
                }
            }
            ++start;
            if(countErrors>10) {
                return;
            }
        }
    }

    if(isFrozen) {
        /* test linear ASCII range from the data array pointer (access to "internal" field) */
        start=0;
        for(i=countSpecials; i<countCheckRanges && start<=0x7f; ++i) {
            limit=checkRanges[i].limit;
            value=checkRanges[i].value;

            while(start<limit && start<=0x7f) {
                if(valueBits==UTRIE3_16_VALUE_BITS) {
                    value2=trie->data16[start];
                } else {
                    value2=trie->data32[start];
                }
                if(value!=value2) {
                    log_err("error: %s(%s).asciiData[U+%04lx]==0x%lx instead of 0x%lx\n",
                            typeName, testName, (long)start, (long)value2, (long)value);
                    ++countErrors;
                }
                ++start;
                if(countErrors>10) {
                    return;
                }
            }
        }
    }

    /* test errorValue */
    if(isFrozen) {
        if(valueBits==UTRIE3_16_VALUE_BITS) {
            UTRIE3_GET16(trie, -1, value);
            UTRIE3_GET16(trie, 0x110000, value2);
        } else {
            UTRIE3_GET32(trie, -1, value);
            UTRIE3_GET32(trie, 0x110000, value2);
        }
        if(value!=errorValue || value2!=errorValue) {
            log_err("error: %s(%s).get(out of range) != errorValue\n",
                    typeName, testName);
        }
        value=utrie3_get(trie, -1);
        value2=utrie3_get(trie, 0x110000);
        if(value!=errorValue || value2!=errorValue) {
            log_err("error: %s(%s).get(out of range) != errorValue\n",
                    typeName, testName);
        }
    } else {
        value=utrie3bld_get(trie, -1);
        value2=utrie3bld_get(trie, 0x110000);
        if(value!=errorValue || value2!=errorValue) {
            log_err("error: %s(%s).get(out of range) != errorValue\n",
                    typeName, testName);
        }
    }
}

#define ACCIDENTAL_SURROGATE_PAIR(s, length, cp) (length > 0 && U16_IS_LEAD(s[length-1]) && U_IS_TRAIL(cp))

static void
testTrieUTF16(const char *testName,
              const UTrie3 *trie, UTrie3ValueBits valueBits,
              const CheckRange checkRanges[], int32_t countCheckRanges) {
    UChar s[2000];
    uint32_t values[1000];

    const UChar *p, *limit;

    uint32_t value, expected;
    UChar32 prevCP, c, c2;
    int32_t i, length, sIndex, countValues;

    /* write a string */
    prevCP=0;
    length=countValues=0;
    for(i=skipSpecialValues(checkRanges, countCheckRanges); i<countCheckRanges; ++i) {
        value=checkRanges[i].value;
        /* write three code points */
        if(!ACCIDENTAL_SURROGATE_PAIR(s, length, prevCP)) {
            U16_APPEND_UNSAFE(s, length, prevCP);   /* start of the range */
            values[countValues++]=value;
        }
        c=checkRanges[i].limit;
        prevCP=(prevCP+c)/2;                    /* middle of the range */
        if(!ACCIDENTAL_SURROGATE_PAIR(s, length, prevCP)) {
            U16_APPEND_UNSAFE(s, length, prevCP);
            values[countValues++]=value;
        }
        prevCP=c;
        --c;                                    /* end of the range */
        if(!ACCIDENTAL_SURROGATE_PAIR(s, length, c)) {
            U16_APPEND_UNSAFE(s, length, c);
            values[countValues++]=value;
        }
    }
    limit=s+length;
    if(length>UPRV_LENGTHOF(s)) {
        log_err("UTF-16 test string length %d > capacity %d\n", (int)length, (int)UPRV_LENGTHOF(s));
        return;
    }
    if(countValues>UPRV_LENGTHOF(values)) {
        log_err("UTF-16 test values length %d > capacity %d\n", (int)countValues, (int)UPRV_LENGTHOF(values));
        return;
    }

    /* try forward */
    p=s;
    i=0;
    while(p<limit) {
        sIndex=(int32_t)(p-s);
        U16_NEXT(s, sIndex, length, c2);
        c=0x33;
        if(valueBits==UTRIE3_16_VALUE_BITS) {
            UTRIE3_U16_NEXT16(trie, p, limit, c, value);
        } else {
            UTRIE3_U16_NEXT32(trie, p, limit, c, value);
        }
        expected= U_IS_SURROGATE(c) ? trie->errorValue : values[i];
        if(value!=expected) {
            log_err("error: wrong value from UTRIE3_NEXT(%s)(U+%04lx): 0x%lx instead of 0x%lx\n",
                    testName, (long)c, (long)value, (long)expected);
        }
        if(c!=c2) {
            log_err("error: wrong code point from UTRIE3_NEXT(%s): U+%04lx != U+%04lx\n",
                    testName, (long)c, (long)c2);
            continue;
        }
        ++i;
    }

    /* try backward */
    p=limit;
    i=countValues;
    while(s<p) {
        --i;
        sIndex=(int32_t)(p-s);
        U16_PREV(s, 0, sIndex, c2);
        c=0x33;
        if(valueBits==UTRIE3_16_VALUE_BITS) {
            UTRIE3_U16_PREV16(trie, s, p, c, value);
        } else {
            UTRIE3_U16_PREV32(trie, s, p, c, value);
        }
        expected= U_IS_SURROGATE(c) ? trie->errorValue : values[i];
        if(value!=expected) {
            log_err("error: wrong value from UTRIE3_PREV(%s)(U+%04lx): 0x%lx instead of 0x%lx\n",
                    testName, (long)c, (long)value, (long)expected);
        }
        if(c!=c2) {
            log_err("error: wrong code point from UTRIE3_PREV(%s): U+%04lx != U+%04lx\n",
                    testName, c, c2);
        }
    }
}

static void
testTrieUTF8(const char *testName,
             const UTrie3 *trie, UTrie3ValueBits valueBits,
             const CheckRange checkRanges[], int32_t countCheckRanges) {
    // Note: The byte sequence comments refer to the original UTF-8 definition.
    // Starting with ICU 60, any sequence that is not a prefix of a valid one
    // is treated as multiple single-byte errors.
    // For testing, we only rely on U8_... and UTrie3 UTF-8 macros
    // iterating consistently.
    static const uint8_t illegal[]={
        0xc0, 0x80,                         /* non-shortest U+0000 */
        0xc1, 0xbf,                         /* non-shortest U+007f */
        0xc2,                               /* truncated */
        0xe0, 0x90, 0x80,                   /* non-shortest U+0400 */
        0xe0, 0xa0,                         /* truncated */
        0xed, 0xa0, 0x80,                   /* lead surrogate U+d800 */
        0xed, 0xbf, 0xbf,                   /* trail surrogate U+dfff */
        0xf0, 0x8f, 0xbf, 0xbf,             /* non-shortest U+ffff */
        0xf0, 0x90, 0x80,                   /* truncated */
        0xf4, 0x90, 0x80, 0x80,             /* beyond-Unicode U+110000 */
        0xf8, 0x80, 0x80, 0x80,             /* truncated */
        0xf8, 0x80, 0x80, 0x80, 0x80,       /* 5-byte UTF-8 */
        0xfd, 0xbf, 0xbf, 0xbf, 0xbf,       /* truncated */
        0xfd, 0xbf, 0xbf, 0xbf, 0xbf, 0xbf, /* 6-byte UTF-8 */
        0xfe,
        0xff
    };
    uint8_t s[6000];
    uint32_t values[2000];

    const uint8_t *p, *limit;

    uint32_t initialValue, errorValue;
    uint32_t value, expectedBytes, actualBytes;
    UChar32 prevCP, c;
    int32_t i, countSpecials, length, countValues;
    int32_t prev8, i8;

    countSpecials=getSpecialValues(checkRanges, countCheckRanges, &initialValue, &errorValue);

    /* write a string */
    prevCP=0;
    length=countValues=0;
    /* first a couple of trail bytes in lead position */
    s[length++]=0x80;
    values[countValues++]=errorValue;
    s[length++]=0xbf;
    values[countValues++]=errorValue;
    prev8=i8=0;
    for(i=countSpecials; i<countCheckRanges; ++i) {
        value=checkRanges[i].value;
        /* write three legal (or surrogate) code points */
        U8_APPEND_UNSAFE(s, length, prevCP);    /* start of the range */
        if(U_IS_SURROGATE(prevCP)) {
            // A surrogate byte sequence counts as 3 single-byte errors.
            values[countValues++]=errorValue;
            values[countValues++]=errorValue;
            values[countValues++]=errorValue;
        } else {
            values[countValues++]=value;
        }
        c=checkRanges[i].limit;
        prevCP=(prevCP+c)/2;                    /* middle of the range */
        U8_APPEND_UNSAFE(s, length, prevCP);
        if(U_IS_SURROGATE(prevCP)) {
            // A surrogate byte sequence counts as 3 single-byte errors.
            values[countValues++]=errorValue;
            values[countValues++]=errorValue;
            values[countValues++]=errorValue;
        } else {
            values[countValues++]=value;
        }
        prevCP=c;
        --c;                                    /* end of the range */
        U8_APPEND_UNSAFE(s, length, c);
        if(U_IS_SURROGATE(c)) {
            // A surrogate byte sequence counts as 3 single-byte errors.
            values[countValues++]=errorValue;
            values[countValues++]=errorValue;
            values[countValues++]=errorValue;
        } else {
            values[countValues++]=value;
        }
        /* write an illegal byte sequence */
        if(i8<sizeof(illegal)) {
            U8_FWD_1(illegal, i8, sizeof(illegal));
            while(prev8<i8) {
                s[length++]=illegal[prev8++];
            }
            values[countValues++]=errorValue;
        }
    }
    /* write the remaining illegal byte sequences */
    while(i8<sizeof(illegal)) {
        U8_FWD_1(illegal, i8, sizeof(illegal));
        while(prev8<i8) {
            s[length++]=illegal[prev8++];
        }
        values[countValues++]=errorValue;
    }
    limit=s+length;
    if(length>UPRV_LENGTHOF(s)) {
        log_err("UTF-8 test string length %d > capacity %d\n", (int)length, (int)UPRV_LENGTHOF(s));
        return;
    }
    if(countValues>UPRV_LENGTHOF(values)) {
        log_err("UTF-8 test values length %d > capacity %d\n", (int)countValues, (int)UPRV_LENGTHOF(values));
        return;
    }

    /* try forward */
    p=s;
    i=0;
    while(p<limit) {
        prev8=i8=(int32_t)(p-s);
        U8_NEXT(s, i8, length, c);
        if(valueBits==UTRIE3_16_VALUE_BITS) {
            UTRIE3_U8_NEXT16(trie, p, limit, value);
        } else {
            UTRIE3_U8_NEXT32(trie, p, limit, value);
        }
        expectedBytes=0;
        if(value!=values[i] || i8!=(p-s)) {
            int32_t k=prev8;
            while(k<i8) {
                expectedBytes=(expectedBytes<<8)|s[k++];
            }
        }
        if(i8==(p-s)) {
            actualBytes=expectedBytes;
        } else {
            actualBytes=0;
            int32_t k=prev8;
            while(k<(p-s)) {
                actualBytes=(actualBytes<<8)|s[k++];
            }
        }
        if(value!=values[i]) {
            log_err("error: wrong value from UTRIE3_U8_NEXT(%s)(from %d %lx->U+%04lx) (read %d bytes): "
                    "0x%lx instead of 0x%lx (from bytes %lx)\n",
                    testName, (int)prev8, (unsigned long)actualBytes, (long)c, (int)((p-s)-prev8),
                    (long)value, (long)values[i], (unsigned long)expectedBytes);
        }
        if(i8!=(p-s)) {
            log_err("error: wrong end index from UTRIE3_U8_NEXT(%s)(from %d %lx->U+%04lx): "
                    "%ld != %ld (bytes %lx)\n",
                    testName, (int)prev8, (unsigned long)actualBytes, (long)c,
                    (long)(p-s), (long)i8, (unsigned long)expectedBytes);
            break;
        }
        ++i;
    }

    /* try backward */
    p=limit;
    i=countValues;
    while(s<p) {
        --i;
        prev8=i8=(int32_t)(p-s);
        U8_PREV(s, 0, i8, c);
        if(valueBits==UTRIE3_16_VALUE_BITS) {
            UTRIE3_U8_PREV16(trie, s, p, value);
        } else {
            UTRIE3_U8_PREV32(trie, s, p, value);
        }
        expectedBytes=0;
        if(value!=values[i] || i8!=(p-s)) {
            int32_t k=i8;
            while(k<prev8) {
                expectedBytes=(expectedBytes<<8)|s[k++];
            }
        }
        if(i8==(p-s)) {
            actualBytes=expectedBytes;
        } else {
            actualBytes=0;
            int32_t k=(int32_t)(p-s);
            while(k<prev8) {
                actualBytes=(actualBytes<<8)|s[k++];
            }
        }
        if(value!=values[i]) {
            log_err("error: wrong value from UTRIE3_U8_PREV(%s)(from %d %lx->U+%04lx) (read %d bytes): "
                    "0x%lx instead of 0x%lx (from bytes %lx)\n",
                    testName, (int)prev8, (unsigned long)actualBytes, (long)c, (int)(prev8-(p-s)),
                    (long)value, (long)values[i], (unsigned long)expectedBytes);
        }
        if(i8!=(p-s)) {
            log_err("error: wrong end index from UTRIE3_U8_PREV(%s)(from %d %lx->U+%04lx): "
                    "%ld != %ld (bytes %lx)\n",
                    testName, (int)prev8, (unsigned long)actualBytes, (long)c,
                    (long)(p-s), (long)i8, (unsigned long)expectedBytes);
            break;
        }
    }
}

static void
testFrozenTrie(const char *testName,
               UTrie3 *trie, UTrie3ValueBits valueBits,
               const CheckRange checkRanges[], int32_t countCheckRanges) {
    UErrorCode errorCode;
    uint32_t value, value2;

    if(!utrie3bld_isFrozen(trie)) {
        log_err("error: utrie3bld_isFrozen(frozen %s) returned FALSE (not frozen)\n",
                testName);
        return;
    }

    testTrieGetters(testName, trie, valueBits, checkRanges, countCheckRanges);
    testTrieGetRanges(testName, trie, checkRanges, countCheckRanges);
    testTrieUTF16(testName, trie, valueBits, checkRanges, countCheckRanges);
    testTrieUTF8(testName, trie, valueBits, checkRanges, countCheckRanges);

    errorCode=U_ZERO_ERROR;
    value=utrie3_get(trie, 1);
    utrie3bld_set(trie, 1, 234, &errorCode);
    value2=utrie3_get(trie, 1);
    if(errorCode!=U_NO_WRITE_PERMISSION || value2!=value) {
        log_err("error: utrie3bld_set(frozen %s) failed: it set %s != U_NO_WRITE_PERMISSION\n",
                testName, u_errorName(errorCode));
        return;
    }

    errorCode=U_ZERO_ERROR;
    utrie3bld_setRange(trie, 1, 5, 234, TRUE, &errorCode);
    value2=utrie3_get(trie, 1);
    if(errorCode!=U_NO_WRITE_PERMISSION || value2!=value) {
        log_err("error: utrie3bld_setRange(frozen %s) failed: it set %s != U_NO_WRITE_PERMISSION\n",
                testName, u_errorName(errorCode));
        return;
    }
}

static void
testNewTrie(const char *testName, const UTrie3 *trie,
            const CheckRange checkRanges[], int32_t countCheckRanges) {
    /* The valueBits are ignored for an unfrozen trie. */
    testTrieGetters(testName, trie, UTRIE3_COUNT_VALUE_BITS, checkRanges, countCheckRanges);
    testTrieGetRanges(testName, trie, checkRanges, countCheckRanges);
}

static uint32_t storage[120000];

static void
testTrieSerialize(const char *testName,
                  UTrie3 *trie, UTrie3ValueBits valueBits,
                  UBool withSwap,
                  const CheckRange checkRanges[], int32_t countCheckRanges) {
    int32_t length1, length2, length3;
    UTrie3ValueBits otherValueBits;
    UErrorCode errorCode;

    /* clone the trie so that the caller can reuse the original */
    errorCode=U_ZERO_ERROR;
    trie=utrie3bld_clone(trie, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("error: utrie3bld_clone(unfrozen %s) failed - %s\n",
                testName, u_errorName(errorCode));
        return;
    }

    /*
     * This is not a loop, but simply a block that we can exit with "break"
     * when something goes wrong.
     */
    do {
        errorCode=U_ZERO_ERROR;
        utrie3_serialize(trie, storage, sizeof(storage), &errorCode);
        if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("error: utrie3_serialize(unfrozen %s) set %s != U_ILLEGAL_ARGUMENT_ERROR\n",
                    testName, u_errorName(errorCode));
            break;
        }
        errorCode=U_ZERO_ERROR;
        utrie3bld_freeze(trie, valueBits, &errorCode);
        if(U_FAILURE(errorCode) || !utrie3bld_isFrozen(trie)) {
            log_err("error: utrie3bld_freeze(%s) failed: %s isFrozen: %d\n",
                    testName, u_errorName(errorCode), utrie3bld_isFrozen(trie));
            break;
        }
        otherValueBits= valueBits==UTRIE3_16_VALUE_BITS ? UTRIE3_32_VALUE_BITS : UTRIE3_16_VALUE_BITS;
        utrie3bld_freeze(trie, otherValueBits, &errorCode);
        if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("error: utrie3bld_freeze(already-frozen with other valueBits %s) "
                    "set %s != U_ILLEGAL_ARGUMENT_ERROR\n",
                    testName, u_errorName(errorCode));
            break;
        }
        errorCode=U_ZERO_ERROR;
        if(withSwap) {
            /* clone a frozen trie */
            UTrie3 *clone=utrie3_clone(trie, &errorCode);
            if(U_FAILURE(errorCode)) {
                log_err("error: cloning a frozen UTrie3 failed (%s) - %s\n",
                        testName, u_errorName(errorCode));
                errorCode=U_ZERO_ERROR;  /* continue with the original */
            } else {
                utrie3_close(trie);
                trie=clone;
            }
        }
        length1=utrie3_serialize(trie, NULL, 0, &errorCode);
        if(errorCode!=U_BUFFER_OVERFLOW_ERROR) {
            log_err("error: utrie3_serialize(%s) preflighting set %s != U_BUFFER_OVERFLOW_ERROR\n",
                    testName, u_errorName(errorCode));
            break;
        }
        errorCode=U_ZERO_ERROR;
        length2=utrie3_serialize(trie, storage, sizeof(storage), &errorCode);
        if(errorCode==U_BUFFER_OVERFLOW_ERROR) {
            log_err("error: utrie3_serialize(%s) needs more memory\n", testName);
            break;
        }
        if(U_FAILURE(errorCode)) {
            log_err("error: utrie3_serialize(%s) failed: %s\n", testName, u_errorName(errorCode));
            break;
        }
        if(length1!=length2) {
            log_err("error: trie serialization (%s) lengths different: "
                    "preflight vs. serialize\n", testName);
            break;
        }

        testFrozenTrie(testName, trie, valueBits, checkRanges, countCheckRanges);
        utrie3_close(trie);
        trie=NULL;

        if(withSwap) {
            uint32_t swapped[10000];
            int32_t swappedLength;

            UDataSwapper *ds;

            /* swap to opposite-endian */
            uprv_memset(swapped, 0x55, length2);
            ds=udata_openSwapper(U_IS_BIG_ENDIAN, U_CHARSET_FAMILY,
                                 !U_IS_BIG_ENDIAN, U_CHARSET_FAMILY, &errorCode);
            swappedLength=utrie3_swap(ds, storage, -1, NULL, &errorCode);
            if(U_FAILURE(errorCode) || swappedLength!=length2) {
                log_err("error: utrie3_swap(%s to OE preflighting) failed (%s) "
                        "or before/after lengths different\n",
                        testName, u_errorName(errorCode));
                udata_closeSwapper(ds);
                break;
            }
            swappedLength=utrie3_swap(ds, storage, length2, swapped, &errorCode);
            udata_closeSwapper(ds);
            if(U_FAILURE(errorCode) || swappedLength!=length2) {
                log_err("error: utrie3_swap(%s to OE) failed (%s) or before/after lengths different\n",
                        testName, u_errorName(errorCode));
                break;
            }

            /* swap back to platform-endian */
            uprv_memset(storage, 0xaa, length2);
            ds=udata_openSwapper(!U_IS_BIG_ENDIAN, U_CHARSET_FAMILY,
                                 U_IS_BIG_ENDIAN, U_CHARSET_FAMILY, &errorCode);
            swappedLength=utrie3_swap(ds, swapped, -1, NULL, &errorCode);
            if(U_FAILURE(errorCode) || swappedLength!=length2) {
                log_err("error: utrie3_swap(%s to PE preflighting) failed (%s) "
                        "or before/after lengths different\n",
                        testName, u_errorName(errorCode));
                udata_closeSwapper(ds);
                break;
            }
            swappedLength=utrie3_swap(ds, swapped, length2, storage, &errorCode);
            udata_closeSwapper(ds);
            if(U_FAILURE(errorCode) || swappedLength!=length2) {
                log_err("error: utrie3_swap(%s to PE) failed (%s) or before/after lengths different\n",
                        testName, u_errorName(errorCode));
                break;
            }
        }

        trie=utrie3_openFromSerialized(valueBits, storage, length2, &length3, &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("error: utrie3_openFromSerialized(%s) failed, %s\n", testName, u_errorName(errorCode));
            break;
        }
        if((valueBits==UTRIE3_16_VALUE_BITS)!=(trie->data32==NULL)) {
            log_err("error: trie serialization (%s) did not preserve 32-bitness\n", testName);
            break;
        }
        if(length2!=length3) {
            log_err("error: trie serialization (%s) lengths different: "
                    "serialize vs. unserialize\n", testName);
            break;
        }
        /* overwrite the storage that is not supposed to be needed */
        uprv_memset((char *)storage+length3, 0xfa, (int32_t)(sizeof(storage)-length3));

        utrie3bld_freeze(trie, valueBits, &errorCode);
        if(U_FAILURE(errorCode) || !utrie3bld_isFrozen(trie)) {
            log_err("error: utrie3bld_freeze(unserialized %s) failed: %s isFrozen: %d\n",
                    testName, u_errorName(errorCode), utrie3bld_isFrozen(trie));
            break;
        }
        utrie3bld_freeze(trie, otherValueBits, &errorCode);
        if(errorCode!=U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("error: utrie3bld_freeze(unserialized with other valueBits %s) "
                    "set %s != U_ILLEGAL_ARGUMENT_ERROR\n",
                    testName, u_errorName(errorCode));
            break;
        }
        errorCode=U_ZERO_ERROR;
        if(withSwap) {
            /* clone a deserialized trie */
            UTrie3 *clone=utrie3_clone(trie, &errorCode);
            if(U_FAILURE(errorCode)) {
                log_err("error: utrie3_clone(unserialized %s) failed - %s\n",
                        testName, u_errorName(errorCode));
                errorCode=U_ZERO_ERROR;
                /* no need to break: just test the original trie */
            } else {
                utrie3_close(trie);
                trie=clone;
                uprv_memset(storage, 0, sizeof(storage));
            }
        }
        testFrozenTrie(testName, trie, valueBits, checkRanges, countCheckRanges);
#if 0  // TODO
        {
            /* clone-as-thawed an unserialized trie */
            UTrie3 *clone=utrie3bld_cloneAsThawed(trie, &errorCode);
            if(U_FAILURE(errorCode) || utrie3bld_isFrozen(clone)) {
                log_err("error: utrie3bld_cloneAsThawed(unserialized %s) failed - "
                        "%s (isFrozen: %d)\n",
                        testName, u_errorName(errorCode), clone!=NULL && utrie3bld_isFrozen(trie));
                break;
            } else {
                utrie3_close(trie);
                trie=clone;
            }
        }
        {
            uint32_t value, value2;

            value=utrie3bld_get(trie, 0xa1);
            utrie3bld_set(trie, 0xa1, 789, &errorCode);
            value2=utrie3bld_get(trie, 0xa1);
            utrie3bld_set(trie, 0xa1, value, &errorCode);
            if(U_FAILURE(errorCode) || value2!=789) {
                log_err("error: modifying a cloneAsThawed UTrie3 (%s) failed - %s\n",
                        testName, u_errorName(errorCode));
            }
        }
        testNewTrie(testName, trie, checkRanges, countCheckRanges);
#endif
    } while(0);

    utrie3_close(trie);
}

static UTrie3 *
testTrieSerializeAllValueBits(const char *testName,
                              UTrie3 *trie, UBool withClone,
                              const CheckRange checkRanges[], int32_t countCheckRanges) {
    char name[40];

    /* verify that all the expected values are in the unfrozen trie */
    testNewTrie(testName, trie, checkRanges, countCheckRanges);

    /*
     * Test with both valueBits serializations,
     * and that utrie3_serialize() can be called multiple times.
     */
    uprv_strcpy(name, testName);
    uprv_strcat(name, ".16");
    testTrieSerialize(name, trie,
                      UTRIE3_16_VALUE_BITS, withClone,
                      checkRanges, countCheckRanges);

    if(withClone) {
#if 0  // TODO
        /*
         * try cloning after the first serialization;
         * clone-as-thawed just to sometimes try it on an unfrozen trie
         */
        UErrorCode errorCode=U_ZERO_ERROR;
        UTrie3 *clone=utrie3bld_cloneAsThawed(trie, &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("error: utrie3bld_cloneAsThawed(%s) after serialization failed - %s\n",
                    testName, u_errorName(errorCode));
        } else {
            utrie3_close(trie);
            trie=clone;

            testNewTrie(testName, trie, checkRanges, countCheckRanges);
        }
#endif
    }

    uprv_strcpy(name, testName);
    uprv_strcat(name, ".32");
    testTrieSerialize(name, trie,
                      UTRIE3_32_VALUE_BITS, withClone,
                      checkRanges, countCheckRanges);

    return trie; /* could be the clone */
}

static UTrie3 *
makeTrieWithRanges(const char *testName, UBool withClone,
                   const SetRange setRanges[], int32_t countSetRanges,
                   const CheckRange checkRanges[], int32_t countCheckRanges) {
    UTrie3 *trie;
    uint32_t initialValue, errorValue;
    uint32_t value;
    UChar32 start, limit;
    int32_t i;
    UErrorCode errorCode;
    UBool overwrite;

    log_verbose("\ntesting Trie '%s'\n", testName);
    errorCode=U_ZERO_ERROR;
    getSpecialValues(checkRanges, countCheckRanges, &initialValue, &errorValue);
    trie=utrie3bld_open(initialValue, errorValue, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("error: utrie3bld_open(%s) failed: %s\n", testName, u_errorName(errorCode));
        return NULL;
    }

    /* set values from setRanges[] */
    for(i=0; i<countSetRanges; ++i) {
        if(withClone && i==countSetRanges/2) {
            /* switch to a clone in the middle of setting values */
            UTrie3 *clone=utrie3bld_clone(trie, &errorCode);
            if(U_FAILURE(errorCode)) {
                log_err("error: utrie3bld_clone(%s) failed - %s\n",
                        testName, u_errorName(errorCode));
                errorCode=U_ZERO_ERROR;  /* continue with the original */
            } else {
                utrie3bld_close(trie);
                trie=clone;
            }
        }
        start=setRanges[i].start;
        limit=setRanges[i].limit;
        value=setRanges[i].value;
        overwrite=setRanges[i].overwrite;
        if((limit-start)==1 && overwrite) {
            utrie3bld_set(trie, start, value, &errorCode);
        } else {
            utrie3bld_setRange(trie, start, limit-1, value, overwrite, &errorCode);
        }
    }

    if(U_SUCCESS(errorCode)) {
        return trie;
    } else {
        log_err("error: setting values into a trie (%s) failed - %s\n",
                testName, u_errorName(errorCode));
        utrie3bld_close(trie);
        return NULL;
    }
}

static void
testTrieRanges(const char *testName, UBool withClone,
               const SetRange setRanges[], int32_t countSetRanges,
               const CheckRange checkRanges[], int32_t countCheckRanges) {
    UTrie3 *trie=makeTrieWithRanges(testName, withClone,
                                    setRanges, countSetRanges,
                                    checkRanges, countCheckRanges);
    if(trie!=NULL) {
        trie=testTrieSerializeAllValueBits(testName, trie, withClone,
                                           checkRanges, countCheckRanges);
        utrie3bld_close(trie);
    }
}

/* test data ----------------------------------------------------------------*/

/* set consecutive ranges, even with value 0 */
static const SetRange
setRanges1[]={
    { 0,        0x40,     0,      FALSE },
    { 0x40,     0xe7,     0x1234, FALSE },
    { 0xe7,     0x3400,   0,      FALSE },
    { 0x3400,   0x9fa6,   0x6162, FALSE },
    { 0x9fa6,   0xda9e,   0x3132, FALSE },
    { 0xdada,   0xeeee,   0x87ff, FALSE },
    { 0xeeee,   0x11111,  1,      FALSE },
    { 0x11111,  0x44444,  0x6162, FALSE },
    { 0x44444,  0x60003,  0,      FALSE },
    { 0xf0003,  0xf0004,  0xf,    FALSE },
    { 0xf0004,  0xf0006,  0x10,   FALSE },
    { 0xf0006,  0xf0007,  0x11,   FALSE },
    { 0xf0007,  0xf0040,  0x12,   FALSE },
    { 0xf0040,  0x110000, 0,      FALSE }
};

static const CheckRange
checkRanges1[]={
    { 0,        0 },
    { 0x40,     0 },
    { 0xe7,     0x1234 },
    { 0x3400,   0 },
    { 0x9fa6,   0x6162 },
    { 0xda9e,   0x3132 },
    { 0xdada,   0 },
    { 0xeeee,   0x87ff },
    { 0x11111,  1 },
    { 0x44444,  0x6162 },
    { 0xf0003,  0 },
    { 0xf0004,  0xf },
    { 0xf0006,  0x10 },
    { 0xf0007,  0x11 },
    { 0xf0040,  0x12 },
    { 0x110000, 0 }
};

/* set some interesting overlapping ranges */
static const SetRange
setRanges2[]={
    { 0x21,     0x7f,     0x5555, TRUE },
    { 0x2f800,  0x2fedc,  0x7a,   TRUE },
    { 0x72,     0xdd,     3,      TRUE },
    { 0xdd,     0xde,     4,      FALSE },
    { 0x201,    0x240,    6,      TRUE },  /* 3 consecutive blocks with the same pattern but */
    { 0x241,    0x280,    6,      TRUE },  /* discontiguous value ranges, testing utrie3_enum() */
    { 0x281,    0x2c0,    6,      TRUE },
    { 0x2f987,  0x2fa98,  5,      TRUE },
    { 0x2f777,  0x2f883,  0,      TRUE },
    { 0x2f900,  0x2ffaa,  1,      FALSE },
    { 0x2ffaa,  0x2ffab,  2,      TRUE },
    { 0x2ffbb,  0x2ffc0,  7,      TRUE }
};

static const CheckRange
checkRanges2[]={
    { 0,        0 },
    { 0x21,     0 },
    { 0x72,     0x5555 },
    { 0xdd,     3 },
    { 0xde,     4 },
    { 0x201,    0 },
    { 0x240,    6 },
    { 0x241,    0 },
    { 0x280,    6 },
    { 0x281,    0 },
    { 0x2c0,    6 },
    { 0x2f883,  0 },
    { 0x2f987,  0x7a },
    { 0x2fa98,  5 },
    { 0x2fedc,  0x7a },
    { 0x2ffaa,  1 },
    { 0x2ffab,  2 },
    { 0x2ffbb,  0 },
    { 0x2ffc0,  7 },
    { 0x110000, 0 }
};

/* use a non-zero initial value */
static const SetRange
setRanges3[]={
    { 0x31,     0xa4,     1, FALSE },
    { 0x3400,   0x6789,   2, FALSE },
    { 0x8000,   0x89ab,   9, TRUE },
    { 0x9000,   0xa000,   4, TRUE },
    { 0xabcd,   0xbcde,   3, TRUE },
    { 0x55555,  0x110000, 6, TRUE },  /* highStart<U+ffff with non-initialValue */
    { 0xcccc,   0x55555,  6, TRUE }
};

static const CheckRange
checkRanges3[]={
    { 0,        9 },  /* non-zero initialValue */
    { 0x31,     9 },
    { 0xa4,     1 },
    { 0x3400,   9 },
    { 0x6789,   2 },
    { 0x9000,   9 },
    { 0xa000,   4 },
    { 0xabcd,   9 },
    { 0xbcde,   3 },
    { 0xcccc,   9 },
    { 0x110000, 6 }
};

/* empty or single-value tries, testing highStart==0 */
static const SetRange
setRangesEmpty[]={
    { 0,        0,        0, FALSE },  /* need some values for it to compile */
};

static const CheckRange
checkRangesEmpty[]={
    { 0,        3 },
    { 0x110000, 3 }
};

static const SetRange
setRangesSingleValue[]={
    { 0,        0x110000, 5, TRUE },
};

static const CheckRange
checkRangesSingleValue[]={
    { 0,        3 },
    { 0x110000, 5 }
};

static void
TrieTestSet1(void) {
    testTrieRanges("set1", FALSE,
        setRanges1, UPRV_LENGTHOF(setRanges1),
        checkRanges1, UPRV_LENGTHOF(checkRanges1));
}

static void
TrieTestSet2Overlap(void) {
    testTrieRanges("set2-overlap", FALSE,
        setRanges2, UPRV_LENGTHOF(setRanges2),
        checkRanges2, UPRV_LENGTHOF(checkRanges2));
}

static void
TrieTestSet3Initial9(void) {
    testTrieRanges("set3-initial-9", FALSE,
        setRanges3, UPRV_LENGTHOF(setRanges3),
        checkRanges3, UPRV_LENGTHOF(checkRanges3));
}

static void
TrieTestSetEmpty(void) {
    testTrieRanges("set-empty", FALSE,
        setRangesEmpty, 0,
        checkRangesEmpty, UPRV_LENGTHOF(checkRangesEmpty));
}

static void
TrieTestSetSingleValue(void) {
    testTrieRanges("set-single-value", FALSE,
        setRangesSingleValue, UPRV_LENGTHOF(setRangesSingleValue),
        checkRangesSingleValue, UPRV_LENGTHOF(checkRangesSingleValue));
}

static void
TrieTestSet2OverlapWithClone(void) {
    testTrieRanges("set2-overlap.withClone", TRUE,
        setRanges2, UPRV_LENGTHOF(setRanges2),
        checkRanges2, UPRV_LENGTHOF(checkRanges2));
}

/* test utrie3_openDummy() -------------------------------------------------- */

static void
dummyTest(UTrie3ValueBits valueBits) {
    CheckRange
    checkRanges[]={
        { -1,       0 },
        { 0,        0 },
        { 0x110000, 0 }
    };

    UTrie3 *trie;
    UErrorCode errorCode;

    const char *testName;
    uint32_t initialValue, errorValue;

    if(valueBits==UTRIE3_16_VALUE_BITS) {
        testName="dummy.16";
        initialValue=0x313;
        errorValue=0xaffe;
    } else {
        testName="dummy.32";
        initialValue=0x01234567;
        errorValue=0x89abcdef;
    }
    checkRanges[0].value=errorValue;
    checkRanges[1].value=checkRanges[2].value=initialValue;

    errorCode=U_ZERO_ERROR;
    trie=utrie3_openDummy(valueBits, initialValue, errorValue, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("utrie3_openDummy(valueBits=%d) failed - %s\n", valueBits, u_errorName(errorCode));
        return;
    }

    testFrozenTrie(testName, trie, valueBits, checkRanges, UPRV_LENGTHOF(checkRanges));
    utrie3_close(trie);
}

static void
DummyTrieTest(void) {
    dummyTest(UTRIE3_16_VALUE_BITS);
    dummyTest(UTRIE3_32_VALUE_BITS);
}

/* test builder memory management ------------------------------------------- */

static void
FreeBlocksTest(void) {
    static const CheckRange
    checkRanges[]={
        { 0,        1 },
        { 0x740,    1 },
        { 0x780,    2 },
        { 0x880,    3 },
        { 0x110000, 1 }
    };
    static const char *const testName="free-blocks";

    UTrie3 *trie;
    int32_t i;
    UErrorCode errorCode;

    errorCode=U_ZERO_ERROR;
    trie=utrie3bld_open(1, 0xbad, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("error: utrie3bld_open(%s) failed: %s\n", testName, u_errorName(errorCode));
        return;
    }

    /*
     * Repeatedly set overlapping same-value ranges to stress the free-data-block management.
     * If it fails, it will overflow the data array.
     */
    for(i=0; i<(0x120000>>UTRIE3_SHIFT_2)/2; ++i) {
        utrie3bld_setRange(trie, 0x740, 0x840-1, 1, TRUE, &errorCode);
        utrie3bld_setRange(trie, 0x780, 0x880-1, 1, TRUE, &errorCode);
        utrie3bld_setRange(trie, 0x740, 0x840-1, 2, TRUE, &errorCode);
        utrie3bld_setRange(trie, 0x780, 0x880-1, 3, TRUE, &errorCode);
    }
    /* make blocks that will be free during compaction */
    utrie3bld_setRange(trie, 0x1000, 0x3000-1, 2, TRUE, &errorCode);
    utrie3bld_setRange(trie, 0x2000, 0x4000-1, 3, TRUE, &errorCode);
    utrie3bld_setRange(trie, 0x1000, 0x4000-1, 1, TRUE, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("error: setting lots of ranges into a trie (%s) failed - %s\n",
                testName, u_errorName(errorCode));
        utrie3bld_close(trie);
        return;
    }

    trie=testTrieSerializeAllValueBits(testName, trie, FALSE,
                                       checkRanges, UPRV_LENGTHOF(checkRanges));
    utrie3bld_close(trie);
}

static void
GrowDataArrayTest(void) {
    static const CheckRange
    checkRanges[]={
        { 0,        1 },
        { 0x720,    2 },
        { 0x7a0,    3 },
        { 0x8a0,    4 },
        { 0x110000, 5 }
    };
    static const char *const testName="grow-data";

    UTrie3 *trie;
    int32_t i;
    UErrorCode errorCode;

    errorCode=U_ZERO_ERROR;
    trie=utrie3bld_open(1, 0xbad, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("error: utrie3bld_open(%s) failed: %s\n", testName, u_errorName(errorCode));
        return;
    }

    /*
     * Use utrie3bld_set() not utrie3bld_setRange() to write non-initialValue-data.
     * Should grow/reallocate the data array to a sufficient length.
     */
    for(i=0; i<0x1000; ++i) {
        utrie3bld_set(trie, i, 2, &errorCode);
    }
    for(i=0x720; i<0x1100; ++i) { /* some overlap */
        utrie3bld_set(trie, i, 3, &errorCode);
    }
    for(i=0x7a0; i<0x900; ++i) {
        utrie3bld_set(trie, i, 4, &errorCode);
    }
    for(i=0x8a0; i<0x110000; ++i) {
        utrie3bld_set(trie, i, 5, &errorCode);
    }
    if(U_FAILURE(errorCode)) {
        log_err("error: setting lots of values into a trie (%s) failed - %s\n",
                testName, u_errorName(errorCode));
        utrie3bld_close(trie);
        return;
    }

    trie=testTrieSerializeAllValueBits(testName, trie, FALSE,
                                          checkRanges, UPRV_LENGTHOF(checkRanges));
    utrie3bld_close(trie);
}

static void
ManyAllSameBlocksTest(void) {
    static const char *const testName="many-all-same";

    UTrie3 *trie;
    int32_t i;
    UErrorCode errorCode;
    CheckRange checkRanges[(0x110000 >> 12) + 1];

    errorCode = U_ZERO_ERROR;
    trie = utrie3bld_open(0xff33, 0xbad, &errorCode);
    if (U_FAILURE(errorCode)) {
        log_err("error: utrie3bld_open(%s) failed: %s\n", testName, u_errorName(errorCode));
        return;
    }
    checkRanges[0].limit = 0;
    checkRanges[0].value = 0xff33;  // initialValue

    // Many all-same-value blocks.
    for (i = 0; i < 0x110000; i += 0x1000) {
        uint32_t value = i >> 12;
        utrie3bld_setRange(trie, i, i + 0xfff, value, TRUE, &errorCode);
        checkRanges[value + 1].limit = i + 0x1000;
        checkRanges[value + 1].value = value;
    }
    for (i = 0; i < 0x110000; i += 0x1000) {
        uint32_t expected = i >> 12;
        uint32_t v0 = utrie3bld_get(trie, i);
        uint32_t vfff = utrie3bld_get(trie, i + 0xfff);
        if (v0 != expected || vfff != expected) {
            log_err("error: UTrie3 builder U+%04lx unexpected value\n", (long)i);
        }
    }

    trie=testTrieSerializeAllValueBits(testName, trie, FALSE,
                                          checkRanges, UPRV_LENGTHOF(checkRanges));
    utrie3bld_close(trie);
}

/* versions 1 and 2 --------------------------------------------------------- */

static void
GetVersionTest(void) {
    uint32_t data[4];
    // version 1
    if( (data[0]=0x54726965, 1!=utrie3_getVersion(data, sizeof(data), FALSE)) ||
        (data[0]=0x54726965, 1!=utrie3_getVersion(data, sizeof(data), TRUE)) ||
        (data[0]=0x65697254, 0!=utrie3_getVersion(data, sizeof(data), FALSE)) ||
        (data[0]=0x65697254, 1!=utrie3_getVersion(data, sizeof(data), TRUE))) {
        log_err("error: utrie3_getVersion(v1) is not working as expected\n");
    }

    // version 2
    if( (data[0]=0x54726932, 2!=utrie3_getVersion(data, sizeof(data), FALSE)) ||
        (data[0]=0x54726932, 2!=utrie3_getVersion(data, sizeof(data), TRUE)) ||
        (data[0]=0x32697254, 0!=utrie3_getVersion(data, sizeof(data), FALSE)) ||
        (data[0]=0x32697254, 2!=utrie3_getVersion(data, sizeof(data), TRUE))) {
        log_err("error: utrie3_getVersion(v2) is not working as expected\n");
    }

    // version 3
    if( (data[0]=0x54726933, 3!=utrie3_getVersion(data, sizeof(data), FALSE)) ||
        (data[0]=0x54726933, 3!=utrie3_getVersion(data, sizeof(data), TRUE)) ||
        (data[0]=0x33697254, 0!=utrie3_getVersion(data, sizeof(data), FALSE)) ||
        (data[0]=0x33697254, 3!=utrie3_getVersion(data, sizeof(data), TRUE))) {
        log_err("error: utrie3_getVersion(v3) is not working as expected\n");
    }

    // illegal arguments
    if( (data[0]=0x54726932, 0!=utrie3_getVersion(NULL, sizeof(data), FALSE)) ||
        (data[0]=0x54726932, 0!=utrie3_getVersion(data, 3, FALSE)) ||
        (data[0]=0x54726932, 0!=utrie3_getVersion((char *)data+1, sizeof(data), FALSE))) {
        log_err("error: utrie3_getVersion(illegal) is not working as expected\n");
    }

    // unknown signature values
    if( (data[0]=0x11223344, 0!=utrie3_getVersion(data, sizeof(data), FALSE)) ||
        (data[0]=0x54726934, 0!=utrie3_getVersion(data, sizeof(data), FALSE))) {
        log_err("error: utrie3_getVersion(unknown) is not working as expected\n");
    }
}

#if 0  // TODO
static UNewTrie *
makeNewTrie1WithRanges(const char *testName,
                       const SetRange setRanges[], int32_t countSetRanges,
                       const CheckRange checkRanges[], int32_t countCheckRanges) {
    UNewTrie *newTrie;
    uint32_t initialValue, errorValue;
    uint32_t value;
    UChar32 start, limit;
    int32_t i;
    UErrorCode errorCode;
    UBool overwrite, ok;

    log_verbose("\ntesting Trie '%s'\n", testName);
    errorCode=U_ZERO_ERROR;
    getSpecialValues(checkRanges, countCheckRanges, &initialValue, &errorValue);
    newTrie=utrie_open(NULL, NULL, 2000,
                       initialValue, initialValue,
                       FALSE);
    if(U_FAILURE(errorCode)) {
        log_err("error: utrie_open(%s) failed: %s\n", testName, u_errorName(errorCode));
        return NULL;
    }

    /* set values from setRanges[] */
    ok=TRUE;
    for(i=0; i<countSetRanges; ++i) {
        start=setRanges[i].start;
        limit=setRanges[i].limit;
        value=setRanges[i].value;
        overwrite=setRanges[i].overwrite;
        if((limit-start)==1 && overwrite) {
            ok&=utrie_set32(newTrie, start, value);
        } else {
            ok&=utrie_setRange32(newTrie, start, limit, value, overwrite);
        }
    }
    if(ok) {
        return newTrie;
    } else {
        log_err("error: setting values into a trie1 (%s) failed\n", testName);
        utrie_close(newTrie);
        return NULL;
    }
}
#endif

static void
testTrie2FromTrie1(const char *testName,
                   const SetRange setRanges[], int32_t countSetRanges,
                   const CheckRange checkRanges[], int32_t countCheckRanges) {
#if 0  // TODO
    uint32_t memory1_16[3000], memory1_32[3000];
    int32_t length16, length32;
    UChar lead;

    char name[40];

    UNewTrie *newTrie1_16, *newTrie1_32;
    UTrie trie1_16, trie1_32;
    UTrie3 *trie2;
    uint32_t initialValue, errorValue;
    UErrorCode errorCode;

    newTrie1_16=makeNewTrie1WithRanges(testName,
                                       setRanges, countSetRanges,
                                       checkRanges, countCheckRanges);
    if(newTrie1_16==NULL) {
        return;
    }
    newTrie1_32=utrie_clone(NULL, newTrie1_16, NULL, 0);
    if(newTrie1_32==NULL) {
        utrie_close(newTrie1_16);
        return;
    }
    errorCode=U_ZERO_ERROR;
    length16=utrie_serialize(newTrie1_16, memory1_16, sizeof(memory1_16),
                             NULL, TRUE, &errorCode);
    length32=utrie_serialize(newTrie1_32, memory1_32, sizeof(memory1_32),
                             NULL, FALSE, &errorCode);
    utrie_unserialize(&trie1_16, memory1_16, length16, &errorCode);
    utrie_unserialize(&trie1_32, memory1_32, length32, &errorCode);
    utrie_close(newTrie1_16);
    utrie_close(newTrie1_32);
    if(U_FAILURE(errorCode)) {
        log_err("error: utrie_serialize or unserialize(%s) failed: %s\n",
                testName, u_errorName(errorCode));
        return;
    }

    getSpecialValues(checkRanges, countCheckRanges, &initialValue, &errorValue);

    uprv_strcpy(name, testName);
    uprv_strcat(name, ".16");
    trie2=utrie3_fromUTrie(&trie1_16, errorValue, &errorCode);
    if(U_SUCCESS(errorCode)) {
        testFrozenTrie(name, trie2, UTRIE3_16_VALUE_BITS, checkRanges, countCheckRanges);
        for(lead=0xd800; lead<0xdc00; ++lead) {
            uint32_t value1, value2;
            value1=UTRIE_GET16_FROM_LEAD(&trie1_16, lead);
            value2=UTRIE3_GET16_FROM_BMP(trie2, lead);
            if(value1!=value2) {
                log_err("error: utrie3_fromUTrie(%s) wrong value %ld!=%ld "
                        "from lead surrogate code unit U+%04lx\n",
                        name, (long)value2, (long)value1, (long)lead);
                break;
            }
        }
    }
    utrie3_close(trie2);

    uprv_strcpy(name, testName);
    uprv_strcat(name, ".32");
    trie2=utrie3_fromUTrie(&trie1_32, errorValue, &errorCode);
    if(U_SUCCESS(errorCode)) {
        testFrozenTrie(name, trie2, UTRIE3_32_VALUE_BITS, checkRanges, countCheckRanges);
        for(lead=0xd800; lead<0xdc00; ++lead) {
            uint32_t value1, value2;
            value1=UTRIE_GET32_FROM_LEAD(&trie1_32, lead);
            value2=UTRIE3_GET32_FROM_BMP(trie2, lead);
            if(value1!=value2) {
                log_err("error: utrie3_fromUTrie(%s) wrong value %ld!=%ld "
                        "from lead surrogate code unit U+%04lx\n",
                        name, (long)value2, (long)value1, (long)lead);
                break;
            }
        }
    }
    utrie3_close(trie2);
#endif
}

static void
Trie12ConversionTest(void) {
    testTrie2FromTrie1("trie1->trie2",
                       setRanges2, UPRV_LENGTHOF(setRanges2),
                       checkRanges2, UPRV_LENGTHOF(checkRanges2));
}

void
addTrie3Test(TestNode** root) {
    addTest(root, &TrieTestSet1, "tsutil/trie3test/TrieTestSet1");
    addTest(root, &TrieTestSet2Overlap, "tsutil/trie3test/TrieTestSet2Overlap");
    addTest(root, &TrieTestSet3Initial9, "tsutil/trie3test/TrieTestSet3Initial9");
    addTest(root, &TrieTestSetEmpty, "tsutil/trie3test/TrieTestSetEmpty");
    addTest(root, &TrieTestSetSingleValue, "tsutil/trie3test/TrieTestSetSingleValue");
    addTest(root, &TrieTestSet2OverlapWithClone, "tsutil/trie3test/TrieTestSet2OverlapWithClone");
    addTest(root, &DummyTrieTest, "tsutil/trie3test/DummyTrieTest");
    addTest(root, &FreeBlocksTest, "tsutil/trie3test/FreeBlocksTest");
    addTest(root, &GrowDataArrayTest, "tsutil/trie3test/GrowDataArrayTest");
    addTest(root, &ManyAllSameBlocksTest, "tsutil/trie3test/ManyAllSameBlocksTest");
    addTest(root, &GetVersionTest, "tsutil/trie3test/GetVersionTest");
    addTest(root, &Trie12ConversionTest, "tsutil/trie3test/Trie12ConversionTest");
}
