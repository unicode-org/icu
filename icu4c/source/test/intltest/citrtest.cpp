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

#include "citrtest.h"
#include "unicode/schriter.h"

CharIterTest::CharIterTest()
{
}

CharIterTest::~CharIterTest()
{
}

void CharIterTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite LocaleTest: ");
    switch (index) {
        case 0: name = "TestConstructionAndEquality"; if (exec) TestConstructionAndEquality(); break;
        case 1: name = "TestIteration"; if (exec) TestIteration(); break;

        default: name = ""; break; //needed to end loop
    }
}

void CharIterTest::TestConstructionAndEquality() {
    UnicodeString  testText("Now is the time for all good men to come to the aid of their country.");
    UnicodeString  testText2("Don't bother using this string.");

    CharacterIterator* test1 = new StringCharacterIterator(testText);
    CharacterIterator* test2 = new StringCharacterIterator(testText, 5);
    CharacterIterator* test3 = new StringCharacterIterator(testText, 2, 20, 5);
    CharacterIterator* test4 = new StringCharacterIterator(testText2);
    CharacterIterator* test5 = test1->clone();

    if (*test1 == *test2 || *test1 == *test3 || *test1 == *test4)
        errln("Construction or operator== failed: Unequal objects compared equal");
    if (*test1 != *test5)
        errln("clone() or equals() failed: Two clones tested unequal");

    if (test1->hashCode() == test2->hashCode() || test1->hashCode() == test3->hashCode()
                    || test1->hashCode() == test4->hashCode())
        errln("hashCode() failed:  different objects have same hash code");

    if (test1->hashCode() != test5->hashCode())
        errln("hashCode() failed:  identical objects have different hash codes");

    test1->setIndex(5);
    if (*test1 != *test2 || *test1 == *test5)
        errln("setIndex() failed");

    *((StringCharacterIterator*)test1) = *((StringCharacterIterator*)test3);
    if (*test1 != *test3 || *test1 == *test5)
        errln("operator= failed");

    delete test1;
    delete test2;
    delete test3;
    delete test4;
    delete test5;
}

void CharIterTest::TestIteration() {
    UnicodeString text("Now is the time for all good men to come to the aid of their country.");

    UChar c;
    UTextOffset i;
    {
        StringCharacterIterator   iter(text, 5);

        UnicodeString iterText;
        iter.getText(iterText);
        if (iterText != text)
          errln("iter.getText() failed");

        if (iter.current() != text[(UTextOffset)5])
            errln("Iterator didn't start out in the right place.");

        c = iter.first();
        i = 0;

        if (iter.startIndex() != 0 || iter.endIndex() != text.length())
            errln("startIndex() or endIndex() failed");

        logln("Testing forward iteration...");
        do {
            if (c == CharacterIterator::DONE && i != text.length())
                errln("Iterator reached end prematurely");
            else if (c != text[i])
                errln((UnicodeString)"Character mismatch at position " + i +
                                    ", iterator has " + UCharToUnicodeString(c) +
                                    ", string has " + UCharToUnicodeString(text[i]));

            if (iter.current() != c)
                errln("current() isn't working right");
            if (iter.getIndex() != i)
                errln("getIndex() isn't working right");

            if (c != CharacterIterator::DONE) {
                c = iter.next();
                i++;
            }
        } while (c != CharacterIterator::DONE);

        c = iter.last();
        i = text.length() - 1;

        logln("Testing backward iteration...");
        do {
            if (c == CharacterIterator::DONE && i >= 0)
                errln("Iterator reached end prematurely");
            else if (c != text[i])
                errln((UnicodeString)"Character mismatch at position " + i +
                                    ", iterator has " + UCharToUnicodeString(c) +
                                    ", string has " + UCharToUnicodeString(text[i]));

            if (iter.current() != c)
                errln("current() isn't working right");
            if (iter.getIndex() != i)
                errln("getIndex() isn't working right");

            if (c != CharacterIterator::DONE) {
                c = iter.previous();
                i--;
            }
        } while (c != CharacterIterator::DONE);
    }

    {
        StringCharacterIterator iter(text, 5, 15, 10);
        if (iter.startIndex() != 5 || iter.endIndex() != 15)
            errln("creation of a restricted-range iterator failed");

        if (iter.getIndex() != 10 || iter.current() != text[(UTextOffset)10])
            errln("starting the iterator in the middle didn't work");

        c = iter.first();
        i = 5;

        logln("Testing forward iteration over a range...");
        do {
            if (c == CharacterIterator::DONE && i != 15)
                errln("Iterator reached end prematurely");
            else if (c != text[i])
                errln((UnicodeString)"Character mismatch at position " + i +
                                    ", iterator has " + UCharToUnicodeString(c) +
                                    ", string has " + UCharToUnicodeString(text[i]));

            if (iter.current() != c)
                errln("current() isn't working right");
            if (iter.getIndex() != i)
                errln("getIndex() isn't working right");

            if (c != CharacterIterator::DONE) {
                c = iter.next();
                i++;
            }
        } while (c != CharacterIterator::DONE);

        c = iter.last();
        i = 14;

        logln("Testing backward iteration over a range...");
        do {
            if (c == CharacterIterator::DONE && i >= 5)
                errln("Iterator reached end prematurely");
            else if (c != text[i])
                errln((UnicodeString)"Character mismatch at position " + i +
                                    ", iterator has " + UCharToUnicodeString(c) +
                                    ", string has " + UCharToUnicodeString(text[i]));

            if (iter.current() != c)
                errln("current() isn't working right");
            if (iter.getIndex() != i)
                errln("getIndex() isn't working right");

            if (c != CharacterIterator::DONE) {
                c = iter.previous();
                i--;
            }
        } while (c != CharacterIterator::DONE);
    }
}
