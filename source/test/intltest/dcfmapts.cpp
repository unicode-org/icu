
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
#include "dcfmapts.h"

#include "decimfmt.h"
#include "dcfmtsym.h"

// This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
// try to test the full functionality.  It just calls each function in the class and
// verifies that it works on a basic level.

void IntlTestDecimalFormatAPI::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln((UnicodeString)"TestSuite DecimalFormatAPI");
    switch (index) {
        case 0: name = "DecimalFormat API test"; 
                if (exec) {
                    logln((UnicodeString)"DecimalFormat API test---"); logln((UnicodeString)"");
                    UErrorCode status = U_ZERO_ERROR;
                    Locale::setDefault(Locale::ENGLISH, status);
                    if(FAILURE(status)) {
                        errln((UnicodeString)"ERROR: Could not set default locale, test may not give correct results");
                    }
                    testAPI(par);
                }
                break;

        default: name = ""; break;
    }
}

/**
 * This test checks various generic API methods in DecimalFormat to achieve 100%
 * API coverage.
 */
void IntlTestDecimalFormatAPI::testAPI(char *par)
{
    UErrorCode status = U_ZERO_ERROR;

// ======= Test constructors

    logln((UnicodeString)"Testing DecimalFormat constructors");

    DecimalFormat def(status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: Could not create DecimalFormat (default)");
    }

    status = U_ZERO_ERROR;
    const UnicodeString pattern("#,##0.# FF");
    DecimalFormat pat(pattern, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: Could not create DecimalFormat (pattern)");
    }

    status = U_ZERO_ERROR;
    DecimalFormatSymbols *symbols = new DecimalFormatSymbols(Locale::FRENCH, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: Could not create DecimalFormatSymbols (French)");
    }

    status = U_ZERO_ERROR;
    DecimalFormat cust1(pattern, symbols, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: Could not create DecimalFormat (pattern, symbols*)");
    }

    status = U_ZERO_ERROR;
    DecimalFormat cust2(pattern, *symbols, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: Could not create DecimalFormat (pattern, symbols)");
    }

    DecimalFormat copy(pat);

// ======= Test clone(), assignment, and equality

    logln((UnicodeString)"Testing clone(), assignment and equality operators");

    if( ! (copy == pat) || copy != pat) {
        errln((UnicodeString)"ERROR: Copy constructor or == failed");
    }

    copy = cust1;
    if(copy != cust1) {
        errln((UnicodeString)"ERROR: Assignment (or !=) failed");
    }

    Format *clone = def.clone();
    if( ! (*clone == def) ) {
        errln((UnicodeString)"ERROR: Clone() failed");
    }
    delete clone;

// ======= Test various format() methods

    logln((UnicodeString)"Testing various format() methods");

    double d = -10456.0037;
    int32_t l = 100000000;
    Formattable fD(d);
    Formattable fL(l);

    UnicodeString res1, res2, res3, res4;
    FieldPosition pos1(0), pos2(0), pos3(0), pos4(0);
    
    res1 = def.format(d, res1, pos1);
    logln( (UnicodeString) "" + (int32_t) d + " formatted to " + res1);

    res2 = pat.format(l, res2, pos2);
    logln((UnicodeString) "" + (int32_t) l + " formatted to " + res2);

    status = U_ZERO_ERROR;
    res3 = cust1.format(fD, res3, pos3, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: format(Formattable [double]) failed");
    }
    logln((UnicodeString) "" + (int32_t) fD.getDouble() + " formatted to " + res3);

    status = U_ZERO_ERROR;
    res4 = cust2.format(fL, res4, pos4, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: format(Formattable [long]) failed");
    }
    logln((UnicodeString) "" + fL.getLong() + " formatted to " + res4);

// ======= Test parse()

    logln((UnicodeString)"Testing parse()");

    UnicodeString text("-10,456.0037");
    Formattable result1, result2;
    ParsePosition pos(0);
    UnicodeString patt("#,##0.#");
    status = U_ZERO_ERROR;
    pat.applyPattern(patt, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: applyPattern() failed");
    }
    pat.parse(text, result1, pos);
    if(result1.getType() != Formattable::kDouble && result1.getDouble() != d) {
        errln((UnicodeString)"ERROR: Roundtrip failed (via parse()) for " + text);
    }
    logln(text + " parsed into " + (int32_t) result1.getDouble());

    status = U_ZERO_ERROR;
    pat.parse(text, result2, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: parse() failed");
    }
    if(result2.getType() != Formattable::kDouble && result2.getDouble() != d) {
        errln((UnicodeString)"ERROR: Roundtrip failed (via parse()) for " + text);
    }
    logln(text + " parsed into " + (int32_t) result2.getDouble());

// ======= Test getters and setters

    logln((UnicodeString)"Testing getters and setters");

    const DecimalFormatSymbols *syms = pat.getDecimalFormatSymbols();
    DecimalFormatSymbols *newSyms = new DecimalFormatSymbols(*syms);
    def.adoptDecimalFormatSymbols(newSyms); 
    def.setDecimalFormatSymbols(*newSyms);
    if( *(pat.getDecimalFormatSymbols()) != *(def.getDecimalFormatSymbols())) {
        errln((UnicodeString)"ERROR: adopt or set DecimalFormatSymbols() failed");
    }

    UnicodeString posPrefix;
    pat.setPositivePrefix("+");
    posPrefix = pat.getPositivePrefix(posPrefix);
    logln((UnicodeString)"Positive prefix (should be +): " + posPrefix);
    if(posPrefix != "+") {
        errln((UnicodeString)"ERROR: setPositivePrefix() failed");
    }

    UnicodeString negPrefix;
    pat.setNegativePrefix("-");
    negPrefix = pat.getNegativePrefix(negPrefix);
    logln((UnicodeString)"Negative prefix (should be -): " + negPrefix);
    if(negPrefix != "-") {
        errln((UnicodeString)"ERROR: setNegativePrefix() failed");
    }

    UnicodeString posSuffix;
    pat.setPositiveSuffix("_");
    posSuffix = pat.getPositiveSuffix(posSuffix);
    logln((UnicodeString)"Positive suffix (should be _): " + posSuffix);
    if(posSuffix != "_") {
        errln((UnicodeString)"ERROR: setPositiveSuffix() failed");
    }

    UnicodeString negSuffix;
    pat.setNegativeSuffix("~");
    negSuffix = pat.getNegativeSuffix(negSuffix);
    logln((UnicodeString)"Negative suffix (should be ~): " + negSuffix);
    if(negSuffix != "~") {
        errln((UnicodeString)"ERROR: setNegativeSuffix() failed");
    }

    int32_t multiplier = 0;
    pat.setMultiplier(8);
    multiplier = pat.getMultiplier();
    logln((UnicodeString)"Multiplier (should be 8): " + multiplier);
    if(multiplier != 8) {
        errln((UnicodeString)"ERROR: setMultiplier() failed");
    }

    int32_t groupingSize = 0;
    pat.setGroupingSize(2);
    groupingSize = pat.getGroupingSize();
    logln((UnicodeString)"Grouping size (should be 2): " + (int32_t) groupingSize);
    if(groupingSize != 2) {
        errln((UnicodeString)"ERROR: setGroupingSize() failed");
    }

    pat.setDecimalSeparatorAlwaysShown(TRUE);
    bool_t tf = pat.isDecimalSeparatorAlwaysShown();
    logln((UnicodeString)"DecimalSeparatorIsAlwaysShown (should be TRUE) is " + (UnicodeString) (tf ? "TRUE" : "FALSE"));
    if(tf != TRUE) {
        errln((UnicodeString)"ERROR: setDecimalSeparatorAlwaysShown() failed");
    }

    UnicodeString funkyPat;
    funkyPat = pat.toPattern(funkyPat);
    logln((UnicodeString)"Pattern is " + funkyPat);

    UnicodeString locPat;
    locPat = pat.toLocalizedPattern(locPat);
    logln((UnicodeString)"Localized pattern is " + locPat);

// ======= Test applyPattern()

    logln((UnicodeString)"Testing applyPattern()");

    UnicodeString p1("#,##0.0#;(#,##0.0#)");
    logln((UnicodeString)"Applying pattern " + p1);
    status = U_ZERO_ERROR;
    pat.applyPattern(p1, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: applyPattern() failed with " + (int32_t) status);
    }
    UnicodeString s2;
    s2 = pat.toPattern(s2);
    logln((UnicodeString)"Extracted pattern is " + s2);
    if(s2 != p1) {
        errln((UnicodeString)"ERROR: toPattern() result did not match pattern applied");
    }

    UnicodeString p2("#,##0.0# FF;(#,##0.0# FF)");
    logln((UnicodeString)"Applying pattern " + p2);
    status = U_ZERO_ERROR;
    pat.applyLocalizedPattern(p2, status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: applyPattern() failed with " + (int32_t) status);
    }
    UnicodeString s3;
    s3 = pat.toLocalizedPattern(s3);
    logln((UnicodeString)"Extracted pattern is " + s3);
    if(s3 != p2) {
        errln((UnicodeString)"ERROR: toLocalizedPattern() result did not match pattern applied");
    }

// ======= Test getStaticClassID()

    logln((UnicodeString)"Testing getStaticClassID()");

    status = U_ZERO_ERROR;
    NumberFormat *test = new DecimalFormat(status);
    if(FAILURE(status)) {
        errln((UnicodeString)"ERROR: Couldn't create a DecimalFormat");
    }

    if(test->getDynamicClassID() != DecimalFormat::getStaticClassID()) {
        errln((UnicodeString)"ERROR: getDynamicClassID() didn't return the expected value");
    }

    delete test;
}
