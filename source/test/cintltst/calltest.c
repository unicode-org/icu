/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
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
void addFormatTest(TestNode** root);
void addConvert(TestNode** root);
void addCollTest(TestNode** root);
void addComplexTest(TestNode** root);

void addAllTests(TestNode** root)
{
    addUtility(root);
    addBreakIter(root);
    addFormatTest(root);
    addConvert(root);
    addCollTest(root);
    addComplexTest(root);
}
