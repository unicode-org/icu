/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1997                                                 *
*   (C) Copyright International Business Machines Corporation,  1997-1998               *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
*/

#include "intltest.h"
#include "brkiter.h"
#include "unicode.h"
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

#include "utypes.h"
#include "ittxtbd.h"

#include <string.h>
#include "schriter.h"

// [HSYS] Just to make it easier to use with UChar array.
UnicodeString CharsToUnicodeString(const char* chars)
{
    int len = strlen(chars);
    int i;
    UnicodeString buffer;
    for (i = 0; i < len;) {
        if ((chars[i] == '\\') && (i+1 < len) && (chars[i+1] == 'u')) {
            int unicode;
            sscanf(&(chars[i+2]), "%4X", &unicode);
            buffer += (UChar)unicode;
            i += 6;
        } else {
            buffer += (UChar)chars[i++];
        }
    }
    return buffer;
}
 

const UChar IntlTestTextBoundary::cannedTestArray[] = {
    0x0001, 0x0002, 0x0003, 0x0004, ' ', '!', '\\', '"', '#', '$', '%', '&', '(', ')', '+', '-', '0', '1', 
    '2', '3', '4', '<', '=', '>', 'A', 'B', 'C', 'D', 'E', '[', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', '{', 
    '}', '|', ',',  0x00a0, 0x00a2,
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
    *cannedTestChars += 0x0000;
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

    Enumeration *elems = wordSelectionData->elements();
    testWordText = createTestData(elems);
    delete elems;
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

    Enumeration *elems = sentenceSelectionData->elements();
    testSentenceText = createTestData(elems);
    delete elems;
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

    Enumeration *elems = lineSelectionData->elements();
    testLineText = createTestData(elems);
    delete elems;
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

    Enumeration *elems = characterSelectionData->elements();
    testCharacterText = createTestData(elems);
    delete elems;
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

void IntlTestTextBoundary::TestForwardSentenceSelection()
{
    BreakIterator* e = BreakIterator::createSentenceInstance();
    doForwardSelectionTest(*e, testSentenceText, sentenceSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestFirstSentenceSelection()
{
    BreakIterator* e = BreakIterator::createSentenceInstance();
    doFirstSelectionTest(*e, testSentenceText, sentenceSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestLastSentenceSelection()
{
    BreakIterator* e = BreakIterator::createSentenceInstance();
    doLastSelectionTest(*e, testSentenceText, sentenceSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestBackwardSentenceSelection()
{
    BreakIterator* e = BreakIterator::createSentenceInstance();
    doBackwardSelectionTest(*e, testSentenceText, sentenceSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestForwardSentenceIndexSelection()
{
    BreakIterator* e = BreakIterator::createSentenceInstance();
    doForwardIndexSelectionTest(*e, testSentenceText, sentenceSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestBackwardSentenceIndexSelection()
{
    BreakIterator* e = BreakIterator::createSentenceInstance();
    doBackwardIndexSelectionTest(*e, testSentenceText, sentenceSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestSentenceMultipleSelection()
{
    BreakIterator* e = BreakIterator::createSentenceInstance();
    doMultipleSelectionTest(*e, testSentenceText);
    delete e;
}

void IntlTestTextBoundary::TestSentenceInvariants()
{
    BreakIterator *e = BreakIterator::createSentenceInstance();
    UnicodeString s = *cannedTestChars + CharsToUnicodeString(".,\\u3001\\u3002\\u3041\\u3042\\u3043\\ufeff");
    doOtherInvariantTest(*e, s);
    delete e;
}
//---------------------------------------------
// WordBreak tests
//---------------------------------------------

void IntlTestTextBoundary::TestForwardWordSelection()
{
    BreakIterator* e = BreakIterator::createWordInstance();
    doForwardSelectionTest(*e, testWordText, wordSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestFirstWordSelection()
{
    BreakIterator* e = BreakIterator::createWordInstance();
    doFirstSelectionTest(*e, testWordText, wordSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestLastWordSelection()
{
    BreakIterator* e = BreakIterator::createWordInstance();
    doLastSelectionTest(*e, testWordText, wordSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestBackwardWordSelection()
{
    BreakIterator* e = BreakIterator::createWordInstance();
    doBackwardSelectionTest(*e, testWordText, wordSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestForwardWordIndexSelection()
{
    BreakIterator* e = BreakIterator::createWordInstance();
    doForwardIndexSelectionTest(*e, testWordText, wordSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestBackwardWordIndexSelection()
{
    BreakIterator* e = BreakIterator::createWordInstance();
    doBackwardIndexSelectionTest(*e, testWordText, wordSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestWordMultipleSelection()
{
    BreakIterator* e = BreakIterator::createWordInstance();
    doMultipleSelectionTest(*e, testWordText);
    delete e;
}

void IntlTestTextBoundary::TestWordInvariants()
{
    BreakIterator *e = BreakIterator::createWordInstance();
    UnicodeString s = *cannedTestChars + CharsToUnicodeString("\',.\\u3041\\u3042\\u3043\\u309b\\u309c\\u30a1\\u30a2\\u30a3\\u4e00\\u4e01\\u4e02");
    doBreakInvariantTest(*e, s);
    s = *cannedTestChars + CharsToUnicodeString("\',.\\u3041\\u3042\\u3043\\u309b\\u309c\\u30a1\\u30a2\\u30a3\\u4e00\\u4e01\\u4e02");
    doOtherInvariantTest(*e, s);
    delete e;
}

//---------------------------------------------
// LineBreak tests
//---------------------------------------------

void IntlTestTextBoundary::TestForwardLineSelection()
{
    BreakIterator* e = BreakIterator::createLineInstance();
    doForwardSelectionTest(*e, testLineText, lineSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestFirstLineSelection()
{
    BreakIterator* e = BreakIterator::createLineInstance();
    doFirstSelectionTest(*e, testLineText, lineSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestLastLineSelection()
{
    BreakIterator* e = BreakIterator::createLineInstance();
    doLastSelectionTest(*e, testLineText, lineSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestBackwardLineSelection()
{
    BreakIterator* e = BreakIterator::createLineInstance();
    doBackwardSelectionTest(*e, testLineText, lineSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestForwardLineIndexSelection()
{
    BreakIterator* e = BreakIterator::createLineInstance();
    doForwardIndexSelectionTest(*e, testLineText, lineSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestBackwardLineIndexSelection()
{
    BreakIterator* e = BreakIterator::createLineInstance();
    doBackwardIndexSelectionTest(*e, testLineText, lineSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestLineMultipleSelection()
{
    BreakIterator* e = BreakIterator::createLineInstance();
    doMultipleSelectionTest(*e, testLineText);
    delete e;
}

void IntlTestTextBoundary::TestLineInvariants()
{
    BreakIterator *e = BreakIterator::createLineInstance();
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
    for (i = 0; i < testChars.size(); i++) {
        UChar c = testChars[i];
        if (c == '\r' || c == '\n' || c == 0x2029 || c == 0x2028 || c == 0x0003)
            continue;
        work[0] = c;
        for (j = 0; j < noBreak.size(); j++) {
            work[1] = noBreak[j];
            for (k = 0; k < testChars.size(); k++) {
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
    for (i = 0; i < testChars.size(); i++) {
        work[0] = testChars[i];
        for (j = 0; j < dashes.size(); j++) {
            work[1] = dashes[j];
            for (k = 0; k < testChars.size(); k++) {
                UChar c = testChars[k];
                if (Unicode::getType(c) == Unicode::DECIMAL_DIGIT_NUMBER ||
                    Unicode::getType(c) == Unicode::OTHER_NUMBER ||
                    Unicode::getType(c) == Unicode::NON_SPACING_MARK ||
                    Unicode::getType(c) == Unicode::ENCLOSING_MARK ||
                    Unicode::getType(c) == Unicode::CURRENCY_SYMBOL ||
                    c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029 ||
                    c == 0x0003 || c == 0x00a0 || c == 0x2007 || c == 0x2011 ||
                    c == 0xfeff)
                    continue;
                work[2] = c;
                e->setText(&work);
                bool_t saw2 = FALSE;
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

//---------------------------------------------
// CharacterBreak tests
//---------------------------------------------

void IntlTestTextBoundary::TestForwardCharacterSelection()
{
    BreakIterator* e = BreakIterator::createCharacterInstance();
    doForwardSelectionTest(*e, testCharacterText, characterSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestFirstCharacterSelection()
{
    BreakIterator* e = BreakIterator::createCharacterInstance();
    doFirstSelectionTest(*e, testCharacterText, characterSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestLastCharacterSelection()
{
    BreakIterator* e = BreakIterator::createCharacterInstance();
    doLastSelectionTest(*e, testCharacterText, characterSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestBackwardCharacterSelection()
{
    BreakIterator* e = BreakIterator::createCharacterInstance();
    doBackwardSelectionTest(*e, testCharacterText, characterSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestForwardCharacterIndexSelection()
{
    BreakIterator* e = BreakIterator::createCharacterInstance();
    doForwardIndexSelectionTest(*e, testCharacterText, characterSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestBackwardCharacterIndexSelection()
{
    BreakIterator* e = BreakIterator::createCharacterInstance();
    doBackwardIndexSelectionTest(*e, testCharacterText, characterSelectionData);
    delete e;
}

void IntlTestTextBoundary::TestCharacterMultipleSelection()
{
    BreakIterator* e = BreakIterator::createCharacterInstance();
    doMultipleSelectionTest(*e, testCharacterText);
    delete e;
}

void IntlTestTextBoundary::TestCharacterInvariants()
{
    BreakIterator *e = BreakIterator::createCharacterInstance();
    UnicodeString s = *cannedTestChars + CharsToUnicodeString("\\u1100\\u1101\\u1102\\u1160\\u1161\\u1162\\u11a8\\u11a9\\u11aa");
    doBreakInvariantTest(*e, s);
    s = *cannedTestChars + CharsToUnicodeString("\\u1100\\u1101\\u1102\\u1160\\u1161\\u1162\\u11a8\\u11a9\\u11aa");
    doOtherInvariantTest(*e, s);
    delete e;
}
//---------------------------------------------
// other tests
//---------------------------------------------

void IntlTestTextBoundary::TestEmptyString()
{
    UnicodeString text = "";
    Vector x;
    x.addElement(text);
    BreakIterator* bi = BreakIterator::createLineInstance();
    doForwardSelectionTest(*bi, text, &x);
    doFirstSelectionTest(*bi, text, &x);
    doLastSelectionTest(*bi, text, &x);
    doBackwardSelectionTest(*bi, text, &x);
    doForwardIndexSelectionTest(*bi, text, &x);
    doBackwardIndexSelectionTest(*bi, text, &x);
    delete bi;
}

void IntlTestTextBoundary::TestGetAvailableLocales()
{
    int32_t locCount = 0;
    const Locale* locList = BreakIterator::getAvailableLocales(locCount);

    if (locCount == 0)
        errln("getAvailableLocales() returned an empty list!");
    // I have no idea how to test this function...
}

/**
 * @bug 4095322
 */
void IntlTestTextBoundary::TestJapaneseLineBreak()
{
    UnicodeString testString = CharsToUnicodeString("\\u4e00x\\u4e8c");
    UnicodeString precedingChars = CharsToUnicodeString("([{\\u00ab$\\u00a5\\u00a3\\u00a4\\u2018\\u201a\\u201c\\u201e\\u201b\\u201f");
    UnicodeString followingChars = CharsToUnicodeString(")]}\\u00bb!%,.\\u3001\\u3002\\u3063\\u3083\\u3085\\u3087\\u30c3\\u30e3\\u30e5\\u30e7\\u30fc:;\\u309b\\u309c\\u3005\\u309d\\u309e\\u30fd\\u30fe\\u2019\\u201d\\u00b0\\u2032\\u2033\\u2034\\u2030\\u2031\\u2103\\u2109\\u00a2\\u0300\\u0301\\u0302");
    BreakIterator *iter = BreakIterator::createLineInstance(Locale::JAPAN);
    StringCharacterIterator* it = new StringCharacterIterator(testString);

    UTextOffset i;

    for (i = 0; i < precedingChars.size(); i++) {
        testString[1] = precedingChars[i];
        iter->adoptText(it);
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

    for (i = 0; i < followingChars.size(); i++) {
        testString[1] = followingChars[i];
        it = new StringCharacterIterator(testString);
        iter->adoptText(it);
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

    // [serialization test has been removed pursuant to bug #4152965]

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
 * @bug 4068137
 */
void IntlTestTextBoundary::TestEndBehavior()
{
    UnicodeString testString("boo.");
    BreakIterator *wb = BreakIterator::createWordInstance();
    wb->setText(&testString);

    if (wb->first() != 0)
        errln("Didn't get break at beginning of string.");
    if (wb->next() != 3)
        errln("Didn't get break before period in \"boo.\"");
    if (wb->current() != 4 && wb->next() != 4)
        errln("Didn't get break at end of string.");
    delete wb;
}

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void IntlTestTextBoundary::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite TextBoundary: ");
    switch (index) {
        case 0: name = "TestForwardSentenceSelection"; if (exec) TestForwardSentenceSelection(); break;
        case 1: name = "TestFirstSentenceSelection"; if (exec) TestFirstSentenceSelection(); break;
        case 2: name = "TestLastSentenceSelection"; if (exec) TestLastSentenceSelection(); break;
        case 3: name = "TestBackwardSentenceSelection"; if (exec) TestBackwardSentenceSelection(); break;
        case 4: name = "TestForwardSentenceIndexSelection"; if (exec) TestForwardSentenceIndexSelection(); break;
        case 5: name = "TestBackwardSentenceIndexSelection"; if (exec) TestBackwardSentenceIndexSelection(); break;
        case 6: name = "TestSentenceMultipleSelection"; if (exec) TestSentenceMultipleSelection(); break;
        case 7: name = "TestForwardWordSelection"; if (exec) TestForwardWordSelection(); break;
        case 8: name = "TestFirstWordSelection"; if (exec) TestFirstWordSelection(); break;
        case 9: name = "TestLastWordSelection"; if (exec) TestLastWordSelection(); break;
        case 10: name = "TestBackwardWordSelection"; if (exec) TestBackwardWordSelection(); break;
        case 11: name = "TestForwardWordIndexSelection"; if (exec) TestForwardWordIndexSelection(); break;
        case 12: name = "TestBackwardWordIndexSelection"; if (exec) TestBackwardWordIndexSelection(); break;
        case 13: name = "TestWordMultipleSelection"; if (exec) TestWordMultipleSelection(); break;
        case 14: name = "TestForwardLineSelection"; if (exec) TestForwardLineSelection(); break;
        case 15: name = "TestFirstLineSelection"; if (exec) TestFirstLineSelection(); break;
        case 16: name = "TestLastLineSelection"; if (exec) TestLastLineSelection(); break;
        case 17: name = "TestBackwardLineSelection"; if (exec) TestBackwardLineSelection(); break;
        case 18: name = "TestForwardLineIndexSelection"; if (exec) TestForwardLineIndexSelection(); break;
        case 19: name = "TestBackwardLineIndexSelection"; if (exec) TestBackwardLineIndexSelection(); break;
        case 20: name = "TestLineMultipleSelection"; if (exec) TestLineMultipleSelection(); break;
        case 21: name = "TestForwardCharacterSelection"; if (exec) TestForwardCharacterSelection(); break;
        case 22: name = "TestFirstCharacterSelection"; if (exec) TestFirstCharacterSelection(); break;
        case 23: name = "TestLastCharacterSelection"; if (exec) TestLastCharacterSelection(); break;
        case 24: name = "TestBackwardCharacterSelection"; if (exec) TestBackwardCharacterSelection(); break;
        case 25: name = "TestForwardCharacterIndexSelection"; if (exec) TestForwardCharacterIndexSelection(); break;
        case 26: name = "TestBackwardCharacterIndexSelection"; if (exec) TestBackwardCharacterIndexSelection(); break;
        case 27: name = "TestCharacterMultipleSelection"; if (exec) TestCharacterMultipleSelection(); break;
        case 28: name = "TestEmptyString"; if (exec) TestEmptyString(); break;
        case 29: name = "TestGetAvailableLocales"; if (exec) TestGetAvailableLocales(); break;
        case 30: name = "TestGetDisplayName"; if (exec) TestGetDisplayName(); break;
        case 31: name = "TestPreceding"; if (exec) TestPreceding(); break;
        case 32: name = "TestBug4153072"; if (exec) TestBug4153072(); break;
      /*
        case 33: 
            name = "BreakIteratorCAPI"; 
            if (exec) {
                logln("BreakIterator C API test---"); logln("");
                IntlTestBreakIteratorFormatCAPI test;
                callTest( test, par );
            }
            break;
      */
        default: name = ""; break; //needed to end loop
    }
}

//---------------------------------------------
// Test implementation routines
//---------------------------------------------

void IntlTestTextBoundary::doForwardSelectionTest(BreakIterator& iterator,
                                                  UnicodeString& testText,
                                                  Vector* result)
{
    int32_t forwardSelectionCounter = 0;
    int32_t forwardSelectionOffset = 0;
    CharacterIterator *itSource = 0;
    CharacterIterator *itTarget = 0;

    logln("doForwardSelectionTest text of length: "+testText.size());

    // check to make sure setText() and getText() work right
    iterator.setText(&testText);
    itSource = iterator.createText();
    itTarget = new StringCharacterIterator(testText);

    if (*itSource != *itTarget)
        errln("createText() didn't return what we passed to setText!");
    delete itSource;
    delete itTarget;
    UnicodeString expectedResult;
    UnicodeString selectionResult;
    
    int32_t lastOffset = iterator.first();
    int32_t offset = iterator.next();
    while(offset != BreakIterator::DONE && forwardSelectionCounter < result->size()) {
        if (offset != iterator.current())
            errln((UnicodeString)"current() failed: it returned " + iterator.current() + " and offset was " + offset);

        expectedResult = result->elementAt(forwardSelectionCounter);
        forwardSelectionOffset += expectedResult.size();
        testText.extractBetween(lastOffset, offset, selectionResult);
        if (offset != forwardSelectionOffset) {
            errln((UnicodeString)"\n*** Selection #" +
                  forwardSelectionCounter +
                  "\nExpected : " +
                  expectedResult +
                  " - length : " +
                  expectedResult.size() +
                  "\nSelected : " +
                  selectionResult +
                  " - length : " +
                  selectionResult.size());
        }
        logln((UnicodeString)"#" + forwardSelectionCounter + " ["+lastOffset+", "+offset+"] : " + selectionResult);

        forwardSelectionCounter++;
        lastOffset = offset;
        offset = iterator.next();
    }
    if (forwardSelectionCounter < result->size() - 1)
        errln((UnicodeString)"\n*** Selection #" + forwardSelectionCounter + " not found at offset "+offset+"!!!");
    else if (forwardSelectionCounter >= result->size() && offset != BreakIterator::DONE)
        errln((UnicodeString)"\n*** Selection #" + forwardSelectionCounter + " should not exist at offset "+offset+"!!!");
}

void IntlTestTextBoundary::doBackwardSelectionTest(BreakIterator& iterator,
                                                   UnicodeString& testText,
                                                   Vector* result)
{
    int32_t backwardSelectionCounter = (result->size() - 1);
    int32_t neededOffset = testText.size();
    int32_t lastOffset = iterator.last();
    iterator.setText(&testText);
    int32_t offset = iterator.previous();
    
    UnicodeString expectedResult;
    UnicodeString selectionResult;
    
    while(offset != BreakIterator::DONE)
    {
        expectedResult = (UnicodeString)result->elementAt(backwardSelectionCounter);
        neededOffset -= expectedResult.size();
        testText.extractBetween(offset, lastOffset, selectionResult);
        if(offset != neededOffset) {
            errln(
                (UnicodeString)"\n*** Selection #" +
                backwardSelectionCounter +
                "\nExpected "+neededOffset+"> "  +
                expectedResult +
                " <" +
                "\nSelected "+offset+"> " +
                selectionResult +
                " <");
        }

        logln((UnicodeString)"#" + backwardSelectionCounter + " : " + selectionResult);
        backwardSelectionCounter--;
        lastOffset = offset;
        offset = iterator.previous();
    }
    if (backwardSelectionCounter >= 0 && offset != BreakIterator::DONE)
        errln((UnicodeString)"*** Selection #" + backwardSelectionCounter + " not found!!!");
}

void IntlTestTextBoundary::doFirstSelectionTest(BreakIterator& iterator,
                                                UnicodeString& testText,
                                                Vector* result)
{
    bool_t success = TRUE;
    UnicodeString expectedFirstSelection;
    UnicodeString tempFirst;

    iterator.setText(&testText);
    int32_t selectionStart = iterator.first();
    int32_t selectionEnd = iterator.next();
    if(selectionEnd != BreakIterator::DONE) {
        testText.extractBetween(selectionStart, selectionEnd, tempFirst);

        expectedFirstSelection = result->elementAt(0);
        if(tempFirst != expectedFirstSelection) {
            errln(
                (UnicodeString)"\n\n" +
                "### Error in TestFindWord::doFirstSelectionTest. First selection not equal to what expected." +
                "\nExpexcted : " +
                expectedFirstSelection +
                " - length : " +
                expectedFirstSelection.size() +
                "\nSelected : " +
                tempFirst +
                " - length : " +
                tempFirst.size() +
                "\n");
            success = FALSE;
        }
    }
    else if (selectionStart != 0 || testText.size() != 0) {
        errln((UnicodeString)"\n### Error in TTestFindWord::doFirstSelectionTest. Could not get first selection.\n"+
            "start = "+selectionStart+"  end = "+selectionEnd);
        success = FALSE;
    }

    if(success) {
        logln(
            (UnicodeString)"IntlTestTextBoundary::doFirstSelectionTest \n" +
            "\nExpexcted first selection: " +
            expectedFirstSelection +
            "\nCalculated first selection: " +
            tempFirst +
            " is correct\n");
    }
}

void IntlTestTextBoundary::doLastSelectionTest(BreakIterator& iterator,
                                               UnicodeString& testText,
                                               Vector* result)
{
    bool_t success = TRUE;
    UnicodeString expectedLastSelection;
    UnicodeString tempLast;

    iterator.setText(&testText);
    int32_t selectionEnd = iterator.last();
    int32_t selectionStart = iterator.previous();
    if(selectionStart != BreakIterator::DONE) {
        testText.extractBetween(selectionStart, selectionEnd, tempLast);
        expectedLastSelection = result->lastElement();
        if(tempLast != expectedLastSelection) {
            errln(
                (UnicodeString)"\n\n" +
                "### Error in TTestFindWord::doLastSelectionTest. Last selection not equal to what expected." +
                "\nExpexcted : " +
                expectedLastSelection +
                " - length : " +
                expectedLastSelection.size() +
                "\nSelected : " +
                 tempLast +
                 " - length : " +
                tempLast.size() +
                 "\n");
            success = FALSE;
        }
    }
    else if (selectionEnd != 0 || testText.size() != 0) {
        errln((UnicodeString)"\n### Error in TTestFindWord::doLastSelectionTest. Could not get last selection."+
            "["+selectionStart+","+selectionEnd+"]");
        success = FALSE;
    }

    if(success) {
        logln(
            (UnicodeString)"TTestFindWord::doLastSelectionTest \n" +
            "\nExpexcted last selection: " +
            expectedLastSelection +
            "\nCalculated last selection: " +
            tempLast +
            "\n");
    }
}

/**
 * @bug 4052418 4068139
 */
void IntlTestTextBoundary::doForwardIndexSelectionTest(BreakIterator& iterator,
                                                       UnicodeString& testText,
                                                       Vector* result)
{
    int32_t arrayCount = result->size();
    int32_t textLength = testText.size();
    iterator.setText(&testText);
    for(UTextOffset offset = 0; offset < textLength; offset++) {
        int32_t selBegin = iterator.preceding(offset);
        int32_t selEnd = iterator.following(offset);
        bool_t isBound = iterator.isBoundary(offset);

        int32_t entry = 0;
        int32_t pos = 0;
        if (selBegin != BreakIterator::DONE) {
            while (pos < selBegin && entry < arrayCount) {
                pos += ((UnicodeString)(result->elementAt(entry))).size();
                ++entry;
            }
            if (pos != selBegin) {
                errln((UnicodeString)"With offset = " + offset + ", got back spurious " + selBegin + " from preceding.");
                continue;
            }
            else {
                pos += ((UnicodeString)(result->elementAt(entry))).size();
                ++entry;
            }
        }
        if (isBound) {
            if (pos != offset) {
                errln((UnicodeString)"isBoundary() erroneously returned true with offset = " + offset);
                continue;
            }
            else {
                pos += ((UnicodeString)(result->elementAt(entry))).size();
                ++entry;
            }
        }
        if (pos != selEnd) {
            errln((UnicodeString)"With offset = " + offset + ", got back erroneous " + selEnd + " from following.");
            continue;
        }
    }
}

/**
 * @bug 4052418 4068139
 */
void IntlTestTextBoundary::doBackwardIndexSelectionTest(BreakIterator& iterator,
                                                        UnicodeString& testText,
                                                        Vector* result)
{
    int32_t arrayCount = result->size();
    int32_t textLength = testText.size();
    iterator.setText(&testText);
    for(UTextOffset offset = textLength - 1; offset >= 0; offset--) {
        int32_t selBegin = iterator.preceding(offset);
        int32_t selEnd = iterator.following(offset);
        bool_t isBound = iterator.isBoundary(offset);

        int32_t entry = 0;
        int32_t pos = 0;
        if (selBegin != BreakIterator::DONE) {
            while (pos < selBegin && entry < arrayCount) {
                pos += ((UnicodeString)(result->elementAt(entry))).size();
                ++entry;
            }
            if (pos != selBegin) {
                errln((UnicodeString)"With offset = " + offset + ", got back spurious " + selBegin + " from preceding.");
                continue;
            }
            else {
                pos += ((UnicodeString)(result->elementAt(entry))).size();
                ++entry;
            }
        }
        if (isBound) {
            if (pos != offset) {
                errln((UnicodeString)"isBoundary() erroneously returned true with offset = " + offset);
                continue;
            }
            else {
                pos += ((UnicodeString)(result->elementAt(entry))).size();
                ++entry;
            }
        }
        if (pos != selEnd) {
            errln((UnicodeString)"With offset = " + offset + ", got back erroneous " + selEnd + " from following.");
            continue;
        }
    }
}

/*
 * @bug 4153072
 */
void IntlTestTextBoundary::TestBug4153072() {
    BreakIterator *iter = BreakIterator::createWordInstance();
    UnicodeString str("...Hello, World!...");
    int32_t begin = 3;
    int32_t end = str.size() - 3;
    bool_t gotException = FALSE;
    bool_t dummy;

    StringCharacterIterator textIterator(str, begin, end, begin);
    iter->adoptText(&textIterator);
    for (int index = -1; index < begin + 1; ++index) {
        dummy = iter->isBoundary(index);
        if (index < begin && dummy == TRUE) {
            errln((UnicodeString)"Didn't handle preceeding correctly with offset = " + index +
                            " and begin index = " + begin);
        }
    }
    delete iter;
}

void IntlTestTextBoundary::doMultipleSelectionTest(BreakIterator& iterator,
                                                   UnicodeString& testText)
{
    iterator.setText(&testText);
    
    BreakIterator* testIterator = iterator.clone();
    int32_t offset = iterator.first();
    int32_t testOffset;
    int32_t count = 0;

    logln("doMultipleSelectionTest text of length: "+testText.size());

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

    for (i = 0; i < breaks.size(); i++) {
        work[1] = breaks[i];
        for (j = 0; j < testChars.size(); j++) {
            work[0] = testChars[j];
            for (int k = 0; k < testChars.size(); k++) {
                UChar c = testChars[k];

                // if a cr is followed by lf, ps, ls or etx, don't do the check (that's
                // not supposed to work)
                if (work[1] == '\r' && (c == '\n' || c == 0x2029
                        || c == 0x2028 || c == 0x0003))
                    continue;

                work[2] = testChars[k];
                tb.setText(&work);
                bool_t seen2 = FALSE;
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
    for (i = 0; i < testChars.size(); i++) {
        work[0] = testChars[i];
        for (j = 0; j < testChars.size(); j++) {
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
    for (i = 0; i < testChars.size(); i++) {
        UChar c = testChars[i];
        if (c == '\n' || c == '\r' || c == 0x2029 || c == 0x2028 || c == 0x0003)
            continue;
        work[1] = c;
        for (j = 0; j < testChars.size(); j++) {
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
    bool_t verboseWas = verbose;
    verbose = TRUE;
    logln("-------------------------"+title+" length = "+text.size());
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

void IntlTestTextBoundary::TestPreceding()
{
    UnicodeString words3("aaa bbb ccc");
    BreakIterator* e = BreakIterator::createWordInstance();
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

