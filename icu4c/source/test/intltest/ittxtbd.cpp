/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "intltest.h"
#include "unicode/brkiter.h"
#include "unicode/unicode.h"
#include <stdio.h>
//#include "txbdapi.h"    // BreakIteratorAPIC

//--------------------------------------------------------------------------------------
/**
 * "Vector" class for holding test tables
 * (this class is actually a linked list, but we use the name and API of the
 * java.util.Vector class to keep as much of our test code as possible the same.)
 */
class Enumeration { // text enumeration
public:
  virtual UBool hasMoreElements() = 0;
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
    
    UBool hasMoreElements() { return fPos->fLink != &fVector->fBase; }
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
 * IntlTestTextBoundary is medium top level test class for everything in the directory "findword".
 */

#include "unicode/utypes.h"
#include "ittxtbd.h"

#include <string.h>
#include "unicode/schriter.h"

const UChar IntlTestTextBoundary::cannedTestArray[] = {
    0x0001, 0x0002, 0x0003, 0x0004, 0x0020, 0x0021, '\\', 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0028, 0x0029, 0x002b, 0x002d, 0x0030, 0x0031,
    0x0032, 0x0033, 0x0034, 0x003c, 0x003d, 0x003e, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x005b, 0x005d, 0x005e, 0x005f, 0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x007b,
    0x007d, 0x007c, 0x002c, 0x00a0, 0x00a2,
    0x00a3, 0x00a4, 0x00a5, 0x00a6, 0x00a7, 0x00a8, 0x00a9, 0x00ab, 0x00ad, 0x00ae, 0x00af, 0x00b0, 0x00b2, 0x00b3, 
    0x00b4, 0x00b9, 0x00bb, 0x00bc, 0x00bd, 0x02b0, 0x02b1, 0x02b2, 0x02b3, 0x02b4, 0x0300, 0x0301, 0x0302, 0x0303,
    0x0304, 0x05d0, 0x05d1, 0x05d2, 0x05d3, 0x05d4, 0x0903, 0x093e, 0x093f, 0x0940, 0x0949, 0x0f3a, 0x0f3b, 0x2000,
    0x2001, 0x2002, 0x200c, 0x200d, 0x200e, 0x200f, 0x2010, 0x2011, 0x2012, 0x2028, 0x2029, 0x202a, 0x203e, 0x203f,
    0x2040, 0x20dd, 0x20de, 0x20df, 0x20e0, 0x2160, 0x2161, 0x2162, 0x2163, 0x2164, 0x0000
};

UnicodeString* IntlTestTextBoundary::cannedTestChars = 0;

//---------------------------------------------
// setup methods
//---------------------------------------------

IntlTestTextBoundary::IntlTestTextBoundary()
{
    UnicodeString temp(cannedTestArray);
    cannedTestChars = new UnicodeString();
    *cannedTestChars += (UChar)0x0000;
    *cannedTestChars += temp;
    addTestWordData();
    addTestSentenceData();
    addTestLineData();
    addTestCharacterData();
}

IntlTestTextBoundary::~IntlTestTextBoundary()
{
    delete wordSelectionData;
    delete sentenceSelectionData;
    delete lineSelectionData;
    delete characterSelectionData;
    delete cannedTestChars;
}

/**
 * @bug 4097779 4098467 4117554
 */
void IntlTestTextBoundary::addTestWordData()
{
    wordSelectionData = new Vector();

    wordSelectionData->addElement("12,34");

    wordSelectionData->addElement(" ");
    wordSelectionData->addElement(UCharToUnicodeString((UChar)(0x00A2)));   //cent sign
    wordSelectionData->addElement(UCharToUnicodeString((UChar)(0x00A3)));   //pound sign
    wordSelectionData->addElement(UCharToUnicodeString((UChar)(0x00A4)));   //currency sign
    wordSelectionData->addElement(UCharToUnicodeString((UChar)(0x00A5)));   //yen sign
    wordSelectionData->addElement("alpha-beta-gamma");
    wordSelectionData->addElement(".");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("Badges");
    wordSelectionData->addElement("?");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("BADGES");
    wordSelectionData->addElement("!");
    wordSelectionData->addElement("?");
    wordSelectionData->addElement("!");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("We");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("don't");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("need");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("no");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("STINKING");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("BADGES");
    wordSelectionData->addElement("!");
    wordSelectionData->addElement("!");
    wordSelectionData->addElement("!");

    wordSelectionData->addElement("012.566,5");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("123.3434,900");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("1000,233,456.000");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("1,23.322%");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("123.1222");

    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("$123,000.20");

    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("179.01%");

    wordSelectionData->addElement("Hello");
    wordSelectionData->addElement(",");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("how");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("are");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("you");
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement("X");
    wordSelectionData->addElement(" ");

    wordSelectionData->addElement("Now");
    wordSelectionData->addElement("\r");
    wordSelectionData->addElement("is");
    wordSelectionData->addElement("\n");
    wordSelectionData->addElement("the");
    wordSelectionData->addElement("\r\n");
    wordSelectionData->addElement("time");
    wordSelectionData->addElement("\n");
    wordSelectionData->addElement("\r");
    wordSelectionData->addElement("for");
    wordSelectionData->addElement("\r");
    wordSelectionData->addElement("\r");
    wordSelectionData->addElement("all");
    wordSelectionData->addElement(" ");

    // to test for bug #4097779
    wordSelectionData->addElement(CharsToUnicodeString("aa\\u0300a"));
    wordSelectionData->addElement(" ");

    // to test for bug #4098467
    // What follows is a string of Korean characters (I found it in the Yellow Pages
    // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
    // it correctly), first as precomposed syllables, and then as conjoining jamo.
    // Both sequences should be semantically identical and break the same way.
    // precomposed syllables...
    wordSelectionData->addElement(CharsToUnicodeString("\\uc0c1\\ud56d"));
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement(CharsToUnicodeString("\\ud55c\\uc778"));
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement(CharsToUnicodeString("\\uc5f0\\ud569"));
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement(CharsToUnicodeString("\\uc7a5\\ub85c\\uad50\\ud68c"));
    wordSelectionData->addElement(" ");
    // conjoining jamo...
    wordSelectionData->addElement(CharsToUnicodeString("\\u1109\\u1161\\u11bc\\u1112\\u1161\\u11bc"));
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11ab\\u110b\\u1175\\u11ab"));
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement(CharsToUnicodeString("\\u110b\\u1167\\u11ab\\u1112\\u1161\\u11b8"));
    wordSelectionData->addElement(" ");
    wordSelectionData->addElement(CharsToUnicodeString("\\u110c\\u1161\\u11bc\\u1105\\u1169\\u1100\\u116d\\u1112\\u116c"));
    wordSelectionData->addElement(" ");

    // this is a test for bug #4117554: the ideographic iteration mark (U+3005) should
    // count as a Kanji character for the purposes of word breaking
    wordSelectionData->addElement("abc");
    wordSelectionData->addElement(CharsToUnicodeString("\\u4e01\\u4e02\\u3005\\u4e03\\u4e03"));
    wordSelectionData->addElement("abc");

    
}

const UChar kParagraphSeparator = 0x2029;
const UChar kLineSeparator = 0x2028;

/**
 * @bug 4111338 4117554 4113835
 */
void IntlTestTextBoundary::addTestSentenceData()
{
    sentenceSelectionData = new Vector();
    sentenceSelectionData->addElement("This is a simple sample sentence. ");
    sentenceSelectionData->addElement("(This is it.) ");
    sentenceSelectionData->addElement("This is a simple sample sentence. ");
    sentenceSelectionData->addElement("\"This isn\'t it.\" ");
    sentenceSelectionData->addElement("Hi! ");
    sentenceSelectionData->addElement("This is a simple sample sentence. ");
    sentenceSelectionData->addElement("It does not have to make any sense as you can see. ");
    sentenceSelectionData->addElement("Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura. ");
    sentenceSelectionData->addElement("Che la dritta via aveo smarrita. ");
    sentenceSelectionData->addElement("He said, that I said, that you said!! ");

    sentenceSelectionData->addElement("Don't rock the boat." + UCharToUnicodeString(kParagraphSeparator));

    sentenceSelectionData->addElement("Because I am the daddy, that is why. ");
    sentenceSelectionData->addElement("Not on my time (el timo.)! ");

    sentenceSelectionData->addElement("So what!!" + UCharToUnicodeString(kParagraphSeparator));

    sentenceSelectionData->addElement("\"But now,\" he said, \"I know!\" ");
    sentenceSelectionData->addElement("Harris thumbed down several, including \"Away We Go\" (which became the huge success Oklahoma!). ");
    sentenceSelectionData->addElement("One species, B. anthracis, is highly virulent.\n");
    sentenceSelectionData->addElement("Wolf said about Sounder:\"Beautifully thought-out and directed.\" ");
    sentenceSelectionData->addElement("Have you ever said, \"This is where\tI shall live\"? ");
    sentenceSelectionData->addElement("He answered, \"You may not!\" ");
    sentenceSelectionData->addElement("Another popular saying is: \"How do you do?\". ");
    sentenceSelectionData->addElement("Yet another popular saying is: \'I\'m fine thanks.\' ");
    sentenceSelectionData->addElement("What is the proper use of the abbreviation pp.? ");
    sentenceSelectionData->addElement("Yes, I am definatelly 12\" tall!!");

    // test for bug #4113835: \n and \r count as spaces, not as paragraph breaks
    sentenceSelectionData->addElement(CharsToUnicodeString("Now\ris\nthe\r\ntime\n\rfor\r\rall\\u2029"));

    // test for bug #4111338: Don't break sentences at the boundary between CJK
    // and other letters
    sentenceSelectionData->addElement(CharsToUnicodeString("\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165:\"JAVA\\u821c")
        + CharsToUnicodeString("\\u8165\\u7fc8\\u51ce\\u306d,\\u2494\\u56d8\\u4ec0\\u60b1\\u8560\\u51ba")
        + CharsToUnicodeString("\\u611d\\u57b6\\u2510\\u5d46\".\\u2029"));
    sentenceSelectionData->addElement(CharsToUnicodeString("\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8")
        + CharsToUnicodeString("\\u97e4JAVA\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8\\u4ec0")
        + CharsToUnicodeString("\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u2029"));
    sentenceSelectionData->addElement(CharsToUnicodeString("\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8\\u97e4")
        + CharsToUnicodeString("\\u6470\\u8790JAVA\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8")
        + CharsToUnicodeString("\\u4ec0\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u2029"));
    sentenceSelectionData->addElement(CharsToUnicodeString("He said, \"I can go there.\"\\u2029"));

    // test for bug #4117554: Treat fullwidth variants of .!? the same as their
    // normal counterparts
    sentenceSelectionData->addElement(CharsToUnicodeString("I know I'm right\\uff0e "));
    sentenceSelectionData->addElement(CharsToUnicodeString("Right\\uff1f "));
    sentenceSelectionData->addElement(CharsToUnicodeString("Right\\uff01 "));

    // test for bug #4117554: Don't break sentences at boundary between CJK and digits
    sentenceSelectionData->addElement(CharsToUnicodeString("\\u5487\\u67ff\\ue591\\u5017\\u61b3\\u60a1\\u9510\\u8165\\u9de8")
        + CharsToUnicodeString("\\u97e48888\\u821c\\u8165\\u7fc8\\u51ce\\u306d\\ue30b\\u2494\\u56d8\\u4ec0")
        + CharsToUnicodeString("\\u60b1\\u8560\\u51ba\\u611d\\u57b6\\u2510\\u5d46\\u97e5\\u7751\\u2029"));

    // test for bug #4117554: Break sentence between a sentence terminator and
    // opening punctuation
    sentenceSelectionData->addElement("no?");
    sentenceSelectionData->addElement("(yes)" + CharsToUnicodeString("\\u2029"));

    // test for bug #4158381: Don't break sentence after period if it isn't
    // followed by a space
    sentenceSelectionData->addElement("Test <code>Flags.Flag</code> class.  ");
    sentenceSelectionData->addElement("Another test." + CharsToUnicodeString("\\u2029"));

    // test for bug #4158381: No breaks when there are no terminators around
    sentenceSelectionData->addElement("<P>Provides a set of &quot;lightweight&quot; (all-java<FONT SIZE=\"-2\"><SUP>TM</SUP></FONT> language) components that, to the maximum degree possible, work the same on all platforms.  ");
    sentenceSelectionData->addElement("Another test." + CharsToUnicodeString("\\u2029"));

    // test for bug #4143071: Make sure sentences that end with digits
    // work right
    sentenceSelectionData->addElement("Today is the 27th of May, 1998.  ");
    sentenceSelectionData->addElement("Tomorrow with be 28 May 1998.  ");
    sentenceSelectionData->addElement("The day after will be the 30th." 
                                        + CharsToUnicodeString("\\u2029"));

    // test for bug #4152416: Make sure sentences ending with a capital
    // letter are treated correctly
    sentenceSelectionData->addElement("The type of all primitive <code>boolean</code> values accessed in the target VM.  ");
    sentenceSelectionData->addElement("Calls to xxx will return an implementor of this interface." + CharsToUnicodeString("\\u2029"));

    // test for bug #4152117: Make sure sentence breaking is handling
    // punctuation correctly [COULD NOT REPRODUCE THIS BUG, BUT TEST IS
    // HERE TO MAKE SURE IT DOESN'T CROP UP]
    sentenceSelectionData->addElement("Constructs a randomly generated BigInteger, uniformly distributed over the range <tt>0</tt> to <tt>(2<sup>numBits</sup> - 1)</tt>, inclusive.  ");
    sentenceSelectionData->addElement("The uniformity of the distribution assumes that a fair source of random bits is provided in <tt>rnd</tt>.  ");
    sentenceSelectionData->addElement("Note that this constructor always constructs a non-negative BigInteger." + CharsToUnicodeString("\\u2029"));

}

/**
 * @bug 4068133 4086052 4035266 4097920 4098467 4117554
 */
void IntlTestTextBoundary::addTestLineData()
{
    lineSelectionData = new Vector();
    lineSelectionData->addElement("Multi-");
    lineSelectionData->addElement("Level ");
    lineSelectionData->addElement("example ");
    lineSelectionData->addElement("of ");
    lineSelectionData->addElement("a ");
    lineSelectionData->addElement("semi-");
    lineSelectionData->addElement("idiotic ");
    lineSelectionData->addElement("non-");
    lineSelectionData->addElement("sensical ");
    lineSelectionData->addElement("(non-");
    lineSelectionData->addElement("important) ");
    lineSelectionData->addElement("sentence. ");

    lineSelectionData->addElement("Hi  ");
    lineSelectionData->addElement("Hello ");
    lineSelectionData->addElement("How\n");
    lineSelectionData->addElement("are\r");
    lineSelectionData->addElement("you" + UCharToUnicodeString(kLineSeparator));
    lineSelectionData->addElement("fine.\t");
    lineSelectionData->addElement("good.  ");

    lineSelectionData->addElement("Now\r");
    lineSelectionData->addElement("is\n");
    lineSelectionData->addElement("the\r\n");
    lineSelectionData->addElement("time\n");
    lineSelectionData->addElement("\r");
    lineSelectionData->addElement("for\r");
    lineSelectionData->addElement("\r");
    lineSelectionData->addElement("all");

    // to test for bug #4068133
    lineSelectionData->addElement(CharsToUnicodeString("\\u96f6"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u4e00\\u3002"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u4e8c\\u3001"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u4e09\\u3002\\u3001"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u56db\\u3001\\u3002\\u3001"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u4e94,"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u516d."));
    lineSelectionData->addElement(CharsToUnicodeString("\\u4e03.\\u3001,\\u3002"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u516b"));

    // to test for bug #4086052
    lineSelectionData->addElement(CharsToUnicodeString("foo\\u00a0bar "));
//        lineSelectionData->addElement("foo\\ufeffbar");

    // to test for bug #4097920
    lineSelectionData->addElement("dog,");
    lineSelectionData->addElement("cat,");
    lineSelectionData->addElement("mouse ");
    lineSelectionData->addElement("(one)");
    lineSelectionData->addElement("(two)\n");

    // to test for bug #4035266
    lineSelectionData->addElement("The ");
    lineSelectionData->addElement("balance ");
    lineSelectionData->addElement("is ");
    lineSelectionData->addElement("$-23,456.78, ");
    lineSelectionData->addElement("not ");
    lineSelectionData->addElement("-$32,456.78!\n");

    // to test for bug #4098467
    // What follows is a string of Korean characters (I found it in the Yellow Pages
    // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
    // it correctly), first as precomposed syllables, and then as conjoining jamo.
    // Both sequences should be semantically identical and break the same way.
    // precomposed syllables...
    lineSelectionData->addElement(CharsToUnicodeString("\\uc0c1\\ud56d "));
    lineSelectionData->addElement(CharsToUnicodeString("\\ud55c\\uc778 "));
    lineSelectionData->addElement(CharsToUnicodeString("\\uc5f0\\ud569 "));
    lineSelectionData->addElement(CharsToUnicodeString("\\uc7a5\\ub85c\\uad50\\ud68c "));
    // conjoining jamo...
    lineSelectionData->addElement(CharsToUnicodeString("\\u1109\\u1161\\u11bc\\u1112\\u1161\\u11bc "));
    lineSelectionData->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11ab\\u110b\\u1175\\u11ab "));
    lineSelectionData->addElement(CharsToUnicodeString("\\u110b\\u1167\\u11ab\\u1112\\u1161\\u11b8 "));
    lineSelectionData->addElement(CharsToUnicodeString("\\u110c\\u1161\\u11bc\\u1105\\u1169\\u1100\\u116d\\u1112\\u116c"));

    // to test for bug #4117554: Fullwidth .!? should be treated as postJwrd
    lineSelectionData->addElement(CharsToUnicodeString("\\u4e01\\uff0e"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u4e02\\uff01"));
    lineSelectionData->addElement(CharsToUnicodeString("\\u4e03\\uff1f"));

}

/*
const UnicodeString graveS = "S" + (UChar)0x0300;
const UnicodeString acuteBelowI = "i" + UCharToUnicodeString(0x0317);
const UnicodeString acuteE = "e" + UCharToUnicodeString(0x0301);
const UnicodeString circumflexA = "a" + UCharToUnicodeString(0x0302);
const UnicodeString tildeE = "e" + UCharToUnicodeString(0x0303);
*/

/**
 * @bug 4098467
 */
void IntlTestTextBoundary::addTestCharacterData()
{
    characterSelectionData = new Vector();
    characterSelectionData->addElement("S" + UCharToUnicodeString(0x0300)); //graveS
    characterSelectionData->addElement("i" + UCharToUnicodeString(0x0301)); // acuteBelowI
    characterSelectionData->addElement("m");
    characterSelectionData->addElement("p");
    characterSelectionData->addElement("l");
    characterSelectionData->addElement("e" + UCharToUnicodeString(0x0301));  // acuteE
    characterSelectionData->addElement(" ");
    characterSelectionData->addElement("s");
    characterSelectionData->addElement("a" + UCharToUnicodeString(0x0302));  // circumflexA
    characterSelectionData->addElement("m");
    characterSelectionData->addElement("p");
    characterSelectionData->addElement("l");
    characterSelectionData->addElement("e" + UCharToUnicodeString(0x0303));  // tildeE
    characterSelectionData->addElement(".");
    characterSelectionData->addElement("w");
    characterSelectionData->addElement("a" + UCharToUnicodeString(0x0302));  // circumflexA
    characterSelectionData->addElement("w");
    characterSelectionData->addElement("a");
    characterSelectionData->addElement("f");
    characterSelectionData->addElement("q");
    characterSelectionData->addElement("\n");
    characterSelectionData->addElement("\r");
    characterSelectionData->addElement("\r\n");
    characterSelectionData->addElement("\n");

    // to test for bug #4098467
    // What follows is a string of Korean characters (I found it in the Yellow Pages
    // ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
    // it correctly), first as precomposed syllables, and then as conjoining jamo.
    // Both sequences should be semantically identical and break the same way.
    // precomposed syllables...
    characterSelectionData->addElement(CharsToUnicodeString("\\uc0c1"));
    characterSelectionData->addElement(CharsToUnicodeString("\\ud56d"));
    characterSelectionData->addElement(" ");
    characterSelectionData->addElement(CharsToUnicodeString("\\ud55c"));
    characterSelectionData->addElement(CharsToUnicodeString("\\uc778"));
    characterSelectionData->addElement(" ");
    characterSelectionData->addElement(CharsToUnicodeString("\\uc5f0"));
    characterSelectionData->addElement(CharsToUnicodeString("\\ud569"));
    characterSelectionData->addElement(" ");
    characterSelectionData->addElement(CharsToUnicodeString("\\uc7a5"));
    characterSelectionData->addElement(CharsToUnicodeString("\\ub85c"));
    characterSelectionData->addElement(CharsToUnicodeString("\\uad50"));
    characterSelectionData->addElement(CharsToUnicodeString("\\ud68c"));
    characterSelectionData->addElement(" ");
    // conjoining jamo...
    characterSelectionData->addElement(CharsToUnicodeString("\\u1109\\u1161\\u11bc"));
    characterSelectionData->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11bc"));
    characterSelectionData->addElement(" ");
    characterSelectionData->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11ab"));
    characterSelectionData->addElement(CharsToUnicodeString("\\u110b\\u1175\\u11ab"));
    characterSelectionData->addElement(" ");
    characterSelectionData->addElement(CharsToUnicodeString("\\u110b\\u1167\\u11ab"));
    characterSelectionData->addElement(CharsToUnicodeString("\\u1112\\u1161\\u11b8"));
    characterSelectionData->addElement(" ");
    characterSelectionData->addElement(CharsToUnicodeString("\\u110c\\u1161\\u11bc"));
    characterSelectionData->addElement(CharsToUnicodeString("\\u1105\\u1169"));
    characterSelectionData->addElement(CharsToUnicodeString("\\u1100\\u116d"));
    characterSelectionData->addElement(CharsToUnicodeString("\\u1112\\u116c"));

}

UnicodeString IntlTestTextBoundary::createTestData(Enumeration* e)
{
    UnicodeString result = "";

    while (e->hasMoreElements()) {
        result += e->nextElement();
    }
    return result;
}

//---------------------------------------------
// SentenceBreak tests
//---------------------------------------------

void IntlTestTextBoundary::TestSentenceIteration()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator* e = BreakIterator::createSentenceInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestSentenceIteration.\n");
        return;
    }
    generalIteratorTest(*e, sentenceSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestSentenceInvariants()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *e = BreakIterator::createSentenceInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestSentenceInvariant.\n");
        return;
    }
    UnicodeString s = *cannedTestChars + CharsToUnicodeString(".,\\u3001\\u3002\\u3041\\u3042\\u3043\\ufeff");
    doOtherInvariantTest(*e, s);
    delete e;
}
//---------------------------------------------
// WordBreak tests
//---------------------------------------------
void IntlTestTextBoundary::TestWordIteration()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator* e = BreakIterator::createWordInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestWordIteration.\n");
        return;
    }
    generalIteratorTest(*e, wordSelectionData);
    delete e;
}
void IntlTestTextBoundary::TestWordInvariants()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *e = BreakIterator::createWordInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestWordInvariants.\n");
        return;
    }
    UnicodeString s = *cannedTestChars + CharsToUnicodeString("\',.\\u3041\\u3042\\u3043\\u309b\\u309c\\u30a1\\u30a2\\u30a3\\u4e00\\u4e01\\u4e02");
    doBreakInvariantTest(*e, s);
    s = *cannedTestChars + CharsToUnicodeString("\',.\\u3041\\u3042\\u3043\\u309b\\u309c\\u30a1\\u30a2\\u30a3\\u4e00\\u4e01\\u4e02");
    doOtherInvariantTest(*e, s);
    delete e;
}
//---------------------------------------------
// CharacterBreak tests
//---------------------------------------------
void IntlTestTextBoundary::TestCharacterIteration()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator* e = BreakIterator::createCharacterInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestCharacterIteration.\n");
        return;
    }
    // generalIteratorTest(*e, testCharacterText, characterSelectionData);
    generalIteratorTest(*e, characterSelectionData);
    delete e;
}
void IntlTestTextBoundary::TestCharacterInvariants()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *e = BreakIterator::createCharacterInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestCharacterInvariants.\n");
        return;
    }
    UnicodeString s = *cannedTestChars + CharsToUnicodeString("\\u1100\\u1101\\u1102\\u1160\\u1161\\u1162\\u11a8\\u11a9\\u11aa");
    doBreakInvariantTest(*e, s);
    s = *cannedTestChars + CharsToUnicodeString("\\u1100\\u1101\\u1102\\u1160\\u1161\\u1162\\u11a8\\u11a9\\u11aa");
    doOtherInvariantTest(*e, s);
    delete e;
}
//---------------------------------------------
// LineBreak tests
//---------------------------------------------
void IntlTestTextBoundary::TestLineIteration()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator* e = BreakIterator::createLineInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestLineIteration.\n");
        return;
    }
    generalIteratorTest(*e, lineSelectionData);
    delete e;
}
void IntlTestTextBoundary::TestLineInvariants()
{
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *e = BreakIterator::createLineInstance(Locale::US, status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestLineInvariants.\n");
        return;
    }
    UnicodeString s = CharsToUnicodeString(".,;:\\u3001\\u3002\\u3041\\u3042\\u3043\\u3044\\u3045\\u30a3\\u4e00\\u4e01\\u4e02");
    UnicodeString testChars = *cannedTestChars + s;
    doBreakInvariantTest(*e, testChars);
    doOtherInvariantTest(*e, testChars);

    int errorCount = 0;
    UTextOffset i, j, k;

    // in addition to the other invariants, a line-break iterator should make sure that:
    // it doesn't break around the non-breaking characters
    UnicodeString noBreak = CharsToUnicodeString("\\u00a0\\u2007\\u2011\\ufeff");
    UnicodeString work("aaa");
    for (i = 0; i < testChars.length(); i++) {
        UChar c = testChars[i];
        if (c == '\r' || c == '\n' || c == 0x2029 || c == 0x2028 || c == 0x0003)
            continue;
        work[0] = c;
        for (j = 0; j < noBreak.length(); j++) {
            work[1] = noBreak[j];
            for (k = 0; k < testChars.length(); k++) {
                work[2] = testChars[k];
                e->setText(&work);
                for (int l = e->first(); l != BreakIterator::DONE; l = e->next())
                    if (l == 1 || l == 2) {
                        errln("Got break between U+" + UCharToUnicodeString(work[l - 1]) + 
                            " and U+" + UCharToUnicodeString(work[l]));
                        errorCount++;
                        if (errorCount >= 75)
                            return;
                    }
            }
        }
    }

    // it does break after hyphens (unless they're followed by a digit, a non-spacing mark,
    // a currency symbol, a non-breaking space, or a line or paragraph separator)
    UnicodeString dashes = CharsToUnicodeString("-\\u00ad\\u2010\\u2012\\u2013\\u2014");
    for (i = 0; i < testChars.length(); i++) {
        work[0] = testChars[i];
        for (j = 0; j < dashes.length(); j++) {
            work[1] = dashes[j];
            for (k = 0; k < testChars.length(); k++) {
                UChar c = testChars[k];
                if (Unicode::getType(c) == Unicode::DECIMAL_DIGIT_NUMBER ||
                    Unicode::getType(c) == Unicode::OTHER_NUMBER ||
                    Unicode::getType(c) == Unicode::NON_SPACING_MARK ||
                    Unicode::getType(c) == Unicode::ENCLOSING_MARK ||
                    Unicode::getType(c) == Unicode::CURRENCY_SYMBOL ||
                    Unicode::getType(c) == Unicode::SPACE_SEPARATOR ||
                    Unicode::getType(c) == Unicode::DASH_PUNCTUATION ||
                    Unicode::getType(c) == Unicode::CONTROL ||
                    Unicode::getType(c) == Unicode::FORMAT ||
                    c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029 ||
                    c == 0x0003 || c == 0x00a0 || c == 0x2007 || c == 0x2011 ||
                    c == 0xfeff)
                    continue;
                work[2] = c;
                e->setText(&work);
                UBool saw2 = FALSE;
                for (int l = e->first(); l != BreakIterator::DONE; l = e->next())
                    if (l == 2)
                        saw2 = TRUE;
                if (!saw2) {
                    errln("Didn't get break between U+" + UCharToUnicodeString(work[1]) + 
                        " and U+" + UCharToUnicodeString(work[2]));
                    errorCount++;
                    if (errorCount >= 75)
                        return;
                }
            }
        }
    }

}

void IntlTestTextBoundary::TestThaiLineBreak() {
        Vector* thaiLineSelection = new Vector();
        UErrorCode status = U_ZERO_ERROR;

        // \u0e2f-- the Thai paiyannoi character-- isn't a letter.  It's a symbol that
        // represents elided letters at the end of a long word.  It should be bound to
        // the end of the word and not treated as an independent punctuation mark.
        

        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e2a\\u0e16\\u0e32\\u0e19\\u0e35\\u0e2f"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e08\\u0e30"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e23\\u0e30\\u0e14\\u0e21"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e08\\u0e49\\u0e32"));
//        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e2b\\u0e19\\u0e49\\u0e32"));
//        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e17\\u0e35\\u0e48"));
thaiLineSelection->addElement(CharsToUnicodeString("\\u0e2b\\u0e19\\u0e49\\u0e32\\u0e17\\u0e35\\u0e48"));
// the commented-out lines (I think) are the preferred result; this line is what our current dictionary is giving us
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e2d\\u0e2d\\u0e01"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e21\\u0e32"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e23\\u0e48\\u0e07"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e23\\u0e30\\u0e1a\\u0e32\\u0e22"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e2d\\u0e22\\u0e48\\u0e32\\u0e07"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e15\\u0e47\\u0e21"));

        // the one time where the paiyannoi occurs somewhere other than at the end
        // of a word is in the Thai abbrevation for "etc.", which both begins and
        // ends with a paiyannoi
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e2f\\u0e25\\u0e2f"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e17\\u0e35\\u0e48"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e19\\u0e31\\u0e49\\u0e19"));

        BreakIterator* e = BreakIterator::createLineInstance(
                                                Locale("th"), status); 
        if (U_FAILURE(status))
        {
            errln("Failed to create the BreakIterator for default locale in TestThaiLineBreak.\n");
            return;
        }

        generalIteratorTest(*e, thaiLineSelection);
        delete e;
        delete thaiLineSelection;
    }

void IntlTestTextBoundary::TestMixedThaiLineBreak() 
{
        UErrorCode status = U_ZERO_ERROR;
        Vector*  thaiLineSelection= new Vector();

        // Arabic numerals should always be separated from surrounding Thai text
/*
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e04\\u0e48\\u0e32"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e07\\u0e34\\u0e19"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e1a\\u0e32\\u0e17"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e41\\u0e15\\u0e30"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e23\\u0e30\\u0e14\\u0e31\\u0e1a"));
        thaiLineSelection->addElement("39");
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e1a\\u0e32\\u0e17 "));

        // words in non-Thai scripts should always be separated from surrounding Thai text
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e17\\u0e14"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e2a\\u0e2d\\u0e1a"));
        thaiLineSelection->addElement("Java");
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e1a\\u0e19"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e04\\u0e23\\u0e37\\u0e48\\u0e2d\\u0e07"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e44\\u0e2d\\u0e1a\\u0e35\\u0e40\\u0e2d\\u0e47\\u0e21 "));

        // Thai numerals should always be separated from the text surrounding them
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e04\\u0e48\\u0e32"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e07\\u0e34\\u0e19"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e1a\\u0e32\\u0e17"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e41\\u0e15\\u0e30"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e23\\u0e30\\u0e14\\u0e31\\u0e1a"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e53\\u0e59"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e1a\\u0e32\\u0e17 "));

        // Thai text should interact correctly with punctuation and symbols
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e44\\u0e2d\\u0e1a\\u0e35\\u0e40\\u0e2d\\u0e47\\u0e21"));
//        thaiLineSelection->addElement(CharsToUnicodeString("(\\u0e1b\\u0e23\\u0e30\\u0e40\\u0e17\\u0e28"));
//        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e44\\u0e17\\u0e22)"));
thaiLineSelection->addElement(CharsToUnicodeString("(\\u0e1b\\u0e23\\u0e30\\u0e40\\u0e17\\u0e28\\u0e44\\u0e17\\u0e22)"));
// I believe the commented-out reading above to be the correct one, but this is what passes with our current dictionary
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e08\\u0e33\\u0e01\\u0e31\\u0e14"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e1b\\u0e34\\u0e14"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e15\\u0e31\\u0e27\""));
*/
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e2e\\u0e32\\u0e23\\u0e4c\\u0e14\\u0e14\\u0e34\\u0e2a\\u0e01\\u0e4c\""));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e23\\u0e38\\u0e48\\u0e19"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e43\\u0e2b\\u0e21\\u0e48"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e14\\u0e37\\u0e2d\\u0e19\\u0e21\\u0e34."));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e22."));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e19\\u0e35\\u0e49"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e23\\u0e32\\u0e04\\u0e32"));
        thaiLineSelection->addElement("$200");
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e17\\u0e48\\u0e32"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e19\\u0e31\\u0e49\\u0e19 "));
        thaiLineSelection->addElement(CharsToUnicodeString("(\"\\u0e2e\\u0e32\\u0e23\\u0e4c\\u0e14\\u0e14\\u0e34\\u0e2a\\u0e01\\u0e4c\")."));

        BreakIterator* e = BreakIterator::createLineInstance(
                                                Locale("th"), status); 
        if (U_FAILURE(status))
        {
            errln("Failed to create the BreakIterator for default locale in TestMixedThaiLineBreak.\n");
            return;
        }


        generalIteratorTest(*e, thaiLineSelection);
        delete e;
        delete thaiLineSelection;
}


void IntlTestTextBoundary::TestMaiyamok() 
{
        Vector*  thaiLineSelection= new Vector();
        UErrorCode status = U_ZERO_ERROR;
        // the Thai maiyamok character is a shorthand symbol that means "repeat the previous
        // word".  Instead of appearing as a word unto itself, however, it's kept together
        // with the word before it
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e44\\u0e1b\\u0e46"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e21\\u0e32\\u0e46"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e23\\u0e30\\u0e2b\\u0e27\\u0e48\\u0e32\\u0e07"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e01\\u0e23\\u0e38\\u0e07\\u0e40\\u0e17\\u0e1e"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e41\\u0e25\\u0e30"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e40\\u0e03\\u0e35\\u0e22\\u0e07"));
        thaiLineSelection->addElement(CharsToUnicodeString("\\u0e43\\u0e2b\\u0e21\\u0e48"));

        BreakIterator* e = BreakIterator::createLineInstance(
                                                Locale("th"), status); 

        if (U_FAILURE(status))
        {
            errln("Failed to create the BreakIterator for default locale in TestMaiyamok.\n");
            return;
        }
        generalIteratorTest(*e, thaiLineSelection);
        delete e;
        delete thaiLineSelection;
}

/**
 * Test Japanese Line Break
 * @bug 4095322
 */
void IntlTestTextBoundary::TestJapaneseLineBreak()
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString testString = CharsToUnicodeString("\\u4e00x\\u4e8c");
    UnicodeString precedingChars = CharsToUnicodeString("([{\\u00ab$\\u00a5\\u00a3\\u00a4\\u2018\\u201a\\u201c\\u201e\\u201b\\u201f");
    UnicodeString followingChars = CharsToUnicodeString(")]}\\u00bb!%,.\\u3001\\u3002\\u3063\\u3083\\u3085\\u3087\\u30c3\\u30e3\\u30e5\\u30e7\\u30fc:;\\u309b\\u309c\\u3005\\u309d\\u309e\\u30fd\\u30fe\\u2019\\u201d\\u00b0\\u2032\\u2033\\u2034\\u2030\\u2031\\u2103\\u2109\\u00a2\\u0300\\u0301\\u0302");
    BreakIterator *iter = BreakIterator::createLineInstance(Locale::JAPAN, status);

    UTextOffset i;
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestJapaneseLineBreak.\n");
        return;
    }

    for (i = 0; i < precedingChars.length(); i++) {
        testString[1] = precedingChars[i];
        iter->setText(&testString);
        int32_t j = iter->first();
        if (j != 0)
            errln("ja line break failure: failed to start at 0");
        j = iter->next();
        if (j != 1)
            errln("ja line break failure: failed to stop before '" + UCharToUnicodeString(precedingChars[i])
                        + "' (" + ((int)(precedingChars[i])) + ")");
        j = iter->next();
        if (j != 3)
            errln("ja line break failure: failed to skip position after '" + UCharToUnicodeString(precedingChars[i])
                        + "' (" + ((int)(precedingChars[i])) + ")");
    }

    for (i = 0; i < followingChars.length(); i++) {
        testString[1] = followingChars[i];
        iter->setText(&testString);
        int j = iter->first();
        if (j != 0)
            errln("ja line break failure: failed to start at 0");
        j = iter->next();
        if (j != 2)
            errln("ja line break failure: failed to skip position before '" + UCharToUnicodeString(followingChars[i])
                        + "' (" + ((int)(followingChars[i])) + ")");
        j = iter->next();
        if (j != 3)
            errln("ja line break failure: failed to stop after '" + UCharToUnicodeString(followingChars[i])
                        + "' (" + ((int)(followingChars[i])) + ")");
    }
    delete iter;
}

//---------------------------------------------
// other tests
//---------------------------------------------/

void IntlTestTextBoundary::TestEmptyString()
{
    UnicodeString text = "";
    Vector x;
    UErrorCode status = U_ZERO_ERROR;
    x.addElement(text);
    BreakIterator* bi = BreakIterator::createLineInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestEmptyString.\n");
        return;
    }
    generalIteratorTest(*bi, &x);
   
    delete bi;
}

void IntlTestTextBoundary::TestGetAvailableLocales()
{
    int32_t locCount = 0;
    const Locale* locList = BreakIterator::getAvailableLocales(locCount);

    if (locCount == 0)
        errln("getAvailableLocales() returned an empty list!");
    // Todo: I have no idea how to test this function...
}

//Testing the BreakIterator::getDisplayName() function 
void IntlTestTextBoundary::TestGetDisplayName()
{
    UnicodeString   result;
    
    BreakIterator::getDisplayName(Locale::US, result);
    if (Locale::getDefault() == Locale::US && result != "English (United States)")
        errln("BreakIterator::getDisplayName() failed: expected \"English (United States)\", got \""
                + result);

    BreakIterator::getDisplayName(Locale::FRANCE, Locale::US, result);
    if (result != "French (France)")
        errln("BreakIterator::getDisplayName() failed: expected \"French (France)\", got \""
                + result);
}
/**
 * Test End Behaviour
 * @bug 4068137
 */
void IntlTestTextBoundary::TestEndBehaviour()
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString testString("boo.");
    BreakIterator *wb = BreakIterator::createWordInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestEndBehaviour.\n");
        return;
    }
    wb->setText(&testString);

    if (wb->first() != 0)
        errln("Didn't get break at beginning of string.");
    if (wb->next() != 3)
        errln("Didn't get break before period in \"boo.\"");
    if (wb->current() != 4 && wb->next() != 4)
        errln("Didn't get break at end of string.");
    delete wb;
}
/*
 * @bug 4153072
 */
void IntlTestTextBoundary::TestBug4153072() {
    UErrorCode status = U_ZERO_ERROR;
    BreakIterator *iter = BreakIterator::createWordInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestBug4153072\n");
        return;
    }
    UnicodeString str("...Hello, World!...");
    int32_t begin = 3;
    int32_t end = str.length() - 3;
    UBool dummy;

    StringCharacterIterator* textIterator = new StringCharacterIterator(str, begin, end, begin);
    iter->adoptText(textIterator);
    for (int index = -1; index < begin + 1; ++index) {
        dummy = iter->isBoundary(index);
        if (index < begin && dummy == TRUE) {
            errln((UnicodeString)"Didn't handle preceeding correctly with offset = " + index +
                            " and begin index = " + begin);
        }
    }
    delete iter;
}
/*
 * Test Preceding()
 */
void IntlTestTextBoundary::TestPreceding()
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeString words3("aaa bbb ccc");
    BreakIterator* e = BreakIterator::createWordInstance(Locale::getDefault(), status);
    if (U_FAILURE(status))
    {
        errln("Failed to create the BreakIterator for default locale in TestPreceeding.\n");
        return;
    }

    e->setText( &words3 );
    e->first();
    UTextOffset p1 = e->next();
    UTextOffset p2 = e->next();
    UTextOffset p3 = e->next();
    UTextOffset p4 = e->next();
    UTextOffset f = e->following( p2+1 );
    UTextOffset p = e->preceding( p2+1 );
    if (f!=p3) errln("IntlTestTextBoundary::TestPreceding: f!=p3");
    if (p!=p2) errln("IntlTestTextBoundary::TestPreceding: p!=p2");
    if (!e->isBoundary(p2) || e->isBoundary(p2+1) || !e->isBoundary(p3))
    {
        errln("IntlTestTextBoundary::TestPreceding: isBoundary err");
    }
    delete e;
}
//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void IntlTestTextBoundary::runIndexedTest( int32_t index, UBool exec, const char* &name, char* par )
{
    if (exec) logln("TestSuite TextBoundary: ");
    switch (index) {
        case 0: name = "TestSentenceIteration"; if(exec) TestSentenceIteration(); break;
        case 1: name = "TestWordIteration"; if(exec) TestWordIteration(); break;
        case 2: name = "TestLineIteration"; if(exec) TestLineIteration(); break;
        case 3: name = "TestCharacterIteration"; if(exec) TestCharacterIteration(); break;
        case 4: name = "TestSentenceInvariants"; if(exec) TestSentenceInvariants();break;
        case 5: name = "TestWordInvariants"; if(exec) TestWordInvariants();break;
        case 6: name = "TestLineInvariants"; if(exec) TestLineInvariants();break;
        case 7: name = "TestCharacterInvariants"; if(exec) TestCharacterInvariants();break;

        case 8: name = "TestEmptyString"; if (exec) TestEmptyString(); break;
        case 9: name = "TestGetAvailableLocales"; if (exec) TestGetAvailableLocales(); break;
        case 10: name = "TestGetDisplayName"; if (exec) TestGetDisplayName(); break;
        case 11: name = "TestPreceding"; if (exec) TestPreceding(); break;
        case 12: name = "TestBug4153072"; if (exec) TestBug4153072(); break;
        case 13: name = "TestEndBehaviour"; if (exec) TestEndBehaviour(); break;


        case 14: name = "TestJapaneseLineBreak"; if (exec) TestJapaneseLineBreak(); break;
        case 15: name = "TestThaiLineBreak"; if(exec) TestThaiLineBreak(); break;
        case 16: name = "TestMixedThaiLineBreak"; if(exec) TestMixedThaiLineBreak(); break;
        case 17: name = "TestMaiyamok"; if(exec) TestMaiyamok(); break;



        default: name = ""; break; //needed to end loop
    }
}

//---------------------------------------------
// Test implementation routines
//---------------------------------------------

// general test Implementation subroutines
void IntlTestTextBoundary::generalIteratorTest(BreakIterator& bi, Vector* expectedResult) 
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
    boundaries[0] = BreakIterator::DONE;
    boundaries[1] = 0;
    for (int i = 0; i < expectedResult->size(); i++)
        boundaries[i + 2] = boundaries[i + 1] + ((UnicodeString)expectedResult->elementAt(i)).
                        length();

    int len = expectedResult->size() + 3 -1;
    boundaries[len] = BreakIterator::DONE;

    testFollowing(bi, text, boundaries);
    testPreceding(bi, text, boundaries);
    testIsBoundary(bi, text, boundaries);

    doMultipleSelectionTest(bi, text);
}

Vector* IntlTestTextBoundary::testFirstAndNext(BreakIterator& bi, UnicodeString& text) 
{
    int32_t p = bi.first();
    int32_t lastP = p;
    Vector *result = new Vector();
    UnicodeString selection;

    if (p != 0)
        errln((UnicodeString)"first() returned " + p + (UnicodeString)" instead of 0");
    while (p != BreakIterator::DONE) {
        p = bi.next();
        if (p != BreakIterator::DONE) {
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

Vector* IntlTestTextBoundary::testLastAndPrevious(BreakIterator& bi, UnicodeString& text) 
{
    int32_t p = bi.last();
    int32_t lastP = p;
    Vector *result = new Vector();
    UnicodeString selection;

    if (p != text.length())
        errln((UnicodeString)"last() returned " + p + (UnicodeString)" instead of " + text.length());
    while (p != BreakIterator::DONE) {
        p = bi.previous();
        if (p != BreakIterator::DONE) {
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

void IntlTestTextBoundary::compareFragmentLists(UnicodeString& f1Name, UnicodeString& f2Name, Vector* f1, Vector* f2) 
{
    int32_t p1 = 0;
    int32_t p2 = 0;
    UnicodeString s1;
    UnicodeString s2;
    int32_t t1 = 0;
    int32_t t2 = 0;
    UnicodeString target;

    while (p1 < f1->size() && p2 < f2->size()) {
        s1 = (UnicodeString)f1->elementAt(p1);
        s2 = (UnicodeString)f2->elementAt(p2);
        t1 += s1.length();
        t2 += s2.length();

        if (s1.compare(s2) == 0) {
            logln(prettify((UnicodeString)"   >" + s1 + (UnicodeString)"<", target));
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
                logln(prettify((UnicodeString)" *** >" + s1 + (UnicodeString)"<", target));
                ++p1;
            }
            logln("***** " + f2Name + " has:");
            while (p2 <= tempP2 && p2 < f2->size()) {
                s2 = (UnicodeString)f2->elementAt(p2);
                t2 += s2.length();
                logln(prettify(" ***** >" + s2 + "<", target));
                ++p2;
            }
            errln((UnicodeString)"Discrepancy between " + f1Name + (UnicodeString)" and " + f2Name);
        }
        }
}

void IntlTestTextBoundary::testFollowing(BreakIterator& bi, UnicodeString& text, int32_t *boundaries) 
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

void IntlTestTextBoundary::testPreceding(BreakIterator& bi, UnicodeString& text, int32_t *boundaries) {
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

void IntlTestTextBoundary::testIsBoundary(BreakIterator& bi, UnicodeString& text, int32_t *boundaries) {
    logln("testIsBoundary():");
    int p = 1;
    UBool isB;
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

void IntlTestTextBoundary::doMultipleSelectionTest(BreakIterator& iterator,
                                                   UnicodeString& testText)
{
    iterator.setText(&testText);
    
    BreakIterator* testIterator = iterator.clone();
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

        if (offset != BreakIterator::DONE) {
            count++;
            offset = iterator.next();

            if (offset != BreakIterator::DONE && *testIterator == iterator)
                errln("operator== failed: Two unequal iterators compared equal.");
        }
    } while (offset != BreakIterator::DONE);

    // now do it backwards...
    offset = iterator.last();
    count = 0;

    do {
        testOffset = testIterator->last();
        testOffset = testIterator->next(count);
        if (offset != testOffset)
            errln(UnicodeString("next(n) and next() not returning consistent results: for step ") + count + ", next(n) returned " + testOffset + " and next() had " + offset);

        if (offset != BreakIterator::DONE) {
            count--;
            offset = iterator.previous();
        }
    } while (offset != BreakIterator::DONE);
    delete testIterator;
}

void IntlTestTextBoundary::doBreakInvariantTest(BreakIterator& tb, UnicodeString& testChars)
{
    UnicodeString work("aaa");
    int errorCount = 0;

    // a break should always occur after CR (unless followed by LF), LF, PS, and LS
    UnicodeString breaks = CharsToUnicodeString("\r\n\\u2029\\u2028");
    UTextOffset i, j;

    for (i = 0; i < breaks.length(); i++) {
        work[1] = breaks[i];
        for (j = 0; j < testChars.length(); j++) {
            work[0] = testChars[j];
            for (int k = 0; k < testChars.length(); k++) {
                UChar c = testChars[k];

                // if a cr is followed by lf, ps, ls or etx, don't do the check (that's
                // not supposed to work)
                if (work[1] == '\r' && (c == '\n' || c == 0x2029
                        || c == 0x2028 || c == 0x0003))
                    continue;

                work[2] = testChars[k];
                tb.setText(&work);
                UBool seen2 = FALSE;
                for (int l = tb.first(); l != BreakIterator::DONE; l = tb.next()) {
                    if (l == 2)
                        seen2 = TRUE;
                }
                if (!seen2) {
                    errln("No break between U+" + UCharToUnicodeString(work[1])
                                + " and U+" + UCharToUnicodeString(work[2]));
                    errorCount++;
                    if (errorCount >= 75)
                        return;
                }
            }
        }
    }
}

void IntlTestTextBoundary::doOtherInvariantTest(BreakIterator& tb, UnicodeString& testChars)
{
    UnicodeString work("a\r\na");
    int32_t errorCount = 0;
    UTextOffset i, j;

    // a break should never occur between CR and LF
    for (i = 0; i < testChars.length(); i++) {
        work[0] = testChars[i];
        for (j = 0; j < testChars.length(); j++) {
            work[3] = testChars[j];
            tb.setText(&work);
            for (int32_t k = tb.first(); k != BreakIterator::DONE; k = tb.next())
                if (k == 2) {
                    errln("Break between CR and LF in string U+" + UCharToUnicodeString(work[0]) + 
                        ", U+d U+a U+" + UCharToUnicodeString(work[3]));
                    errorCount++;
                    if (errorCount >= 75)
                        return;
                }
        }
    }

    // a break should never occur before a non-spacing mark, unless the preceding
    // character is CR, LF, PS, or LS
    work.remove();
    work += "aaaa";
    for (i = 0; i < testChars.length(); i++) {
        UChar c = testChars[i];
        if (c == '\n' || c == '\r' || c == 0x2029 || c == 0x2028 || c == 0x0003)
            continue;
        work[1] = c;
        for (j = 0; j < testChars.length(); j++) {
            c = testChars[j];
            if ((Unicode::getType(c) != Unicode::NON_SPACING_MARK) && 
                (Unicode::getType(c) != Unicode::ENCLOSING_MARK))
                continue;
            work[2] = c;
            tb.setText(&work);
            for (int k = tb.first(); k != BreakIterator::DONE; k = tb.next())
                if (k == 2) {
                    errln("Break between U+" + UCharToUnicodeString(work[1])
                            + " and U+" + UCharToUnicodeString(work[2]));
                    errorCount++;
                    if (errorCount >= 75)
                        return;
                }
        }
    }
}

void IntlTestTextBoundary::sample(BreakIterator& tb,
                                  UnicodeString& text,
                                  UnicodeString& title)
{
    UnicodeString   substring;
    UBool verboseWas = verbose;
    verbose = TRUE;
    logln("-------------------------"+title+" length = "+text.length());
    tb.setText(&text);
    int32_t start = tb.first();
    int32_t end;
    for (end = tb.next(); end != BreakIterator::DONE; end = tb.next()) {
        text.extractBetween(start, end, substring);
        logln(UnicodeString("[")+start+","+end+"] \""+substring+"\"");
        start = end;
    }
    verbose = verboseWas;
}



