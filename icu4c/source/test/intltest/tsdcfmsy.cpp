/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/utypes.h"
#include "unicode/dcfmtsym.h"
#include "unicode/decimfmt.h"
#include "unicode/unum.h"
#include "tsdcfmsy.h"

void IntlTestDecimalFormatSymbols::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
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
    if(U_FAILURE(status)) {
        errln("ERROR: Couldn't create French DecimalFormatSymbols");
    }

    status = U_ZERO_ERROR;
    DecimalFormatSymbols en(Locale::ENGLISH, status);
    if(U_FAILURE(status)) {
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

    // test get/setSymbol()
    if((int) UNUM_FORMAT_SYMBOL_COUNT != (int) DecimalFormatSymbols::kFormatSymbolCount) {
        errln("unum.h and decimfmt.h have inconsistent numbers of format symbols!");
        return;
    }

    int i;
    for(i = 0; i < (int)DecimalFormatSymbols::kFormatSymbolCount; ++i) {
        foo.setSymbol((DecimalFormatSymbols::ENumberFormatSymbol)i, UnicodeString((UChar32)(0x10330 + i)));
    }
    for(i = 0; i < (int)DecimalFormatSymbols::kFormatSymbolCount; ++i) {
        if(foo.getSymbol((DecimalFormatSymbols::ENumberFormatSymbol)i) != UnicodeString((UChar32)(0x10330 + i))) {
            errln("get/setSymbol did not roundtrip, got " +
                  foo.getSymbol((DecimalFormatSymbols::ENumberFormatSymbol)i) +
                  ", expected " +
                  UnicodeString((UChar32)(0x10330 + i)));
        }
    }
   
   
    DecimalFormatSymbols sym(Locale::US, status);

    UnicodeString customDecSeperator("S");
    Verify(34.5, (UnicodeString)"00.00", sym, (UnicodeString)"34.50");
    sym.setSymbol((DecimalFormatSymbols::ENumberFormatSymbol)0, customDecSeperator);
    Verify(34.5, (UnicodeString)"00.00", sym, (UnicodeString)"34S50");
    sym.setSymbol((DecimalFormatSymbols::ENumberFormatSymbol)3, (UnicodeString)"P");
    Verify(34.5, (UnicodeString)"00 %", sym, (UnicodeString)"3450 P");
    sym.setSymbol((DecimalFormatSymbols::ENumberFormatSymbol)8, (UnicodeString)"D");
    Verify(34.5, CharsToUnicodeString("\\u00a4##.##"), sym, (UnicodeString)"D34.5");
    sym.setSymbol((DecimalFormatSymbols::ENumberFormatSymbol)1, (UnicodeString)"|");
    Verify(3456.5, (UnicodeString)"0,000.##", sym, (UnicodeString)"3|456S5");
    
}
void IntlTestDecimalFormatSymbols::Verify(double value, const UnicodeString& pattern, DecimalFormatSymbols sym, const UnicodeString& expected){
    UErrorCode status = U_ZERO_ERROR;
    DecimalFormat *df = new DecimalFormat(pattern, sym, status);
    if(U_FAILURE(status)){
        errln("ERROR: construction of decimal format failed");
    }
    UnicodeString buffer;
    FieldPosition pos(FieldPosition::DONT_CARE);
    buffer = df->format(value, buffer, pos);
    if(buffer != expected){
        errln((UnicodeString)"ERROR: format failed after setSymbols()\n Expected" + 
            expected + ", Got " + buffer);
    }
    delete df;
}
