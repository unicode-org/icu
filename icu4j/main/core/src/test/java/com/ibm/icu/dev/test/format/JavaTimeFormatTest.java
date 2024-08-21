// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.format;

import java.text.FieldPosition;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.message2.MessageFormatter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateIntervalFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.TimeZone;

@RunWith(JUnit4.class)
public class JavaTimeFormatTest extends CoreTestFmwk {
    final static LocalDateTime LDT =
            LocalDateTime.of(/*year*/ 2013, Month.SEPTEMBER, 27,
                    /*hour*/ 19, /*min*/43, /*sec*/ 56, /*nanosec*/ 123_456_789);

    @Test
    public void testLocalDateFormatting() {
        LocalDate ld = LDT.toLocalDate();

        // Formatting with skeleton
        DateFormat formatFromSkeleton = DateFormat.getInstanceForSkeleton("EEEEyMMMMd", Locale.US);
        assertEquals("", "Friday, September 27, 2013", formatFromSkeleton.format(ld));

        // Format with style
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
        assertEquals("", "September 27, 2013", dateFormat.format(ld));
    }

    @Test
    public void testLocalDateTimeFormatting() {
        // Formatting with skeleton
        DateFormat formatFromSkeleton = DateFormat.getInstanceForSkeleton("EEEEyMMMMd jmsSSS", Locale.US);
        assertEquals("", "Friday, September 27, 2013 at 7:43:56.123\u202FPM", formatFromSkeleton.format(LDT));

        // Format with style
        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT, Locale.US);
        assertEquals("", "September 27, 2013 at 7:43\u202FPM", dateTimeFormat.format(LDT));

        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US);
        assertEquals("", "7:43\u202FPM", timeFormat.format(LDT));
    }

    @Test
    public void testThatConvertedCalendarUsesDefaultTimeZone() {
        // Save the default time zones
        TimeZone savedTimeZone = TimeZone.getDefault();
        java.util.TimeZone jdkSavedTimeZone = java.util.TimeZone.getDefault();
        // Set to one that we control
        String timeZoneId = "America/New_York";
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(timeZoneId));

        // We check that the calendar from conversion uses the default time zone.
        DateFormat icuDateFormat = DateFormat.getInstanceForSkeleton("EEEEdMMMMyjmszzzz", Locale.US);
        String result = icuDateFormat.format(LDT);

        // Restore the default time zones
        TimeZone.setDefault(savedTimeZone);
        java.util.TimeZone.setDefault(jdkSavedTimeZone);

        assertEquals("", "Friday, September 27, 2013 at 7:43:56 PM Eastern Daylight Time", result);
    }

    @Test
    public void testDateTimeWithTimeZoneFormatting() {
        // Formatting with skeleton
        ZonedDateTime zdt = ZonedDateTime.of(LDT, ZoneId.of("Europe/Paris"));
        OffsetDateTime odt = OffsetDateTime.of(LDT, ZoneOffset.ofHoursMinutes(5, 30));

        DateFormat formatFromSkeleton = DateFormat.getInstanceForSkeleton("EEEEyMMMMd jmsSSS vvvv", Locale.US);
        assertEquals("", "Friday, September 27, 2013 at 7:43:56.123\u202FPM Central European Time", formatFromSkeleton.format(zdt));
        assertEquals("", "Friday, September 27, 2013 at 7:43:56.123\u202FPM GMT+05:30", formatFromSkeleton.format(odt));

        // Format with style
        DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.FULL, Locale.US);
        assertEquals("", "7:43:56\u202FPM Central European Summer Time",timeFormat.format(zdt));
        assertEquals("", "7:43:56\u202FPM GMT+05:30", timeFormat.format(odt));

        DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
        assertEquals("", "Friday, September 27, 2013 at 7:43:56\u202FPM Central European Summer Time", dateTimeFormat.format(zdt));
        assertEquals("", "Friday, September 27, 2013 at 7:43:56\u202FPM GMT+05:30", dateTimeFormat.format(odt));
    }

    @Test
    public void testNonGregorianDateFormatting() {
        // Non-Gregorian as input
        LocalDate ld = LDT.toLocalDate();
        HijrahDate hd = HijrahDate.from(ld);
        JapaneseDate jd = JapaneseDate.from(ld);
        MinguoDate md = MinguoDate.from(ld);
        ThaiBuddhistDate td = ThaiBuddhistDate.from(ld);

        DateFormat formatFromSkeleton = DateFormat.getInstanceForSkeleton("EEEEGGGyMMMMd", Locale.US);
        String expected = "Friday, September 27, 2013 AD";
        assertEquals("", expected, formatFromSkeleton.format(hd));
        assertEquals("", expected, formatFromSkeleton.format(jd));
        assertEquals("", expected, formatFromSkeleton.format(md));
        assertEquals("", expected, formatFromSkeleton.format(td));

        // Non-Gregorian as formatting calendar
        String[] expectedPerCalendar = {
                "buddhist", "September 27, 2556 BE",
                "chinese",  "Eighth Month 23, 2013(gui-si)",
                "hebrew",   "23 Tishri 5774 AM",
                "indian",   "Asvina 5, 1935 Saka",
                "islamic",  "Dhuʻl-Qiʻdah 22, 1434 AH",
                "japanese", "September 27, 25 Heisei",
                "persian",  "Mehr 5, 1392 AP",
                "roc",      "September 27, 102 Minguo",
        };
        String skeleton = "GGGGyMMMMd";
        for (int i = 0; i < expectedPerCalendar.length; i++) {
            Locale locale = Locale.forLanguageTag("en-u-ca-" + expectedPerCalendar[i++]);
            formatFromSkeleton = DateFormat.getInstanceForSkeleton(skeleton, locale);
            assertEquals("", expectedPerCalendar[i], formatFromSkeleton.format(LDT));
        }
    }

    @Test
    public void testInstantAndClockFormatting() {
        DateFormat formatFromSkeleton = DateFormat.getInstanceForSkeleton("yMMMMd jmsSSSvvvv", Locale.US);

        Instant instant = LDT.toInstant(ZoneOffset.UTC);
        assertEquals("", "September 27, 2013 at 7:43:56.123 PM Greenwich Mean Time", formatFromSkeleton.format(instant));

        Clock clock = Clock.fixed(instant, ZoneId.of("America/Los_Angeles"));
        assertEquals("", "September 27, 2013 at 12:43:56.123 PM Pacific Time", formatFromSkeleton.format(clock));
    }

    @Test
    public void testMessageFormat() {
        Locale locale = Locale.FRENCH;
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("expDate", LDT);

        // Make sure that the type detection works, we don't pass a type for the formatter
        MessageFormat mf = new MessageFormat("Your card expires on {expDate}", locale);
        assertEquals("", "Your card expires on 27/09/2013 19:43", mf.format(arguments));

        // Now we specify that the placeholder is a date, make sure that the style & skeleton are honored.
        mf = new MessageFormat("Your card expires on {expDate, date}", locale);
        assertEquals("", "Your card expires on 27 sept. 2013", mf.format(arguments));

        mf = new MessageFormat("Your card expires on {expDate, date, FULL}", locale);
        assertEquals("", "Your card expires on vendredi 27 septembre 2013", mf.format(arguments));

        mf = new MessageFormat("Your card expires on {expDate, date, ::EEEyMMMd}", locale);
        assertEquals("", "Your card expires on ven. 27 sept. 2013", mf.format(arguments));

        // MessageFormatter (MF2)

        MessageFormatter.Builder mf2Builder = MessageFormatter.builder()
                .setLocale(locale);

        MessageFormatter mf2 = mf2Builder.setPattern("(mf2) Your card expires on {$expDate}").build();
        assertEquals("", "(mf2) Your card expires on 27/09/2013 19:43", mf2.formatToString(arguments));

        mf2 = mf2Builder.setPattern("(mf2) Your card expires on {$expDate :date}").build();
        assertEquals("", "(mf2) Your card expires on 27/09/2013", mf2.formatToString(arguments));

        mf2 = mf2Builder.setPattern("(mf2) Your card expires on {$expDate :datetime dateStyle=long}").build();
        assertEquals("", "(mf2) Your card expires on 27 septembre 2013", mf2.formatToString(arguments));

        mf2 = mf2Builder.setPattern("(mf2) Your card expires on {$expDate :date icu:skeleton=EEEyMMMd}").build();
        assertEquals("", "(mf2) Your card expires on ven. 27 sept. 2013", mf2.formatToString(arguments));

        // Test several java.time types
        // We don't care much about the string result, as we test that somewhere else.
        // We only want to make sure that MessageFormat(ter) recognizes the types.

        String expectedMf1Result = "Your card expires on ven. 27 sept. 2013";
        String expectedMf2Result = "(mf2) " + expectedMf1Result;
        // LocalDate
        arguments.put("expDate", LDT.toLocalDate());
        assertEquals("", expectedMf1Result, mf.format(arguments));
        assertEquals("", expectedMf2Result, mf2.formatToString(arguments));
        // ZonedDateTime
        arguments.put("expDate", LDT.atZone(ZoneId.of("Europe/Paris")));
        assertEquals("", expectedMf1Result, mf.format(arguments));
        assertEquals("", expectedMf2Result, mf2.formatToString(arguments));
        // OffsetDateTime
        arguments.put("expDate", LDT.atOffset(ZoneOffset.ofHours(2)));
        assertEquals("", expectedMf1Result, mf.format(arguments));
        assertEquals("", expectedMf2Result, mf2.formatToString(arguments));
        // Instant
        Instant instant = LDT.toInstant(ZoneOffset.UTC);
        arguments.put("expDate", instant);
        assertEquals("", expectedMf1Result, mf.format(arguments));
        assertEquals("", expectedMf2Result, mf2.formatToString(arguments));
        // Clock
        arguments.put("expDate", Clock.fixed(instant, ZoneId.of("Europe/Paris")));
        assertEquals("", expectedMf1Result, mf.format(arguments));
        assertEquals("", expectedMf2Result, mf2.formatToString(arguments));

        // Test that both JDK and ICU Calendar are recognized as types.
        arguments.put("expDate", new java.util.GregorianCalendar(2013, 8, 27));
        // We don't test MessageFormat (MF1) with a java.util.Calendar
        // because it throws. The ICU DateFormat does not support it.
        // I filed https://unicode-org.atlassian.net/browse/ICU-22852
        // MF2 converts the JDK Calendar to an ICU Calendar, so it works.
        assertEquals("", expectedMf2Result, mf2.formatToString(arguments));
    }

    @Test
    public void testDateIntervalFormat() {
        Locale locale = Locale.FRENCH;
        String intervalSkeleton = "dMMMMy";
        LocalDate from = LocalDate.of(2024, Month.SEPTEMBER, 17);
        LocalDate to = LocalDate.of(2024, Month.SEPTEMBER, 23);
        StringBuffer result = new StringBuffer();

        result.setLength(0);
        DateIntervalFormat di = DateIntervalFormat.getInstance(intervalSkeleton, locale);
        assertEquals("", "17–23 septembre 2024",
                di.format(from, to, result, new FieldPosition(0)).toString());

        to = LocalDate.of(2024, Month.OCTOBER, 3);
        result.setLength(0);
        di = DateIntervalFormat.getInstance(intervalSkeleton, locale);
        assertEquals("", "17 septembre – 3 octobre 2024",
                di.format(from, to, result, new FieldPosition(0)).toString());

        // LocalDateTime. Date + time difference, same day, different times

        LocalDateTime fromDt = LocalDateTime.of(2024, Month.SEPTEMBER, 17, 9, 30, 0);
        LocalDateTime toDt = LocalDateTime.of(2024, Month.SEPTEMBER, 17, 18, 0, 0);

        result.setLength(0);
        di = DateIntervalFormat.getInstance("dMMMMy jm", locale);
        assertEquals("", "17 septembre 2024, 09:30 – 18:00",
                di.format(fromDt, toDt, result, new FieldPosition(0)).toString());

        // LocalDateTime. Time difference, same day

        LocalTime fromT = LocalTime.of(9, 30, 0);
        LocalTime toT = LocalTime.of(18, 0, 0);

        result.setLength(0);
        di = DateIntervalFormat.getInstance("jm", locale);
        assertEquals("", "09:30 – 18:00",
                di.format(fromT, toT, result, new FieldPosition(0)).toString());

        // Non-Gregorian output

        di = DateIntervalFormat.getInstance(intervalSkeleton, Locale.forLanguageTag("fr-u-ca-hebrew"));
        result.setLength(0);
        assertEquals("", "14 éloul – 1 tichri 5785 A. M.",
                di.format(from, to, result, new FieldPosition(0)).toString());

        di = DateIntervalFormat.getInstance(intervalSkeleton, Locale.forLanguageTag("fr-u-ca-coptic"));
        result.setLength(0);
        assertEquals("", "7 tout – 23 tout 1741 ap. D.",
                di.format(from, to, result, new FieldPosition(0)).toString());

        di = DateIntervalFormat.getInstance(intervalSkeleton, Locale.forLanguageTag("fr-u-ca-japanese"));
        result.setLength(0);
        assertEquals("", "17 septembre – 3 octobre 6 Reiwa",
                di.format(from, to, result, new FieldPosition(0)).toString());
    }
}
