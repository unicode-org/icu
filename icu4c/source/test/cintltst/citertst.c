/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CITERTST.C
*
* Modification History:
* Date      Name               Description
*           Madhu Katragadda   Ported for C API
* 02/19/01  synwee             Modified test case for new collation iterator
*********************************************************************************/
/*
 * Collation Iterator tests.
 * (Let me reiterate my position...)
 */

#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "cintltst.h"
#include "citertst.h"
#include "ccolltst.h"


void addCollIterTest(TestNode** root)
{
    addTest(root, &TestPrevious, "tscoll/citertst/TestPrevious");
    addTest(root, &TestOffset, "tscoll/citertst/TestOffset");
    addTest(root, &TestSetText, "tscoll/citertst/TestSetText");
    addTest(root, &TestMaxExpansion, "tscoll/citertst/TestMaxExpansion");
    addTest(root, &TestUnicodeChar, "tscoll/citertst/TestUnicodeChar");
    addTest(root, &TestNormalizedUnicodeChar,
                                "tscoll/citertst/TestNormalizedUnicodeChar");
    addTest(root, &TestNormalization, "tscoll/citertst/TestNormalization");
    addTest(root, &TestBug672, "tscoll/citertst/TestBug672");
    addTest(root, &TestBug672Normalize, "tscoll/citertst/TestBug672Normalize");
    addTest(root, &TestSmallBuffer, "tscoll/citertst/TestSmallBuffer");
}

/* The locales we support */

static const char * LOCALES[] = {"en_AU", "en_BE", "en_CA"};

static void TestBug672() {
    UErrorCode  status = U_ZERO_ERROR;
    UChar       pattern[20];
    UChar       text[50];
    int         i;
    int         result[3][3];

    u_uastrcpy(pattern, "resume");
    u_uastrcpy(text, "Time to resume updating my resume.");

    for (i = 0; i < 3; ++ i) {
        UCollator          *coll = ucol_open(LOCALES[i], &status);
        UCollationElements *pitr = ucol_openElements(coll, pattern, -1,
                                                     &status);
        UCollationElements *titer = ucol_openElements(coll, text, -1,
                                                     &status);
        if (U_FAILURE(status)) {
            log_err("ERROR: in creation of either the collator or the collation iterator :%s\n",
                    myErrorName(status));
            return;
        }

        log_verbose("locale tested %s\n", LOCALES[i]);

        while (ucol_next(pitr, &status) != UCOL_NULLORDER &&
               U_SUCCESS(status)) {
        }
        if (U_FAILURE(status)) {
            log_err("ERROR: reversing collation iterator :%s\n",
                    myErrorName(status));
            return;
        }
        ucol_reset(pitr);

        ucol_setOffset(titer, u_strlen(pattern), &status);
        if (U_FAILURE(status)) {
            log_err("ERROR: setting offset in collator :%s\n",
                    myErrorName(status));
            return;
        }
        result[i][0] = ucol_getOffset(titer);
        log_verbose("Text iterator set to offset %d\n", result[i][0]);

        /* Use previous() */
        ucol_previous(titer, &status);
        result[i][1] = ucol_getOffset(titer);
        log_verbose("Current offset %d after previous\n", result[i][1]);

        /* Add one to index */
        log_verbose("Adding one to current offset...\n");
        ucol_setOffset(titer, ucol_getOffset(titer) + 1, &status);
        if (U_FAILURE(status)) {
            log_err("ERROR: setting offset in collator :%s\n",
                    myErrorName(status));
            return;
        }
        result[i][2] = ucol_getOffset(titer);
        log_verbose("Current offset in text = %d\n", result[i][2]);
        ucol_closeElements(pitr);
        ucol_closeElements(titer);
        ucol_close(coll);
    }

    if (uprv_memcmp(result[0], result[1], 3) != 0 ||
        uprv_memcmp(result[1], result[2], 3) != 0) {
        log_err("ERROR: Different locales have different offsets at the same character\n");
    }
}



/*  Running this test with normalization enabled showed up a bug in the incremental
    normalization code. */
static void TestBug672Normalize() {
    UErrorCode  status = U_ZERO_ERROR;
    UChar       pattern[20];
    UChar       text[50];
    int         i;
    int         result[3][3];

    u_uastrcpy(pattern, "resume");
    u_uastrcpy(text, "Time to resume updating my resume.");

    for (i = 0; i < 3; ++ i) {
        UCollator          *coll = ucol_open(LOCALES[i], &status);
        UCollationElements *pitr = NULL;
        UCollationElements *titer = NULL;

        ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);

        pitr = ucol_openElements(coll, pattern, -1, &status);
        titer = ucol_openElements(coll, text, -1, &status);
        if (U_FAILURE(status)) {
            log_err("ERROR: in creation of either the collator or the collation iterator :%s\n",
                    myErrorName(status));
            return;
        }

        log_verbose("locale tested %s\n", LOCALES[i]);

        while (ucol_next(pitr, &status) != UCOL_NULLORDER &&
               U_SUCCESS(status)) {
        }
        if (U_FAILURE(status)) {
            log_err("ERROR: reversing collation iterator :%s\n",
                    myErrorName(status));
            return;
        }
        ucol_reset(pitr);

        ucol_setOffset(titer, u_strlen(pattern), &status);
        if (U_FAILURE(status)) {
            log_err("ERROR: setting offset in collator :%s\n",
                    myErrorName(status));
            return;
        }
        result[i][0] = ucol_getOffset(titer);
        log_verbose("Text iterator set to offset %d\n", result[i][0]);

        /* Use previous() */
        ucol_previous(titer, &status);
        result[i][1] = ucol_getOffset(titer);
        log_verbose("Current offset %d after previous\n", result[i][1]);

        /* Add one to index */
        log_verbose("Adding one to current offset...\n");
        ucol_setOffset(titer, ucol_getOffset(titer) + 1, &status);
        if (U_FAILURE(status)) {
            log_err("ERROR: setting offset in collator :%s\n",
                    myErrorName(status));
            return;
        }
        result[i][2] = ucol_getOffset(titer);
        log_verbose("Current offset in text = %d\n", result[i][2]);
        ucol_closeElements(pitr);
        ucol_closeElements(titer);
        ucol_close(coll);
    }

    if (uprv_memcmp(result[0], result[1], 3) != 0 ||
        uprv_memcmp(result[1], result[2], 3) != 0) {
        log_err("ERROR: Different locales have different offsets at the same character\n");
    }
}




/**
 * Test for CollationElementIterator previous and next for the whole set of
 * unicode characters.
 */
static void TestUnicodeChar()
{
    UChar source[0x100];
    UCollator *en_us;
    UCollationElements *iter;
    UErrorCode status = U_ZERO_ERROR;
    UChar codepoint;

    UChar *test;
    en_us = ucol_open("en_US", &status);
    if (U_FAILURE(status)){
       log_err("ERROR: in creation of collation data using ucol_open()\n %s\n",
              myErrorName(status));
       return;
    }

    for (codepoint = 1; codepoint < 0xFFFE;)
    {
      test = source;

      while (codepoint % 0xFF != 0)
      {
        if (u_isdefined(codepoint))
          *(test ++) = codepoint;
        codepoint ++;
      }

      if (u_isdefined(codepoint))
        *(test ++) = codepoint;

      if (codepoint != 0xFFFF)
        codepoint ++;

      *test = 0;
      iter=ucol_openElements(en_us, source, u_strlen(source), &status);
      if(U_FAILURE(status)){
          log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
              myErrorName(status));
          ucol_close(en_us);
          return;
      }
      /* A basic test to see if it's working at all */
      backAndForth(iter);
      ucol_closeElements(iter);

      /* null termination test */
      iter=ucol_openElements(en_us, source, -1, &status);
      if(U_FAILURE(status)){
          log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
              myErrorName(status));
          ucol_close(en_us);
          return;
      }
      /* A basic test to see if it's working at all */
      backAndForth(iter);
      ucol_closeElements(iter);
    }

    ucol_close(en_us);
}

/**
 * Test for CollationElementIterator previous and next for the whole set of
 * unicode characters with normalization on.
 */
static void TestNormalizedUnicodeChar()
{
    UChar source[0x100];
    UCollator *th_th;
    UCollationElements *iter;
    UErrorCode status = U_ZERO_ERROR;
    UChar codepoint;

    UChar *test;
    /* thai should have normalization on */
    th_th = ucol_open("th_TH", &status);
    if (U_FAILURE(status)){
        log_err("ERROR: in creation of thai collation using ucol_open()\n %s\n",
              myErrorName(status));
        return;
    }
    
    for (codepoint = 1; codepoint < 0xFFFE;)
    {
      test = source;

      while (codepoint % 0xFF != 0)
      {
        if (u_isdefined(codepoint))
          *(test ++) = codepoint;
        codepoint ++;
      }

      if (u_isdefined(codepoint))
        *(test ++) = codepoint;

      if (codepoint != 0xFFFF)
        codepoint ++;

      *test = 0;
      iter=ucol_openElements(th_th, source, u_strlen(source), &status);
      if(U_FAILURE(status)){
          log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
              myErrorName(status));
            ucol_close(th_th);
          return;
      }

      backAndForth(iter);
      ucol_closeElements(iter);

      iter=ucol_openElements(th_th, source, -1, &status);
      if(U_FAILURE(status)){
          log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
              myErrorName(status));
            ucol_close(th_th);
          return;
      }

      backAndForth(iter);
      ucol_closeElements(iter);
    }

    ucol_close(th_th);
}

/**
* Test the incremental normalization
*/
static void TestNormalization()
{
          UErrorCode          status = U_ZERO_ERROR;
    const char               *str    = 
                            "&a < \\u0300\\u0315 < A\\u0300\\u0315 < \\u0316\\u0315B < \\u0316\\u0300\\u0315";
          UCollator          *coll;
          UChar               rule[50];
          int                 rulelen = u_unescape(str, rule, 50);
          int                 count = 0;
    const char                *testdata[] =
                        {"\\u0300\\u0315", "\\u0315\\u0300",
                        "A\\u0300\\u0315B", "A\\u0315\\u0300B",
                        "A\\u0316\\u0315B", "A\\u0315\\u0316B",
                        "\\u0316\\u0300\\u0315", "\\u0315\\u0300\\u0316",
                        "A\\u0316\\u0300\\u0315B", "A\\u0315\\u0300\\u0316B",
                        "\\u0316\\u0315\\u0300", "A\\u0316\\u0315\\u0300B"};

    coll = ucol_open("fr", &status);
    if (U_SUCCESS(status)) {
        UChar source[2];
        UChar target[1];
        UCollationElements *iter;
        source[0] = 0x00E6;
        source[1] = 'E';
        iter = ucol_openElements(coll, source, 2, &status);
        backAndForth(iter);
        ucol_closeElements(iter);
        target[0] = 0x00C6;
        iter = ucol_openElements(coll, source, 1, &status);
        backAndForth(iter);
        ucol_closeElements(iter);
        ucol_strcoll(coll, source, 2, target, 1);
        ucol_close(coll);
    }

    coll = ucol_openRules(rule, rulelen, UNORM_NFD, UCOL_TERTIARY, &status);
    ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_ON, &status);
    if (U_FAILURE(status)){
        log_err("ERROR: in creation of collator using ucol_openRules()\n %s\n",
              myErrorName(status));
        return;
    }

    while (count < 12) {
        UCollationElements *iter;
        UChar               source[10];
        int                 srclen = 0;

        srclen = u_unescape(testdata[count], source, 10);
        iter = ucol_openElements(coll, source, srclen - 1, &status);

        if (U_FAILURE(status)){
            log_err("ERROR: in creation of collator element iterator\n %s\n",
                  myErrorName(status));
            return;
        }
        backAndForth(iter);
        ucol_closeElements(iter);

        iter = ucol_openElements(coll, source, -1, &status);

        if (U_FAILURE(status)){
            log_err("ERROR: in creation of collator element iterator\n %s\n",
                  myErrorName(status));
            return;
        }
        backAndForth(iter);
        ucol_closeElements(iter);
        count ++;
    }
    ucol_close(coll);
}

/**
 * Test for CollationElementIterator.previous()
 *
 * @bug 4108758 - Make sure it works with contracting characters
 *
 */
static void TestPrevious()
{
    UCollator *coll=NULL;
    UChar rule[50];
    UChar *source;
    UCollator *c1, *c2, *c3;
    UCollationElements *iter;
    UErrorCode status = U_ZERO_ERROR;

    test1=(UChar*)malloc(sizeof(UChar) * 50);
    test2=(UChar*)malloc(sizeof(UChar) * 50);
    u_uastrcpy(test1, "What subset of all possible test cases?");
    u_uastrcpy(test2, "has the highest probability of detecting");
    coll = ucol_open("en_US", &status);

    iter=ucol_openElements(coll, test1, u_strlen(test1), &status);
    log_verbose("English locale testing back and forth\n");
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
            myErrorName(status));
        ucol_close(coll);
        return;
    }
    /* A basic test to see if it's working at all */
    backAndForth(iter);
    ucol_closeElements(iter);
    ucol_close(coll);

    /* Test with a contracting character sequence */
    u_uastrcpy(rule, "&a,A < b,B < c,C, d,D < z,Z < ch,cH,Ch,CH");
    c1 = ucol_openRules(rule, u_strlen(rule), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);

    log_verbose("Contraction rule testing back and forth with no normalization\n");
    
    if (c1 == NULL || U_FAILURE(status))
    {
        log_err("Couldn't create a RuleBasedCollator with a contracting sequence\n %s\n",
            myErrorName(status));
        return;
    }
    source=(UChar*)malloc(sizeof(UChar) * 20);
    u_uastrcpy(source, "abchdcba");
    iter=ucol_openElements(c1, source, u_strlen(source), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
            myErrorName(status));
        return;
    }
    backAndForth(iter);
    ucol_closeElements(iter);
    ucol_close(c1);
    
    /* Test with an expanding character sequence */
    u_uastrcpy(rule, "&a < b < c/abd < d");
    c2 = ucol_openRules(rule, u_strlen(rule), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH,  &status);
    log_verbose("Expansion rule testing back and forth with no normalization\n");
    if (c2 == NULL || U_FAILURE(status))
    {
        log_err("Couldn't create a RuleBasedCollator with a contracting sequence.\n %s\n",
            myErrorName(status));
        return;
    }
    u_uastrcpy(source, "abcd");
    iter=ucol_openElements(c2, source, u_strlen(source), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
            myErrorName(status));
        return;
    }
    backAndForth(iter);
    ucol_closeElements(iter);
    ucol_close(c2);
    /* Now try both */
    u_uastrcpy(rule, "&a < b < c/aba < d < z < ch");
    c3 = ucol_openRules(rule, u_strlen(rule), UCOL_DEFAULT_NORMALIZATION,  UCOL_DEFAULT_STRENGTH, &status);
    log_verbose("Expansion/contraction rule testing back and forth with no normalization\n");

    if (c3 == NULL || U_FAILURE(status))
    {
        log_err("Couldn't create a RuleBasedCollator with a contracting sequence.\n %s\n",
            myErrorName(status));
        return;
    }
    u_uastrcpy(source, "abcdbchdc");
    iter=ucol_openElements(c3, source, u_strlen(source), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
            myErrorName(status));
        return;
    }
    backAndForth(iter);
    ucol_closeElements(iter);
    ucol_close(c3);
    source[0] = 0x0e41;
    source[1] = 0x0e02;
    source[2] = 0x0e41;
    source[3] = 0x0e02;
    source[4] = 0x0e27;
    source[5] = 0x61;
    source[6] = 0x62;
    source[7] = 0x63;
    source[8] = 0;

    coll = ucol_open("th_TH", &status);
    log_verbose("Thai locale testing back and forth with normalization\n");
    iter=ucol_openElements(coll, source, u_strlen(source), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
            myErrorName(status));
        return;
    }
    backAndForth(iter);
    ucol_closeElements(iter);
    ucol_close(coll);

    /* prev test */
    source[0] = 0x0061;
    source[1] = 0x30CF;
    source[2] = 0x3099;
    source[3] = 0x30FC;
    source[4] = 0;

    coll = ucol_open("ja_JP", &status);
    log_verbose("Japanese locale testing back and forth with normalization\n");
    iter=ucol_openElements(coll, source, u_strlen(source), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
            myErrorName(status));
        return;
    }
    backAndForth(iter);
    ucol_closeElements(iter);
    ucol_close(coll);

    free(source);
    free(test1);
    free(test2);
}

/**
 * Test for getOffset() and setOffset()
 */
static void TestOffset()
{
    UErrorCode status= U_ZERO_ERROR;
    UCollator *en_us=NULL;
    UCollationElements *iter, *pristine;
    int32_t offset;
    int32_t *orders;
    int32_t orderLength=0;
    test1=(UChar*)malloc(sizeof(UChar) * 50);
    test2=(UChar*)malloc(sizeof(UChar) * 50);
    u_uastrcpy(test1, "What subset of all possible test cases?");
    u_uastrcpy(test2, "has the highest probability of detecting");
    en_us = ucol_open("en_US", &status);
    log_verbose("Testing getOffset and setOffset for CollationElements\n");
    iter=ucol_openElements(en_us, test1, u_strlen(test1), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
            myErrorName(status));
    ucol_close(en_us);
        return;
    }
    /* Run all the way through the iterator, then get the offset */

    orders = getOrders(iter, &orderLength);

    offset = ucol_getOffset(iter);

    if (offset != u_strlen(test1))
    {
        log_err("offset at end != length %d vs %d\n", offset,
            u_strlen(test1) );
    }

    /* Now set the offset back to the beginning and see if it works */
    pristine=ucol_openElements(en_us, test1, u_strlen(test1), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n",
            myErrorName(status));
    ucol_close(en_us);
        return;
    }
    status = U_ZERO_ERROR;

    ucol_setOffset(iter, 0, &status);
    if (U_FAILURE(status))
    {
        log_err("setOffset failed. %s\n",    myErrorName(status));
    }
    else
    {
        assertEqual(iter, pristine);
    }

    ucol_closeElements(pristine);
    ucol_closeElements(iter);
    free(orders);
    ucol_close(en_us);
    free(test1);
    free(test2);
}

/**
 * Test for setText()
 */
static void TestSetText()
{
    int32_t c,i;
    UErrorCode status = U_ZERO_ERROR;
    UCollator *en_us=NULL;
    UCollationElements *iter1, *iter2;
    test1=(UChar*)malloc(sizeof(UChar) * 50);
    test2=(UChar*)malloc(sizeof(UChar) * 50);
    u_uastrcpy(test1, "What subset of all possible test cases?");
    u_uastrcpy(test2, "has the highest probability of detecting");
    en_us = ucol_open("en_US", &status);
    log_verbose("testing setText for Collation elements\n");
    iter1=ucol_openElements(en_us, test1, u_strlen(test1), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator1 using ucol_openElements()\n %s\n",
            myErrorName(status));
    ucol_close(en_us);
        return;
    }
    iter2=ucol_openElements(en_us, test2, u_strlen(test2), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator2 using ucol_openElements()\n %s\n",
            myErrorName(status));
    ucol_close(en_us);
        return;
    }

    /* Run through the second iterator just to exercise it */
    c = ucol_next(iter2, &status);
    i = 0;

    while ( ++i < 10 && (c != UCOL_NULLORDER))
    {
        if (U_FAILURE(status))
        {
            log_err("iter2->next() returned an error. %s\n", myErrorName(status));
            ucol_closeElements(iter2);
            ucol_closeElements(iter1);
    ucol_close(en_us);
            return;
        }

        c = ucol_next(iter2, &status);
    }

    /* Now set it to point to the same string as the first iterator */
    ucol_setText(iter2, test1, u_strlen(test1), &status);
    if (U_FAILURE(status))
    {
        log_err("call to iter2->setText(test1) failed. %s\n", myErrorName(status));
    }
    else
    {
        assertEqual(iter1, iter2);
    }

    ucol_closeElements(iter2);
    ucol_closeElements(iter1);
    ucol_close(en_us);
    free(test1);
    free(test2);
}



static void backAndForth(UCollationElements *iter)
{
    /* Run through the iterator forwards and stick it into an array */
    int32_t index, o;
    UErrorCode status = U_ZERO_ERROR;
    int32_t orderLength = 0;
    int32_t *orders;
    orders= getOrders(iter, &orderLength);


    /* Now go through it backwards and make sure we get the same values */
    index = orderLength;
    ucol_reset(iter);

    /* synwee : changed */
    while ((o = ucol_previous(iter, &status)) != UCOL_NULLORDER)
    {
      if (o != orders[-- index])
      {
        if (o == 0)
          index ++;
        else
        {
          while (index > 0 && orders[-- index] == 0)
          {
          }
          if (o != orders[index])
          {
            log_err("Mismatch at index : 0x%x\n", index);
            return;
          }
        }
      }
    }

    while (index != 0 && orders[index - 1] == 0) {
      index --;
    }

    if (index != 0)
    {
        log_err("Didn't get back to beginning - index is %d\n", index);

        ucol_reset(iter);
        log_err("\nnext: ");
        if ((o = ucol_next(iter, &status)) != UCOL_NULLORDER)
        {
            log_err("Error at %x\n", o);
        }
        log_err("\nprev: ");
        if ((o = ucol_previous(iter, &status)) != UCOL_NULLORDER)
        {
            log_err("Error at %x\n", o);
        }
        log_verbose("\n");
    }

    free(orders);
}

/** @bug 4108762
 * Test for getMaxExpansion()
 */
static void TestMaxExpansion()
{
    UErrorCode          status = U_ZERO_ERROR;
    UCollator          *coll   ;/*= ucol_open("en_US", &status);*/
    UChar               ch     = 0;
    UCollationElements *iter   ;/*= ucol_openElements(coll, &ch, 1, &status);*/

    UChar rule[256];
    u_uastrcpy(rule, "&a < ab < c/aba < d < z < ch");
    coll = ucol_openRules(rule, u_strlen(rule), UCOL_DEFAULT_NORMALIZATION,
        UCOL_DEFAULT_STRENGTH, &status);
    iter = ucol_openElements(coll, &ch, 1, &status);

    while (ch < 0xFFFF && U_SUCCESS(status)) {
        int      count = 1;
        uint32_t order;
        ch++;
        ucol_setText(iter, &ch, 1, &status);
        order = ucol_previous(iter, &status);

        /* thai management */
        if (order == 0)
            order = ucol_previous(iter, &status);

        while (U_SUCCESS(status) &&
            ucol_previous(iter, &status) != UCOL_NULLORDER) {
            count ++;
        }

        if (U_FAILURE(status) && ucol_getMaxExpansion(iter, order) < count) {
            log_err("Failure at codepoint %d, maximum expansion count < %d\n",
                ch, count);
        }
    }

    ucol_closeElements(iter);
    ucol_close(coll);
}

/**
 * Return an integer array containing all of the collation orders
 * returned by calls to next on the specified iterator
 */
static int32_t* getOrders(UCollationElements *iter, int32_t *orderLength)
{
    UErrorCode status;
    int32_t order;
    int32_t maxSize = 100;
    int32_t size = 0;
    int32_t *temp;
    int32_t *orders =(int32_t*)malloc(sizeof(int32_t) * maxSize);
    status= U_ZERO_ERROR;


    while ((order=ucol_next(iter, &status)) != UCOL_NULLORDER)
    {
        if (size == maxSize)
        {
            maxSize *= 2;
            temp = (int32_t*)malloc(sizeof(int32_t) * maxSize);

            memcpy(temp, orders, size * sizeof(int32_t));
            free(orders);
            orders = temp;

        }

        orders[size++] = order;
    }

    if (maxSize > size)
    {
        if (size == 0) {
            size = 1;
            temp = (int32_t*)malloc(sizeof(int32_t) * size);
            temp[0] = 0;
        }
        else {
            temp = (int32_t*)malloc(sizeof(int32_t) * size);
            memcpy(temp, orders, size * sizeof(int32_t));
        }

        free(orders);
        orders = temp;
    }

    *orderLength = size;
    return orders;
}


static void assertEqual(UCollationElements *i1, UCollationElements *i2)
{
    int32_t c1, c2;
    int32_t count = 0;
    UErrorCode status = U_ZERO_ERROR;

    do
    {
        c1 = ucol_next(i1, &status);
        c2 = ucol_next(i2, &status);

        if (c1 != c2)
        {
            log_err("Error in iteration %d assetEqual between\n  %d  and   %d, they are not equal\n", count, c1, c2);
            break;
        }

        count += 1;
    }
    while (c1 != UCOL_NULLORDER);
}

/**
 * Testing iterators with extremely small buffers
 */
static void TestSmallBuffer()
{
    UErrorCode          status = U_ZERO_ERROR;
    UCollator          *coll;
    UCollationElements *testiter,
                       *iter;
    int32_t             count = 0;
    int32_t            *testorders,
                       *orders;

    UChar teststr[500];
    UChar str[] = {0x300, 0x31A, 0};
    /*
    creating a long string of decomposable characters,
    since by default the writable buffer is of size 256
    */
    while (count < 500) {
        if ((count & 1) == 0) {
            teststr[count ++] = 0x300;
        }
        else {
            teststr[count ++] = 0x31A;
        }
    }

    coll = ucol_open("th_TH", &status);
    testiter = ucol_openElements(coll, teststr, 500, &status);
    iter = ucol_openElements(coll, str, 2, &status);

    orders     = getOrders(iter, &count);
    if (count != 2) {
        log_err("Error collation elements size is not 2 for \\u0300\\u031A\n");
    }

    /*
    this will rearrange the string data to 250 characters of 0x300 first then
    250 characters of 0x031A
    */
    testorders = getOrders(testiter, &count);

    if (count != 500) {
        log_err("Error decomposition does not give the right sized collation elements\n");
    }

    while (count != 0) {
        /* UCA collation element for 0x0F76 */
        if ((count > 250 && testorders[-- count] != orders[1]) ||
            (count <= 250 && testorders[-- count] != orders[0])) {
            log_err("Error decomposition does not give the right collation element at %d count\n", count);
            break;
        }
    }

    free(testorders);
    free(orders);
    ucol_closeElements(testiter);
    ucol_closeElements(iter);
    ucol_close(coll);
}
