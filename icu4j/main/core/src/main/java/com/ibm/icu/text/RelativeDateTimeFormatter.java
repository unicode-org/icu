// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.InvalidObjectException;
import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.util.EnumMap;
import java.util.Locale;

import com.ibm.icu.impl.CacheBase;
import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.FormattedValueStringBuilderImpl;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.SoftCache;
import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;


/**
 * Formats simple relative dates. There are two types of relative dates that
 * it handles:
 * <ul>
 *   <li>relative dates with a quantity e.g "in 5 days"</li>
 *   <li>relative dates without a quantity e.g "next Tuesday"</li>
 * </ul>
 * <p>
 * This API is very basic and is intended to be a building block for more
 * fancy APIs. The caller tells it exactly what to display in a locale
 * independent way. While this class automatically provides the correct plural
 * forms, the grammatical form is otherwise as neutral as possible. It is the
 * caller's responsibility to handle cut-off logic such as deciding between
 * displaying "in 7 days" or "in 1 week." This API supports relative dates
 * involving one single unit. This API does not support relative dates
 * involving compound units.
 * e.g "in 5 days and 4 hours" nor does it support parsing.
 * This class is both immutable and thread-safe.
 * <p>
 * Here are some examples of use:
 * <blockquote>
 * <pre>
 * RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance();
 * fmt.format(1, Direction.NEXT, RelativeUnit.DAYS); // "in 1 day"
 * fmt.format(3, Direction.NEXT, RelativeUnit.DAYS); // "in 3 days"
 * fmt.format(3.2, Direction.LAST, RelativeUnit.YEARS); // "3.2 years ago"
 *
 * fmt.format(Direction.LAST, AbsoluteUnit.SUNDAY); // "last Sunday"
 * fmt.format(Direction.THIS, AbsoluteUnit.SUNDAY); // "this Sunday"
 * fmt.format(Direction.NEXT, AbsoluteUnit.SUNDAY); // "next Sunday"
 * fmt.format(Direction.PLAIN, AbsoluteUnit.SUNDAY); // "Sunday"
 *
 * fmt.format(Direction.LAST, AbsoluteUnit.DAY); // "yesterday"
 * fmt.format(Direction.THIS, AbsoluteUnit.DAY); // "today"
 * fmt.format(Direction.NEXT, AbsoluteUnit.DAY); // "tomorrow"
 *
 * fmt.format(Direction.PLAIN, AbsoluteUnit.NOW); // "now"
 * </pre>
 * </blockquote>
 * <p>
 * In the future, we may add more forms, such as abbreviated/short forms
 * (3 secs ago), and relative day periods ("yesterday afternoon"), etc.
 *
 * @stable ICU 53
 */
public final class RelativeDateTimeFormatter {

    /**
     * The formatting style
     * @stable ICU 54
     *
     */
    public static enum Style {

        /**
         * Everything spelled out.
         * @stable ICU 54
         */
        LONG,

        /**
         * Abbreviations used when possible.
         * @stable ICU 54
         */
        SHORT,

        /**
         * Use single letters when possible.
         * @stable ICU 54
         */
        NARROW;

        private static final int INDEX_COUNT = 3;  // NARROW.ordinal() + 1
    }

    /**
     * Represents the unit for formatting a relative date. e.g "in 5 days"
     * or "in 3 months"
     * @stable ICU 53
     */
    public static enum RelativeUnit {

        /**
         * Seconds
         * @stable ICU 53
         */
        SECONDS,

        /**
         * Minutes
         * @stable ICU 53
         */
        MINUTES,

       /**
        * Hours
        * @stable ICU 53
        */
        HOURS,

        /**
         * Days
         * @stable ICU 53
         */
        DAYS,

        /**
         * Weeks
         * @stable ICU 53
         */
        WEEKS,

        /**
         * Months
         * @stable ICU 53
         */
        MONTHS,

        /**
         * Years
         * @stable ICU 53
         */
        YEARS,

        /**
         * Quarters
         * @internal TODO: propose for addition in ICU 57
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        QUARTERS,
    }

    /**
     * Represents an absolute unit.
     * @stable ICU 53
     */
    public static enum AbsoluteUnit {

       /**
        * Sunday
        * @stable ICU 53
        */
        SUNDAY,

        /**
         * Monday
         * @stable ICU 53
         */
        MONDAY,

        /**
         * Tuesday
         * @stable ICU 53
         */
        TUESDAY,

        /**
         * Wednesday
         * @stable ICU 53
         */
        WEDNESDAY,

        /**
         * Thursday
         * @stable ICU 53
         */
        THURSDAY,

        /**
         * Friday
         * @stable ICU 53
         */
        FRIDAY,

        /**
         * Saturday
         * @stable ICU 53
         */
        SATURDAY,

        /**
         * Day
         * @stable ICU 53
         */
        DAY,

        /**
         * Week
         * @stable ICU 53
         */
        WEEK,

        /**
         * Month
         * @stable ICU 53
         */
        MONTH,

        /**
         * Year
         * @stable ICU 53
         */
        YEAR,

        /**
         * Now
         * @stable ICU 53
         */
        NOW,

        /**
         * Quarter
         * @stable ICU 64
         */
        QUARTER,

        /**
         * Hour
         * @stable ICU 65
         */
        HOUR,

        /**
         * Minute
         * @stable ICU 65
         */
        MINUTE,
    }

    /**
     * Represents a direction for an absolute unit e.g "Next Tuesday"
     * or "Last Tuesday"
     * @stable ICU 53
     */
    public static enum Direction {
          /**
           * Two before. Not fully supported in every locale
           * @stable ICU 53
           */
          LAST_2,

          /**
           * Last
           * @stable ICU 53
           */
          LAST,

          /**
           * This
           * @stable ICU 53
           */
          THIS,

          /**
           * Next
           * @stable ICU 53
           */
          NEXT,

          /**
           * Two after. Not fully supported in every locale
           * @stable ICU 53
           */
          NEXT_2,

          /**
           * Plain, which means the absence of a qualifier
           * @stable ICU 53
           */
          PLAIN,
    }

    /**
     * Represents the unit for formatting a relative date. e.g "in 5 days"
     * or "next year"
     * @stable ICU 57
     */
    public static enum RelativeDateTimeUnit {
        /**
         * Specifies that relative unit is year, e.g. "last year",
         * "in 5 years".
         * @stable ICU 57
         */
        YEAR,
        /**
         * Specifies that relative unit is quarter, e.g. "last quarter",
         * "in 5 quarters".
         * @stable ICU 57
         */
        QUARTER,
        /**
         * Specifies that relative unit is month, e.g. "last month",
         * "in 5 months".
         * @stable ICU 57
         */
        MONTH,
        /**
         * Specifies that relative unit is week, e.g. "last week",
         * "in 5 weeks".
         * @stable ICU 57
         */
        WEEK,
        /**
         * Specifies that relative unit is day, e.g. "yesterday",
         * "in 5 days".
         * @stable ICU 57
         */
        DAY,
        /**
         * Specifies that relative unit is hour, e.g. "1 hour ago",
         * "in 5 hours".
         * @stable ICU 57
         */
        HOUR,
        /**
         * Specifies that relative unit is minute, e.g. "1 minute ago",
         * "in 5 minutes".
         * @stable ICU 57
         */
        MINUTE,
        /**
         * Specifies that relative unit is second, e.g. "1 second ago",
         * "in 5 seconds".
         * @stable ICU 57
         */
        SECOND,
        /**
         * Specifies that relative unit is Sunday, e.g. "last Sunday",
         * "this Sunday", "next Sunday", "in 5 Sundays".
         * @stable ICU 57
         */
        SUNDAY,
        /**
         * Specifies that relative unit is Monday, e.g. "last Monday",
         * "this Monday", "next Monday", "in 5 Mondays".
         * @stable ICU 57
         */
        MONDAY,
        /**
         * Specifies that relative unit is Tuesday, e.g. "last Tuesday",
         * "this Tuesday", "next Tuesday", "in 5 Tuesdays".
         * @stable ICU 57
         */
        TUESDAY,
        /**
         * Specifies that relative unit is Wednesday, e.g. "last Wednesday",
         * "this Wednesday", "next Wednesday", "in 5 Wednesdays".
         * @stable ICU 57
         */
        WEDNESDAY,
        /**
         * Specifies that relative unit is Thursday, e.g. "last Thursday",
         * "this Thursday", "next Thursday", "in 5 Thursdays".
         * @stable ICU 57
         */
        THURSDAY,
        /**
         * Specifies that relative unit is Friday, e.g. "last Friday",
         * "this Friday", "next Friday", "in 5 Fridays".
         * @stable ICU 57
         */
        FRIDAY,
        /**
         * Specifies that relative unit is Saturday, e.g. "last Saturday",
         * "this Saturday", "next Saturday", "in 5 Saturdays".
         * @stable ICU 57
         */
        SATURDAY,
    }

    /**
     * Field constants used when accessing field information for relative
     * datetime strings in FormattedValue.
     * <p>
     * There is no public constructor to this class; the only instances are the
     * constants defined here.
     * <p>
     * @stable ICU 64
     */
    public static class Field extends Format.Field {
        private static final long serialVersionUID = -5327685528663492325L;

        /**
         * Represents a literal text string, like "tomorrow" or "days ago".
         *
         * @stable ICU 64
         */
        public static final Field LITERAL = new Field("literal");

        /**
         * Represents a number quantity, like "3" in "3 days ago".
         *
         * @stable ICU 64
         */
        public static final Field NUMERIC = new Field("numeric");

        private Field(String fieldName) {
            super(fieldName);
        }

        /**
         * Serizalization method resolve instances to the constant Field values
         *
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (this.getName().equals(LITERAL.getName()))
                return LITERAL;
            if (this.getName().equals(NUMERIC.getName()))
                return NUMERIC;

            throw new InvalidObjectException("An invalid object.");
        }
    }

    /**
     * Represents the result of a formatting operation of a relative datetime.
     * Access the string value or field information.
     *
     * Instances of this class are immutable and thread-safe.
     *
     * Not intended for public subclassing.
     *
     * @author sffc
     * @stable ICU 64
     */
    public static class FormattedRelativeDateTime implements FormattedValue {

        private final FormattedStringBuilder string;

        private FormattedRelativeDateTime(FormattedStringBuilder string) {
            this.string = string;
        }

        /**
         * {@inheritDoc}
         *
         * @stable ICU 64
         */
        @Override
        public String toString() {
            return string.toString();
        }

        /**
         * {@inheritDoc}
         *
         * @stable ICU 64
         */
        @Override
        public int length() {
            return string.length();
        }

        /**
         * {@inheritDoc}
         *
         * @stable ICU 64
         */
        @Override
        public char charAt(int index) {
            return string.charAt(index);
        }

        /**
         * {@inheritDoc}
         *
         * @stable ICU 64
         */
        @Override
        public CharSequence subSequence(int start, int end) {
            return string.subString(start, end);
        }

        /**
         * {@inheritDoc}
         *
         * @stable ICU 64
         */
        @Override
        public <A extends Appendable> A appendTo(A appendable) {
            return Utility.appendTo(string, appendable);
        }

        /**
         * {@inheritDoc}
         *
         * @stable ICU 64
         */
        @Override
        public boolean nextPosition(ConstrainedFieldPosition cfpos) {
            return FormattedValueStringBuilderImpl.nextPosition(string, cfpos, Field.NUMERIC);
        }

        /**
         * {@inheritDoc}
         *
         * @stable ICU 64
         */
        @Override
        public AttributedCharacterIterator toCharacterIterator() {
            return FormattedValueStringBuilderImpl.toCharacterIterator(string, Field.NUMERIC);
        }
    }

    /**
     * Returns a RelativeDateTimeFormatter for the default locale.
     * @stable ICU 53
     */
    public static RelativeDateTimeFormatter getInstance() {
        return getInstance(ULocale.getDefault(), null, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    /**
     * Returns a RelativeDateTimeFormatter for a particular locale.
     *
     * @param locale the locale.
     * @return An instance of RelativeDateTimeFormatter.
     * @stable ICU 53
     */
    public static RelativeDateTimeFormatter getInstance(ULocale locale) {
        return getInstance(locale, null, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    /**
     * Returns a RelativeDateTimeFormatter for a particular {@link java.util.Locale}.
     *
     * @param locale the {@link java.util.Locale}.
     * @return An instance of RelativeDateTimeFormatter.
     * @stable ICU 54
     */
    public static RelativeDateTimeFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    /**
     * Returns a RelativeDateTimeFormatter for a particular locale that uses a particular
     * NumberFormat object.
     *
     * @param locale the locale
     * @param nf the number format object. It is defensively copied to ensure thread-safety
     * and immutability of this class.
     * @return An instance of RelativeDateTimeFormatter.
     * @stable ICU 53
     */
    public static RelativeDateTimeFormatter getInstance(ULocale locale, NumberFormat nf) {
        return getInstance(locale, nf, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    /**
     * Returns a RelativeDateTimeFormatter for a particular locale that uses a particular
     * NumberFormat object, style, and capitalization context
     *
     * @param locale the locale
     * @param nf the number format object. It is defensively copied to ensure thread-safety
     * and immutability of this class. May be null.
     * @param style the style.
     * @param capitalizationContext the capitalization context.
     * @stable ICU 54
     */
    public static RelativeDateTimeFormatter getInstance(
            ULocale locale,
            NumberFormat nf,
            Style style,
            DisplayContext capitalizationContext) {
        RelativeDateTimeFormatterData data = cache.get(locale);
        if (nf == null) {
            nf = NumberFormat.getInstance(locale);
        } else {
            nf = (NumberFormat) nf.clone();
        }
        return new RelativeDateTimeFormatter(
                data.qualitativeUnitMap,
                data.relUnitPatternMap,
                SimpleFormatterImpl.compileToStringMinMaxArguments(
                        data.dateTimePattern, new StringBuilder(), 2, 2),
                PluralRules.forLocale(locale),
                nf,
                style,
                capitalizationContext,
                capitalizationContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE ?
                    BreakIterator.getSentenceInstance(locale) : null,
                locale);
    }

    /**
     * Returns a RelativeDateTimeFormatter for a particular {@link java.util.Locale} that uses a
     * particular NumberFormat object.
     *
     * @param locale the {@link java.util.Locale}
     * @param nf the number format object. It is defensively copied to ensure thread-safety
     * and immutability of this class.
     * @return An instance of RelativeDateTimeFormatter.
     * @stable ICU 54
     */
    public static RelativeDateTimeFormatter getInstance(Locale locale, NumberFormat nf) {
        return getInstance(ULocale.forLocale(locale), nf);
    }

    /**
     * Formats a relative date with a quantity such as "in 5 days" or
     * "3 months ago".
     *
     * This method returns a String. To get more information about the
     * formatting result, use formatToValue().
     *
     * @param quantity The numerical amount e.g 5. This value is formatted
     * according to this object's {@link NumberFormat} object.
     * @param direction NEXT means a future relative date; LAST means a past
     * relative date.
     * @param unit the unit e.g day? month? year?
     * @return the formatted string
     * @throws IllegalArgumentException if direction is something other than
     * NEXT or LAST.
     * @stable ICU 53
     */
    public String format(double quantity, Direction direction, RelativeUnit unit) {
        FormattedStringBuilder output = formatImpl(quantity, direction, unit);
        return adjustForContext(output.toString());
    }

    /**
     * Formats a relative date with a quantity such as "in 5 days" or
     * "3 months ago".
     *
     * This method returns a FormattedRelativeDateTime, which exposes more
     * information than the String returned by format().
     *
     * @param quantity The numerical amount e.g 5. This value is formatted
     * according to this object's {@link NumberFormat} object.
     * @param direction NEXT means a future relative date; LAST means a past
     * relative date.
     * @param unit the unit e.g day? month? year?
     * @return the formatted relative datetime
     * @throws IllegalArgumentException if direction is something other than
     * NEXT or LAST.
     * @stable ICU 64
     */
    public FormattedRelativeDateTime formatToValue(double quantity, Direction direction, RelativeUnit unit) {
        checkNoAdjustForContext();
        return new FormattedRelativeDateTime(formatImpl(quantity, direction, unit));
    }

    /** Implementation method for format and formatToValue with RelativeUnit */
    private FormattedStringBuilder formatImpl(double quantity, Direction direction, RelativeUnit unit) {
        if (direction != Direction.LAST && direction != Direction.NEXT) {
            throw new IllegalArgumentException("direction must be NEXT or LAST");
        }
        int pastFutureIndex = (direction == Direction.NEXT ? 1 : 0);

        FormattedStringBuilder output = new FormattedStringBuilder();
        String pluralKeyword;
        if (numberFormat instanceof DecimalFormat) {
            DecimalQuantity dq = new DecimalQuantity_DualStorageBCD(quantity);
            ((DecimalFormat) numberFormat).toNumberFormatter().formatImpl(dq, output);
            pluralKeyword = pluralRules.select(dq);
        } else {
            String result = numberFormat.format(quantity);
            output.append(result, null);
            pluralKeyword = pluralRules.select(quantity);
        }
        StandardPlural pluralForm = StandardPlural.orOtherFromString(pluralKeyword);

        String compiledPattern = getRelativeUnitPluralPattern(style, unit, pastFutureIndex, pluralForm);
        SimpleFormatterImpl.formatPrefixSuffix(compiledPattern, Field.LITERAL, 0, output.length(), output);
        return output;
    }

    /**
     * Format a combination of RelativeDateTimeUnit and numeric offset
     * using a numeric style, e.g. "1 week ago", "in 1 week",
     * "5 weeks ago", "in 5 weeks".
     *
     * This method returns a String. To get more information about the
     * formatting result, use formatNumericToValue().
     *
     * @param offset    The signed offset for the specified unit. This
     *                  will be formatted according to this object's
     *                  NumberFormat object.
     * @param unit      The unit to use when formatting the relative
     *                  date, e.g. RelativeDateTimeUnit.WEEK,
     *                  RelativeDateTimeUnit.FRIDAY.
     * @return          The formatted string (may be empty in case of error)
     * @stable ICU 57
     */
    public String formatNumeric(double offset, RelativeDateTimeUnit unit) {
        FormattedStringBuilder output = formatNumericImpl(offset, unit);
        return adjustForContext(output.toString());
    }

    /**
     * Format a combination of RelativeDateTimeUnit and numeric offset
     * using a numeric style, e.g. "1 week ago", "in 1 week",
     * "5 weeks ago", "in 5 weeks".
     *
     * This method returns a FormattedRelativeDateTime, which exposes more
     * information than the String returned by formatNumeric().
     *
     * @param offset    The signed offset for the specified unit. This
     *                  will be formatted according to this object's
     *                  NumberFormat object.
     * @param unit      The unit to use when formatting the relative
     *                  date, e.g. RelativeDateTimeUnit.WEEK,
     *                  RelativeDateTimeUnit.FRIDAY.
     * @return          The formatted string (may be empty in case of error)
     * @stable ICU 64
     */
    public FormattedRelativeDateTime formatNumericToValue(double offset, RelativeDateTimeUnit unit) {
        checkNoAdjustForContext();
        return new FormattedRelativeDateTime(formatNumericImpl(offset, unit));
    }

    /** Implementation method for formatNumeric and formatNumericToValue */
    private FormattedStringBuilder formatNumericImpl(double offset, RelativeDateTimeUnit unit) {
        // TODO:
        // The full implementation of this depends on CLDR data that is not yet available,
        // see: http://unicode.org/cldr/trac/ticket/9165 Add more relative field data.
        // In the meantime do a quick bring-up by calling the old format method. When the
        // new CLDR data is available, update the data storage accordingly, rewrite this
        // to use it directly, and rewrite the old format method to call this new one;
        // that is covered by https://unicode-org.atlassian.net/browse/ICU-12171.
        RelativeUnit relunit = RelativeUnit.SECONDS;
        switch (unit) {
            case YEAR:      relunit = RelativeUnit.YEARS; break;
            case QUARTER:   relunit = RelativeUnit.QUARTERS; break;
            case MONTH:     relunit = RelativeUnit.MONTHS; break;
            case WEEK:      relunit = RelativeUnit.WEEKS; break;
            case DAY:       relunit = RelativeUnit.DAYS; break;
            case HOUR:      relunit = RelativeUnit.HOURS; break;
            case MINUTE:    relunit = RelativeUnit.MINUTES; break;
            case SECOND:    break; // set above
            default: // SUNDAY..SATURDAY
                throw new UnsupportedOperationException("formatNumeric does not currently support RelativeUnit.SUNDAY..SATURDAY");
        }
        Direction direction = Direction.NEXT;
        if (Double.compare(offset,0.0) < 0) { // needed to handle -0.0
            direction = Direction.LAST;
            offset = -offset;
        }
        return formatImpl(offset, direction, relunit);
    }

    private int[] styleToDateFormatSymbolsWidth = {
                DateFormatSymbols.WIDE, DateFormatSymbols.SHORT, DateFormatSymbols.NARROW
    };

    /**
     * Formats a relative date without a quantity.
     *
     * This method returns a String. To get more information about the
     * formatting result, use formatToValue().
     *
     * @param direction NEXT, LAST, THIS, etc.
     * @param unit e.g SATURDAY, DAY, MONTH
     * @return the formatted string. If direction has a value that is documented as not being
     *  fully supported in every locale (for example NEXT_2 or LAST_2) then this function may
     *  return null to signal that no formatted string is available.
     * @throws IllegalArgumentException if the direction is incompatible with
     * unit this can occur with NOW which can only take PLAIN.
     * @stable ICU 53
     */
    public String format(Direction direction, AbsoluteUnit unit) {
        String result = formatAbsoluteImpl(direction, unit);
        return result != null ? adjustForContext(result) : null;
    }

    /**
     * Formats a relative date without a quantity.
     *
     * This method returns a FormattedRelativeDateTime, which exposes more
     * information than the String returned by format().
     *
     * @param direction NEXT, LAST, THIS, etc.
     * @param unit e.g SATURDAY, DAY, MONTH
     * @return the formatted string. If direction has a value that is documented as not being
     *  fully supported in every locale (for example NEXT_2 or LAST_2) then this function may
     *  return null to signal that no formatted string is available.
     * @throws IllegalArgumentException if the direction is incompatible with
     * unit this can occur with NOW which can only take PLAIN.
     * @stable ICU 64
     */
    public FormattedRelativeDateTime formatToValue(Direction direction, AbsoluteUnit unit) {
        checkNoAdjustForContext();
        String string = formatAbsoluteImpl(direction, unit);
        if (string == null) {
            return null;
        }
        FormattedStringBuilder nsb = new FormattedStringBuilder();
        nsb.append(string, Field.LITERAL);
        return new FormattedRelativeDateTime(nsb);
    }

    /** Implementation method for format and formatToValue with AbsoluteUnit */
    private String formatAbsoluteImpl(Direction direction, AbsoluteUnit unit) {
        if (unit == AbsoluteUnit.NOW && direction != Direction.PLAIN) {
            throw new IllegalArgumentException("NOW can only accept direction PLAIN.");
        }
        String result;
        // Get plain day of week names from DateFormatSymbols.
        if ((direction == Direction.PLAIN) &&  (AbsoluteUnit.SUNDAY.ordinal() <= unit.ordinal() &&
                unit.ordinal() <= AbsoluteUnit.SATURDAY.ordinal())) {
            // Convert from AbsoluteUnit days to Calendar class indexing.
            int dateSymbolsDayOrdinal = (unit.ordinal() - AbsoluteUnit.SUNDAY.ordinal()) + Calendar.SUNDAY;
            String[] dayNames =
                    dateFormatSymbols.getWeekdays(DateFormatSymbols.STANDALONE,
                    styleToDateFormatSymbolsWidth[style.ordinal()]);
            result = dayNames[dateSymbolsDayOrdinal];
        } else {
            // Not PLAIN, or not a weekday.
            result = getAbsoluteUnitString(style, unit, direction);
        }
        return result;
    }

    /**
     * Format a combination of RelativeDateTimeUnit and numeric offset
     * using a text style if possible, e.g. "last week", "this week",
     * "next week", "yesterday", "tomorrow". Falls back to numeric
     * style if no appropriate text term is available for the specified
     * offset in the object’s locale.
     *
     * This method returns a String. To get more information about the
     * formatting result, use formatToValue().
     *
     * @param offset    The signed offset for the specified field.
     * @param unit      The unit to use when formatting the relative
     *                  date, e.g. RelativeDateTimeUnit.WEEK,
     *                  RelativeDateTimeUnit.FRIDAY.
     * @return          The formatted string (may be empty in case of error)
     * @stable ICU 57
     */
    public String format(double offset, RelativeDateTimeUnit unit) {
        return adjustForContext(formatRelativeImpl(offset, unit).toString());
    }

    /**
     * Format a combination of RelativeDateTimeUnit and numeric offset
     * using a text style if possible, e.g. "last week", "this week",
     * "next week", "yesterday", "tomorrow". Falls back to numeric
     * style if no appropriate text term is available for the specified
     * offset in the object’s locale.
     *
     * This method returns a FormattedRelativeDateTime, which exposes more
     * information than the String returned by format().
     *
     * @param offset    The signed offset for the specified field.
     * @param unit      The unit to use when formatting the relative
     *                  date, e.g. RelativeDateTimeUnit.WEEK,
     *                  RelativeDateTimeUnit.FRIDAY.
     * @return          The formatted string (may be empty in case of error)
     * @stable ICU 64
     */
    public FormattedRelativeDateTime formatToValue(double offset, RelativeDateTimeUnit unit) {
        checkNoAdjustForContext();
        CharSequence cs = formatRelativeImpl(offset, unit);
        FormattedStringBuilder nsb;
        if (cs instanceof FormattedStringBuilder) {
            nsb = (FormattedStringBuilder) cs;
        } else {
            nsb = new FormattedStringBuilder();
            nsb.append(cs, Field.LITERAL);
        }
        return new FormattedRelativeDateTime(nsb);
    }


    /** Implementation method for format and formatToValue with RelativeDateTimeUnit. */
    private CharSequence formatRelativeImpl(double offset, RelativeDateTimeUnit unit) {
        // TODO:
        // The full implementation of this depends on CLDR data that is not yet available,
        // see: http://unicode.org/cldr/trac/ticket/9165 Add more relative field data.
        // In the meantime do a quick bring-up by calling the old format method. When the
        // new CLDR data is available, update the data storage accordingly, rewrite this
        // to use it directly, and rewrite the old format method to call this new one;
        // that is covered by https://unicode-org.atlassian.net/browse/ICU-12171.
        boolean useNumeric = true;
        Direction direction = Direction.THIS;
        if (offset > -2.1 && offset < 2.1) {
            // Allow a 1% epsilon, so offsets in -1.01..-0.99 map to LAST
            double offsetx100 = offset * 100.0;
            int intoffsetx100 = (offsetx100 < 0)? (int)(offsetx100-0.5) : (int)(offsetx100+0.5);
            switch (intoffsetx100) {
                case -200/*-2*/: direction = Direction.LAST_2; useNumeric = false; break;
                case -100/*-1*/: direction = Direction.LAST;   useNumeric = false; break;
                case    0/* 0*/: useNumeric = false; break; // direction = Direction.THIS was set above
                case  100/* 1*/: direction = Direction.NEXT;   useNumeric = false; break;
                case  200/* 2*/: direction = Direction.NEXT_2; useNumeric = false; break;
                default: break;
            }
        }
        AbsoluteUnit absunit = AbsoluteUnit.NOW;
        switch (unit) {
            case YEAR:      absunit = AbsoluteUnit.YEAR;    break;
            case QUARTER:   absunit = AbsoluteUnit.QUARTER; break;
            case MONTH:     absunit = AbsoluteUnit.MONTH;   break;
            case WEEK:      absunit = AbsoluteUnit.WEEK;    break;
            case DAY:       absunit = AbsoluteUnit.DAY;     break;
            case SUNDAY:    absunit = AbsoluteUnit.SUNDAY;  break;
            case MONDAY:    absunit = AbsoluteUnit.MONDAY;  break;
            case TUESDAY:   absunit = AbsoluteUnit.TUESDAY; break;
            case WEDNESDAY: absunit = AbsoluteUnit.WEDNESDAY; break;
            case THURSDAY:  absunit = AbsoluteUnit.THURSDAY; break;
            case FRIDAY:    absunit = AbsoluteUnit.FRIDAY;  break;
            case SATURDAY:  absunit = AbsoluteUnit.SATURDAY; break;
            case HOUR:      absunit = AbsoluteUnit.HOUR;    break;
            case MINUTE:    absunit = AbsoluteUnit.MINUTE;  break;
            case SECOND:
                if (direction == Direction.THIS) {
                    // absunit = AbsoluteUnit.NOW was set above
                    direction = Direction.PLAIN;
                    break;
                }
                // could just fall through here but that produces warnings
                useNumeric = true;
                break;
            default:
                useNumeric = true;
                break;
        }
        if (!useNumeric) {
            String result = formatAbsoluteImpl(direction, absunit);
            if (result != null && result.length() > 0) {
                return result;
            }
        }
        // otherwise fallback to formatNumeric
        return formatNumericImpl(offset, unit);
    }

    /**
     * Gets the string value from qualitativeUnitMap with fallback based on style.
     */
    private String getAbsoluteUnitString(Style style, AbsoluteUnit unit, Direction direction) {
        EnumMap<AbsoluteUnit, EnumMap<Direction, String>> unitMap;
        EnumMap<Direction, String> dirMap;

        do {
            unitMap = qualitativeUnitMap.get(style);
            if (unitMap != null) {
                dirMap = unitMap.get(unit);
                if (dirMap != null) {
                    String result = dirMap.get(direction);
                    if (result != null) {
                        return result;
                    }
                }

            }

            // Consider other styles from alias fallback.
            // Data loading guaranteed no endless loops.
        } while ((style = fallbackCache[style.ordinal()]) != null);
        return null;
    }

    /**
     * Combines a relative date string and a time string in this object's
     * locale. This is done with the same date-time separator used for the
     * default calendar in this locale.
     * @param relativeDateString the relative date e.g 'yesterday'
     * @param timeString the time e.g '3:45'
     * @return the date and time concatenated according to the default
     * calendar in this locale e.g 'yesterday, 3:45'
     * @stable ICU 53
     */
    public String combineDateAndTime(String relativeDateString, String timeString) {
        return SimpleFormatterImpl.formatCompiledPattern(
                combinedDateAndTime, timeString, relativeDateString);
    }

    /**
     * Returns a copy of the NumberFormat this object is using.
     * @return A copy of the NumberFormat.
     * @stable ICU 53
     */
    public NumberFormat getNumberFormat() {
        // This class is thread-safe, yet numberFormat is not. To ensure thread-safety of this
        // class we must guarantee that only one thread at a time uses our numberFormat.
        synchronized (numberFormat) {
            return (NumberFormat) numberFormat.clone();
        }
    }

    /**
     * Return capitalization context.
     * @return The capitalization context.
     * @stable ICU 54
     */
    public DisplayContext getCapitalizationContext() {
        return capitalizationContext;
    }

    /**
     * Return style
     * @return The formatting style.
     * @stable ICU 54
     */
    public Style getFormatStyle() {
        return style;
    }

    private String adjustForContext(String originalFormattedString) {
        if (breakIterator == null || originalFormattedString.length() == 0
                || !UCharacter.isLowerCase(UCharacter.codePointAt(originalFormattedString, 0))) {
            return originalFormattedString;
        }
        synchronized (breakIterator) {
            return UCharacter.toTitleCase(
                    locale,
                    originalFormattedString,
                    breakIterator,
                    UCharacter.TITLECASE_NO_LOWERCASE | UCharacter.TITLECASE_NO_BREAK_ADJUSTMENT);
        }
    }

    private void checkNoAdjustForContext() {
        if (breakIterator != null) {
            throw new UnsupportedOperationException("Capitalization context is not supported in formatV");
        }
    }

    private RelativeDateTimeFormatter(
            EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap,
            EnumMap<Style, EnumMap<RelativeUnit, String[][]>> patternMap,
            String combinedDateAndTime,
            PluralRules pluralRules,
            NumberFormat numberFormat,
            Style style,
            DisplayContext capitalizationContext,
            BreakIterator breakIterator,
            ULocale locale) {
        this.qualitativeUnitMap = qualitativeUnitMap;
        this.patternMap = patternMap;
        this.combinedDateAndTime = combinedDateAndTime;
        this.pluralRules = pluralRules;
        this.numberFormat = numberFormat;
        this.style = style;
        if (capitalizationContext.type() != DisplayContext.Type.CAPITALIZATION) {
            throw new IllegalArgumentException(capitalizationContext.toString());
        }
        this.capitalizationContext = capitalizationContext;
        this.breakIterator = breakIterator;
        this.locale = locale;
        this.dateFormatSymbols = new DateFormatSymbols(locale);
    }

    private String getRelativeUnitPluralPattern(
            Style style, RelativeUnit unit, int pastFutureIndex, StandardPlural pluralForm) {
        if (pluralForm != StandardPlural.OTHER) {
            String formatter = getRelativeUnitPattern(style, unit, pastFutureIndex, pluralForm);
            if (formatter != null) {
                return formatter;
            }
        }
        return getRelativeUnitPattern(style, unit, pastFutureIndex, StandardPlural.OTHER);
    }

    private String getRelativeUnitPattern(
            Style style, RelativeUnit unit, int pastFutureIndex, StandardPlural pluralForm) {
        int pluralIndex = pluralForm.ordinal();
        do {
            EnumMap<RelativeUnit, String[][]> unitMap = patternMap.get(style);
            if (unitMap != null) {
                String[][] spfCompiledPatterns = unitMap.get(unit);
                if (spfCompiledPatterns != null) {
                    if (spfCompiledPatterns[pastFutureIndex][pluralIndex] != null) {
                        return spfCompiledPatterns[pastFutureIndex][pluralIndex];
                    }
                }

            }

            // Consider other styles from alias fallback.
            // Data loading guaranteed no endless loops.
        } while ((style = fallbackCache[style.ordinal()]) != null);
        return null;
    }

    private final EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap;
    private final EnumMap<Style, EnumMap<RelativeUnit, String[][]>> patternMap;

    private final String combinedDateAndTime;  // compiled SimpleFormatter pattern
    private final PluralRules pluralRules;
    private final NumberFormat numberFormat;

    private final Style style;
    private final DisplayContext capitalizationContext;
    private final BreakIterator breakIterator;
    private final ULocale locale;

    private final DateFormatSymbols dateFormatSymbols;

    private static final Style fallbackCache[] = new Style[Style.INDEX_COUNT];

    private static class RelativeDateTimeFormatterData {
        public RelativeDateTimeFormatterData(
                EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap,
                EnumMap<Style, EnumMap<RelativeUnit, String[][]>> relUnitPatternMap,
                String dateTimePattern) {
            this.qualitativeUnitMap = qualitativeUnitMap;
            this.relUnitPatternMap = relUnitPatternMap;

            this.dateTimePattern = dateTimePattern;
        }

        public final EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap;
        EnumMap<Style, EnumMap<RelativeUnit, String[][]>> relUnitPatternMap;
        public final String dateTimePattern;  // Example: "{1}, {0}"
    }

    private static class Cache {
        private final CacheBase<String, RelativeDateTimeFormatterData, ULocale> cache =
            new SoftCache<String, RelativeDateTimeFormatterData, ULocale>() {
                @Override
                protected RelativeDateTimeFormatterData createInstance(String key, ULocale locale) {
                    return new Loader(locale).load();
                }
            };

        public RelativeDateTimeFormatterData get(ULocale locale) {
            String key = locale.toString();
            return cache.getInstance(key, locale);
        }
    }

    private static Direction keyToDirection(UResource.Key key) {
        if (key.contentEquals("-2")) {
            return Direction.LAST_2;
        }
        if (key.contentEquals("-1")) {
            return Direction.LAST;
        }
        if (key.contentEquals("0")) {
            return Direction.THIS;
        }
        if (key.contentEquals("1")) {
            return Direction.NEXT;
        }
        if (key.contentEquals("2")) {
            return Direction.NEXT_2;
        }
        return null;
    }

    /**
     * Sink for enumerating all of the relative data time formatter names.
     *
     * More specific bundles (en_GB) are enumerated before their parents (en_001, en, root):
     * Only store a value if it is still missing, that is, it has not been overridden.
     */
    private static final class RelDateTimeDataSink extends UResource.Sink {

        // For white list of units to handle in RelativeDateTimeFormatter.
        private enum DateTimeUnit {
            SECOND(RelativeUnit.SECONDS, null),
            MINUTE(RelativeUnit.MINUTES, AbsoluteUnit.MINUTE),
            HOUR(RelativeUnit.HOURS, AbsoluteUnit.HOUR),
            DAY(RelativeUnit.DAYS, AbsoluteUnit.DAY),
            WEEK(RelativeUnit.WEEKS, AbsoluteUnit.WEEK),
            MONTH(RelativeUnit.MONTHS, AbsoluteUnit.MONTH),
            QUARTER(RelativeUnit.QUARTERS, AbsoluteUnit.QUARTER),
            YEAR(RelativeUnit.YEARS, AbsoluteUnit.YEAR),
            SUNDAY(null, AbsoluteUnit.SUNDAY),
            MONDAY(null, AbsoluteUnit.MONDAY),
            TUESDAY(null, AbsoluteUnit.TUESDAY),
            WEDNESDAY(null, AbsoluteUnit.WEDNESDAY),
            THURSDAY(null, AbsoluteUnit.THURSDAY),
            FRIDAY(null, AbsoluteUnit.FRIDAY),
            SATURDAY(null, AbsoluteUnit.SATURDAY);

            RelativeUnit relUnit;
            AbsoluteUnit absUnit;

            DateTimeUnit(RelativeUnit relUnit, AbsoluteUnit absUnit) {
                this.relUnit = relUnit;
                this.absUnit = absUnit;
            }

            private static final DateTimeUnit orNullFromString(CharSequence keyword) {
                // Quick check from string to enum.
                switch (keyword.length()) {
                case 3:
                    if ("day".contentEquals(keyword)) {
                        return DAY;
                    } else if ("sun".contentEquals(keyword)) {
                        return SUNDAY;
                    } else if ("mon".contentEquals(keyword)) {
                        return MONDAY;
                    } else if ("tue".contentEquals(keyword)) {
                        return TUESDAY;
                    } else if ("wed".contentEquals(keyword)) {
                        return WEDNESDAY;
                    } else if ("thu".contentEquals(keyword)) {
                        return THURSDAY;
                    }    else if ("fri".contentEquals(keyword)) {
                        return FRIDAY;
                    } else if ("sat".contentEquals(keyword)) {
                        return SATURDAY;
                    }
                    break;
                case 4:
                    if ("hour".contentEquals(keyword)) {
                        return HOUR;
                    } else if ("week".contentEquals(keyword)) {
                        return WEEK;
                    } else if ("year".contentEquals(keyword)) {
                        return YEAR;
                    }
                    break;
                case 5:
                    if ("month".contentEquals(keyword)) {
                        return MONTH;
                    }
                    break;
                case 6:
                    if ("minute".contentEquals(keyword)) {
                        return MINUTE;
                    }else if ("second".contentEquals(keyword)) {
                        return SECOND;
                    }
                    break;
                case 7:
                    if ("quarter".contentEquals(keyword)) {
                        return QUARTER;  // RelativeUnit.QUARTERS is deprecated
                    }
                    break;
                default:
                    break;
                }
                return null;
            }
        }

        EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap =
                new EnumMap<>(Style.class);
        EnumMap<Style, EnumMap<RelativeUnit, String[][]>> styleRelUnitPatterns =
                new EnumMap<>(Style.class);

        StringBuilder sb = new StringBuilder();

        // Values keep between levels of parsing the CLDR data.
        int pastFutureIndex;
        Style style;                        // {LONG, SHORT, NARROW} Derived from unit key string.
        DateTimeUnit unit;                  // From the unit key string, with the style (e.g., "-short") separated out.

        private Style styleFromKey(UResource.Key key) {
            if (key.endsWith("-short")) {
                return Style.SHORT;
            } else if (key.endsWith("-narrow")) {
                return Style.NARROW;
            } else {
                return Style.LONG;
            }
        }

        private Style styleFromAlias(UResource.Value value) {
                String s = value.getAliasString();
                if (s.endsWith("-short")) {
                    return Style.SHORT;
                } else if (s.endsWith("-narrow")) {
                    return Style.NARROW;
                } else {
                    return Style.LONG;
                }
        }

        private static int styleSuffixLength(Style style) {
            switch (style) {
            case SHORT: return 6;
            case NARROW: return 7;
            default: return 0;
            }
        }

        public void consumeTableRelative(UResource.Key key, UResource.Value value) {
            UResource.Table unitTypesTable = value.getTable();
            for (int i = 0; unitTypesTable.getKeyAndValue(i, key, value); i++) {
                if (value.getType() == ICUResourceBundle.STRING) {
                    String valueString = value.getString();

                    EnumMap<AbsoluteUnit, EnumMap<Direction, String>> absMap = qualitativeUnitMap.get(style);

                    if (unit.relUnit == RelativeUnit.SECONDS) {
                        if (key.contentEquals("0")) {
                            // Handle Zero seconds for "now".
                            EnumMap<Direction, String> unitStrings = absMap.get(AbsoluteUnit.NOW);
                            if (unitStrings == null) {
                                unitStrings = new EnumMap<>(Direction.class);
                                absMap.put(AbsoluteUnit.NOW, unitStrings);
                            }
                            if (unitStrings.get(Direction.PLAIN) == null) {
                                unitStrings.put(Direction.PLAIN, valueString);
                            }
                            continue;
                        }
                    }
                    Direction keyDirection = keyToDirection(key);
                    if (keyDirection == null) {
                        continue;
                    }
                    AbsoluteUnit absUnit = unit.absUnit;
                    if (absUnit == null) {
                        continue;
                    }

                    if (absMap == null) {
                        absMap = new EnumMap<>(AbsoluteUnit.class);
                        qualitativeUnitMap.put(style, absMap);
                    }
                    EnumMap<Direction, String> dirMap = absMap.get(absUnit);
                    if (dirMap == null) {
                        dirMap = new EnumMap<>(Direction.class);
                        absMap.put(absUnit, dirMap);
                    }
                    if (dirMap.get(keyDirection) == null) {
                        // Do not override values already entered.
                        dirMap.put(keyDirection, value.getString());
                    }
                }
            }
        }

        // Record past or future and
        public void consumeTableRelativeTime(UResource.Key key, UResource.Value value) {
            if (unit.relUnit == null) {
                return;
            }
            UResource.Table unitTypesTable = value.getTable();
            for (int i = 0; unitTypesTable.getKeyAndValue(i, key, value); i++) {
                if (key.contentEquals("past")) {
                    pastFutureIndex = 0;
                } else if (key.contentEquals("future")) {
                    pastFutureIndex = 1;
                } else {
                    continue;
                }
                // Get the details of the relative time.
                consumeTimeDetail(key, value);
            }
        }

        public void consumeTimeDetail(UResource.Key key, UResource.Value value) {
            UResource.Table unitTypesTable = value.getTable();

            EnumMap<RelativeUnit, String[][]> unitPatterns  = styleRelUnitPatterns.get(style);
            if (unitPatterns == null) {
                unitPatterns = new EnumMap<>(RelativeUnit.class);
                styleRelUnitPatterns.put(style, unitPatterns);
            }
            String[][] patterns = unitPatterns.get(unit.relUnit);
            if (patterns == null) {
                patterns = new String[2][StandardPlural.COUNT];
                unitPatterns.put(unit.relUnit, patterns);
            }

            // Stuff the pattern for the correct plural index with a simple formatter.
            for (int i = 0; unitTypesTable.getKeyAndValue(i, key, value); i++) {
                if (value.getType() == ICUResourceBundle.STRING) {
                    int pluralIndex = StandardPlural.indexFromString(key.toString());
                    if (patterns[pastFutureIndex][pluralIndex] == null) {
                        patterns[pastFutureIndex][pluralIndex] =
                                SimpleFormatterImpl.compileToStringMinMaxArguments(
                                        value.getString(), sb, 0, 1);
                    }
                }
            }
        }

        private void handlePlainDirection(UResource.Key key, UResource.Value value) {
            AbsoluteUnit absUnit = unit.absUnit;
            if (absUnit == null) {
                return;  // Not interesting.
            }
            EnumMap<AbsoluteUnit, EnumMap<Direction, String>> unitMap =
                    qualitativeUnitMap.get(style);
            if (unitMap == null) {
                unitMap = new EnumMap<>(AbsoluteUnit.class);
                qualitativeUnitMap.put(style, unitMap);
            }
            EnumMap<Direction,String> dirMap = unitMap.get(absUnit);
            if (dirMap == null) {
                dirMap = new EnumMap<>(Direction.class);
                unitMap.put(absUnit, dirMap);
            }
            if (dirMap.get(Direction.PLAIN) == null) {
                dirMap.put(Direction.PLAIN, value.toString());
            }
        }

        // Handle at the Unit level,
        public void consumeTimeUnit(UResource.Key key, UResource.Value value) {
            UResource.Table unitTypesTable = value.getTable();
            for (int i = 0; unitTypesTable.getKeyAndValue(i, key, value); i++) {
                if (key.contentEquals("dn") && value.getType() == ICUResourceBundle.STRING) {
                    handlePlainDirection(key, value);
                }
                if (value.getType() == ICUResourceBundle.TABLE) {
                    if (key.contentEquals("relative")) {
                        consumeTableRelative(key, value);
                    } else if (key.contentEquals("relativeTime")) {
                        consumeTableRelativeTime(key, value);
                    }
                }
            }
        }

        private void handleAlias(UResource.Key key, UResource.Value value, boolean noFallback) {
            Style sourceStyle = styleFromKey(key);
            int limit = key.length() - styleSuffixLength(sourceStyle);
            DateTimeUnit unit = DateTimeUnit.orNullFromString(key.substring(0, limit));
            if (unit != null) {
                // Record the fallback chain for the values.
                // At formatting time, limit to 2 levels of fallback.
                Style targetStyle = styleFromAlias(value);
                if (sourceStyle == targetStyle) {
                    throw new ICUException("Invalid style fallback from " + sourceStyle + " to itself");
                }

                // Check for inconsistent fallbacks.
                if (fallbackCache[sourceStyle.ordinal()] == null) {
                    fallbackCache[sourceStyle.ordinal()] = targetStyle;
                } else if (fallbackCache[sourceStyle.ordinal()] != targetStyle) {
                    throw new ICUException(
                            "Inconsistent style fallback for style " + sourceStyle + " to " + targetStyle);
                }
                return;
            }
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean noFallback) {
            // Main entry point to sink
            if (value.getType() == ICUResourceBundle.ALIAS) {
                return;
            }

            UResource.Table table = value.getTable();
            // Process each key / value in this table.
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (value.getType() == ICUResourceBundle.ALIAS) {
                    handleAlias(key, value, noFallback);
                } else {
                    // Remember style and unit for deeper levels.
                    style = styleFromKey(key);
                    int limit = key.length() - styleSuffixLength(style);
                    unit = DateTimeUnit.orNullFromString(key.substring(0, limit));
                    if (unit != null) {
                        // Process only if unitString is in the white list.
                        consumeTimeUnit(key, value);
                    }
                }
            }
        }

        RelDateTimeDataSink() {
        }
    }

    private static class Loader {
        private final ULocale ulocale;

        public Loader(ULocale ulocale) {
            this.ulocale = ulocale;
        }

        private String getDateTimePattern() {
            Calendar cal = Calendar.getInstance(ulocale);
            return Calendar.getDateAtTimePattern(cal, ulocale, DateFormat.MEDIUM);
        }

        public RelativeDateTimeFormatterData load() {
            // Sink for traversing data.
            RelDateTimeDataSink sink = new RelDateTimeDataSink();

            ICUResourceBundle r = (ICUResourceBundle)UResourceBundle.
                    getBundleInstance(ICUData.ICU_BASE_NAME, ulocale);
            r.getAllItemsWithFallback("fields", sink);

            // Check fallbacks array for loops or too many levels.
            for (Style testStyle : Style.values()) {
                Style newStyle1 = fallbackCache[testStyle.ordinal()];
                // Data loading guaranteed newStyle1 != testStyle.
                if (newStyle1 != null) {
                    Style newStyle2 = fallbackCache[newStyle1.ordinal()];
                    if (newStyle2 != null) {
                        // No fallback should take more than 2 steps.
                        if (fallbackCache[newStyle2.ordinal()] != null) {
                            throw new IllegalStateException("Style fallback too deep");
                        }
                    }
                }
            }

            return new RelativeDateTimeFormatterData(
                    sink.qualitativeUnitMap, sink.styleRelUnitPatterns,
                    getDateTimePattern());
        }
    }

    private static final Cache cache = new Cache();
}
