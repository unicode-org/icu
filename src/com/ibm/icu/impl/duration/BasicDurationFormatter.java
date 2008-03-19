/*
******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.impl.duration;

import java.util.Date;
import java.util.TimeZone;

/**
 * Core implementation class for DurationFormatter.
 */
class BasicDurationFormatter implements DurationFormatter {
  private PeriodFormatter formatter;
  private PeriodBuilder builder;
  private DateFormatter fallback;
  private long fallbackLimit;
  private String localeName;
  private TimeZone timeZone;

  /**
   * Creates a basic duration formatter with the given formatter,
   * builder, and fallback.  It's up to the caller to ensure that
   * the locales and timezones of these are in sync.
   */
  public BasicDurationFormatter(PeriodFormatter formatter,
                                PeriodBuilder builder, 
                                DateFormatter fallback,
                                long fallbackLimit) {
    this.formatter = formatter;
    this.builder = builder;
    this.fallback = fallback;
    this.fallbackLimit = fallbackLimit < 0 ? 0 : fallbackLimit;
  }

  protected BasicDurationFormatter(PeriodFormatter formatter,
                                   PeriodBuilder builder, 
                                   DateFormatter fallback,
                                   long fallbackLimit,
                                   String localeName,
                                   TimeZone timeZone) {
    this.formatter = formatter;
    this.builder = builder;
    this.fallback = fallback;
    this.fallbackLimit = fallbackLimit;
    this.localeName = localeName;
    this.timeZone = timeZone;
  }

  public String formatDurationFromNowTo(Date targetDate) {
    long now = System.currentTimeMillis();
    long duration = now - targetDate.getTime();
    return formatDurationFrom(duration, now);
  }

  public String formatDurationFromNow(long duration) {
    return formatDurationFrom(duration, System.currentTimeMillis());
  }

  public String formatDurationFrom(long duration, long referenceDate) {
    String s = doFallback(duration, referenceDate);
    if (s == null) {
      Period p = doBuild(duration, referenceDate);
      s = doFormat(p);
    }
    return s;
  }

  public DurationFormatter withLocale(String localeName) {
    if (!localeName.equals(this.localeName)) {
      PeriodFormatter newFormatter = formatter.withLocale(localeName);
      PeriodBuilder newBuilder = builder.withLocale(localeName);
      DateFormatter newFallback = fallback == null 
          ? null 
          : fallback.withLocale(localeName);
      return new BasicDurationFormatter(newFormatter, newBuilder,
                                        newFallback, fallbackLimit,
                                        localeName, timeZone);
    }
    return this;
  }

  public DurationFormatter withTimeZone(TimeZone timeZone) {
    if (!timeZone.equals(this.timeZone)) {
      PeriodBuilder newBuilder = builder.withTimeZone(timeZone);
      DateFormatter newFallback = fallback == null 
          ? null 
          : fallback.withTimeZone(timeZone);
      return new BasicDurationFormatter(formatter, newBuilder,
                                        newFallback, fallbackLimit,
                                        localeName, timeZone);
    }
    return this;
  }

  protected String doFallback(long duration, long referenceDate) {
    if (fallback != null 
        && fallbackLimit > 0
        && Math.abs(duration) >= fallbackLimit) {
      return fallback.format(referenceDate + duration);
    }
    return null;
  }

  protected Period doBuild(long duration, long referenceDate) {
    return builder.createWithReferenceDate(duration, referenceDate);
  }

  protected String doFormat(Period period) {
    if (!period.isSet()) {
      throw new IllegalArgumentException("period is not set");
    }
    return formatter.format(period);
  }
}
