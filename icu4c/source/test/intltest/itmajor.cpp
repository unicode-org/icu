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
 * MajorTestLevel is the top level test class for everything in the directory "IntlWork".
 */

#include "unicode/utypes.h"
#include "itmajor.h"

#include "itutil.h"
#include "tscoll.h"
#include "ittxtbd.h"
#include "itformat.h"
#include "itconv.h"
#include "transtst.h"


void MajorTestLevel::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    switch (index) {
        case 0: name = "utility"; 
                if (exec) { 
                    logln("TestSuite Utilities---"); logln();
                    IntlTestUtilities test;
                    callTest( test, par );
                }
                break;

        case 1: name = "collate"; 
                if (exec) {
                    logln("TestSuite Collator----"); logln();
                    IntlTestCollator test;
                    callTest( test, par );
                }
                break;

        case 2: name = "textbounds"; 
                if (exec) {
                    logln("TestSuite TextBoundary----"); logln();
                    IntlTestTextBoundary test;
                    callTest( test, par );
                }
                break;

        case 3: name = "format"; 
                if (exec) {
                    logln("TestSuite Format----"); logln();
                    IntlTestFormat test;
                    callTest( test, par );
                }
                break;

        case 4: name = "convert"; 
                if (exec) {
                    logln("TestSuite Convert----"); logln();
                    IntlTestConvert test;
                    callTest( test, par );
                }
                break;

        case 5: name = "translit"; 
                if (exec) {
                    logln("TestSuite Transliterator----"); logln();
                    TransliteratorTest test;
                    callTest( test, par );
                }
                break;


        default: name = ""; break;
    }
}

