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
* File CNORMTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
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

    /*internal functions*/
    
    static void assertEqual(const UChar* result,  const UChar* expected, int32_t index);


    


#endif
