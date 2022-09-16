// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.ibm.icu.impl.locale.AsciiUtil;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * Creates a {@link Formatter} doing formatting of date / time, similar to
 * <code>{exp, date}</code> and <code>{exp, time}</code> in {@link com.ibm.icu.text.MessageFormat}.
 */
class DateTimeFormatterFactory implements FormatterFactory {

    private static int stringToStyle(String option) {
        switch (AsciiUtil.toUpperString(option)) {
            case "FULL": return DateFormat.FULL;
            case "LONG": return DateFormat.LONG;
            case "MEDIUM": return DateFormat.MEDIUM;
            case "SHORT": return DateFormat.SHORT;
            case "": // intentional fall-through
            case "DEFAULT": return DateFormat.DEFAULT;
            default: throw new IllegalArgumentException("Invalid datetime style: " + option);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException when something goes wrong
     *         (for example conflicting options, invalid option values, etc.)
     */
    @Override
    public Formatter createFormatter(Locale locale, Map<String, Object> fixedOptions) {
        DateFormat df;

        // TODO: how to handle conflicts. What if we have both skeleton and style, or pattern?
        Object opt = fixedOptions.get("skeleton");
        if (opt != null) {
            String skeleton = Objects.toString(opt);
            df = DateFormat.getInstanceForSkeleton(skeleton, locale);
            return new DateTimeFormatter(df);
        }

        opt = fixedOptions.get("pattern");
        if (opt != null) {
            String pattern = Objects.toString(opt);
            SimpleDateFormat sf = new SimpleDateFormat(pattern, locale);
            return new DateTimeFormatter(sf);
        }

        int dateStyle = DateFormat.NONE;
        opt = fixedOptions.get("datestyle");
        if (opt != null) {
            dateStyle = stringToStyle(Objects.toString(opt, ""));
        }

        int timeStyle = DateFormat.NONE;
        opt = fixedOptions.get("timestyle");
        if (opt != null) {
            timeStyle = stringToStyle(Objects.toString(opt, ""));
        }

        if (dateStyle == DateFormat.NONE && timeStyle == DateFormat.NONE) {
            // Match the MessageFormat behavior
            dateStyle = DateFormat.SHORT;
            timeStyle = DateFormat.SHORT;
        }
        df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);

        return new DateTimeFormatter(df);
    }

    private static class DateTimeFormatter implements Formatter {
        private final DateFormat icuFormatter;

        private DateTimeFormatter(DateFormat df) {
            this.icuFormatter = df;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
            // TODO: use a special type to indicate function without input argument.
            if (toFormat == null) {
                throw new IllegalArgumentException("The date to format can't be null");
            }
            String result = icuFormatter.format(toFormat);
            return new FormattedPlaceholder(toFormat, new PlainStringFormattedValue(result));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
            return format(toFormat, variableOptions).toString();
        }
    }
}
