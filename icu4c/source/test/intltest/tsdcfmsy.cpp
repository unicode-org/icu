
/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright Taligent, Inc., 1997
* (C) Copyright International Business Machines Corporation, 1997 - 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/

#include "utypes.h"
#include "tsdcfmsy.h"

#include "dcfmtsym.h"


void IntlTestDecimalFormatSymbols::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite DecimalFormatSymbols");
    switch (index) {
        case 0: name = "DecimalFormatSymbols test"; 
                if (exec) {
                    logln("DecimalFormatSymbols test---"); logln("");
                    testSymbols(par);
                }
                break;

        default: name = ""; break;
    }
}

/**
 * Test the API of DecimalFormatSymbols; primarily a simple get/set set.
 */
void IntlTestDecimalFormatSymbols::testSymbols(char *par)
{
    UErrorCode status = U_ZERO_ERROR;

    DecimalFormatSymbols fr(Locale::FRENCH, status);
    if(FAILURE(status)) {
        errln("ERROR: Couldn't create French DecimalFormatSymbols");
    }

    status = U_ZERO_ERROR;
    DecimalFormatSymbols en(Locale::ENGLISH, status);
    if(FAILURE(status)) {
        errln("ERROR: Couldn't create English DecimalFormatSymbols");
    }

    if(en == fr || ! (en != fr) ) {
        errln("ERROR: English DecimalFormatSymbols equal to French");
    }

    // just do some VERY basic tests to make sure that get/set work

    UChar zero = en.getZeroDigit();
    fr.setZeroDigit(zero);
    if(fr.getZeroDigit() != en.getZeroDigit()) {
        errln("ERROR: get/set ZeroDigit failed");
    }

    UChar group = en.getGroupingSeparator();
    fr.setGroupingSeparator(group);
    if(fr.getGroupingSeparator() != en.getGroupingSeparator()) {
        errln("ERROR: get/set GroupingSeparator failed");
    }

    UChar decimal = en.getDecimalSeparator();
    fr.setDecimalSeparator(decimal);
    if(fr.getDecimalSeparator() != en.getDecimalSeparator()) {
        errln("ERROR: get/set DecimalSeparator failed");
    }

    UChar perMill = en.getPerMill();
    fr.setPerMill(perMill);
    if(fr.getPerMill() != en.getPerMill()) {
        errln("ERROR: get/set PerMill failed");
    }

    UChar percent = en.getPercent();
    fr.setPercent(percent);
    if(fr.getPercent() != en.getPercent()) {
        errln("ERROR: get/set Percent failed");
    }

    UChar digit = en.getDigit();
    fr.setDigit(digit);
    if(fr.getPercent() != en.getPercent()) {
        errln("ERROR: get/set Percent failed");
    }

    UChar patternSeparator = en.getPatternSeparator();
    fr.setPatternSeparator(patternSeparator);
    if(fr.getPatternSeparator() != en.getPatternSeparator()) {
        errln("ERROR: get/set PatternSeparator failed");
    }

    UnicodeString infinity;
    infinity = en.getInfinity(infinity);
    fr.setInfinity(infinity);
    UnicodeString infinity2;
    infinity2 = fr.getInfinity(infinity2);
    if(infinity != infinity2) {
        errln("ERROR: get/set Infinity failed");
    }

    UnicodeString nan;
    nan = en.getNaN(infinity);
    fr.setNaN(nan);
    UnicodeString nan2;
    nan2 = fr.getNaN(nan2);
    if(nan != nan2) {
        errln("ERROR: get/set NaN failed");
    }

    UChar minusSign = en.getMinusSign();
    fr.setMinusSign(minusSign);
    if(fr.getMinusSign() != en.getMinusSign()) {
        errln("ERROR: get/set MinusSign failed");
    }
 
    UChar exponential = en.getExponentialSymbol();
    fr.setExponentialSymbol(exponential);
    if(fr.getExponentialSymbol() != en.getExponentialSymbol()) {
        errln("ERROR: get/set Exponential failed");
    }

    status = U_ZERO_ERROR;
    DecimalFormatSymbols foo(status);
    
    DecimalFormatSymbols bar(foo);

    en = fr;

    if(en != fr || foo != bar) {
        errln("ERROR: Copy Constructor or Assignment failed");
    }
}
