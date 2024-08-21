// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.impl;

import static java.time.temporal.ChronoField.MILLI_OF_SECOND;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.temporal.Temporal;
import java.util.Date;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;


/**
 * This class provides utility methods for converting between Java 8's {@code java.time}
 * classes and the {@link com.ibm.icu.util.Calendar} and related classes from the
 * {@code com.ibm.icu.util} package.
 *
 * <p>
 * The class includes methods for converting various temporal types, such as
 * {@link Instant}, {@link ZonedDateTime}, {@link OffsetTime}, {@link OffsetDateTime}, {@link LocalTime},
 * {@link ChronoLocalDate}, and {@link ChronoLocalDateTime}, to {@link Calendar} instances.
 *
 * <p>
 * Additionally, it provides methods to convert between {@link ZoneId} and {@link TimeZone}, and
 * {@link ZoneOffset} and {@link TimeZone}.
 *
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public class JavaTimeConverters {
    // Milliseconds per day
    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1_000;

    private JavaTimeConverters() {
        // Prevent instantiation, making this an utility class
    }

    /**
     * Converts the current instant from a {@link Clock} to a {@link Calendar}.
     *
     * <p>
     * This method creates a {@link Calendar} instance that represents the current
     * instant as provided by the specified {@link Clock}.
     *
     * @param clock The {@link Clock} providing the current instant.
     * @return A {@link Calendar} instance representing the current instant as
     *         provided by the specified {@link Clock}.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Calendar temporalToCalendar(Clock clock) {
        long epochMillis = clock.millis();
        String timeZone = clock.getZone().getId();
        TimeZone icuTimeZone = TimeZone.getTimeZone(timeZone);
        return millisToCalendar(epochMillis, icuTimeZone);
    }

    /**
     * Converts an {@link Instant} to a {@link Calendar}.
     *
     * <p>
     * This method creates a {@link Calendar} instance that represents the same
     * point in time as the specified {@link Instant}. The resulting
     * {@link Calendar} will be in the default time zone of the JVM.
     *
     * @param instant The {@link Instant} to convert.
     * @return A {@link Calendar} instance representing the same point in time as
     *         the specified {@link Instant}.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Calendar temporalToCalendar(Instant instant) {
        long epochMillis = instant.toEpochMilli();
        return millisToCalendar(epochMillis, TimeZone.GMT_ZONE);
    }

    /**
     * Converts a {@link ZonedDateTime} to a {@link Calendar}.
     *
     * <p>
     * This method creates a {@link Calendar} instance that represents the same date
     * and time as the specified {@link ZonedDateTime}, taking into account the time
     * zone information associated with the {@link ZonedDateTime}.
     *
     * @param dateTime The {@link ZonedDateTime} to convert.
     * @return A {@link Calendar} instance representing the same date and time as
     *         the specified {@link ZonedDateTime}, with the time zone set
     *         accordingly.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Calendar temporalToCalendar(ZonedDateTime dateTime) {
        long epochMillis = dateTime.toEpochSecond() * 1_000 + dateTime.get(MILLI_OF_SECOND);
        TimeZone icuTimeZone = zoneIdToTimeZone(dateTime.getZone());
        return millisToCalendar(epochMillis, icuTimeZone);
    }

    /**
     * Converts an {@link OffsetTime} to a {@link Calendar}.
     *
     * <p>
     * This method creates a {@link Calendar} instance that represents the same time
     * of day as the specified {@link OffsetTime}, taking into account the offset
     * from UTC associated with the {@link OffsetTime}. The resulting
     * {@link Calendar} will have its date components (year, month, day) set to the
     * current date in the time zone represented by the offset.
     *
     * @param time The {@link OffsetTime} to convert.
     * @return A {@link Calendar} instance representing the same time of day as the
     *         specified {@link OffsetTime}, with the time zone set accordingly and
     *         date components set to the current date in that time zone.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Calendar temporalToCalendar(OffsetTime time) {
        return temporalToCalendar(time.atDate(LocalDate.now()));
    }

    /**
     * Converts an {@link OffsetDateTime} to a {@link Calendar}.
     *
     * <p>
     * This method creates a {@link Calendar} instance that represents the same date
     * and time as the specified {@link OffsetDateTime}, taking into account the
     * offset from UTC associated with the {@link OffsetDateTime}.
     *
     * @param dateTime The {@link OffsetDateTime} to convert.
     * @return A {@link Calendar} instance representing the same date and time as
     *         the specified {@link OffsetDateTime}, with the time zone set
     *         accordingly.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Calendar temporalToCalendar(OffsetDateTime dateTime) {
        long epochMillis = dateTime.toEpochSecond() * 1_000 + dateTime.get(MILLI_OF_SECOND);
        TimeZone icuTimeZone = zoneOffsetToTimeZone(dateTime.getOffset());
        return millisToCalendar(epochMillis, icuTimeZone);
    }

    /**
     * Converts a {@link ChronoLocalDate} to a {@link Calendar}.
     *
     * <p>
     * This method creates a {@link Calendar} instance that represents the same date
     * as the specified {@link ChronoLocalDate}. The resulting {@link Calendar} will
     * be in the default time zone of the JVM and will have its time components
     * (hour, minute, second, millisecond) set to zero.
     *
     * @param date The {@link ChronoLocalDate} to convert.
     * @return A {@link Calendar} instance representing the same date as the
     *         specified {@link ChronoLocalDate}, with time components set to zero.
     */
    @Deprecated
    static Calendar temporalToCalendar(ChronoLocalDate date) {
        long epochMillis = date.toEpochDay() * MILLIS_PER_DAY;
        return millisToCalendar(epochMillis);
    }

    /**
     * Converts a {@link LocalTime} to a {@link Calendar}.
     *
     * <p>
     * This method creates a {@link Calendar} instance that represents the same time
     * of day as the specified {@link LocalTime}. The resulting {@link Calendar}
     * will be in the default time zone of the JVM and will have its date components
     * (year, month, day) set to the current date in the default time zone.
     *
     * @param time The {@link LocalTime} to convert.
     * @return A {@link Calendar} instance representing the same time of day as the
     *         specified {@link LocalTime}, with date components set to the current
     *         date in the default time zone.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Calendar temporalToCalendar(LocalTime time) {
        long epochMillis = time.toNanoOfDay() / 1_000_000;
        return millisToCalendar(epochMillis);
    }

    /**
     * Converts a {@link ChronoLocalDateTime} to a {@link Calendar}.
     *
     * <p>
     * This method creates a {@link Calendar} instance that represents the same date
     * and time as the specified {@link ChronoLocalDateTime}. The resulting
     * {@link Calendar} will be in the default time zone of the JVM.
     *
     * @param dateTime The {@link ChronoLocalDateTime} to convert.
     * @return A {@link Calendar} instance representing the same date and time as
     *         the specified {@link ChronoLocalDateTime}.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Calendar temporalToCalendar(LocalDateTime dateTime) {
        ZoneOffset zoneOffset = ZoneId.systemDefault().getRules().getOffset(dateTime);
        long epochMillis = dateTime.toEpochSecond(zoneOffset) * 1_000 + dateTime.get(MILLI_OF_SECOND);
        return millisToCalendar(epochMillis, TimeZone.getDefault());
    }

    /**
     * Converts a {@link Temporal} to a {@link Calendar}.
     *
     * @param temp The {@link Temporal} to convert.
     * @return A {@link Calendar} instance representing the same date and time as
     *         the specified {@link Temporal}.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static Calendar temporalToCalendar(Temporal temp) {
        if (temp instanceof Clock) {
            return temporalToCalendar((Clock) temp);
        } else if (temp instanceof Instant) {
            return temporalToCalendar((Instant) temp);
        } else if (temp instanceof ZonedDateTime) {
            return temporalToCalendar((ZonedDateTime) temp);
        } else if (temp instanceof OffsetDateTime) {
            return temporalToCalendar((OffsetDateTime) temp);
        } else if (temp instanceof OffsetTime) {
            return temporalToCalendar((OffsetTime) temp);
        } else if (temp instanceof LocalDate) {
            return temporalToCalendar((LocalDate) temp);
        } else if (temp instanceof LocalDateTime) {
            return temporalToCalendar((LocalDateTime) temp);
        } else if (temp instanceof LocalTime) {
            return temporalToCalendar((LocalTime) temp);
        } else if (temp instanceof ChronoLocalDate) {
            return temporalToCalendar((ChronoLocalDate) temp);
        } else if (temp instanceof ChronoLocalDateTime) {
            return temporalToCalendar((ChronoLocalDateTime<?>) temp);
        } else {
            System.out.println("WTF is " + temp.getClass());
            return null;
        }
    }

    /**
     * Converts a {@link ZoneId} to a {@link TimeZone}.
     *
     * <p>
     * This method creates a {@link TimeZone} from the specified {@link ZoneId}. The
     * resulting {@link TimeZone} will represent the time zone rules associated with
     * the given {@link ZoneId}.
     *
     * @param zoneId The zone ID to convert.
     * @return A {@link TimeZone} representing the time zone rules associated with
     *         the given {@link ZoneId}.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static TimeZone zoneIdToTimeZone(ZoneId zoneId) {
        return TimeZone.getTimeZone(zoneId.getId());
    }

    /**
     * Converts a {@link ZoneOffset} to a {@link TimeZone}.
     *
     * <p>
     * This method creates a {@link TimeZone} that has a fixed offset from UTC,
     * represented by the given {@link ZoneOffset}.
     *
     * @param zoneOffset The zone offset to convert.
     * @return A {@link TimeZone} that has a fixed offset from UTC, represented by
     *         the given {@link ZoneOffset}.
     *
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static TimeZone zoneOffsetToTimeZone(ZoneOffset zoneOffset) {
        return new SimpleTimeZone(zoneOffset.getTotalSeconds() * 1_000, zoneOffset.getId());
    }

    private static Calendar millisToCalendar(long epochMillis) {
        return millisToCalendar(epochMillis, TimeZone.GMT_ZONE);
    }

    private static Calendar millisToCalendar(long epochMillis, TimeZone timeZone) {
        GregorianCalendar calendar = new GregorianCalendar(timeZone, ULocale.US);
        // java.time doesn't switch to Julian calendar
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        calendar.setTimeInMillis(epochMillis);
        return calendar;
    }
}
