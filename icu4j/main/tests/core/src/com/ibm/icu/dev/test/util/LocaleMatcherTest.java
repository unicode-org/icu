/*
 ******************************************************************************************
 * Copyright (C) 2009-2010, Google, Inc.; International Business Machines Corporation and *
 * others. All Rights Reserved.                                                           *
 ******************************************************************************************
 */

package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.ULocale;

/**
 * Test the LanguageMatcher.
 * 
 * @author markdavis
 */
public class LocaleMatcherTest extends TestFmwk {
    
    public static void main(String[] args) throws Exception {
        new LocaleMatcherTest().run(args);
      }

  public void testBasics() {
    final LocaleMatcher matcher = new LocaleMatcher(LocalePriorityList
        .add(ULocale.FRENCH).add(ULocale.UK)
        .add(ULocale.ENGLISH).build());
    logln(matcher.toString());

    assertEquals(ULocale.UK, matcher.getBestMatch(ULocale.UK));
    assertEquals(ULocale.ENGLISH, matcher.getBestMatch(ULocale.US));
    assertEquals(ULocale.FRENCH, matcher.getBestMatch(ULocale.FRANCE));
    assertEquals(ULocale.FRENCH, matcher.getBestMatch(ULocale.JAPAN));
  }

  public void testFallback() {
    // check that script fallbacks are handled right
    final LocaleMatcher matcher = new LocaleMatcher("zh_CN, zh_TW, iw");
    assertEquals(new ULocale("zh_TW"), matcher.getBestMatch("zh_Hant"));
    assertEquals(new ULocale("zh_CN"), matcher.getBestMatch("zh"));
    assertEquals(new ULocale("zh_CN"), matcher.getBestMatch("zh_Hans_CN"));
    assertEquals(new ULocale("zh_TW"), matcher.getBestMatch("zh_Hant_HK"));
    assertEquals(new ULocale("he"), matcher.getBestMatch("iw_IT"));
  }

  public void testSpecials() {
    // check that nearby languages are handled
    final LocaleMatcher matcher = new LocaleMatcher("en, fil, ro, nn");
    assertEquals(new ULocale("fil"), matcher.getBestMatch("tl"));
    assertEquals(new ULocale("ro"), matcher.getBestMatch("mo"));
    assertEquals(new ULocale("nn"), matcher.getBestMatch("nb"));
    // make sure default works
    assertEquals(new ULocale("en"), matcher.getBestMatch("ja"));
  }

  public void testRegionalSpecials() {
    // verify that en_AU is closer to en_GB than to en (which is en_US)
    final LocaleMatcher matcher = new LocaleMatcher("en, en_GB, es, es_419");
    assertEquals("en_AU in {en, en_GB, es, es_419}", new ULocale("en_GB"), matcher.getBestMatch("en_AU"));
    assertEquals("es_MX in {en, en_GB, es, es_419}", new ULocale("es_419"), matcher.getBestMatch("es_MX"));
    assertEquals("es_ES in {en, en_GB, es, es_419}", new ULocale("es"), matcher.getBestMatch("es_ES"));
  }
  
  private void assertEquals(Object expected, Object string) {
      assertEquals("", expected, string);
  }

}
