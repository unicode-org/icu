/**
*******************************************************************************
* Copyright (C) 1996-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.StringTokenizer;
import com.ibm.icu.text.UnicodeSet;

/**
* Testing class for StringTokenizer class
* @author Syn Wee Quek
* @since oct 26 2002
*/
public final class StringTokenizerTest extends TestFmwk
{ 
      // constructor ===================================================
  
      /**
      * Constructor
      */
      public StringTokenizerTest()
      {
      }
  
      // public methods --------------------------------------------------------
    
    /**
     * Testing constructors
     */
    public void TestConstructors()
    {
        String str = "this\tis\na\rstring\ftesting\tStringTokenizer\nconstructors!";
        String delimiter = " \t\n\r\f";
        String expected[] = {"this", "is", "a", "string", "testing", 
                             "StringTokenizer", "constructors!"};
        StringTokenizer defaultst = new StringTokenizer(str);
        StringTokenizer stdelimiter = new StringTokenizer(str, delimiter);
        StringTokenizer stdelimiterreturn = new StringTokenizer(str, delimiter,
                                                                false);
        UnicodeSet delimiterset = new UnicodeSet("[" + delimiter + "]", false);
        StringTokenizer stdelimiterset = new StringTokenizer(str, delimiterset);
        StringTokenizer stdelimitersetreturn = new StringTokenizer(str, 
                                                                delimiterset,
                                                                false);
        for (int i = 0; i < expected.length; i ++) {
            if (!(defaultst.nextElement().equals(expected[i]) 
                  && stdelimiter.nextElement().equals(expected[i])
                  && stdelimiterreturn.nextElement().equals(expected[i])
                  && stdelimiterset.nextElement().equals(expected[i])
                  && stdelimitersetreturn.nextElement().equals(expected[i]))) {
                errln("Constructor with default delimiter gives wrong results");
            }
        }
        
        String expected1[] = {"this", "\t", "is", "\n", "a", "\r", "string", "\f",
                            "testing", "\t", "StringTokenizer", "\n",
                            "constructors!"};
        stdelimiterreturn = new StringTokenizer(str, delimiter, true);
        stdelimitersetreturn = new StringTokenizer(str, delimiterset, true);
        for (int i = 0; i < expected1.length; i ++) {
            if (!(stdelimiterreturn.nextElement().equals(expected1[i])
                  && stdelimitersetreturn.nextElement().equals(expected1[i]))) {
                errln("Constructor with default delimiter and delimiter tokens gives wrong results");
            }
        }
                            
        stdelimiter = new StringTokenizer(str, (String)null);
        stdelimiterreturn = new StringTokenizer(str, (String)null, false);
        delimiterset = null;
        stdelimiterset = new StringTokenizer(str, delimiterset);
        stdelimitersetreturn = new StringTokenizer(str, delimiterset, false);
        
        if (!(stdelimiter.nextElement().equals(str)
              && stdelimiterreturn.nextElement().equals(str)
              && stdelimiterset.nextElement().equals(str)
              && stdelimitersetreturn.nextElement().equals(str))) {
            errln("Constructor with null delimiter gives wrong results");
        }
        
        delimiter = "";
        stdelimiter = new StringTokenizer(str, delimiter);
        stdelimiterreturn = new StringTokenizer(str, delimiter, false);
        delimiterset = new UnicodeSet();
        stdelimiterset = new StringTokenizer(str, delimiterset);
        stdelimitersetreturn = new StringTokenizer(str, delimiterset, false);
        
        if (!(stdelimiter.nextElement().equals(str)
              && stdelimiterreturn.nextElement().equals(str)
              && stdelimiterset.nextElement().equals(str)
              && stdelimitersetreturn.nextElement().equals(str))) {
            errln("Constructor with empty delimiter gives wrong results");
        }
        
        try {
            defaultst = new StringTokenizer(null);
            errln("null string should throw an exception");
        } catch (Exception e) {
        }
        try {
            stdelimiter = new StringTokenizer(null, delimiter);
            errln("null string should throw an exception");
        } catch (Exception e) {
        }
        try {
            stdelimiterreturn = new StringTokenizer(null, delimiter, false);
            errln("null string should throw an exception");
        } catch (Exception e) {
        }
        try {
            stdelimiterset = new StringTokenizer(null, delimiterset);
            errln("null string should throw an exception");
        } catch (Exception e) {
        }
        try {
            stdelimitersetreturn = new StringTokenizer(null, delimiterset,
                                                       false);
            errln("null string should throw an exception");
        } catch (Exception e) {
        }
    }
    
    /**
     * Testing supplementary
     */
    public void TestSupplementary()
    {
        String str = "bmp string \ud800 with a unmatched surrogate character";
        String delimiter = "\ud800\udc00";
        String expected[] = {str};
            
        StringTokenizer tokenizer = new StringTokenizer(str, delimiter);
        if (!tokenizer.nextElement().equals(expected[0])) {
            errln("Error parsing \"" + Utility.hex(str) + "\"");
        }
        if (tokenizer.hasMoreElements()) {
            errln("Number of tokens exceeded expected");
        }
        delimiter = "\ud800";
        String expected1[] = {"bmp string ", 
                              " with a unmatched surrogate character"};
        tokenizer = new StringTokenizer(str, delimiter);
        int i = 0;
        while (tokenizer.hasMoreElements()) {
            if (!tokenizer.nextElement().equals(expected1[i ++])) {
                errln("Error parsing \"" + Utility.hex(str) + "\"");
            }
        }
        if (tokenizer.hasMoreElements()) {
            errln("Number of tokens exceeded expected");
        }
        
        str = "string \ud800\udc00 with supplementary character";
        delimiter = "\ud800";
        String expected2[] = {str};
        tokenizer = new StringTokenizer(str, delimiter);
        if (!tokenizer.nextElement().equals(expected2[0])) {
            errln("Error parsing \"" + Utility.hex(str) + "\"");
        }
        if (tokenizer.hasMoreElements()) {
            errln("Number of tokens exceeded expected");
        }
  
        delimiter = "\ud800\udc00";
        String expected3[] = {"string ", " with supplementary character"};
        tokenizer = new StringTokenizer(str, delimiter);
        i = 0;
        while (tokenizer.hasMoreElements()) {
            if (!tokenizer.nextElement().equals(expected3[i ++])) {
                errln("Error parsing \"" + Utility.hex(str) + "\"");
            }
        }
        if (tokenizer.hasMoreElements()) {
            errln("Number of tokens exceeded expected");
        }
        
        str = "\ud800 \ud800\udc00 \ud800 \ud800\udc00";
        delimiter = "\ud800";
        String expected4[] = {" \ud800\udc00 ", " \ud800\udc00"};
        i = 0;
        while (tokenizer.hasMoreElements()) {
            if (!tokenizer.nextElement().equals(expected4[i ++])) {
                errln("Error parsing \"" + Utility.hex(str) + "\"");
            }
        }
        if (tokenizer.hasMoreElements()) {
            errln("Number of tokens exceeded expected");
        }
        
        delimiter = "\ud800\udc00";
        String expected5[] = {"\ud800 ", " \ud800 "};
        i = 0;
        while (tokenizer.hasMoreElements()) {
            if (!tokenizer.nextElement().equals(expected5[i ++])) {
                errln("Error parsing \"" + Utility.hex(str) + "\"");
            }
        }
        if (tokenizer.hasMoreElements()) {
            errln("Number of tokens exceeded expected");
        }
    }
  
      /**
      * Testing next api
      */
      public void TestNextNonDelimiterToken()
      {
        String str = "  ,  1 2 3  AHHHHH! 5.5 6 7    ,        8\n";
        String expected[] = {",", "1", "2", "3", "AHHHHH!", "5.5", "6", "7", 
                             ",", "8\n"};
        String delimiter = " ";
                           
        StringTokenizer tokenizer = new StringTokenizer(str, delimiter);
        int currtoken = 0;
        while (tokenizer.hasMoreElements()) {
            if (!tokenizer.nextElement().equals(expected[currtoken])) {
                errln("Error token mismatch, expected " + expected[currtoken]);
            }
            currtoken ++;
        }

        if (currtoken != expected.length) {
            errln("Didn't get correct number of tokens");
        }
        
        tokenizer = new StringTokenizer("", delimiter);
        if (tokenizer.hasMoreElements()) {
            errln("Empty string should not have any tokens");
        }
        try {
            tokenizer.nextElement();
            errln("Empty string should not have any tokens");
        } catch (Exception e) {
        }
        
        tokenizer = new StringTokenizer(", ,", ", ");
        if (tokenizer.hasMoreElements()) {
            errln("String with only delimiters should not have any tokens");
        }
        try {
            tokenizer.nextElement();
            errln("String with only delimiters should not have any tokens");
        } catch (Exception e) {
        }

        tokenizer = new StringTokenizer("q, ,", ", ");
        if (!tokenizer.hasMoreElements()) {
            errln("String that does not begin with delimiters should have some tokens");
        }
        if (!tokenizer.nextElement().equals("q")) {
            errln("String that does not begin with delimiters should have some tokens");
        } 
        try {
            tokenizer.nextElement();
            errln("String has only one tokens");
        } catch (Exception e) {
        }

        try {
            tokenizer = new StringTokenizer(null, delimiter);
            errln("StringTokenizer constructed with null source should throw a nullpointerexception");
        } catch (Exception e) {
        }

        tokenizer = new StringTokenizer(str, "q");
        if (!tokenizer.nextElement().equals(str)) {
            errln("Should have received the same string when there are no delimiters");
        }
    }
    
    /**
    * Testing next api
    */
    public void TestNextDelimiterToken()
    {
        String str = "  ,  1 2 3  AHHHHH! 5.5 6 7    ,        8\n";
        String expected[] = {"  ", ",", "  ", "1", " ", "2", " ", "3", "  ",
                             "AHHHHH!", " ", "5.5", " ", "6", " ", "7", "    ",
                             ",", "        ", "8\n"};
        String delimiter = " ";
                           
        StringTokenizer tokenizer = new StringTokenizer(str, delimiter, true);
        int currtoken = 0;
        while (tokenizer.hasMoreElements()) {
            if (!tokenizer.nextElement().equals(expected[currtoken])) {
                errln("Error token mismatch, expected " + expected[currtoken]);
            }
            currtoken ++;
        }

        if (currtoken != expected.length) {
            errln("Didn't get correct number of tokens");
        }
        
        tokenizer = new StringTokenizer("", delimiter, true);
        if (tokenizer.hasMoreElements()) {
            errln("Empty string should not have any tokens");
        }
        try {
            tokenizer.nextElement();
            errln("Empty string should not have any tokens");
        } catch (Exception e) {
        }
        
        tokenizer = new StringTokenizer(", ,", ", ", true);
        if (!tokenizer.hasMoreElements()) {
            errln("String with only delimiters should have tokens when delimiter is treated as tokens");
        }
        if (!tokenizer.nextElement().equals(", ,")) {
            errln("String with only delimiters should return itself when delimiter is treated as tokens");
        }

        tokenizer = new StringTokenizer("q, ,", ", ", true);
        
        if (!tokenizer.hasMoreElements()) {
            errln("String should have some tokens");
        }
        if (!tokenizer.nextElement().equals("q") 
            || !tokenizer.nextElement().equals(", ,")) {
            errln("String tokens do not match expected results");
        } 

        try {
            tokenizer = new StringTokenizer(null, delimiter, true);
            errln("StringTokenizer constructed with null source should throw a nullpointerexception");
        } catch (Exception e) {
        }

        tokenizer = new StringTokenizer(str, "q", true);
        if (!tokenizer.nextElement().equals(str)) {
            errln("Should have recieved the same string when there are no delimiters");
        }
    }
    
    /**
     * Testing count tokens
     */
    public void TestCountTokens()
    {
        String str = "this\tis\na\rstring\ftesting\tStringTokenizer\nconstructors!";
        String delimiter = " \t\n\r\f";
        String expected[] = {"this", "is", "a", "string", "testing", 
                             "StringTokenizer", "constructors!"};
        String expectedreturn[] = {"this", "\t", "is", "\n", "a", "\r", 
                                   "string", "\f", "testing", "\t", 
                                   "StringTokenizer", "\n", "constructors!"};
        StringTokenizer st = new StringTokenizer(str, delimiter);
        StringTokenizer streturn = new StringTokenizer(str, delimiter, true);
        if (st.countTokens() != expected.length) {
            errln("CountTokens failed for non-delimiter tokens");
        }
        if (streturn.countTokens() != expectedreturn.length) {
            errln("CountTokens failed for delimiter tokens");
        }
        for (int i = 0; i < expected.length; i ++) {
            if (!st.nextElement().equals(expected[i])
                || st.countTokens() != expected.length - i - 1) {
                errln("CountTokens default delimiter gives wrong results");
            }
        }
        for (int i = 0; i < expectedreturn.length; i ++) {
            if (!streturn.nextElement().equals(expectedreturn[i])
                || streturn.countTokens() != expectedreturn.length - i - 1) {
                errln("CountTokens with default delimiter and delimiter tokens gives wrong results");
            }
        }    
    }
        
    /**
     * Next token with new delimiters
     */
    public void TestNextNewDelimiters()
    {
        String str = "abc0def1ghi2jkl3mno4pqr0stu1vwx2yza3bcd4efg0hij1klm2nop3qrs4tuv";
        String delimiter[] = {"0", "1", "2", "3", "4"};
        String expected[][] = {{"abc", "pqr", "efg"},
                               {"def", "stu", "hij"},
                               {"ghi", "vwx", "klm"},
                               {"jkl", "yza", "nop"},
                               {"mno", "bcd", "qrs"}
                              };
        StringTokenizer st = new StringTokenizer(str);
        int size = expected[0].length;
        for (int i = 0; i < size; i ++) {
            for (int j = 0; j < delimiter.length; j ++) {
                if (!st.nextToken(delimiter[j]).equals(expected[j][i])) {
                    errln("nextToken() with delimiters error " + i + " " + j);
                }
                if (st.countTokens() != expected[j].length - i) {            
                    errln("countTokens() after nextToken() with delimiters error"
                          + i + " " + j);
                }
            }
        }    
        st = new StringTokenizer(str);
        String delimiter1[] = {"0", "2", "4"};
        String expected1[] = {"abc", "def1ghi", "jkl3mno", "pqr", "stu1vwx", 
                              "yza3bcd", "efg", "hij1klm", "nop3qrs", "tuv"};
        for (int i = 0; i < expected1.length; i ++) {
            if (!st.nextToken(delimiter1[i % delimiter1.length]).equals(
                                                            expected1[i])) {
                errln("nextToken() with delimiters error " + i);
            }
        }
    }
    
    public static void main(String[] arg)
    {
        try
        {
            StringTokenizerTest test = new StringTokenizerTest();
            test.run(arg);
            // test.TestCaseCompare();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}

