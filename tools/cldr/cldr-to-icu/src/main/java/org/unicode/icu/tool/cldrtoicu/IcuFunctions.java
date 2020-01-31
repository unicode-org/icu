// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseInt;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.icu.tool.cldrtoicu.regex.NamedFunction;

import com.google.common.base.Ascii;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableMap;

/**
 * The named functions used by the {@code RegexTransformer} for {@code ldml2icu_supplemental.txt}.
 */
final class IcuFunctions {
    /**
     * Converts an ISO date string to a space-separated pair of integer values representing the top
     * and bottom parts of a deconstructed millisecond epoch value (i.e. {@code
     * "<hi32bits> <low32bits>"}).
     *
     * <p>Note that the values are formatted as <em>signed</em> decimal values, so it's entirely
     * possible that the low bits value will be appear as a negative number (the high bits won't
     * appear negative for many thousands of years).
     *
     * <ul>
     *   <li>args[0] = ISO date string (e.g. "2019-05-23")
     *   <li>args[1] = Date field type name (e.g. "from")
     * </ul>
     */
    static final NamedFunction DATE_FN =
        NamedFunction.create("date", 2, args -> {
            long millis =
                DateFieldType.toEnum(args.get(1)).toEpochMillis(LocalDate.parse(args.get(0)));
            // Strictly speaking the masking is redundant and could be removed.
            int hiBits = (int) ((millis >>> 32) & 0xFFFFFFFFL);
            int loBits = (int) (millis & 0xFFFFFFFFL);
            return hiBits + " " + loBits;
        });

    // TODO: Improve this documentation (e.g. why is this being done, give examples?).
    /**
     * Inserts '%' into numberingSystems descriptions.
     *
     * <ul>
     *   <li>args[0] = numbering system description (string)
     * </ul>
     */
    static final NamedFunction ALGORITHM_FN =
        NamedFunction.create("algorithm", 1, args -> {
            String value = args.get(0);
            int percentPos = value.lastIndexOf('/') + 1;
            return value.substring(0, percentPos) + '%' + value.substring(percentPos);
        });

    /**
     * Converts a number into a special integer that represents the number in normalized scientific
     * notation for ICU's RB parser.
     *
     * <p>Resultant integers are in the form "xxyyyyyy", where "xx" is the exponent offset by 50
     * and "yyyyyy" is the coefficient to 5 decimal places. Results may also have a leading '-' to
     * denote negative values.
     *
     * <p>For example:
     * <pre>{@code
     * 14660000000000 -> 1.466E13    -> 63146600
     * 0.0001         -> 1E-4        -> 46100000
     * -123.456       -> -1.23456E-2 -> -48123456
     * }</pre>
     *
     * <p>The additional exponent offset is applied directly to the calculated exponent and is used
     * to do things like converting percentages into their decimal representation (i.e. by passing
     * a value of "-2").
     *
     * <ul>
     *   <li>args[0] = number to be converted (double)
     *   <li>args[1] = additional exponent offset (integer)
     * </ul>
     */
    static final NamedFunction EXP_FN =
        NamedFunction.create("exp", 2, args -> {
            double value = Double.parseDouble(args.get(0));
            if (value == 0) {
                return "0";
            }
            int exponent = 50;
            if (args.size() == 2) {
                exponent += Integer.parseInt(args.get(1));
            }
            String sign = value >= 0 ? "" : "-";
            value = Math.abs(value);
            while (value >= 10) {
                value /= 10;
                exponent++;
            }
            while (value < 1) {
                value *= 10;
                exponent--;
            }
            if (exponent < 0 || exponent > 99) {
                throw new IllegalArgumentException("Exponent out of bounds: " + exponent);
            }
            return sign + exponent + Math.round(value * 100000);
        });

    // Allow for single digit values in any part and negative year values.
    private static final Pattern YMD = Pattern.compile("(-?[0-9]+)-([0-9]{1,2})-([0-9]{1,2})");

    /**
     * Converts an ISO date string (i.e. "YYYY-MM-DD") into an ICU date string, which is
     * the same but with spaces instead of hyphens. Since functions are expanded before the
     * resulting value is split, this function will result in 3 separate values being created,
     * unless the function call is enclosed in quotes.
     *
     * <p>Note that for some cases (e.g. "eras") the year part can be negative (e.g. "-2165-1-1")
     * so this is not as simple as "split by hyphen".
     *
     * <ul>
     *   <li>args[0] = ISO date string (e.g. "2019-05-23" or "-2165-1-1")
     * </ul>
     */
    static final NamedFunction YMD_FN =
        NamedFunction.create("ymd", 1, args -> {
            Matcher m = YMD.matcher(args.get(0));
            checkArgument(m.matches(), "invalid year-month-day string: %s", args.get(0));
            // NOTE: Re-parsing is not optional since it removes leading zeros (needed for ICU).
            return String.format("%s %s %s",
                parseInt(m.group(1)), parseInt(m.group(2)), parseInt(m.group(3)));
        });

    // For transforming day-of-week identifiers.
    private static final ImmutableMap<String, String> WEEKDAY_MAP_ID =
        ImmutableMap.<String, String>builder()
            .put("sun", "1")
            .put("mon", "2")
            .put("tues", "3")
            .put("wed", "4")
            .put("thu", "5")
            .put("fri", "6")
            .put("sat", "7")
            .build();

    /**
     * Converts a day-of-week identifier into its ordinal value (e.g. "sun" --> 1, "mon" --> 2 ...).
     */
    static final NamedFunction DAY_NUMBER_FN =
        NamedFunction.create("day_number", 1,
            args -> {
                String id = WEEKDAY_MAP_ID.get(args.get(0));
                checkArgument(id != null, "unknown weekday: %s", args.get(0));
                return id;
            });

    // For transform IDs in <contextTransform> elements.
    private static final ImmutableMap<String, String> TRANSFORM_ID_MAP =
        ImmutableMap.of("no-change", "0", "titlecase-firstword", "1");

    /**
     * Converts the transform type in the {@code <contextTransform>} element into its ICU index
     * (e.g. "titlecase-firstword" --> 1).
     */
    static final NamedFunction CONTEXT_TRANSFORM_INDEX_FN =
        NamedFunction.create("context_transform_index", 1,
            args -> {
                String id = TRANSFORM_ID_MAP.get(args.get(0));
                checkArgument(id != null, "unknown contextTransform: %s", args.get(0));
                return id;
            });

    // For DATE_FN only.
    private enum DateFieldType {
        from(LocalDate::atStartOfDay),
        // Remember that atTime() takes nanoseconds, not micro or milli.
        to(d -> d.atTime(23, 59, 59, 999_000_000));

        private final Function<LocalDate, LocalDateTime> adjustFn;

        DateFieldType(Function<LocalDate, LocalDateTime> adjustFn) {
            this.adjustFn = adjustFn;
        }

        long toEpochMillis(LocalDate date) {
            return adjustFn.apply(date).toInstant(ZoneOffset.UTC).toEpochMilli();
        }

        static DateFieldType toEnum(String value) {
            switch (Ascii.toLowerCase(CharMatcher.whitespace().trimFrom(value))) {
            case "from":
            case "start":
                return from;
            case "to":
            case "end":
                return to;
            default:
                throw new IllegalArgumentException(value + " is not a valid date field type");
            }
        }
    }

    private IcuFunctions() {}
}
