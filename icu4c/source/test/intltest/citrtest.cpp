/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 * Modification History:
 *
 *   Date          Name        Description
 *   05/22/2000    Madhu	   Added tests for testing new API for utf16 support and more
 **********************************************************************/
#include "citrtest.h"
#include "unicode/schriter.h"

CharIterTest::CharIterTest()
{
}

CharIterTest::~CharIterTest()
{
}

void CharIterTest::runIndexedTest( int32_t index, UBool exec, char* &name, char* par )
{
    if (exec) logln("TestSuite LocaleTest: ");
    switch (index) {
        case 0: name = "TestConstructionAndEquality"; if (exec) TestConstructionAndEquality(); break;
        case 1: name = "TestIteration"; if (exec) TestIteration(); break;
        case 2: name = "TestIterationUChar32"; if (exec) TestIterationUChar32(); break;


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
			if(iter.setIndex(i) != c)
				errln("setIndex() isn't working right");

            if (c != CharacterIterator::DONE) {
                c = iter.previous();
                i--;
            }
        } while (c != CharacterIterator::DONE);

		//testing firstPostInc, nextPostInc, setTostart
        i = 0;
		c=iter.firstPostInc();
		if(c != text[i])
			errln((UnicodeString)"firstPostInc failed.  Expected->" +  
			             UCharToUnicodeString(text[i]) + " Got->" + UCharToUnicodeString(c));
		if(iter.getIndex() != i+1)
			errln((UnicodeString)"getIndex() after firstPostInc() failed");
		
		iter.setToStart();
		i=0;
        if (iter.startIndex() != 0)
            errln("setToStart failed");
       
        logln("Testing forward iteration...");
        do {
           	if (c != CharacterIterator::DONE)
                c = iter.nextPostInc();
			
            if(c != text[i])
                errln((UnicodeString)"Character mismatch at position " + i +
                                    (UnicodeString)", iterator has " + UCharToUnicodeString(c) +
                                    (UnicodeString)", string has " + UCharToUnicodeString(text[i]));
			
			i++;
			if(iter.getIndex() != i)
				errln("getIndex() aftr nextPostInc() isn't working right");
			if(iter.current() != text[i])
				errln("current() after nextPostInc() isn't working right");
			} while (iter.hasNext());

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
			if(iter.setIndex(i) != c)
				errln("setIndex() isn't working right");

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
//Tests for new API for utf-16 support 
void CharIterTest::TestIterationUChar32() {
    UChar textChars[]={ 0x0061, 0x0062, 0xd841, 0xdc02, 0x20ac, 0xd7ff, 0xd842, 0xdc06, 0xd801, 0xdc00, 0x0061, 0x0000};
    UnicodeString text(textChars);
    UChar32 c;
    UTextOffset i;
    {
        StringCharacterIterator   iter(text, 1);

        UnicodeString iterText;
        iter.getText(iterText);
        if (iterText != text)
          errln("iter.getText() failed");

        if (iter.current32() != text[(UTextOffset)1])
            errln("Iterator didn't start out in the right place.");

        c = iter.first32();
        i = 0;

        if (iter.startIndex() != 0 || iter.endIndex() != text.length())
            errln("startIndex() or endIndex() failed");

        logln("Testing forward iteration...");
        do {
            if (c == CharacterIterator::DONE && i != text.length())
                errln("Iterator reached end prematurely");
			else if(iter.hasNext() == FALSE && i != text.length())
				errln("Iterator reached end prematurely.  Failed at hasNext");
            else if (c != text.char32At(i))
                errln((UnicodeString)"Character mismatch at position " + i +
                                    (UnicodeString)", iterator has " + c +
                                    (UnicodeString)", string has " + text.char32At(i));

            if (iter.current32() != c)
                errln("current32() isn't working right");
			if(iter.setIndex32(i) != c)
				errln("setIndex32() isn't working right");
            if (c != CharacterIterator::DONE) {
                c = iter.next32();
				i=UTF16_NEED_MULTIPLE_UCHAR(c) ? i+2 : i+1;
              
            }
        } while (c != CharacterIterator::DONE);
        if(iter.hasNext() == TRUE)
           errln("hasNext() returned true at the end of the string");
	
           
		c=iter.setToEnd();
		if(iter.getIndex() != text.length() || iter.hasNext() != FALSE)
			errln("setToEnd failed");

		c = iter.last32();
        i = text.length()-1;
        logln("Testing backward iteration...");
        do {
            if (c == CharacterIterator::DONE && i >= 0)
                errln((UnicodeString)"Iterator reached start prematurely for i=" + i);
			else if(iter.hasPrevious() == FALSE && i>0)
				errln((UnicodeString)"Iterator reached start prematurely for i=" + i);
            else if (c != text.char32At(i))
                errln(prettify((UnicodeString)"Character mismatch at position " + i +
                                    (UnicodeString)", iterator has " + c +
                                    (UnicodeString)", string has " + text.char32At(i)));
            
            if (iter.current32() != c)
                errln("current32() isn't working right");
            if(iter.setIndex32(i) != c)
				errln("setIndex32() isn't working right");
		    if (iter.getIndex() != i)
                errln("getIndex() isn't working right");
            if (c != CharacterIterator::DONE) {
                c = iter.previous32();
               	i=UTF16_NEED_MULTIPLE_UCHAR(c) ? i-2 : i-1;
            }
        } while (c != CharacterIterator::DONE);
		if(iter.hasPrevious() == TRUE)
			errln("hasPrevious returned true after reaching the start");
		


		//testing first32PostInc, next32PostInc, setTostart
        i = 0;
		c=iter.first32PostInc();
		if(c != text.char32At(i))
			errln((UnicodeString)"first32PostInc failed.  Expected->" +  text.char32At(i) + " Got->" + c);
		if(iter.getIndex() != UTF16_CHAR_LENGTH(c) + i)
			errln((UnicodeString)"getIndex() after first32PostInc() failed");
		
		iter.setToStart();
		i=0;
        if (iter.startIndex() != 0)
            errln("setToStart failed");
       
        logln("Testing forward iteration...");
        do {
           	if (c != CharacterIterator::DONE)
                c = iter.next32PostInc();
			
            if(c != text.char32At(i))
                errln((UnicodeString)"Character mismatch at position " + i +
                                    (UnicodeString)", iterator has " + c +
                                    (UnicodeString)", string has " + text.char32At(i));
			
			i=UTF16_NEED_MULTIPLE_UCHAR(c) ? i+2 : i+1;
			if(iter.getIndex() != i)
				errln("getIndex() aftr next32PostInc() isn't working right");
			if(iter.current32() != text.char32At(i))
				errln("current() after next32PostInc() isn't working right");
			} while (iter.hasNext());


    }

	 {
        StringCharacterIterator iter(text, 1, 11, 10);
        if (iter.startIndex() != 1 || iter.endIndex() != 11)
            errln("creation of a restricted-range iterator failed");

        if (iter.getIndex() != 10 || iter.current32() != text.char32At(10))
            errln("starting the iterator in the middle didn't work");

        c = iter.first32();
        i = 1;

        logln("Testing forward iteration over a range...");
        do {
            if (c == CharacterIterator::DONE && i != 11)
                errln("Iterator reached end prematurely");
			else if(iter.hasNext() == FALSE)
				errln("Iterator reached end prematurely");
            else if (c != text.char32At(i))
                errln((UnicodeString)"Character mismatch at position " + i +
                                    (UnicodeString)", iterator has " + c +
                                    (UnicodeString)", string has " + text.char32At(i));

            if (iter.current32() != c)
                errln("current32() isn't working right");
           	if(iter.setIndex32(i) != c)
				errln("setIndex32() isn't working right");

            if (c != CharacterIterator::DONE) {
                c = iter.next32();
               	i=UTF16_NEED_MULTIPLE_UCHAR(c) ? i+2 : i+1;
            }
        } while (c != CharacterIterator::DONE);

        c = iter.last32();
        i = 10;
        logln("Testing backward iteration over a range...");
        do {
            if (c == CharacterIterator::DONE && i >= 5)
                errln("Iterator reached start prematurely");
			else if(iter.hasPrevious() == FALSE && i > 5)
				errln("Iterator reached start prematurely");
            else if (c != text.char32At(i))
                errln((UnicodeString)"Character mismatch at position " + i +
                                    (UnicodeString)", iterator has " + c +
                                    (UnicodeString)", string has " + text.char32At(i));

            if (iter.current32() != c)
                errln("current32() isn't working right");
            if (iter.getIndex() != i)
                errln("getIndex() isn't working right");
			if(iter.setIndex32(i) != c)
				errln("setIndex32() isn't working right");

            if (c != CharacterIterator::DONE) {
                c = iter.previous32();
                i=UTF16_NEED_MULTIPLE_UCHAR(c) ? i-2 : i-1;
            }
        } while (c != CharacterIterator::DONE);


    }
}
