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

#include "ustrtest.h"
#include "unistr.h"
#include "locid.h"
#include <stdio.h>

UnicodeStringTest::UnicodeStringTest()
{
}

UnicodeStringTest::~UnicodeStringTest()
{
}

void UnicodeStringTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite LocaleTest: ");
    switch (index) {
        case 0: name = "TestBasicManipulation"; if (exec) TestBasicManipulation(); break;
        case 1: name = "TestCompare"; if (exec) TestCompare(); break;
        case 2: name = "TestExtract"; if (exec) TestExtract(); break;
        case 3: name = "TestRemoveReplace"; if (exec) TestRemoveReplace(); break;
        case 4: name = "TestCaseConversion"; if (exec) TestCaseConversion(); break;
        case 5: name = "TestNothing"; break;
        case 6: name = "TestSearching"; if (exec) TestSearching(); break;
        case 7: name = "TestSpacePadding"; if (exec) TestSpacePadding(); break;
        case 8: name = "TestPrefixAndSuffix"; if (exec) TestPrefixAndSuffix(); break;
        case 9: name = "TestFindAndReplace"; if (exec) TestFindAndReplace(); break;
        case 10: name = "TestCellWidth"; if (exec) TestCellWidth(); break;
        case 11: name = "TestReverse"; if (exec) TestReverse(); break;
        case 12: name = "TestMiscellaneous"; if (exec) TestMiscellaneous(); break;
        case 13: name = "TestStackAllocation"; if (exec) TestStackAllocation(); break;

        default: name = ""; break; //needed to end loop
    }
}

void
UnicodeStringTest::TestBasicManipulation()
{
    UnicodeString   test1("Now is the time for all men to come swiftly to the aid of the party.\n");
    UnicodeString   expectedValue;

    test1.insert(24, "good ");
    expectedValue = "Now is the time for all good men to come swiftly to the aid of the party.\n";
    if (test1 != expectedValue)
        errln("insert() failed:  expected \"" + expectedValue + "\"\n,got \"" + test1 + "\"");

    test1.remove(41, 8);
    expectedValue = "Now is the time for all good men to come to the aid of the party.\n";
    if (test1 != expectedValue)
        errln("remove() failed:  expected \"" + expectedValue + "\"\n,got \"" + test1 + "\"");
    
    test1.replace(58, 6, "ir country");
    expectedValue = "Now is the time for all good men to come to the aid of their country.\n";
    if (test1 != expectedValue)
        errln("replace() failed:  expected \"" + expectedValue + "\"\n,got \"" + test1 + "\"");
    
    UChar     temp[80];
    test1.extract(0, 15, temp);
    
    UnicodeString       test2(temp, 15);
    
    expectedValue = "Now is the time";
    if (test2 != expectedValue)
        errln("extract() failed:  expected \"" + expectedValue + "\"\n,got \"" + test2 + "\"");
    
    test2 += " for me to go!\n";
    expectedValue = "Now is the time for me to go!\n";
    if (test2 != expectedValue)
        errln("operator+=() failed:  expected \"" + expectedValue + "\"\n,got \"" + test2 + "\"");
    
    if (test1.size() != 70)
        errln("size() failed: expected 70, got " + test1.size());
    if (test2.size() != 30)
        errln("size() failed: expected 30, got " + test2.size());
}

void
UnicodeStringTest::TestCompare()
{
    UnicodeString   test1("this is a test");
    UnicodeString   test2("this is a test");
    UnicodeString   test3("this is a test of the emergency broadcast system");
    UnicodeString   test4("never say, \"this is a test\"!!");

    UChar         uniChars[] = { 't', 'h', 'i', 's', ' ', 'i', 's', 
                 ' ', 'a', ' ', 't', 'e', 's', 't', 0 };
    char            chars[] = "this is a test";

    // test operator== and operator!=
    if (test1 != test2 || test1 == test3 || test1 == test4)
        errln("operator== or operator!= failed");

    // test operator> and operator<
    if (test1 > test2 || test1 < test2 || test1 > test3 || test1 < test4)
        errln("operator> or operator< failed");

    // test operator>= and operator<=
    if (!(test1 >= test2) || !(test1 <= test2) || !(test1 <= test3) || !(test1 >= test4))
        errln("operator>= or operator<= failed");

    // test compare(UnicodeString)
    if (test1.compare(test2) != 0 || test1.compare(test3) >= 0 || test1.compare(test4) <= 0)
        errln("compare(UnicodeString) failed");

    //test compare(offset, length, UnicodeString)
    if(test1.compare(0, 14, test2) != 0 ||
        test3.compare(0, 14, test2) != 0 ||
        test4.compare(12, 14, test2) != 0 ||
        test3.compare(0, 18, test1) <=0  )
        errln("compare(offset, length, UnicodeString) failes");

    // test compare(UChar*)
    if (test2.compare(uniChars) != 0 || test3.compare(uniChars) <= 0 || test4.compare(uniChars) >= 0)
        errln("compare(UChar*) failed");

    // test compare(char*)
    if (test2.compare(chars) != 0 || test3.compare(chars) <= 0 || test4.compare(chars) >= 0)
        errln("compare(char*) failed");

    // test compare(UChar*, length)
    if (test1.compare(uniChars, 4) <= 0 || test1.compare(uniChars, 4) <= 0)
        errln("compare(UChar*, length) failed");

    // test compare(thisOffset, thisLength, that, thatOffset, thatLength)
    if (test1.compare(0, 14, test2, 0, 14) != 0 
    || test1.compare(0, 14, test3, 0, 14) != 0
    || test1.compare(0, 14, test4, 12, 14) != 0)
        errln("1. compare(thisOffset, thisLength, that, thatOffset, thatLength) failed");

    if (test1.compare(10, 4, test2, 0, 4) >= 0 
    || test1.compare(10, 4, test3, 22, 9) <= 0
    || test1.compare(10, 4, test4, 22, 4) != 0)
        errln("2. compare(thisOffset, thisLength, that, thatOffset, thatLength) failed");

    // test compareBetween
    if (test1.compareBetween(0, 14, test2, 0, 14) != 0 || test1.compareBetween(0, 14, test3, 0, 14) != 0
                    || test1.compareBetween(0, 14, test4, 12, 26) != 0)
        errln("compareBetween failed");

    if (test1.compareBetween(10, 14, test2, 0, 4) >= 0 || test1.compareBetween(10, 14, test3, 22, 31) <= 0
                    || test1.compareBetween(10, 14, test4, 22, 26) != 0)
        errln("compareBetween failed");
}

void
UnicodeStringTest::TestExtract()
{
    UnicodeString   test1("Now is the time for all good men to come to the aid of their country.");
    UnicodeString   test2;
    UChar         test3[13];
    char            test4[13];
    UnicodeString   test5;

    test1.extract(11, 12, test2);
    test1.extract(11, 12, test3);
    test1.extract(11, 12, test4);
    test1.extractBetween(11, 23, test5);

    for (UTextOffset i = 0; i < 12; i++) {
        if (test1[(UTextOffset)(11 + i)] != test2[i]) {
            errln(UnicodeString("extracting into a UnicodeString failed at position ") + i);
            break;
        }
        if (test1[(UTextOffset)(11 + i)] != test3[i]) {
            errln(UnicodeString("extracting into an array of UChar failed at position ") + i);
            break;
        }
        if ((((char)test1[(UTextOffset)(11 + i)]) & 0xff) != test4[i]) {
            errln(UnicodeString("extracting into an array of char failed at position ") + i);
            break;
        }
        if (test1[(UTextOffset)(11 + i)] != test5[i]) {
            errln(UnicodeString("extracting with extractBetween failed at position ") + i);
            break;
        }
    }
}

void
UnicodeStringTest::TestRemoveReplace()
{
    UnicodeString   test1("The rain in Spain stays mainly on the plain");
    UnicodeString   test2("eat SPAMburgers!");
    UChar         test3[] = { 'S', 'P', 'A', 'M', 'M', 0 };
    char            test4[] = "SPAM";
    UnicodeString&  test5 = test1;

    test1.replace(4, 4, test2, 4, 4);
    test1.replace(12, 5, test3, 4);
    test3[4] = 0;
    test1.replace(17, 4, test3);
    test1.replace(23, 4, test4);
    test1.replaceBetween(37, 42, test2, 4, 8);

    if (test1 != "The SPAM in SPAM SPAMs SPAMly on the SPAM")
        errln("One of the replace methods failed:\n"
              "  expected \"The SPAM in SPAM SPAMs SPAMly on the SPAM\",\n"
              "  got \"" + test1 + "\"");

    test1.remove(21, 1);
    test1.removeBetween(26, 28);

    if (test1 != "The SPAM in SPAM SPAM SPAM on the SPAM")
        errln("One of the remove methods failed:\n"
              "  expected \"The SPAM in SPAM SPAM SPAM on the SPAM\",\n"
              "  got \"" + test1 + "\"");

    for (UTextOffset i = 0; i < test1.size(); i++)
        if (test5[i] != 'S' && test5[i] != 'P' && test5[i] != 'A' && test5[i] != 'M' && test5[i] != ' ')
            test1[i] = 'x';

    if (test1 != "xxx SPAM xx SPAM SPAM SPAM xx xxx SPAM")
        errln("One of the remove methods failed:\n"
              "  expected \"xxx SPAM xx SPAM SPAM SPAM xx xxx SPAM\",\n"
              "  got \"" + test1 + "\"");

    test1.remove();
    if (test1.size() != 0)
        errln("Remove() failed: expected empty string, got \"" + test1 + "\"");
}

void
UnicodeStringTest::TestCaseConversion()
{
    UChar uppercaseGreek[] =
        { 0x399, 0x395, 0x3a3, 0x3a5, 0x3a3, ' ', 0x03a7, 0x3a1, 0x399, 0x3a3, 0x3a4,
        0x39f, 0x3a3, 0 };
        // "IESUS CHRISTOS"

    UChar lowercaseGreek[] = 
        { 0x3b9, 0x3b5, 0x3c3, 0x3c5, 0x3c2, ' ', 0x03c7, 0x3c1, 0x3b9, 0x3c3, 0x3c4,
        0x3bf, 0x3c2, 0 };
        // "iesus christos"

    UChar lowercaseTurkish[] = 
        { 'i', 's', 't', 'a', 'n', 'b', 'u', 'l', ',', ' ', 'n', 'o', 't', ' ', 'c', 'o', 
        'n', 's', 't', 'a', 'n', 't', 0x0131, 'n', 'o', 'p', 'l', 'e', '!', 0 };

    UChar uppercaseTurkish[] = 
        { 'T', 'O', 'P', 'K', 'A', 'P', 'I', ' ', 'P', 'A', 'L', 'A', 'C', 'E', ',', ' ',
        0x0130, 'S', 'T', 'A', 'N', 'B', 'U', 'L', 0 };
    
    UnicodeString expectedResult;
    UnicodeString   test3;

    test3 += 0x0130;
    test3 += "STANBUL, NOT CONSTANTINOPLE!";

    UnicodeString   test4(test3);
    test4.toLower();
    expectedResult = "istanbul, not constantinople!";
    if (test4 != expectedResult)
        errln("1. toLower failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    test4 = test3;
    test4.toLower(Locale("tr", "TR"));
    expectedResult = lowercaseTurkish;
    if (test4 != expectedResult)
        errln("2. toLower failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    test3 = "topkap";
    test3 += 0x0131;
    test3 += " palace, istanbul";
    test4 = test3;

    test4.toUpper();
    expectedResult = "TOPKAPI PALACE, ISTANBUL";
    if (test4 != expectedResult)
        errln("toUpper failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    test4 = test3;
    test4.toUpper(Locale("tr", "TR"));
    expectedResult = uppercaseTurkish;
    if (test4 != expectedResult)
        errln("toUpper failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");

    test3 = "Süßmayrstraße";

    test3.toUpper(Locale("de", "DE"));
    expectedResult = "SÜSSMAYRSTRASSE";
    if (test3 != expectedResult)
        errln("toUpper failed: expected \"" + expectedResult + "\", got \"" + test3 + "\".");
    
    test4.replace(0, test4.size(), uppercaseGreek);

    test4.toLower(Locale("el", "GR"));
    expectedResult = lowercaseGreek;
    if (test4 != expectedResult)
        errln("toLower failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");
    
    test4.replace(0, test4.size(), lowercaseGreek);

    test4.toUpper();
    expectedResult = uppercaseGreek;
    if (test4 != expectedResult)
        errln("toUpper failed: expected \"" + expectedResult + "\", got \"" + test4 + "\".");
}

void
UnicodeStringTest::TestSearching()
{
    UnicodeString test1("test test ttest tetest testesteststt");
    UnicodeString test2("test");
    UChar testChar = 't';

    uint16_t occurrences = 0;
    UTextOffset startPos = 0;
    for ( ;
          startPos != -1 && startPos < test1.size();
          (startPos = test1.indexOf(test2, startPos)) != -1 ? (++occurrences, startPos += 4) : 0)
        ;
    if (occurrences != 6)
        errln("indexOf failed: expected to find 6 occurrences, found " + occurrences);

    for ( occurrences = 0, startPos = 10;
          startPos != -1 && startPos < test1.size();
          (startPos = test1.indexOf(test2, startPos)) != -1 ? (++occurrences, startPos += 4) : 0)
        ;
    if (occurrences != 4)
        errln("indexOf with starting offset failed: expected to find 4 occurrences, found " + occurrences);

    UTextOffset endPos = 28;
    for ( occurrences = 0, startPos = 5;
          startPos != -1 && startPos < test1.size();
          (startPos = test1.indexOf(test2, startPos, endPos - startPos)) != -1 ? (++occurrences, startPos += 4) : 0)
        ;
    if (occurrences != 4)
        errln("indexOf with starting and ending offsets failed: expected to find 4 occurrences, found " + occurrences);

    for ( occurrences = 0, startPos = 0;
          startPos != -1 && startPos < test1.size();
          (startPos = test1.indexOf(testChar, startPos)) != -1 ? (++occurrences, startPos += 1) : 0)
        ;
    if (occurrences != 16)
        errln("indexOf with character failed: expected to find 16 occurrences, found " + occurrences);

    for ( occurrences = 0, startPos = 10;
          startPos != -1 && startPos < test1.size();
          (startPos = test1.indexOf(testChar, startPos)) != -1 ? (++occurrences, startPos += 1) : 0)
        ;
    if (occurrences != 12)
        errln("indexOf with character & start offset failed: expected to find 12 occurrences, found " + occurrences);

    for ( occurrences = 0, startPos = 5, endPos = 28;
          startPos != -1 && startPos < test1.size();
          (startPos = test1.indexOf(testChar, startPos, endPos - startPos)) != -1 ? (++occurrences, startPos += 1) : 0)
        ;
    if (occurrences != 10)
        errln("indexOf with character & start & end offsets failed: expected to find 10 occurrences, found " + occurrences);

    for ( occurrences = 0, startPos = 32;
          startPos != -1;
          (startPos = test1.lastIndexOf(test2, 5, startPos - 5)) != -1 ? ++occurrences : 0)
        ;
    if (occurrences != 4)
        errln("lastIndexOf with starting and ending offsets failed: expected to find 4 occurrences, found " + occurrences);

    for ( occurrences = 0, startPos = 32;
          startPos != -1;
          (startPos = test1.lastIndexOf(testChar, 5, startPos - 5)) != -1 ? ++occurrences : 0)
        ;
    if (occurrences != 11)
        errln("lastIndexOf with character & start & end offsets failed: expected to find 11 occurrences, found " + occurrences);
}

void
UnicodeStringTest::TestSpacePadding()
{
    UnicodeString test1("hello");
    UnicodeString test2("   there");
    UnicodeString test3("Hi!  How ya doin'?  Beautiful day, isn't it?");
    UnicodeString test4;
    bool_t returnVal;
    UnicodeString expectedValue;

    returnVal = test1.padLeading(15);
    expectedValue = "          hello";
    if (returnVal == FALSE || test1 != expectedValue)
        errln("padLeading() failed: expected \"" + expectedValue + "\", got \"" + test1 + "\".");

    returnVal = test2.padTrailing(15);
    expectedValue = "   there       ";
    if (returnVal == FALSE || test2 != expectedValue)
        errln("padTrailing() failed: expected \"" + expectedValue + "\", got \"" + test2 + "\".");

    expectedValue = test3;
    returnVal = test3.padTrailing(15);
    if (returnVal == TRUE || test3 != expectedValue)
        errln("padTrailing() failed: expected \"" + expectedValue + "\", got \"" + test3 + "\".");

    expectedValue = "hello";
    test4.setTo(test1).trim();

    if (test4 != expectedValue || test1 == expectedValue || test4 != expectedValue)
        errln("trim(UnicodeString&) failed");
    
    test1.trim();
    if (test1 != expectedValue)
        errln("trim() failed: expected \"" + expectedValue + "\", got \"" + test1 + "\".");

    test2.trim();
    expectedValue = "there";
    if (test2 != expectedValue)
        errln("trim() failed: expected \"" + expectedValue + "\", got \"" + test2 + "\".");

    test3.trim();
    expectedValue = "Hi!  How ya doin'?  Beautiful day, isn't it?";
    if (test3 != expectedValue)
        errln("trim() failed: expected \"" + expectedValue + "\", got \"" + test3 + "\".");

    returnVal = test1.truncate(15);
    expectedValue = "hello";
    if (returnVal == TRUE || test1 != expectedValue)
        errln("truncate() failed: expected \"" + expectedValue + "\", got \"" + test1 + "\".");

    returnVal = test2.truncate(15);
    expectedValue = "there";
    if (returnVal == TRUE || test2 != expectedValue)
        errln("truncate() failed: expected \"" + expectedValue + "\", got \"" + test2 + "\".");

    returnVal = test3.truncate(15);
    expectedValue = "Hi!  How ya doi";
    if (returnVal == FALSE || test3 != expectedValue)
        errln("truncate() failed: expected \"" + expectedValue + "\", got \"" + test3 + "\".");
}

void
UnicodeStringTest::TestPrefixAndSuffix()
{
    UnicodeString test1("Now is the time for all good men to come to the aid of their country.");
    UnicodeString test2("Now");
    UnicodeString test3("country.");
    UnicodeString test4("count");

    if (!test1.startsWith(test2))
        errln("startsWith() failed: \"" + test2 + "\" should be a prefix of \"" + test1 + "\".");

    if (test1.startsWith(test3))
        errln("startsWith() failed: \"" + test3 + "\" shouldn't be a prefix of \"" + test1 + "\".");

    if (test1.endsWith(test2))
        errln("endsWith() failed: \"" + test2 + "\" shouldn't be a suffix of \"" + test1 + "\".");

    if (!test1.endsWith(test3))
        errln("endsWith() failed: \"" + test3 + "\" should be a suffix of \"" + test1 + "\".");

    if (!test3.startsWith(test4))
        errln("startsWith() failed: \"" + test4 + "\" should be a prefix of \"" + test3 + "\".");

    if (test4.startsWith(test3))
        errln("startsWith() failed: \"" + test3 + "\" shouldn't be a prefix of \"" + test4 + "\".");
}

void
UnicodeStringTest::TestFindAndReplace()
{
    UnicodeString test1("One potato, two potato, three potato, four\n");
    UnicodeString test2("potato");
    UnicodeString test3("MISSISSIPPI");

    UnicodeString expectedValue;

    test1.findAndReplace(test2, test3);
    expectedValue = "One MISSISSIPPI, two MISSISSIPPI, three MISSISSIPPI, four\n";
    if (test1 != expectedValue)
        errln("findAndReplace failed: expected \"" + expectedValue + "\", got \"" + test1 + "\".");
    test1.findAndReplace(test3, test2, 2, 32);
    expectedValue = "One potato, two potato, three MISSISSIPPI, four\n";
    if (test1 != expectedValue)
        errln("findAndReplace failed: expected \"" + expectedValue + "\", got \"" + test1 + "\".");
}

void
UnicodeStringTest::TestCellWidth()
{
    UChar     testData2[] = { 'M', 'o', 0x308, 't', 'l', 'e', 'y', ' ', 'C', 'r', 'u', 0x308, 'e', 0x0000 };
    UChar     testData3[] = { '1', '9', '9', '7', 0x5e74, ' ', 0x516d, 0x6708, ' ', '0', '3', 0x65e5, 0x5e73, 0x6210, 0x0000 };
    UChar     testData4[] = { '9', '7', 0xb144, '6', 0xc6d4, '0', '3', 0xc77c, 0x0000 };
    UChar     testData5[] = { '9', '7', 0x1103, 0x1167, 0x11ab, '6', 0x110b, 0x117b, 0x11af, '0', '3', 0x110b, 0x1175, 0x11af, 0x0000 };
    
    UnicodeString   test1("The rain in Spain stays mainly on the plain.");
    UnicodeString   test2(testData2);
    UnicodeString   test3(testData3);
    UnicodeString   test4(testData4);
    UnicodeString   test5(testData5);

    if (test1.numDisplayCells() != 44)
        errln("numDisplayCells() failed: expected 44, got " + test1.numDisplayCells());
    if (test2.numDisplayCells() != 11)
        errln("numDisplayCells() failed: expected 11, got " + test2.numDisplayCells());
    if (test3.numDisplayCells() != 20)
        errln("numDisplayCells() failed: expected 20, got " + test3.numDisplayCells());
    if (test4.numDisplayCells() != 11)
        errln("numDisplayCells() failed: expected 11, got " + test4.numDisplayCells());
    if (test5.numDisplayCells() != 11)
        errln("numDisplayCells() failed: expected 11, got " + test5.numDisplayCells());
}

void
UnicodeStringTest::TestReverse()
{
    UnicodeString test("backwards words say to used I");

    test.reverse();
    test.reverse(2, 6);
    test.reverse(7, 9);
    test.reverse(10, 13);
    test.reverse(14, 19);
    test.reverse(20, 29);

    if (test != "I used to say words backwards")
        errln("reverse() failed:  Expected \"I used to say words backwards\",\n got \""
            + test + "\"");
}

void
UnicodeStringTest::TestMiscellaneous()
{
    UnicodeString   test1("This is a test");
    UnicodeString   test2("This is a test");
    UnicodeString   test3("Me too!");
    const UChar*  test4;

    if (test1.isBogus() || test2.isBogus() || test3.isBogus())
        errln("A string returned true for isBogus()!");

    if (test1.hashCode() != test2.hashCode() || test1.hashCode() == test3.hashCode())
        errln("hashCode() failed");

    test4 = test1.getUChars();

    if (test1 != test2)
        errln("getUChars() affected the string!");

    UTextOffset i;
    for (i = 0; i < test2.size(); i++)
        if (test2[i] != test4[i])
            errln(UnicodeString("getUChars() failed: strings differ at position ") + i);

    test4 = test1.orphanStorage();

    if (test1.size() != 0)
        errln("orphanStorage() failed: orphaned string's contents is " + test1);

    for (i = 0; i < test2.size(); i++)
        if (test2[i] != test4[i])
            errln(UnicodeString("orphanStorage() failed: strings differ at position ") + i);

    delete (UChar*)test4;
}

void
UnicodeStringTest::TestStackAllocation()
{
     UChar            testString[] ={ 
        'T', 'h', 'i', 's', ' ', 'i', 's', ' ', 'a', ' ', 'c', 'r', 'a', 'z', 'y', ' ', 't', 'e', 's', 't','.'};
    UChar           guardWord = 0x4DED;
    UnicodeString*  test = 0;

    test = new  UnicodeString(testString);
    if (*test != "This is a crazy test.")
        errln("Test string failed to initialize properly.");
    if (guardWord != 0x04DED)
        errln("Test string initialization overwrote guard word!");

    test->insert(8, "only ");
    test->remove(15, 6);
    if (*test != "This is only a test.")
        errln("Manipulation of test string failed to work right.");
    if (guardWord != 0x4DED)
        errln("Manipulation of test string overwrote guard word!");

    // we have to deinitialize and release the backing store by calling the destructor
    // explicitly, since we can't overload operator delete
    test->~UnicodeString();

    UChar workingBuffer[] = {
        'N', 'o', 'w', ' ', 'i', 's', ' ', 't', 'h', 'e', ' ', 't', 'i', 'm', 'e', ' ',
        'f', 'o', 'r', ' ', 'a', 'l', 'l', ' ', 'm', 'e', 'n', ' ', 't', 'o', ' ',
        'c', 'o', 'm', 'e', 0xffff, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    UChar guardWord2 = 0x4DED;

    test = new UnicodeString(workingBuffer, 35, 50);
    if (*test != "Now is the time for all men to come")
        errln("Stack-allocated backing store failed to initialize correctly.");
    if (guardWord2 != 0x4DED)
        errln("Stack-allocated backing store overwrote guard word!");

    test->insert(24, "good ");
    if (*test != "Now is the time for all good men to come")
        errln("insert() on stack-allocated UnicodeString didn't work right");
    if (guardWord2 != 0x4DED)
        errln("insert() on stack-allocated UnicodeString overwrote guard word!");
    if (workingBuffer[24] != 'g')
        errln("insert() on stack-allocated UnicodeString didn't affect backing store");

    *test += " to the aid of their country.";
    if (*test != "Now is the time for all good men to come to the aid of their country.")
        errln("Stack-allocated UnicodeString overflow didn't work");
    if (guardWord2 != 0x4DED)
        errln("Stack-allocated UnicodeString overflow overwrote guard word!");

    *test = "ha!";
    if (*test != "ha!")
        errln("Assignment to stack-allocated UnicodeString didn't work");
    if (workingBuffer[0] != 'N')
        errln("Change to UnicodeString after overflow are stil affecting original buffer");
    if (guardWord2 != 0x4DED)
        errln("Change to UnicodeString after overflow overwrote guard word!");
#ifdef _WIN32
    test->~UnicodeString();
#endif
}
