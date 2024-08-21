// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.time.Clock;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.impl.JavaTimeConverters;
import com.ibm.icu.text.DateFormat;

/**
 * Creates a {@link Formatter} doing formatting of date / time, similar to
 * <code>{exp, date}</code> and <code>{exp, time}</code> in {@link com.ibm.icu.text.MessageFormat}.
 */
class DateTimeFormatterFactory implements FormatterFactory {
    private final String kind;

    // "datetime", "date", "time"
    DateTimeFormatterFactory(String kind) {
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

    private static int stringToStyle(String option) {
        switch (option) {
            case "full":
                return DateFormat.FULL;
            case "long":
                return DateFormat.LONG;
            case "medium":
                return DateFormat.MEDIUM;
            case "short":
                return DateFormat.SHORT;
            default:
                throw new IllegalArgumentException("Invalid datetime style: " + option);
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
        int dateStyle = DateFormat.NONE;
        int timeStyle = DateFormat.NONE;
        switch (kind) {
            case "date":
                dateStyle = getDateTimeStyle(fixedOptions, "style");
                break;
            case "time":
                timeStyle = getDateTimeStyle(fixedOptions, "style");
                break;
            case "datetime": // $FALL-THROUGH$
            default:
                dateStyle = getDateTimeStyle(fixedOptions, "dateStyle");
                timeStyle = getDateTimeStyle(fixedOptions, "timeStyle");
                break;
        }

        // TODO: how to handle conflicts. What if we have both skeleton and style, or pattern?
        if (dateStyle == DateFormat.NONE && timeStyle == DateFormat.NONE) {
            String skeleton = "";
            switch (kind) {
                case "date":
                    skeleton = getDateFieldOptions(fixedOptions);
                    break;
                case "time":
                    skeleton = getTimeFieldOptions(fixedOptions);
                    break;
                case "datetime": // $FALL-THROUGH$
                default:
                    skeleton = getDateFieldOptions(fixedOptions);
                    skeleton += getTimeFieldOptions(fixedOptions);
                    break;
            }

            if (skeleton.isEmpty()) {
                // Custom option, icu namespace
                skeleton = OptUtils.getString(fixedOptions, "icu:skeleton", "");
            }
            if (!skeleton.isEmpty()) {
                DateFormat df = DateFormat.getInstanceForSkeleton(skeleton, locale);
                return new DateTimeFormatter(locale, df);
            }

            // No skeletons, custom or otherwise, match fallback to short / short as per spec.
            switch (kind) {
                case "date":
                    dateStyle = DateFormat.SHORT;
                    timeStyle = DateFormat.NONE;
                    break;
                case "time":
                    dateStyle = DateFormat.NONE;
                    timeStyle = DateFormat.SHORT;
                    break;
                case "datetime": // $FALL-THROUGH$
                default:
                    dateStyle = DateFormat.SHORT;
                    timeStyle = DateFormat.SHORT;
            }
        }

        DateFormat df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
        return new DateTimeFormatter(locale, df);
    }

    private static int getDateTimeStyle(Map<String, Object> options, String key) {
        String opt = OptUtils.getString(options, key);
        if (opt != null) {
            return stringToStyle(opt);
        }
        return DateFormat.NONE;
    }

    private static String getDateFieldOptions(Map<String, Object> options) {
        StringBuilder skeleton = new StringBuilder();
        String opt;

        // In all the switches below we just ignore invalid options.
        // Would be nice to report (log?), but ICU does not have a clear policy on how to do that.
        // But we don't want to throw, that is too drastic.

        opt = OptUtils.getString(options, "weekday", "");
        switch (opt) {
            case "long":
                skeleton.append("EEEE");
                break;
            case "short":
                skeleton.append("E");
                break;
            case "narrow":
                skeleton.append("EEEEEE");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "era", "");
        switch (opt) {
            case "long":
                skeleton.append("GGGG");
                break;
            case "short":
                skeleton.append("G");
                break;
            case "narrow":
                skeleton.append("GGGGG");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "year", "");
        switch (opt) {
            case "numeric":
                skeleton.append("y");
                break;
            case "2-digit":
                skeleton.append("yy");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "month", "");
        switch (opt) {
            case "numeric":
                skeleton.append("M");
                break;
            case "2-digit":
                skeleton.append("MM");
                break;
            case "long":
                skeleton.append("MMMM");
                break;
            case "short":
                skeleton.append("MMM");
                break;
            case "narrow":
                skeleton.append("MMMMM");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "day", "");
        switch (opt) {
            case "numeric":
                skeleton.append("d");
                break;
            case "2-digit":
                skeleton.append("dd");
                break;
            default:
                // invalid value, we just ignore it.
        }
        return skeleton.toString();
    }

    private static String getTimeFieldOptions(Map<String, Object> options) {
        StringBuilder skeleton = new StringBuilder();
        String opt;

        // In all the switches below we just ignore invalid options.
        // Would be nice to report (log?), but ICU does not have a clear policy on how to do that.
        // But we don't want to throw, that is too drastic.

        int showHour = 0;
        opt = OptUtils.getString(options, "hour", "");
        switch (opt) {
            case "numeric":
                showHour = 1;
                break;
            case "2-digit":
                showHour = 2;
                break;
            default:
                // invalid value, we just ignore it.
        }
        if (showHour > 0) {
            String hourCycle = "";
            opt = OptUtils.getString(options, "hourCycle", "");
            switch (opt) {
                case "h11":
                    hourCycle = "K";
                    break;
                case "h12":
                    hourCycle = "h";
                    break;
                case "h23":
                    hourCycle = "H";
                    break;
                case "h24":
                    hourCycle = "k";
                    break;
                default:
                    hourCycle = "j"; // default for the locale
            }
            skeleton.append(hourCycle);
            if (showHour == 2) {
                skeleton.append(hourCycle);
            }
        }

        opt = OptUtils.getString(options, "minute", "");
        switch (opt) {
            case "numeric":
                skeleton.append("m");
                break;
            case "2-digit":
                skeleton.append("mm");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "second", "");
        switch (opt) {
            case "numeric":
                skeleton.append("s");
                break;
            case "2-digit":
                skeleton.append("ss");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "fractionalSecondDigits", "");
        switch (opt) {
            case "1":
                skeleton.append("S");
                break;
            case "2":
                skeleton.append("SS");
                break;
            case "3":
                skeleton.append("SSS");
                break;
            default:
                // invalid value, we just ignore it.
        }

        opt = OptUtils.getString(options, "timeZoneName", "");
        switch (opt) {
            case "long":
                skeleton.append("z");
                break;
            case "short":
                skeleton.append("zzzz");
                break;
            case "shortOffset":
                skeleton.append("O");
                break;
            case "longOffset":
                skeleton.append("OOOO");
                break;
            case "shortGeneric":
                skeleton.append("v");
                break;
            case "longGeneric":
                skeleton.append("vvvv");
                break;
            default:
                // invalid value, we just ignore it.
        }

        return skeleton.toString();
    }

    private static class DateTimeFormatter implements Formatter {
        private final DateFormat icuFormatter;
        private final Locale locale;

        private DateTimeFormatter(Locale locale, DateFormat df) {
            this.locale = locale;
            this.icuFormatter = df;
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
                    return new FormattedPlaceholder(
                            toFormat, new PlainStringFormattedValue("{|" + toFormat + "|}"));
                }
            } else if (toFormat instanceof Clock) {
            	toFormat = JavaTimeConverters.temporalToCalendar((Clock) toFormat);
            } else if (toFormat instanceof Temporal) {
            	toFormat = JavaTimeConverters.temporalToCalendar((Temporal) toFormat);
            }
            // Not an else-if here, because the `Clock` & `Temporal` conditions before make `toFormat` a `Calendar`
            if (toFormat instanceof Calendar) {
                TimeZone tz = ((Calendar) toFormat).getTimeZone();
                long milis = ((Calendar) toFormat).getTimeInMillis();
                com.ibm.icu.util.TimeZone icuTz = com.ibm.icu.util.TimeZone.getTimeZone(tz.getID());
                com.ibm.icu.util.Calendar calendar =
                        com.ibm.icu.util.Calendar.getInstance(icuTz, locale);
                calendar.setTimeInMillis(milis);
                toFormat = calendar;
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
        if (str == null || str.isEmpty())
            return null;
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
