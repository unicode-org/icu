/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CBKITTST.C
*
* Modification History:
*        Name                      Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/
#include "cintltst.h"

void addBrkIterAPITest(TestNode**);
void addBrkIterRegrTest(TestNode**);

void addBreakIter(TestNode** root)
{
    addBrkIterAPITest(root);
    addBrkIterRegrTest(root);
}
