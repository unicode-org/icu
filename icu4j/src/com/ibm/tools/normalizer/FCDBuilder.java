/*
******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/normalizer/Attic/FCDBuilder.java,v $ 
* $Date: 2001/02/28 20:53:29 $ 
* $Revision: 1.1 $
*
******************************************************************************
*/

package com.ibm.tools.normalizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import com.ibm.util.ByteTrie;
import com.ibm.icu.text.UCharacter;
import com.ibm.icu.text.UTF16;
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
    byte result[] = new byte[UCharacter.MAX_VALUE + 1];
    
    String cstr,
           nfd;
    for (int ch = UCharacter.MIN_VALUE; ch <= UCharacter.MAX_VALUE; ch ++)
      result[ch] = getFCD(ch);
    
    ByteTrie trie = new ByteTrie(result);
      
    // testing, checking trie values
    for (int ch = UCharacter.MIN_VALUE; ch <= UCharacter.MAX_VALUE; ch ++)
      if (trie.getValue(ch) != getFCD(ch))
      {
        System.out.println("error at 0x" + Integer.toHexString(ch) + " " +
                           getFCD(ch));
        break;
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
  * f(ch) = combining class of (last code point in (NFD of ch))
  */
  private byte getFCD(int ch)
  {
    String cstr = UCharacter.toString(ch),
           nfd = Normalizer.decompose(cstr, false, 0);
    int lastindex = UTF16.countCP(nfd) - 1;
    int lastch = UTF16.charAtCPOffset(nfd, lastindex);
    return UCharacter.getCombiningClass(lastch);
  }

  // private data members ------------------------------------------------
  
  /**
  * Output file path
  */
  private final String DEFAULT_OUTPUT_PATH_ = "fcdcheck.txt";
}