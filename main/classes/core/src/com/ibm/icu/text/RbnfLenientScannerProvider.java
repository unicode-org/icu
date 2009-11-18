/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import com.ibm.icu.util.ULocale;

/**
 * A provider for an RbnfLenientScanner.
 *
 * @draft ICU 4.4
 */
public interface RbnfLenientScannerProvider {
  /**
   * Returns a scanner appropriate for the given locale, with optional extra data.
   * in the form of collation rules.
   *
   * @param locale the locale to provide the default lenient rules.
   * @param extras extra collation rules
   * @return the lenient scanner, or null
   * @draft ICU 4.4
   */
  RbnfLenientScanner get(ULocale locale, String extras);
}