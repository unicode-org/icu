/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/***********************************************************************
*   Date        Name        Description
*   01/03/2000  Madhu        Creation.
***********************************************************************/
/**
 * IntlTestTransliterator is the medium level test class for Transliterator 
 */

#include "unicode/utypes.h"
#include "ittrans.h"
#include "transtst.h"
#include "transapi.h"

void IntlTestTransliterator::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
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

		default: name=""; break;		
	}
}

