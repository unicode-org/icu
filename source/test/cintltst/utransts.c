/*
*******************************************************************************
*   Copyright (C) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   Date        Name        Description
*   06/23/00    aliu        Creation.
*******************************************************************************
*/

#include "unicode/utrans.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "cintltst.h"
#include "unicode/ustring.h"
#include <stdio.h>

#define TEST(x) addTest(root, &x, "utrans/" # x)

void TestAPI();
void TestSimpleRules();
static void _expectRules(const char*, const char*, const char*);

void
addUTransTest(TestNode** root) {
    TEST(TestAPI);
    TEST(TestSimpleRules);
}

void TestAPI() {
    enum { BUF_CAP = 128 };
    char buf[BUF_CAP], buf2[BUF_CAP];
    UErrorCode status = U_ZERO_ERROR;
    UTransliterator* trans = NULL;
    int32_t i, n;
    
    /* Test getAvailableIDs */
    n = utrans_countAvailableIDs();
    if (n < 1) {
        log_err("FAIL: utrans_countAvailableIDs() returned %d\n", n);
    } else {
        log_verbose("System ID count: %d\n", n);
    }
    for (i=0; i<n; ++i) {
        utrans_getAvailableID(i, buf, BUF_CAP);
        if (*buf == 0) {
            log_err("FAIL: System transliterator %d: \"\"\n", i);
        } else {
            log_verbose("System transliterator %d: \"%s\"\n", i, buf);
        }
    }

    /* Test open */
    utrans_getAvailableID(0, buf, BUF_CAP);
    trans = utrans_open(buf, UTRANS_FORWARD, &status);
    if (U_FAILURE(status)) {
        log_err("FAIL: utrans_open(%s) failed, error=%s\n",
                buf, u_errorName(status));
    }
    
    /* Test getID */
    utrans_getID(trans, buf2, BUF_CAP);
    if (0 != uprv_strcmp(buf, buf2)) {
        log_err("FAIL: utrans_getID(%s) returned %s\n",
                buf, buf2);
    }
    utrans_close(trans);
}

void TestSimpleRules() {
    /* Test rules */
    /* Example: rules 1. ab>x|y
     *                2. yc>z
     *
     * []|eabcd  start - no match, copy e to tranlated buffer
     * [e]|abcd  match rule 1 - copy output & adjust cursor
     * [ex|y]cd  match rule 2 - copy output & adjust cursor
     * [exz]|d   no match, copy d to transliterated buffer
     * [exzd]|   done
     */
    _expectRules("ab>x|y;"
                 "yc>z",
                 "eabcd", "exzd");

    /* Another set of rules:
     *    1. ab>x|yzacw
     *    2. za>q
     *    3. qc>r
     *    4. cw>n
     *
     * []|ab       Rule 1
     * [x|yzacw]   No match
     * [xy|zacw]   Rule 2
     * [xyq|cw]    Rule 4
     * [xyqn]|     Done
     */
    _expectRules("ab>x|yzacw;"
                 "za>q;"
                 "qc>r;"
                 "cw>n",
                 "ab", "xyqn");

    /* Test categories
     */
    _expectRules("$dummy=" "\\uE100" ";" /* careful here with E100 */
                 "$vowel=[aeiouAEIOU];"
                 "$lu=[:Lu:];"
                 "$vowel } $lu > '!';"
                 "$vowel > '&';"
                 "'!' { $lu > '^';"
                 "$lu > '*';"
                 "a > ERROR",
                 "abcdefgABCDEFGU", "&bcd&fg!^**!^*&");
}

void _expectRules(const char* crules,
                  const char* cfrom,
                  const char* cto) {
    /* u_uastrcpy has no capacity param for the buffer -- so just
     * make all buffers way too big */
    enum { CAP = 256 };
    UChar rules[CAP];
    UChar from[CAP];
    UChar to[CAP];
    UChar buf[CAP];
    UTransliterator *trans;
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseErr;
    int32_t limit;
    UTransPosition pos;

    u_uastrcpy(rules, crules);
    u_uastrcpy(from, cfrom);
    u_uastrcpy(to, cto);

    trans = utrans_openRules("ID", rules, -1, UTRANS_FORWARD,
                             &parseErr, &status);
    if (U_FAILURE(status)) {
        utrans_close(trans);
        log_err("FAIL: utrans_openRules(%s) failed, error=%s\n",
                crules, u_errorName(status));
        return;
    }

    /* utrans_transUChars() */
    u_strcpy(buf, from);
    limit = u_strlen(buf);
    utrans_transUChars(trans, buf, NULL, CAP, 0, &limit, &status);
    if (U_FAILURE(status)) {
        utrans_close(trans);
        log_err("FAIL: utrans_transUChars() failed, error=%s\n",
                u_errorName(status));
        return;
    }

    if (0 == u_strcmp(buf, to)) {
        log_verbose("Ok: utrans_transUChars(%s) x %s -> %s\n",
                    crules, cfrom, cto);
    } else {
        char actual[CAP];
        u_austrcpy(actual, buf);
        log_err("FAIL: utrans_transUChars(%s) x %s -> %s, expected %s\n",
                crules, cfrom, actual, cto);
    }

    /* utrans_transIncrementalUChars() */
    u_strcpy(buf, from);
    pos.start = pos.contextStart = 0;
    pos.limit = pos.contextLimit = u_strlen(buf);
    utrans_transIncrementalUChars(trans, buf, NULL, CAP, &pos, &status);
    utrans_transUChars(trans, buf, NULL, CAP, pos.start, &pos.limit, &status);
    if (U_FAILURE(status)) {
        utrans_close(trans);
        log_err("FAIL: utrans_transIncrementalUChars() failed, error=%s\n",
                u_errorName(status));
        return;
    }

    if (0 == u_strcmp(buf, to)) {
        log_verbose("Ok: utrans_transIncrementalUChars(%s) x %s -> %s\n",
                    crules, cfrom, cto);
    } else {
        char actual[CAP];
        u_austrcpy(actual, buf);
        log_err("FAIL: utrans_transIncrementalUChars(%s) x %s -> %s, expected %s\n",
                crules, cfrom, actual, cto);
    }

    utrans_close(trans);
}
