/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CALLTEST.C
*
* Modification History:
*   Creation:   Madhu Katragadda 
*********************************************************************************
*/
/* THE FILE WHERE ALL C API TESTS ARE ADDED TO THE ROOT */


#include "cintltst.h"

void addUtility(TestNode** root);
void addBreakIter(TestNode** root);
void addStandardNamesTest(TestNode **root);
void addFormatTest(TestNode** root);
void addConvert(TestNode** root);
void addCollTest(TestNode** root);
void addComplexTest(TestNode** root);
void addUDataTest(TestNode** root);
void addUTF16Test(TestNode** root);
void addUTF8Test(TestNode** root);
void addUTransTest(TestNode** root);
void addPUtilTest(TestNode** root);
void addCompactArrayTest(TestNode** root);
void addTestDeprecatedAPI(TestNode** root);
void addUCharTransformTest(TestNode** root);
void addUSetTest(TestNode** root);
void addUStringPrepTest(TestNode** root);
void addIDNATest(TestNode** root);
void addHeapMutexTest(TestNode **root);
void addUTraceTest(TestNode** root);
void addURegexTest(TestNode** root);

void addAllTests(TestNode** root)
{
    addUDataTest(root);
    addPUtilTest(root);
    addUTF16Test(root);
    addUTF8Test(root);
    addUtility(root);
    addConvert(root);
    addUCharTransformTest(root);
    addStandardNamesTest(root);
    addCompactArrayTest(root);
#if !UCONFIG_NO_FORMATTING
    addFormatTest(root);
#endif
#if !UCONFIG_NO_BREAK_ITERATION
    addBreakIter(root);
#endif
#if !UCONFIG_NO_COLLATION
    addCollTest(root);
#endif
    addComplexTest(root);
#if !UCONFIG_NO_TRANSLITERATION
    addUTransTest(root);
#endif
    addUSetTest(root);
    addTestDeprecatedAPI(root);
#if !UCONFIG_NO_IDNA
    addUStringPrepTest(root);
    addIDNATest(root);
#endif
    addHeapMutexTest(root);
    addUTraceTest(root);
#if !UCONFIG_NO_REGULAR_EXPRESSIONS
    addURegexTest(root);
#endif
}

