//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import java.util.*;

/**
 * @test
 * @summary General test of UnicodeSet
 */
public class UnicodeMapTest extends TestFmwk {
  
  static final int MODIFY_TEST_LIMIT = 32;
  static final int MODIFY_TEST_ITERATIONS = 100000;
  
  public static void main(String[] args) throws Exception {
    new UnicodeMapTest().run(args);
  }
  
  public void TestModify() {
    Random random = new Random(0);
    UnicodeMap unicodeMap = new UnicodeMap();
    HashMap hashMap = new HashMap();
    String[] values = {null, "the", "quick", "brown", "fox"};
    for (int count = 1; count <= MODIFY_TEST_ITERATIONS; ++count) {
      String value = values[random.nextInt(values.length)];
      int start = random.nextInt(MODIFY_TEST_LIMIT); // test limited range
      int end = random.nextInt(MODIFY_TEST_LIMIT);
      if (start > end) {
        int temp = start;
        start = end;
        end = temp;
      }
      int modCount = count & 0xFF;
      if (modCount == 0 && isVerbose()) {
        logln("***"+count);
        logln(unicodeMap.toString());
      }
      unicodeMap.putAll(start, end, value);
      if (modCount == 1 && isVerbose()) {
        logln(">>>\t" + Utility.hex(start) + ".." + Utility.hex(end) + "\t" + value);
        logln(unicodeMap.toString());
      }
      for (int i = start; i <= end; ++i) {
        hashMap.put(new Integer(i), value);
      }
      if (!hasSameValues(unicodeMap, hashMap)) {
        errln("Failed at " + count);
      }
    }
  }

  private boolean hasSameValues(UnicodeMap unicodeMap, HashMap hashMap) {
    for (int i = 0; i < MODIFY_TEST_LIMIT; ++i) {
      Object unicodeMapValue = unicodeMap.getValue(i);
      Object hashMapValue = hashMap.get(new Integer(i));
      if (unicodeMapValue != hashMapValue) {
        return false;
      }
    }
    return true;
  }
}
//#endif
