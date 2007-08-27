/*
******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

package com.ibm.icu.impl.duration;

import java.util.Collection;

/**
 * Provider of Factory instances for building PeriodBuilders, PeriodFormatters,
 * and DurationFormatters.
 */
public interface PeriodFormatterService {

  /**
   * Creates a new factory for creating DurationFormatters.
   *
   * @return a new DurationFormatterFactory.
   */
  DurationFormatterFactory newDurationFormatterFactory();

  /**
   * Creates a new factory for creating PeriodFormatters.
   *
   * @return a new PeriodFormatterFactory
   */
  PeriodFormatterFactory newPeriodFormatterFactory();

  /**
   * Creates a new factory for creating PeriodBuilders.
   *
   * @return a new PeriodBuilderFactory
   */
  PeriodBuilderFactory newPeriodBuilderFactory();

  /**
   * Return the names of locales supported by factories produced by
   * this service.
   * @return a collection of String (locale names)
   */
  Collection getAvailableLocaleNames();
}
