// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

/**
 * This is a small interface I made to assist with converting from a formatter pipeline object to a
 * pattern string. It allows classes to "export" themselves to a property bag, which in turn can be
 * passed to {@link PatternString#propertiesToString(Properties)} to generate the pattern string.
 *
 * <p>Depending on the new API we expose, this process might not be necessary if we persist the
 * property bag in the current DecimalFormat shim.
 */
public interface Exportable {
  public void export(Properties properties);
}
