/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright International Business Machines Corporation,  2000                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/
/***********************************************************************
*   Date        Name        Description
*   01/03/2000  Madhu        Creation.
*   03/2000     Madhu        Added additional tests
***********************************************************************/
/**
 * IntlTestTransliterator is the medium level test class for Transliterator 
 */

#include "unicode/utypes.h"
#include "ittrans.h"
#include "transtst.h"
#include "transapi.h"
#include "cpdtrtst.h"
#include "unhxtrts.h"
#include "hxuntrts.h"
#include "jahatrts.h"
#include "hajatrts.h"
#include "ufltlgts.h"

void IntlTestTransliterator::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
{
    if (exec) logln("TestSuite Transliterator");
    switch (index) {
        case 0:
            name = "TransliteratorTest"; 
            if (exec) {
                logln("TransliteratorTest--"); logln("");
                TransliteratorTest test;
                callTest( test, par );
            }
            break;

        case 1:
            name = "TransliteratorAPITest"; 
            if (exec) {
                logln("TransliteratorAPITest---"); logln("");
                TransliteratorAPITest test;
                callTest( test, par );
			}
            break;
	    case 2:
            name = "CompoundTransliteratorTest"; 
            if (exec) {
                logln("CompoundTransliteratorTest---"); logln("");
                CompoundTransliteratorTest test;
                callTest( test, par );
			}
            break;
		case 3:
            name = "UniToHexTransliteratorTest"; 
            if (exec) {
                logln("UnicodeToHexadecimalTransliteratorTest---"); logln("");
                UniToHexTransliteratorTest test;
                callTest( test, par );
			}
            break;
		case 4:
            name = "HexToUniTransliteratorTest"; 
            if (exec) {
                logln("HexadecimalToUnicodeTransliteratorTest---"); logln("");
                HexToUniTransliteratorTest test;
                callTest( test, par );
			}
            break;
		case 5:
            name = "JamoToHangTransliteratorTest"; 
            if (exec) {
                logln("JamoToHangulTransliteratorTest---"); logln("");
                JamoToHangTransliteratorTest test;
                callTest( test, par );
			}
            break;
		case 6:
            name = "HangToJamoTransliteratorTest"; 
            if (exec) {
                logln("HangulToJamoTransliteratorTest---"); logln("");
                HangToJamoTransliteratorTest test;
                callTest( test, par );
			}
            break;
		case 7:
            name = "UnicodeFilterLogicTest"; 
            if (exec) {
                logln("UnicodeFilterLogicTest---"); logln("");
                UnicodeFilterLogicTest test;
                callTest( test, par );
			}
            break;


		default: name=""; break;		
	}
}

