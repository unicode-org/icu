// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

/**
 * The main entrypoint to the formatting of ranges of numbers, including currencies and other units of measurement.
 *
 * @author sffc
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 * @see NumberFormatter
 */
public abstract class NumberRangeFormatter {

    public static enum RangeCollapse {}

    public static enum RangeIdentityFallback {}

}
