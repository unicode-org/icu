/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CITERTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for C API
*********************************************************************************
/*
 * Collation Iterator tests.
 * (Let me reiterate my position...)
 */

#include "unicode/utypes.h"
#include "unicode/ucol.h"
#include "unicode/uloc.h"
#include "cintltst.h"
#include "citertst.h"
#include "unicode/ustring.h"

#define ARRAY_LENGTH(array) (sizeof array / sizeof array[0])

static UErrorCode status = U_ZERO_ERROR;
UCollator *en_us=0;


void addCollIterTest(TestNode** root)
{
    
   
    addTest(root, &TestPrevious, "tscoll/citertst/TestPrevious");
    addTest(root, &TestOffset, "tscoll/citertst/TestOffset");
    addTest(root, &TestSetText, "tscoll/citertst/TestSetText");
    addTest(root, &TestMaxExpansion, "tscoll/citertst/TestMaxExpansion");
   
    
}


/**
 * Test for CollationElementIterator.previous()
 *
 * @bug 4108758 - Make sure it works with contracting characters
 * 
 */
void TestPrevious()
{
    UChar rule[50];
    UChar *source;
    UCollator *c1, *c2, *c3;
    UCollationElements *iter;
    UErrorCode status = U_ZERO_ERROR;
    test1=(UChar*)malloc(sizeof(UChar) * 50);
    test2=(UChar*)malloc(sizeof(UChar) * 50);
    u_uastrcpy(test1, "What subset of all possible test cases?");
    u_uastrcpy(test2, "has the highest probability of detecting");
    en_us = ucol_open("en_US", &status);
    
    iter=ucol_openElements(en_us, test1, u_strlen(test1), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n", 
            myErrorName(status));
        return;
    }
    /* A basic test to see if it's working at all */
    backAndForth(iter);
    ucol_closeElements(iter);

    /* Test with a contracting character sequence */
    u_uastrcpy(rule, " < a,A < b,B < c,C, d,D < z,Z < ch,cH,Ch,CH");
    c1 = ucol_openRules(rule, u_strlen(rule), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
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
    free(source);

    /* Test with an expanding character sequence */
    u_uastrcpy(rule, "< a < b < c/abd < d");
    c2 = ucol_openRules(rule, u_strlen(rule), UCOL_NO_NORMALIZATION, UCOL_DEFAULT_STRENGTH,  &status);
    if (c2 == NULL || U_FAILURE(status))
    {
        log_err("Couldn't create a RuleBasedCollator with a contracting sequence.\n %s\n", 
            myErrorName(status));
        return;
    }
    source=(UChar*)malloc(sizeof(UChar) * 5);    
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
    free(source);
    /* Now try both */
    u_uastrcpy(rule, "< a < b < c/aba < d < z < ch");
    c3 = ucol_openRules(rule, u_strlen(rule), UCOL_DEFAULT_NORMALIZATION,  UCOL_DEFAULT_STRENGTH, &status);
    if (c3 == NULL || U_FAILURE(status))
    {
        log_err("Couldn't create a RuleBasedCollator with a contracting sequence.\n %s\n", 
            myErrorName(status));
        return;
    }
    source=(UChar*)malloc(sizeof(UChar) * 10);    
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
    free(source);
    ucol_close(en_us);
    free(test1);
    free(test2);
}

/**
 * Test for getOffset() and setOffset()
 */
void TestOffset()
{    
    UErrorCode status= U_ZERO_ERROR;
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
void TestSetText()
{
    int32_t c,i;
    UErrorCode status = U_ZERO_ERROR;
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
        return;
    }
    iter2=ucol_openElements(en_us, test2, u_strlen(test2), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator2 using ucol_openElements()\n %s\n", 
            myErrorName(status));
        return;
    }
   
    /* Run through the second iterator just to exercise it */
    c = ucol_next(iter2, &status);
    i = 0;

    while ( ++i < 10 && c != UCOL_NULLORDER)
    {
        if (U_FAILURE(status))
        {
            log_err("iter2->next() returned an error. %s\n", myErrorName(status));
            ucol_closeElements(iter2);
            ucol_closeElements(iter1);
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



void backAndForth(UCollationElements *iter)
{
    /* Run through the iterator forwards and stick it into an array */
    int32_t index, o;
    UErrorCode status = U_ZERO_ERROR;
    int32_t orderLength = 0;
    int32_t *orders;
    orders= getOrders(iter, &orderLength);
    
    
    /* Now go through it backwards and make sure we get the same values */
    index = orderLength;
    
    while ((o = ucol_previous(iter, &status)) != UCOL_NULLORDER)
    {
        if (o != orders[--index])
        {
            
            log_err("Mismatch at index : %d\n", index);
            break;
        }
    }

    if (index != 0)
    {
        
        log_err("Didn't get back to beginning - index is %d\n", index);

        ucol_reset(iter);
        log_err("\nnext: ");
        while ((o = ucol_next(iter, &status)) != UCOL_NULLORDER)
        {
            log_err("Error at %d\n", o);
        }
        log_err("\nprev: ");
        while ((o = ucol_previous(iter, &status)) != UCOL_NULLORDER)
        {
            log_err("Error at %d\n", o);
        }
        log_verbose("\n");
    }

    
}
/** @bug 4108762
 * Test for getMaxExpansion()
 */
void TestMaxExpansion()
{
  /* Try a simple one first: */
  /* The only expansion ends with 'e' and has length 2 */
    UChar rule1[50];
    UChar temp[20];
    UChar singleUChar[2]={0x00e4};
    const UChar test1[] =
    {
      /*character, count */
        0x61, 1,
        0x62, 1,
        0x65, 2
    };
    const UChar test2[] =
    {
     /*character, count */
        0x61, 1,
        0x62, 1,
        0x65, 2,
        0x66, 4
    };
   
    u_uastrcpy(rule1, "< a & ae = ");
    u_strcat(rule1, singleUChar);
    u_uastrcpy(temp, " < b < e");
    u_strcat(rule1, temp);
    verifyExpansion(rule1, test1, ARRAY_LENGTH(test1));
    
    /* Now a more complicated one:
        "a1" --> "ae"
        "z" --> "aeef" */
    
    u_uastrcpy(rule1, "");
    u_uastrcpy(rule1, "< a & ae = a1 & aeef = z < b < e < f");
    verifyExpansion(rule1, test2, ARRAY_LENGTH(test2));
}
/**
 * Verify that getMaxExpansion works on a given set of collation rules
 *
 * The first row of the "tests" array contains the collation rules
 * at index 0, and the string at index 1 is ignored.
 *
 * Subsequent rows of the array contain a character and a number, both
 * represented as strings.  The character's collation order is determined,
 * and getMaxExpansion is called for that character.  If its value is
 * not equal to the specified number, an error results.
 */
void verifyExpansion(UChar* rules, const UChar expansionTests[], int32_t testCount)
{
    int32_t i;
    UErrorCode status = U_ZERO_ERROR;
    UCollator *coll = NULL;
    UChar source[10];
    UCollationElements *iter=NULL;
    coll = ucol_openRules(rules, u_strlen(rules), UCOL_DEFAULT_NORMALIZATION, UCOL_DEFAULT_STRENGTH, &status);
    if (coll == NULL || U_FAILURE(status)) {
        log_err("Couldn't create a RuleBasedCollator. Error =%s \n", myErrorName(status));
        return;
    }
    u_uastrcpy(source, "");
    iter=ucol_openElements(coll, source, u_strlen(source), &status);
    if(U_FAILURE(status)){
        log_err("ERROR: in creation of collation element iterator using ucol_openElements()\n %s\n", 
            myErrorName(status));
        return;
    }
    for (i = 0; i < testCount; i += 2)
    {
        int32_t expansion, expect, order;
        /* First get the collation key that the test string expands to */
        UChar test[2] = { 0, 0} ;

	test[0] = expansionTests[i+0];
        
        ucol_setText(iter, test, u_strlen(test), &status);
        if (U_FAILURE(status)) {
            log_err("call to ucol_setText(iter, test, length) failed.");
            return;
        }
        order = ucol_next(iter, &status);

        if (U_FAILURE(status)) {
            log_err("call to iter->next() failed.");
            return;
        }
        
        /*if (order == UCOL_NULLORDER || ucol_next(iter, &status) != UCOL_NULLORDER) {
            ucol_reset(iter);
            log_err("verifyExpansion: \'%s\' has multiple orders\n", austrdup(test));
        }*/
        
        expansion = ucol_getMaxExpansion(iter, order);
        expect = expansionTests[i+1];
        
        if (expansion != expect) {
            log_err("Expansion for \'%s\' is wrong. Expected %d, Got %d\n", austrdup(test), expect, expansion); 
        }
    }
    ucol_closeElements(iter);
    ucol_close(coll);
}

/**
 * Return an integer array containing all of the collation orders
 * returned by calls to next on the specified iterator
 */
int32_t* getOrders(UCollationElements *iter, int32_t *orderLength)
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
        temp = (int32_t*)malloc(sizeof(int32_t) * size);
        
        memcpy(temp, orders, size * sizeof(int32_t));
        free(orders);
        orders = temp;
       

    }
/*     ucol_previous(iter, &status); */
    *orderLength = size;
    return orders;
}


void assertEqual(UCollationElements *i1, UCollationElements *i2)
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

