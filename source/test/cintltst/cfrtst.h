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
* File CFRTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*********************************************************************************
/**
 * CollationFrenchTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#ifndef _CFRCOLLTST
#define _CFRCOLLTST


#include "cintltst.h"

#define MAX_TOKEN_LEN 128

   
    /*main test routine, tests comparisons for a set of strings against sets of expected results*/
    static void doTest(UCollator*, const UChar* source, const UChar* target, UCollationResult result);

    /* performs Extra tests*/
   static void TestExtra(void);

    /* perform test with strength SECONDARY*/
 static   void TestSecondary(void);

    /*perform test with strength TERTIARY*/
static    void TestTertiary(void);


   

#endif
