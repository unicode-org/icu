/********************************************************************
 * COPYRIGHT: 
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
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

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "cintltst.h"

void addBrkIterAPITest(TestNode**);

void addBreakIter(TestNode** root);

void addBreakIter(TestNode** root)
{
    addBrkIterAPITest(root);
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
