/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/test/cintltst/usettest.c,v $ 
**********************************************************************
*/
#include "unicode/uset.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "cintltst.h"
#include "unicode/ustring.h"

#define TEST(x) addTest(root, &x, "uset/" # x)

static void TestAPI(void);
static void Testj2269(void);

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
}

/*------------------------------------------------------------------
 * Tests
 *------------------------------------------------------------------*/

static void Testj2269() {
  UErrorCode status = U_ZERO_ERROR;
  UChar a[4] = { 0x61, 0x62, 0x63, 0 };
  UChar b[4] = { 0x61, 0x62, 0x63, 0 };
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
    char pat[128];
    UErrorCode ec;
            
    ec = U_ZERO_ERROR;
    uset_toPattern(set, ustr, sizeof(ustr), TRUE, &ec);
    u_UCharsToChars(ustr, pat, u_strlen(ustr)+1);

    while (*p) {
        if (*p=='{') {
            const char* stringStart = ++p;
            int32_t stringLength = 0;
            char strCopy[64];

            while (*p++ != '}') {}
            stringLength = p - stringStart - 1;
            uprv_strncpy(strCopy, stringStart, stringLength);
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

            ++p;
        }
    }
}

/* This only works for BMP chars */
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
    char pat[128], buf[128];
    UErrorCode ec;
    int32_t expectedSize = 0;
    int32_t itemCount = uset_getItemCount(set);
    int32_t itemIndex = 0;
    UChar32 start = 1, end = 0;
    int32_t itemLen = 0;

    ec = U_ZERO_ERROR;
    uset_toPattern(set, ustr, sizeof(ustr), TRUE, &ec);
    if (U_FAILURE(ec)) {
        log_err("FAIL: uset_toPattern => %s\n", u_errorName(ec));
        return;
    }
    u_UCharsToChars(ustr, pat, u_strlen(ustr)+1);

    if (uset_isEmpty(set) != (uprv_strlen(items)==0)) {
        log_err("FAIL: %s should return %s from isEmpty\n",
                pat,
                uprv_strlen(items)==0 ? "TRUE" : "FALSE");
    }

    /* Don't test patterns starting with "[^" */
    if (uprv_strlen(pat) > 2 && pat[1] == '^') {
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
            uprv_strncpy(strCopy, stringStart, stringLength);
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

/*eof*/
