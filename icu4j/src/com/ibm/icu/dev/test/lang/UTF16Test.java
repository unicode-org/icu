/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/lang/UTF16Test.java,v $ 
* $Date: 2001/09/06 16:32:54 $ 
* $Revision: 1.8 $
*
*******************************************************************************
*/

package com.ibm.icu.test.text;

import com.ibm.test.TestFmwk;
import com.ibm.text.UCharacter;
import com.ibm.text.UTF16;

/**
* Testing class for UTF16
* @author Syn Wee Quek
* @since feb 09 2001
*/
public final class UTF16Test extends TestFmwk
{ 
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public UTF16Test()
  {
  }
  
  // public methods ================================================
  
  /**
  * Testing UTF16 class methods append
  */
  public void TestAppend()
  {
      StringBuffer strbuff = new StringBuffer("this is a string ");
      char array[] = new char[UCharacter.MAX_VALUE >> 2];
      int strsize = strbuff.length();
      int arraysize = strsize;
        
      strbuff.getChars(0, strsize, array, 0);
      for (int i = 1; i < UCharacter.MAX_VALUE; i += 100)
      {
          UTF16.append(strbuff, i);
          arraysize = UTF16.append(array, arraysize, i);
          
          String arraystr = new String(array, 0, arraysize);
          if (!arraystr.equals(strbuff.toString())) {
              errln("FAIL Comparing char array append and string append with " 
                    + " 0x" + Integer.toHexString(i));
          }
          
          // this is to cater for the combination of 0xDBXX 0xDC50 which forms
          // a supplementary character
          if (i == 0xDC51) {
              strsize --;
          }
            
          if (UTF16.countCodePoint(strbuff) != strsize + (i / 100) + 1) {
              errln("FAIL Counting code points in string appended with " + 
                    " 0x" + Integer.toHexString(i));
              break;
          }  
      }
  }
  
  /**
  * Testing UTF16 class methods bounds
  */
  public void TestBounds()
  {
      StringBuffer strbuff = 
                            //0   12345     6     7     8     9
          new StringBuffer("\udc000123\ud800\udc00\ud801\udc01\ud802");
      String str = strbuff.toString();
      char array[] = str.toCharArray();
      int boundtype[] = {UTF16.SINGLE_CHAR_BOUNDARY, 
                         UTF16.SINGLE_CHAR_BOUNDARY,
                         UTF16.SINGLE_CHAR_BOUNDARY,
                         UTF16.SINGLE_CHAR_BOUNDARY,
                         UTF16.SINGLE_CHAR_BOUNDARY,
                         UTF16.LEAD_SURROGATE_BOUNDARY,
                         UTF16.TRAIL_SURROGATE_BOUNDARY,
                         UTF16.LEAD_SURROGATE_BOUNDARY,
                         UTF16.TRAIL_SURROGATE_BOUNDARY,
                         UTF16.SINGLE_CHAR_BOUNDARY};
      int length = str.length();
      for (int i = 0; i < length; i ++) {
          if (UTF16.bounds(str, i) != boundtype[i]) {
              errln("FAIL checking bound type at index " + i);
          }
          if (UTF16.bounds(strbuff, i) != boundtype[i]) {
              errln("FAIL checking bound type at index " + i);
          }
          if (UTF16.bounds(array, 0, length, i) != boundtype[i]) {
              errln("FAIL checking bound type at index " + i);
          }
      }
      // does not straddle between supplementary character
      int start = 4;
      int limit = 9;
      int subboundtype1[] = {UTF16.SINGLE_CHAR_BOUNDARY,
                             UTF16.LEAD_SURROGATE_BOUNDARY,
                             UTF16.TRAIL_SURROGATE_BOUNDARY,
                             UTF16.LEAD_SURROGATE_BOUNDARY,
                             UTF16.TRAIL_SURROGATE_BOUNDARY};
      try {
          UTF16.bounds(array, start, limit, -1);
          errln("FAIL Out of bounds index in bounds should fail");
      } catch (Exception e) {
          // getting rid of warnings
          System.out.print("");
      }
      
      for (int i = 0; i < limit - start; i ++) {
          if (UTF16.bounds(array, start, limit, i) != subboundtype1[i]) {
              errln("FAILED Subarray bounds in [" + start + ", " + limit + 
                    "] expected " + subboundtype1[i] + " at offset " + i);
          }
      }
      
      // starts from the mid of a supplementary character
      int subboundtype2[] = {UTF16.SINGLE_CHAR_BOUNDARY,
                             UTF16.LEAD_SURROGATE_BOUNDARY,
                             UTF16.TRAIL_SURROGATE_BOUNDARY};
      
      start = 6;
      limit = 9;
      for (int i = 0; i < limit - start; i ++) {
          if (UTF16.bounds(array, start, limit, i) != subboundtype2[i]) {
              errln("FAILED Subarray bounds in [" + start + ", " + limit + 
                    "] expected " + subboundtype2[i] + " at offset " + i);
          }
      }
      
      // ends in the mid of a supplementary character
      int subboundtype3[] = {UTF16.LEAD_SURROGATE_BOUNDARY,
                             UTF16.TRAIL_SURROGATE_BOUNDARY,
                             UTF16.SINGLE_CHAR_BOUNDARY};
      start = 5;
      limit = 8;
      for (int i = 0; i < limit - start; i ++) {
          if (UTF16.bounds(array, start, limit, i) != subboundtype3[i]) {
              errln("FAILED Subarray bounds in [" + start + ", " + limit + 
                    "] expected " + subboundtype3[i] + " at offset " + i);
          }
      }
  }
  
  /**
  * Testing UTF16 class methods charAt and charAtCodePoint 
  */
  public void TestCharAt()
  {
      StringBuffer strbuff = 
                      new StringBuffer("12345\ud800\udc0167890\ud800\udc02");
      if (UTF16.charAt(strbuff, 0) != '1' || UTF16.charAt(strbuff, 2) != '3' || 
          UTF16.charAt(strbuff, 5) != 0x10001 || 
          UTF16.charAt(strbuff, 6) != 0x10001 || 
          UTF16.charAt(strbuff, 12) != 0x10002 || 
          UTF16.charAt(strbuff, 13) != 0x10002) {
          errln("FAIL Getting character from string buffer error" );
      }
      String str = strbuff.toString();
      if (UTF16.charAt(str, 0) != '1' || UTF16.charAt(str, 2) != '3' || 
          UTF16.charAt(str, 5) != 0x10001 || UTF16.charAt(str, 6) != 0x10001 || 
          UTF16.charAt(str, 12) != 0x10002 || UTF16.charAt(str, 13) != 0x10002) 
      {
          errln("FAIL Getting character from string error" );
      }
      char array[] = str.toCharArray();
      int start = 0;
      int limit = str.length();
      if (UTF16.charAt(array, start, limit, 0) != '1' || 
          UTF16.charAt(array, start, limit, 2) != '3' || 
          UTF16.charAt(array, start, limit, 5) != 0x10001 || 
          UTF16.charAt(array, start, limit, 6) != 0x10001 || 
          UTF16.charAt(array, start, limit, 12) != 0x10002 || 
          UTF16.charAt(array, start, limit, 13) != 0x10002) {
          errln("FAIL Getting character from array error" );
      }
      
      // check the sub array here.
      start = 6;
      limit = 13;
      try {
          UTF16.charAt(array, start, limit, -1);
          errln("FAIL out of bounds error expected");
      } catch (Exception e) {
          System.out.print("");
      }
      try {
          UTF16.charAt(array, start, limit, 8);
          errln("FAIL out of bounds error expected");
      } catch (Exception e) {
          System.out.print("");
      }
      if (UTF16.charAt(array, start, limit, 0) != 0xdc01) {
          errln("FAIL Expected result in subarray 0xdc01");
      }
      if (UTF16.charAt(array, start, limit, 6) != 0xd800) {
          errln("FAIL Expected result in subarray 0xd800");
      }
  }
  
  /**
  * Testing UTF16 class methods countCodePoint
  */
  public void TestCountCodePoint()
  {
    StringBuffer strbuff = new StringBuffer("");
    if (UTF16.countCodePoint(strbuff) != 0 ||
        UTF16.countCodePoint("") != 0) {
        errln("FAIL Counting code points for empty strings");
    }
    
    strbuff = new StringBuffer("this is a string ");
    String str = strbuff.toString();
    char array[] = str.toCharArray();
    int size = str.length();
    
    if (UTF16.countCodePoint(array, 0, 0) != 0) {
        errln("FAIL Counting code points for 0 offset array");
    }
    
    if (UTF16.countCodePoint(str) != size ||
        UTF16.countCodePoint(strbuff) != size ||
        UTF16.countCodePoint(array, 0, size) != size) {
        errln("FAIL Counting code points");
    } 
    
    UTF16.append(strbuff, 0x10000);
    str = strbuff.toString();
    array = str.toCharArray();
    if (UTF16.countCodePoint(str) != size + 1 ||
        UTF16.countCodePoint(strbuff) != size + 1 ||
        UTF16.countCodePoint(array, 0, size + 1) != size + 1 ||
        UTF16.countCodePoint(array, 0, size + 2) != size + 1) {
        errln("FAIL Counting code points");
    }
    UTF16.append(strbuff, 0x61);
    str = strbuff.toString();
    array = str.toCharArray();
    if (UTF16.countCodePoint(str) != size + 2 ||
        UTF16.countCodePoint(strbuff) != size + 2 ||
        UTF16.countCodePoint(array, 0, size + 1) != size + 1 ||
        UTF16.countCodePoint(array, 0, size + 2) != size + 1 ||
        UTF16.countCodePoint(array, 0, size + 3) != size + 2) {
        errln("FAIL Counting code points");
    }
  }
  
  /**
  * Testing UTF16 class methods delete
  */
  public void TestDelete()
  {                                        //01234567890123456
    StringBuffer strbuff = new StringBuffer("these are strings");
    int size = strbuff.length();
    char array[] = strbuff.toString().toCharArray();
    
    UTF16.delete(strbuff, 3);
    UTF16.delete(strbuff, 3);
    UTF16.delete(strbuff, 3);
    UTF16.delete(strbuff, 3);
    UTF16.delete(strbuff, 3);
    UTF16.delete(strbuff, 3);
    try {
        UTF16.delete(strbuff, strbuff.length());
        errln("FAIL deleting out of bounds character should fail");
    } catch (Exception e) {
        System.out.print("");
    }
    UTF16.delete(strbuff, strbuff.length() - 1);
    if (!strbuff.toString().equals("the string")) {
        errln("FAIL expected result after deleting characters is " +
              "\"the string\"");
    }
    
    size = UTF16.delete(array, size, 3);
    size = UTF16.delete(array, size, 3);
    size = UTF16.delete(array, size, 3);
    size = UTF16.delete(array, size, 3);
    size = UTF16.delete(array, size, 3);
    size = UTF16.delete(array, size, 3);
    try {
        UTF16.delete(array, size, size);
        errln("FAIL deleting out of bounds character should fail");
    } catch (Exception e) {
        System.out.print("");
    }
    size = UTF16.delete(array, size, size - 1);
    String str = new String(array, 0, size);
    if (!str.equals("the string")) {
        errln("FAIL expected result after deleting characters is " +
              "\"the string\"");
    }
                              //012345678     9     01     2      3     4
    strbuff = new StringBuffer("string: \ud800\udc00 \ud801\udc01 \ud801\udc01");
    size = strbuff.length();
    array = strbuff.toString().toCharArray();
    
    UTF16.delete(strbuff, 8);
    UTF16.delete(strbuff, 8);
    UTF16.delete(strbuff, 9);
    UTF16.delete(strbuff, 8);
    UTF16.delete(strbuff, 9);
    UTF16.delete(strbuff, 6);
    UTF16.delete(strbuff, 6);
    if (!strbuff.toString().equals("string")) {
        errln("FAIL expected result after deleting characters is \"string\"");
    }
    
    size = UTF16.delete(array, size, 8);
    size = UTF16.delete(array, size, 8);
    size = UTF16.delete(array, size, 9);
    size = UTF16.delete(array, size, 8);
    size = UTF16.delete(array, size, 9);
    size = UTF16.delete(array, size, 6);
    size = UTF16.delete(array, size, 6);
    str = new String(array, 0, size);
    if (!str.equals("string")) {
        errln("FAIL expected result after deleting characters is \"string\"");
    }
  }
  
  /**
  * Testing findOffsetFromCodePoint and findCodePointOffset
  */
  public void TestfindOffset()
  {
    // jitterbug 47
    String str = "a\uD800\uDC00b";
    StringBuffer strbuff = new StringBuffer(str);
    char array[] = str.toCharArray();
    int limit = str.length();
    if (UTF16.findCodePointOffset(str, 0) != 0 ||
        UTF16.findOffsetFromCodePoint(str, 0) != 0 ||
        UTF16.findCodePointOffset(strbuff, 0) != 0 ||
        UTF16.findOffsetFromCodePoint(strbuff, 0) != 0 ||
        UTF16.findCodePointOffset(array, 0, limit, 0) != 0 ||
        UTF16.findOffsetFromCodePoint(array, 0, limit, 0) != 0) {
        errln("FAIL Getting the first codepoint offset to a string with " +
              "supplementary characters");
    }
    if (UTF16.findCodePointOffset(str, 1) != 1 ||
        UTF16.findOffsetFromCodePoint(str, 1) != 1 ||
        UTF16.findCodePointOffset(strbuff, 1) != 1 ||
        UTF16.findOffsetFromCodePoint(strbuff, 1) != 1 ||
        UTF16.findCodePointOffset(array, 0, limit, 1) != 1 ||
        UTF16.findOffsetFromCodePoint(array, 0, limit, 1) != 1) {
        errln("FAIL Getting the second codepoint offset to a string with " +
              "supplementary characters");
    }
    if (UTF16.findCodePointOffset(str, 2) != 1 ||
        UTF16.findOffsetFromCodePoint(str, 2) != 3 ||
        UTF16.findCodePointOffset(strbuff, 2) != 1 ||
        UTF16.findOffsetFromCodePoint(strbuff, 2) != 3 ||
        UTF16.findCodePointOffset(array, 0, limit, 2) != 1 ||
        UTF16.findOffsetFromCodePoint(array, 0, limit, 2) != 3) {
        errln("FAIL Getting the third codepoint offset to a string with " +
              "supplementary characters");
    }
    if (UTF16.findCodePointOffset(str, 3) != 2 ||
        UTF16.findOffsetFromCodePoint(str, 3) != 4 ||
        UTF16.findCodePointOffset(strbuff, 3) != 2 ||
        UTF16.findOffsetFromCodePoint(strbuff, 3) != 4 ||
        UTF16.findCodePointOffset(array, 0, limit, 3) != 2 ||
        UTF16.findOffsetFromCodePoint(array, 0, limit, 3) != 4) {
        errln("FAIL Getting the last codepoint offset to a string with " +
              "supplementary characters");
    }
    if (UTF16.findCodePointOffset(str, 4) != 3 || 
        UTF16.findCodePointOffset(strbuff, 4) != 3 ||
        UTF16.findCodePointOffset(array, 0, limit, 4) != 3) {
        errln("FAIL Getting the length offset to a string with " +
              "supplementary characters");
    }
    try {
        UTF16.findCodePointOffset(str, 5);
        errln("FAIL Getting the a non-existence codepoint to a string with " +
              "supplementary characters");
    } catch (Exception e) {
        // this is a success
        logln("Passed out of bounds codepoint offset");
    }
    try {
        UTF16.findOffsetFromCodePoint(str, 4);
        errln("FAIL Getting the a non-existence codepoint to a string with " +
              "supplementary characters");
    } catch (Exception e) {
        // this is a success
        logln("Passed out of bounds codepoint offset");
    }
    try {
        UTF16.findCodePointOffset(strbuff, 5);
        errln("FAIL Getting the a non-existence codepoint to a string with " +
              "supplementary characters");
    } catch (Exception e) {
        // this is a success
        logln("Passed out of bounds codepoint offset");
    }
    try {
        UTF16.findOffsetFromCodePoint(strbuff, 4);
        errln("FAIL Getting the a non-existence codepoint to a string with " +
              "supplementary characters");
    } catch (Exception e) {
        // this is a success
        logln("Passed out of bounds codepoint offset");
    }
    try {
        UTF16.findCodePointOffset(array, 0, limit, 5);
        errln("FAIL Getting the a non-existence codepoint to a string with " +
              "supplementary characters");
    } catch (Exception e) {
        // this is a success
        logln("Passed out of bounds codepoint offset");
    }
    try {
        UTF16.findOffsetFromCodePoint(array, 0, limit, 4);
        errln("FAIL Getting the a non-existence codepoint to a string with " +
              "supplementary characters");
    } catch (Exception e) {
        // this is a success
        logln("Passed out of bounds codepoint offset");
    }
    
    if (UTF16.findCodePointOffset(array, 1, 3, 0) != 0 ||
        UTF16.findOffsetFromCodePoint(array, 1, 3, 0) != 0 ||
        UTF16.findCodePointOffset(array, 1, 3, 1) != 0 ||
        UTF16.findCodePointOffset(array, 1, 3, 2) != 1 ||
        UTF16.findOffsetFromCodePoint(array, 1, 3, 1) != 2) {
        errln("FAIL Getting valid codepoint offset in sub array");
    }
  }
  
  /**
  * Testing UTF16 class methods getCharCount, *Surrogate
  */
  public void TestGetCharCountSurrogate()
  {
    if (UTF16.getCharCount(0x61) != 1 ||
        UTF16.getCharCount(0x10000) != 2) {
        errln("FAIL getCharCount result failure");
    }
    if (UTF16.getLeadSurrogate(0x61) != 0 ||
        UTF16.getTrailSurrogate(0x61) != 0x61 ||
        UTF16.isLeadSurrogate((char)0x61) ||
        UTF16.isTrailSurrogate((char)0x61) ||
        UTF16.getLeadSurrogate(0x10000) != 0xd800 ||
        UTF16.getTrailSurrogate(0x10000) != 0xdc00 ||
        UTF16.isLeadSurrogate((char)0xd800) != true ||
        UTF16.isTrailSurrogate((char)0xd800) ||
        UTF16.isLeadSurrogate((char)0xdc00) ||
        UTF16.isTrailSurrogate((char)0xdc00) != true) {
        errln("FAIL *Surrogate result failure");
    }
    
    if (UTF16.isSurrogate((char)0x61) || !UTF16.isSurrogate((char)0xd800) ||
        !UTF16.isSurrogate((char)0xdc00)) {
        errln("FAIL isSurrogate result failure");
    }
  }
  
  /**
  * Testing UTF16 class method insert
  */
  public void TestInsert()
  {
    StringBuffer strbuff = new StringBuffer("0123456789");
    char array[] = new char[128];
    strbuff.getChars(0, strbuff.length(), array, 0);
    int length = 10;
    UTF16.insert(strbuff, 5, 't');
    UTF16.insert(strbuff, 5, 's');
    UTF16.insert(strbuff, 5, 'e');
    UTF16.insert(strbuff, 5, 't');
    if (!(strbuff.toString().equals("01234test56789"))) {
        errln("FAIL inserting \"test\"");
    }
    length = UTF16.insert(array, length, 5, 't');
    length = UTF16.insert(array, length, 5, 's');
    length = UTF16.insert(array, length, 5, 'e');
    length = UTF16.insert(array, length, 5, 't');
    String str = new String(array, 0, length);
    if (!(str.equals("01234test56789"))) {
        errln("FAIL inserting \"test\"");
    }
    UTF16.insert(strbuff, 0, 0x10000);
    UTF16.insert(strbuff, 11, 0x10000);
    UTF16.insert(strbuff, strbuff.length(), 0x10000);
    if (!(strbuff.toString().equals(
                     "\ud800\udc0001234test\ud800\udc0056789\ud800\udc00"))) {
        errln("FAIL inserting supplementary characters");
    }
    length = UTF16.insert(array, length, 0, 0x10000);
    length = UTF16.insert(array, length, 11, 0x10000);
    length = UTF16.insert(array, length, length, 0x10000);
    str = new String(array, 0, length);
    if (!(str.equals("\ud800\udc0001234test\ud800\udc0056789\ud800\udc00"))) {
        errln("FAIL inserting supplementary characters");
    }
    
    try {
        UTF16.insert(strbuff, -1, 0);
        errln("FAIL invalid insertion offset");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.insert(strbuff, 64, 0);
        errln("FAIL invalid insertion offset");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.insert(array, length, -1, 0);
        errln("FAIL invalid insertion offset");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.insert(array, length, 64, 0);
        errln("FAIL invalid insertion offset");
    } catch (Exception e) {
        System.out.print("");
    }
  }
  
  /* 
  * Testing moveCodePointOffset APIs
  */
  public void TestMoveCodePointOffset()
  {
                             //01234567890     1     2     3     45678901234
      String str = new String("0123456789\ud800\udc00\ud801\udc010123456789");
      int move1[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 12, 14, 14, 15, 16, 
                     17, 18, 19, 20, 21, 22, 23, 24};
      int move2[] = {2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 14, 15, 15, 16, 17, 
                     18, 19, 20, 21, 22, 23, 24};
      int move3[] = {3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 15, 15, 16, 16, 17, 18,
                     19, 20, 21, 22, 23, 24};
      int size = str.length();
      for (int i = 0; i < size; i ++) {
          if (UTF16.moveCodePointOffset(str, i, 1) != move1[i]) {
              errln("FAIL: Moving offset " + i + 
                    " by 1 codepoint expected result " + move1[i]);
          }
          try {
              if (UTF16.moveCodePointOffset(str, i, 2) != move2[i]) {
                  errln("FAIL: Moving offset " + i + 
                          " by 2 codepoint expected result " + move2[i]);
              }
          } catch (IndexOutOfBoundsException e) {
              if (i <= 22) {
                  throw e;
              }
          }
          try
          {
              if (UTF16.moveCodePointOffset(str, i, 3) != move3[i]) {
                  errln("FAIL: Moving offset " + i + 
                          " by 3 codepoint expected result " + move3[i]);
              }
          } catch (IndexOutOfBoundsException e) {
              if (i <= 21) {
                  throw e;
              }
          }
      }
     
      StringBuffer strbuff = new StringBuffer(str);
      for (int i = 0; i < size; i ++) {
          if (UTF16.moveCodePointOffset(strbuff, i, 1) != move1[i]) {
              errln("FAIL: Moving offset " + i + 
                    " by 1 codepoint expected result " + move1[i]);
          }
          try {
              if (UTF16.moveCodePointOffset(strbuff, i, 2) != move2[i]) {
                  errln("FAIL: Moving offset " + i + 
                          " by 2 codepoint expected result " + move2[i]);
              }
          } catch (IndexOutOfBoundsException e) {
              if (i <= 22) {
                  throw e;
              }
          }
          try
          {
              if (UTF16.moveCodePointOffset(strbuff, i, 3) != move3[i]) {
                  errln("FAIL: Moving offset " + i + 
                          " by 3 codepoint expected result " + move3[i]);
              }
          } catch (IndexOutOfBoundsException e) {
              if (i <= 21) {
                  throw e;
              }
          }  
      }
      
      char strarray[] = str.toCharArray();
      for (int i = 0; i < size; i ++) {
          if (UTF16.moveCodePointOffset(strarray, 0, size, i, 1) != move1[i]) {
              errln("FAIL: Moving offset " + i + 
                    " by 1 codepoint expected result " + move1[i]);
          }
          try {
              if (UTF16.moveCodePointOffset(strarray, 0, size, i, 2) != 
                                                                   move2[i]) {
                  errln("FAIL: Moving offset " + i + 
                          " by 2 codepoint expected result " + move2[i]);
              }
          } catch (IndexOutOfBoundsException e) {
              if (i <= 22) {
                  throw e;
              }
          }
          try
          {
              if (UTF16.moveCodePointOffset(strarray,  0, size, i, 3) != 
                                                                   move3[i]) {
                  errln("FAIL: Moving offset " + i + 
                          " by 3 codepoint expected result " + move3[i]);
              }
          } catch (IndexOutOfBoundsException e) {
              if (i <= 21) {
                  throw e;
              }
          }  
      }
      
      if (UTF16.moveCodePointOffset(strarray, 9, 13, 0, 2) != 3) {
          errln("FAIL: Moving offset 0 by 2 codepoint in subarray [9, 13] " +
                "expected result 3");
      }
      if (UTF16.moveCodePointOffset(strarray, 9, 13, 1, 2) != 4) {
          errln("FAIL: Moving offset 1 by 2 codepoint in subarray [9, 13] " +
                "expected result 4");
      }
      if (UTF16.moveCodePointOffset(strarray, 11, 14, 0, 2) != 3) {
          errln("FAIL: Moving offset 0 by 2 codepoint in subarray [11, 14] " +
                "expected result 3");
      }
  }
  
  /**
  * Testing UTF16 class methods setCharAt
  */
  public void TestSetCharAt()
  {
    StringBuffer strbuff = new StringBuffer("012345");
    char array[] = new char[128];
    strbuff.getChars(0, strbuff.length(), array, 0);
    int length = 6;
    for (int i = 0; i < length; i ++) {
        UTF16.setCharAt(strbuff, i, '0');
        UTF16.setCharAt(array, length, i, '0');
    }
    String str = new String(array, 0, length);
    if (!(strbuff.toString().equals("000000")) ||
        !(str.equals("000000"))) {
        errln("FAIL: setChar to '0' failed");
    }
    UTF16.setCharAt(strbuff, 0, 0x10000);
    UTF16.setCharAt(strbuff, 4, 0x10000);
    UTF16.setCharAt(strbuff, 7, 0x10000);
    if (!(strbuff.toString().equals(
                               "\ud800\udc0000\ud800\udc000\ud800\udc00"))) {
        errln("FAIL: setChar to 0x10000 failed");
    }
    length = UTF16.setCharAt(array, length, 0, 0x10000);
    length = UTF16.setCharAt(array, length, 4, 0x10000);
    length = UTF16.setCharAt(array, length, 7, 0x10000);
    str = new String(array, 0, length);
    if (!(str.equals("\ud800\udc0000\ud800\udc000\ud800\udc00"))) {
        errln("FAIL: setChar to 0x10000 failed");
    }
    try {
        UTF16.setCharAt(strbuff, -1, 0);
        errln("FAIL: setting character at invalid offset");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.setCharAt(array, length, -1, 0);
        errln("FAIL: setting character at invalid offset");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.setCharAt(strbuff, length, 0);
        errln("FAIL: setting character at invalid offset");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.setCharAt(array, length, length, 0);
        errln("FAIL: setting character at invalid offset");
    } catch (Exception e) {
        System.out.print("");
    }
  }

  /**
  * Testing UTF16 valueof APIs
  */
  public void TestValueOf()
  {
    if (!UTF16.valueOf(0x61).equals("a") || 
        !UTF16.valueOf(0x10000).equals("\ud800\udc00")) {
        errln("FAIL: valueof(char32)");
    }
    String str = new String("01234\ud800\udc0056789");
    StringBuffer strbuff = new StringBuffer(str);
    char array[] = str.toCharArray();
    int length = str.length();
    
    String expected[] = {"0", "1", "2", "3", "4", "\ud800\udc00", 
                         "\ud800\udc00", "5", "6", "7", "8", "9"};
    for (int i = 0; i < length; i ++) {
        if (!UTF16.valueOf(str, i).equals(expected[i]) ||
            !UTF16.valueOf(strbuff, i).equals(expected[i]) ||
            !UTF16.valueOf(array, 0, length, i).equals(expected[i])) {
            errln("FAIL: valueOf() expected " + expected[i]);
        }
    }
    try {
        UTF16.valueOf(str, -1);
        errln("FAIL: out of bounds error expected");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.valueOf(strbuff, -1);
        errln("FAIL: out of bounds error expected");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.valueOf(array, 0, length, -1);
        errln("FAIL: out of bounds error expected");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.valueOf(str, length);
        errln("FAIL: out of bounds error expected");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.valueOf(strbuff, length);
        errln("FAIL: out of bounds error expected");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.valueOf(array, 0, length, length);
        errln("FAIL: out of bounds error expected");
    } catch (Exception e) {
        System.out.print("");
    }
    if (!UTF16.valueOf(array, 6, length, 0).equals("\udc00") ||
        !UTF16.valueOf(array, 0, 6, 5).equals("\ud800")) {
        errln("FAIL: error getting partial supplementary character");
    }
    try {
        UTF16.valueOf(array, 3, 5, -1);
        errln("FAIL: out of bounds error expected");
    } catch (Exception e) {
        System.out.print("");
    }
    try {
        UTF16.valueOf(array, 3, 5, 3);
        errln("FAIL: out of bounds error expected");
    } catch (Exception e) {
        System.out.print("");
    }
  }
  
  public static void main(String[] arg)
  {
    try
    {
      UTF16Test test = new UTF16Test();
      test.run(arg);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}

