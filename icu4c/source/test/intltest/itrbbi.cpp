/*
**********************************************************************
* Copyright (C) 1998-2000, International Business Machines Corporation 
* and others.  All Rights Reserved.
**********************************************************************
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

void IntlTestRBBI::runIndexedTest( int32_t index, UBool exec, const char* &name, char* par )
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

