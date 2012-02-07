/*
******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

// Copyright 2006 Google Inc.  All Rights Reserved.

package com.ibm.icu.dev.test.duration.languages;

import com.ibm.icu.dev.test.duration.LanguageTestRoot;


/**
 * Test cases for zh_Hans
 */
public class Test_zh_Hans extends LanguageTestRoot {

  /**
   * Invoke the tests.
   */
  public static void main(String[] args) {
      new Test_zh_Hans().run(args);
  }

  /**
   * Constructor.
   */
  public Test_zh_Hans() {
    super("zh_Hans", false);
  }
}
