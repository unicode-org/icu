/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "intltest.h"

/**
 * Tests for class ResourceBundle
 **/
class ResourceBundleTest: public IntlTest {
public:
    ResourceBundleTest();
    ~ResourceBundleTest();
    
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par = NULL );

    /** 
     * Perform several extensive tests using the subtest routine testTag
     **/
    void TestResourceBundles(void);
    /** 
     * Test construction of ResourceBundle accessing a custom test resource-file
     **/
    void TestConstruction(void);

private:
    /**
     * extensive subtests called by TestResourceBundles
     **/
    UBool testTag(const char* frag, UBool in_Root, UBool in_te, UBool in_te_IN);

    void record_pass(void);
    void record_fail(void);

    int32_t pass;
    int32_t fail;

    IntlTest& OUT;
};

