/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CUTILTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/
#include "cintltst.h"

void addLocaleTest(TestNode**);
void addUnicodeTest(TestNode**);
void addResourceBundleTest(TestNode**);
void addNEWResourceBundleTest(TestNode**);
void addSCSUTest(TestNode** root);
void addHashtableTest(TestNode** root);
void addCStringTest(TestNode** root);
void addMemoryStreamTest(TestNode** root);

void addUtility(TestNode** root)
{
    addLocaleTest(root);
    addUnicodeTest(root);
    addResourceBundleTest(root);
	addNEWResourceBundleTest(root);
    addSCSUTest(root);
    addHashtableTest(root);
    addCStringTest(root);
    addMemoryStreamTest(root);
}

