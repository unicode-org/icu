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
* File CITERTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*********************************************************************************

/**
 * Collation Iterator tests.
 * (Let me reiterate my position...)
 */

#ifndef _CITERCOLLTST
#define _CITERCOLLTST

#include "cintltst.h"
#include "utypes.h"
#include "ucol.h"
struct ExpansionRecord
    {
        UChar character;
        int32_t count;
    };
typedef struct ExpansionRecord ExpansionRecord;
#define MAX_TOKEN_LEN 128
   
       /**
     * Test for CollationElementIterator.previous()
     *
     * @bug 4108758 - Make sure it works with contracting characters
     * 
     */
    void TestPrevious(void);
    
    /**
     * Test for getOffset() and setOffset()
     */
    void TestOffset(void);
    /**
     * Test for setText()
     */
    void TestSetText(void);
    
       
    
    /*------------------------------------------------------------------------
     Internal utilities
     */

    static void backAndForth(UCollationElements* iter);

    
      
    /**
     * Return an integer array containing all of the collation orders
     * returned by calls to next on the specified iterator
     */
    static int32_t* getOrders(UCollationElements* iter, int32_t *orderLength);

    
    static void assertEqual(UCollationElements *i1, UCollationElements *i2);

    
    static  UChar *test1;
    static  UChar *test2;



#endif
