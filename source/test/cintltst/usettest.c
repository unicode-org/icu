/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#include "unicode/uset.h"
#include "unicode/ustring.h"
#include "cintltst.h"
#include <stdlib.h>
#include <string.h>

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

#define TEST(x) addTest(root, &x, "uset/" # x)

static void TestAPI(void);
static void Testj2269(void);
static void TestSerialized(void);
static void TestNonInvariantPattern(void);

void addUSetTest(TestNode** root);

static void expect(const USet* set,
                   const char* inList,
                   const char* outList,
                   UErrorCode* ec);
static void expectContainment(const USet* set,
                              const char* list,
                              UBool isIn);
static char oneUCharToChar(UChar32 c);
static void expectItems(const USet* set,
                        const char* items);

void
addUSetTest(TestNode** root) {
    TEST(TestAPI);
    TEST(Testj2269);
    TEST(TestSerialized);
    TEST(TestNonInvariantPattern);
}

/*------------------------------------------------------------------
 * Tests
 *------------------------------------------------------------------*/

static void Testj2269() {
  UErrorCode status = U_ZERO_ERROR;
  UChar a[4] = { 0x61, 0x62, 0x63, 0 };
  USet *s = uset_open(1, 0);
  uset_addString(s, a, 3);
  a[0] = 0x63; a[1] = 0x63;
  expect(s, "{abc}", "{ccc}", &status);
  uset_close(s);
}

static const UChar PAT[] = {91,97,45,99,123,97,98,125,93,0}; /* "[a-c{ab}]" */
static const int32_t PAT_LEN = (sizeof(PAT) / sizeof(PAT[0])) - 1;

static const UChar STR_bc[] = {98,99,0}; /* "bc" */
static const int32_t STR_bc_LEN = (sizeof(STR_bc) / sizeof(STR_bc[0])) - 1;

static const UChar STR_ab[] = {97,98,0}; /* "ab" */
static const int32_t STR_ab_LEN = (sizeof(STR_ab) / sizeof(STR_ab[0])) - 1;

/**
 * Basic API test for uset.x
 */
static void TestAPI() {
    USet* set;
    UErrorCode ec;
    
    /* [] */
    set = uset_open(1, 1);
    uset_clear(set);
    expect(set, "", "abc{ab}", NULL);
    uset_close(set);

    /* [ABC] */
    set = uset_open(0x0041, 0x0043);
    expect(set, "ABC", "DEF{ab}", NULL);
    uset_close(set);

    /* [a-c{ab}] */
    ec = U_ZERO_ERROR;
    set = uset_openPattern(PAT, PAT_LEN, &ec);
    if(U_FAILURE(ec)) {
        log_data_err("uset_openPattern([a-c{ab}]) failed - %s\n", u_errorName(ec));
        return;
    }
    expect(set, "abc{ab}", "def{bc}", &ec);

    /* [a-d{ab}] */
    uset_add(set, 0x64);
    expect(set, "abcd{ab}", "ef{bc}", NULL);

    /* [acd{ab}{bc}] */
    uset_remove(set, 0x62);
    uset_addString(set, STR_bc, STR_bc_LEN);
    expect(set, "acd{ab}{bc}", "bef{cd}", NULL);

    /* [acd{bc}] */
    uset_removeString(set, STR_ab, STR_ab_LEN);
    expect(set, "acd{bc}", "bfg{ab}", NULL);

    /* [^acd{bc}] */
    uset_complement(set);
    expect(set, "bef{bc}", "acd{ac}", NULL);

    /* [a-e{bc}] */
    uset_complement(set);
    uset_addRange(set, 0x0062, 0x0065);
    expect(set, "abcde{bc}", "fg{ab}", NULL);

    /* [de{bc}] */
    uset_removeRange(set, 0x0050, 0x0063);
    expect(set, "de{bc}", "bcfg{ab}", NULL);

    uset_close(set);
}

/*------------------------------------------------------------------
 * Support
 *------------------------------------------------------------------*/

/**
 * Verifies that the given set contains the characters and strings in
 * inList, and does not contain those in outList.  Also verifies that
 * 'set' is not NULL and that 'ec' succeeds.
 * @param set the set to test, or NULL (on error)
 * @param inList list of set contents, in iteration order.  Format is
 * list of individual strings, in iteration order, followed by sorted
 * list of strings, delimited by {}.  This means we do not test
 * characters '{' or '}' and we do not test strings containing those
 * characters either.
 * @param outList list of things not in the set.  Same format as
 * inList.
 * @param ec an error code, checked for success.  May be NULL in which
 * case it is ignored.
 */
static void expect(const USet* set,
                   const char* inList,
                   const char* outList,
                   UErrorCode* ec) {
    if (ec!=NULL && U_FAILURE(*ec)) {
        log_err("FAIL: %s\n", u_errorName(*ec));
        return;
    }
    if (set == NULL) {
        log_err("FAIL: USet is NULL\n");
        return;
    }
    expectContainment(set, inList, TRUE);
    expectContainment(set, outList, FALSE);
    expectItems(set, inList);
}

static void expectContainment(const USet* set,
                              const char* list,
                              UBool isIn) {
    const char* p = list;
    UChar ustr[128];
    char *pat;
    UErrorCode ec;
    int32_t rangeStart = -1, rangeEnd = -1, length;
            
    ec = U_ZERO_ERROR;
    length = uset_toPattern(set, ustr, sizeof(ustr), TRUE, &ec);
    if(U_FAILURE(ec)) {
        log_err("FAIL: uset_toPattern() fails in expectContainment() - %s\n", u_errorName(ec));
        return;
    }
    pat=aescstrdup(ustr, length);

    while (*p) {
        if (*p=='{') {
            const char* stringStart = ++p;
            int32_t stringLength = 0;
            char strCopy[64];

            while (*p++ != '}') {}
            stringLength = p - stringStart - 1;
            strncpy(strCopy, stringStart, stringLength);
            strCopy[stringLength] = 0;

            u_charsToUChars(stringStart, ustr, stringLength);
            
            if (uset_containsString(set, ustr, stringLength) == isIn) {
                log_verbose("Ok: %s %s \"%s\"\n", pat,
                            (isIn ? "contains" : "does not contain"),
                            strCopy);
            } else {
                log_err("FAIL: %s %s \"%s\"\n", pat,
                        (isIn ? "does not contain" : "contains"),
                        strCopy);
            }
        }

        else {
            UChar32 c;

            u_charsToUChars(p, ustr, 1);
            c = ustr[0];

            if (uset_contains(set, c) == isIn) {
                log_verbose("Ok: %s %s '%c'\n", pat,
                            (isIn ? "contains" : "does not contain"),
                            *p);
            } else {
                log_err("FAIL: %s %s '%c'\n", pat,
                        (isIn ? "does not contain" : "contains"),
                        *p);
            }

            /* Test the range API too by looking for ranges */
            if (c == rangeEnd+1) {
                rangeEnd = c;
            } else {
                if (rangeStart >= 0) {
                    if (uset_containsRange(set, rangeStart, rangeEnd) == isIn) {
                        log_verbose("Ok: %s %s U+%04X-U+%04X\n", pat,
                                    (isIn ? "contains" : "does not contain"),
                                    rangeStart, rangeEnd);
                    } else {
                        log_err("FAIL: %s %s U+%04X-U+%04X\n", pat,
                                (isIn ? "does not contain" : "contains"),
                                rangeStart, rangeEnd);
                    }
                }
                rangeStart = rangeEnd = c;
            }

            ++p;
        }
    }

    if (rangeStart >= 0) {
        if (uset_containsRange(set, rangeStart, rangeEnd) == isIn) {
            log_verbose("Ok: %s %s U+%04X-U+%04X\n", pat,
                        (isIn ? "contains" : "does not contain"),
                        rangeStart, rangeEnd);
        } else {
            log_err("FAIL: %s %s U+%04X-U+%04X\n", pat,
                    (isIn ? "does not contain" : "contains"),
                    rangeStart, rangeEnd);
        }
    }
}

/* This only works for invariant BMP chars */
static char oneUCharToChar(UChar32 c) {
    UChar ubuf[1];
    char buf[1];
    ubuf[0] = (UChar) c;
    u_UCharsToChars(ubuf, buf, 1);
    return buf[0];
}

static void expectItems(const USet* set,
                        const char* items) {
    const char* p = items;
    UChar ustr[128], itemStr[128];
    char buf[128];
    char *pat;
    UErrorCode ec;
    int32_t expectedSize = 0;
    int32_t itemCount = uset_getItemCount(set);
    int32_t itemIndex = 0;
    UChar32 start = 1, end = 0;
    int32_t itemLen = 0, length;

    ec = U_ZERO_ERROR;
    length = uset_toPattern(set, ustr, sizeof(ustr), TRUE, &ec);
    if (U_FAILURE(ec)) {
        log_err("FAIL: uset_toPattern => %s\n", u_errorName(ec));
        return;
    }
    pat=aescstrdup(ustr, length);

    if (uset_isEmpty(set) != (strlen(items)==0)) {
        log_err("FAIL: %s should return %s from isEmpty\n",
                pat,
                strlen(items)==0 ? "TRUE" : "FALSE");
    }

    /* Don't test patterns starting with "[^" */
    if (u_strlen(ustr) > 2 && ustr[1] == 0x5e /*'^'*/) {
        return;
    }

    while (*p) {

        ++expectedSize;

        if (start > end || start == -1) {
            /* Fetch our next item */
            if (itemIndex >= itemCount) {
                log_err("FAIL: ran out of items iterating %s\n", pat);
                return;
            }

            itemLen = uset_getItem(set, itemIndex, &start, &end,
                                   itemStr, sizeof(itemStr), &ec);
            if (U_FAILURE(ec) || itemLen < 0) {
                log_err("FAIL: uset_getItem => %s\n", u_errorName(ec));
                return;
            }

            if (itemLen == 0) {
                log_verbose("Ok: %s item %d is %c-%c\n", pat,
                            itemIndex, oneUCharToChar(start),
                            oneUCharToChar(end));
            } else {
                itemStr[itemLen] = 0;
                u_UCharsToChars(itemStr, buf, itemLen+1);
                log_verbose("Ok: %s item %d is \"%s\"\n", pat, itemIndex, buf);
            }

            ++itemIndex;
        }

        if (*p=='{') {
            const char* stringStart = ++p;
            int32_t stringLength = 0;
            char strCopy[64];

            while (*p++ != '}') {}
            stringLength = p - stringStart - 1;
            strncpy(strCopy, stringStart, stringLength);
            strCopy[stringLength] = 0;

            u_charsToUChars(stringStart, ustr, stringLength);
            ustr[stringLength] = 0;
            
            if (itemLen == 0) {
                log_err("FAIL: for %s expect \"%s\" next, but got a char\n",
                        pat, strCopy);
                return;
            }

            if (u_strcmp(ustr, itemStr) != 0) {
                log_err("FAIL: for %s expect \"%s\" next\n",
                        pat, strCopy);
                return;
            }
        }

        else {
            UChar32 c;

            u_charsToUChars(p, ustr, 1);
            c = ustr[0];

            if (itemLen != 0) {
                log_err("FAIL: for %s expect '%c' next, but got a string\n",
                        pat, *p);
                return;
            }

            if (c != start++) {
                log_err("FAIL: for %s expect '%c' next\n",
                        pat, *p);
                return;
            }

            ++p;
        }
    }

    if (uset_size(set) == expectedSize) {
        log_verbose("Ok: %s size is %d\n", pat, expectedSize);
    } else {
        log_err("FAIL: %s size is %d, expected %d\n",
                pat, uset_size(set), expectedSize);
    }
}

static void
TestSerialized() {
    uint16_t buffer[1000];
    USerializedSet sset;
    USet *set;
    UErrorCode errorCode;
    UChar32 c;
    int32_t length;

    /* use a pattern that generates both BMP and supplementary code points */
    U_STRING_DECL(pattern, "[:Cf:]", 6);
    U_STRING_INIT(pattern, "[:Cf:]", 6);

    errorCode=U_ZERO_ERROR;
    set=uset_openPattern(pattern, -1, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_data_err("uset_openPattern([:Cf:]) failed - %s\n", u_errorName(errorCode));
        return;
    }

    length=uset_serialize(set, buffer, LENGTHOF(buffer), &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("unable to uset_serialize([:Cf:]) - %s\n", u_errorName(errorCode));
        uset_close(set);
        return;
    }

    uset_getSerializedSet(&sset, buffer, length);
    for(c=0; c<=0x10ffff; ++c) {
        if(uset_contains(set, c)!=uset_serializedContains(&sset, c)) {
            log_err("uset_contains(U+%04x)!=uset_serializedContains(U+%04x)\n", c);
            break;
        }
    }

    uset_close(set);
}

/**
 * Make sure that when non-invariant chars are passed to uset_openPattern
 * they do not cause an ugly failure mode (e.g. assertion failure).
 * JB#3795.
 */
static void
TestNonInvariantPattern() {
    UErrorCode ec = U_ZERO_ERROR;
    /* The critical part of this test is that the following pattern
       must contain a non-invariant character. */
    static const char *pattern = "[:ccc!=0:]";
    UChar buf[256];
    int32_t len = u_unescape(pattern, buf, 256);
    /* This test 'fails' by having an assertion failure within the
       following call.  It passes by running to completion with no
       assertion failure. */
    USet *set = uset_openPattern(buf, len, &ec);
    uset_close(set);
}

/*eof*/
