/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
********************************************************************************
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
UCollator *en_us;


void addCollIterTest(TestNode** root)
{
    
    
    addTest(root, &TestPrevious, "tscoll/citertst/TestPrevious");
    addTest(root, &TestOffset, "tscoll/citertst/TestOffset");
    addTest(root, &TestSetText, "tscoll/citertst/TestSetText");
    
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

