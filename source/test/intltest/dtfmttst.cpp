/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
 
#include "dtfmttst.h"
#include "unicode/timezone.h"
#include "unicode/gregocal.h"
#include "unicode/smpdtfmt.h"
#include "unicode/datefmt.h"
#include "unicode/simpletz.h"

// *****************************************************************************
// class DateFormatTest
// *****************************************************************************

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;

void DateFormatTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    // if (exec) logln((UnicodeString)"TestSuite DateFormatTest");
    switch (index) {
        CASE(0,TestEquals)
        CASE(1,TestTwoDigitYearDSTParse)
        CASE(2,TestFieldPosition)
        CASE(3,TestPartialParse994)
        CASE(4,TestRunTogetherPattern985)
        CASE(5,TestRunTogetherPattern917)
        CASE(6,TestCzechMonths459)
        CASE(7,TestLetterDPattern212)
        CASE(8,TestDayOfYearPattern195)
        CASE(9,TestQuotePattern161)
        CASE(10,TestBadInput135)
        CASE(11,TestBadInput135a)
        CASE(12,TestTwoDigitYear)
        CASE(13,TestDateFormatZone061)
        CASE(14,TestDateFormatZone146)
        CASE(15,TestLocaleDateFormat)
        CASE(16,TestWallyWedel)
        default: name = ""; break;
    }
}

// Test written by Wally Wedel and emailed to me.
void DateFormatTest::TestWallyWedel()
{
    UErrorCode status = U_ZERO_ERROR;
    /*
     * Instantiate a TimeZone so we can get the ids.
     */
    TimeZone *tz = new SimpleTimeZone(7,"");
    /*
     * Computational variables.
     */
    int32_t offset, hours, minutes;
    /*
     * Instantiate a SimpleDateFormat set up to produce a full time
     zone name.
     */
    SimpleDateFormat *sdf = new SimpleDateFormat("zzzz", status);
    /*
     * A String array for the time zone ids.
     */
    int32_t ids_length;
    const UnicodeString **ids = TimeZone::createAvailableIDs(ids_length);
    /*
     * How many ids do we have?
     */
    logln("Time Zone IDs size: " + ids_length);
    /*
     * Column headings (sort of)
     */
    logln("Ordinal ID offset(h:m) name");
    /*
     * Loop through the tzs.
     */
    UDate today = Calendar::getNow();
    Calendar *cal = Calendar::createInstance(status);
    for (int32_t i = 0; i < ids_length; i++) {
        // logln(i + " " + ids[i]);
        TimeZone *ttz = TimeZone::createTimeZone(*ids[i]);
        // offset = ttz.getRawOffset();
        cal->setTimeZone(*ttz);
        cal->setTime(today, status);
        offset = cal->get(Calendar::ZONE_OFFSET, status) + cal->get(Calendar::DST_OFFSET, status);
        // logln(i + " " + ids[i] + " offset " + offset);
        char* sign = "+";
        if (offset < 0) { sign = "-"; offset = -offset; }
        hours = offset/3600000;
        minutes = (offset%3600000)/60000;
        UnicodeString dstOffset = (UnicodeString)"" + sign + (hours < 10 ? "0" : "") +
            (int32_t)hours + ":" + (minutes < 10 ? "0" : "") + (int32_t)minutes;
        /*
         * Instantiate a date so we can display the time zone name.
         */
        sdf->setTimeZone(*ttz);
        /*
         * Format the output.
         */
        UnicodeString fmtOffset;
        FieldPosition pos(0);
        sdf->format(today,fmtOffset, pos);
        // UnicodeString fmtOffset = tzS.toString();
        UnicodeString *fmtDstOffset = 0;
        if (fmtOffset.startsWith("GMT"))
        {
            //fmtDstOffset = fmtOffset->substring(3);
            fmtDstOffset = new UnicodeString();
            fmtOffset.extract(3, fmtOffset.length(), *fmtDstOffset);
        }
        /*
         * Show our result.
         */
        bool_t ok = fmtDstOffset == 0 || *fmtDstOffset == dstOffset;
        if (ok)
        {
            logln(UnicodeString() + i + " " + *ids[i] + " " + dstOffset +
                  " " + fmtOffset +
                  (fmtDstOffset != 0 ? " ok" : " ?"));
        }
        else
        {
            errln(UnicodeString() + i + " " + *ids[i] + " " + dstOffset +
                  " " + fmtOffset + " *** FAIL ***");
        }
        delete ttz;
        delete fmtDstOffset;
    }
    delete cal;
    delete ids;
    delete sdf;
    delete tz;
}

// -------------------------------------
 
/**
 * Test operator==
 */
void
DateFormatTest::TestEquals()
{
    DateFormat* fmtA = DateFormat::createDateTimeInstance(DateFormat::MEDIUM, DateFormat::FULL);
    DateFormat* fmtB = DateFormat::createDateTimeInstance(DateFormat::MEDIUM, DateFormat::FULL);
    if (!(*fmtA == *fmtB)) errln((UnicodeString)"FAIL");
    delete fmtA;
    delete fmtB;
}
 
// -------------------------------------

/**
 * Test the parsing of 2-digit years.
 */
void
DateFormatTest::TestTwoDigitYearDSTParse(void)
{
    UErrorCode status = U_ZERO_ERROR;
    SimpleDateFormat* fullFmt = new SimpleDateFormat("EEE MMM dd HH:mm:ss.SSS zzz yyyy G", status);
    SimpleDateFormat *fmt = new SimpleDateFormat("dd-MMM-yy h:mm:ss 'o''clock' a z", Locale::ENGLISH, status);
    //DateFormat* fmt = DateFormat::createDateTimeInstance(DateFormat::MEDIUM, DateFormat::FULL, Locale::ENGLISH);
    UnicodeString* s = new UnicodeString("03-Apr-04 2:20:47 o'clock AM PST");
    int32_t hour = 2;

    UnicodeString str;
    UDate d = fmt->parse(*s, status);
    logln(*s + " P> " + ((DateFormat*)fullFmt)->format(d, str));
    int32_t y, m, day, hr, min, sec;
    dateToFields(d, y, m, day, hr, min, sec);
    if (hr != hour) errln((UnicodeString)"FAIL: Should parse to hour " + hour);

    if (U_FAILURE(status))
        errln((UnicodeString)"FAIL: " + (int32_t)status);

    delete s;
    delete fmt;
    delete fullFmt;
}
 
// -------------------------------------
 
char toHexString(int32_t i) { return i + (i < 10 ? '0' : ('A' - 10)); }

UnicodeString&
DateFormatTest::escape(UnicodeString& s)
{
    UnicodeString buf;
    for (int32_t i=0; i<s.length(); ++i)
    {
        UChar c = s[(UTextOffset)i];
        if (c <= (UChar)0x7F) buf += c;
        else {
            buf += '\\'; buf += 'U';
            buf += toHexString((c & 0xF000) >> 12);
            buf += toHexString((c & 0x0F00) >> 8);
            buf += toHexString((c & 0x00F0) >> 4);
            buf += toHexString(c & 0x000F);
        }
    }
    return (s = buf);
}
 
char* DateFormatTest::fieldNames[] = {
    "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH", "DAY_OF_MONTH", 
        "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH", "AM_PM", "HOUR", 
        "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND", "ZONE_OFFSET", //"DST_OFFSET", 
        "YEAR_WOY", "DOW_LOCAL"
};
 
// -------------------------------------

// Map Calendar field number to to DateFormat field number
const DateFormat::EField
DateFormatTest::fgCalendarToDateFormatField[] = {
    DateFormat::kEraField, 
    DateFormat::kYearField, 
    DateFormat::kMonthField,
    DateFormat::kWeekOfYearField, 
    DateFormat::kWeekOfMonthField,
    DateFormat::kDateField, 
    DateFormat::kDayOfYearField, 
    DateFormat::kDayOfWeekField,     
    DateFormat::kDayOfWeekInMonthField, 
    DateFormat::kAmPmField,
    DateFormat::kHour1Field, 
    DateFormat::kHourOfDay0Field, 
    DateFormat::kMinuteField, 
    DateFormat::kSecondField, 
    DateFormat::kMillisecondField,
    DateFormat::kTimezoneField, 
    DateFormat::kYearWOYField,
    DateFormat::kDOWLocalField,
    (DateFormat::EField) -1
};

/**
 * Verify that returned field position indices are correct.
 */
void
DateFormatTest::TestFieldPosition(void)
{
    UErrorCode status = U_ZERO_ERROR;
    DateFormat* dateFormats[4];
    int32_t dateFormats_length = sizeof(dateFormats) / sizeof(dateFormats[0]);

    /* {sfb} This test was coded incorrectly.
    / FieldPosition uses the fields in the class you are formatting with
    / So, for example, to get the DATE field from a DateFormat use
    / DateFormat::DATE_FIELD, __not__ Calendar::DATE
    / The ordering of the expected values used previously was wrong.
    / instead of re-ordering this mess of strings, just transform the index values */

    /* field values, in Calendar order */

    const char* expected[] = {
        "", "1997", "August", "", "", "13", "", "Wednesday", "", "PM", "2", "", 
        "34", "12", "", "PDT", "", 
        /* Following two added by weiv for two new fields */ "", "", 
        "", "1997", "#",/* # is a marker for "ao\xfbt" == "aou^t" */  "", "", "13", "", "mercredi", 
        "", "", "", "14", "34", "", "", "GMT-07:00", "", 
        /* Following two added by weiv for two new fields */ "", "", 
        "AD", "97", "8", "33", "3", "13", "225", "Wed", "2", "PM", "2", 
        "14", "34", "12", "5", "PDT", 
        /* Following two added by weiv for two new fields */ "97", "4", "",
        "AD", "1997", "August", "0033", 
        "0003", "0013", "0225", "Wednesday", "0002", "PM", "0002", "0014", 
        "0034", "0012", "513", "Pacific Daylight Time", 
        /* Following two added by weiv for two new fields */ "1997", "0004",
        ""

    };

    UDate someDate = 871508052513.0;
    int32_t j, exp;

    dateFormats[0] = DateFormat::createDateTimeInstance(DateFormat::FULL, DateFormat::FULL, Locale::US);
    dateFormats[1] = DateFormat::createDateTimeInstance(DateFormat::FULL, DateFormat::FULL, Locale::FRANCE);
    dateFormats[2] = new SimpleDateFormat("G, y, M, d, k, H, m, s, S, E, D, F, w, W, a, h, K, z, Y, e", status);
    dateFormats[3] = new SimpleDateFormat("GGGG, yyyy, MMMM, dddd, kkkk, HHHH, mmmm, ssss, SSSS, EEEE, DDDD, FFFF, wwww, WWWW, aaaa, hhhh, KKKK, zzzz, YYYY, eeee", status);
    for (j = 0, exp = 0; j < dateFormats_length;++j) {
        UnicodeString str;
        DateFormat* df = dateFormats[j];
        logln((UnicodeString)" Pattern = " + ((SimpleDateFormat*)df)->toPattern(str));
        str.truncate(0);
        logln((UnicodeString)"  Result = " + df->format(someDate, str));
        for (int32_t i = 0; i < Calendar::FIELD_COUNT;++i) {
            UnicodeString field;
            getFieldText(df, i, someDate, field);
            UnicodeString expStr;
            if(expected[exp][0]!='#') {
                expStr=UnicodeString(expected[exp]);
            } else {
                /* we cannot have latin-1 characters in source code, therefore we fix up the string for "aou^t" */
                expStr.append(0x61).append(0x6f).append(0xfb).append(0x74);
            }
            
            if (!(field == expStr)) errln(UnicodeString("FAIL: field #") + i + " " +
                fieldNames[i] + " = \"" + escape(field) + "\", expected \"" + escape(expStr) + "\"");
            ++exp;
        }
    }
    for (j=0; j<dateFormats_length; ++j) delete dateFormats[j];
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status); 
}
 
// -------------------------------------
 
void
DateFormatTest::getFieldText(DateFormat* df, int32_t field, UDate date, UnicodeString& str)
{
    UnicodeString formatResult;
    // {sfb} added to convert Calendar Fields to DateFormat fields
    FieldPosition pos(fgCalendarToDateFormatField[field]);
    df->format(date, formatResult, pos);
    //formatResult.extract(pos.getBeginIndex(), pos.getEndIndex(), str);
    formatResult.extractBetween(pos.getBeginIndex(), pos.getEndIndex(), str);
}
 
// -------------------------------------

/**
 * Verify that strings which contain incomplete specifications are parsed
 * correctly.  In some instances, this means not being parsed at all, and
 * returning an appropriate error.
 */
void
DateFormatTest::TestPartialParse994()
{
    UErrorCode status = U_ZERO_ERROR;
    SimpleDateFormat* f = new SimpleDateFormat(status);
    UDate null = 0;
    tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10:11:42", date(97, 1 - 1, 17, 10, 11, 42));
    tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10:", null);
    tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10", null);
    tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 ", null);
    tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17", null);
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
    delete f;
}
 
// -------------------------------------
 
void
DateFormatTest::tryPat994(SimpleDateFormat* format, const char* pat, const char* str, UDate expected)
{
    UErrorCode status = U_ZERO_ERROR;
    UDate null = 0;
    logln(UnicodeString("Pattern \"") + pat + "\"   String \"" + str + "\"");
    //try {
        format->applyPattern(pat);
        UDate date = format->parse(str, status);
        if (U_FAILURE(status) || date == null)
        {
            logln((UnicodeString)"ParseException: " + (int32_t)status);
            if (expected != null) errln((UnicodeString)"FAIL: Expected " + dateToString(expected));
        }
        else
        {
            UnicodeString f;
            ((DateFormat*)format)->format(date, f);
            logln(UnicodeString(" parse(") + str + ") -> " + dateToString(date));
            logln((UnicodeString)" format -> " + f);
            if (expected == null ||
                !(date == expected)) errln((UnicodeString)"FAIL: Expected null");//" + expected);
            if (!(f == str)) errln(UnicodeString("FAIL: Expected ") + str);
        }
    //}
    //catch(ParseException e) {
    //    logln((UnicodeString)"ParseException: " + e.getMessage());
    //    if (expected != null) errln((UnicodeString)"FAIL: Expected " + dateToString(expected));
    //}
    //catch(Exception e) {
    //    errln((UnicodeString)"*** Exception:");
    //    e.printStackTrace();
    //}
}
 
// -------------------------------------
 
/**
 * Verify the behavior of patterns in which digits for different fields run together
 * without intervening separators.
 */
void
DateFormatTest::TestRunTogetherPattern985()
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString format("yyyyMMddHHmmssSSS");
    UnicodeString now, then;
    //bool_t flag;
    SimpleDateFormat *formatter = new SimpleDateFormat(format, status);
    UDate date1 = Calendar::getNow();
    ((DateFormat*)formatter)->format(date1, now);
    logln(now);
    ParsePosition pos(0);
    UDate date2 = formatter->parse(now, pos);
    if (date2 == 0) then = "Parse stopped at " + pos.getIndex();
    else ((DateFormat*)formatter)->format(date2, then);
    logln(then);
    if (!(date2 == date1)) errln((UnicodeString)"FAIL");
    delete formatter;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
}
 
// -------------------------------------
 
/**
 * Verify the behavior of patterns in which digits for different fields run together
 * without intervening separators.
 */
void
DateFormatTest::TestRunTogetherPattern917()
{
    UErrorCode status = U_ZERO_ERROR;
    SimpleDateFormat* fmt;
    UnicodeString myDate;
    fmt = new SimpleDateFormat("yyyy/MM/dd", status);
    myDate = "1997/02/03";
    testIt917(fmt, myDate, date(97, 2 - 1, 3));
    delete fmt;
    fmt = new SimpleDateFormat("yyyyMMdd", status);
    myDate = "19970304";
    testIt917(fmt, myDate, date(97, 3 - 1, 4));
    delete fmt;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
}
 
// -------------------------------------
 
void
DateFormatTest::testIt917(SimpleDateFormat* fmt, UnicodeString& str, UDate expected)
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString pattern;
    logln((UnicodeString)"pattern=" + fmt->toPattern(pattern) + "   string=" + str);
    Formattable o;
    //try {
        ((Format*)fmt)->parseObject(str, o, status);
    //}
    if (U_FAILURE(status)) return;
    //catch(ParseException e) {
    //    e.printStackTrace();
    //    return;
    //}
    logln((UnicodeString)"Parsed object: " + dateToString(o.getDate()));
    if (!(o.getDate() == expected)) errln((UnicodeString)"FAIL: Expected " + dateToString(expected));
    UnicodeString formatted; ((Format*)fmt)->format(o, formatted, status);
    logln((UnicodeString)"Formatted string: " + formatted);
    if (!(formatted == str)) errln((UnicodeString)"FAIL: Expected " + str);
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
}
 
// -------------------------------------
 
/**
 * Verify the handling of Czech June and July, which have the unique attribute that
 * one is a proper prefix substring of the other.
 */
void
DateFormatTest::TestCzechMonths459()
{
    UErrorCode status = U_ZERO_ERROR;
    DateFormat* fmt = DateFormat::createDateInstance(DateFormat::FULL, Locale("cs", "", ""));
    UnicodeString pattern;
    logln((UnicodeString)"Pattern " + ((SimpleDateFormat*) fmt)->toPattern(pattern));
    UDate june = date(97, Calendar::JUNE, 15);
    UDate july = date(97, Calendar::JULY, 15);
    UnicodeString juneStr; fmt->format(june, juneStr);
    UnicodeString julyStr; fmt->format(july, julyStr);
    //try {
        logln((UnicodeString)"format(June 15 1997) = " + juneStr);
        UDate d = fmt->parse(juneStr, status);
        UnicodeString s; fmt->format(d, s);
        int32_t month,yr,day,hr,min,sec; dateToFields(d,yr,month,day,hr,min,sec);
        logln((UnicodeString)"  -> parse -> " + s + " (month = " + month + ")");
        if (month != Calendar::JUNE) errln((UnicodeString)"FAIL: Month should be June");
        logln((UnicodeString)"format(July 15 1997) = " + julyStr);
        d = fmt->parse(julyStr, status);
        fmt->format(d, s);
        dateToFields(d,yr,month,day,hr,min,sec);
        logln((UnicodeString)"  -> parse -> " + s + " (month = " + month + ")");
        if (month != Calendar::JULY) errln((UnicodeString)"FAIL: Month should be July");
    //}
    //catch(ParseException e) {
    if (U_FAILURE(status))
        errln((UnicodeString)"Exception: " + (int32_t)status);
    //}
    delete fmt;
}
 
// -------------------------------------

/**
 * Test the handling of 'D' in patterns.
 */
void
DateFormatTest::TestLetterDPattern212()
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString dateString("1995-040.05:01:29");
    UnicodeString bigD("yyyy-DDD.hh:mm:ss");
    UnicodeString littleD("yyyy-ddd.hh:mm:ss");
    UDate expLittleD = date(95, 0, 1, 5, 1, 29);
    UDate expBigD = expLittleD + 39 * 24 * 3600000.0;
    expLittleD = expBigD; // Expect the same, with default lenient parsing
    logln((UnicodeString)"dateString= " + dateString);
    SimpleDateFormat *formatter = new SimpleDateFormat(bigD, status);
    ParsePosition pos(0);
    UDate myDate = formatter->parse(dateString, pos);
    logln((UnicodeString)"Using " + bigD + " -> " + myDate);
    if (myDate != expBigD) errln((UnicodeString)"FAIL: Expected " + dateToString(expBigD));
    delete formatter;
    formatter = new SimpleDateFormat(littleD, status);
    pos = ParsePosition(0);
    myDate = formatter->parse(dateString, pos);
    logln((UnicodeString)"Using " + littleD + " -> " + dateToString(myDate));
    if (myDate != expLittleD) errln((UnicodeString)"FAIL: Expected " + dateToString(expLittleD));
    delete formatter;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
}
 
// -------------------------------------

/**
 * Test the day of year pattern.
 */
void
DateFormatTest::TestDayOfYearPattern195()
{
    UErrorCode status = U_ZERO_ERROR;
    UDate today = Calendar::getNow();
    int32_t year,month,day,hour,min,sec; dateToFields(today,year,month,day,hour,min,sec);
    UDate expected = date(year, month, day);
    logln((UnicodeString)"Test Date: " + dateToString(today));
    SimpleDateFormat* sdf = (SimpleDateFormat*)DateFormat::createDateInstance();
    tryPattern(*sdf, today, 0, expected);
    tryPattern(*sdf, today, "G yyyy DDD", expected);
    delete sdf;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
}
 
// -------------------------------------
 
void
DateFormatTest::tryPattern(SimpleDateFormat& sdf, UDate d, const char* pattern, UDate expected)
{
    UErrorCode status = U_ZERO_ERROR;
    if (pattern != 0) sdf.applyPattern(pattern);
    UnicodeString thePat;
    logln((UnicodeString)"pattern: " + sdf.toPattern(thePat));
    UnicodeString formatResult; (*(DateFormat*)&sdf).format(d, formatResult);
    logln((UnicodeString)" format -> " + formatResult);
    // try {
        UDate d2 = sdf.parse(formatResult, status);
        logln((UnicodeString)" parse(" + formatResult + ") -> " + dateToString(d2));
        if (d2 != expected) errln((UnicodeString)"FAIL: Expected " + dateToString(expected));
        UnicodeString format2; (*(DateFormat*)&sdf).format(d2, format2);
        logln((UnicodeString)" format -> " + format2);
        if (!(formatResult == format2)) errln((UnicodeString)"FAIL: Round trip drift");
    //}
    //catch(Exception e) {
    if (U_FAILURE(status))
        errln((UnicodeString)"Error: " + (int32_t)status);
    //}
}
 
// -------------------------------------

/**
 * Test the handling of single quotes in patterns.
 */
void
DateFormatTest::TestQuotePattern161()
{
    UErrorCode status = U_ZERO_ERROR;
    SimpleDateFormat* formatter = new SimpleDateFormat("MM/dd/yyyy 'at' hh:mm:ss a zzz", status);
    UDate currentTime_1 = date(97, Calendar::AUGUST, 13, 10, 42, 28);
    UnicodeString dateString; ((DateFormat*)formatter)->format(currentTime_1, dateString);
    UnicodeString exp("08/13/1997 at 10:42:28 AM ");
    logln((UnicodeString)"format(" + dateToString(currentTime_1) + ") = " + dateString);
    if (0 != dateString.compareBetween(0, exp.length(), exp, 0, exp.length())) errln((UnicodeString)"FAIL: Expected " + exp);
    delete formatter;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
}
 
// -------------------------------------

/**
 * Verify the correct behavior when handling invalid input strings.
 */
void
DateFormatTest::TestBadInput135()
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t looks[] = {
        DateFormat::SHORT, DateFormat::MEDIUM, DateFormat::LONG, DateFormat::FULL
    };
    int32_t looks_length = sizeof(looks) / sizeof(looks[0]);
    const char* strings[] = {
        "Mar 15", "Mar 15 1997", "asdf", "3/1/97 1:23:", "3/1/00 1:23:45 AM"
    };
    int32_t strings_length = sizeof(strings) / sizeof(strings[0]);
    DateFormat *full = DateFormat::createDateTimeInstance(DateFormat::LONG, DateFormat::LONG);
    UnicodeString expected("March 1, 2000 1:23:45 AM ");
    for (int32_t i = 0; i < strings_length;++i) {
        const char* text = strings[i];
        for (int32_t j = 0; j < looks_length;++j) {
            int32_t dateLook = looks[j];
            for (int32_t k = 0; k < looks_length;++k) {
                int32_t timeLook = looks[k];
                DateFormat *df = DateFormat::createDateTimeInstance((DateFormat::EStyle)dateLook, (DateFormat::EStyle)timeLook);
                UnicodeString prefix = UnicodeString(text) + ", " + dateLook + "/" + timeLook + ": ";
                //try {
                    UDate when = df->parse(text, status);
                    if (when == 0 && U_SUCCESS(status)) {
                        errln(prefix + "SHOULD NOT HAPPEN: parse returned null.");
                        continue;
                    }
                    if (U_SUCCESS(status))
                    {
                        UnicodeString format; full->format(when, format);
                        logln(prefix + "OK: " + format);
                        if (0!=format.compareBetween(0, expected.length(), expected, 0, expected.length()))
                            errln((UnicodeString)"FAIL: Expected " + expected);
                    }
                //}
                //catch(ParseException e) {
                    else status = U_ZERO_ERROR;
                //}
                //catch(StringIndexOutOfBoundsException e) {
                //    errln(prefix + "SHOULD NOT HAPPEN: " + (int)status);
                //}
                delete df;
            }
        }
    }
    delete full;
    if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
}
 
const char* DateFormatTest::parseFormats[] = {
    "MMMM d, yyyy",
    "MMMM d yyyy",
    "M/d/yy",
    "d MMMM, yyyy",
    "d MMMM yyyy",
    "d MMMM",
    "MMMM d",
    "yyyy",
    "h:mm a MMMM d, yyyy"
};
 
const char* DateFormatTest::inputStrings[] = {
    "bogus string", 0, 0, 0, 0, 0, 0, 0, 0, 0,
    "April 1, 1997", "April 1, 1997", 0, 0, 0, 0, 0, "April 1", 0, 0,
    "Jan 1, 1970", "January 1, 1970", 0, 0, 0, 0, 0, "January 1", 0, 0,
    "Jan 1 2037", 0, "January 1 2037", 0, 0, 0, 0, "January 1", 0, 0,
    "1/1/70", 0, 0, "1/1/70", 0, 0, 0, 0, "0001", 0,
    "5 May 1997", 0, 0, 0, 0, "5 May 1997", "5 May", 0, "0005", 0,
    "16 May", 0, 0, 0, 0, 0, "16 May", 0, "0016", 0,
    "April 30", 0, 0, 0, 0, 0, 0, "April 30", 0, 0,
    "1998", 0, 0, 0, 0, 0, 0, 0, "1998", 0,
    "1", 0, 0, 0, 0, 0, 0, 0, "0001", 0,
    "3:00 pm Jan 1, 1997", 0, 0, 0, 0, 0, 0, 0, "0003", "3:00 PM January 1, 1997",
};
 
// -------------------------------------

/**
 * Verify the correct behavior when parsing an array of inputs against an
 * array of patterns, with known results.  The results are encoded after
 * the input strings in each row.
 */
void
DateFormatTest::TestBadInput135a()
{
  UErrorCode status = U_ZERO_ERROR;
  SimpleDateFormat* dateParse = new SimpleDateFormat(status);
  const char* s;
  UDate date;
  int32_t PFLENGTH = sizeof(parseFormats)/sizeof(parseFormats[0]);
  dateParse->applyPattern("d MMMM, yyyy");
  dateParse->adoptTimeZone(TimeZone::createDefault());
  s = "not parseable";
  UnicodeString thePat;
  logln(UnicodeString("Trying to parse \"") + s + "\" with " + dateParse->toPattern(thePat));
  //try {
  date = dateParse->parse(s, status);
  if (U_SUCCESS(status))
    errln((UnicodeString)"FAIL: Expected exception during parse");
  //}
  //catch(Exception ex) {
  else
    logln((UnicodeString)"Exception during parse: " + (int32_t)status);
  status = U_ZERO_ERROR;
  //}
  for (int32_t i = 0; i < sizeof(inputStrings)/sizeof(inputStrings[0]); i += (PFLENGTH + 1)) {
    ParsePosition parsePosition(0);
    UnicodeString s( inputStrings[i]);
    for (int32_t index = 0; index < PFLENGTH;++index) {
      const char* expected = inputStrings[i + 1 + index];
      dateParse->applyPattern(parseFormats[index]);
      dateParse->adoptTimeZone(TimeZone::createDefault());
      //try {
      parsePosition.setIndex(0);
      date = dateParse->parse(s, parsePosition);
      if (parsePosition.getIndex() != 0) {
    UnicodeString s1, s2;
    s.extract(0, parsePosition.getIndex(), s1);
    s.extract(parsePosition.getIndex(), s.length(), s2);
    if (date == 0) errln((UnicodeString)"ERROR: null result fmt=\"" +
                 parseFormats[index] +
                 "\" pos=" + parsePosition.getIndex() + " " +
                 s1 + "|" + s2);
    else {
      UnicodeString result; ((DateFormat*)dateParse)->format(date, result);
      logln((UnicodeString)"Parsed \"" + s + "\" using \"" + dateParse->toPattern(thePat) + "\" to: " + result);
      if (expected == 0) errln((UnicodeString)"FAIL: Expected parse failure");
      else if (!(result == expected)) errln(UnicodeString("FAIL: Expected ") + expected);
    }
      }
      else {
    if (expected != 0) errln(UnicodeString("FAIL: Expected ") + expected + " from \"" +
                 s + "\" with \"" + dateParse->toPattern(thePat) + "\"");
      }
      //}
      //catch(Exception ex) {
      if (U_FAILURE(status))
    errln((UnicodeString)"An exception was thrown during parse: " + (int32_t)status);
      //}
    }
  }
  delete dateParse;
  if (U_FAILURE(status)) errln((UnicodeString)"FAIL: UErrorCode received during test: " + (int32_t)status);
}
 
// -------------------------------------

/**
 * Test the parsing of two-digit years.
 */
void
DateFormatTest::TestTwoDigitYear()
{
    DateFormat* fmt = DateFormat::createDateInstance(DateFormat::SHORT);
    parse2DigitYear(*fmt, "6/5/17", date(117, Calendar::JUNE, 5));
    parse2DigitYear(*fmt, "6/4/34", date(34, Calendar::JUNE, 4));
    delete fmt;
}
 
// -------------------------------------
 
void
DateFormatTest::parse2DigitYear(DateFormat& fmt, const char* str, UDate expected)
{
    UErrorCode status = U_ZERO_ERROR;
    //try {
        UDate d = fmt.parse(str, status);
        UnicodeString thePat;
        logln(UnicodeString("Parsing \"") + str + "\" with " + ((SimpleDateFormat*)&fmt)->toPattern(thePat) +
            "  => " + dateToString(d));
        if (d != expected) errln((UnicodeString)"FAIL: Expected " + expected);
    //}
    //catch(ParseException e) {
        if (U_FAILURE(status))
        errln((UnicodeString)"FAIL: Got exception");
    //}
}
 
// -------------------------------------

/**
 * Test the formatting of time zones.
 */
void
DateFormatTest::TestDateFormatZone061()
{
    UErrorCode status = U_ZERO_ERROR;
    UDate date;
    DateFormat *formatter;
    date= 859248000000.0;
    logln((UnicodeString)"Date 1997/3/25 00:00 GMT: " + date);
    formatter = new SimpleDateFormat("dd-MMM-yyyyy HH:mm", Locale::UK, status);
    formatter->adoptTimeZone(TimeZone::createTimeZone("GMT"));
    UnicodeString temp; formatter->format(date, temp);
    logln((UnicodeString)"Formatted in GMT to: " + temp);
    //try {
        UDate tempDate = formatter->parse(temp, status);
        logln((UnicodeString)"Parsed to: " + dateToString(tempDate));
        if (tempDate != date) errln((UnicodeString)"FAIL: Expected " + dateToString(date));
    //}
    //catch(Throwable t) {
    if (U_FAILURE(status))
        errln((UnicodeString)"Date Formatter throws: " + (int32_t)status);
    //}
    delete formatter;
}
 
// -------------------------------------
 
/**
 * Test the formatting of time zones.
 */
void
DateFormatTest::TestDateFormatZone146()
{
    TimeZone *saveDefault = TimeZone::createDefault();

        //try {
    TimeZone *thedefault = TimeZone::createTimeZone("GMT");
    TimeZone::setDefault(*thedefault);
            // java.util.Locale.setDefault(new java.util.Locale("ar", "", ""));

            // check to be sure... its GMT all right
TimeZone *testdefault = TimeZone::createDefault();
        UnicodeString testtimezone;
        testdefault->getID(testtimezone);
        if (testtimezone == "GMT")
            logln("Test timezone = " + testtimezone);
        else
            errln("Test timezone should be GMT, not " + testtimezone);

        UErrorCode status = U_ZERO_ERROR;
        // now try to use the default GMT time zone
        GregorianCalendar *greenwichcalendar =
            new GregorianCalendar(1997, 3, 4, 23, 0, status);
        failure(status, "new GregorianCalendar");
        //*****************************greenwichcalendar.setTimeZone(TimeZone.getDefault());
        //greenwichcalendar.set(1997, 3, 4, 23, 0);
        // try anything to set hour to 23:00 !!!
        greenwichcalendar->set(Calendar::HOUR_OF_DAY, 23);
        // get time
        UDate greenwichdate = greenwichcalendar->getTime(status);
        // format every way
        UnicodeString DATA [] = {
            UnicodeString("simple format:  "), UnicodeString("04/04/97 23:00 GMT"),
                UnicodeString("MM/dd/yy HH:mm z"),
            UnicodeString("full format:    "), UnicodeString("Friday, April 4, 1997 11:00:00 o'clock PM GMT"),
                UnicodeString("EEEE, MMMM d, yyyy h:mm:ss 'o''clock' a z"),
            UnicodeString("long format:    "), UnicodeString("April 4, 1997 11:00:00 PM GMT"),
                UnicodeString("MMMM d, yyyy h:mm:ss a z"),
            UnicodeString("default format: "), UnicodeString("04-Apr-97 11:00:00 PM"),
                UnicodeString("dd-MMM-yy h:mm:ss a"),
            UnicodeString("short format:   "), UnicodeString("4/4/97 11:00 PM"),
                UnicodeString("M/d/yy h:mm a")
        };
        int32_t DATA_length = sizeof(DATA) / sizeof(DATA[0]);

        for (int32_t i=0; i<DATA_length; i+=3) {
            DateFormat *fmt = new SimpleDateFormat(DATA[i+2], Locale::ENGLISH, status);
            if(failure(status, "new SimpleDateFormat")) break;
            fmt->setCalendar(*greenwichcalendar);
            UnicodeString result;
            result = fmt->format(greenwichdate, result);
            logln(DATA[i] + result);
            if (result != DATA[i+1]) 
                errln("FAIL: Expected " + DATA[i+1] + ", got " + result);
        }
    //}
    //finally {
        TimeZone::adoptDefault(saveDefault);
    //}


}
 
// -------------------------------------
 
/**
 * Test the formatting of dates in different locales.
 */
void
DateFormatTest::TestLocaleDateFormat() // Bug 495
{
    UDate testDate = date(97, Calendar::SEPTEMBER, 15);
    DateFormat *dfFrench = DateFormat::createDateTimeInstance(DateFormat::FULL, 
        DateFormat::FULL, Locale::FRENCH);
    DateFormat *dfUS = DateFormat::createDateTimeInstance(DateFormat::FULL, 
        DateFormat::FULL, Locale::US);
    UnicodeString expectedFRENCH ( "lundi 15 septembre 1997 00 h 00 GMT-07:00" );
    //UnicodeString expectedUS ( "Monday, September 15, 1997 12:00:00 o'clock AM PDT" );
    UnicodeString expectedUS ( "Monday, September 15, 1997 12:00:00 AM PDT" );
    logln((UnicodeString)"Date set to : " + dateToString(testDate));
    UnicodeString out; 
    dfFrench->format(testDate, out);
    logln((UnicodeString)"Date Formated with French Locale " + out);
    if (!(out == expectedFRENCH))
        errln((UnicodeString)"FAIL: Expected " + expectedFRENCH);
    out.truncate(0);
    dfUS->format(testDate, out);
    logln((UnicodeString)"Date Formated with US Locale " + out);
    if (!(out == expectedUS))
        errln((UnicodeString)"FAIL: Expected " + expectedUS);
    delete dfUS;
    delete dfFrench;
}

//eof
