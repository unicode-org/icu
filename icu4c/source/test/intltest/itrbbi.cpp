/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1999-2000               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/
/***********************************************************************
*   Date        Name        Description
*   12/14/99    Madhu        Creation.
***********************************************************************/
/**
 * IntlTestRBBI is the medium level test class for RuleBasedBreakIterator
 */

#include "unicode/utypes.h"
#include "itrbbi.h"
#include "rbbiapts.h"
#include "rbbitst.h"

void IntlTestRBBI::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite RuleBasedBreakIterator: ");
    switch (index) {
        case 0:
            name = "RBBIAPITest"; 
            if (exec) {
                logln("RBBIAPITest--"); logln("");
                RBBIAPITest test;
                callTest( test, par );
            }
            break;

        case 1:
           name = "RBBITest"; 
            if (exec) {
                logln("RBBITest---"); logln("");
                RBBITest test;
                callTest( test, par );
            }
            break;
        default: name=""; break;
	}
}

