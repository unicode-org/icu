/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CESTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*********************************************************************************
/**
 * CollationSpanishTest is a third level test class.  This tests the locale
 * specific primary, secondary and tertiary rules.  For example, the ignorable
 * character '-' in string "black-bird".  The en_US locale uses the default
 * collation rules as its sorting sequence.
 */

#ifndef _CESCOLLTST
#define _CESCOLLTST


#include "cintltst.h"


#define MAX_TOKEN_LEN 128
   
    /* main test routine, tests comparisons for a set of strings against sets of expected results*/
static  void doTest(UCollator*, const UChar* source, const UChar* target, UCollationResult result);

      /* perform test with strength SECONDARY */
 static   void TestPrimary(void);

    /* perform test with strength TERTIARY */
 static    void TestTertiary(void);


    

#endif
