/*
******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

// Copyright 2006 Google Inc.  All Rights Reserved.

package com.ibm.icu.dev.test.duration;

import com.ibm.icu.dev.test.TestFmwk;

import com.ibm.icu.impl.duration.impl.ResourceBasedPeriodFormatterDataService;
import com.ibm.icu.impl.duration.impl.PeriodFormatterData;

import java.util.Iterator;
import java.util.Collection;

public class ResourceBasedPeriodFormatterDataServiceTest extends TestFmwk {

  /**
   * Invoke the tests.
   */
  public static void main(String[] args) {
      new ResourceBasedPeriodFormatterDataServiceTest().run(args);
  }

  public void testAvailable() {
    ResourceBasedPeriodFormatterDataService service =
        ResourceBasedPeriodFormatterDataService.getInstance();
    Collection locales = service.getAvailableLocales();
    for (Iterator i = locales.iterator(); i.hasNext();) {
      String locale = (String)i.next();
      PeriodFormatterData pfd = service.get(locale);
      assertFalse(locale + ": " + pfd.pluralization(), -1 == pfd.pluralization());
    }
  }
}
