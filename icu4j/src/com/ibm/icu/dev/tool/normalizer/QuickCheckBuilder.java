/*
******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/normalizer/Attic/QuickCheckBuilder.java,v $ 
* $Date: 2001/05/18 20:45:10 $ 
* $Revision: 1.4 $
*
******************************************************************************
*/

package com.ibm.tools.normalizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Writer;
import java.io.Reader;
import java.io.EOFException;
import java.util.StringTokenizer;

import com.ibm.util.ByteTrie;
import com.ibm.text.UCharacter;
import com.ibm.text.UTF16;

/**
* Class to generate modified quickcheck data for collation.
* Data generated is a trie of normalization form corresponding to the index 
* code point.
* Hence codepoint 0xABCD will have normalization form 
* <code>
*    quickcheck[codepoint] = 
*              STAGE_3_[STAGE_2_[STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
*              ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
*              (codepoint & STAGE_3_MASK_)];
* </code>
* value is a byte containing 2 sets of 4 bits information.<br>
* bits 1 2 3 4                        5678<br>
*      NFKC NFC NFKD NFD MAYBES       NFKC NFC NFKD NFD YES<br>
* ie if quick[0xABCD] = 10000001, this means that 0xABCD is in NFD form and 
* maybe in NFKC form.
*/
public class QuickCheckBuilder
{
  // public methods ----------------------------------------------------
  
  /**
  * constructor with default input, output file path
  */
  public QuickCheckBuilder()
  {
  }
  
  /**
  * Building method.
  * Each unicode character will be used to generate data, output to the default
  * file path  
  */
  public void build()
  {
    build(DEFAULT_INPUT_PATH_, DEFAULT_OUTPUT_PATH_);
  }
  
  /**
  * Building method.
  * Each unicode character will be used to generate data.
  * @param input file path
  * @param output file path
  */
  public void build(String input, String output)
  {   
    try
    {
      byte result[] = getQuickCheckArray(input);
      int notyes[] = getNotYesFirstCP(result);
      ByteTrie trie = new ByteTrie(result);
      FileWriter f = new FileWriter(output);
      BufferedWriter w = new BufferedWriter(f);
      
      w.write("# QuickCheck data\n");
      w.write("# Generated from NormalizationQuickCheck.txt\n\n");
      w.write("int UQUICK_CHECK_MIN_VALUES_[] = {");
      w.write("0x" + Integer.toHexString(notyes[0]) + ", 0x" + 
              Integer.toHexString(notyes[1]) + ", 0x" + 
              Integer.toHexString(notyes[2]) + ", 0x" + 
              Integer.toHexString(notyes[3]) + "};\n");
      
      String s = trie.toString();
      w.write(s);
      w.close();
      System.out.println("test " + test(input, trie));
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
    QuickCheckBuilder qcb = new QuickCheckBuilder();
    qcb.build();
  }
  
  // private methods -----------------------------------------------------
  
  /**
  * Method to manipulate the values of the read in normalized form
  * @param array of quickcheck values
  * @param startcp starting code point
  * @param endcp ending code point
  * @param normalizationformat normalization format and the following argument
  *        state indicates if codepoints from startcp to endcp are of the 
  *        indicated normalization format
  * @param state indicates if codepoints from startcp to endcp are of the 
  *        argument normalization format
  */
  private void formQuickCheckValue(byte[] array, int startcp, int endcp,
                                   String normalizationformat, String state) 
  {
    byte value = NFD_MAYBE_MASK_;
    byte clear = NFD_NO_MASK_;

    if (normalizationformat.equals(NFC_)) {
      clear = NFC_NO_MASK_;
      if (state.equals(NO_)) {
        value = NFC_NO_MASK_;
      }
      else {
        if (state.equals(YES_)) {
          value = NFC_YES_MASK_;
        }
        else {
          value = NFC_MAYBE_MASK_;
        }
      }
    }
    else {
      if (normalizationformat.equals(NFD_)) {
        if (state.equals(NO_)) {
          value = NFD_NO_MASK_;
        }
        else {
          if (state.equals(YES_)) {
            value = NFD_YES_MASK_;
          }
        }
      }
      else {
        if (normalizationformat.equals(NFKC_)) {
          clear = NFKC_NO_MASK_;
          if (state.equals(NO_)) {
            value = NFKC_NO_MASK_;
          }
          else {
            if (state.equals(YES_)) {
              value = NFKC_YES_MASK_;
            }
            else {
              value = NFKC_MAYBE_MASK_; 
            }
          }
        }
        else {
          if (normalizationformat.equals(NFKD_)) {
            clear = NFKD_NO_MASK_;
            if (state.equals(NO_)) {
              value = NFKD_NO_MASK_;
            }
            else {
              if (state.equals(YES_)) {
                value = NFKD_YES_MASK_;
              }
              else {
                value = NFKD_MAYBE_MASK_;
              }
            }
          }
        }
      }
    }

    for (; startcp <= endcp; startcp ++) {
      array[startcp] &= clear;
      if (value != clear) {
        array[startcp] |= value;
      }
    }
  }

  /**
  * Reads in the NormalizationQuickCheck.txt file and generates a byte of 
  * QuickCheck data for each codepoint
  * @param input file path
  * @exception thrown when file reading error occurs
  */
  private byte[] getQuickCheckArray(String input) throws Exception
  {
    byte result[] = new byte[UCharacter.MAX_VALUE + 1];
    
    // initializing quickcheck array
    initializeQuickCheckArray(result);
    
    StringTokenizer st;
    // read in start and end codepoint
    int startcp,
        endcp;
    // read in normalization format and its corresponding value
    String token,
           nf,
           check;
    
    try
    {
      FileReader f = new FileReader(input);
      BufferedReader r = new BufferedReader(f);
      String s = "";
      while (true)
      {
        s = r.readLine();
        if (s == null)
          break;
          
        if (!s.equals("") && s.charAt(0) != '#')
        {
          st = new StringTokenizer(s, "; ");
          startcp = Integer.parseInt(st.nextToken(), 16);
          endcp = Integer.parseInt(st.nextToken(), 16);
          nf = st.nextToken();
          check = st.nextToken();
          formQuickCheckValue(result, startcp, endcp, nf, check);
        }
      }
      r.close();
    }
    catch(EOFException e)
    {
    }
    
    return result;
  }
  
  /**
  * Initializing quickcheck array.
  * All assigned codepoints have the value Y, all unassigned codepoints have 
  * the value M, all non-character codepoints (D800..DFFF, *FFFE, *FFFF) have 
  * the value N.
  * @param array quickcheck byte array
  */
  private void initializeQuickCheckArray(byte[] array)
  {
    for (int ch = UCharacter.MIN_VALUE; ch <= UCharacter.MAX_VALUE; ch ++)
    {
      array[ch] = DEFINED_CHAR_NORM_VALUE_;  
      if ((ch >= SURROGATE_START_ && ch <= SURROGATE_END_) || 
          ((ch & MASK_LAST_16_BITS_) >= 
                                     MIN_NOT_A_CHAR_AFTER_MASK_LAST_16_BITS_))
        array[ch] = NOT_A_CHARACTER_VALUE_;
    }
  }
  
  /**
  * Tests the generated trie with the data in the input file.
  * @param input file path
  * @param trie object
  * @exception thrown when file reading error occurs
  */
  private boolean test(String input, ByteTrie trie) throws Exception
  {
    StringTokenizer st;
    // read in start and end codepoint
    int startcp,
        endcp;
    // read in normalization format and its corresponding value
    String token,
           nf,
           check;
    byte temp;
    
    try
    {
      FileReader f = new FileReader(input);
      BufferedReader r = new BufferedReader(f);
      String s = "";
      while (true)
      {
        s = r.readLine();
        if (s == null)
          break;
          
        if (!s.equals("") && s.charAt(0) != '#')
        {
          st = new StringTokenizer(s, "; ");
          startcp = Integer.parseInt(st.nextToken(), 16);
          endcp = Integer.parseInt(st.nextToken(), 16);
          nf = st.nextToken();
          check = st.nextToken();
          
          byte mask = 0;
          
          if (nf.equals(NFC_))
            mask = (byte)0x44;
          else
            if (nf.equals(NFD_))
              mask = (byte)0x11;
            else 
              if (nf.equals(NFKC_))
                mask = (byte)0x88;
              else 
                if (nf.equals(NFKD_))
                  mask = (byte)0x22;
          for (; startcp <= endcp; startcp ++)
          {
            temp = (byte)(trie.getValue(startcp) & mask);
            if (check.equals(NO_) && (temp != 0))
            {
              System.out.println("Error at NO " + startcp);
              return false;
            }
            
            if (check.equals(YES_) && (temp < 0 || temp > 0x8))
            {
              System.out.println("Error at YES " + startcp);
              return false;
            }
            
            if (check.equals(MAYBE_) && (temp < 0x10) && (temp != -128))
            {
              System.out.println("Error at MAYBE " + startcp);
              return false;
            }
          }
        }
      }
      r.close();
    }
    catch(EOFException e)
    {
    }
    return true;
  }
  
  /**
  * Returns an array of the first codepoints that do not have a YES for their
  * respective normalization format. Hence 
  * <p> getNotYesFirstCP[0] is the first codepoint that is not a NFD_YES<br>
  * getNotYesFirstCP[1] is the first codepoint that is not a NFKD_YES<br>
  * getNotYesFirstCP[2] is the first codepoint that is not a NFC_YES<br>
  * getNotYesFirstCP[3] is the first codepoint that is not a NFKC_YES<br>
  * @param quickcheck array of quickcheck values
  * @return array of first codepoints not a YES
  */
  private int[] getNotYesFirstCP(byte[] quickcheck)
  {
    int result[] = {UCharacter.MAX_VALUE, UCharacter.MAX_VALUE, 
                    UCharacter.MAX_VALUE, UCharacter.MAX_VALUE};
    int length = quickcheck.length;
    byte value;
    for (int codepoint = 0; codepoint < UCharacter.MAX_VALUE; codepoint ++)
    {
      value = quickcheck[codepoint];
      if ((value & NFD_YES_MASK_) == 0 && (codepoint < result[0]))
        result[0] = codepoint;
      if ((value & NFKD_YES_MASK_) == 0 && (codepoint < result[1]))
        result[1] = codepoint;
      if ((value & NFC_YES_MASK_) == 0 && (codepoint < result[2]))
        result[2] = codepoint;
      if ((value & NFKC_YES_MASK_) == 0 && (codepoint < result[3]))
        result[3] = codepoint;
    }
    return result;
  }
  
  // private data members ------------------------------------------------
  
  /**
  * Input file path
  */
  private final String DEFAULT_INPUT_PATH_ = 
                                 "data//unicode//NormalizationQuickCheck.txt";
  
  /**
  * Output file path
  */
  private final String DEFAULT_OUTPUT_PATH_ = "QuickCheck.txt";
  
  /**
  * 16 bits mask
  */
  private final int MASK_LAST_16_BITS_ = 0xFFFF;
  
  /**
  * Minimum value of a not-a-character after the 16 bit masking
  */
  private final int MIN_NOT_A_CHAR_AFTER_MASK_LAST_16_BITS_ = 0xFFFE;
  
  /**
  * Surrogate code points
  */
  private final int SURROGATE_START_ = 0xD800;
  private final int SURROGATE_END_   = 0xDBFF;

  /**
  * NFD 2 bit mask
  */
  private final byte NFD_NO_MASK_ = (byte)0xEE;
  private final byte NFD_YES_MASK_ = (byte)0x01;
  private final byte NFD_MAYBE_MASK_ = (byte)0x10;

  /**
  * NFKD 2 bit mask
  */
  private final byte NFKD_NO_MASK_ = (byte)0xDD;
  private final byte NFKD_YES_MASK_ = (byte)0x02;
  private final byte NFKD_MAYBE_MASK_ = (byte)0xd0;

  /**
  * NFC 2 bit mask
  */
  private final byte NFC_NO_MASK_ = (byte)0xBB;
  private final byte NFC_YES_MASK_ = (byte)0x04;
  private final byte NFC_MAYBE_MASK_ = (byte)0x40;

  /**
  * NFKC 2 bit mask
  */
  private final byte NFKC_NO_MASK_ = (byte)0x77;
  private final byte NFKC_YES_MASK_ = (byte)0x08;
  private final byte NFKC_MAYBE_MASK_ = (byte)0x80;

  /**
  * Default value for undefined characters
  */
  private final byte UNDEFINED_CHAR_NORM_VALUE_ = (byte)0xF0;

  /**
  * Default value for defined characters
  */
  private final byte DEFINED_CHAR_NORM_VALUE_ = (byte)0x0F;

  /**
  * Default value for not-a-character
  */
  private final byte NOT_A_CHARACTER_VALUE_ = (byte)0x00;
  
  /**
  * NFC name
  */
  private final String NFC_ = "NFC";
  
  /**
  * NFKC name
  */
  private final String NFKC_ = "NFKC";
  
  /**
  * NFD name
  */
  private final String NFD_ = "NFD";
  
  /**
  * NFKD name
  */
  private final String NFKD_ = "NFKD";
  
  /**
  * YES
  */
  private final String YES_ = "Y";
  
  /**
  * NO
  */
  private final String NO_ = "N";
  
  /**
  * MAYBE
  */
  private final String MAYBE_ = "M";
}