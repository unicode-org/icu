/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CG7COLL.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*********************************************************************************
/**
 * G7CollationTest is a third level test class.  This test performs the examples 
 * mentioned on the Taligent international demos web site.  
 * Sample Rules: & Z < p , P 
 * Effect :  Making P sort after Z.
 *
 * Sample Rules: & c < ch , cH, Ch, CH 
 * Effect : As well as adding sequences of characters that act as a single character (this is
 * known as contraction), you can also add characters that act like a sequence of
 * characters (this is known as expansion).  
 * 
 * Sample Rules: & Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&' 
 * Effect : Expansion and contraction can actually be combined.  
 * 
 * Sample Rules: & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-'
 * Effect : sorted sequence as the following,
 * aardvark  
 * a-rdvark  
 * abbot  
 * coop  
 * co-p  
 * cop 
 */

#ifndef _CG7COLLTST
#define _CG7COLLTST

#include "cintltst.h"

#define MAX_TOKEN_LEN 128
#define  TESTLOCALES  12 
#define  FIXEDTESTSET 15 
#define  TOTALTESTSET  30 

        
    /* main test routine, tests comparisons for a set of strings against sets of expected results */
   static  void doTest(UCollator *myCollation, const UChar* source, const UChar* target, UCollationResult result);
    
    /* perform test for G7 locales */
    void TestG7Locales(void);

    /* perform test with added rules " & Z < p, P" */
    void TestDemo1(void);

    /* perorm test with added rules "& C < ch , cH, Ch, CH" */
    void TestDemo2(void);

    /* perform test with added rules  */
    /* "& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&'" */
    void TestDemo3(void);

    /* perform test with added rules  */
    /* " & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' " */
    void TestDemo4(void);

#endif
