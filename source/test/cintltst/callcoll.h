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
* File CALLCOLL.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda              Ported to C
*********************************************************************************
*/
/**
 * CollationDummyTest is a third level test class.  This tests creation of 
 * a customized collator object.  For example, number 1 to be sorted 
 * equlivalent to word 'one'.
 */
#ifndef _CALLCOLLTST
#define _CALLCOLLTST

#include "cintltst.h"



    /* static constants */
#define MAX_TOKEN_LEN 128
      
    /* tests comparison of custom collation with different strengths */
static    void doTest(UCollator*, const UChar* source, const UChar* target, UCollationResult result);

    /* perform test with strength PRIMARY */
static    void TestPrimary(void);

    /* perform test with strength SECONDARY */
static void TestSecondary(void);

    /* perform test with strength tertiary */
    static void TestTertiary(void);

    /*perform tests with strength Identical */
static    void TestIdentical(void);

    /* perform extra tests */
    static void TestExtra(void);


   



#endif
