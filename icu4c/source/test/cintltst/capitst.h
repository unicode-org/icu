/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
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
    /**
     * Test ucol_GetDefaultRules ()
     **/
    void TestGetDefaultRules(void);

    
 
    

#endif
