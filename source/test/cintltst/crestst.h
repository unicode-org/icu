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
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*********************************************************************************
*/
#ifndef _CRESTST
#define _CRESTST
/* C API TEST FOR RESOURCEBUNDLE */
#include "cintltst.h"




    void addTestResourceBundleTest(TestNode**);

 /**
 *Perform several extensive tests using the subtest routine testTag
 */
    void TestResourceBundles(void);
    /** 
     * Test construction of ResourceBundle accessing a custom test resource-file
     **/
    void TestConstruction1(void);

    void TestConstruction2(void);
    /**
     * extensive subtests called by TestResourceBundles
     **/
     
    void setUpDataTable(void);
    bool_t testTag(const char* frag, bool_t in_Default, bool_t in_te, bool_t in_te_IN);

    void record_pass(void);
    void record_fail(void);

    
    int32_t pass;
    int32_t fail;

#endif
