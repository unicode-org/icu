/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CNORMTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*     synwee                      added test for quick check
*********************************************************************************
*/
#ifndef _NORMTST
#define _NORMTST
/**
 *  tests for u_normalization
 */

#include "cintltst.h"


    
    void TestDecomp(void);
    void TestCompatDecomp(void);
    void TestCanonDecompCompose(void);
    void TestCompatDecompCompose(void);
    void TestNull(void);
    void TestQuickCheck(void);

    /*internal functions*/
    
/*    static void assertEqual(const UChar* result,  const UChar* expected, int32_t index);
*/
    static void assertEqual(const UChar* result,  const char* expected, int32_t index);


    


#endif
