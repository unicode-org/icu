/*
******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/normalizer/Attic/FCDBuilder.java,v $ 
* $Date: 2001/03/28 00:01:13 $ 
* $Revision: 1.4 $
*
******************************************************************************
*/

package com.ibm.tools.normalizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import com.ibm.util.CharTrie;
import com.ibm.text.UCharacter;
import com.ibm.text.UTF16;
import com.ibm.text.Normalizer;

/**
* Class to generate modified checkFCD data for collation.
* Data generated is used only in internal ICU collation.
* FCD is the set of strings such that for each string if you simply decomposed
* any composites (including singleton composites) without canonical reordering.
* FCD is not a normalization form, since there's no uniqueness.
*/
public class FCDBuilder
{
  // public methods ----------------------------------------------------
  
  /**
  * constructor with default output file path
  */
  public FCDBuilder()
  {
  }
  
  /**
  * Building method.
  * Each unicode character will be used to generate data, output to the default
  * file path  
  */
  public void build()
  {
    build(DEFAULT_OUTPUT_PATH_);
  }
  
  /**
  * Building method.
  * Each unicode character will be used to generate data.
  * @param output file path
  */
  public void build(String output)
  {
    char result[] = new char[UCharacter.MAX_VALUE + 1];
    
    String cstr,
           nfd;
    for (int ch = UCharacter.MIN_VALUE; ch <= UCharacter.MAX_VALUE; ch ++) {
      result[ch] = getFCD(ch);
    }
    
    CharTrie trie = new CharTrie(result);
      
    // testing, checking trie values
    for (int ch = UCharacter.MIN_VALUE; ch <= UCharacter.MAX_VALUE; ch ++) {
      if (trie.getValue(ch) != getFCD(ch))
      {
        System.out.println("error at 0x" + Integer.toHexString(ch) + " " +
                           getFCD(ch));
        break;
      }
    }
   
    try
    {
      FileWriter f = new FileWriter(output);
      BufferedWriter w = new BufferedWriter(f);
      String s = trie.toString();
      w.write(s);
      w.close();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }
  
  /**
  * Main method
  */
  public static void main(String arg[])
  {
    FCDBuilder fcdb = new FCDBuilder();
    fcdb.build();
  }
  
  // private methods -----------------------------------------------------
  
  /**
  * Retrieved the FCDcheck value of the argument codepoint.
  * f(ch) = combining class of 
  * (first codepoint in (NFD of ch)) | (last code point in (NFD of ch))
  * @param ch character to get FCD from
  */
  private char getFCD(int ch)
  {
    String cstr = UCharacter.toString(ch),
           nfd = Normalizer.decompose(cstr, false, 0);
    int lastindex = UTF16.countCodePoint(nfd) - 1;
    int firstch = UTF16.charAtCodePointOffset(nfd, 0);
    int lastch = UTF16.charAtCodePointOffset(nfd, lastindex);
    return (char)((UCharacter.getCombiningClass(firstch) << LEAD_CC_SHIFT_) |
                  (UCharacter.getCombiningClass(lastch) & LAST_BYTE_MASK_));
  }

  // private data members ------------------------------------------------
  
  /**
  * Output file path
  */
  private final String DEFAULT_OUTPUT_PATH_ = "fcdcheck.txt";
  
  /**
  * Lead combining class shift
  */
  private final int LEAD_CC_SHIFT_ = 8;
  
  /**
  * Last byte mask
  */
  private final int LAST_BYTE_MASK_ = 0xFF;
}