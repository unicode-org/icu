/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1999-2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Date        Name        Description
*   12/15/99    Madhu        Creation.
*   01/12/2000  Madhu        Updated for changed API and added new tests
************************************************************************/


#include "intltest.h"
#include "unicode/brkiter.h"
#include "unicode/rbbi.h"
#include "unicode/unicode.h"
#include <stdio.h>
#include "unicode/utypes.h"
#include "rbbitst.h"
#include <string.h>
#include "unicode/schriter.h"

//--------------------------------------------------------------------------------------
/**
 * "Vector" class for holding test tables
 * (this class is actually a linked list, but we use the name and API of the
 * java.util.Vector class to keep as much of our test code as possible the same.)
 */
class Enumeration { // text enumeration
public:
  virtual bool_t hasMoreElements() = 0;
  virtual UnicodeString nextElement() = 0;
};

class Vector { // text vector
public:

  class TextLink {
  public:
    TextLink() : fLink(0), fText() {}
    TextLink(TextLink* link, UnicodeString text) : fLink(link), fText(text) {}

    TextLink* fLink;
    UnicodeString fText;
  };

public:
  TextLink fBase;
  TextLink* fEnd;
  int32_t fSize;

public:
  class VectorEnumeration : public Enumeration {
  public:
    VectorEnumeration(Vector* vector) : fVector(vector), fPos(&vector->fBase) {}
    
    bool_t hasMoreElements() { return fPos->fLink != &fVector->fBase; }
    UnicodeString nextElement() { fPos = fPos->fLink; return fPos->fText; }

    Vector* fVector;
    TextLink* fPos;
  };

  Vector() : fBase(), fEnd(&fBase), fSize(0) { fBase.fLink = &fBase; }

  ~Vector() { 
    while (fBase.fLink != &fBase) { 
        TextLink* link = fBase.fLink;
        fBase.fLink = link->fLink;
        delete link;
        }
  }

  void addElement(UnicodeString text) { fEnd->fLink = new TextLink(&fBase, text); fEnd = fEnd->fLink; ++fSize; }
  void insertElementAt(UnicodeString text, int pos) { 
	  if(pos >= fSize || pos < 0)
		  ;
	  else if(pos == 0){
          TextLink* insert = new TextLink(&fBase, text);
		  insert->fLink=fBase.fLink;
		  ++fSize;
		  fBase.fLink=insert;
	  }
	  else{
          TextLink* link = fBase.fLink; 
		  while(--pos > 0)
             link=link->fLink;
          TextLink* insert = new TextLink(&fBase, text);
		  insert->fLink =link->fLink;
          link->fLink=insert;
		  ++fSize;

	  }

  }
    UnicodeString elementAt(int32_t pos) {
        if (pos >= fSize)
          return UnicodeString();

    TextLink* link = fBase.fLink; 
    while (pos-- > 0) link = link->fLink; 
    return link->fText;
  }
  UnicodeString lastElement() { return fEnd == &fBase ? UnicodeString() : fEnd->fText; }
  int32_t size() { return fSize; }

  Enumeration* elements() { return new VectorEnumeration(this); }

};

//--------------------------------------------------------------------------------------
/**
 * RBBITest is medium top level test class RuleBasedBreakIterator
 */

const UnicodeString  halfNA  = CharsToUnicodeString("\\u0928\\u094d\\u200d");  //halfform NA = devanigiri NA + virama(supresses inherent vowel)+ zero width joiner   
const UnicodeString  halfSA  = CharsToUnicodeString("\\u0938\\u094d\\u200d"); 
const UnicodeString  halfCHA = CharsToUnicodeString("\\u091a\\u094d\\u200d"); 
const UnicodeString  halfKA  = CharsToUnicodeString("\\u0915\\u094d\\u200d"); 
const UnicodeString  deadTA  = CharsToUnicodeString("\\u0924\\u094d");
//--------------------------------------------------------------------
//tests default rules based character iteration
//--------------------------------------------------------------------
void RBBITest::TestDefaultRuleBasedCharacterIteration()
{
   //	RuleBasedBreakIterator* rbbi=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance();
      logln((UnicodeString)"Testing the RBBI for character iteration by using default rules");
  //fetch the rules used to create the above RuleBasedBreakIterator
    //    UnicodeString defaultRules=rbbi->getRules();
    //     RuleBasedCharacterIterator charIterDefault = new RuleBasedBreakIterator(defaultRules); 
	 
	  UErrorCode status=U_ZERO_ERROR;
	  RuleBasedBreakIterator* charIterDefault=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
	  if(U_FAILURE(status)){
		  errln("FAIL : in construction");
		  return;
	  }

      Vector *chardata = new Vector();
      chardata->addElement("H");
      chardata->addElement("e");
      chardata->addElement("l");
      chardata->addElement("l");
      chardata->addElement("o");
      chardata->addElement(CharsToUnicodeString("e\\u0301"));                   //acuteE
      chardata->addElement("&");
      chardata->addElement(CharsToUnicodeString("e\\u0303"));                   //tildaE
      //devanagiri characters for Hindi support
      chardata->addElement(CharsToUnicodeString("\\u0906"));                    //devanagiri AA
      chardata->addElement(CharsToUnicodeString("\\u093e\\u0901"));              //devanagiri vowelsign AA+ chandrabindhu
      chardata->addElement(CharsToUnicodeString("\\u0916\\u0947"));              //devanagiri KHA+vowelsign E
      chardata->addElement(CharsToUnicodeString("\\u0938\\u0941\\u0902"));        //devanagiri SA+vowelsign U + anusvara(bindu)
      chardata->addElement(CharsToUnicodeString("\\u0926"));                    //devanagiri consonant DA
      chardata->addElement(CharsToUnicodeString("\\u0930"));                    //devanagiri consonant RA
      chardata->addElement(CharsToUnicodeString("\\u0939\\u094c"));              //devanagiri HA+vowel sign AI
      chardata->addElement(CharsToUnicodeString("\\u0964"));                    //devanagiri danda
      //end hindi characters      
      chardata->addElement(CharsToUnicodeString("A\\u0302"));                   //circumflexA 
      chardata->addElement(CharsToUnicodeString("i\\u0301"));                   //acuteBelowI   
      // conjoining jamo->.. 
      chardata->addElement(CharsToUnicodeString("\\u1109\\u1161\\u11bc"));
      chardata->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11bc"));
      chardata->addElement("\n");
      chardata->addElement("\r\n");                      //keep CRLF sequences together  
      chardata->addElement(CharsToUnicodeString("S\\u0300"));                   //graveS
      chardata->addElement(CharsToUnicodeString("i\\u0301"));                   //acuteBelowI
      chardata->addElement("!");

	  // What follows is a string of Korean characters (I found it in the Yellow Pages
    // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
    // it correctly), first as precomposed syllables, and then as conjoining jamo.
    // Both sequences should be semantically identical and break the same way.
    // precomposed syllables...
      chardata->addElement(CharsToUnicodeString("\\uc0c1"));
      chardata->addElement(CharsToUnicodeString("\\ud56d"));
      chardata->addElement(" ");
      chardata->addElement(CharsToUnicodeString("\\ud55c"));
      chardata->addElement(CharsToUnicodeString("\\uc778"));
      chardata->addElement(" ");
      chardata->addElement(CharsToUnicodeString("\\uc5f0"));
      chardata->addElement(CharsToUnicodeString("\\ud569"));
      chardata->addElement(" ");
      chardata->addElement(CharsToUnicodeString("\\uc7a5"));
      chardata->addElement(CharsToUnicodeString("\\ub85c"));
      chardata->addElement(CharsToUnicodeString("\\uad50"));
      chardata->addElement(CharsToUnicodeString("\\ud68c"));
      chardata->addElement(" ");
    // conjoining jamo...
      chardata->addElement(CharsToUnicodeString("\\u1109\\u1161\\u11bc"));
      chardata->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11bc"));
      chardata->addElement(" ");
      chardata->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11ab"));
      chardata->addElement(CharsToUnicodeString("\\u110b\\u1175\\u11ab"));
      chardata->addElement(" ");
      chardata->addElement(CharsToUnicodeString("\\u110b\\u1167\\u11ab"));
      chardata->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11b8"));
      chardata->addElement(" ");
      chardata->addElement(CharsToUnicodeString("\\u110c\\u1161\\u11bc"));
      chardata->addElement(CharsToUnicodeString("\\u1105\\u1169"));
      chardata->addElement(CharsToUnicodeString("\\u1100\\u116d"));
      chardata->addElement(CharsToUnicodeString("\\u1112\\u116c"));
      generalIteratorTest(*charIterDefault, chardata);

	  delete charIterDefault;
	  delete chardata;
	 // delete rbbi;

}
//--------------------------------------------------------------------
//tests default rules based word iteration
//--------------------------------------------------------------------
void RBBITest::TestDefaultRuleBasedWordIteration()
{
      logln((UnicodeString)"Testing the RBBI for word iteration using default rules");
   //   RuleBasedBreakIterator *rbbi=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance();
      //fetch the rules used to create the above RuleBasedBreakIterator
  //    UnicodeString defaultRules=rbbi->getRules();
  //     RuleBasedBreakIterator wordIterDefault = new RuleBasedBreakIterator(defaultRules); 

	  UErrorCode status=U_ZERO_ERROR; 
	  RuleBasedBreakIterator* wordIterDefault=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
      if(U_FAILURE(status)){
		  errln("FAIL : in construction");
		  return;
	  }

      Vector *worddata = new Vector();
      worddata->addElement ("Write");
      worddata->addElement (" ");
      worddata->addElement ("wordrules");
      worddata->addElement (".");
      worddata->addElement(" ");
	  worddata->addElement("alpha-beta-gamma");
	  worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u092f\\u0939"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0939\\u093f") + halfNA + CharsToUnicodeString("\\u0926\\u0940"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0939\\u0948"));   
      //worddata->addElement("\\u0964");   //danda followed by a space "\u0964->danda: hindi phrase seperator"
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0905\\u093e\\u092a"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0938\\u093f\\u0916\\u094b\\u0917\\u0947"));
      worddata->addElement("?");
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0968\\u0966.\\u0969\\u096f"));            //hindi numbers
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0967\\u0966\\u0966.\\u0966\\u0966%"));     //postnumeric
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u20a8\\u0967,\\u0967\\u0966\\u0966.\\u0966\\u0966")); //pre-number India currency symbol Rs->\\u20aD
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0905\\u092e\\u091c"));
      worddata->addElement("\n");
      worddata->addElement(halfSA+CharsToUnicodeString("\\u0935\\u0924\\u0902")+deadTA+CharsToUnicodeString("\\u0930"));
      worddata->addElement("\r");
      worddata->addElement("It's");
      worddata->addElement(" ");
      worddata->addElement("$30.10");
      worddata->addElement(" ");  
      worddata->addElement(CharsToUnicodeString("\\u00A2")); //cent sign
      worddata->addElement(CharsToUnicodeString("\\u00A3")); //pound sign
      worddata->addElement(CharsToUnicodeString("\\u00A4")); //currency sign
      worddata->addElement(CharsToUnicodeString("\\u00A5")); //yen sign
      worddata->addElement("alpha-beta-gamma");
      worddata->addElement(" ");
      worddata->addElement("Badges");
      worddata->addElement("?");
      worddata->addElement(" ");
      worddata->addElement("BADGES");
      worddata->addElement("!");
      worddata->addElement("1000,233,456.000");
      worddata->addElement(" ");
      worddata->addElement("1,23.322%");
      worddata->addElement(" ");
      worddata->addElement("123.1222");
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0024123,000.20"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("179.01\\u0025"));
      worddata->addElement("X");
      worddata->addElement(" ");
      worddata->addElement("Now");
      worddata->addElement("\r");
      worddata->addElement("is");
      worddata->addElement("\n");
      worddata->addElement("the");
      worddata->addElement("\r\n");
      worddata->addElement("time");
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\uc5f0\\ud569"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\uc7a5\\ub85c\\uad50\\ud68c"));
      worddata->addElement(" ");
      // conjoining jamo...
      worddata->addElement(CharsToUnicodeString("\\u1109\\u1161\\u11bc\\u1112\\u1161\\u11bc"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11ab\\u110b\\u1175\\u11ab"));
      worddata->addElement(" ");
	  worddata->addElement("Hello");
      worddata->addElement(",");
      worddata->addElement(" ");
      worddata->addElement("how");
      worddata->addElement(" ");
      worddata->addElement("are");
      worddata->addElement(" ");
      worddata->addElement("you");
      worddata->addElement(" ");
      generalIteratorTest(*wordIterDefault, worddata);

	  delete wordIterDefault;
	  delete worddata;
	 // delete rbbi;
}
//--------------------------------------------------------------------
//tests default rules based word iteration
//--------------------------------------------------------------------
const UnicodeString kParagraphSeparator = CharsToUnicodeString("\\u2029");
const UnicodeString kLineSeparator = CharsToUnicodeString("\\u2028");

void RBBITest::TestDefaultRuleBasedSentenceIteration()
{
      logln((UnicodeString)"Testing the RBBI for sentence iteration using default rules");
     // RuleBasedBreakIterator *rbbi=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createSentenceInstance();
      //fetch the rules used to create the above RuleBasedBreakIterator
    //  UnicodeString defaultRules=rbbi->getRules();
    //  RuleBasedBreakIterator sentIterDefault = new RuleBasedBreakIterator(defaultRules); 
      UErrorCode status=U_ZERO_ERROR;
      RuleBasedBreakIterator* sentIterDefault=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createSentenceInstance(Locale::getDefault(), status);
      if(U_FAILURE(status)){
		  errln("FAIL : in construction");
		  return;
	  }
      Vector *sentdata = new Vector();
      sentdata->addElement("(This is it.) ");
      sentdata->addElement("Testing the sentence iterator. ");
      sentdata->addElement("\"This isn\'t it.\" ");
      sentdata->addElement("Hi! ");
	  sentdata = new Vector();
      sentdata->addElement("This is a simple sample sentence. ");
      sentdata->addElement("(This is it.) ");
      sentdata->addElement("This is a simple sample sentence. ");
      sentdata->addElement("\"This isn\'t it.\" ");
      sentdata->addElement("Hi! ");
      sentdata->addElement("This is a simple sample sentence. ");
      sentdata->addElement("It does not have to make any sense as you can see. ");
      sentdata->addElement("Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura. ");
      sentdata->addElement("Che la dritta via aveo smarrita. ");
      sentdata->addElement("He said, that I said, that you said!! ");

      sentdata->addElement("Don't rock the boat." + kParagraphSeparator);

      sentdata->addElement("Because I am the daddy, that is why. ");
      sentdata->addElement("Not on my time (el timo.)! ");

      sentdata->addElement("So what!!" + kParagraphSeparator);

      sentdata->addElement("\"But now,\" he said, \"I know!\" ");
      sentdata->addElement("Harris thumbed down several, including \"Away We Go\" (which became the huge success Oklahoma!). ");
      sentdata->addElement("One species, B. anthracis, is highly virulent.\n");
      sentdata->addElement("Wolf said about Sounder:\"Beautifully thought-out and directed.\" ");
      sentdata->addElement("Have you ever said, \"This is where\tI shall live\"? ");
      sentdata->addElement("He answered, \"You may not!\" ");
      sentdata->addElement("Another popular saying is: \"How do you do?\". ");
      sentdata->addElement("Yet another popular saying is: \'I\'m fine thanks.\' ");
      sentdata->addElement("What is the proper use of the abbreviation pp.? ");
      sentdata->addElement("Yes, I am definatelly 12\" tall!!");
      // test for bug #4113835: \n and \r count as spaces, not as paragraph breaks
      sentdata->addElement(CharsToUnicodeString("Now\ris\nthe\r\ntime\n\rfor\r\rall\\u2029"));

    // test that it doesn't break sentences at the boundary between CJK
    // and other letters
      sentdata->addElement(CharsToUnicodeString("\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165:\"JAVA\\u821c")
        + CharsToUnicodeString("\\u8165\\u7fc8\\u51ce\\u306d,\\u2494\\u56d8\\u4ec0\\u60b1\\u8560\\u51ba")
        + CharsToUnicodeString("\\u611d\\u57b6\\u2510\\u5d46\".\\u2029"));
      sentdata->addElement(CharsToUnicodeString("\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8")
        + CharsToUnicodeString("\\u97e4JAVA\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8\\u4ec0")
        + CharsToUnicodeString("\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u2029"));
      sentdata->addElement(CharsToUnicodeString("\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8\\u97e4")
        + CharsToUnicodeString("\\u6470\\u8790JAVA\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8")
        + CharsToUnicodeString("\\u4ec0\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u2029"));
      sentdata->addElement(CharsToUnicodeString("He said, \"I can go there.\"\\u2029"));

      // Treat fullwidth variants of .!? the same as their
      // normal counterparts
      sentdata->addElement(CharsToUnicodeString("I know I'm right\\uff0e "));
      sentdata->addElement(CharsToUnicodeString("Right\\uff1f "));
      sentdata->addElement(CharsToUnicodeString("Right\\uff01 "));

      // Don't break sentences at boundary between CJK and digits
      sentdata->addElement(CharsToUnicodeString("\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8")
                + CharsToUnicodeString("\\u97e48888\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8\\u4ec0")
                + CharsToUnicodeString("\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u2029"));

      // Break sentence between a sentence terminator and
      // opening punctuation
      sentdata->addElement("How do you do?");
      sentdata->addElement("(fine).");
      //sentence breaks for hindi which used Devanagari script
      //make sure there is sentence break after ?,danda(hindi phrase separator),fullstop followed by space and no break after \n \r 
      sentdata->addElement(CharsToUnicodeString("\\u0928\\u092e")+halfSA
								   + CharsToUnicodeString("\\u0924\\u0947 ")  
                                   + CharsToUnicodeString("\\u0930\\u092e\\u0947\\u0936, ") 
                                   + CharsToUnicodeString("\\u0905\\u093e\\u092a")
                                   + CharsToUnicodeString("\\u0915\\u0948\\u0938\\u0947 ")
                                   + CharsToUnicodeString("\\u0939\\u0948?"));   
      sentdata->addElement(CharsToUnicodeString("\\u092e\\u0948 \\u0905")
							       + halfCHA
								   +CharsToUnicodeString("\\u091b\\u093e \\u0939\\u0942\\u0901\\u0964 "));   
      sentdata->addElement(CharsToUnicodeString("\\u0905\\u093e\\u092a\r\n \\u0915\\u0948\\u0938\\u0947 \\u0939\\u0948?"));   
      sentdata->addElement(CharsToUnicodeString("\\u0935\\u0939 ")
								   + halfKA
								   +CharsToUnicodeString("\\u092f\\u093e\n \\u0939\\u0948?"));   
      sentdata->addElement(CharsToUnicodeString("\\u092f\\u0939 \\u0905\\u093e\\u092e \\u0939\\u0948. "));   
      sentdata->addElement(CharsToUnicodeString("\\u092f\\u0939 means \"this\". ")); 
      sentdata->addElement(CharsToUnicodeString("\"\\u092a\\u095d\\u093e\\u0908\" meaning \"education\" or \"studies\". "));
      sentdata->addElement(CharsToUnicodeString("\\u0905\\u093e\\u091c") 
                                   + CharsToUnicodeString("(")
								   + halfSA
								   + CharsToUnicodeString("\\u0935\\u0924\\u0902")
								   + deadTA+ CharsToUnicodeString("\\u0930 ")
                                   + CharsToUnicodeString("\\u0926\\u093f\\u0935\\u093e\\u0938) ")
                                   + CharsToUnicodeString("\\u0939\\u0948\\u0964 "));
      sentdata->addElement("Let's end here. ");
      generalIteratorTest(*sentIterDefault, sentdata);

	  delete sentIterDefault;
	  delete sentdata;
//	  delete rbbi;
}
//--------------------------------------------------------------------
//tests default rules based line iteration
//--------------------------------------------------------------------
void RBBITest::TestDefaultRuleBasedLineIteration()
{
    UErrorCode status= U_ZERO_ERROR;
    RuleBasedBreakIterator* lineIterDefault=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createLineInstance(Locale::getDefault(), status);
    if(U_FAILURE(status)){
		  errln("FAIL : in construction");
		  return;
	  }
    Vector *linedata = new Vector();
      linedata->addElement("Multi-");
      linedata->addElement("Level ");
      linedata->addElement("example ");
      linedata->addElement("of ");
      linedata->addElement("a ");
      linedata->addElement("semi-");
      linedata->addElement("idiotic ");
      linedata->addElement("non-");
      linedata->addElement("sensical ");
      linedata->addElement("(non-");
      linedata->addElement("important) ");
      linedata->addElement("sentence. ");

      linedata->addElement("Hi  ");
      linedata->addElement("Hello ");
      linedata->addElement("How\n");
      linedata->addElement("are\r");
      linedata->addElement("you" + kLineSeparator);
      linedata->addElement("fine.\t");
      linedata->addElement("good.  ");

      linedata->addElement("Now\r");
      linedata->addElement("is\n");
      linedata->addElement("the\r\n");
      linedata->addElement("time\n");
      linedata->addElement("\r");
      linedata->addElement("for\r");
      linedata->addElement("\r");
      linedata->addElement("all");

    // to test for bug #4068133
      linedata->addElement(CharsToUnicodeString("\\u96f6"));
      linedata->addElement(CharsToUnicodeString("\\u4e00\\u3002"));
      linedata->addElement(CharsToUnicodeString("\\u4e8c\\u3001"));
      linedata->addElement(CharsToUnicodeString("\\u4e09\\u3002\\u3001"));
      linedata->addElement(CharsToUnicodeString("\\u56db\\u3001\\u3002\\u3001"));
      linedata->addElement(CharsToUnicodeString("\\u4e94,"));
      linedata->addElement(CharsToUnicodeString("\\u516d."));
      linedata->addElement(CharsToUnicodeString("\\u4e03.\\u3001,\\u3002"));
      linedata->addElement(CharsToUnicodeString("\\u516b"));

    // to test for bug #4086052
      linedata->addElement(CharsToUnicodeString("foo\\u00a0bar "));
//          linedata->addElement("foo\\ufeffbar");

    // to test for bug #4097920
      linedata->addElement("dog,");
      linedata->addElement("cat,");
      linedata->addElement("mouse ");
      linedata->addElement("(one)");
      linedata->addElement("(two)\n");

    // to test for bug #4035266
      linedata->addElement("The ");
      linedata->addElement("balance ");
      linedata->addElement("is ");
      linedata->addElement("$-23,456.78, ");
      linedata->addElement("not ");
      linedata->addElement("-$32,456.78!\n");

    // to test for bug #4098467
    // What follows is a string of Korean characters (I found it in the Yellow Pages
    // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
    // it correctly), first as precomposed syllables, and then as conjoining jamo.
    // Both sequences should be semantically identical and break the same way.
    // precomposed syllables...
      linedata->addElement(CharsToUnicodeString("\\uc0c1\\ud56d "));
      linedata->addElement(CharsToUnicodeString("\\ud55c\\uc778 "));
      linedata->addElement(CharsToUnicodeString("\\uc5f0\\ud569 "));
      linedata->addElement(CharsToUnicodeString("\\uc7a5\\ub85c\\uad50\\ud68c "));
    // conjoining jamo...
      linedata->addElement(CharsToUnicodeString("\\u1109\\u1161\\u11bc\\u1112\\u1161\\u11bc "));
      linedata->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11ab\\u110b\\u1175\\u11ab "));
      linedata->addElement(CharsToUnicodeString("\\u110b\\u1167\\u11ab\\u1112\\u1161\\u11b8 "));
      linedata->addElement(CharsToUnicodeString("\\u110c\\u1161\\u11bc\\u1105\\u1169\\u1100\\u116d\\u1112\\u116c"));

    // to test for bug #4117554: Fullwidth .!? should be treated as postJwrd
      linedata->addElement(CharsToUnicodeString("\\u4e01\\uff0e"));
      linedata->addElement(CharsToUnicodeString("\\u4e02\\uff01"));
      linedata->addElement(CharsToUnicodeString("\\u4e03\\uff1f"));

	generalIteratorTest(*lineIterDefault, linedata);

	delete lineIterDefault;
	delete linedata;

}
//--------------------------------------------------------------------
//Testing the BreakIterator for devanagari script
//--------------------------------------------------------------------
 
const UnicodeString deadRA  = CharsToUnicodeString("\\u0930\\u094d");         /*deadform RA = devanagari RA + virama*/
const UnicodeString deadPHA = CharsToUnicodeString("\\u092b\\u094d");         /*deadform PHA = devanagari PHA + virama*/
const UnicodeString deadTTHA= CharsToUnicodeString("\\u0920\\u094d");
const UnicodeString deadPA  = CharsToUnicodeString("\\u092a\\u094d");
const UnicodeString deadSA  = CharsToUnicodeString("\\u0938\\u094d");
const UnicodeString visarga = CharsToUnicodeString("\\u0903");              /*devanagari visarga looks like a english colon*/

void RBBITest::TestHindiCharacterBreak()
{
      Vector *hindicharData = new Vector();
      //devanagari characters for Hindi support
      hindicharData->addElement(CharsToUnicodeString("\\u0906"));                    //devanagari AA
      //hindi character break should make sure that it 
      // doesn't break in-between a vowelsign and a chandrabindu
      hindicharData->addElement(CharsToUnicodeString("\\u093e\\u0901"));              //devanagari vowelsign AA+ chandrabindu
      hindicharData->addElement(CharsToUnicodeString("\\u0916\\u0947"));              //devanagari KHA+vowelsign E
      hindicharData->addElement(CharsToUnicodeString("\\u0938\\u0941\\u0902"));        //devanagari SA+vowelsign U + anusvara(bindu)
      hindicharData->addElement(CharsToUnicodeString("\\u0926"));                    //devanagari consonant DA
      hindicharData->addElement(CharsToUnicodeString("\\u0930"));                    //devanagari consonant RA
      hindicharData->addElement(CharsToUnicodeString("\\u0939\\u094c"));              //devanagari consonant HA+dependent vowel sign AI
      hindicharData->addElement(CharsToUnicodeString("\\u0964"));                    //devanagari danda
      hindicharData->addElement(CharsToUnicodeString("\\u0950"));                    //devanagari OM 
      hindicharData->addElement(CharsToUnicodeString("\\u0915\\u0943"));              //devanagari KA+dependent vowel RI->KRI

      //dependent half-forms
      hindicharData->addElement(halfSA+CharsToUnicodeString("\\u0924"));             //halfSA+base consonant TA->STA
      hindicharData->addElement(halfSA+CharsToUnicodeString("\\u0925"));             //halfSA+base consonant THA->STHA
      hindicharData->addElement(halfSA+CharsToUnicodeString("\\u092e"));             //halfSA+base consonant MA->SMA
      hindicharData->addElement(halfCHA+CharsToUnicodeString("\\u091b"));            //halfCHA+base consonant CHHA->CHHHA
      hindicharData->addElement(halfNA+CharsToUnicodeString("\\u0917"));             //halfNA+base consonant GA->NGA
      hindicharData->addElement(CharsToUnicodeString("\\u092a\\u094d\\u200d\\u092f"));   //halfPA(PA+virama+zerowidthjoiner+base consonant YA->PYA


      //consonant RA rules ----------
      //if the dead consonant RA precedes either a consonant or an independent vowel,
      //then it is replaced by its superscript non-spacing mark
      hindicharData->addElement(deadRA+ CharsToUnicodeString("\\u0915"));             //deadRA+devanagari consonant KA->KA+superRA 
      hindicharData->addElement(deadRA+ CharsToUnicodeString("\\u0923"));             //deadRA+devanagari consonant NNA->NNA+superRA
      hindicharData->addElement(deadRA+ CharsToUnicodeString("\\u0917"));             //deadRA+devanagari consonant GA->GA+superRA
    //  hindicharData->addElement(deadRA+ CharsToUnicodeString("\\u0960"));           //deadRA+devanagari cosonant RRI->RRI+superRA
      
      //if any dead consonant(other than dead RA)precedes the consonant RA, then
      //it is replaced with its nominal forma nd RA is replaced by the subscript non-spacing mark.
      hindicharData->addElement(deadPHA+ CharsToUnicodeString("\\u0930"));            //deadPHA+devanagari consonant RA->PHA+subRA
      hindicharData->addElement(deadPA+ CharsToUnicodeString("\\u0930"));             //deadPA+devanagari consonant RA->PA+subRA
      hindicharData->addElement(deadTTHA+ CharsToUnicodeString("\\u0930"));           //deadTTHA+devanagari consonant RA->TTHA+subRA
      hindicharData->addElement(deadTA+ CharsToUnicodeString("\\u0930"));             //deadTA+RA->TRA 
      hindicharData->addElement(CharsToUnicodeString("\\u0936\\u094d\\u0930"));         //deadSHA(SHA+virama)+RA->SHRA 

      //conjuct ligatures
      hindicharData->addElement(CharsToUnicodeString("\\u0915\\u094d\\u0937"));         //deadKA(KA+virama) followed by SSHA wraps up into a single character KSSHA
      hindicharData->addElement(deadTA+CharsToUnicodeString("\\u0924"));              //deadTA+TA wraps up into glyph TTHA
      hindicharData->addElement(CharsToUnicodeString("\\u0926\\u094d\\u0935"));         //deadDA(DA+virama)+VA wraps up into DVA
      hindicharData->addElement(CharsToUnicodeString("\\u091c\\u094d\\u091e"));         //deadJA(JA+virama)+NYA wraps up into JNYA
     
      UErrorCode status= U_ZERO_ERROR;
	  RuleBasedBreakIterator *e=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
	  if(U_FAILURE(status)){
		  errln("FAIL : in construction");
		  return;
	  }
	  generalIteratorTest(*e, hindicharData);
   	  delete e;
	  delete hindicharData;
}
void RBBITest::TestHindiWordBreak()
   {
     Vector *hindiWordData = new Vector();

      //hindi
     hindiWordData->addElement(CharsToUnicodeString("\\u0917\\u092a-\\u0936\\u092a"));
     hindiWordData->addElement("!");
     hindiWordData->addElement(CharsToUnicodeString("\\u092f\\u0939"));
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u0939\\u093f") + halfNA + CharsToUnicodeString("\\u0926\\u0940"));
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u0939\\u0948"));
     //danda is similar to full stop. danda is a hindi phrase seperator
     //Make sure it breaks before danda and after danda when it is followed by a space
     //hindiWordData->addElement(CharsToUnicodeString("\\u0964"));   //fails here doesn't break at danda
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u0905\\u093e\\u092a"));
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u0938\\u093f\\u0916\\u094b\\u0917\\u0947"));
     hindiWordData->addElement("?");
     hindiWordData->addElement("\n"); 
     hindiWordData->addElement(":");
     hindiWordData->addElement(deadPA+CharsToUnicodeString("\\u0930\\u093e\\u092f")+visarga);    //no break before visarga
     hindiWordData->addElement(" ");

     hindiWordData->addElement(CharsToUnicodeString("\\u0935") + deadRA+ CharsToUnicodeString("\\u0937\\u093e"));
     hindiWordData->addElement("\r\n");
     hindiWordData->addElement(deadPA+ CharsToUnicodeString("\\u0930\\u0915\\u093e\\u0936"));     //deadPA+RA+KA+vowel AA+SHA -> prakash
     hindiWordData->addElement(","); 
     hindiWordData->addElement(CharsToUnicodeString("\\u0924\\u0941\\u092e\\u093e\\u0930\\u094b")); 
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u092e\\u093f")+ deadTA+ CharsToUnicodeString("\\u0930"));       //MA+vowel I+ deadTA + RA 
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u0915\\u093e"));
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u092a")+ deadTA + CharsToUnicodeString("\\u0930"));            //PA + deadTA + RA
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u092a\\u095d\\u094b"));
    // hindiWordData->addElement(CharsToUnicodeString("\\u0964")); //fails here doesn't break at danda
     hindiWordData->addElement(" ");
     hindiWordData->addElement(deadSA + deadTA + CharsToUnicodeString("\\u0930\\u093f"));       //deadSA+deadTA+RA+vowel I->sthri
     hindiWordData->addElement(".");
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u0968\\u0966.\\u0969\\u096f"));            //hindi numbers
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u0967\\u0966\\u0966.\\u0966\\u0966%"));     //postnumeric
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u20a8\\u0967,\\u0967\\u0966\\u0966.\\u0966\\u0966")); //pre-number India currency symbol Rs.\\u20aD
     hindiWordData->addElement(" ");
     hindiWordData->addElement(CharsToUnicodeString("\\u0905\\u092e\\u091c"));
     hindiWordData->addElement("\n");
     hindiWordData->addElement(halfSA+CharsToUnicodeString("\\u0935\\u0924\\u0902")+deadTA+CharsToUnicodeString("\\u0930"));
     hindiWordData->addElement("\r");
  
     UErrorCode status=U_ZERO_ERROR;
	 RuleBasedBreakIterator *e=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
	  if(U_FAILURE(status)){
		  errln("FAIL : in construction");
		  return;
	  }
	  generalIteratorTest(*e, hindiWordData);
   	  delete e;
	  delete hindiWordData;
}
/*
//Bug: if there is no word break before and after danda when it is followed by a space
void RBBITest::TestDanda()
{
      Vector *hindiWordData = new Vector();
      //hindi
      hindiWordData->addElement(CharsToUnicodeString("\\u092f\\u0939"));
      hindiWordData->addElement(" ");
      //Danda is similar to full stop, danda is a hindi phrase seperator.
      //Make sure there is a word break before and after danda when it is followed by a space
     //following fail----
      hindiWordData->addElement(CharsToUnicodeString("\\u0939\\u0948"));
    //  hindiWordData->addElement(CharsToUnicodeString("\\u0964"));         // devanagari danda
	  hindiWordData->addElement(" ");
      hindiWordData->addElement(CharsToUnicodeString("\\u092f\\u0939"));
  //    hindiWordData->addElement(CharsToUnicodeString("\\u0965"));         //devanagari double danda
	  hindiWordData->addElement(" ");
		
	  RuleBasedBreakIterator* e=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance();
	  generalIteratorTest(*e, hindiWordData);
   	  delete e;
	  delete hindiWordData;
}
//Make sure the character wrapping is done correctly
void RBBITest::TestHindiCharacterWrapping()
{
      Vector *hindicharData = new Vector();
      //if the dead consonant RA precedes either a consonant or an independent vowel,
      //then it is replaced by its superscript non-spacing mark
      hindicharData->addElement(deadRA+ CharsToUnicodeString("\\u0917")); //deadRA+devanagari consonant GA->GA+superRA
      //following fail----
     // hindicharData->addElement(deadRA+ CharsToUnicodeString("\\u0960"));   //deadRA+devanagari RRI->RRI+superRA
      
	  RuleBasedBreakIterator* e=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance();
	  generalIteratorTest(*e, hindicharData);
   	  delete e;
	  delete hindicharData;

}*/






//----------------------------------------------------------------------------------
//adds rules for telugu support and tests the behaviour of chracterIterator of RBBI 
//----------------------------------------------------------------------------------
/*void RBBITest::TestTeluguRuleBasedCharacterIteration()
{
     logln((UnicodeString)"Testing the RBBI by adding rules for Telugu(Indian Language) Support");
	 //get the default rules
     RuleBasedBreakIterator *rb= (RuleBasedBreakIterator*)BreakIterator::createCharacterInstance();
     //additional rules for Telugu(Indian Language) support
	 UnicodeString crules1 = rb->getRules()                                                 +  //default rules +
                      "<telvirama>=[\\u0c4d];"                                               +  //telugu virama
                      "<telVowelSign>=[\\u0c3e-\\u0c44\\u0c46\\u0c47\\u0c48\\u0c4a\\u0c4b\\u0c4c];" +  //telugu dependent vowel signs
                      "<telConsonant>=[\\u0c15-\\u0c28\\u0c2a-\\u0c33\\u0c35-\\u0c39];"           +  //telugu consonants
                      "<telCharEnd>=[\\u0c02\\u0c03\\u0c55\\u0c56];"                            +  //to create half forms and dead forms
                      "<telConjunct>=({<telConsonant><telvirama>{<zwj>}}<telConsonant>);"   +
                      "<telConjunct>{<telVowelSign>}{<telCharEnd>};";
      RuleBasedBreakIterator charIter=null;
      charIter   = new RuleBasedBreakIterator(crules1); 
      
	  Vector *chardata = new Vector();
      //behaviour of telugu characters from specified rules
      chardata->addElement(CharsToUnicodeString("\\u0c15"));                    //telugu consonant KA
      chardata->addElement(CharsToUnicodeString("\\u0c30\\u0c47"));              //telugu consonant RA+telugu dependent vowel EE
      chardata->addElement(CharsToUnicodeString("\\u0c1b\\u0c3e"));              //telugu consonant CHA+telegu depenednt vowel AA
      chardata->addElement(CharsToUnicodeString("\\u0c17\\u0c48"));              //telegu consonant GA+teleugu dependent vowel AI
      chardata->addElement(CharsToUnicodeString("\\u0c17\\u0c46\\u0c56"));        //telugu consonant GA+telugu dependent vowel sign E+telugu AI length mark
      chardata->addElement(CharsToUnicodeString("\\u0c28\\u0c4d\\u200d\\u0c28"));  //telugu consonant NA+telugu virama+zwj=>halfNA+NA->NNA(dependent half-form)
      chardata->addElement(CharsToUnicodeString("\\u0c17\\u0c4d\\u0c30"));        //GA+deadRA(RA+telvirama)->GA+subRA->GRA
      chardata->addElement(CharsToUnicodeString("\\u0c66"));                    //telugu digit
      chardata->addElement(CharsToUnicodeString("\\u0c37\\u0c4d\\u0c15"));        //deadSSA(SSA+telvirama)+KA+subSSA->KSHA
      //behaviour of other characters from default rules
      chardata->addElement("h");
      chardata->addElement(CharsToUnicodeString("A\\u0302"));                   // circumflexA 
      chardata->addElement(CharsToUnicodeString("i\\u0301"));                   // acuteBelowI 
      chardata->addElement(CharsToUnicodeString("\\u1109\\u1161\\u11bc"));
      chardata->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11bc"));
      chardata->addElement("\n");
      chardata->addElement("\r\n");    

	  generalIteratorTest(charIter, chardata);
       
	  delete charIter;
	  delete charData;
	  delete rb;
}

//--------------------------------------------------------------------
//tests the behaviour of character iteration of RBBI with custom rules
//--------------------------------------------------------------------
 
void RBBITest::TestCustomRuleBasedCharacterIteration()
{
      logln((UnicodeString)"Testing the RBBI by using custom rules for character iteration");

      UnicodeString crules2="<ignore>=[e];"                         + //ignore the character "e"
                     ".;"                                           + 
                     "<devVowelSign>=[\\u093e-\\u094c\\u0962\\u0963];"  +  //devanagiri vowel = \\u093e tp \\u094c and \\u0962.\\u0963
                     "<devConsonant>=[\\u0915-\\u0939];"              +  //devanagiri consonant = \\u0915 to \\u0939
                     "<devConsonant>{<devVowelSign>};" ;               //break at all places except the  following 
                                                                       //devanagiri consonants+ devanagiri vowelsign
     
	  RuleBasedCharacterIterator charIterCustom   = new RuleBasedBreakIterator(crules2);
      Vector *chardata = new Vector();
      chardata->addElement("He");              //ignores 'e'
      chardata->addElement("l");                
      chardata->addElement("l");
      chardata->addElement("oe");              //ignores 'e' hence wraps it into 'o' instead of wrapping with
      chardata->addElement(CharsToUnicodeString("\\u0301"));          //'\\u0301' to form 'acuteE '
      chardata->addElement("&e");              //ignores 'e' hence wraps it into '&' instead of wrapping with
      chardata->addElement(CharsToUnicodeString("\\u0303"));          //'\\u0303 to form 'tildaE'
      //devanagiri characters 
      chardata->addElement(CharsToUnicodeString("\\u0906"));          //devanagiri AA
      chardata->addElement(CharsToUnicodeString("\\u093e"));          //devanagiri vowelsign AA:--breaks at \\u0901 which is devanagiri 
      chardata->addElement(CharsToUnicodeString("\\u0901"));          //chandra bindhu since it is not mentioned in the rules
      chardata->addElement(CharsToUnicodeString("\\u0916\\u0947"));    //devanagiri KHA+vowelsign E
      chardata->addElement(CharsToUnicodeString("\\u0938\\u0941"));    //devanagiri SA+vowelsign U : - breaks at
      chardata->addElement(CharsToUnicodeString("\\u0902"));          //\\u0902 devanagiri anusvara since it is not mentioned in the rules
      chardata->addElement(CharsToUnicodeString("\\u0926"));          //devanagiri consonant DA
      chardata->addElement(CharsToUnicodeString("\\u0930"));          //devanagiri consonant RA
      chardata->addElement(CharsToUnicodeString("\\u0939\\u094c"));    //devanagiri HA+vowel sign AI
      chardata->addElement(CharsToUnicodeString("\\u0964"));          //devanagiri danda
      // devanagiri chracters end
      chardata->addElement("A");               //breaks in between since it is not mentioned in the rules
      chardata->addElement(CharsToUnicodeString("\\u0302"));          // circumflexA    
      chardata->addElement("i");               //breaks in between since not mentioned in the rules
      chardata->addElement(CharsToUnicodeString("\\u0301"));          // acuteBelowI   
      //Rules don't support conjoining jamo->->..
      chardata->addElement(CharsToUnicodeString("\\u1109"));          //break at every character since rules
      chardata->addElement(CharsToUnicodeString("\\u1161"));          //don't support conjoining jamo
      chardata->addElement(CharsToUnicodeString("\\u11bc"));
      chardata->addElement(CharsToUnicodeString("\\u1112"));
      chardata->addElement(CharsToUnicodeString("\\u1161"));
      chardata->addElement(CharsToUnicodeString("\\u11bc"));
      chardata->addElement("\n");
      chardata->addElement("\r");             //doesn't keep CRLGF together since rules do not mention it
      chardata->addElement("\n");
      chardata->addElement("S");              //graveS
      chardata->addElement(CharsToUnicodeString("\\u0300"));         //breaks in between since it is not mentioned in the rules
      chardata->addElement("i");              //acuteBelowI
      chardata->addElement(CharsToUnicodeString("\\u0301"));         //breaks in between since it is not mentioned in the rules
      generalIteratorTest(charIterCustom, chardata);

	  delete charIterCustom;
	  delete chardata;
}*/
/*//--------------------------------------------------------------------
//tests custom rules based word iteration
//--------------------------------------------------------------------
void RBBITest::TestCustomRuleBasedWordIteration(){
      logln("(UnicodeString)Testing the RBBI by using custom rules for word iteration");
      UnicodeString wrules1="<ignore>=[:Mn::Me::Cf:];"                  + //ignore non-spacing marks, enclosing marks, and format characters,
                      "<danda>=[\\u0964\\u0965];"                       + //Hindi Phrase seperator
                      "<let>=[:L::Mc:];"                                + //uppercase(Lu), lowercase(Ll), titlecase(Lt), modifier(Lm) letters, Mc-combining space mark
                      "<mid-word>=[:Pd:\\\"\\\'\\.];"                   + //dashes, quotation, apostraphes, period
                      "<ls>=[\\n\\u000c\\u2028\\u2029];"                + //line separators:  LF, FF, PS, and LS
                      "<ws>=[:Zs:\\t];"                                 + //all space separators and the tab character
                      "<word>=((<let><let>*(<mid-word><let><let>*)*));" +  
                      ".;"                                              + //break after every character, with the following exceptions
                      "{<word>};"                                       + 
                      "<ws>*{\\r}{<ls>}{<danda>};" ;

      RuleBasedBreakIterator wordIterCustom   = new RuleBasedBreakIterator(wrules1); 
      Vector *worddata = new Vector();
      worddata->addElement("Write");
      worddata->addElement(" ");
      worddata->addElement("wordrules");
      worddata->addElement(".");
      worddata->addElement(" ");
      //play with hindi
      worddata->addElement(CharsToUnicodeString("\\u092f\\u0939"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0939\\u093f") + halfNA + CharsToUnicodeString("\\u0926\\u0940"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0939\\u0948"));
      worddata->addElement(CharsToUnicodeString("\\u0964"));   //Danda is similar to full stop-> Danda followed by a space
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0905\\u093e\\u092a"));
      worddata->addElement(" ");
      worddata->addElement(CharsToUnicodeString("\\u0938\\u093f\\u0916\\u094b\\u0917\\u0947"));
      worddata->addElement("?");
      worddata->addElement(" ");
      worddata->addElement("It's");
      worddata->addElement(" ");
      worddata->addElement("$");
      worddata->addElement("3");
      worddata->addElement("0");
      worddata->addElement(".");
      worddata->addElement("1");
      worddata->addElement("0");
      worddata->addElement(" ");
      // worddata->addElement(" ");
      generalIteratorTest(wordIterCustom, worddata); 
      
	  delete wordIterCustom;
	  delete worddata;
}   
//-------------------------------------------------------------------------------
//adds extra rules to deal with abbrevations(limited) and test the word Iteration
//-------------------------------------------------------------------------------
void RBBITest::TestAbbrRuleBasedWordIteration()
{
      logln((UnicodeString)"Testing the RBBI for word iteration by adding rules to support abbreviation");
      RuleBasedBreakIterator *rb =(RuleBasedBreakIterator*)BreakIterator::createWordInstance();
      
      UnicodeString wrules2="<abbr>=((Mr.)|(Mrs.)|(Ms.)|(Dr.)|(U.S.));" + // abbreviations. 
                     rb->getRules()                               +
                     "{(<abbr><ws>)*<word>};";
      RuleBasedBreakIterator wordIter=null;
      //try{
      wordIter   = new RuleBasedBreakIterator(wrules2); 
    //  }catch(IllegalArgumentException iae){
   //       errln("ERROR: failed construction illegal rules");
   //   }
      Vector *worddata = new Vector();
      worddata->addElement("Mr. George");
      worddata->addElement(" ");
      worddata->addElement("is");
      worddata->addElement(" ");
      worddata->addElement("from");
      worddata->addElement(" ");
      worddata->addElement("U.S. Navy");
      worddata->addElement(".");
      worddata->addElement(" ");
      worddata->addElement("His");
      worddata->addElement("\n");
      worddata->addElement("friend");
      worddata->addElement("\t");
      worddata->addElement("Dr. Steven");
      worddata->addElement(" ");
      worddata->addElement("married");
      worddata->addElement(" ");
      worddata->addElement("Ms. Benneth");
      worddata->addElement("!");
      worddata->addElement(" ");
      worddata->addElement("Mrs. Johnson");
      worddata->addElement("\r\n");
      worddata->addElement("paid");
      worddata->addElement(" ");
      worddata->addElement("$2,400.00");
      generalIteratorTest(wordIter, worddata);
	  
	  delete wordIter;
	  delete worddata;
	  delete rb;
} */  

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void RBBITest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite RuleBasedBreakIterator: ");
    switch (index) {
    
        case 0: name = "TestDefaultRuleBasedCharacterIteration"; 
			if(exec) TestDefaultRuleBasedCharacterIteration(); break;
        case 1: name = "TestDefaultRuleBasedWordIteration";      
			if(exec) TestDefaultRuleBasedWordIteration();      break;
        case 2: name = "TestDefaultRuleBasedSentenceIteration";  
			if(exec) TestDefaultRuleBasedSentenceIteration();  break;
        case 3: name = "TestDefaulRuleBasedLineIteration()";
			if(exec) TestDefaultRuleBasedLineIteration();      break;
        case 4: name = "TestHindiCharacterBreak()";
            if(exec) TestHindiCharacterBreak();                break;
        case 5: name = "TestHindiWordBreak()";
            if(exec) TestHindiWordBreak();                     break;
        case 6: name = "TestHindiWordBreak()";
            if(exec) TestHindiWordBreak();                     break;
//      case 7: name = "TestDanda()";
//           if(exec) TestDanda();                              break;
//       case 8: name = "TestHindiCharacterWrapping()";
//           if(exec) TestHindiCharacterWrapping();             break;

//		case 9: name = "TestCustomRuleBasedWordIteration";       
//			if(exec) TestCustomRuleBasedWordIteration();       break;
//		case 10: name = "TestAbbrRuleBasedWordIteration";         
//			if(exec) TestAbbrRuleBasedWordIteration();         break;
     //	case 11: name = "TestTeluguRuleBasedCharacterIteration";  
	//		if(exec) TestTeluguRuleBasedCharacterIteration();  break;
	//	case 12: name = "TestCustomRuleBasedCharacterIteration";  
	//		if(exec) TestCustomRuleBasedCharacterIteration();  break;
		
        	       
        default: name = ""; break; //needed to end loop
    }
}

//---------------------------------------------
// Test implementation routines
//---------------------------------------------
UnicodeString RBBITest::createTestData(Enumeration* e)
{
  UnicodeString result = "";

  while (e->hasMoreElements()) {
    result += e->nextElement();
  }
  return result;
}
// general test Implementation subroutines
void RBBITest::generalIteratorTest(RuleBasedBreakIterator& bi, Vector* expectedResult) 
{
        Enumeration *elems = expectedResult->elements();
		UnicodeString text = createTestData(elems);
		delete elems;

        bi.setText(&text);

        Vector *nextResults = testFirstAndNext(bi, text);
        Vector *previousResults = testLastAndPrevious(bi, text);

        logln("comparing forward and backward...");
        int errs = getErrors();
		UnicodeString str1="forward iteration";
		UnicodeString str2="backward iteration";
        compareFragmentLists(str1, str2, nextResults,
                        previousResults);
        if (getErrors() == errs) {
            logln("comparing expected and actual...");
			str1="expected result";
			str2="actual result";
            compareFragmentLists(str1, str2, expectedResult,
                            nextResults);
        }

        int32_t *boundaries = new int32_t[expectedResult->size() + 3];
        boundaries[0] = RuleBasedBreakIterator::DONE;
        boundaries[1] = 0;
        for (int i = 0; i < expectedResult->size(); i++)
            boundaries[i + 2] = boundaries[i + 1] + ((UnicodeString)expectedResult->elementAt(i)).
                            length();
		
        int len = expectedResult->size() + 3 -1;
		boundaries[len] = RuleBasedBreakIterator::DONE;
      
        testFollowing(bi, text, boundaries);
        testPreceding(bi, text, boundaries);
        testIsBoundary(bi, text, boundaries);

        doMultipleSelectionTest(bi, text);
}

Vector* RBBITest::testFirstAndNext(RuleBasedBreakIterator& bi, UnicodeString& text) 
{
        int32_t p = bi.first();
        int32_t lastP = p;
        Vector *result = new Vector();
        UnicodeString selection;

        if (p != 0)
            errln((UnicodeString)"first() returned " + p + (UnicodeString)" instead of 0");
        while (p != RuleBasedBreakIterator::DONE) {
            p = bi.next();
            if (p != RuleBasedBreakIterator::DONE) {
                if (p <= lastP)
                    errln((UnicodeString)"next() failed to move forward: next() on position "
                                    + lastP + (UnicodeString)" yielded " + p);

                text.extractBetween(lastP, p, selection);  
                result->addElement(selection);
            }
            else {
                if (lastP != text.length())
                    errln((UnicodeString)"next() returned DONE prematurely: offset was "
                                    + lastP + (UnicodeString)" instead of " + text.length());
            }
            lastP = p;
        }
        return result;
}
Vector* RBBITest::testLastAndPrevious(RuleBasedBreakIterator& bi, UnicodeString& text) 
{
        int32_t p = bi.last();
        int32_t lastP = p;
        Vector *result = new Vector();
		UnicodeString selection;

        if (p != text.length())
            errln((UnicodeString)"last() returned " + p + (UnicodeString)" instead of " + text.length());
        while (p != RuleBasedBreakIterator::DONE) {
            p = bi.previous();
            if (p != RuleBasedBreakIterator::DONE) {
                if (p >= lastP)
                    errln((UnicodeString)"previous() failed to move backward: previous() on position "
                                    + lastP + (UnicodeString)" yielded " + p);
                text.extractBetween(p, lastP, selection);
                result->insertElementAt(selection, 0);
            }
            else {
                if (lastP != 0)
                    errln((UnicodeString)"previous() returned DONE prematurely: offset was "
                                    + lastP + (UnicodeString)" instead of 0");
            }
            lastP = p;
        }
		return result;
}

void RBBITest::compareFragmentLists(UnicodeString& f1Name, UnicodeString& f2Name, Vector* f1, Vector* f2) 
{
        int32_t p1 = 0;
        int32_t p2 = 0;
        UnicodeString s1;
        UnicodeString s2;
        int32_t t1 = 0;
        int32_t t2 = 0;
	

        while (p1 < f1->size() && p2 < f2->size()) {
            s1 = (UnicodeString)f1->elementAt(p1);
            s2 = (UnicodeString)f2->elementAt(p2);
            t1 += s1.length();
            t2 += s2.length();

            if (s1.compare(s2) == 0) {
                logln(prettify((UnicodeString)"   >" + s1 + (UnicodeString)"<"));
                ++p1;
                ++p2;
            }
            else {
                int32_t tempT1 = t1;
                int32_t tempT2 = t2;
                int32_t tempP1 = p1;
                int32_t tempP2 = p2;

                while (tempT1 != tempT2 && tempP1 < f1->size() && tempP2 < f2->size()) {
                    while (tempT1 < tempT2 && tempP1 < f1->size()) {
                        tempT1 += ((UnicodeString)f1->elementAt(tempP1)).length();
                        ++tempP1;
                    }
                    while (tempT2 < tempT1 && tempP2 < f2->size()) {
                        tempT2 += ((UnicodeString)f2->elementAt(tempP2)).length();
                        ++tempP2;
                    }
                }
                logln((UnicodeString)"*** " + f1Name + (UnicodeString)" has:");
                while (p1 <= tempP1 && p1 < f1->size()) {
                    s1 = (UnicodeString)f1->elementAt(p1);
                    t1 += s1.length();
                    logln(prettify((UnicodeString)" *** >" + s1 + (UnicodeString)"<"));
                    ++p1;
                }
                logln("***** " + f2Name + " has:");
                while (p2 <= tempP2 && p2 < f2->size()) {
                    s2 = (UnicodeString)f2->elementAt(p2);
                    t2 += s2.length();
                    logln(prettify(" ***** >" + s2 + "<"));
                    ++p2;
                }
                errln((UnicodeString)"Discrepancy between " + f1Name + (UnicodeString)" and " + f2Name);
            }
        }
}

void RBBITest::testFollowing(RuleBasedBreakIterator& bi, UnicodeString& text, int32_t *boundaries) 
{
        logln("testFollowing():");
        int p = 2;
        for (int i = 0; i <= text.length(); i++) {
            if (i == boundaries[p])
                ++p;

            int32_t b = bi.following(i);
            logln((UnicodeString)"bi.following(" + i + ") -> " + b);
            if (b != boundaries[p])
                errln((UnicodeString)"Wrong result from following() for " + i + (UnicodeString)": expected " + boundaries[p]
                                + (UnicodeString)", got " + b);
        }
}

void RBBITest::testPreceding(RuleBasedBreakIterator& bi, UnicodeString& text, int32_t *boundaries) {
        logln("testPreceding():");
        int p = 0;
        for (int i = 0; i <= text.length(); i++) {
            int32_t b = bi.preceding(i);
            logln((UnicodeString)"bi.preceding(" + i + ") -> " + b);
            if (b != boundaries[p])
                errln((UnicodeString)"Wrong result from preceding() for " + i + (UnicodeString)": expected " + boundaries[p]
                                + (UnicodeString)", got " + b);

            if (i == boundaries[p + 1])
                ++p;
        }
}

void RBBITest::testIsBoundary(RuleBasedBreakIterator& bi, UnicodeString& text, int32_t *boundaries) {
        logln("testIsBoundary():");
        int p = 1;
        bool_t isB;
        for (int i = 0; i < text.length(); i++) {
            isB = bi.isBoundary(i);
            logln((UnicodeString)"bi.isBoundary(" + i + ") -> " + isB);

            if (i == boundaries[p]) {
                if (!isB)
                    errln((UnicodeString)"Wrong result from isBoundary() for " + i + (UnicodeString)": expected true, got false");
                p++;
            }
            else {
                if (isB)
                    errln((UnicodeString)"Wrong result from isBoundary() for " + i + (UnicodeString)": expected false, got true");
            }
        }
}

void RBBITest::doMultipleSelectionTest(RuleBasedBreakIterator& iterator,
                                                  UnicodeString& testText)
{
    iterator.setText(&testText);
    
    RuleBasedBreakIterator* testIterator =(RuleBasedBreakIterator*)iterator.clone();
    int32_t offset = iterator.first();
    int32_t testOffset;
    int32_t count = 0;

    logln("doMultipleSelectionTest text of length: "+testText.length());

    if (*testIterator != iterator)
        errln("clone() or operator!= failed: two clones compared unequal");
    
    do {
        testOffset = testIterator->first();
        testOffset = testIterator->next(count);
        if (offset != testOffset)
            errln(UnicodeString("next(n) and next() not returning consistent results: for step ") + count + ", next(n) returned " + testOffset + " and next() had " + offset);

        if (offset != RuleBasedBreakIterator::DONE) {
            count++;
            offset = iterator.next();

            if (offset != RuleBasedBreakIterator::DONE && *testIterator == iterator)
                errln("operator== failed: Two unequal iterators compared equal.");
        }
    } while (offset != RuleBasedBreakIterator::DONE);

    // now do it backwards...
    offset = iterator.last();
    count = 0;

    do {
        testOffset = testIterator->last();
        testOffset = testIterator->next(count);
        if (offset != testOffset)
            errln(UnicodeString("next(n) and next() not returning consistent results: for step ") + count + ", next(n) returned " + testOffset + " and next() had " + offset);

        if (offset != RuleBasedBreakIterator::DONE) {
            count--;
            offset = iterator.previous();
        }
    } while (offset != RuleBasedBreakIterator::DONE);

    //delete testIterator;
}



