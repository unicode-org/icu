/*
********************************************************************
* COPYRIGHT: 
* (C) Copyright International Business Machines Corporation, 1998
* Licensed Material - Program-Property of IBM - All Rights Reserved. 
* US Government Users Restricted Rights - Use, duplication, or disclosure 
* restricted by GSA ADP Schedule Contract with IBM Corp. 
*
********************************************************************
*/
 
#include "dtfmtrtts.h"

#include <stdio.h>

#include "unicode/datefmt.h"
#include "unicode/smpdtfmt.h"
#include "unicode/gregocal.h"


// *****************************************************************************
// class DateFormatRoundTripTest
// *****************************************************************************

// Useful for turning up subtle bugs: Change the following to TRUE, recompile,
// and run while at lunch.
bool_t DateFormatRoundTripTest::INFINITE = FALSE; // Warning -- makes test run infinite loop!!!

// If SPARSENESS is > 0, we don't run each exhaustive possibility.
// There are 24 total possible tests per each locale.  A SPARSENESS
// of 12 means we run half of them.  A SPARSENESS of 23 means we run
// 1 of them.  SPARSENESS _must_ be in the range 0..23.
int32_t DateFormatRoundTripTest::SPARSENESS = 18;
int32_t DateFormatRoundTripTest::TRIALS = 4;
int32_t DateFormatRoundTripTest::DEPTH = 5;


#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;

void 
DateFormatRoundTripTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    // if (exec) logln((UnicodeString)"TestSuite NumberFormatRegressionTest");
    switch (index) {
        CASE(0,TestDateFormatRoundTrip)
        default: name = ""; break;
    }
}

bool_t 
DateFormatRoundTripTest::failure(UErrorCode status, const char* msg)
{
    if(U_FAILURE(status)) {
        errln(UnicodeString("FAIL: ") + msg + " failed, error " + u_errorName(status));
        return TRUE;
    }

    return FALSE;
}

// ==

void DateFormatRoundTripTest::TestDateFormatRoundTrip() 
{
    UErrorCode status = U_ZERO_ERROR;
    dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss.SSS zzz yyyy G", status);
    failure(status, "new SimpleDateFormat");

    getFieldCal = Calendar::createInstance(status);
    failure(status, "Calendar::createInstance");


    int32_t locCount = 0;
    const Locale *avail = DateFormat::getAvailableLocales(locCount);
    logln("DateFormat available locales: " + locCount);
    if(quick) {
        if(locCount > 5)
            locCount = 5;
        logln("Quick mode: only testing first 5 Locales");
    }
    TimeZone *tz = TimeZone::createDefault();
    UnicodeString temp;
    logln("Default TimeZone:             " + tz->getID(temp));
    delete tz;

    if (INFINITE) {
        // Special infinite loop test mode for finding hard to reproduce errors
        Locale loc = Locale::getDefault();
        logln("ENTERING INFINITE TEST LOOP FOR Locale: " + loc.getDisplayName(temp));
        for(;;) 
            test(loc);
    }
    else {
        test(Locale::getDefault());

        for (int i=0; i < locCount; ++i) {
            test(avail[i]);
        }
    }

    delete dateFormat;
    delete getFieldCal;
}

void DateFormatRoundTripTest::test(const Locale& loc) 
{
    UnicodeString temp;
    if( ! INFINITE) 
        logln("Locale: " + loc.getDisplayName(temp));

    // Total possibilities = 24
    //  4 date
    //  4 time
    //  16 date-time
    bool_t TEST_TABLE [24];//= new boolean[24];
    int32_t i = 0;
    for(i = 0; i < 24; ++i) 
        TEST_TABLE[i] = TRUE;

    // If we have some sparseness, implement it here.  Sparseness decreases
    // test time by eliminating some tests, up to 23.
    for(i = 0; i < SPARSENESS; ) {
        int random = (int)(randFraction() * 24);
        if (random >= 0 && random < 24 && TEST_TABLE[i]) {
            TEST_TABLE[i] = FALSE;
            ++i;
        }
    }

    int32_t itable = 0;
    int32_t style = 0;
    for(style = DateFormat::FULL; style <= DateFormat::SHORT; ++style) {
        if(TEST_TABLE[itable++]) {
            DateFormat *df = DateFormat::createDateInstance((DateFormat::EStyle)style, loc);
            test(df);
            delete df;
        }
    }
    
    for(style = DateFormat::FULL; style <= DateFormat::SHORT; ++style) {
        if (TEST_TABLE[itable++]) {
            DateFormat *df = DateFormat::createTimeInstance((DateFormat::EStyle)style, loc);
            test(df, TRUE);
            delete df;
        }
    }
    
    for(int32_t dstyle = DateFormat::FULL; dstyle <= DateFormat::SHORT; ++dstyle) {
        for(int32_t tstyle = DateFormat::FULL; tstyle <= DateFormat::SHORT; ++tstyle) {
            if(TEST_TABLE[itable++]) {
                DateFormat *df = DateFormat::createDateTimeInstance((DateFormat::EStyle)dstyle, (DateFormat::EStyle)tstyle, loc);
                test(df);
                delete df;
            }
        }
    }
}

void DateFormatRoundTripTest::test(DateFormat *fmt, bool_t timeOnly) 
{
    UnicodeString pat;
    if(fmt->getDynamicClassID() != SimpleDateFormat::getStaticClassID()) {
        errln("DateFormat wasn't a SimpleDateFormat");
        return;
    }

    pat = ((SimpleDateFormat*)fmt)->toPattern(pat);

    // NOTE TO MAINTAINER
    // This indexOf check into the pattern needs to be refined to ignore
    // quoted characters.  Currently, this isn't a problem with the locale
    // patterns we have, but it may be a problem later.

    bool_t hasEra = (pat.indexOf(UnicodeString("G")) != -1);
    bool_t hasZone = (pat.indexOf(UnicodeString("z")) != -1);

    // Because patterns contain incomplete data representing the Date,
    // we must be careful of how we do the roundtrip.  We start with
    // a randomly generated Date because they're easier to generate.
    // From this we get a string.  The string is our real starting point,
    // because this string should parse the same way all the time.  Note
    // that it will not necessarily parse back to the original date because
    // of incompleteness in patterns.  For example, a time-only pattern won't
    // parse back to the same date.

    //try {
        for(int i = 0; i < TRIALS; ++i) {
            UDate *d                = new UDate    [DEPTH];
            UnicodeString *s    = new UnicodeString[DEPTH];

            d[0] = generateDate();

            UErrorCode status = U_ZERO_ERROR;

            // We go through this loop until we achieve a match or until
            // the maximum loop count is reached.  We record the points at
            // which the date and the string starts to match.  Once matching
            // starts, it should continue.
            int loop;
            int dmatch = 0; // d[dmatch].getTime() == d[dmatch-1].getTime()
            int smatch = 0; // s[smatch].equals(s[smatch-1])
            for(loop = 0; loop < DEPTH; ++loop) {
                if (loop > 0)  {
                    d[loop] = fmt->parse(s[loop-1], status);
                    failure(status, "fmt->parse");
                }

                s[loop] = fmt->format(d[loop], s[loop]);

                if(loop > 0) {
                    if(smatch == 0) {
                        bool_t match = s[loop] == s[loop-1];
                        if(smatch == 0) {
                            if(match) 
                                smatch = loop;
                        }
                        else if( ! match) 
                            errln("FAIL: String mismatch after match");
                    }

                    if(dmatch == 0) {
                        // {sfb} watch out here, this might not work
                        bool_t match = d[loop]/*.getTime()*/ == d[loop-1]/*.getTime()*/;
                        if(dmatch == 0) {
                            if(match) 
                                dmatch = loop;
                        }
                        else if( ! match) 
                            errln("FAIL: Date mismatch after match");
                    }

                    if(smatch != 0 && dmatch != 0) 
                        break;
                }
            }
            // At this point loop == DEPTH if we've failed, otherwise loop is the
            // max(smatch, dmatch), that is, the index at which we have string and
            // date matching.

            // Date usually matches in 2.  Exceptions handled below.
            int maxDmatch = 2;
            int maxSmatch = 1;
            if (dmatch > maxDmatch) {
                // Time-only pattern with zone information and a starting date in PST.
                if(timeOnly && hasZone && fmt->getTimeZone().inDaylightTime(d[0], status) && ! failure(status, "TimeZone::inDST()")) {
                    maxDmatch = 3;
                    maxSmatch = 2;
                }
            }

            // String usually matches in 1.  Exceptions are checked for here.
            if(smatch > maxSmatch) { // Don't compute unless necessary
                // Starts in BC, with no era in pattern
                if( ! hasEra && getField(d[0], Calendar::ERA) == GregorianCalendar::BC)
                    maxSmatch = 2;
                // Starts in DST, no year in pattern
                else if(fmt->getTimeZone().inDaylightTime(d[0], status) && ! failure(status, "foo") &&
                         pat.indexOf(UnicodeString("yyyy")) == -1)
                    maxSmatch = 2;
                // Two digit year with zone and year change and zone in pattern
                else if (hasZone &&
                         fmt->getTimeZone().inDaylightTime(d[0], status) !=
                         fmt->getTimeZone().inDaylightTime(d[dmatch], status) && ! failure(status, "foo") &&
                         getField(d[0], Calendar::YEAR) !=
                         getField(d[dmatch], Calendar::YEAR) &&
                         pat.indexOf(UnicodeString("y")) != -1 &&
                         pat.indexOf(UnicodeString("yyyy")) == -1)
                    maxSmatch = 2;
            }
            
            if(dmatch > maxDmatch || smatch > maxSmatch) {
                errln(UnicodeString("Pattern: ") + pat);
                logln(UnicodeString(" Date ") + dmatch + "  String " + smatch);
                
                for(int j = 0; j <= loop && j < DEPTH; ++j) {
                    UnicodeString temp;
                    FieldPosition pos(FieldPosition::DONT_CARE);
                    logln((j>0?" P> ":"    ") + dateFormat->format(d[j], temp, pos) + " F> " +
                          escape(s[j], temp) +
                          (j > 0 && d[j]/*.getTime()*/==d[j-1]/*.getTime()*/?" d==":"") +
                          (j > 0 && s[j] == s[j-1]?" s==":""));
                }
            }
        }
    /*}
    catch (ParseException e) {
        errln("Exception: " + e.getMessage());
        logln(e.toString());
    }*/
}

/**
 * Return a field of the given date
 */
int32_t DateFormatRoundTripTest::getField(UDate d, int32_t f) {
    // Should be synchronized, but we're single threaded so it's ok
    UErrorCode status = U_ZERO_ERROR;
    getFieldCal->setTime(d, status);
    failure(status, "getfieldCal->setTime");
    int32_t ret = getFieldCal->get((Calendar::EDateFields)f, status);
    failure(status, "getfieldCal->get");
    return ret;
}

UnicodeString& DateFormatRoundTripTest::escape(const UnicodeString& src, UnicodeString& dst ) 
{
    dst.remove();
    for (int32_t i = 0; i < src.length(); ++i) {
        UChar c = src[i];
        if(c < 0x0080) 
            dst += c;
        else {
            dst += UnicodeString("[");
            char buf [8];
            sprintf(buf, "%#x", c);
            dst += UnicodeString(buf);
            dst += UnicodeString("]");
        }
    }

    return dst;
}

UDate DateFormatRoundTripTest::generateDate() 
{
    double a = randFraction();
    
    // Now 'a' ranges from 0..1; scale it to range from 0 to 8000 years
    a *= 8000;
    
    // Range from (4000-1970) BC to (8000-1970) AD
    a -= 4000;
    
    // Now scale up to ms
    a *= 365.25 * 24 * 60 * 60 * 1000;
    
    //return new Date((long)a);
    return a;
}

//eof
