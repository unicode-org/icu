/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CCONVTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/
#include "cintltst.h"

void addTestConvert(TestNode**);
void addTestNewConvert(TestNode**);
void addTestConvertErrorCallBack(TestNode** root);
void addTestEuroRegression(TestNode** root);
void addTestConverterFallBack(TestNode** root);

void addConvert(TestNode** root)
{    
    addTestConvert(root);
	addTestNewConvert(root);
	addTestConvertErrorCallBack(root);
    addTestEuroRegression(root);
    addTestConverterFallBack(root);
}
