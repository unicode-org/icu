
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
 
#include "dtfmrgts.h"

#include "timezone.h"
#include "gregocal.h"
#include "smpdtfmt.h"
#include "datefmt.h"
#include "simpletz.h"
#include "resbund.h"

// *****************************************************************************
// class DateFormatRegressionTest
// *****************************************************************************

#define CASE(id,test) case id: name = #test; if (exec) { logln(#test "---"); logln((UnicodeString)""); test(); } break;

void 
DateFormatRegressionTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    // if (exec) logln((UnicodeString)"TestSuite DateFormatRegressionTest");
    switch (index) {
        CASE(0,Test4029195)
        CASE(1,Test4052408)
        CASE(2,Test4056591)
        CASE(3,Test4059917)
        CASE(4,Test4060212)
        CASE(5,Test4061287)
        CASE(6,Test4065240)
        CASE(7,Test4071441)
        CASE(8,Test4073003)
        CASE(9,Test4089106)
        CASE(10,Test4100302)
        CASE(11,Test4101483)
        CASE(12,Test4103340)
        CASE(13,Test4103341)
        CASE(14,Test4104136)
        CASE(15,Test4104522)
        CASE(16,Test4106807)
        CASE(17,Test4108407) 
        CASE(18,Test4134203)
        CASE(19,Test4151631)
        CASE(20,Test4151706)
        CASE(21,Test4162071)

        default: name = ""; break;
    }
}

/**
 * @bug 4029195
 */
void DateFormatRegressionTest::Test4029195() 
{
    UErrorCode status = ZERO_ERROR;

    UDate today = Calendar::getNow();
    logln((UnicodeString) "today: " + today);

    SimpleDateFormat *sdf = (SimpleDateFormat*) DateFormat::createDateInstance();
    failure(status, "SimpleDateFormat::createDateInstance");
    UnicodeString pat;
    pat = sdf->toPattern(pat);
    logln("pattern: " + pat);
    UnicodeString fmtd;
    FieldPosition pos(FieldPosition::DONT_CARE);
    fmtd = sdf->format(today, fmtd, pos);
    logln("today: " + fmtd);

    sdf->applyPattern("G yyyy DDD");
    UnicodeString todayS;
    todayS = sdf->format(today, todayS, pos);
    logln("today: " + todayS);
    //try {
        today = sdf->parse(todayS, status);
        failure(status, "sdf->parse");
        logln((UnicodeString)"today date: " + today);
    /*} catch(Exception e) {
        logln("Error reparsing date: " + e.getMessage());
    }*/

    //try {
        UnicodeString rt;
        rt = sdf->format(sdf->parse(todayS, status), rt, pos);
        failure(status, "sdf->parse");
        logln("round trip: " + rt);
        if(rt != todayS) 
            errln("Fail: Want " + todayS + " Got " + rt);
    /*}
    catch (ParseException e) {
        errln("Fail: " + e);
        e.printStackTrace();
    }*/

    delete sdf;
}

/**
 * @bug 4052408
 */
void DateFormatRegressionTest::Test4052408() 
{

    DateFormat *fmt = DateFormat::createDateTimeInstance(DateFormat::SHORT,
                                                DateFormat::SHORT, Locale::US);
    UDate dt = date(97, Calendar::MAY, 3, 8, 55);
    UnicodeString str;
    str = fmt->format(dt, str);
    logln(str);
    
    if(str != "5/3/97 8:55 AM")
        errln("Fail: Test broken; Want 5/3/97 8:55 AM Got " + str);   
    
    UnicodeString expected[] = {
        (UnicodeString) "", //"ERA_FIELD",
        (UnicodeString) "97", //"YEAR_FIELD",
        (UnicodeString) "5", //"MONTH_FIELD",
        (UnicodeString) "3", //"DATE_FIELD",
        (UnicodeString) "", //"HOUR_OF_DAY1_FIELD",
        (UnicodeString) "", //"HOUR_OF_DAY0_FIELD",
        (UnicodeString) "55", //"MINUTE_FIELD",
        (UnicodeString) "", //"SECOND_FIELD",
        (UnicodeString) "", //"MILLISECOND_FIELD",
        (UnicodeString) "", //"DAY_OF_WEEK_FIELD",
        (UnicodeString) "", //"DAY_OF_YEAR_FIELD",
        (UnicodeString) "", //"DAY_OF_WEEK_IN_MONTH_FIELD",
        (UnicodeString) "", //"WEEK_OF_YEAR_FIELD",
        (UnicodeString) "", //"WEEK_OF_MONTH_FIELD",
        (UnicodeString) "AM", //"AM_PM_FIELD",
        (UnicodeString) "8", //"HOUR1_FIELD",
        (UnicodeString) "", //"HOUR0_FIELD",
        (UnicodeString) "" //"TIMEZONE_FIELD"
    };
    
    //Hashtable expected;// = new Hashtable();
    //expected.put(new LongKey(DateFormat.MONTH_FIELD), "5");
    //expected.put(new LongKey(DateFormat.DATE_FIELD), "3");
    //expected.put(new LongKey(DateFormat.YEAR_FIELD), "97");
    //expected.put(new LongKey(DateFormat.HOUR1_FIELD), "8");
    //expected.put(new LongKey(DateFormat.MINUTE_FIELD), "55");
    //expected.put(new LongKey(DateFormat.AM_PM_FIELD), "AM");
    
    //StringBuffer buf = new StringBuffer();
    UnicodeString fieldNames[] = {
        (UnicodeString) "ERA_FIELD",
        (UnicodeString) "YEAR_FIELD",
        (UnicodeString) "MONTH_FIELD",
        (UnicodeString) "DATE_FIELD",
        (UnicodeString) "HOUR_OF_DAY1_FIELD",
        (UnicodeString) "HOUR_OF_DAY0_FIELD",
        (UnicodeString) "MINUTE_FIELD",
        (UnicodeString) "SECOND_FIELD",
        (UnicodeString) "MILLISECOND_FIELD",
        (UnicodeString) "DAY_OF_WEEK_FIELD",
        (UnicodeString) "DAY_OF_YEAR_FIELD",
        (UnicodeString) "DAY_OF_WEEK_IN_MONTH_FIELD",
        (UnicodeString) "WEEK_OF_YEAR_FIELD",
        (UnicodeString) "WEEK_OF_MONTH_FIELD",
        (UnicodeString) "AM_PM_FIELD",
        (UnicodeString) "HOUR1_FIELD",
        (UnicodeString) "HOUR0_FIELD",
        (UnicodeString) "TIMEZONE_FIELD"
    };

    bool_t pass = TRUE;
    for(int i = 0; i <= 17; ++i) {
        FieldPosition pos(i);
        UnicodeString buf;
        fmt->format(dt, buf, pos);
        //char[] dst = new char[pos.getEndIndex() - pos.getBeginIndex()];
        UnicodeString dst;
    buf.extractBetween(pos.getBeginIndex(), pos.getEndIndex(), dst);
        UnicodeString str(dst);
        log(i + ": " + fieldNames[i] +
                         ", \"" + str + "\", " +
                         pos.getBeginIndex() + ", " +
                         pos.getEndIndex());
        UnicodeString exp = expected[i];
        if((exp.size() == 0 && str.size() == 0) || str == exp)
            logln(" ok");
        else {
            logln(UnicodeString(" expected ") + exp);
            pass = FALSE;
        }
    
    }
    if( ! pass) 
        errln("Fail: FieldPosition not set right by DateFormat");

    delete fmt;
}

/**
 * @bug 4056591
 * Verify the function of the [s|g]et2DigitYearStart() API.
 */
void DateFormatRegressionTest::Test4056591() 
{
    UErrorCode status = ZERO_ERROR;

    //try {
        SimpleDateFormat *fmt = new SimpleDateFormat("yyMMdd", Locale::US, status);
        failure(status, "new SimpleDateFormat");
        UDate start = date(1809-1900, Calendar::DECEMBER, 25);
        fmt->set2DigitYearStart(start, status);
        failure(status, "fmt->setTwoDigitStartDate");
        if( (fmt->get2DigitYearStart(status) != start) || failure(status, "get2DigitStartDate"))
            errln("get2DigitYearStart broken");
        UDate dates [] = {
            date(1809-1900, Calendar::DECEMBER, 25),
            date(1909-1900, Calendar::DECEMBER, 24),
            date(1809-1900, Calendar::DECEMBER, 26),
            date(1861-1900, Calendar::DECEMBER, 25),
        };

        UnicodeString strings [] = {
            (UnicodeString) "091225",
            (UnicodeString) "091224",
            (UnicodeString) "091226",
            (UnicodeString) "611225"
        };

        /*Object[] DATA = {
            "091225", new Date(1809-1900, Calendar.DECEMBER, 25),
            "091224", new Date(1909-1900, Calendar.DECEMBER, 24),
            "091226", new Date(1809-1900, Calendar.DECEMBER, 26),
            "611225", new Date(1861-1900, Calendar.DECEMBER, 25),
        };*/

        for(int i = 0; i < 4; i++) {
            UnicodeString s = strings[i];
            UDate exp = dates[i];
            UDate got = fmt->parse(s, status);
            failure(status, "fmt->parse");
            logln(s + " -> " + got + "; exp " + exp);
            if(got != exp) 
                errln("set2DigitYearStart broken");
        }
    /*}
    catch (ParseException e) {
        errln("Fail: " + e);
        e.printStackTrace();
    }*/

    delete fmt;
}

/**
 * @bug 4059917
 */
void DateFormatRegressionTest::Test4059917() 
{
    UErrorCode status = ZERO_ERROR;
    
    SimpleDateFormat *fmt;
    UnicodeString myDate;

    fmt = new SimpleDateFormat( "yyyy/MM/dd", status );
    failure(status, "new SimpleDateFormat");
    myDate = "1997/01/01";
    aux917( fmt, myDate );
    
    delete fmt;
    fmt = NULL;
    
    fmt = new SimpleDateFormat( "yyyyMMdd", status );
    failure(status, "new SimpleDateFormat");
    myDate = "19970101";
    aux917( fmt, myDate );
              
    delete fmt;
}

void DateFormatRegressionTest::aux917( SimpleDateFormat *fmt, UnicodeString& str ) {
    //try {
    UnicodeString pat;
    pat = fmt->toPattern(pat);
    logln( "==================" );
    logln( "testIt: pattern=" + pat +
               " string=" + str );
                
        
    Formattable o;
    //Object o;
    ParsePosition pos(0);
    fmt->parseObject( str, o, pos );
    //logln( UnicodeString("Parsed object: ") + o );
    
    UErrorCode status = ZERO_ERROR;
    UnicodeString formatted;
    FieldPosition poss(FieldPosition::DONT_CARE);
    formatted = fmt->format( o, formatted, poss, status );
    failure(status, "fmt->format");
    logln( "Formatted string: " + formatted );
    if( formatted != str) 
        errln("Fail: Want " + str + " Got " + formatted);
    /*}
    catch (ParseException e) {
        errln("Fail: " + e);
        e.printStackTrace();
    }*/
}

/**
 * @bug 4060212
 */
void DateFormatRegressionTest::Test4060212() 
{
    UnicodeString dateString = "1995-040.05:01:29";

    logln( "dateString= " + dateString );
    logln("Using yyyy-DDD.hh:mm:ss");
    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat *formatter = new SimpleDateFormat("yyyy-DDD.hh:mm:ss", status);
    failure(status, "new SimpleDateFormat");
    ParsePosition pos(0);
    UDate myDate = formatter->parse( dateString, pos );
    UnicodeString myString;
    DateFormat *fmt = DateFormat::createDateTimeInstance( DateFormat::FULL,
                                                            DateFormat::LONG);
    myString = fmt->format( myDate, myString);
    logln( myString );

    Calendar *cal = new GregorianCalendar(status);
    failure(status, "new GregorianCalendar");
    cal->setTime(myDate, status);
    failure(status, "cal->setTime");
    if ((cal->get(Calendar::DAY_OF_YEAR, status) != 40) || failure(status, "cal->get"))
        errln((UnicodeString) "Fail: Got " + cal->get(Calendar::DAY_OF_YEAR, status) +
                            " Want 40");

    logln("Using yyyy-ddd.hh:mm:ss");
    delete formatter;
    formatter = NULL;
    formatter = new SimpleDateFormat("yyyy-ddd.hh:mm:ss", status);
    failure(status, "new SimpleDateFormat");
    pos.setIndex(0);
    myDate = formatter->parse( dateString, pos );
    myString = fmt->format( myDate, myString );
    logln( myString );
    cal->setTime(myDate, status);
    failure(status, "cal->setTime");
    if ((cal->get(Calendar::DAY_OF_YEAR, status) != 40) || failure(status, "cal->get"))
        errln((UnicodeString) "Fail: Got " + cal->get(Calendar::DAY_OF_YEAR, status) +
                            " Want 40");

    delete formatter;
    delete fmt;
    delete cal;
}

/**
 * @bug 4061287
 */
void DateFormatRegressionTest::Test4061287() 
{
    UErrorCode status = ZERO_ERROR;
    
    SimpleDateFormat *df = new SimpleDateFormat("dd/MM/yyyy", status);
    failure(status, "new SimpleDateFormat");
    //try {
    logln(UnicodeString("") + df->parse("35/01/1971", status));  
    failure(status, "df->parse");
    //logln(df.parse("35/01/1971").toString());
    //}
    /*catch (ParseException e) {
        errln("Fail: " + e);
        e.printStackTrace();
    }*/
    df->setLenient(FALSE);
    bool_t ok = FALSE;
    //try {
    logln(UnicodeString("") + df->parse("35/01/1971", status));
    if(FAILURE(status))
        ok = TRUE;
    //logln(df.parse("35/01/1971").toString());
    //} catch (ParseException e) {ok=TRUE;}
    if(!ok) 
        errln("Fail: Lenient not working");
}

/**
 * @bug 4065240
 */
void DateFormatRegressionTest::Test4065240() 
{
    UDate curDate;
    DateFormat *shortdate, *fulldate;
    UnicodeString strShortDate, strFullDate;
    Locale saveLocale = Locale::getDefault();
    TimeZone *saveZone = TimeZone::createDefault();

    UErrorCode status = ZERO_ERROR;
    //try {
        Locale *curLocale = new Locale("de","DE");
        Locale::setDefault(*curLocale, status);
        failure(status, "Locale::setDefault");
        // {sfb} adoptDefault instead of setDefault
        //TimeZone::setDefault(TimeZone::createTimeZone("EST"));
        TimeZone::adoptDefault(TimeZone::createTimeZone("EST"));
        curDate = date(98, 0, 1);
        shortdate = DateFormat::createDateInstance(DateFormat::SHORT);
        fulldate = DateFormat::createDateTimeInstance(DateFormat::LONG, DateFormat::LONG);
        strShortDate = "The current date (short form) is ";
        UnicodeString temp;
        temp = shortdate->format(curDate, temp);
        strShortDate += temp;
        strFullDate = "The current date (long form) is ";
        UnicodeString temp2;
        fulldate->format(curDate, temp2);
        strFullDate += temp2;

        logln(strShortDate);
        logln(strFullDate);

        // {sfb} What to do with resource bundle stuff?????

        // Check to see if the resource is present; if not, we can't test
        ResourceBundle *bundle = new ResourceBundle(
            icu_getDefaultDataDirectory(), *curLocale, status);
        failure(status, "new ResourceBundle");
            //(UnicodeString) "java.text.resources.DateFormatZoneData", curLocale);

        // {sfb} API change to ResourceBundle -- add getLocale()
        /*if (bundle->getLocale().getLanguage(temp) == UnicodeString("de")) {
            // UPDATE THIS AS ZONE NAME RESOURCE FOR <EST> in de_DE is updated
            if (!strFullDate.endsWith(UnicodeString("GMT-05:00")))
                errln("Fail: Want GMT-05:00");
        }
        else {
            logln("*** TEST COULD NOT BE COMPLETED BECAUSE DateFormatZoneData ***");
            logln("*** FOR LOCALE de OR de_DE IS MISSING ***");
        }*/
    //}
    //finally {
    Locale::setDefault(saveLocale, status);
    failure(status, "Locale::setDefault");
    TimeZone::setDefault(*saveZone);
    //}

    delete saveZone;
}

/*
  DateFormat.equals is too narrowly defined.  As a result, MessageFormat
  does not work correctly.  DateFormat.equals needs to be written so
  that the Calendar sub-object is not compared using Calendar.equals,
  but rather compared for equivalency.  This may necessitate adding a
  (package private) method to Calendar to test for equivalency.
  
  Currently this bug breaks MessageFormat.toPattern
  */
/**
 * @bug 4071441
 */
void DateFormatRegressionTest::Test4071441() 
{
    DateFormat *fmtA = DateFormat::createInstance();
    DateFormat *fmtB = DateFormat::createInstance();
    
    // {sfb} Is it OK to cast away const here?
    Calendar *calA = (Calendar*) fmtA->getCalendar();
    Calendar *calB = (Calendar*) fmtB->getCalendar();
    UDate epoch = date(0, 0, 0);
    UDate xmas = date(61, Calendar::DECEMBER, 25);

    UErrorCode status = ZERO_ERROR;
    calA->setTime(epoch, status);
    failure(status, "calA->setTime");
    calB->setTime(epoch, status);
    failure(status, "calB->setTime");
    if (*calA != *calB)
        errln("Fail: Can't complete test; Calendar instances unequal");
    if (*fmtA != *fmtB)
        errln("Fail: DateFormat unequal when Calendars equal");
    calB->setTime(xmas, status);
    failure(status, "calB->setTime");
    if (*calA == *calB)
        errln("Fail: Can't complete test; Calendar instances equal");
    if (*fmtA != *fmtB)
        errln("Fail: DateFormat unequal when Calendars equivalent");
    logln("DateFormat.equals ok");

    delete fmtA;
    delete fmtB;
}

/* The java.text.DateFormat.parse(String) method expects for the
  US locale a string formatted according to mm/dd/yy and parses it
  correctly.

  When given a string mm/dd/yyyy it only parses up to the first
  two y's, typically resulting in a date in the year 1919.
  
  Please extend the parsing method(s) to handle strings with
  four-digit year values (probably also applicable to various
  other locales.  */
/**
 * @bug 4073003
 */
void DateFormatRegressionTest::Test4073003() 
{
    //try {
    DateFormat *fmt = DateFormat::createDateInstance(DateFormat::SHORT, Locale::US);
        UnicodeString tests [] = { 
            (UnicodeString) "12/25/61", 
            (UnicodeString) "12/25/1961", 
            (UnicodeString) "4/3/2010", 
            (UnicodeString) "4/3/10" 
        };
        UErrorCode status = ZERO_ERROR;
        for(int i= 0; i < 4; i+=2) {
            UDate d = fmt->parse(tests[i], status);
            failure(status, "fmt->parse");
            UDate dd = fmt->parse(tests[i+1], status);
            failure(status, "fmt->parse");
            UnicodeString s;
            s = fmt->format(d, s);
            UnicodeString ss;
            ss = fmt->format(dd, ss);
            if (d != dd)
                errln((UnicodeString) "Fail: " + d + " != " + dd);
            if (s != ss)
                errln((UnicodeString)"Fail: " + s + " != " + ss);
            logln("Ok: " + s + " " + d);
        }
    /*}
    catch (ParseException e) {
        errln("Fail: " + e);
        e.printStackTrace();
    }*/
}

/**
 * @bug 4089106
 */
void DateFormatRegressionTest::Test4089106() 
{
    TimeZone *def = TimeZone::createDefault();
    //try {
        TimeZone *z = new SimpleTimeZone((int)(1.25 * 3600000), "FAKEZONE");
        TimeZone::setDefault(*z);
        UErrorCode status = ZERO_ERROR;
        SimpleDateFormat *f = new SimpleDateFormat(status);
        failure(status, "new SimpleDateFormat");
        if (f->getTimeZone()!= *z)
            errln("Fail: SimpleTimeZone should use TimeZone.getDefault()");
        
        //}
    //finally {
        TimeZone::setDefault(*def);
    //}

    delete z;
    delete f;
    delete def;
}

/**
 * @bug 4100302
 */

// {sfb} not applicable in C++??

void DateFormatRegressionTest::Test4100302() 
{
/*    Locale locales [] =  {
        Locale::CANADA,
        Locale::CANADA_FRENCH,
        Locale::CHINA,
        Locale::CHINESE,
        Locale::ENGLISH,
        Locale::FRANCE,
        Locale::FRENCH,
        Locale::GERMAN,
        Locale::GERMANY,
        Locale::ITALIAN,
        Locale::ITALY,
        Locale::JAPAN,
        Locale::JAPANESE,
        Locale::KOREA,
        Locale::KOREAN,
        Locale::PRC,
        Locale::SIMPLIFIED_CHINESE,
        Locale::TAIWAN,
        Locale::TRADITIONAL_CHINESE,
        Locale::UK,
        Locale::US
        };
    //try {
        bool_t pass = TRUE;
        for(int i = 0; i < 21; i++) {

            Format *format = DateFormat::createDateTimeInstance(DateFormat::FULL,
                DateFormat::FULL, locales[i]);
            byte[] bytes;
        
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
        
            oos.writeObject(format);
            oos.flush();
        
            baos.close();
            bytes = baos.toByteArray();
        
            ObjectInputStream ois =
                new ObjectInputStream(new ByteArrayInputStream(bytes));
        
            if (!format.equals(ois.readObject())) {
                pass = FALSE;
                logln("DateFormat instance for locale " +
                      locales[i] + " is incorrectly serialized/deserialized.");
            } else {
                logln("DateFormat instance for locale " +
                      locales[i] + " is OKAY.");
            }
        }
        if (!pass) errln("Fail: DateFormat serialization/equality bug");      
    }
    catch (IOException e) {
        errln("Fail: " + e);
        e.printStackTrace();
    }
    catch (ClassNotFoundException e) {
        errln("Fail: " + e);
        e.printStackTrace();
    }
*/}

/**
 * @bug 4101483
 */
void DateFormatRegressionTest::Test4101483() 
{
    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat *sdf = new SimpleDateFormat("z", Locale::US, status);
    failure(status, "new SimpleDateFormat");
    FieldPosition fp(DateFormat::TIMEZONE_FIELD);
    //Date d = date(9234567890L);
    UDate d = 9234567890.0;
    //StringBuffer buf = new StringBuffer("");
    UnicodeString buf;
    sdf->format(d, buf, fp);
    //logln(sdf.format(d, buf, fp).toString());
    logln(dateToString(d) + " => " + buf);
    logln("beginIndex = " + fp.getBeginIndex());
    logln("endIndex = " + fp.getEndIndex());
    if (fp.getBeginIndex() == fp.getEndIndex()) 
        errln("Fail: Empty field");

    delete sdf;
}

/**
 * @bug 4103340
 * @bug 4138203
 * This bug really only works in Locale.US, since that's what the locale
 * used for Date.toString() is.  Bug 4138203 reports that it fails on Korean
 * NT; it would actually have failed on any non-US locale.  Now it should
 * work on all locales.
 */
void DateFormatRegressionTest::Test4103340() 
{
    UErrorCode status = ZERO_ERROR;

    // choose a date that is the FIRST of some month 
    // and some arbitrary time 
    UDate d = date(97, 3, 1, 1, 1, 1); 
    SimpleDateFormat *df = new SimpleDateFormat("MMMM", Locale::US, status); 
    failure(status, "new SimpleDateFormat");

    UnicodeString s;
    s = dateToString(d, s);
    UnicodeString s2;
    FieldPosition pos(FieldPosition::DONT_CARE);
    s2 = df->format(d, s2, pos);
    logln("Date=" + s); 
    logln("DF=" + s2);
    UnicodeString substr;
    s2.extract(0,2, substr);
    if (s.indexOf(substr) == -1)
      errln("Months should match");
    
    delete df;
}

/**
 * @bug 4103341
 */
void DateFormatRegressionTest::Test4103341() 
{
    TimeZone *saveZone  =TimeZone::createDefault();
    //try {
        
    // {sfb} changed from setDefault to adoptDefault
    TimeZone::adoptDefault(TimeZone::createTimeZone("CST"));
    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat *simple = new SimpleDateFormat("MM/dd/yyyy HH:mm", status);
    failure(status, "new SimpleDateFormat");
    TimeZone *temp = TimeZone::createDefault();
    if(simple->getTimeZone() != *temp)
            errln("Fail: SimpleDateFormat not using default zone");
    //}
    //finally {
        TimeZone::adoptDefault(saveZone);
    //}

    delete temp;
    delete simple;
}

/**
 * @bug 4104136
 */
void DateFormatRegressionTest::Test4104136() 
{
    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat *sdf = new SimpleDateFormat(status); 
    failure(status, "new SimpleDateFormat");
    UnicodeString pattern = "'time' hh:mm"; 
    sdf->applyPattern(pattern); 
    logln("pattern: \"" + pattern + "\""); 

    UnicodeString strings [] = {
        (UnicodeString)"time 10:30",
        (UnicodeString) "time 10:x",
        (UnicodeString) "time 10x"
    };

    ParsePosition ppos [] = {
        ParsePosition(10),
        ParsePosition(0),
        ParsePosition(0)
    };

    UDate dates [] = {
        date(70, Calendar::JANUARY, 1, 10, 30),
        -1,
        -1
    };

    /*Object[] DATA = {
        "time 10:30", new ParsePosition(10), new Date(70, Calendar.JANUARY, 1, 10, 30),
        "time 10:x", new ParsePosition(0), null,
        "time 10x", new ParsePosition(0), null,
    };*/
    
    for(int i = 0; i < 3; i++) {
        UnicodeString text = strings[i];
        ParsePosition finish = ppos[i];
        UDate exp = dates[i];
        
        ParsePosition pos(0);
        UDate d = sdf->parse(text, pos);
        logln(" text: \"" + text + "\""); 
        logln(" index: " + pos.getIndex()); 
        logln((UnicodeString) " result: " + d);
        if(pos.getIndex() != finish.getIndex())
            errln("Fail: Expected pos " + finish.getIndex());
        if (! ((d == 0 && exp == -1) || (d == exp)))
            errln((UnicodeString) "Fail: Expected result " + exp);
    }

    delete sdf;
}

/**
 * @bug 4104522
 * CANNOT REPRODUCE
 * According to the bug report, this test should throw a
 * StringIndexOutOfBoundsException during the second parse.  However,
 * this is not seen.
 */
void DateFormatRegressionTest::Test4104522() 
{
    UErrorCode status = ZERO_ERROR;
    
    SimpleDateFormat *sdf = new SimpleDateFormat(status);
    failure(status, "new SimpleDateFormat");
    UnicodeString pattern = "'time' hh:mm";
    sdf->applyPattern(pattern);
    logln("pattern: \"" + pattern + "\"");

    // works correctly
    ParsePosition pp(0);
    UnicodeString text = "time ";
    UDate dt = sdf->parse(text, pp);
    logln(" text: \"" + text + "\"" +
          " date: " + dt);

    // works wrong
    pp.setIndex(0);
    text = "time";
    dt = sdf->parse(text, pp);
    logln(" text: \"" + text + "\"" +
          " date: " + dt);
}

/**
 * @bug 4106807
 */
void DateFormatRegressionTest::Test4106807() 
{
    UDate dt; 
    DateFormat *df = DateFormat::createDateTimeInstance(); 
    
    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat *sdfs [] = {
        new SimpleDateFormat("yyyyMMddHHmmss", status),
        new SimpleDateFormat("yyyyMMddHHmmss'Z'", status),
        new SimpleDateFormat("yyyyMMddHHmmss''", status),
        new SimpleDateFormat("yyyyMMddHHmmss'a''a'", status),
        new SimpleDateFormat("yyyyMMddHHmmss %", status)
    };
    failure(status, "new SimpleDateFormat");
    
    UnicodeString strings [] = {
        (UnicodeString) "19980211140000",
        (UnicodeString) "19980211140000",
        (UnicodeString) "19980211140000",
        (UnicodeString) "19980211140000a",
        (UnicodeString) "19980211140000 "
    };

    /*Object[] data = {
        new SimpleDateFormat("yyyyMMddHHmmss"),       "19980211140000",
        new SimpleDateFormat("yyyyMMddHHmmss'Z'"),    "19980211140000",
        new SimpleDateFormat("yyyyMMddHHmmss''"),     "19980211140000",
        new SimpleDateFormat("yyyyMMddHHmmss'a''a'"), "19980211140000a",
        new SimpleDateFormat("yyyyMMddHHmmss %"),     "19980211140000 ",
    };*/
    GregorianCalendar *gc = new GregorianCalendar(status);
    failure(status, "new GregorianCalendar");
    TimeZone *timeZone = TimeZone::createDefault(); 

    TimeZone *gmt = timeZone->clone(); 

    gmt->setRawOffset(0); 

    for(int32_t i = 0; i < 5; i++) {
        SimpleDateFormat *format = sdfs[i];
        UnicodeString dateString = strings[i];
        //try {
            format->setTimeZone(*gmt); 
            dt = format->parse(dateString, status);
            // {sfb} some of these parses will fail purposely
            if(FAILURE(status))
                break;
            status = ZERO_ERROR;
            UnicodeString fmtd;
            FieldPosition pos(FieldPosition::DONT_CARE);
            fmtd = df->format(dt, fmtd, pos);
            logln(fmtd);
            //logln(df->format(dt)); 
            gc->setTime(dt, status); 
            failure(status, "gc->getTime");
            logln(UnicodeString("") + gc->get(Calendar::ZONE_OFFSET, status));
            failure(status, "gc->get");
            UnicodeString s;
            s = format->format(dt, s, pos);
            logln(s); 
        /*}
        catch (ParseException e) { 
            logln("No way Jose"); 
        }*/ 
    } 

    delete timeZone;
    delete df;
    for(int32_t j = 0; j < 5; j++)
        delete sdfs [j];
     delete gc;
    delete gmt;
}

/*
  Synopsis: Chinese time zone CTT is not recogonized correctly.
  Description: Platform Chinese Windows 95 - ** Time zone set to CST ** 
  */
/**
 * @bug 4108407
 */

// {sfb} what to do with this one ?? 
void DateFormatRegressionTest::Test4108407() 
{ 
    /*long l = System.currentTimeMillis(); 
    logln("user.timezone = " + System.getProperty("user.timezone", "?"));
    logln("Time Zone :" + 
                       DateFormat.getDateInstance().getTimeZone().getID()); 
    logln("Default format :" + 
                       DateFormat.getDateInstance().format(new Date(l))); 
    logln("Full format :" + 
                       DateFormat.getDateInstance(DateFormat.FULL).format(new 
                                                                          Date(l))); 
    logln("*** Set host TZ to CST ***");
    logln("*** THE RESULTS OF THIS TEST MUST BE VERIFIED MANUALLY ***");*/
} 

/**
 * @bug 4134203
 * SimpleDateFormat won't parse "GMT"
 */
void DateFormatRegressionTest::Test4134203() 
{
    UErrorCode status = ZERO_ERROR;
    UnicodeString dateFormat = "MM/dd/yy HH:mm:ss zzz";
    SimpleDateFormat *fmt = new SimpleDateFormat(dateFormat, status);
    failure(status, "new SimpleDateFormat");
    ParsePosition p0(0);
    UDate d = fmt->parse("01/22/92 04:52:00 GMT", p0);
    logln(dateToString(d));
    if(p0 == ParsePosition(0))
        errln("Fail: failed to parse 'GMT'");
    // In the failure case an exception is thrown by parse();
    // if no exception is thrown, the test passes.

    delete fmt;
}

/**
 * @bug 4151631
 * SimpleDateFormat incorrect handling of 2 single quotes in format()
 */
void DateFormatRegressionTest::Test4151631() 
{
    UnicodeString pattern = "'TO_DATE('''dd'-'MM'-'yyyy HH:mm:ss''' , ''DD-MM-YYYY HH:MI:SS'')'";
    logln("pattern=" + pattern);
    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat *format = new SimpleDateFormat(pattern, Locale::US, status);
    failure(status, "new SimpleDateFormat");
    UnicodeString result;
    FieldPosition pos(FieldPosition::DONT_CARE);
    result = format->format(date(1998-1900, Calendar::JUNE, 30, 13, 30, 0), result, pos);
    if (result != "TO_DATE('30-06-1998 13:30:00' , 'DD-MM-YYYY HH:MI:SS')") {
        errln("Fail: result=" + result);
    }
    else {
        logln("Pass: result=" + result);
    }

    delete format;
}

/**
 * @bug 4151706
 * 'z' at end of date format throws index exception in SimpleDateFormat
 * CANNOT REPRODUCE THIS BUG ON 1.2FCS
 */
void DateFormatRegressionTest::Test4151706() 
{
    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat *fmt =
        new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", Locale::US, status);
    failure(status, "new SimpleDateFormat");
    //try {
        UDate d = fmt->parse("Thursday, 31-Dec-98 23:00:00 GMT", status);
        failure(status, "fmt->parse");
       // {sfb} what about next two lines?
        //if (d.getTime() != Date.UTC(1998-1900, Calendar.DECEMBER, 31, 23, 0, 0))
        //    errln("Incorrect value: " + d);
    /*} catch (Exception e) {
        errln("Fail: " + e);
    }*/
}

/**
 * @bug 4162071
 * Cannot reproduce this bug under 1.2 FCS -- it may be a convoluted duplicate
 * of some other bug that has been fixed.
 */
void 
DateFormatRegressionTest::Test4162071() 
{
    UnicodeString dateString("Thu, 30-Jul-1999 11:51:14 GMT");
    UnicodeString format("EEE', 'dd-MMM-yyyy HH:mm:ss z"); // RFC 822/1123
    UErrorCode status = ZERO_ERROR;
    SimpleDateFormat df(format, Locale::US, status);
    if(FAILURE(status))
        errln("Couldn't create SimpleDateFormat");
    
    //try {
        UDate x = df.parse(dateString, status);
        if(SUCCESS(status))
            logln("Parse format \"" + format + "\" ok");
        else
            errln("Parse format \"" + format + "\" failed.");
        UnicodeString temp;
        FieldPosition pos(0);
        logln(dateString + " -> " + df.format(x, temp, pos));
    //} catch (Exception e) {
    //    errln("Parse format \"" + format + "\" failed.");
    //}
}

//eof
