/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/test/text/Attic/UTF16Test.java,v $ 
* $Date: 2001/02/26 23:52:29 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu.test.text;

import com.ibm.test.TestFmwk;
import com.ibm.icu.text.UCharacter;
import com.ibm.icu.text.UTF16;

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
        
      if (UTF16.countCP(str.toString()) != initstrsize + (i / 100) + 1)
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
             UTF16.boundsAtCPOffset(str.toString(), initstrsize + (i /100)) 
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
            UTF16.boundsAtCPOffset(str.toString(), initstrsize + (i / 100)) 
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
  * Testing UTF16 class methods findCPOffset, findOffsetFromCP, charAt and
  * charAtCP 
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
        UTF16.charAtCPOffset(s, 0) != '1' || UTF16.charAtCPOffset(s, 2) != '3' || 
        UTF16.charAtCPOffset(s, 5) != 0x10001 || 
        UTF16.charAtCPOffset(s, 6) != '6' || 
        UTF16.charAtCPOffset(s, 11) != 0x10002)
      errln("FAIL Getting character from string error" );

    if (UTF16.findCPOffset(s, 3) != 3 || UTF16.findCPOffset(s, 5) != 5 || 
        UTF16.findCPOffset(s, 6) != 5)
      errln("FAIL Getting codepoint offset from string error" );
    if (UTF16.findOffsetFromCP(s, 3) != 3 || 
        UTF16.findOffsetFromCP(s, 5) != 5 || 
        UTF16.findOffsetFromCP(s, 6) != 7)
      errln("FAIL Getting UTF16 offset from codepoint in string error" );
      
    UTF16.setCharAt(str, 3, '3');
    UTF16.setCharAtCPOffset(str, 4, '3');
    if (UTF16.charAt(str.toString(), 3) != '3' || 
        UTF16.charAtCPOffset(str.toString(), 3) != '3' ||
        UTF16.charAt(str.toString(), 4) != '3' || 
        UTF16.charAtCPOffset(str.toString(), 4) != '3')
      errln("FAIL Setting non-supplementary characters at a " +
            "non-supplementary position");
            
    UTF16.setCharAt(str, 5, '3');
    if (UTF16.charAt(str.toString(), 5) != '3' || 
        UTF16.charAtCPOffset(str.toString(), 5) != '3' || 
        UTF16.charAt(str.toString(), 6) != '6' || 
        UTF16.charAtCPOffset(str.toString(), 5) != '3' || 
        UTF16.charAtCPOffset(str.toString(), 6) != '6')
      errln("FAIL Setting non-supplementary characters at a " +
            "supplementary position");
            
    UTF16.setCharAt(str, 5, 0x10001);
    if (UTF16.charAt(str.toString(), 5) != 0x10001 || 
        UTF16.charAtCPOffset(str.toString(), 5) != 0x10001 ||
        UTF16.charAt(str.toString(), 7) != '6' || 
        UTF16.charAtCPOffset(str.toString(), 6) != '6')
      errln("FAIL Setting supplementary characters at a " +
            "non-supplementary position");
            
    UTF16.setCharAtCPOffset(str, 5, '3');
    if (UTF16.charAt(str.toString(), 5) != '3' || 
        UTF16.charAtCPOffset(str.toString(), 5) != '3' ||
        UTF16.charAt(str.toString(), 6) != '6' || 
        UTF16.charAtCPOffset(str.toString(), 6) != '6')
      errln("FAIL Setting non-supplementary characters at a " +
            "supplementary position");
            
    UTF16.setCharAt(str, 5, 0x10001);
    if (UTF16.charAt(str.toString(), 5) != 0x10001 || 
        UTF16.charAtCPOffset(str.toString(), 5) != 0x10001 ||
        UTF16.charAt(str.toString(), 7) != '6' || 
        UTF16.charAtCPOffset(str.toString(), 6) != '6')
      errln("FAIL Setting supplementary characters at a " +
            "non-supplementary position");
            
     
   UTF16.setCharAt(str, 5, 0xD800);
   UTF16.setCharAt(str, 6, 0xD800);
   if (UTF16.charAt(str.toString(), 5) != 0xD800 ||
       UTF16.charAt(str.toString(), 6) != 0xD800 ||
       UTF16.charAtCPOffset(str.toString(), 5) != 0xD800 ||
       UTF16.charAtCPOffset(str.toString(), 6) != 0xD800)
      errln("FAIL Setting lead characters at a supplementary position");   
      
   UTF16.setCharAt(str, 5, 0xDDDD);
   if (UTF16.charAt(str.toString(), 5) != 0xDDDD ||
       UTF16.charAt(str.toString(), 6) != 0xD800 ||
       UTF16.charAtCPOffset(str.toString(), 5) != 0xDDDD ||
       UTF16.charAtCPOffset(str.toString(), 6) != 0xD800)
      errln("FAIL Setting trail characters at a surrogate position");
      
   UTF16.setCharAt(str, 5, '3');
   if (UTF16.charAt(str.toString(), 5) != '3' ||
       UTF16.charAt(str.toString(), 6) != 0xD800 ||
       UTF16.charAtCPOffset(str.toString(), 5) != '3' ||
       UTF16.charAtCPOffset(str.toString(), 6) != 0xD800)
      errln("FAIL Setting non-supplementary characters at a surrogate " +
            "position");
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

