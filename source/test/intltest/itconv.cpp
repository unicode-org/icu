/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/


/**
 * IntlTestConvert is the medium level test class for everything in the directory "utility".
 */

#include "unicode/utypes.h"
#include "itconv.h"
#include "cppcnvt.h"

void IntlTestConvert::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite Convert: ");
    switch (index) {
        case 0:
            name = "TestConvert"; 
            if (exec) {
                logln("TestConvert---"); logln("");
                ConvertTest test;
                callTest( test, par );
            }
            break;

        case 1:
            name = "TestAmbiguous";
            if (exec) {
                logln("TestAmbiguous---"); logln("");
                ConvertTest test;
                callTest( test, par );
            }
            break;

        default: name = ""; break; //needed to end loop
    }
}

