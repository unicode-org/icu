/*
******************************************************************************
* Copyright (C) 2007-2008, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.impl.duration.impl;

import  com.ibm.icu.impl.duration.DateFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A DateFormatter that formats the requested date fields.
 */
public class YMDDateFormatter implements DateFormatter {
  private String requestedFields;
  private String localeName;
  private TimeZone timeZone;
  private SimpleDateFormat df; // cache

  /**
   * Creates a new formatter that formats the requested 
   * fields.  The formatter defaults to the current locale
   * and time zone.
   *
   * @param requestedFields the requested fields
   */
  public YMDDateFormatter(String requestedFields) {
    this(requestedFields, Locale.getDefault().toString(),
         TimeZone.getDefault());
  }

  /**
   * Creates a new formatter that formats the requested 
   * fields using the provided locale and time zone.
   *
   * @param requestedFields the requested fields
   * @param localeName the locale to use
   * @param timeZone the time zone to use
   */
  public YMDDateFormatter(String requestedFields, String localeName, 
                             TimeZone timeZone) {
    this.requestedFields = requestedFields;
    this.localeName = localeName;
    this.timeZone = timeZone;

    Locale locale = Utils.localeFromString(localeName);
    this.df = new SimpleDateFormat("yyyy/mm/dd", locale);
    this.df.setTimeZone(timeZone);
  }

  /**
   * Returns a string representing the formatted date.
   * @param date the date in milliseconds
   */
  public String format(long date) {
    return format(new Date(date));
  }

  /**
   * Returns a string representing the formatted date.
   * @param date the date
   */
  public String format(Date date) {
    synchronized (this) {
      if (df == null) {
        // ignores requested fields
        // todo: make this really work
      }
    }
    return df.format(date);
  }

  /**
   * Returns a version of this formatter customized to the provided locale.
   */
  public DateFormatter withLocale(String locName) {
    if (!locName.equals(localeName)) {
      return new YMDDateFormatter(requestedFields, locName, timeZone);
    }
    return this;
  }

  /**
   * Returns a version of this formatter customized to the provided time zone.
   */
  public DateFormatter withTimeZone(TimeZone tz) {
    if (!tz.equals(timeZone)) {
      return new YMDDateFormatter(requestedFields, localeName, tz);
    }
    return this;
  }
}
