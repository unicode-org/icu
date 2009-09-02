/*
 ****************************************************************************************
 * Copyright (C) 2009, Google, Inc.; International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                         *
 ****************************************************************************************
 */

package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.LocalePriorityList;

/**
 * Test the LanguagePriorityList
 * @author markdavis@google.com
 */
public class LocalePriorityListTest extends TestFmwk {
    
    public static void main(String[] args) throws Exception {
        new LocalePriorityListTest().run(args);
      }

  public void testLanguagePriorityList() {
    final String expected = "af, en, fr";

    LocalePriorityList list = LocalePriorityList.add("af, en, fr;q=0.9").build();
    assertEquals(expected, list.toString());

    // check looseness, and that later values win
    LocalePriorityList list2 = LocalePriorityList.add(
        ", fr ; q = 0.9 ,   en;q=0.1 , af, en, de;q=0, ").build();
    assertEquals(expected, list2.toString());
    assertEquals(list, list2);

    LocalePriorityList list3 = LocalePriorityList
        .add(new ULocale("af"))
        .add(ULocale.FRENCH, 0.9d)
        .add(ULocale.ENGLISH)
        .build();
    assertEquals(expected, list3.toString());
    assertEquals(list, list3);
    
    LocalePriorityList list4 = LocalePriorityList
    .add(list).build();
    assertEquals(expected, list4.toString());
    assertEquals(list, list4);
    
    LocalePriorityList list5 = LocalePriorityList.add("af, fr;q=0.9, en").build(true);
    assertEquals("af, en, fr;q=0.9", list5.toString());
  }

private void assertEquals(Object expected, Object string) {
    assertEquals("", expected, string);
}
}
