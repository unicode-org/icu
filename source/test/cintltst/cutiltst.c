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
void addSCSUTest(TestNode** root);

void addUtility(TestNode** root)
{
    addLocaleTest(root);
    addUnicodeTest(root);
    addResourceBundleTest(root);
    addSCSUTest(root);

}

