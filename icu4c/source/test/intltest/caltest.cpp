/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

 
#include "caltest.h"
#include "unicode/gregocal.h"
#include "unicode/smpdtfmt.h"
#include "unicode/simpletz.h"
 
// *****************************************************************************
// class CalendarTest
// *****************************************************************************

void CalendarTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite TestCalendar");
    switch (index) {
        case 0:
            name = "TestDOW943";
            if (exec) {
                logln("TestDOW943---"); logln("");
                TestDOW943();
            }
            break;
        case 1:
            name = "TestClonesUnique908";
            if (exec) {
                logln("TestClonesUnique908---"); logln("");
                TestClonesUnique908();
            }
            break;
        case 2:
            name = "TestGregorianChange768";
            if (exec) {
                logln("TestGregorianChange768---"); logln("");
                TestGregorianChange768();
            }
            break;
        case 3:
            name = "TestDisambiguation765";
            if (exec) {
                logln("TestDisambiguation765---"); logln("");
                TestDisambiguation765();
            }
            break;
        case 4:
            name = "TestGMTvsLocal4064654";
            if (exec) {
                logln("TestGMTvsLocal4064654---"); logln("");
                TestGMTvsLocal4064654();
            }
            break;
        case 5:
            name = "TestAddSetOrder621";
            if (exec) {
                logln("TestAddSetOrder621---"); logln("");
                TestAddSetOrder621();
            }
            break;
        case 6:
            name = "TestAdd520";
            if (exec) {
                logln("TestAdd520---"); logln("");
                TestAdd520();
            }
            break;
        case 7:
            name = "TestFieldSet4781";
            if (exec) {
                logln("TestFieldSet4781---"); logln("");
                TestFieldSet4781();
            }
            break;
        case 8:
            name = "TestSerialize337";
            if (exec) {
                logln("TestSerialize337---"); logln("");
            //  TestSerialize337();
            }
            break;
        case 9:
            name = "TestSecondsZero121";
            if (exec) {
                logln("TestSecondsZero121---"); logln("");
                TestSecondsZero121();
            }
            break;
        case 10:
            name = "TestAddSetGet0610";
            if (exec) {
                logln("TestAddSetGet0610---"); logln("");
                TestAddSetGet0610();
            }
            break;
        case 11:
            name = "TestFields060";
            if (exec) {
                logln("TestFields060---"); logln("");
                TestFields060();
            }
            break;
        case 12:
            name = "TestEpochStartFields";
            if (exec) {
                logln("TestEpochStartFields---"); logln("");
                TestEpochStartFields();
            }
            break;
        case 13:
            name = "TestDOWProgression";
            if (exec) {
                logln("TestDOWProgression---"); logln("");
                TestDOWProgression();
            }
            break;
        case 14:
            name = "TestGenericAPI";
            if (exec) {
                logln("TestGenericAPI---"); logln("");
                TestGenericAPI();
            }
            break;
        case 15:
            name = "TestAddRollExtensive";
            if (exec) {
                logln("TestAddRollExtensive---"); logln("");
                TestAddRollExtensive();
            }
            break;
        default: name = ""; break;
    }
}

// ---------------------------------------------------------------------------------

/**
 * Test various API methods for API completeness.
 */
void
CalendarTest::TestGenericAPI()
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString str;

    UDate when = date(90, Calendar::APRIL, 15);

    UnicodeString tzid("TestZone");
    int32_t tzoffset = 123400;

    SimpleTimeZone *zone = new SimpleTimeZone(tzoffset, tzid);
    Calendar *cal = Calendar::createInstance(zone->clone(), status);
    if (failure(status, "Calendar::createInstance")) return;

    if (*zone != cal->getTimeZone()) errln("FAIL: Calendar::getTimeZone failed");

    Calendar *cal2 = Calendar::createInstance(cal->getTimeZone(), status);
    if (failure(status, "Calendar::createInstance")) return;
    cal->setTime(when, status);
    cal2->setTime(when, status);
    if (failure(status, "Calendar::setTime")) return;
    
    if (!(*cal == *cal2)) errln("FAIL: Calendar::operator== failed");
    if ((*cal != *cal2))  errln("FAIL: Calendar::operator!= failed");
    if (!cal->equals(*cal2, status) ||
        cal->before(*cal2, status) ||
        cal->after(*cal2, status) ||
        U_FAILURE(status)) errln("FAIL: equals/before/after failed");

    cal2->setTime(when + 1000, status);
    if (failure(status, "Calendar::setTime")) return;
    if (cal->equals(*cal2, status) ||
        cal2->before(*cal, status) ||
        cal->after(*cal2, status) ||
        U_FAILURE(status)) errln("FAIL: equals/before/after failed");

    cal->roll(Calendar::SECOND, (bool_t) TRUE, status);
    if (failure(status, "Calendar::roll")) return;
    if (!cal->equals(*cal2, status) ||
        cal->before(*cal2, status) ||
        cal->after(*cal2, status) ||
        U_FAILURE(status)) errln("FAIL: equals/before/after failed");

    // Roll back to January
    cal->roll(Calendar::MONTH, (int32_t)(1 + Calendar::DECEMBER - cal->get(Calendar::MONTH, status)), status);
    if (failure(status, "Calendar::roll")) return;
    if (cal->equals(*cal2, status) ||
        cal2->before(*cal, status) ||
        cal->after(*cal2, status) ||
        U_FAILURE(status)) errln("FAIL: equals/before/after failed");

    TimeZone *z = cal->orphanTimeZone();
    if (z->getID(str) != tzid ||
        z->getRawOffset() != tzoffset)
        errln("FAIL: orphanTimeZone failed");

    int32_t i;
    for (i=0; i<2; ++i)
    {
        bool_t lenient = ( i > 0 );
        cal->setLenient(lenient);
        if (lenient != cal->isLenient()) errln("FAIL: setLenient/isLenient failed");
        // Later: Check for lenient behavior
    }

    for (i=Calendar::SUNDAY; i<=Calendar::SATURDAY; ++i)
    {
        cal->setFirstDayOfWeek((Calendar::EDaysOfWeek)i);
        if (cal->getFirstDayOfWeek() != i) errln("FAIL: set/getFirstDayOfWeek failed");
    }

    for (i=0; i<=7; ++i)
    {
        cal->setMinimalDaysInFirstWeek((uint8_t)i);
        if (cal->getMinimalDaysInFirstWeek() != i) errln("FAIL: set/getFirstDayOfWeek failed");
    }

    for (i=0; i<Calendar::FIELD_COUNT; ++i)
    {
        if (cal->getMinimum((Calendar::EDateFields)i) != cal->getGreatestMinimum((Calendar::EDateFields)i))
            errln("FAIL: getMinimum doesn't match getGreatestMinimum for field " + i);
        if (cal->getLeastMaximum((Calendar::EDateFields)i) > cal->getMaximum((Calendar::EDateFields)i))
            errln("FAIL: getLeastMaximum larger than getMaximum for field " + i);
        if (cal->getMinimum((Calendar::EDateFields)i) >= cal->getMaximum((Calendar::EDateFields)i))
            errln("FAIL: getMinimum not less than getMaximum for field " + i);
    }

    cal->adoptTimeZone(TimeZone::createDefault());
    cal->clear();
    cal->set(1984, 5, 24);
    if (cal->getTime(status) != date(84, 5, 24) || U_FAILURE(status))
        errln("FAIL: Calendar::set(3 args) failed");

    cal->clear();
    cal->set(1985, 3, 2, 11, 49);
    if (cal->getTime(status) != date(85, 3, 2, 11, 49) || U_FAILURE(status))
        errln("FAIL: Calendar::set(5 args) failed");

    cal->clear();
    cal->set(1995, 9, 12, 1, 39, 55);
    if (cal->getTime(status) != date(95, 9, 12, 1, 39, 55) || U_FAILURE(status))
        errln("FAIL: Calendar::set(6 args) failed");

    cal->getTime(status);
    if (failure(status, "Calendar::getTime")) return;
    for (i=0; i<Calendar::FIELD_COUNT; ++i)
    {
        switch(i) {
            case Calendar::YEAR: case Calendar::MONTH: case Calendar::DATE:
            case Calendar::HOUR_OF_DAY: case Calendar::MINUTE: case Calendar::SECOND:
                if (!cal->isSet((Calendar::EDateFields)i)) errln("FAIL: Calendar::isSet failed");
                break;
            default:
                if (cal->isSet((Calendar::EDateFields)i)) errln("FAIL: Calendar::isSet failed");
        }
        cal->clear((Calendar::EDateFields)i);
        if (cal->isSet((Calendar::EDateFields)i)) errln("FAIL: Calendar::clear/isSet failed");
    }

    delete cal;
    delete cal2;

    int32_t count;
    const Locale* loc = Calendar::getAvailableLocales(count);
    if (count < 1 || loc == 0)
    {
        errln("FAIL: getAvailableLocales failed");
    }
    else
    {
        for (i=0; i<count; ++i)
        {
            cal = Calendar::createInstance(loc[i], status);
            if (failure(status, "Calendar::createInstance")) return;
            delete cal;
        }
    }

    cal = Calendar::createInstance(TimeZone::createDefault(), Locale::ENGLISH, status);
    if (failure(status, "Calendar::createInstance")) return;
    delete cal;

    cal = Calendar::createInstance(*zone, Locale::ENGLISH, status);
    if (failure(status, "Calendar::createInstance")) return;
    delete cal;

    GregorianCalendar *gc = new GregorianCalendar(*zone, status);
    if (failure(status, "new GregorianCalendar")) return;
    delete gc;

    gc = new GregorianCalendar(Locale::ENGLISH, status);
    if (failure(status, "new GregorianCalendar")) return;
    delete gc;

    gc = new GregorianCalendar(Locale::ENGLISH, status);
    delete gc;

    gc = new GregorianCalendar(*zone, Locale::ENGLISH, status);
    if (failure(status, "new GregorianCalendar")) return;
    delete gc;

    gc = new GregorianCalendar(zone, status);
    if (failure(status, "new GregorianCalendar")) return;
    delete gc;

    gc = new GregorianCalendar(1998, 10, 14, 21, 43, status);
    if (gc->getTime(status) != date(98, 10, 14, 21, 43) || U_FAILURE(status))
        errln("FAIL: new GregorianCalendar(ymdhm) failed");
    delete gc;

    gc = new GregorianCalendar(1998, 10, 14, 21, 43, 55, status);
    if (gc->getTime(status) != date(98, 10, 14, 21, 43, 55) || U_FAILURE(status))
        errln("FAIL: new GregorianCalendar(ymdhms) failed");

    GregorianCalendar gc2(Locale::ENGLISH, status);
    if (failure(status, "new GregorianCalendar")) return;
    gc2 = *gc;
    if (gc2 != *gc || !(gc2 == *gc)) errln("FAIL: GregorianCalendar assignment/operator==/operator!= failed");
    delete gc;
    delete z;
}

// -------------------------------------

/**
 * This test confirms the correct behavior of add when incrementing
 * through subsequent days.
 */
void
CalendarTest::TestRog()
{
    UErrorCode status = U_ZERO_ERROR;
    GregorianCalendar* gc = new GregorianCalendar(status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
    int32_t year = 1997, month = Calendar::APRIL, date = 1;
    gc->set(year, month, date);
    gc->set(Calendar::HOUR_OF_DAY, 23);
    gc->set(Calendar::MINUTE, 0);
    gc->set(Calendar::SECOND, 0);
    gc->set(Calendar::MILLISECOND, 0);
    for (int32_t i = 0; i < 9; i++, gc->add(Calendar::DATE, 1, status)) {
        if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
        if (gc->get(Calendar::YEAR, status) != year ||
            gc->get(Calendar::MONTH, status) != month ||
            gc->get(Calendar::DATE, status) != (date + i)) errln("FAIL: Date wrong");
        if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
    }
    delete gc;
}
 
// -------------------------------------

/**
 * Test the handling of the day of the week, checking for correctness and
 * for correct minimum and maximum values.
 */
void
CalendarTest::TestDOW943()
{
    dowTest(FALSE);
    dowTest(TRUE);
}

void CalendarTest::dowTest(bool_t lenient)
{
    UErrorCode status = U_ZERO_ERROR;
    GregorianCalendar* cal = new GregorianCalendar(status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
    cal->set(1997, Calendar::AUGUST, 12);
    cal->getTime(status);
    if (U_FAILURE(status)) { errln("Calendar::getTime failed"); return; }
    cal->setLenient(lenient);
    cal->set(1996, Calendar::DECEMBER, 1);
    int32_t dow = cal->get(Calendar::DAY_OF_WEEK, status);
    if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
    int32_t min = cal->getMinimum(Calendar::DAY_OF_WEEK);
    int32_t max = cal->getMaximum(Calendar::DAY_OF_WEEK);
    if (dow < min ||
        dow > max) errln(UnicodeString("FAIL: Day of week ") + (int32_t)dow + " out of range");
    if (dow != Calendar::SUNDAY) errln("FAIL: Day of week should be SUNDAY");
    if (min != Calendar::SUNDAY ||
        max != Calendar::SATURDAY) errln("FAIL: Min/max bad");
    delete cal;
}

// -------------------------------------

/** 
 * Confirm that cloned Calendar objects do not inadvertently share substructures.
 */
void
CalendarTest::TestClonesUnique908()
{
    UErrorCode status = U_ZERO_ERROR;
    Calendar *c = Calendar::createInstance(status);
    if (U_FAILURE(status)) { errln("Calendar::createInstance failed"); return; }
    Calendar *d = (Calendar*) c->clone();
    c->set(Calendar::MILLISECOND, 123);
    d->set(Calendar::MILLISECOND, 456);
    if (c->get(Calendar::MILLISECOND, status) != 123 ||
        d->get(Calendar::MILLISECOND, status) != 456) {
        errln("FAIL: Clones share fields");
    }
    if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
    delete c;
    delete d;
}
 
// -------------------------------------

/**
 * Confirm that the Gregorian cutoff value works as advertised.
 */
void
CalendarTest::TestGregorianChange768()
{
    bool_t b;
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString str;
    GregorianCalendar* c = new GregorianCalendar(status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
    logln(UnicodeString("With cutoff ") + dateToString(c->getGregorianChange(), str));
    logln(UnicodeString(" isLeapYear(1800) = ") + ((b = c->isLeapYear(1800)) ? "true" : "false"));
    logln(UnicodeString(" (should be FALSE)"));
    if (b) errln("FAIL");
    c->setGregorianChange(date(0, 0, 1), status);
    if (U_FAILURE(status)) { errln("GregorianCalendar::setGregorianChange failed"); return; }
    logln(UnicodeString("With cutoff ") + dateToString(c->getGregorianChange(), str));
    logln(UnicodeString(" isLeapYear(1800) = ") + ((b = c->isLeapYear(1800)) ? "true" : "false"));
    logln(UnicodeString(" (should be TRUE)"));
    if (!b) errln("FAIL");
    delete c;
}
 
// -------------------------------------

/**
 * Confirm the functioning of the field disambiguation algorithm.
 */
void
CalendarTest::TestDisambiguation765()
{
    UErrorCode status = U_ZERO_ERROR;
    Calendar *c = Calendar::createInstance(status);
    if (U_FAILURE(status)) { errln("Calendar::createInstance failed"); return; }
    c->setLenient(FALSE);
    c->clear();
    c->set(Calendar::YEAR, 1997);
    c->set(Calendar::MONTH, Calendar::JUNE);
    c->set(Calendar::DATE, 3);
    verify765("1997 third day of June = ", c, 1997, Calendar::JUNE, 3);
    c->clear();
    c->set(Calendar::YEAR, 1997);
    c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
    c->set(Calendar::MONTH, Calendar::JUNE);
    c->set(Calendar::DAY_OF_WEEK_IN_MONTH, 1);
    verify765("1997 first Tuesday in June = ", c, 1997, Calendar::JUNE, 3);
    c->clear();
    c->set(Calendar::YEAR, 1997);
    c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
    c->set(Calendar::MONTH, Calendar::JUNE);
    c->set(Calendar::DAY_OF_WEEK_IN_MONTH, - 1);
    verify765("1997 last Tuesday in June = ", c, 1997, Calendar::JUNE, 24);
    // IllegalArgumentException e = null;
    status = U_ZERO_ERROR;
    //try {
        c->clear();
        c->set(Calendar::YEAR, 1997);
        c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
        c->set(Calendar::MONTH, Calendar::JUNE);
        c->set(Calendar::DAY_OF_WEEK_IN_MONTH, 0);
        c->getTime(status);
    //}
    //catch(IllegalArgumentException ex) {
    //    e = ex;
    //}
    verify765("1997 zero-th Tuesday in June = ", status);
    c->clear();
    c->set(Calendar::YEAR, 1997);
    c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
    c->set(Calendar::MONTH, Calendar::JUNE);
    c->set(Calendar::WEEK_OF_MONTH, 1);
    verify765("1997 Tuesday in week 1 of June = ", c, 1997, Calendar::JUNE, 3);
    c->clear();
    c->set(Calendar::YEAR, 1997);
    c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
    c->set(Calendar::MONTH, Calendar::JUNE);
    c->set(Calendar::WEEK_OF_MONTH, 5);
    verify765("1997 Tuesday in week 5 of June = ", c, 1997, Calendar::JULY, 1);
    status = U_ZERO_ERROR;
    //try {
        c->clear();
        c->set(Calendar::YEAR, 1997);
        c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
        c->set(Calendar::MONTH, Calendar::JUNE);
        c->set(Calendar::WEEK_OF_MONTH, 0);
        verify765("1997 Tuesday in week 0 of June = ", c, 1997, Calendar::MAY, 27);
    //}
    //catch(IllegalArgumentException ex) {
    //    errln("FAIL: Exception seen:");
    //    ex.printStackTrace(log);
    //}
    c->clear();
    c->set(Calendar::YEAR, 1997);
    c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
    c->set(Calendar::WEEK_OF_YEAR, 1);
    verify765("1997 Tuesday in week 1 of year = ", c, 1996, Calendar::DECEMBER, 31);
    c->clear();
    c->set(Calendar::YEAR, 1997);
    c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
    c->set(Calendar::WEEK_OF_YEAR, 10);
    verify765("1997 Tuesday in week 10 of year = ", c, 1997, Calendar::MARCH, 4);
    //try {

    // {sfb} week 0 is no longer a valid week of year
    /*c->clear();
    c->set(Calendar::YEAR, 1997);
    c->set(Calendar::DAY_OF_WEEK, Calendar::TUESDAY);
    //c->set(Calendar::WEEK_OF_YEAR, 0);
    c->set(Calendar::WEEK_OF_YEAR, 1);
    verify765("1997 Tuesday in week 0 of year = ", c, 1996, Calendar::DECEMBER, 24);*/

    //}
    //catch(IllegalArgumentException ex) {
    //    errln("FAIL: Exception seen:");
    //    ex.printStackTrace(log);
    //}
    delete c;
}
 
// -------------------------------------
 
void
CalendarTest::verify765(const UnicodeString& msg, Calendar* c, int32_t year, int32_t month, int32_t day)
{
    UnicodeString str;
    UErrorCode status = U_ZERO_ERROR;
    if (c->get(Calendar::YEAR, status) == year &&
        c->get(Calendar::MONTH, status) == month &&
        c->get(Calendar::DATE, status) == day) {
        if (U_FAILURE(status)) { errln("FAIL: Calendar::get failed"); return; }
        logln("PASS: " + msg + dateToString(c->getTime(status), str));
        if (U_FAILURE(status)) { errln("Calendar::getTime failed"); return; }
    }
    else {
        errln("FAIL: " + msg + dateToString(c->getTime(status), str) + "; expected " + (int32_t)year + "/" + (int32_t)(month + 1) + "/" + (int32_t)day);
        if (U_FAILURE(status)) { errln("Calendar::getTime failed"); return; }
    }
}
 
// -------------------------------------
 
void
CalendarTest::verify765(const UnicodeString& msg/*, IllegalArgumentException e*/, UErrorCode status)
{
    if (status != U_ILLEGAL_ARGUMENT_ERROR) errln("FAIL: No IllegalArgumentException for " + msg);
    else logln("PASS: " + msg + "IllegalArgument as expected");
}
 
// -------------------------------------

/**
 * Confirm that the offset between local time and GMT behaves as expected.
 */
void
CalendarTest::TestGMTvsLocal4064654()
{
    test4064654(1997, 1, 1, 12, 0, 0);
    test4064654(1997, 4, 16, 18, 30, 0);
}
 
// -------------------------------------
 
void
CalendarTest::test4064654(int32_t yr, int32_t mo, int32_t dt, int32_t hr, int32_t mn, int32_t sc)
{
    UDate date;
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString str;
    Calendar *gmtcal = Calendar::createInstance(status);
    if (U_FAILURE(status)) { errln("Calendar::createInstance failed"); return; }
    gmtcal->adoptTimeZone(TimeZone::createTimeZone("Africa/Casablanca"));
    gmtcal->set(yr, mo - 1, dt, hr, mn, sc);
    gmtcal->set(Calendar::MILLISECOND, 0);
    date = gmtcal->getTime(status);
    if (U_FAILURE(status)) { errln("Calendar::getTime failed"); return; }
    logln("date = " + dateToString(date, str));
    Calendar *cal = Calendar::createInstance(status);
    if (U_FAILURE(status)) { errln("Calendar::createInstance failed"); return; }
    cal->setTime(date, status);
    if (U_FAILURE(status)) { errln("Calendar::setTime failed"); return; }
    int32_t offset = cal->getTimeZone().getOffset((uint8_t)cal->get(Calendar::ERA, status),
                                                  cal->get(Calendar::YEAR, status),
                                                  cal->get(Calendar::MONTH, status),
                                                  cal->get(Calendar::DATE, status),
                                                  (uint8_t)cal->get(Calendar::DAY_OF_WEEK, status),
                                                  cal->get(Calendar::MILLISECOND, status));
    if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
    logln("offset for " + dateToString(date, str) + "= " + (offset / 1000 / 60 / 60.0) + "hr");
    int32_t utc = ((cal->get(Calendar::HOUR_OF_DAY, status) * 60 +
                    cal->get(Calendar::MINUTE, status)) * 60 +
                   cal->get(Calendar::SECOND, status)) * 1000 +
        cal->get(Calendar::MILLISECOND, status) - offset;
    if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
    int32_t expected = ((hr * 60 + mn) * 60 + sc) * 1000;
    if (utc != expected) errln(UnicodeString("FAIL: Discrepancy of ") + (utc - expected) +
                               " millis = " + ((utc - expected) / 1000 / 60 / 60.0) + " hr");
    delete gmtcal;
    delete cal;
}
 
// -------------------------------------
 
/**
 * The operations of adding and setting should not exhibit pathological
 * dependence on the order of operations.  This test checks for this.
 */
void
CalendarTest::TestAddSetOrder621()
{
    UDate d = date(97, 4, 14, 13, 23, 45);
    UErrorCode status = U_ZERO_ERROR;
    Calendar *cal = Calendar::createInstance(status);
    if (U_FAILURE(status)) { 
        errln("Calendar::createInstance failed"); 
        delete cal;
        return; 
    }
    cal->setTime(d, status);
    if (U_FAILURE(status)) { 
        errln("Calendar::setTime failed"); 
        delete cal;
        return; 
    }
    cal->add(Calendar::DATE, - 5, status);
    if (U_FAILURE(status)) { 
        errln("Calendar::add failed"); 
        delete cal;
        return; 
    }
    cal->set(Calendar::HOUR_OF_DAY, 0);
    cal->set(Calendar::MINUTE, 0);
    cal->set(Calendar::SECOND, 0);
    UnicodeString s;
    dateToString(cal->getTime(status), s);
    if (U_FAILURE(status)) { 
        errln("Calendar::getTime failed"); 
        delete cal;
        return; 
    }
    delete cal;

    cal = Calendar::createInstance(status);
    if (U_FAILURE(status)) { 
        errln("Calendar::createInstance failed"); 
        delete cal;
        return; 
    }
    cal->setTime(d, status);
    if (U_FAILURE(status)) { 
        errln("Calendar::setTime failed"); 
        delete cal;
        return; 
    }
    cal->set(Calendar::HOUR_OF_DAY, 0);
    cal->set(Calendar::MINUTE, 0);
    cal->set(Calendar::SECOND, 0);
    cal->add(Calendar::DATE, - 5, status);
    if (U_FAILURE(status)) { 
        errln("Calendar::add failed"); 
        delete cal;
        return; 
    }
    UnicodeString s2;
    dateToString(cal->getTime(status), s2);
    if (U_FAILURE(status)) { 
        errln("Calendar::getTime failed"); 
        delete cal;
        return; 
    }
    if (s == s2) 
        logln("Pass: " + s + " == " + s2);
    else 
        errln("FAIL: " + s + " != " + s2);
    delete cal;
}
 
// -------------------------------------
 
/**
 * Confirm that adding to various fields works.
 */
void
CalendarTest::TestAdd520()
{
    int32_t y = 1997, m = Calendar::FEBRUARY, d = 1;
    UErrorCode status = U_ZERO_ERROR;
    GregorianCalendar *temp = new GregorianCalendar(y, m, d, status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
    check520(temp, y, m, d);
    temp->add(temp->YEAR, 1, status);
    if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
    y++;
    check520(temp, y, m, d);
    temp->add(temp->MONTH, 1, status);
    if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
    m++;
    check520(temp, y, m, d);
    temp->add(temp->DATE, 1, status);
    if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
    d++;
    check520(temp, y, m, d);
    temp->add(temp->DATE, 2, status);
    if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
    d += 2;
    check520(temp, y, m, d);
    temp->add(temp->DATE, 28, status);
    if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
    d = 1;++m;
    check520(temp, y, m, d);
    delete temp;
}
 
// -------------------------------------
 
/**
 * Execute adding and rolling in GregorianCalendar extensively,
 */
void
CalendarTest::TestAddRollExtensive()
{
    int32_t maxlimit = 40;
    int32_t y = 1997, m = Calendar::FEBRUARY, d = 1, hr = 1, min = 1, sec = 0, ms = 0;
    UErrorCode status = U_ZERO_ERROR;
    GregorianCalendar *temp = new GregorianCalendar(y, m, d, status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }

    temp->set(Calendar::HOUR, hr);
    temp->set(Calendar::MINUTE, min);
    temp->set(Calendar::SECOND, sec);
    temp->set(Calendar::MILLISECOND, ms);

    Calendar::EDateFields e;

    logln("Testing GregorianCalendar add...");
    e = Calendar::YEAR;
    while (e < Calendar::FIELD_COUNT) {
        int32_t i;
        int32_t limit = maxlimit;
        status = U_ZERO_ERROR;
        for (i = 0; i < limit; i++) {
            temp->add(e, 1, status);
            if (U_FAILURE(status)) { limit = i; status = U_ZERO_ERROR; }
        }
        for (i = 0; i < limit; i++) {
            temp->add(e, -1, status);
            if (U_FAILURE(status)) { errln("GregorianCalendar::add -1 failed"); return; }
        }
        check520(temp, y, m, d, hr, min, sec, ms, e);

        e = (Calendar::EDateFields) ((int32_t) e + 1);
    }

    logln("Testing GregorianCalendar roll...");
    e = Calendar::YEAR;
    while (e < Calendar::FIELD_COUNT) {
        int32_t i;
        int32_t limit = maxlimit;
        status = U_ZERO_ERROR;
        for (i = 0; i < limit; i++) {
            temp->roll(e, 1, status);
            if (U_FAILURE(status)) { limit = i; status = U_ZERO_ERROR; }
        }
        for (i = 0; i < limit; i++) {
            temp->roll(e, -1, status);
            if (U_FAILURE(status)) { errln("GregorianCalendar::roll -1 failed"); return; }
        }
        check520(temp, y, m, d, hr, min, sec, ms, e);

        e = (Calendar::EDateFields) ((int32_t) e + 1);
    }

    delete temp;
}
 
// -------------------------------------
void 
CalendarTest::check520(Calendar* c, 
                        int32_t y, int32_t m, int32_t d, 
                        int32_t hr, int32_t min, int32_t sec, 
                        int32_t ms, Calendar::EDateFields field)

{
    UErrorCode status = U_ZERO_ERROR;
    if (c->get(Calendar::YEAR, status) != y ||
        c->get(Calendar::MONTH, status) != m ||
        c->get(Calendar::DATE, status) != d ||
        c->get(Calendar::HOUR, status) != hr ||
        c->get(Calendar::MINUTE, status) != min ||
        c->get(Calendar::SECOND, status) != sec ||
        c->get(Calendar::MILLISECOND, status) != ms) {
        errln(UnicodeString("U_FAILURE for field ") + (int32_t)field + 
                ": Expected y/m/d h:m:s:ms of " +
                y + "/" + (m + 1) + "/" + d + " " +
              hr + ":" + min + ":" + sec + ":" + ms +
              "; got " + c->get(Calendar::YEAR, status) +
              "/" + (c->get(Calendar::MONTH, status) + 1) +
              "/" + c->get(Calendar::DATE, status) +
              " " + c->get(Calendar::HOUR, status) + ":" +
              c->get(Calendar::MINUTE, status) + ":" + 
              c->get(Calendar::SECOND, status) + ":" +
              c->get(Calendar::MILLISECOND, status)
              );

        if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
    }
    else 
        logln(UnicodeString("Confirmed: ") + y + "/" +
                (m + 1) + "/" + d + " " +
                hr + ":" + min + ":" + sec + ":" + ms);
}
 
// -------------------------------------
void 
CalendarTest::check520(Calendar* c, 
                        int32_t y, int32_t m, int32_t d)

{
    UErrorCode status = U_ZERO_ERROR;
    if (c->get(Calendar::YEAR, status) != y ||
        c->get(Calendar::MONTH, status) != m ||
        c->get(Calendar::DATE, status) != d) {
        errln(UnicodeString("FAILURE: Expected y/m/d of ") +
              y + "/" + (m + 1) + "/" + d + " " +
              "; got " + c->get(Calendar::YEAR, status) +
              "/" + (c->get(Calendar::MONTH, status) + 1) +
              "/" + c->get(Calendar::DATE, status)
              );

        if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
    }
    else 
        logln(UnicodeString("Confirmed: ") + y + "/" +
                (m + 1) + "/" + d);
}

// -------------------------------------
 
/**
 * Test that setting of fields works.  In particular, make sure that all instances
 * of GregorianCalendar don't share a static instance of the fields array.
 */
void
CalendarTest::TestFieldSet4781()
{
    // try {
        UErrorCode status = U_ZERO_ERROR;
        GregorianCalendar *g = new GregorianCalendar(status);
        if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
        GregorianCalendar *g2 = new GregorianCalendar(status);
        if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
        g2->set(Calendar::HOUR, 12, status);
        g2->set(Calendar::MINUTE, 0, status);
        g2->set(Calendar::SECOND, 0, status);
        if (U_FAILURE(status)) { errln("Calendar::set failed"); return; }
        if (*g == *g2) logln("Same");
        else logln("Different");
    //}
        //catch(IllegalArgumentException e) {
        //errln("Unexpected exception seen: " + e);
    //}
        delete g;
        delete g2;
}
 
// -------------------------------------

/* We don't support serialization on C++
void
CalendarTest::TestSerialize337()
{
    Calendar cal = Calendar::getInstance();
    bool_t ok = FALSE;
    try {
        FileOutputStream f = new FileOutputStream(FILENAME);
        ObjectOutput s = new ObjectOutputStream(f);
        s.writeObject(PREFIX);
        s.writeObject(cal);
        s.writeObject(POSTFIX);
        f.close();
        FileInputStream in = new FileInputStream(FILENAME);
        ObjectInputStream t = new ObjectInputStream(in);
        UnicodeString& pre = (UnicodeString&) t.readObject();
        Calendar c = (Calendar) t.readObject();
        UnicodeString& post = (UnicodeString&) t.readObject();
        in.close();
        ok = pre.equals(PREFIX) &&
            post.equals(POSTFIX) &&
            cal->equals(c);
        File fl = new File(FILENAME);
        fl.delete();
    }
    catch(IOException e) {
        errln("FAIL: Exception received:");
        e.printStackTrace(log);
    }
    catch(ClassNotFoundException e) {
        errln("FAIL: Exception received:");
        e.printStackTrace(log);
    }
    if (!ok) errln("Serialization of Calendar object failed.");
}
 
UnicodeString& CalendarTest::PREFIX = "abc";
 
UnicodeString& CalendarTest::POSTFIX = "def";
 
UnicodeString& CalendarTest::FILENAME = "tmp337.bin";
 */

// -------------------------------------

/**
 * Verify that the seconds of a Calendar can be zeroed out through the
 * expected sequence of operations.
 */ 
void
CalendarTest::TestSecondsZero121()
{
    UErrorCode status = U_ZERO_ERROR;
    Calendar *cal = new GregorianCalendar(status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
    cal->setTime(Calendar::getNow(), status);
    if (U_FAILURE(status)) { errln("Calendar::setTime failed"); return; }
    cal->set(Calendar::SECOND, 0);
    if (U_FAILURE(status)) { errln("Calendar::set failed"); return; }
    UDate d = cal->getTime(status);
    if (U_FAILURE(status)) { errln("Calendar::getTime failed"); return; }
    UnicodeString s;
    dateToString(d, s);
    if (s.indexOf(":00 ") < 0) errln("Expected to see :00 in " + s);
    delete cal;
}
 
// -------------------------------------
 
/**
 * Verify that a specific sequence of adding and setting works as expected;
 * it should not vary depending on when and whether the get method is
 * called.
 */
void
CalendarTest::TestAddSetGet0610()
{
    UErrorCode status = U_ZERO_ERROR;
    {
        Calendar *calendar = new GregorianCalendar(status);
        if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
        calendar->set(1993, Calendar::JANUARY, 4);
        logln("1A) " + value(calendar));
        calendar->add(Calendar::DATE, 1, status);
        if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
        UnicodeString v = value(calendar);
        logln("1B) " + v);
        logln("--) 1993/0/5");
        if (!(v == EXPECTED_0610)) errln("Expected " + EXPECTED_0610 + "; saw " + v);
        delete calendar;
    }
    {
        Calendar *calendar = new GregorianCalendar(1993, Calendar::JANUARY, 4, status);
        if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
        logln("2A) " + value(calendar));
        calendar->add(Calendar::DATE, 1, status);
        if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
        UnicodeString v = value(calendar);
        logln("2B) " + v);
        logln("--) 1993/0/5");
        if (!(v == EXPECTED_0610)) errln("Expected " + EXPECTED_0610 + "; saw " + v);
        delete calendar;
    }
    {
        Calendar *calendar = new GregorianCalendar(1993, Calendar::JANUARY, 4, status);
        if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
        logln("3A) " + value(calendar));
        calendar->getTime(status);
        if (U_FAILURE(status)) { errln("Calendar::getTime failed"); return; }
        calendar->add(Calendar::DATE, 1, status);
        if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
        UnicodeString v = value(calendar);
        logln("3B) " + v);
        logln("--) 1993/0/5");
        if (!(v == EXPECTED_0610)) errln("Expected " + EXPECTED_0610 + "; saw " + v);
        delete calendar;
    }
}
 
// -------------------------------------
 
UnicodeString
CalendarTest::value(Calendar* calendar)
{
    UErrorCode status = U_ZERO_ERROR;
    return UnicodeString("") + (int32_t)calendar->get(Calendar::YEAR, status) +
        "/" + (int32_t)calendar->get(Calendar::MONTH, status) +
        "/" + (int32_t)calendar->get(Calendar::DATE, status) +
        (U_FAILURE(status) ? " FAIL: Calendar::get failed" : "");
}
 
UnicodeString CalendarTest::EXPECTED_0610 = "1993/0/5";
 
// -------------------------------------
 
/**
 * Verify that various fields on a known date are set correctly.
 */
void
CalendarTest::TestFields060()
{
    UErrorCode status = U_ZERO_ERROR;
    int32_t year = 1997;
    int32_t month = Calendar::OCTOBER;
    int32_t dDate = 22;
    GregorianCalendar *calendar = 0;
    calendar = new GregorianCalendar(year, month, dDate, status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
    for (int32_t i = 0; i < EXPECTED_FIELDS_length;) {
        Calendar::EDateFields field = (Calendar::EDateFields)EXPECTED_FIELDS[i++];
        int32_t expected = EXPECTED_FIELDS[i++];
        if (calendar->get(field, status) != expected) {
            errln(UnicodeString("Expected field ") + (int32_t)field + " to have value " + (int32_t)expected +
                  "; received " + (int32_t)calendar->get(field, status) + " instead");
            if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
        }
    }
    delete calendar;
}
 
int32_t CalendarTest::EXPECTED_FIELDS[] = {
    Calendar::YEAR, 1997,
    Calendar::MONTH, Calendar::OCTOBER,
    Calendar::DAY_OF_MONTH, 22,
    Calendar::DAY_OF_WEEK, Calendar::WEDNESDAY,
    Calendar::DAY_OF_WEEK_IN_MONTH, 4,
    Calendar::DAY_OF_YEAR, 295
};
 
const int32_t CalendarTest::EXPECTED_FIELDS_length = sizeof(CalendarTest::EXPECTED_FIELDS) /
    sizeof(CalendarTest::EXPECTED_FIELDS[0]);

// -------------------------------------
 
/**
 * Verify that various fields on a known date are set correctly.  In this
 * case, the start of the epoch (January 1 1970).
 */
void
CalendarTest::TestEpochStartFields()
{
    UErrorCode status = U_ZERO_ERROR;
    TimeZone *z = TimeZone::createDefault();
    Calendar *c = Calendar::createInstance(status);
    if (U_FAILURE(status)) { errln("Calendar::createInstance failed"); return; }
    UDate d = - z->getRawOffset();
    GregorianCalendar *gc = new GregorianCalendar(status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
    gc->setTimeZone(*z);
    gc->setTime(d, status);
    if (U_FAILURE(status)) { errln("Calendar::setTime failed"); return; }
    bool_t idt = gc->inDaylightTime(status);
    if (U_FAILURE(status)) { errln("GregorianCalendar::inDaylightTime failed"); return; }
    if (idt) {
        UnicodeString str;
        logln("Warning: Skipping test because " + dateToString(d, str) + " is in DST.");
    }
    else {
        c->setTime(d, status);
        if (U_FAILURE(status)) { errln("Calendar::setTime failed"); return; }
        for (int32_t i = 0; i < Calendar::ZONE_OFFSET;++i) {
            if (c->get((Calendar::EDateFields)i, status) != EPOCH_FIELDS[i])
                errln(UnicodeString("Expected field ") + i + " to have value " + EPOCH_FIELDS[i] +
                      "; saw " + c->get((Calendar::EDateFields)i, status) + " instead");
            if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
        }
        if (c->get(Calendar::ZONE_OFFSET, status) != z->getRawOffset())
        {
            errln(UnicodeString("Expected field ZONE_OFFSET to have value ") + z->getRawOffset() +
                  "; saw " + c->get(Calendar::ZONE_OFFSET, status) + " instead");
            if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
        }
        if (c->get(Calendar::DST_OFFSET, status) != 0)
        {
            errln(UnicodeString("Expected field DST_OFFSET to have value 0") +
                  "; saw " + c->get(Calendar::DST_OFFSET, status) + " instead");
            if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
        }
    }
    delete c;
    delete z;
    delete gc;
}
 
int32_t CalendarTest::EPOCH_FIELDS[] = {
    1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, - 28800000, 0
};
 
// -------------------------------------
 
/**
 * Test that the days of the week progress properly when add is called repeatedly
 * for increments of 24 days.
 */
void
CalendarTest::TestDOWProgression()
{
    UErrorCode status = U_ZERO_ERROR;
    Calendar *cal = new GregorianCalendar(1972, Calendar::OCTOBER, 26, status);
    if (U_FAILURE(status)) { errln("Couldn't create GregorianCalendar"); return; }
    marchByDelta(cal, 24);
    delete cal;
}
 
// -------------------------------------
 
void
CalendarTest::marchByDelta(Calendar* cal, int32_t delta)
{
    UErrorCode status = U_ZERO_ERROR;
    Calendar *cur = (Calendar*) cal->clone();
    int32_t initialDOW = cur->get(Calendar::DAY_OF_WEEK, status);
    if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
    int32_t DOW, newDOW = initialDOW;
    do {
        UnicodeString str;
        DOW = newDOW;
        logln(UnicodeString("DOW = ") + DOW + "  " + dateToString(cur->getTime(status), str));
        if (U_FAILURE(status)) { errln("Calendar::getTime failed"); return; }
        cur->add(Calendar::DAY_OF_WEEK, delta, status);
        if (U_FAILURE(status)) { errln("Calendar::add failed"); return; }
        newDOW = cur->get(Calendar::DAY_OF_WEEK, status);
        if (U_FAILURE(status)) { errln("Calendar::get failed"); return; }
        int32_t expectedDOW = 1 + (DOW + delta - 1) % 7;
        if (newDOW != expectedDOW) {
            errln(UnicodeString("Day of week should be ") + expectedDOW + " instead of " + newDOW +
                  " on " + dateToString(cur->getTime(status), str));
            if (U_FAILURE(status)) { errln("Calendar::getTime failed"); return; }
            return;
        }
    }
    while (newDOW != initialDOW);
    delete cur;
}

//eof
