/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

/**
 * IntlTestConvert is the medium level test class for everything in the directory "utility".
 */

#include "utypes.h"
#include "itconv.h"
#include "cppcnvt.h"

void IntlTestConvert::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite Convert: ");
    switch (index) {
        case 0:
            name = "ConvertTest"; 
            if (exec) {
                logln("ConvertTest---"); logln("");
                ConvertTest test;
                callTest( test, par );
            }
            break;

        default: name = ""; break; //needed to end loop
    }
}

