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
* File CAPITEST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*********************************************************************************
*//* C API TEST For COLLATOR */

#ifndef _CCOLLAPITST
#define _CCOLLAPITST


#include "cintltst.h"
#define   MAX_TOKEN_LEN  128  


    /**
     * error reporting utility method
     **/

    static void doAssert(int condition, const char *message);
    /**
     * Collator Class Properties
     * ctor, dtor, createInstance, compare, getStrength/setStrength
     * getDecomposition/setDecomposition, getDisplayName
     */
    void TestProperty(void);
    /**
     * Test RuleBasedCollator and getRules
     **/
    void TestRuleBasedColl(void);
    
    /**
     * Test compare
     **/
    void TestCompare(void);
    /**
     * Test hashCode functionality
     **/
    void TestHashCode(void);
    /**
     * Tests the constructor and numerous other methods for CollationKey
     **/
   void TestSortKey(void);
    /**
     * test the CollationElementIterator methods
     **/
   void TestElemIter(void);
    /**
     * Test ucol_getAvailable and ucol_countAvailable()
     **/
    void TestGetAll(void);

    
 
    

#endif
