// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.JavaTimeConverters;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/* This class tests the raw conversion, java.time classes to an ICU Calendar. */
@RunWith(JUnit4.class)
public class JavaTimeConvertersTest extends CoreTestFmwk {

    /*
     * Fields that we expect in the calendar when formatting dates.
     *
     * A LocalDate object will not have hour, minutes, seconds, etc.
     * So when we convert it to a Calendar the result can't be directly compared
     * to the expected Calendar because some fields are different.
     *
     * Think of this field list as a "mask" we use when we compare a calendar
     * from conversion with the expected Calendar.
     */
    private static final int[] DATE_ONLY_FIELDS = {
        Calendar.DAY_OF_MONTH,
        Calendar.MONTH,
        Calendar.YEAR,
        Calendar.DAY_OF_WEEK,
        Calendar.DAY_OF_YEAR,
        Calendar.ERA,
        Calendar.DAY_OF_WEEK_IN_MONTH,
        Calendar.DOW_LOCAL,
        Calendar.WEEK_OF_MONTH,
        Calendar.WEEK_OF_YEAR,
        Calendar.EXTENDED_YEAR
    };

    // Fields that we expect in the calendar when formatting time
    private static final int[] TIME_ONLY_FIELDS = {
        Calendar.HOUR_OF_DAY,
        Calendar.MINUTE,
        Calendar.SECOND,
        Calendar.MILLISECOND,
        Calendar.AM_PM,
        Calendar.MILLISECONDS_IN_DAY
    };

    // Make it easier to build all kind of temporal objects
    static final LocalDateTime LOCAL_DATE_TIME =
            LocalDateTime.of(2018, Month.SEPTEMBER, 23, 19, 42, 57, /*nanoseconds*/ 123_000_000);

    static final String TIME_ZONE_ID = "Europe/Paris";

    // Match the fields in the LOCAL_DATE_TIME above
    static final Calendar EXPECTED_CALENDAR =
            new GregorianCalendar(2018, Calendar.SEPTEMBER, 23, 19, 42, 57);

    static {
        EXPECTED_CALENDAR.setTimeZone(TimeZone.getTimeZone(TIME_ZONE_ID));
        EXPECTED_CALENDAR.setTimeInMillis(EXPECTED_CALENDAR.getTimeInMillis() + 123);
    }

    @Test
    public void testDateOnly() {
        LocalDate ld = LOCAL_DATE_TIME.toLocalDate();
        Calendar calendar = JavaTimeConverters.temporalToCalendar(ld);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, DATE_ONLY_FIELDS);

        HijrahDate hd = HijrahDate.of(1440, 1, 13);
        calendar = JavaTimeConverters.temporalToCalendar(hd);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, DATE_ONLY_FIELDS);

        JapaneseDate jd = JapaneseDate.of(JapaneseEra.HEISEI, 30, Month.SEPTEMBER.getValue(), 23);
        calendar = JavaTimeConverters.temporalToCalendar(jd);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, DATE_ONLY_FIELDS);

        MinguoDate md = MinguoDate.of(107, Month.SEPTEMBER.getValue(), 23);
        calendar = JavaTimeConverters.temporalToCalendar(md);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, DATE_ONLY_FIELDS);

        ThaiBuddhistDate td = ThaiBuddhistDate.of(2561, Month.SEPTEMBER.getValue(), 23);
        calendar = JavaTimeConverters.temporalToCalendar(td);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, DATE_ONLY_FIELDS);
    }

    @Test
    public void testTimesOnly() {
        LocalTime lt = LOCAL_DATE_TIME.toLocalTime();
        Calendar calendar = JavaTimeConverters.temporalToCalendar(lt);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, TIME_ONLY_FIELDS);

        OffsetTime ot = OffsetTime.of(lt, ZoneOffset.ofHours(1));
        calendar = JavaTimeConverters.temporalToCalendar(ot);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, TIME_ONLY_FIELDS);
    }

    @Test
    public void testDateAndTimes() {
        Calendar calendar = JavaTimeConverters.temporalToCalendar(LOCAL_DATE_TIME);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, DATE_ONLY_FIELDS);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, TIME_ONLY_FIELDS);

        ZonedDateTime zdt =
                ZonedDateTime.of(
                        LOCAL_DATE_TIME, ZoneId.of(TIME_ZONE_ID)); // Date + Time + TimeZone
        calendar = JavaTimeConverters.temporalToCalendar(zdt);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, DATE_ONLY_FIELDS);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, TIME_ONLY_FIELDS);
        assertEquals("", EXPECTED_CALENDAR.getTimeZone().getID(), calendar.getTimeZone().getID());

        OffsetDateTime odt =
                OffsetDateTime.of(LOCAL_DATE_TIME, ZoneOffset.ofHours(1)); // Date + Time + TimeZone
        calendar = JavaTimeConverters.temporalToCalendar(odt);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, DATE_ONLY_FIELDS);
        assertCalendarsEquals(EXPECTED_CALENDAR, calendar, TIME_ONLY_FIELDS);
        assertEquals(
                "",
                EXPECTED_CALENDAR.getTimeZone().getRawOffset(),
                calendar.getTimeZone().getRawOffset());

        int[] DATE_OF_WEEK_ONLY_FIELD = {Calendar.DAY_OF_WEEK};
        calendar = JavaTimeConverters.dayOfWeekToCalendar(DayOfWeek.MONDAY);
        assertEquals("", Calendar.MONDAY, calendar.get(Calendar.DAY_OF_WEEK));

        int[] MONTH_ONLY_FIELD = {Calendar.MONTH};
        calendar = JavaTimeConverters.monthToCalendar(Month.SEPTEMBER);
        assertEquals("", Calendar.SEPTEMBER, calendar.get(Calendar.MONTH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstantFails() {
        Instant instant = Instant.ofEpochMilli(EXPECTED_CALENDAR.getTimeInMillis());
        JavaTimeConverters.temporalToCalendar(instant);
    }

    // Compare the expected / actual calendar, but using an allowlist
    private static void assertCalendarsEquals(
            Calendar exected, Calendar actual, int[] fieldsToCheck) {
        for (int field : fieldsToCheck) {
            assertEquals("Bad conversion", exected.get(field), actual.get(field));
        }
    }
}
