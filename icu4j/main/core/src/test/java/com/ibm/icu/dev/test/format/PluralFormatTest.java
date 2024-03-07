// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2007-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.PluralFormat;
import com.ibm.icu.util.ULocale;

/**
 * @author tschumann (Tim Schumann)
 *
 */
@RunWith(JUnit4.class)
public class PluralFormatTest extends CoreTestFmwk {
  private void helperTestRules(String localeIDs, String testPattern, Map<Integer,String> changes) {
    String[] locales = Utility.split(localeIDs, ',');

    // Create example outputs for all supported locales.
    /*
    System.out.println("\n" + localeIDs);
    String lastValue = (String) changes.get(Integer.valueOf(0));
    int  lastNumber = 0;

    for (int i = 1; i < 199; ++i) {
        if (changes.get(Integer.valueOf(i)) != null) {
            if (lastNumber == i-1) {
                System.out.println(lastNumber + ": " + lastValue);
            } else {
                System.out.println(lastNumber + "... " + (i-1) + ": " + lastValue);
            }
            lastNumber = i;
            lastValue = (String) changes.get(Integer.valueOf(i));
        }
    }
    System.out.println(lastNumber + "..." + 199 + ": " + lastValue);
    */
    log("test pattern: '" + testPattern + "'");
    for (int i = 0; i < locales.length; ++i) {
      try {
        PluralFormat plf = new PluralFormat(new ULocale(locales[i]), testPattern);
        log("plf: " + plf);
        String expected = changes.get(0);
        for (int n = 0; n < 200; ++n) {
          String value = changes.get(n);
          if (value != null) {
            expected = value;
          }
          assertEquals("Locale: " + locales[i] + ", number: " + n,
                       expected, plf.format(n));
        }
      } catch (IllegalArgumentException e) {
        errln(e.getMessage() + " locale: " + locales[i] + " pattern: '" + testPattern + "' " + System.currentTimeMillis());
      }
    }
  }

  @Test
  public void TestOneFormLocales() {
    String localeIDs = "ja,ko,tr,vi";
    String testPattern = "other{other}";
    Map<Integer, String> changes = new HashMap<>();
    changes.put(0, "other");
    helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestSingular1Locales() {
    String localeIDs = "bem,da,de,el,en,eo,es,et,fi,fo,he,it,mr,nb,nl,nn,no,pt_PT,sv,af,bg,ca,eu,fur,fy,ha,ku,lb,ml," +
        "nah,ne,om,or,pap,ps,so,sq,sw,ta,te,tk,ur,mn,gsw,rm";
    String testPattern = "one{one} other{other}";
    Map<Integer, String> changes = new HashMap<>();
    changes.put(0, "other");
    changes.put(1, "one");
    changes.put(2, "other");
    helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestSingular01Locales() {
    String localeIDs = "ff,fr,kab,gu,pa,pt,zu,bn";
    String testPattern = "one{one} other{other}";
    Map<Integer, String> changes = new HashMap<>();
    changes.put(0, "one");
    changes.put(2, "other");
    helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestZeroSingularLocales() {
    String localeIDs = "lv";
    String testPattern = "zero{zero} one{one} other{other}";
    Map<Integer, String> changes = new HashMap<>();
    changes.put(0, "zero");
    changes.put(1, "one");
    for (int i = 2; i < 20; ++i) {
      if (i < 10) {
        changes.put(i, "other");
      } else {
        changes.put(i, "zero");
      }
      changes.put(i*10, "zero");
      if (i == 11) {
        changes.put(i * 10 + 1, "zero");
        changes.put(i * 10 + 2, "zero");
      } else {
        changes.put(i * 10 + 1, "one");
        changes.put(i * 10 + 2, "other");
      }
    }
    helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestSingularDual() {
      String localeIDs = "ga";
      String testPattern = "one{one} two{two} other{other}";
      Map<Integer, String> changes = new HashMap<>();
      changes.put(0, "other");
      changes.put(1, "one");
      changes.put(2, "two");
      changes.put(3, "other");
      helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestSingularZeroSome() {
      String localeIDs = "ro";
      String testPattern = "few{few} one{one} other{other}";
      Map<Integer, String> changes = new HashMap<>();
      changes.put(0, "few");
      changes.put(1, "one");
      changes.put(2, "few");
      changes.put(20, "other");
      changes.put(101, "few");
      changes.put(102, "few");
      changes.put(120, "other");
      helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestSpecial12_19() {
      String localeIDs = "lt";
      String testPattern = "one{one} few{few} other{other}";
      Map<Integer, String> changes = new HashMap<>();
      changes.put(0, "other");
      changes.put(1, "one");
      changes.put(2, "few");
      changes.put(10, "other");
      for (int i = 2; i < 20; ++i) {
        if (i == 11) {
          continue;
        }
        changes.put(i * 10 + 1, "one");
        changes.put(i * 10 + 2, "few");
        changes.put((i + 1) * 10, "other");
      }
      helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestPaucalExcept11_14() {
      String localeIDs = "hr,sr,uk";
      String testPattern = "one{one} few{few} other{other}";
      Map<Integer, String> changes = new HashMap<>();
      changes.put(0, "other");
      changes.put(1, "one");
      changes.put(2, "few");
      changes.put(5, "other");
      for (int i = 2; i < 20; ++i) {
        if (i == 11) {
          continue;
        }
        changes.put(i * 10 + 1, "one");
        changes.put(i * 10 + 2, "few");
        changes.put(i * 10 + 5, "other");
      }
      helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestPaucalRu() {
      String localeIDs = "ru";
      String testPattern = "one{one} many{many} other{other}";
      Map<Integer, String> changes = new HashMap<>();
      for (int i = 0; i < 200; i+=10) {
          if (i == 10 || i == 110) {
              put(i, 0, 9, "many", changes);
              continue;
          }
          put(i, 0, "many", changes);
          put(i, 1, "one", changes);
          put(i, 2, 4, "other", changes);
          put(i, 5, 9, "many", changes);
      }
      helperTestRules(localeIDs, testPattern, changes);
  }

  public <T> void put(int base, int start, int end, T value, Map<Integer, T> m) {
      for (int i = start; i <= end; ++i) {
          if (m.containsKey(base + i)) {
              throw new IllegalArgumentException();
          }
          m.put(base + i, value);
      }
  }

  public <T> void put(int base, int start, T value, Map<Integer, T> m) {
      put(base, start, start, value, m);
  }

  @Test
  public void TestSingularPaucal() {
      String localeIDs = "cs,sk";
      String testPattern = "one{one} few{few} other{other}";
      Map<Integer, String> changes = new HashMap<>();
      changes.put(0, "other");
      changes.put(1, "one");
      changes.put(2, "few");
      changes.put(5, "other");
      helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestPaucal1_234() {
      String localeIDs = "pl";
      String testPattern = "one{one} few{few} other{other}";
      Map<Integer, String> changes = new HashMap<>();
      changes.put(0, "other");
      changes.put(1, "one");
      changes.put(2, "few");
      changes.put(5, "other");
      for (int i = 2; i < 20; ++i) {
        if (i == 11) {
          continue;
        }
        changes.put(i * 10 + 2, "few");
        changes.put(i * 10 + 5, "other");
      }
      helperTestRules(localeIDs, testPattern, changes);
  }

  @Test
  public void TestPaucal1_2_34() {
      String localeIDs = "sl";
      String testPattern = "one{one} two{two} few{few} other{other}";
      Map<Integer, String> changes = new HashMap<>();
      changes.put(0, "other");
      changes.put(1, "one");
      changes.put(2, "two");
      changes.put(3, "few");
      changes.put(5, "other");
      changes.put(101, "one");
      changes.put(102, "two");
      changes.put(103, "few");
      changes.put(105, "other");
      helperTestRules(localeIDs, testPattern, changes);
  }

    /* Tests the method public PluralRules getPluralRules() */
    @Test
    public void TestGetPluralRules() {
        CurrencyPluralInfo cpi = new CurrencyPluralInfo();
        try {
            cpi.getPluralRules();
        } catch (Exception e) {
            errln("CurrencyPluralInfo.getPluralRules() was not suppose to " + "return an exception.");
        }
    }

    /* Tests the method public ULocale getLocale() */
    @Test
    public void TestGetLocale() {
        CurrencyPluralInfo cpi = new CurrencyPluralInfo(new ULocale("en_US"));
        if (!cpi.getLocale().equals(new ULocale("en_US"))) {
            errln("CurrencyPluralInfo.getLocale() was suppose to return true " + "when passing the same ULocale");
        }
        if (cpi.getLocale().equals(new ULocale("jp_JP"))) {
            errln("CurrencyPluralInfo.getLocale() was not suppose to return true " + "when passing a different ULocale");
        }
    }

    /* Tests the method public void setLocale(ULocale loc) */
    @Test
    public void TestSetLocale() {
        CurrencyPluralInfo cpi = new CurrencyPluralInfo();
        cpi.setLocale(new ULocale("en_US"));
        if (!cpi.getLocale().equals(new ULocale("en_US"))) {
            errln("CurrencyPluralInfo.setLocale() was suppose to return true when passing the same ULocale");
        }
        if (cpi.getLocale().equals(new ULocale("jp_JP"))) {
            errln("CurrencyPluralInfo.setLocale() was not suppose to return true when passing a different ULocale");
        }
    }

    /* Tests the method public boolean equals(Object a) */
    @Test
    public void TestEquals(){
        CurrencyPluralInfo cpi = new CurrencyPluralInfo();
        if(cpi.equals(0)){
            errln("CurrencyPluralInfo.equals(Object) was not suppose to return true when comparing to an invalid object for integer 0.");
        }
        if(cpi.equals(0.0)){
            errln("CurrencyPluralInfo.equals(Object) was not suppose to return true when comparing to an invalid object for float 0.");
        }
        if(cpi.equals("0")){
            errln("CurrencyPluralInfo.equals(Object) was not suppose to return true when comparing to an invalid object for string 0.");
        }
    }

    /* Test for https://unicode-org.atlassian.net/browse/ICU-13151 */
    @Test
    public void TestFractionRounding() {
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(0);
        PluralFormat pf = new PluralFormat(ULocale.ENGLISH, "one{#kg}other{#kgs}");
        pf.setNumberFormat(nf);
        assertEquals("1.2kg", "1kg", pf.format(1.2));
    }
}
