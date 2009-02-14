/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2008, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File UCNVSELTST.H
*
* Modification History:
*        Name                     Description
*     Mohamed Eldawy               Creation
*********************************************************************************/
/* C API TEST FOR CONVERTER SELECTOR */
#ifndef _UCNVSELTST
#define _UCNVSELTST

#include "unicode/utypes.h"
#include "cintltst.h"

    /**
     * The function used to test selection for UTF8 strings
     **/
    static void TestConversionUTF8(void);
    /**
     * The function used to test selection for UTF16 strings
     **/
    static void TestConversionUTF16(void);
    /**
     * The function used to test serialization and unserialization 
     **/
    static void TestSerializationAndUnserialization(void);
#endif
