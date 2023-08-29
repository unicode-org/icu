// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.lang;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;

/**
 * @author aheninger
 *
 */
@RunWith(JUnit4.class)
public class UCharacterThreadTest extends TestFmwk {
  // constructor -----------------------------------------------------------

    /**
    * Private constructor to prevent initialization
    */
    public UCharacterThreadTest()
    {
    }

      // public methods --------------------------------------------------------

    //
    //  Test multi-threaded parallel calls to UCharacter.getName(codePoint)
    //  Regression test for ticket 6264.
    //
    @Test
    public void TestUCharactersGetName() throws InterruptedException {
        List threads = new LinkedList();
        for(int t=0; t<20; t++) {
          int codePoint = 47 + t;
          String correctName = UCharacter.getName(codePoint);
          GetNameThread thread = new GetNameThread(codePoint, correctName);
          thread.start();
          threads.add(thread);
        }
        ListIterator i = threads.listIterator();
        while (i.hasNext()) {
            GetNameThread thread = (GetNameThread)i.next();
            thread.join();
            if (!thread.correctName.equals(thread.actualName)) {
                errln("FAIL, expected \"" + thread.correctName + "\", got \"" + thread.actualName + "\"");
            }
        }
      }

      private static class GetNameThread extends Thread {
        private final int codePoint;
        private final String correctName;
        private String actualName;

        GetNameThread(int codePoint, String correctName) {
           this.codePoint = codePoint;
           this.correctName = correctName;
        }

        @Override
        public void run() {
          for(int i=0; i<10000; i++) {
            actualName = UCharacter.getName(codePoint);
            if (!correctName.equals(actualName)) {
              break;
            }
          }
        }
      }
}
