/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;
 
//Regression testing of RuleBasedBreakIterator
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator_Old;
import java.util.Vector;

public class RBBITest extends TestFmwk 
{  
  private RuleBasedBreakIterator characterBreak;
  private RuleBasedBreakIterator wordBreak;
  private RuleBasedBreakIterator lineBreak;
  private RuleBasedBreakIterator sentenceBreak;
    
  public static void main(String[] args) throws Exception {
    new RBBITest().run(args);
  }

  public RBBITest() { 
  }
  
   private static final String halfNA = "\u0928\u094d\u200d";  /*halfform NA = devanigiri NA + virama(supresses inherent vowel)+ zero width joiner */  
  private static final String halfSA  = "\u0938\u094d\u200d"; 
  private static final String halfCHA = "\u091a\u094d\u200d"; 
  private static final String halfKA  = "\u0915\u094d\u200d"; 
  private static final String deadTA  = "\u0924\u094d";

  private static final String deadRA   = "\u0930\u094d";  /*deadform RA = devanagari RA + virama*/
  private static final String deadPHA  = "\u092b\u094d";  /*deadform PHA = devanagari PHA + virama*/
  private static final String deadTTHA = "\u0920\u094d"; 
  private static final String deadPA   = "\u092a\u094d"; 
  private static final String deadSA   = "\u0938\u094d";   /*devanagari visarga looks like a english colon*/
  private static final String visarga  = "\u0903";


  //tests default rules based character iteration
  public void TestDefaultRuleBasedCharacterIteration(){
      RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)RuleBasedBreakIterator.getCharacterInstance();
      logln("Testing the RBBI for character iteration by using default rules");
      try {
          RuleBasedBreakIterator_Old obi = (RuleBasedBreakIterator_Old)rbbi;
      }
      catch (ClassCastException e) {
          // Bail out if using new RBBI implementation
          logln("Test Skipped.");
          return;
      }

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
      chardata.addElement("\u0939\u094c");              //devanagiri HA+vowel sign AI
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

      //Testing the BreakIterator for devanagari script
      //devanagari characters for Hindi support
      chardata.addElement("\u0906");                    //devanagari AA
      //hindi character break should make sure that it 
      // doesn't break in-between a vowelsign and a chandrabindu
    //  chardata.addElement("\u093e\u0901");              //devanagari vowelsign AA+ chandrabindu
      chardata.addElement("\u0916\u0947");              //devanagari KHA+vowelsign E
      chardata.addElement("\u0938\u0941\u0902");        //devanagari SA+vowelsign U + anusvara(bindu)
      chardata.addElement("\u0926");                    //devanagari consonant DA
      chardata.addElement("\u0930");                    //devanagari consonant RA
      chardata.addElement("\u0939\u094c");              //devanagari consonant HA+dependent vowel sign AI
      chardata.addElement("\u0964");                    //devanagari danda
      chardata.addElement("\u0950");                    //devanagari OM 
      chardata.addElement("\u0915\u0943");              //devanagari KA+dependent vowel RI->KRI

      //dependent half-forms
      chardata.addElement(halfSA+ "\u0924");             //halfSA+base consonant TA->STA
      chardata.addElement(halfSA+ "\u0925");             //halfSA+base consonant THA->STHA
      chardata.addElement(halfSA+ "\u092e");             //halfSA+base consonant MA->SMA
      chardata.addElement(halfCHA+ "\u091b");            //halfCHA+base consonant CHHA->CHHHA
      chardata.addElement(halfNA+ "\u0917");             //halfNA+base consonant GA->NGA
      chardata.addElement("\u092a\u094d\u200d\u092f");   //halfPA(PA+virama+zerowidthjoiner+base consonant YA->PYA


      //consonant RA rules ----------
      //if the dead consonant RA precedes either a consonant or an independent vowel,
      //then it is replaced by its superscript non-spacing mark
      chardata.addElement(deadRA+ "\u0915");             //deadRA+devanagari consonant KA->KA+superRA 
      chardata.addElement(deadRA+ "\u0923");             //deadRA+devanagari consonant NNA->NNA+superRA
      chardata.addElement(deadRA+ "\u0917");             //deadRA+devanagari consonant GA->GA+superRA
   //   chardata.addElement(deadRA+ "\u0960");           //deadRA+devanagari cosonant RRI->RRI+superRA
      
      //if any dead consonant(other than dead RA)precedes the consonant RA, then
      //it is replaced with its nominal forma nd RA is replaced by the subscript non-spacing mark.
      chardata.addElement(deadPHA+ "\u0930");            //deadPHA+devanagari consonant RA->PHA+subRA
      chardata.addElement(deadPA+ "\u0930");             //deadPA+devanagari consonant RA->PA+subRA
      chardata.addElement(deadTTHA+ "\u0930");           //deadTTHA+devanagari consonant RA->TTHA+subRA
      chardata.addElement(deadTA+ "\u0930");             //deadTA+RA->TRA 
      chardata.addElement("\u0936\u094d\u0930");         //deadSHA(SHA+virama)+RA->SHRA 

      //conjuct ligatures
      chardata.addElement("\u0915\u094d\u0937");         //deadKA(KA+virama) followed by SSHA wraps up into a single character KSSHA
      chardata.addElement(deadTA+ "\u0924");              //deadTA+TA wraps up into glyph TTHA
      chardata.addElement("\u0926\u094d\u0935");         //deadDA(DA+virama)+VA wraps up into DVA
      chardata.addElement("\u091c\u094d\u091e");         //deadJA(JA+virama)+NYA wraps up into JNYA
     


      generalIteratorTest(charIterDefault, chardata);

  }

  public void TestDefaultRuleBasedWordIteration(){
      logln("Testing the RBBI for word iteration using default rules");
      RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)RuleBasedBreakIterator.getWordInstance();
      try {
          RuleBasedBreakIterator_Old obi = (RuleBasedBreakIterator_Old)rbbi;
      }
      catch (ClassCastException e) {
          // Bail out if using new RBBI implementation
          logln("Test Skipped.");
          return;
      }
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
      worddata.addElement("alpha-beta-gamma");
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
      worddata.addElement("\u0968\u0966.\u0969\u096f");            //hindi numbers
      worddata.addElement(" ");
      worddata.addElement("\u0967\u0966\u0966.\u0966\u0966%");     //postnumeric
      worddata.addElement(" ");
      worddata.addElement("\u20a8\u0967,\u0967\u0966\u0966.\u0966\u0966"); //pre-number India currency symbol Rs.\u20aD
      worddata.addElement(" ");
      worddata.addElement("\u0905\u092e\u091c");
      worddata.addElement("\n");
      worddata.addElement(halfSA+"\u0935\u0924\u0902"+deadTA+"\u0930");
      worddata.addElement("\r");
      worddata.addElement("It's");
      worddata.addElement(" ");
      worddata.addElement("$30.10");
      worddata.addElement(" ");  
      worddata.addElement("\u00A2"); //cent sign
      worddata.addElement("\u00A3"); //pound sign
      worddata.addElement("\u00A4"); //currency sign
      worddata.addElement("\u00A5"); //yen sign
      worddata.addElement("alpha-beta-gamma");
      worddata.addElement(" ");
      worddata.addElement("Badges");
      worddata.addElement("?");
      worddata.addElement(" ");
      worddata.addElement("BADGES");
      worddata.addElement("!");
      worddata.addElement("1000,233,456.000");
      worddata.addElement(" ");
      worddata.addElement("1,23.322%");
      worddata.addElement(" ");
      worddata.addElement("123.1222");
      worddata.addElement(" ");
      worddata.addElement("\u0024123,000.20");
      worddata.addElement(" ");
      worddata.addElement("179.01\u0025");
      worddata.addElement("X");
      worddata.addElement(" ");
      worddata.addElement("Now");
      worddata.addElement("\r");
      worddata.addElement("is");
      worddata.addElement("\n");
      worddata.addElement("the");
      worddata.addElement("\r\n");
      worddata.addElement("time");
      worddata.addElement(" ");
      worddata.addElement("\uc5f0\ud569");
      worddata.addElement(" ");
      worddata.addElement("\uc7a5\ub85c\uad50\ud68c");
      worddata.addElement(" ");
      // conjoining jamo...
      worddata.addElement("\u1109\u1161\u11bc\u1112\u1161\u11bc");
      worddata.addElement(" ");
      worddata.addElement("\u1112\u1161\u11ab\u110b\u1175\u11ab");
      worddata.addElement(" ");
      worddata.addElement("Hello");
      worddata.addElement(",");
      worddata.addElement(" ");
      worddata.addElement("how");
      worddata.addElement(" ");
      worddata.addElement("are");
      worddata.addElement(" ");
      worddata.addElement("you");
      worddata.addElement(" ");

      //Testing the BreakIterator for devanagari script
      //hindi
      worddata.addElement("\u0917\u092a-\u0936\u092a");
      worddata.addElement("!");
      worddata.addElement("\u092f\u0939");
      worddata.addElement(" ");
      worddata.addElement("\u0939\u093f" + halfNA + "\u0926\u0940");
      worddata.addElement(" ");
      worddata.addElement("\u0939\u0948");
      //danda is similar to full stop. danda is a hindi phrase seperator
      //Make sure it breaks before danda and after danda when it is followed by a space
      //worddata.addElement("\u0964");   //fails here doesn't break at danda
      worddata.addElement(" ");
      worddata.addElement("\u0905\u093e\u092a");
      worddata.addElement(" ");
      worddata.addElement("\u0938\u093f\u0916\u094b\u0917\u0947");
      worddata.addElement("?");
      worddata.addElement("\n"); 
      worddata.addElement(":");
      worddata.addElement(deadPA+"\u0930\u093e\u092f"+visarga);    //no break before visarga
      worddata.addElement(" ");

      worddata.addElement("\u0935" + deadRA+ "\u0937\u093e");
      worddata.addElement("\r\n");
      worddata.addElement(deadPA+ "\u0930\u0915\u093e\u0936");     //deadPA+RA+KA+vowel AA+SHA -> prakash
      worddata.addElement(","); 
      worddata.addElement("\u0924\u0941\u092e\u093e\u0930\u094b"); 
      worddata.addElement(" ");
      worddata.addElement("\u092e\u093f"+ deadTA+ "\u0930");       //MA+vowel I+ deadTA + RA 
      worddata.addElement(" ");
      worddata.addElement("\u0915\u093e");
      worddata.addElement(" ");
      worddata.addElement("\u092a"+ deadTA + "\u0930");            //PA + deadTA + RA
      worddata.addElement(" ");
      worddata.addElement("\u092a\u095d\u094b");
      // worddata.addElement("\u0964"); //fails here doesn't break at danda
      worddata.addElement(" ");
      worddata.addElement(deadSA + deadTA + "\u0930\u093f");       //deadSA+deadTA+RA+vowel I->sthri
      worddata.addElement(".");
      worddata.addElement(" ");
      worddata.addElement("\u0968\u0966.\u0969\u096f");            //hindi numbers
      worddata.addElement(" ");
      worddata.addElement("\u0967\u0966\u0966.\u0966\u0966%");     //postnumeric
      worddata.addElement(" ");
      worddata.addElement("\u20a8\u0967,\u0967\u0966\u0966.\u0966\u0966"); //pre-number India currency symbol Rs.\u20aD
      worddata.addElement(" ");
      worddata.addElement("\u0905\u092e\u091c");
      worddata.addElement("\n");
      worddata.addElement(halfSA + "\u0935\u0924\u0902" +deadTA+ "\u0930");
      worddata.addElement("\r");

      generalIteratorTest(wordIterDefault, worddata);
  }
  private static final String kParagraphSeparator = "\u2029";
  private static final String kLineSeparator      = "\u2028";

  public void TestDefaultRuleBasedSentenceIteration(){
      logln("Testing the RBBI for sentence iteration using default rules");
      RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)RuleBasedBreakIterator.getSentenceInstance();
      //fetch the rules used to create the above RuleBasedBreakIterator
      try {
          RuleBasedBreakIterator_Old obi = (RuleBasedBreakIterator_Old)rbbi;
      }
      catch (ClassCastException e) {
          // Bail out if using new RBBI implementation
          logln("Test Skipped.");
          return;
      }
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
      sentdata.addElement("He said, that I said, that you said!! ");
      sentdata.addElement("Don't rock the boat." + kParagraphSeparator);
      sentdata.addElement("Because I am the daddy, that is why. ");
      sentdata.addElement("Not on my time (el timo.)! ");
      sentdata.addElement("So what!!" + kParagraphSeparator);
      sentdata.addElement("\"But now,\" he said, \"I know!\" ");
      sentdata.addElement("Harris thumbed down several, including \"Away We Go\" (which became the huge success Oklahoma!). ");
      sentdata.addElement("One species, B. anthracis, is highly virulent.\n");
      sentdata.addElement("Wolf said about Sounder:\"Beautifully thought-out and directed.\" ");
      sentdata.addElement("Have you ever said, \"This is where\tI shall live\"? ");
      sentdata.addElement("He answered, \"You may not!\" ");
      sentdata.addElement("Another popular saying is: \"How do you do?\". ");
      sentdata.addElement("Yet another popular saying is: \'I\'m fine thanks.\' ");
      sentdata.addElement("What is the proper use of the abbreviation pp.? ");
      sentdata.addElement("Yes, I am definatelly 12\" tall!!");
      sentdata.addElement("Now\ris\nthe\r\ntime\n\rfor\r\rall\u2029");
      // Don't break sentences at the boundary between CJK
      // and other letters
      sentdata.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165:\"JAVA\u821c"
                + "\u8165\u7fc8\u51ce\u306d,\u2494\u56d8\u4ec0\u60b1\u8560\u51ba"
                + "\u611d\u57b6\u2510\u5d46\".\u2029");
      sentdata.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8"
                + "\u97e4JAVA\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8\u4ec0"
                + "\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");
      sentdata.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8\u97e4"
                + "\u6470\u8790JAVA\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8"
                + "\u4ec0\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");
      sentdata.addElement("He said, \"I can go there.\"\u2029");
      // Treat fullwidth variants of .!? the same as their
      // normal counterparts
      sentdata.addElement("I know I'm right\uff0e ");
      sentdata.addElement("Right\uff1f ");
      sentdata.addElement("Right\uff01 ");

      // Don't break sentences at boundary between CJK and digits
      sentdata.addElement("\u5487\u67ff\ue591\u5017\u61b3\u60a1\u9510\u8165\u9de8"
                + "\u97e48888\u821c\u8165\u7fc8\u51ce\u306d\ue30b\u2494\u56d8\u4ec0"
                + "\u60b1\u8560\u51ba\u611d\u57b6\u2510\u5d46\u97e5\u7751\u2029");

      // Break sentence between a sentence terminator and
      // opening punctuation
      sentdata.addElement("How do you do?");
      sentdata.addElement("(fine).");
      //sentence breaks for hindi
      //make sure there is sentence break after ?,danda(hindi phrase separator),fullstop followed by space and no break after \n \r 
      sentdata.addElement("\u0928\u092e"+halfSA+"\u0924\u0947 "  
                                   + "\u0930\u092e\u0947\u0936, " 
                                   + "\u0905\u093e\u092a"
                                   + "\u0915\u0948\u0938\u0947 "
                                   + "\u0939\u0948?");   
      sentdata.addElement("\u092e\u0948 \u0905"
                        + halfCHA
                        + "\u091b\u093e \u0939\u0942\u0901\u0964 ");   
      sentdata.addElement("\u0905\u093e\u092a\r\n \u0915\u0948\u0938\u0947 \u0939\u0948?");   
      sentdata.addElement("\u0935\u0939 "
                       + halfKA
                       + "\u092f\u093e\n \u0939\u0948?");   
      sentdata.addElement("\u092f\u0939 \u0905\u093e\u092e \u0939\u0948. ");   
      sentdata.addElement("\u092f\u0939 means \"this\". "); 
      sentdata.addElement("\"\u092a\u095d\u093e\u0908\" meaning \"education\" or \"studies\". ");
      sentdata.addElement("\u0905\u093e\u091c" 
                        + "("+halfSA+"\u0935\u0924\u0902"
                        + deadTA
                        + "\u0930 "
                        + "\u0926\u093f\u0935\u093e\u0938) "
                        + "\u0939\u0948\u0964 ");
      sentdata.addElement("Let's end here. ");
      generalIteratorTest(sentIterDefault, sentdata);
  }
   
  public void TestDefaultRuleBasedLineIteration(){
      logln("Testing the RBBI for line iteration using default rules");
      RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)RuleBasedBreakIterator.getLineInstance();
      //fetch the rules used to create the above RuleBasedBreakIterator
      try {
          RuleBasedBreakIterator_Old obi = (RuleBasedBreakIterator_Old)rbbi;
      }
      catch (ClassCastException e) {
          // Bail out if using new RBBI implementation
          logln("Test Skipped.");
          return;
      }
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

    // to test for bug #4068133
     linedata.addElement("\u96f6");
     linedata.addElement("\u4e00\u3002");
     linedata.addElement("\u4e8c\u3001");
     linedata.addElement("\u4e09\u3002\u3001");
     linedata.addElement("\u56db\u3001\u3002\u3001");
     linedata.addElement("\u4e94,");
     linedata.addElement("\u516d.");
     linedata.addElement("\u4e03.\u3001,\u3002");
     linedata.addElement("\u516b");

    // to test for bug #4086052
     linedata.addElement("foo\u00a0bar ");
    // linedata.addElement("foo\ufeffbar");

    // to test for bug #4097920
     linedata.addElement("dog,cat,mouse ");
     linedata.addElement("(one)");
     linedata.addElement("(two)\n");

    // to test for bug #4035266
     linedata.addElement("The ");
     linedata.addElement("balance ");
     linedata.addElement("is ");
     linedata.addElement("$-23,456.78, ");
     linedata.addElement("not ");
     linedata.addElement("-$32,456.78!\n");

    // to test for bug #4098467
    // What follows is a string of Korean characters (I found it in the Yellow Pages
    // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
    // it correctly), first as precomposed syllables, and then as conjoining jamo.
    // Both sequences should be semantically identical and break the same way.
    // precomposed syllables...
     linedata.addElement("\uc0c1\ud56d ");
     linedata.addElement("\ud55c\uc778 ");
     linedata.addElement("\uc5f0\ud569 ");
     linedata.addElement("\uc7a5\ub85c\uad50\ud68c ");
    // conjoining jamo...
     linedata.addElement("\u1109\u1161\u11bc\u1112\u1161\u11bc ");
     linedata.addElement("\u1112\u1161\u11ab\u110b\u1175\u11ab ");
     linedata.addElement("\u110b\u1167\u11ab\u1112\u1161\u11b8 ");
     linedata.addElement("\u110c\u1161\u11bc\u1105\u1169\u1100\u116d\u1112\u116c");

    // to test for bug #4117554: Fullwidth .!? should be treated as postJwrd
     linedata.addElement("\u4e01\uff0e");
     linedata.addElement("\u4e02\uff01");
     linedata.addElement("\u4e03\uff1f");
     generalIteratorTest(lineIterDefault, linedata);


  }
  public void TestCustomRuleBasedLineIterator(){

    RuleBasedBreakIterator rbbi=(RuleBasedBreakIterator)RuleBasedBreakIterator.getLineInstance();
    //fetch the rules used to create the above RuleBasedBreakIterator
    String rules=//"$_ignore_=[e];"  + // [liu] Not sure what the intention was here.
        // It's illegal to redefine variables, also, why prepend the ignore definition --
        // even if it's legal to redefine variables, the prepended definition will just
        // get overridden.
                 rbbi.toString();
    RuleBasedBreakIterator lineIter=null;
    try {
        RuleBasedBreakIterator_Old obi = (RuleBasedBreakIterator_Old)rbbi;
    }
    catch (ClassCastException e) {
        // Bail out if using new RBBI implementation
        logln("Test Skipped.");
        return;
    }

    try{
     lineIter   = new RuleBasedBreakIterator(rules); 
       }catch(IllegalArgumentException iae){
         errln("ERROR: failed construction in TestCustomRuleBasedLineIterator() -- custom rules\n" + iae.toString());
       }
    Vector linedata=new Vector();
    linedata.addElement("SLhello! ");
    linedata.addElement("How ");
    generalIteratorTest(lineIter, linedata);
  }
  
  //tests the behaviour of character iteration of RBBI with custom rules
  public void TestCustomRuleBasedCharacterIteration(){
      logln("Testing the RBBI by using custom rules for character iteration");

      String crules2="$_ignore_=[e];"                                 + //ignore the character "e"
                     ".;"                                            + 
                     "$devVowelSign=[\u093e-\u094c\u0962\u0963];"   +  //devanagiri vowel = \u093e tp \u094c and \u0962.\u0963
                     "$devConsonant=[\u0915-\u0939];"               +  //devanagiri consonant = \u0915 to \u0939
                     "$devConsonant$devVowelSign;" ;                //break at all places except the  following 
                                                                            //devanagiri consonants+ devanagiri vowelsign
      
      RuleBasedBreakIterator charIterRules=null;
      try{
      charIterRules   = new RuleBasedBreakIterator(crules2); 
      }catch(IllegalArgumentException iae){
          errln("ERROR: failed construction in TestCustomBasedCharacterIteration()-- custom rules" + iae.toString());
      }

      Vector chardata = new Vector();
      chardata.addElement("He");              //ignores 'e'
      chardata.addElement("l");                
      chardata.addElement("l");
      chardata.addElement("oe");              //ignores 'e' hence wraps it into 'o' instead of wrapping with
      chardata.addElement("\u0301");          //'\u0301' to form 'acuteE '
      chardata.addElement("&e");              //ignores 'e' hence wraps it into '&' instead of wrapping with
      chardata.addElement("\u0303");          //'\u0303 to form 'tildaE'
      //devanagiri characters 
      chardata.addElement("\u0906");          //devanagiri AA
      chardata.addElement("\u093e");          //devanagiri vowelsign AA:--breaks at \u0901 which is devanagiri 
      chardata.addElement("\u0901");          //chandra bindhu since it is not mentioned in the rules
      chardata.addElement("\u0916\u0947");    //devanagiri KHA+vowelsign E
      chardata.addElement("\u0938\u0941");    //devanagiri SA+vowelsign U : - breaks at
      chardata.addElement("\u0902");          //\u0902 devanagiri anusvara since it is not mentioned in the rules
      chardata.addElement("\u0926");          //devanagiri consonant DA
      chardata.addElement("\u0930");          //devanagiri consonant RA
      chardata.addElement("\u0939\u094c");    //devanagiri HA+vowel sign AI
      chardata.addElement("\u0964");          //devanagiri danda
      // devanagiri chracters end
      chardata.addElement("A");               //breaks in between since it is not mentioned in the rules
      chardata.addElement("\u0302");          /* circumflexA */    
      chardata.addElement("i");               //breaks in between since not mentioned in the rules
      chardata.addElement("\u0301");          /* acuteBelowI */  
      //Rules don't support conjoining jamo....
      chardata.addElement("\u1109");          //break at every character since rules
      chardata.addElement("\u1161");          //don't support conjoining jamo
      chardata.addElement("\u11bc");
      chardata.addElement("\u1112");
      chardata.addElement("\u1161");
      chardata.addElement("\u11bc");
      chardata.addElement("\n");
      chardata.addElement("\r");             //doesn't keep CRLGF together since rules do not mention it
      chardata.addElement("\n");
      chardata.addElement("S");              //graveS
      chardata.addElement("\u0300");         //breaks in between since it is not mentioned in the rules
      chardata.addElement("i");              //acuteBelowI
      chardata.addElement("\u0301");         //breaks in between since it is not mentioned in the rules

      generalIteratorTest(charIterRules, chardata);

  }    
  //tests custom rules based word iteration
  public void TestCustomRuleBasedWordIteration(){
      logln("Testing the RBBI by using custom rules for word iteration");
      String wrules1="$_ignore_=[[:Mn:][:Me:][:Cf:]];"         + // ignore non-spacing marks, enclosing marks, and format characters,
                      "$danda=[\u0964\u0965];"                + // Hindi Phrase seperator
                      "$let=[[:L:][:Mc:]];"                   + // uppercase(Lu), lowercase(Ll), titlecase(Lt), modifier(Lm) letters, Mc-combining space mark
                      "$mid_word=[[:Pd:]\\\"\\\'\\.];"        + // dashes, quotation, apostraphes, period
                      "$ls=[\n\u000c\u2028\u2029];"           + // line separators:  LF, FF, PS, and LS
                      "$ws=[[:Zs:]\t];"                       + // all space separators and the tab character
                      "$word=(($let+($mid_word$let+)*));"  +  
                      ".;"                                   + // break after every character, with the following exceptions
                      "$word;"                              + 
                      "$ws*\r$ls$danda?;" ;

      RuleBasedBreakIterator wordIterRules=null;
      try{
      wordIterRules   = new RuleBasedBreakIterator(wrules1); 
      }catch(IllegalArgumentException iae){
          errln("ERROR: failed construction in TestCustomRuleBasedWordIteration() -- custom rules" + iae.toString());
      }

      Vector worddata = new Vector();
      worddata.addElement("Write");
      worddata.addElement(" ");
      worddata.addElement("wordrules");
      worddata.addElement(".");
      worddata.addElement(" ");
      //play with hindi
      worddata.addElement("\u092f\u0939");
      worddata.addElement(" ");
      worddata.addElement("\u0939\u093f" + halfNA + "\u0926\u0940");
      worddata.addElement(" ");
      worddata.addElement("\u0939\u0948");
      worddata.addElement("\u0964");   //Danda is similar to full stop. Danda followed by a space
      worddata.addElement(" ");
      worddata.addElement("\u0905\u093e\u092a");
      worddata.addElement(" ");
      worddata.addElement("\u0938\u093f\u0916\u094b\u0917\u0947");
      worddata.addElement("?");
      worddata.addElement(" ");
      worddata.addElement("It's");
      worddata.addElement(" ");
      worddata.addElement("$");
      worddata.addElement("3");
      worddata.addElement("0");
      worddata.addElement(".");
      worddata.addElement("1");
      worddata.addElement("0");
      worddata.addElement(" ");
      // worddata.addElement(" ");
      generalIteratorTest(wordIterRules, worddata); 
  } 
  //adds extra rules to deal with abbrevations(limited) and test the word Iteration
  public void TestAbbrRuleBasedWordIteration(){
      logln("Testing the RBBI for word iteration by adding rules to support abbreviation");
      RuleBasedBreakIterator rb =(RuleBasedBreakIterator)RuleBasedBreakIterator.getWordInstance();
      try {
        // This test won't work with the new break iterators.  Cast will fail in this case.
         RuleBasedBreakIterator_Old  obi =  (RuleBasedBreakIterator_Old) rb;
      }
      catch (ClassCastException e) {
          logln("Test skipped.");
          return;
      }
       
      String wrules2="$abbr=((Mr.)|(Mrs.)|(Ms.)|(Dr.)|(U.S.));" + // abbreviations. 
                     rb.toString()                             +
                     "($abbr$ws)*$word;";
      RuleBasedBreakIterator wordIter=null;
      try{
      wordIter   = new RuleBasedBreakIterator(wrules2); 
      }catch(IllegalArgumentException iae){
          errln("ERROR: failed construction in TestAbbrRuleBasedWordIteration() --custom rules" + iae.toString());
      }
      Vector worddata = new Vector();
      worddata.addElement("Mr. George");
      worddata.addElement(" ");
      worddata.addElement("is");
      worddata.addElement(" ");
      worddata.addElement("from");
      worddata.addElement(" ");
      worddata.addElement("U.S. Navy");
      worddata.addElement(".");
      worddata.addElement(" ");
      worddata.addElement("His");
      worddata.addElement("\n");
      worddata.addElement("friend");
      worddata.addElement("\t");
      worddata.addElement("Dr. Steven");
      worddata.addElement(" ");
      worddata.addElement("married");
      worddata.addElement(" ");
      worddata.addElement("Ms. Benneth");
      worddata.addElement("!");
      worddata.addElement(" ");
      worddata.addElement("Mrs. Johnson");
      worddata.addElement("\r\n");
      worddata.addElement("paid");
      worddata.addElement(" ");
      worddata.addElement("$2,400.00");
      generalIteratorTest(wordIter, worddata); 
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
  
}
