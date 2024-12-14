// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
******************************************************************************
* Copyright (C) 2007-2010, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

// Copyright 2006 Google Inc.  All Rights Reserved.

package com.ibm.icu.dev.test.duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.duration.impl.PeriodFormatterData;
import com.ibm.icu.impl.duration.impl.ResourceBasedPeriodFormatterDataService;

@RunWith(JUnit4.class)
public class ResourceBasedPeriodFormatterDataServiceTest extends CoreTestFmwk {
  @Test
  public void testAvailable() {
    ResourceBasedPeriodFormatterDataService service =
        ResourceBasedPeriodFormatterDataService.getInstance();
    for (String locale : service.getAvailableLocales()) {
      PeriodFormatterData pfd = service.get(locale);
      assertFalse(locale + ": " + pfd.pluralization(), -1 == pfd.pluralization());
    }
  }
}
