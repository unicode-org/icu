// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

/**
 * @deprecated ICU 62 Use {@link CurrencyPrecision} instead. This class is for backwards compatibility
 *             and will be removed in ICU 64. See http://bugs.icu-project.org/trac/ticket/13746
 */
@Deprecated
public abstract class CurrencyRounder extends CurrencyPrecision {
    // package private constructor just for blocking
    // java compiler to generate public no-arg constructor.
    CurrencyRounder() {
    }
}