/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CRESTST.H
*
* Modification History:
*        Name              Date            Description            
*     Madhu Katragadda     05/09/200       Ported Tests for New ResourceBundle API
*********************************************************************************
*/
#ifndef _CRESTSTN
#define _CRESTSTN
/* C TEST FOR NEW RESOURCEBUNDLE API*/
#include "cintltst.h"




    void addNEWResourceBundleTest(TestNode**);

 /**
 *Perform several extensive tests using the subtest routine testTag
 */
    static void TestResourceBundles(void);
    /** 
     * Test construction of ResourceBundle accessing a custom test resource-file
     **/
    static void TestConstruction1(void);

    static void TestConstruction2(void);

    static void TestAliasConflict(void);

	static void TestFallback(void);
    /**
     * extensive subtests called by TestResourceBundles
     **/
     
   
    static bool_t testTag(const char* frag, bool_t in_Root, bool_t in_te, bool_t in_te_IN);

    static void record_pass(void);
    static void record_fail(void);

    
    static int32_t pass;
    static int32_t fail;

#endif
