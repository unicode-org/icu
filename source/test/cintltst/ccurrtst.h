/********************************************************************
 * COPYRIGHT: 
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 ********************************************************************/
/********************************************************************************
*
* File CCURRTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Converted to C
*********************************************************************************
*/
/**
 * Collation currency tests.
 * (It's important to stay current!)
 */

#ifndef _CCURRCOLLTST
#define _CCURRCOLLTST

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "cintltst.h"

#define MAX_TOKEN_LEN 16
   /* Perform Collation Currency Test */   
    void currTest(void);

#endif /* #if !UCONFIG_NO_COLLATION */

#endif
