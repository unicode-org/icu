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
* File CDATTST.H
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda               Creation
*********************************************************************************
*/
/* C API TEST FOR DATE FORMAT */
#ifndef _CDATFRMTST
#define _CDATFRMTST

#include "cintltst.h"

    /**
     * The function used to test the Date format API
     **/
    static void TestDateFormat(void);

    /**
     * The function used to test API  udat_getSymbols(), udat_setSymbols() and udat_countSymbols()
     **/
    static void TestSymbols(void);

    /**
     * test subroutines used by TestSymbols
     **/
    void VerifygetSymbols(UDateFormat*, UDateFormatSymbolType, int32_t, const char*);
    void VerifysetSymbols(UDateFormat*, UDateFormatSymbolType, int32_t, const char*);
    void VerifygetsetSymbols(UDateFormat*, UDateFormat*, UDateFormatSymbolType, int32_t);
    
    /**
     * test subroutine used by the testing functions
     **/
    UChar* myNumformat(const UNumberFormat* numfor, double d);

#endif
