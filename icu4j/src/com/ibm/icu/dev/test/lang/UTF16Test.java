/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/lang/UTF16Test.java,v $ 
* $Date: 2001/08/23 02:20:59 $ 
* $Revision: 1.6 $
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
  * Testing UTF16 class methods append, getCharCount and bounds
  */
  public void TestUTF16AppendBoundCount()
  {
    StringBuffer str = new StringBuffer("this is a string ");
    int initstrsize = str.length();
    int length;
    
    for (int i = UCharacter.MIN_VALUE; i < UCharacter.MAX_VALUE; i += 100)
    {
      length = str.length();
      UTF16.append(str, i);
      
      // this is to cater for the combination of 0xDBXX 0xDC50 which forms
      // a supplementary character
      if (i == 0xDC50)
        initstrsize --;
        
      if (UTF16.countCodePoint(str.toString()) != initstrsize + (i / 100) + 1)
      {
        errln("FAIL Counting code points in string appended with " + 
              " 0x" + Integer.toHexString(i));
        break;
      }
       
      if (!UCharacter.isSupplementary(i))
      {
        if (UTF16.getCharCount(i) != 1)
        {
          errln("FAIL Counting BMP character size error" );
          break;
        }  
        if (str.length() != length + 1)
        {
          errln("FAIL Adding a BMP character error" );
          break;
        }
        if (!UTF16.isSurrogate((char)i) && 
            (UTF16.bounds(str.toString(), str.length() - 1) != 
                                                UTF16.SINGLE_CHAR_BOUNDARY ||
             UTF16.boundsAtCodePointOffset(str.toString(), 
                                           initstrsize + (i /100)) 
                                              != UTF16.SINGLE_CHAR_BOUNDARY))
        {
          errln("FAIL Finding BMP character bounds error" );
          break;
        }
      }
      else 
      {
        if (UTF16.getCharCount(i) != 2)
        {
          errln("FAIL Counting Supplementary character size error" );
          break;
        }
        if (str.length() != length + 2)
        {
          errln("FAIL Adding a Supplementary character error" );
          break;
        }
        length = str.length();
        if (UTF16.bounds(str.toString(), str.length() - 2) != 
            UTF16.LEAD_SURROGATE_BOUNDARY || 
            UTF16.bounds(str.toString(), str.length() - 1) != 
            UTF16.TRAIL_SURROGATE_BOUNDARY ||
            UTF16.boundsAtCodePointOffset(str.toString(), 
                                          initstrsize + (i / 100)) 
                                            != UTF16.LEAD_SURROGATE_BOUNDARY)
        {
          errln("FAIL Finding Supplementary character bounds error with " +
                "string appended with 0x" + Integer.toHexString(i));
          break;
        }
      }
    } 
  }
  
  /**
  * Testing UTF16 class methods findCodePointOffset, findOffsetFromCodePoint, charAt and
  * charAtCodePoint 
  */
  public void TestUTF16OffsetCharAt()
  {
    StringBuffer str = new StringBuffer("12345");
    UTF16.append(str, 0x10001);
    str.append("67890");
    UTF16.append(str, 0x10002);
    String s = str.toString();
    if (UTF16.charAt(s, 0) != '1' || UTF16.charAt(s, 2) != '3' || 
        UTF16.charAt(s, 5) != 0x10001 || UTF16.charAt(s, 6) != 0x10001 || 
        UTF16.charAt(s, 12) != 0x10002 || UTF16.charAt(s, 13) != 0x10002 ||
        UTF16.charAtCodePointOffset(s, 0) != '1' || 
        UTF16.charAtCodePointOffset(s, 2) != '3' || 
        UTF16.charAtCodePointOffset(s, 5) != 0x10001 || 
        UTF16.charAtCodePointOffset(s, 6) != '6' || 
        UTF16.charAtCodePointOffset(s, 11) != 0x10002)
      errln("FAIL Getting character from string error" );

    if (UTF16.findCodePointOffset(s, 3) != 3 || 
        UTF16.findCodePointOffset(s, 5) != 5 || 
        UTF16.findCodePointOffset(s, 6) != 5)
      errln("FAIL Getting codepoint offset from string error" );
    if (UTF16.findOffsetFromCodePoint(s, 3) != 3 || 
        UTF16.findOffsetFromCodePoint(s, 5) != 5 || 
        UTF16.findOffsetFromCodePoint(s, 6) != 7)
      errln("FAIL Getting UTF16 offset from codepoint in string error" );
      
    UTF16.setCharAt(str, 3, '3');
    UTF16.setCharAtCodePointOffset(str, 4, '3');
    if (UTF16.charAt(str.toString(), 3) != '3' || 
        UTF16.charAtCodePointOffset(str.toString(), 3) != '3' ||
        UTF16.charAt(str.toString(), 4) != '3' || 
        UTF16.charAtCodePointOffset(str.toString(), 4) != '3')
      errln("FAIL Setting non-supplementary characters at a " +
            "non-supplementary position");
            
    UTF16.setCharAt(str, 5, '3');
    if (UTF16.charAt(str.toString(), 5) != '3' || 
        UTF16.charAtCodePointOffset(str.toString(), 5) != '3' || 
        UTF16.charAt(str.toString(), 6) != '6' || 
        UTF16.charAtCodePointOffset(str.toString(), 5) != '3' || 
        UTF16.charAtCodePointOffset(str.toString(), 6) != '6')
      errln("FAIL Setting non-supplementary characters at a " +
            "supplementary position");
            
    UTF16.setCharAt(str, 5, 0x10001);
    if (UTF16.charAt(str.toString(), 5) != 0x10001 || 
        UTF16.charAtCodePointOffset(str.toString(), 5) != 0x10001 ||
        UTF16.charAt(str.toString(), 7) != '6' || 
        UTF16.charAtCodePointOffset(str.toString(), 6) != '6')
      errln("FAIL Setting supplementary characters at a " +
            "non-supplementary position");
            
    UTF16.setCharAtCodePointOffset(str, 5, '3');
    if (UTF16.charAt(str.toString(), 5) != '3' || 
        UTF16.charAtCodePointOffset(str.toString(), 5) != '3' ||
        UTF16.charAt(str.toString(), 6) != '6' || 
        UTF16.charAtCodePointOffset(str.toString(), 6) != '6')
      errln("FAIL Setting non-supplementary characters at a " +
            "supplementary position");
            
    UTF16.setCharAt(str, 5, 0x10001);
    if (UTF16.charAt(str.toString(), 5) != 0x10001 || 
        UTF16.charAtCodePointOffset(str.toString(), 5) != 0x10001 ||
        UTF16.charAt(str.toString(), 7) != '6' || 
        UTF16.charAtCodePointOffset(str.toString(), 6) != '6')
      errln("FAIL Setting supplementary characters at a " +
            "non-supplementary position");
            
     
   UTF16.setCharAt(str, 5, 0xD800);
   UTF16.setCharAt(str, 6, 0xD800);
   if (UTF16.charAt(str.toString(), 5) != 0xD800 ||
       UTF16.charAt(str.toString(), 6) != 0xD800 ||
       UTF16.charAtCodePointOffset(str.toString(), 5) != 0xD800 ||
       UTF16.charAtCodePointOffset(str.toString(), 6) != 0xD800)
      errln("FAIL Setting lead characters at a supplementary position");   
      
   UTF16.setCharAt(str, 5, 0xDDDD);
   if (UTF16.charAt(str.toString(), 5) != 0xDDDD ||
       UTF16.charAt(str.toString(), 6) != 0xD800 ||
       UTF16.charAtCodePointOffset(str.toString(), 5) != 0xDDDD ||
       UTF16.charAtCodePointOffset(str.toString(), 6) != 0xD800)
      errln("FAIL Setting trail characters at a surrogate position");
      
   UTF16.setCharAt(str, 5, '3');
   if (UTF16.charAt(str.toString(), 5) != '3' ||
       UTF16.charAt(str.toString(), 6) != 0xD800 ||
       UTF16.charAtCodePointOffset(str.toString(), 5) != '3' ||
       UTF16.charAtCodePointOffset(str.toString(), 6) != 0xD800)
      errln("FAIL Setting non-supplementary characters at a surrogate " +
            "position");
  }
  
  /**
  * Testing countCodePoint, findOffsetFromCodePoint and findCodePointOffset
  */
  public void TestUTF16CodePointOffset()
  {
    // jitterbug 47
    String str = "a\uD800\uDC00b";
    if (UTF16.findCodePointOffset(str, 0) != 0 ||
        UTF16.findOffsetFromCodePoint(str, 0) != 0) {
        errln("FAIL Getting the first codepoint offset to a string with " +
              "supplementary characters");
    }
    if (UTF16.findCodePointOffset(str, 1) != 1 ||
        UTF16.findOffsetFromCodePoint(str, 1) != 1) {
        errln("FAIL Getting the second codepoint offset to a string with " +
              "supplementary characters");
    }
    if (UTF16.findCodePointOffset(str, 2) != 1 ||
        UTF16.findOffsetFromCodePoint(str, 2) != 3) {
        errln("FAIL Getting the third codepoint offset to a string with " +
              "supplementary characters");
    }
    if (UTF16.findCodePointOffset(str, 3) != 2 ||
        UTF16.findOffsetFromCodePoint(str, 3) != 4) {
        errln("FAIL Getting the last codepoint offset to a string with " +
              "supplementary characters");
    }
    if (UTF16.findCodePointOffset(str, 4) != 3) {
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
    if (UTF16.countCodePoint(str) != 3) {
        errln("FAIL Counting the number of codepoints in a string with " +
              "supplementary characters");
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

