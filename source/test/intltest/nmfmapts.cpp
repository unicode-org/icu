
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
#include "nmfmapts.h"

#include "numfmt.h"
#include "decimfmt.h"
#include "locid.h"

// This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
// try to test the full functionality.  It just calls each function in the class and
// verifies that it works on a basic level.

void IntlTestNumberFormatAPI::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite NumberFormatAPI");
    switch (index) {
        case 0: name = "NumberFormat API test"; 
                if (exec) {
                    logln("NumberFormat API test---"); logln("");
                    UErrorCode status = ZERO_ERROR;
                    Locale::setDefault(Locale::ENGLISH, status);
                    if(FAILURE(status)) {
                        errln("ERROR: Could not set default locale, test may not give correct results");
                    }
                    testAPI(par);
                }
                break;

        default: name = ""; break;
    }
}

/**
 * This test does round-trip testing (format -> parse -> format -> parse -> etc.) of
 * NumberFormat.
 */
void IntlTestNumberFormatAPI::testAPI(char *par)
{
    UErrorCode status = ZERO_ERROR;

// ======= Test constructors

    logln("Testing NumberFormat constructors");

    NumberFormat *def = NumberFormat::createInstance(status);
    if(FAILURE(status)) {
        errln("ERROR: Could not create NumberFormat (default)");
    }

    status = ZERO_ERROR;
    NumberFormat *fr = NumberFormat::createInstance(Locale::FRENCH, status);
    if(FAILURE(status)) {
        errln("ERROR: Could not create NumberFormat (French)");
    }

    NumberFormat *cur = NumberFormat::createCurrencyInstance(status);
    if(FAILURE(status)) {
        errln("ERROR: Could not create NumberFormat (currency, default)");
    }

    status = ZERO_ERROR;
    NumberFormat *cur_fr = NumberFormat::createCurrencyInstance(Locale::FRENCH, status);
    if(FAILURE(status)) {
        errln("ERROR: Could not create NumberFormat (currency, French)");
    }

    NumberFormat *per = NumberFormat::createPercentInstance(status);
    if(FAILURE(status)) {
        errln("ERROR: Could not create NumberFormat (percent, default)");
    }

    status = ZERO_ERROR;
    NumberFormat *per_fr = NumberFormat::createPercentInstance(Locale::FRENCH, status);
    if(FAILURE(status)) {
        errln("ERROR: Could not create NumberFormat (percent, French)");
    }

// ======= Test equality

    logln("Testing equality operator");
    
    if( *per_fr == *cur_fr || ! ( *per_fr != *cur_fr) ) {
        errln("ERROR: == failed");
    }

// ======= Test various format() methods

    logln("Testing various format() methods");

    double d = -10456.0037;
    int32_t l = 100000000;
    Formattable fD(d);
    Formattable fL(l);

    UnicodeString res1, res2, res3, res4, res5, res6;
    FieldPosition pos1(0), pos2(0), pos3(0), pos4(0);
    
    res1 = cur_fr->format(d, res1);
    logln( (UnicodeString) "" + (int32_t) d + " formatted to " + res1);

    res2 = cur_fr->format(l, res2);
    logln((UnicodeString) "" + (int32_t) l + " formatted to " + res2);

    res3 = cur_fr->format(d, res3, pos1);
    logln( (UnicodeString) "" + (int32_t) d + " formatted to " + res3);

    res4 = cur_fr->format(l, res4, pos2);
    logln((UnicodeString) "" + (int32_t) l + " formatted to " + res4);

    status = ZERO_ERROR;
    res5 = cur_fr->format(fD, res5, pos3, status);
    if(FAILURE(status)) {
        errln("ERROR: format(Formattable [double]) failed");
    }
    logln((UnicodeString) "" + (int32_t) fD.getDouble() + " formatted to " + res5);

    status = ZERO_ERROR;
    res6 = cur_fr->format(fL, res6, pos4, status);
    if(FAILURE(status)) {
        errln("ERROR: format(Formattable [long]) failed");
    }
    logln((UnicodeString) "" + fL.getLong() + " formatted to " + res6);


// ======= Test parse()

    logln("Testing parse()");

    UnicodeString text("-10,456.0037");
    Formattable result1, result2, result3;
    ParsePosition pos(0), pos01(0);
    fr->parseObject(text, result1, pos);
    if(result1.getType() != Formattable::kDouble && result1.getDouble() != d) {
        errln("ERROR: Roundtrip failed (via parse()) for " + text);
    }
    logln(text + " parsed into " + (int32_t) result1.getDouble());

    fr->parse(text, result2, pos01);
    if(result2.getType() != Formattable::kDouble && result2.getDouble() != d) {
        errln("ERROR: Roundtrip failed (via parse()) for " + text);
    }
    logln(text + " parsed into " + (int32_t) result2.getDouble());

    status = ZERO_ERROR;
    fr->parse(text, result3, status);
    if(FAILURE(status)) {
        errln("ERROR: parse() failed");
    }
    if(result3.getType() != Formattable::kDouble && result3.getDouble() != d) {
        errln("ERROR: Roundtrip failed (via parse()) for " + text);
    }
    logln(text + " parsed into " + (int32_t) result3.getDouble());


// ======= Test getters and setters

    logln("Testing getters and setters");

    int32_t count = 0;
    const Locale *locales = NumberFormat::getAvailableLocales(count);
    logln((UnicodeString) "Got " + count + " locales" );
    for(int32_t i = 0; i < count; i++) {
        UnicodeString name;
        name = locales[i].getName(name);
        logln(name);
    }

    fr->setParseIntegerOnly( def->isParseIntegerOnly() );
    if(fr->isParseIntegerOnly() != def->isParseIntegerOnly() ) {
        errln("ERROR: setParseIntegerOnly() failed");
    }

    fr->setGroupingUsed( def->isGroupingUsed() );
    if(fr->isGroupingUsed() != def->isGroupingUsed() ) {
        errln("ERROR: setGroupingUsed() failed");
    }

    fr->setMaximumIntegerDigits( def->getMaximumIntegerDigits() );
    if(fr->getMaximumIntegerDigits() != def->getMaximumIntegerDigits() ) {
        errln("ERROR: setMaximumIntegerDigits() failed");
    }

    fr->setMinimumIntegerDigits( def->getMinimumIntegerDigits() );
    if(fr->getMinimumIntegerDigits() != def->getMinimumIntegerDigits() ) {
        errln("ERROR: setMinimumIntegerDigits() failed");
    }

    fr->setMaximumFractionDigits( def->getMaximumFractionDigits() );
    if(fr->getMaximumFractionDigits() != def->getMaximumFractionDigits() ) {
        errln("ERROR: setMaximumFractionDigits() failed");
    }

    fr->setMinimumFractionDigits( def->getMinimumFractionDigits() );
    if(fr->getMinimumFractionDigits() != def->getMinimumFractionDigits() ) {
        errln("ERROR: setMinimumFractionDigits() failed");
    }


// ======= Test getStaticClassID()

    logln("Testing getStaticClassID()");

    status = ZERO_ERROR;
    NumberFormat *test = new DecimalFormat(status);
    if(FAILURE(status)) {
        errln("ERROR: Couldn't create a NumberFormat");
    }

    if(test->getDynamicClassID() != DecimalFormat::getStaticClassID()) {
        errln("ERROR: getDynamicClassID() didn't return the expected value");
    }

    delete test;
    delete def;
    delete fr;
    delete cur;
    delete cur_fr;
    delete per;
    delete per_fr;
}
