/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.dev.test.TestFmwk;

import java.util.Locale;

public class RBNFParseTest extends TestFmwk {
    public static void main(String[] args) {
    new RBNFParseTest().run(args);
    }

  public void TestParse() {

    // these rules make no sense but behave rationally
    String[] okrules = {
      "random text",
      "%foo:bar",
      "%foo: bar",
      "0:",
      "0::",
      "%%foo:;",
      "-",
      "-1",
      "-:",
      ".",
      ".1",
      "[",
      "]",
      "[]",
      "[foo]",
      "[[]",
      "[]]",
      "[[]]",
      "[][]",
      "<",
      ">",
      "=",
      "==",
      "===",
      "=foo=",
    };

    String[] exceptrules = {
      "",
      ";",
      ";;",
      ":",
      "::",
      ":1",
      ":;",
      ":;:;",
      "<<",
      "<<<",
      "10:;9:;",
      ">>",
      ">>>",
      "10:", // formatting any value with a one's digit will fail
      "11: << x", // formating a multiple of 10 causes rollback rule to fail
      "%%foo: 0 foo; 10: =%%bar=; %%bar: 0: bar; 10: =%%foo=;",
    };

    String[][] allrules = {
      okrules,
      exceptrules,
    };

    for (int j = 0; j < allrules.length; ++j) {
      String[] tests = allrules[j];
      boolean except = tests == exceptrules;
      for (int i = 0; i < tests.length; ++i) {
    logln("----------");
    logln("rules: '" + tests[i] + "'");
    boolean caughtException = false;
    try {
      RuleBasedNumberFormat fmt = new RuleBasedNumberFormat(tests[i], Locale.US);
      logln("1.23: " + fmt.format(20));
      logln("-123: " + fmt.format(-123));
      logln(".123: " + fmt.format(.123));
      logln(" 123: " + fmt.format(123));
    }
    catch (Exception e) {
      if (!except) {
        errln("Unexpected exception: " + e.getMessage());
      } else {
        caughtException = true;
      }
    }
    if (except && !caughtException) {
      errln("expected exception but didn't get one!");
    }
      }
    }
  }
}
