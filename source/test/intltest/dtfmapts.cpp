
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
#include "dtfmapts.h"

#include "datefmt.h"
#include "smpdtfmt.h"


// This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
// try to test the full functionality.  It just calls each function in the class and
// verifies that it works on a basic level.

void IntlTestDateFormatAPI::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite DateFormatAPI");
    switch (index) {
        case 0: name = "DateFormat API test"; 
                if (exec) {
                    logln("DateFormat API test---"); logln("");
                    UErrorCode status = ZERO_ERROR;
                    Locale::setDefault(Locale::ENGLISH, status);
                    if(FAILURE(status)) {
                        errln("ERROR: Could not set default locale, test may not give correct results");
                    }
                    testAPI(par);
                }
                break;

        case 1: name = "TestEquals"; 
                if (exec) {
                    logln("TestEquals---"); logln("");
                    TestEquals();
                }
                break;

        default: name = ""; break;
    }
}

/**
 * Test that the equals method works correctly.
 */
void IntlTestDateFormatAPI::TestEquals()
{
    UErrorCode status = ZERO_ERROR;
    // Create two objects at different system times
    DateFormat *a = DateFormat::createInstance();
    UDate start = Calendar::getNow();
    while (Calendar::getNow() == start) ; // Wait for time to change
    DateFormat *b = DateFormat::createInstance();

    if (!(*a == *b))
        errln("FAIL: DateFormat objects created at different times are unequal.");

    if (b->getDynamicClassID() == SimpleDateFormat::getStaticClassID())
    {
        double ONE_YEAR = 365*24*60*60*1000.0;
        ((SimpleDateFormat*)b)->set2DigitYearStart(start + 50*ONE_YEAR, status);
        if (FAILURE(status))
            errln("FAIL: setTwoDigitStartDate failed.");
        else if (*a == *b)
            errln("FAIL: DateFormat objects with different two digit start dates are equal.");
    }
    delete a;
    delete b;
}

/**
 * This test checks various generic API methods in DateFormat to achieve 100%
 * API coverage.
 */
void IntlTestDateFormatAPI::testAPI(char *par)
{
    UErrorCode status = ZERO_ERROR;

// ======= Test constructors

    logln("Testing DateFormat constructors");

    DateFormat *def = DateFormat::createInstance();
    DateFormat *fr = DateFormat::createTimeInstance(DateFormat::FULL, Locale::FRENCH);
    DateFormat *it = DateFormat::createDateInstance(DateFormat::MEDIUM, Locale::ITALIAN);
    DateFormat *de = DateFormat::createDateTimeInstance(DateFormat::LONG, DateFormat::LONG, Locale::GERMAN);

// ======= Test equality

    logln("Testing equality operator");
    
    if( *fr == *it ) {
        errln("ERROR: == failed");
    }

// ======= Test various format() methods

    logln("Testing various format() methods");

    UDate d = 837039928046.0;
    Formattable fD(d, Formattable::kIsDate);

    UnicodeString res1, res2, res3;
    FieldPosition pos1(0), pos2(0);
    
    status = ZERO_ERROR;
    res1 = fr->format(d, res1, pos1, status);
    if(FAILURE(status)) {
        errln("ERROR: format() failed (French)");
    }
    logln( (UnicodeString) "" + d + " formatted to " + res1);

    res2 = it->format(d, res2, pos2);
    logln( (UnicodeString) "" + d + " formatted to " + res2);

    res3 = de->format(d, res3);
    logln( (UnicodeString) "" + d + " formatted to " + res3);

// ======= Test parse()

    logln("Testing parse()");

    UnicodeString text("02/03/76 2:50 AM, CST");
    Formattable result1;
    UDate result2, result3;
    ParsePosition pos(0), pos01(0);
    def->parseObject(text, result1, pos);
    if(result1.getType() != Formattable::kDate) {
        errln("ERROR: parseObject() failed for " + text);
    }
    logln(text + " parsed into " + result1.getDate());

    status = ZERO_ERROR;
    result2 = def->parse(text, status);
    if(FAILURE(status)) {
        errln("ERROR: parse() failed");
    }
    logln(text + " parsed into " + result2);

    result3 = def->parse(text, pos01);
    logln(text + " parsed into " + result3);


// ======= Test getters and setters

    logln("Testing getters and setters");

    int32_t count = 0;
    const Locale *locales = DateFormat::getAvailableLocales(count);
    logln((UnicodeString) "Got " + count + " locales" );
    for(int32_t i = 0; i < count; i++) {
        UnicodeString name;
        name = locales[i].getName(name);
        logln(name);
    }

    fr->setLenient(it->isLenient());
    if(fr->isLenient() != it->isLenient()) {
        errln("ERROR: setLenient() failed");
    }

    const Calendar *cal = def->getCalendar();
    Calendar *newCal = cal->clone();
    de->adoptCalendar(newCal);  
    it->setCalendar(*newCal);
    if( *(de->getCalendar()) != *(it->getCalendar())) {
        errln("ERROR: adopt or set Calendar() failed");
    }

    const NumberFormat *nf = def->getNumberFormat();
    NumberFormat *newNf = (NumberFormat*) nf->clone();
    de->adoptNumberFormat(newNf);   
    it->setNumberFormat(*newNf);
    if( *(de->getNumberFormat()) != *(it->getNumberFormat())) {
        errln("ERROR: adopt or set NumberFormat() failed");
    }

    const TimeZone& tz = def->getTimeZone();
    TimeZone *newTz = tz.clone();
    de->adoptTimeZone(newTz);   
    it->setTimeZone(*newTz);
    if( de->getTimeZone() != it->getTimeZone()) {
        errln("ERROR: adopt or set TimeZone() failed");
    }

// ======= Test getStaticClassID()

    logln("Testing getStaticClassID()");

    status = ZERO_ERROR;
    DateFormat *test = new SimpleDateFormat(status);
    if(FAILURE(status)) {
        errln("ERROR: Couldn't create a DateFormat");
    }

    if(test->getDynamicClassID() != SimpleDateFormat::getStaticClassID()) {
        errln("ERROR: getDynamicClassID() didn't return the expected value");
    }

    delete test;
    delete def;
    delete fr;
    delete it;
    delete de;
}
