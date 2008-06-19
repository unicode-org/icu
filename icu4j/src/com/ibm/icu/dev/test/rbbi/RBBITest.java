/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;
 
//Regression testing of RuleBasedBreakIterator
//
//  TODO:  These tests should be mostly retired.
//          Much of the test data that was originally here was removed when the RBBI rules
//            were updated to match the Unicode boundary TRs, and the data was found to be invalid.
//          Much of the remaining data has been moved into the rbbitst.txt test data file,
//            which is common between ICU4C and ICU4J.  The remaining test data should also be moved,
//            or simply retired if it is no longer interesting.
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

import java.util.Vector;

public class RBBITest extends TestFmwk 
{  
    
  public static void main(String[] args) throws Exception {
    new RBBITest().run(args);
  }

  public RBBITest() { 
  }
  
  private static final String halfNA = "\u0928\u094d\u200d";  /*halfform NA = devanigiri NA + virama(supresses inherent vowel)+ zero width joiner */  


  // tests default rules based character iteration.
  // Builds a new iterator from the source rules in the default (prebuilt) iterator.
  //
  public void TestDefaultRuleBasedCharacterIteration(){
      RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)BreakIterator.getCharacterInstance();
      logln("Testing the RBBI for character iteration by using default rules");

      //fetch the rules used to create the above RuleBasedBreakIterator
      String defaultRules=rbbi.toString();
      
      RuleBasedBreakIterator charIterDefault=null;
      try{
          charIterDefault   = new RuleBasedBreakIterator(defaultRules); 
      }catch(IllegalArgumentException iae){
          errln("ERROR: failed construction in TestDefaultRuleBasedCharacterIteration()"+ iae.toString());
      }

      Vector chardata = new Vector();
      chardata.addElement("H");
      chardata.addElement("e");
      chardata.addElement("l");
      chardata.addElement("l");
      chardata.addElement("o");
      chardata.addElement("e\u0301");                   //acuteE
      chardata.addElement("&");
      chardata.addElement("e\u0303");                   //tildaE
      //devanagiri characters for Hindi support
      chardata.addElement("\u0906");                    //devanagiri AA
      //chardata.addElement("\u093e\u0901");              //devanagiri vowelsign AA+ chandrabindhu
      chardata.addElement("\u0916\u0947");              //devanagiri KHA+vowelsign E
      chardata.addElement("\u0938\u0941\u0902");        //devanagiri SA+vowelsign U + anusvara(bindu)
      chardata.addElement("\u0926");                    //devanagiri consonant DA
      chardata.addElement("\u0930");                    //devanagiri consonant RA
      // chardata.addElement("\u0939\u094c");              //devanagiri HA+vowel sign AI
      chardata.addElement("\u0964");                    //devanagiri danda
      //end hindi characters      
      chardata.addElement("A\u0302");                   // circumflexA 
      chardata.addElement("i\u0301");                   // acuteBelowI   
      // conjoining jamo... 
      chardata.addElement("\u1109\u1161\u11bc");
      chardata.addElement("\u1112\u1161\u11bc");
      chardata.addElement("\n");
      chardata.addElement("\r\n");                      // keep CRLF sequences together  
      chardata.addElement("S\u0300");                   //graveS
      chardata.addElement("i\u0301");                   // acuteBelowI
      chardata.addElement("!");

       // What follows is a string of Korean characters (I found it in the Yellow Pages
      // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
      // it correctly), first as precomposed syllables, and then as conjoining jamo.
      // Both sequences should be semantically identical and break the same way.
      // precomposed syllables...
      chardata.addElement("\uc0c1");
      chardata.addElement("\ud56d");
      chardata.addElement(" ");
      chardata.addElement("\ud55c");
      chardata.addElement("\uc778");
      chardata.addElement(" ");
      chardata.addElement("\uc5f0");
      chardata.addElement("\ud569");
      chardata.addElement(" ");
      chardata.addElement("\uc7a5");
      chardata.addElement("\ub85c");
      chardata.addElement("\uad50");
      chardata.addElement("\ud68c");
      chardata.addElement(" ");
       // conjoining jamo...
      chardata.addElement("\u1109\u1161\u11bc");
      chardata.addElement("\u1112\u1161\u11bc");
      chardata.addElement(" ");
      chardata.addElement("\u1112\u1161\u11ab");
      chardata.addElement("\u110b\u1175\u11ab");
      chardata.addElement(" ");
      chardata.addElement("\u110b\u1167\u11ab");
      chardata.addElement("\u1112\u1161\u11b8");
      chardata.addElement(" ");
      chardata.addElement("\u110c\u1161\u11bc");
      chardata.addElement("\u1105\u1169");
      chardata.addElement("\u1100\u116d");
      chardata.addElement("\u1112\u116c");


      generalIteratorTest(charIterDefault, chardata);

  }

  public void TestDefaultRuleBasedWordIteration(){
      logln("Testing the RBBI for word iteration using default rules");
      RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)BreakIterator.getWordInstance();
      //fetch the rules used to create the above RuleBasedBreakIterator
      String defaultRules=rbbi.toString();
      
      RuleBasedBreakIterator wordIterDefault=null;
      try{
      wordIterDefault   = new RuleBasedBreakIterator(defaultRules); 
      }catch(IllegalArgumentException iae){
          errln("ERROR: failed construction in TestDefaultRuleBasedWordIteration() -- custom rules"+ iae.toString());
      }

      Vector worddata = new Vector();
      worddata.addElement ("Write");
      worddata.addElement (" ");
      worddata.addElement ("wordrules");
      worddata.addElement (".");
      worddata.addElement(" ");
      //worddata.addElement("alpha-beta-gamma");
      worddata.addElement(" ");      
      worddata.addElement("\u092f\u0939");
      worddata.addElement(" ");
      worddata.addElement("\u0939\u093f" + halfNA + "\u0926\u0940");
      worddata.addElement(" ");
      worddata.addElement("\u0939\u0948");
      //  worddata.addElement("\u0964");   //danda followed by a space
      worddata.addElement(" ");
      worddata.addElement("\u0905\u093e\u092a");
      worddata.addElement(" ");
      worddata.addElement("\u0938\u093f\u0916\u094b\u0917\u0947");
      worddata.addElement("?");
      worddata.addElement(" ");
       worddata.addElement("\r");
      worddata.addElement("It's");
      worddata.addElement(" ");
     // worddata.addElement("$30.10");
      worddata.addElement(" ");  
       worddata.addElement(" ");
      worddata.addElement("Badges");
      worddata.addElement("?");
      worddata.addElement(" ");
      worddata.addElement("BADGES");
      worddata.addElement("!");
      worddata.addElement("1000,233,456.000");
      worddata.addElement(" ");

      generalIteratorTest(wordIterDefault, worddata);
  }
//  private static final String kParagraphSeparator = "\u2029";
  private static final String kLineSeparator      = "\u2028";

  public void TestDefaultRuleBasedSentenceIteration(){
      logln("Testing the RBBI for sentence iteration using default rules");
      RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)BreakIterator.getSentenceInstance();
      
      //fetch the rules used to create the above RuleBasedBreakIterator
      String defaultRules=rbbi.toString();
      RuleBasedBreakIterator sentIterDefault=null;
      try{
          sentIterDefault   = new RuleBasedBreakIterator(defaultRules); 
      }catch(IllegalArgumentException iae){
          errln("ERROR: failed construction in TestDefaultRuleBasedSentenceIteration()" + iae.toString());
      }
      
      Vector sentdata = new Vector();
      sentdata.addElement("(This is it.) ");
      sentdata.addElement("Testing the sentence iterator. ");
      sentdata.addElement("\"This isn\'t it.\" ");
      sentdata.addElement("Hi! ");
      sentdata.addElement("This is a simple sample sentence. ");
      sentdata.addElement("(This is it.) ");
      sentdata.addElement("This is a simple sample sentence. ");
      sentdata.addElement("\"This isn\'t it.\" ");
      sentdata.addElement("Hi! ");
      sentdata.addElement("This is a simple sample sentence. ");
      sentdata.addElement("It does not have to make any sense as you can see. ");
      sentdata.addElement("Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura. ");
      sentdata.addElement("Che la dritta via aveo smarrita. ");
       generalIteratorTest(sentIterDefault, sentdata);
  }
   
  public void TestDefaultRuleBasedLineIteration(){
      logln("Testing the RBBI for line iteration using default rules");
      RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)RuleBasedBreakIterator.getLineInstance();
      //fetch the rules used to create the above RuleBasedBreakIterator
      String defaultRules=rbbi.toString();
      RuleBasedBreakIterator lineIterDefault=null;
      try{
          lineIterDefault   = new RuleBasedBreakIterator(defaultRules); 
      }catch(IllegalArgumentException iae){
          errln("ERROR: failed construction in TestDefaultRuleBasedLineIteration()" + iae.toString());
      }

     Vector linedata = new Vector();
     linedata.addElement("Multi-");
     linedata.addElement("Level ");
     linedata.addElement("example ");
     linedata.addElement("of ");
     linedata.addElement("a ");
     linedata.addElement("semi-");
     linedata.addElement("idiotic ");
     linedata.addElement("non-");
     linedata.addElement("sensical ");
     linedata.addElement("(non-");
     linedata.addElement("important) ");
     linedata.addElement("sentence. ");

     linedata.addElement("Hi  ");
     linedata.addElement("Hello ");
     linedata.addElement("How\n");
     linedata.addElement("are\r");
     linedata.addElement("you" + kLineSeparator);
     linedata.addElement("fine.\t");
     linedata.addElement("good.  ");

     linedata.addElement("Now\r");
     linedata.addElement("is\n");
     linedata.addElement("the\r\n");
     linedata.addElement("time\n");
     linedata.addElement("\r");
     linedata.addElement("for\r");
     linedata.addElement("\r");
     linedata.addElement("all");

     generalIteratorTest(lineIterDefault, linedata);


  }
 
      //=========================================================================
     // general test subroutines
     //=========================================================================

     private void generalIteratorTest(RuleBasedBreakIterator rbbi, Vector expectedResult){
         StringBuffer buffer = new StringBuffer();
         String text;
         for (int i = 0; i < expectedResult.size(); i++) {
             text = (String)expectedResult.elementAt(i);
             buffer.append(text);
         }
         text = buffer.toString();
         if (rbbi == null) {
             errln("null iterator, test skipped.");
             return;
         }

         rbbi.setText(text);

         Vector nextResults = _testFirstAndNext(rbbi, text);
         Vector previousResults = _testLastAndPrevious(rbbi, text);

         logln("comparing forward and backward...");
         int errs = getErrorCount();
         compareFragmentLists("forward iteration", "backward iteration", nextResults,
                         previousResults);
         if (getErrorCount() == errs) {
             logln("comparing expected and actual...");
             compareFragmentLists("expected result", "actual result", expectedResult,
                             nextResults);
         }

        int[] boundaries = new int[expectedResult.size() + 3];
        boundaries[0] = RuleBasedBreakIterator.DONE;
        boundaries[1] = 0;
        for (int i = 0; i < expectedResult.size(); i++)
         boundaries[i + 2] = boundaries[i + 1] + ((String)expectedResult.elementAt(i)).length();
      
        boundaries[boundaries.length - 1] = RuleBasedBreakIterator.DONE;
      
        _testFollowing(rbbi, text, boundaries);
        _testPreceding(rbbi, text, boundaries);
        _testIsBoundary(rbbi, text, boundaries);

        doMultipleSelectionTest(rbbi, text);
     }

     private Vector _testFirstAndNext(RuleBasedBreakIterator rbbi, String text) {
         int p = rbbi.first();
         int lastP = p;
         Vector result = new Vector();

         if (p != 0)
             errln("first() returned " + p + " instead of 0");
         while (p != RuleBasedBreakIterator.DONE) {
             p = rbbi.next();
             if (p != RuleBasedBreakIterator.DONE) {
                 if (p <= lastP)
                     errln("next() failed to move forward: next() on position "
                                     + lastP + " yielded " + p);

                 result.addElement(text.substring(lastP, p));
             }
             else {
                 if (lastP != text.length())
                     errln("next() returned DONE prematurely: offset was "
                                     + lastP + " instead of " + text.length());
             }
             lastP = p;
         }
         return result;
     }

     private Vector _testLastAndPrevious(RuleBasedBreakIterator rbbi, String text) {
         int p = rbbi.last();
         int lastP = p;
         Vector result = new Vector();

         if (p != text.length())
             errln("last() returned " + p + " instead of " + text.length());
         while (p != RuleBasedBreakIterator.DONE) {
             p = rbbi.previous();
             if (p != RuleBasedBreakIterator.DONE) {
                 if (p >= lastP)
                     errln("previous() failed to move backward: previous() on position "
                                     + lastP + " yielded " + p);

                 result.insertElementAt(text.substring(p, lastP), 0);
             }
             else {
                 if (lastP != 0)
                     errln("previous() returned DONE prematurely: offset was "
                                     + lastP + " instead of 0");
             }
             lastP = p;
         }
         return result;
     }

     private void compareFragmentLists(String f1Name, String f2Name, Vector f1, Vector f2) {
         int p1 = 0;
         int p2 = 0;
         String s1;
         String s2;
         int t1 = 0;
         int t2 = 0;

         while (p1 < f1.size() && p2 < f2.size()) {
             s1 = (String)f1.elementAt(p1);
             s2 = (String)f2.elementAt(p2);
             t1 += s1.length();
             t2 += s2.length();

             if (s1.equals(s2)) {
                 debugLogln("   >" + s1 + "<");
                 ++p1;
                 ++p2;
             }
             else {
                 int tempT1 = t1;
                 int tempT2 = t2;
                 int tempP1 = p1;
                 int tempP2 = p2;

                 while (tempT1 != tempT2 && tempP1 < f1.size() && tempP2 < f2.size()) {
                     while (tempT1 < tempT2 && tempP1 < f1.size()) {
                         tempT1 += ((String)f1.elementAt(tempP1)).length();
                         ++tempP1;
                     }
                     while (tempT2 < tempT1 && tempP2 < f2.size()) {
                         tempT2 += ((String)f2.elementAt(tempP2)).length();
                         ++tempP2;
                     }
                 }
                 logln("*** " + f1Name + " has:");
                 while (p1 <= tempP1 && p1 < f1.size()) {
                     s1 = (String)f1.elementAt(p1);
                     t1 += s1.length();
                     debugLogln(" *** >" + s1 + "<");
                     ++p1;
                 }
                 logln("***** " + f2Name + " has:");
                 while (p2 <= tempP2 && p2 < f2.size()) {
                     s2 = (String)f2.elementAt(p2);
                     t2 += s2.length();
                     debugLogln(" ***** >" + s2 + "<");
                     ++p2;
                 }
                 errln("Discrepancy between " + f1Name + " and " + f2Name);
             }
         }
     }

    private void _testFollowing(RuleBasedBreakIterator rbbi, String text, int[] boundaries) {
       logln("testFollowing():");
       int p = 2;
       for(int i = 0; i <= text.length(); i++) {
           if (i == boundaries[p])
               ++p;
           int b = rbbi.following(i);
           logln("rbbi.following(" + i + ") -> " + b);
           if (b != boundaries[p])
               errln("Wrong result from following() for " + i + ": expected " + boundaries[p]
                               + ", got " + b);
       }
   }

   private void _testPreceding(RuleBasedBreakIterator rbbi, String text, int[] boundaries) {
       logln("testPreceding():");
       int p = 0;
       for(int i = 0; i <= text.length(); i++) {
           int b = rbbi.preceding(i);
           logln("rbbi.preceding(" + i + ") -> " + b);
           if (b != boundaries[p])
               errln("Wrong result from preceding() for " + i + ": expected " + boundaries[p]
                              + ", got " + b);
           if (i == boundaries[p + 1])
               ++p;
       }
   }

   private void _testIsBoundary(RuleBasedBreakIterator rbbi, String text, int[] boundaries) {
       logln("testIsBoundary():");
       int p = 1;
       boolean isB;
       for(int i = 0; i <= text.length(); i++) {
           isB = rbbi.isBoundary(i);
           logln("rbbi.isBoundary(" + i + ") -> " + isB);
           if(i == boundaries[p]) {
               if (!isB)
                   errln("Wrong result from isBoundary() for " + i + ": expected true, got false");
               ++p;
           }
           else {
               if(isB)
                   errln("Wrong result from isBoundary() for " + i + ": expected false, got true");
           }
       }
   }
   private void doMultipleSelectionTest(RuleBasedBreakIterator iterator, String testText)
   {
       logln("Multiple selection test...");
       RuleBasedBreakIterator testIterator = (RuleBasedBreakIterator)iterator.clone();
       int offset = iterator.first();
       int testOffset;
       int count = 0;

       do {
           testOffset = testIterator.first();
           testOffset = testIterator.next(count);
           logln("next(" + count + ") -> " + testOffset);
           if (offset != testOffset)
               errln("next(n) and next() not returning consistent results: for step " + count + ", next(n) returned " + testOffset + " and next() had " + offset);

           if (offset != RuleBasedBreakIterator.DONE) {
               count++;
               offset = iterator.next();
           }
       } while (offset != RuleBasedBreakIterator.DONE);

       // now do it backwards...
       offset = iterator.last();
       count = 0;

       do {
           testOffset = testIterator.last();
           testOffset = testIterator.next(count);
           logln("next(" + count + ") -> " + testOffset);
           if (offset != testOffset)
               errln("next(n) and next() not returning consistent results: for step " + count + ", next(n) returned " + testOffset + " and next() had " + offset);

           if (offset != RuleBasedBreakIterator.DONE) {
               count--;
               offset = iterator.previous();
           }
       } while (offset != RuleBasedBreakIterator.DONE);
   }

   private void debugLogln(String s) {
        final String zeros = "0000";
        String temp;
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c < '\u007f')
                out.append(c);
            else {
                out.append("\\u");
                temp = Integer.toHexString((int)c);
                out.append(zeros.substring(0, 4 - temp.length()));
                out.append(temp);
            }
        }
         logln(out.toString());
    }
   
   public void TestThaiDictionaryBreakIterator() {
       int position;
       int index;
       int result[] = { 1, 2, 5, 10, 11, 12, 11, 10, 5, 2, 1, 0 };
       char ctext[] = { 
               0x0041, 0x0020,
               0x0E01, 0x0E32, 0x0E23, 0x0E17, 0x0E14, 0x0E25, 0x0E2D, 0x0E07,
               0x0020, 0x0041
               };
       String text = new String(ctext);
       
       ULocale locale = ULocale.createCanonical("th");
       BreakIterator b = BreakIterator.getWordInstance(locale);
       
       b.setText(text);
       
       index = 0;
       // Test forward iteration
       while ((position = b.next())!= BreakIterator.DONE) {
           if (position != result[index++]) {
               errln("Error with ThaiDictionaryBreakIterator forward iteration test at " + position + ".\nShould have been " + result[index-1]);
           }
       }
       
       // Test backward iteration
       while ((position = b.previous())!= BreakIterator.DONE) {
           if (position != result[index++]) {
               errln("Error with ThaiDictionaryBreakIterator backward iteration test at " + position + ".\nShould have been " + result[index-1]);
           }
       }
       
       //Test invalid sequence and spaces
       char text2[] = {
               0x0E01, 0x0E39, 0x0020, 0x0E01, 0x0E34, 0x0E19, 0x0E01, 0x0E38, 0x0E49, 0x0E07, 0x0020, 0x0E1B, 
               0x0E34, 0x0E49, 0x0E48, 0x0E07, 0x0E2D, 0x0E22, 0x0E39, 0x0E48, 0x0E43, 0x0E19, 
               0x0E16, 0x0E49, 0x0E33
       };
       int expectedWordResult[] = {
               2, 3, 6, 10, 11, 15, 17, 20, 22
       };
       int expectedLineResult[] = {
               3, 6, 11, 15, 17, 20, 22
       };
       BreakIterator brk = BreakIterator.getWordInstance(new ULocale("th"));
       brk.setText(new String(text2));
       position = index = 0;
       while ((position = brk.next()) != BreakIterator.DONE && position < text2.length) {
           if (position != expectedWordResult[index++]) {
               errln("Incorrect break given by thai word break iterator. Expected: " + expectedWordResult[index-1] + " Got: " + position);
           }
       }
      
       brk = BreakIterator.getLineInstance(new ULocale("th"));
       brk.setText(new String(text2));
       position = index = 0;
       while ((position = brk.next()) != BreakIterator.DONE && position < text2.length) {
           if (position != expectedLineResult[index++]) {
               errln("Incorrect break given by thai line break iterator. Expected: " + expectedLineResult[index-1] + " Got: " + position);
           }
       }
   }
  
}
