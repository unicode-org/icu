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
