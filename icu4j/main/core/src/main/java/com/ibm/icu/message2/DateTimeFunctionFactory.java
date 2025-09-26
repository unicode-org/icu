// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.temporal.Temporal;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.impl.JavaTimeConverters;
import com.ibm.icu.text.DateFormat;

/**
 * Creates a {@link Function} doing formatting of date / time, similar to
 * <code>{exp, date}</code> and <code>{exp, time}</code> in {@link com.ibm.icu.text.MessageFormat}.
 *
 * It does not do selection.
 */
class DateTimeFunctionFactory implements FunctionFactory {
    private final String kind;

    // "datetime", "date", "time"
    DateTimeFunctionFactory(String kind) {
        switch (kind) {
            case "date":
                break;
            case "time":
                break;
            case "datetime":
                break;
            default:
                kind = "datetime";
        }
        this.kind = kind;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException when something goes wrong
     *         (for example conflicting options, invalid option values, etc.)
     */
    @Override
    public Function create(Locale locale, Map<String, Object> fixedOptions) {
        locale = OptUtils.getBestLocale(fixedOptions, locale);
        Directionality dir = OptUtils.getBestDirectionality(fixedOptions, locale);

        boolean reportErrors = OptUtils.reportErrors(fixedOptions);

        // TODO: how to handle conflicts. What if we have both skeleton and style, or pattern?
        String skeleton = "";
        switch (kind) {
            case "date":
                skeleton = getDateFieldOptions(fixedOptions, "fields", "length");
                break;
            case "time":
                skeleton = getTimeFieldOptions(fixedOptions, "precision");
                break;
            case "datetime": // $FALL-THROUGH$
            default:
                skeleton = getDateFieldOptions(fixedOptions, "dateFields", "dateLength");
                skeleton += getTimeFieldOptions(fixedOptions, "timePrecision");
                break;
        }

        if (skeleton.isEmpty()) {
            // Custom option, icu namespace
            skeleton = OptUtils.getString(fixedOptions, "icu:skeleton", "");
        }

        if (skeleton.isEmpty()) {
            // No MF2 standard options and no icu:skeleton, use defaults
            skeleton = "";
            // No skeletons, custom or otherwise, match fallback to short / short as per spec.
            switch (kind) {
                case "date": // {$d :date fields=year-month-day length=medium}
                    skeleton = DATE_STYLES_TO_SKELETON.get("year-month-day-weekday::medium");
                    break;
                case "time": // {$t :time precision=minute}
                    skeleton = TIME_STYLES_TO_SKELETON.get("minute::");
                    break;
                case "datetime": // $FALL-THROUGH$
                default: // {$d :datetime dateFields=year-month-day timePrecision=minute}
                    skeleton = DATE_STYLES_TO_SKELETON.get("year-month-day-weekday::medium")
                            + TIME_STYLES_TO_SKELETON.get("minute::");
            }
        }

        com.ibm.icu.util.TimeZone tz = com.ibm.icu.util.TimeZone.getDefault();
        String timeZoneOverride = OptUtils.getString(fixedOptions, "timeZone", "");
        if (!timeZoneOverride.isEmpty()) {
            tz = com.ibm.icu.util.TimeZone.getTimeZone(timeZoneOverride);
        }

        String calendarOverride = OptUtils.getString(fixedOptions, "calendar", "");
        if (!calendarOverride.isEmpty()) {
            locale = new Locale.Builder()
                    .setLocale(locale)
                    .setUnicodeLocaleKeyword("ca", calendarOverride)
                    .build();
        }

        DateFormat df = DateFormat.getInstanceForSkeleton(skeleton, locale);
        if (!tz.equals(com.ibm.icu.util.TimeZone.UNKNOWN_ZONE)) {
            df.setTimeZone(tz);
        }

        return new DateTimeFunctionImpl(locale, df, reportErrors);
    }

    private static Map<String, String> DATE_STYLES_TO_SKELETON = Map.ofEntries(
            // dateFields + dateLength
            Map.entry("weekday::long", "EEEE"),
            Map.entry("weekday::medium", "E"),
            Map.entry("weekday::short", "EEEEEE"),
            Map.entry("day-weekday::long", "dEEEE"),
            Map.entry("day-weekday::medium", "dE"),
            Map.entry("day-weekday::short", "dEEEEEE"),
            Map.entry("month-day::long", "MMMMd"),
            Map.entry("month-day::medium", "MMMd"),
            Map.entry("month-day::short", "Md"),
            Map.entry("month-day-weekday::long", "MMMMdEEEE"),
            Map.entry("month-day-weekday::medium", "MMMdE"),
            Map.entry("month-day-weekday::short", "MdEEEEEE"),
            Map.entry("year-month-day::long", "yMMMMd"),
            Map.entry("year-month-day::medium", "yMMMd"),
            Map.entry("year-month-day::short", "yMd"),
            Map.entry("year-month-day-weekday::long", "yMMMMdEEEE"),
            Map.entry("year-month-day-weekday::medium", "yMMMdE"),
            Map.entry("year-month-day-weekday::short", "yMdEEEEEE")
    );

    private static Map<String, String> TIME_STYLES_TO_SKELETON = Map.ofEntries(
            // timePrecision + hour12
            Map.entry("hour::", "j"),
            Map.entry("hour::true", "h"),
            Map.entry("hour::false", "H"),
            Map.entry("minute::", "jm"),
            Map.entry("minute::true", "hm"),
            Map.entry("minute::false", "Hm"),
            Map.entry("second::", "jms"),
            Map.entry("second::true", "hms"),
            Map.entry("second::false", "Hms")
    );

    private static String getDateFieldOptions(Map<String, Object> options,
            String fieldName, String lengthName) {
        StringBuilder skeleton = new StringBuilder();
        String opt;

        // In all the switches below we just ignore invalid options.
        // Would be nice to report (log?), but ICU does not have a clear policy on how to do that.
        // But we don't want to throw, that is too drastic.

        opt = OptUtils.getString(options, fieldName, "")
                + "::"
                + OptUtils.getString(options, lengthName, "");
        opt = DATE_STYLES_TO_SKELETON.get(opt);
        if (opt != null) {
            skeleton.append(opt);
        }

        return skeleton.toString();
    }

    private static String getTimeFieldOptions(Map<String, Object> options,
            String precisionName) {
        StringBuilder skeleton = new StringBuilder();
        String opt;

        // In all the switches below we just ignore invalid options.
        // Would be nice to report (log?), but ICU does not have a clear policy on how to do that.
        // But we don't want to throw, that is too drastic.

        opt = OptUtils.getString(options, precisionName, "")
                + "::"
                + OptUtils.getString(options, "hour12", "");
        opt = TIME_STYLES_TO_SKELETON.get(opt);
        if (opt != null) {
            skeleton.append(opt);
        }

        opt = OptUtils.getString(options, "timeZoneStyle", "");
        switch (opt) {
            case "long":
                skeleton.append("zzzz");
                break;
            case "short":
                skeleton.append("z");
                break;
            default:
                break;
        }

        return skeleton.toString();
    }

    private static class DateTimeFunctionImpl implements Function {
        private final DateFormat icuFormatter;
        private final Locale locale;
        private final boolean reportErrors;

        private DateTimeFunctionImpl(Locale locale, DateFormat df, boolean reportErrors) {
            this.locale = locale;
            this.icuFormatter = df;
            this.reportErrors = reportErrors;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
            // TODO: use a special type to indicate function without input argument.
            if (toFormat == null) {
                return null;
            }
            if (toFormat instanceof CharSequence) {
                toFormat = parseIso8601(toFormat.toString());
                // We were unable to parse the input as iso date
                if (toFormat instanceof CharSequence) {
                    if (reportErrors) {
                        throw new IllegalArgumentException("bad-operand: argument must be ISO 8601");
                    }
                    return new FormattedPlaceholder(
                            toFormat, new PlainStringFormattedValue("{|" + toFormat + "|}"));
                }
            } else if (toFormat instanceof Temporal) {
                toFormat = JavaTimeConverters.temporalToCalendar((Temporal) toFormat);
            } else if (toFormat instanceof DayOfWeek) {
                toFormat = JavaTimeConverters.dayOfWeekToCalendar((DayOfWeek) toFormat);
            } else if (toFormat instanceof Month) {
                toFormat = JavaTimeConverters.monthToCalendar((Month) toFormat);
            }
            // Not an else-if here, because the `Temporal` conditions before make `toFormat` a `Calendar`
            if (toFormat instanceof java.util.Calendar) {
                toFormat = JavaTimeConverters.convertCalendar((java.util.Calendar) toFormat);
            }
            String result = icuFormatter.format(toFormat);
            return new FormattedPlaceholder(toFormat, new PlainStringFormattedValue(result));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
            FormattedPlaceholder result = format(toFormat, variableOptions);
            return result != null ? result.toString() : null;
        }
    }

    private final static Pattern ISO_PATTERN = Pattern.compile(
            "^(([0-9]{4})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])){1}(T([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]{1,3})?(Z|[+-]((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?)?$");

    private static Integer safeParse(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        return Integer.parseInt(str);
    }

    private static Object parseIso8601(String text) {
        Matcher m = ISO_PATTERN.matcher(text);
        if (m.find() && m.groupCount() == 12 && !m.group().isEmpty()) {
            Integer year = safeParse(m.group(2));
            Integer month = safeParse(m.group(3));
            Integer day = safeParse(m.group(4));
            Integer hour = safeParse(m.group(6));
            Integer minute = safeParse(m.group(7));
            Integer second = safeParse(m.group(8));
            Integer millisecond = 0;
            if (m.group(9) != null) {
                String z = (m.group(9) + "000").substring(1, 4);
                millisecond = safeParse(z);
            } else {
                millisecond = 0;
            }
            String tzPart = m.group(10);

            if (hour == null) {
                hour = 0;
                minute = 0;
                second = 0;
            }

            com.ibm.icu.util.GregorianCalendar gc = new com.ibm.icu.util.GregorianCalendar(
                    year, month - 1, day, hour, minute, second);
            gc.set(com.ibm.icu.util.Calendar.MILLISECOND, millisecond);

            if (tzPart != null) {
                if (tzPart.equals("Z")) {
                    gc.setTimeZone(com.ibm.icu.util.TimeZone.GMT_ZONE);
                } else {
                    int sign = tzPart.startsWith("-") ? -1 : 1;
                    String[] tzParts = tzPart.substring(1).split(":");
                    if (tzParts.length == 2) {
                        Integer tzHour = safeParse(tzParts[0]);
                        Integer tzMin = safeParse(tzParts[1]);
                        if (tzHour != null && tzMin != null) {
                            int offset = sign * (tzHour * 60 + tzMin) * 60 * 1000;
                            gc.setTimeZone(new com.ibm.icu.util.SimpleTimeZone(offset, "offset"));
                        }
                    }
                }
            }

            return gc;
        }
        return text;
    }

}
