/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/***************************************************************************
*
* File CRESTST.H
*
* Modification History:
*        Name               Date               Description
*   Madhu Katragadda    05/09/2000   Ported Tests for New ResourceBundle API
*   Madhu Katragadda    05/24/2000   Added new tests to test RES_BINARY for collationElements
*************************************************************************************************
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

static void TestAliasConflict(void);

static void TestFallback(void);

static void TestBinaryCollationData(void);

static void TestNewTypes(void);

static void TestEmptyTypes(void);

static void TestAPI(void);

static void TestErrorConditions(void);

static void TestGetVersion(void);

static void TestGetVersionColl(void);

static void TestEmptyBundle(void);

static void TestDirectAccess(void);

static void TestResourceLevelAliasing(void);

static void TestErrorCodes(void);

/**
* extensive subtests called by TestResourceBundles
**/
static UBool testTag(const char* frag, UBool in_Root, UBool in_te, UBool in_te_IN);

static void record_pass(void);
static void record_fail(void);


#endif
