/**
*******************************************************************************
* Copyright (C) 1996-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.test.lang;


import com.ibm.icu.dev.test.TestFmwk;

/**
* Testing class for UCharacterIterator
* @author Syn Wee Quek
* @since april 02 2002
*/
public final class UCharacterIteratorTest extends TestFmwk
{ 
    // constructor -----------------------------------------------------
  
    /**
     * Constructor
     */
      public UCharacterIteratorTest()
      {
      }
  
      // public methods --------------------------------------------------
  
      /**
      * Testing cloning
      */
    /*
      public void TestClone()
      {
          UnicodeCharacterIterator iterator = new UnicodeCharacterIterator("testing");
          UnicodeCharacterIterator cloned = (UnicodeCharacterIterator)iterator.clone();
          char completed = 0;
          while (completed != UnicodeCharacterIterator.DONE) {
              completed = iterator.next();
              if (completed != cloned.next()) {
                  errln("Cloned operation failed");
              }
          }
      }
    *
      
      /**
       * Testing iteration
       */
    /*
      public void TestIteration()
      {
          UnicodeCharacterIterator iterator  = new UnicodeCharacterIterator(
                                                         ITERATION_STRING_);
          UnicodeCharacterIterator iterator2 = new UnicodeCharacterIterator(
                                                         ITERATION_STRING_);
          if (iterator.first() != ITERATION_STRING_.charAt(0)) {
              errln("Iterator failed retrieving first character");
          }
          if (iterator.last() != ITERATION_STRING_.charAt(
                                       ITERATION_STRING_.length() - 1)) {
              errln("Iterator failed retrieving last character");
          }                                               
          if (iterator.getBeginIndex() != 0 || 
              iterator.getEndIndex() != ITERATION_STRING_.length()) {
              errln("Iterator failed determining begin and end index");
          }  
          iterator2.setIndex(0);
          iterator.setIndex(0);
          int ch = 0;
          while (ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
              int index = iterator2.getIndex();
              ch = iterator2.nextCodePoint();
              if (index != ITERATION_SUPPLEMENTARY_INDEX) {
                  if (ch != (int)iterator.next() && 
                      ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
                      errln("Error mismatch in next() and nextCodePoint()"); 
                  }
              }
              else {
                  if (UTF16.getLeadSurrogate(ch) != iterator.next() ||
                      UTF16.getTrailSurrogate(ch) != iterator.next()) {
                      errln("Error mismatch in next and nextCodePoint for " +
                            "supplementary characters");
                  }
              }
          }
          iterator.setIndex(ITERATION_STRING_.length());
          iterator2.setIndex(ITERATION_STRING_.length());
          while (ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
              int index = iterator2.getIndex();
              ch = iterator2.previousCodePoint();
              if (index != ITERATION_SUPPLEMENTARY_INDEX) {
                  if (ch != (int)iterator.previous() && 
                      ch != UnicodeCharacterIterator.DONE_CODEPOINT) {
                      errln("Error mismatch in previous() and " +
                            "previousCodePoint()"); 
                  }
              }
              else {
                  if (UTF16.getLeadSurrogate(ch) != iterator.previous() || 
                      UTF16.getTrailSurrogate(ch) != iterator.previous()) {
                      errln("Error mismatch in previous and " +
                            "previousCodePoint for supplementary characters");
                  }
              }
          }
      }
    */
    public static void main(String[] arg)
    {
        try
        {
            UCharacterIteratorTest test = new UCharacterIteratorTest();
            test.run(arg);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    // private data members ---------------------------------------------
    
    private static final String ITERATION_STRING_ =
                                        "Testing 1 2 3 \ud800\udc00 456";
    private static final int ITERATION_SUPPLEMENTARY_INDEX = 14;    
}

