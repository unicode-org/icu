/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 * Modification History:
 *
 *   Date          Name        Description
 *   05/22/2000    Madhu       Added tests for testing new API for utf16 support and more
 **********************************************************************/

#include <string.h>
#include "unicode/chariter.h"
#include "unicode/ustring.h"
#include "unicode/unistr.h"
#include "unicode/schriter.h"
#include "unicode/uchriter.h"
#include "unicode/uiter.h"
#include "citrtest.h"

CharIterTest::CharIterTest()
{
}

void CharIterTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln("TestSuite LocaleTest: ");
    switch (index) {
        case 0: name = "TestConstructionAndEquality"; if (exec) TestConstructionAndEquality(); break;
        case 1: name = "TestConstructionAndEqualityUChariter"; if (exec) TestConstructionAndEqualityUChariter(); break;
        case 2: name = "TestIteration"; if (exec) TestIteration(); break;
        case 3: name = "TestIterationUChar32"; if (exec) TestIterationUChar32(); break;
        case 4: name = "TestUCharIterator"; if (exec) TestUCharIterator(); break;

        default: name = ""; break; //needed to end loop
    }
}

void CharIterTest::TestConstructionAndEquality() {
    UnicodeString  testText("Now is the time for all good men to come to the aid of their country.");
    UnicodeString  testText2("Don't bother using this string.");
    UnicodeString result1, result2, result3;

    CharacterIterator* test1 = new StringCharacterIterator(testText);
    CharacterIterator* test1b= new StringCharacterIterator(testText, -1);
    CharacterIterator* test1c= new StringCharacterIterator(testText, 100);
    CharacterIterator* test1d= new StringCharacterIterator(testText, -2, 100, 5);
    CharacterIterator* test1e= new StringCharacterIterator(testText, 100, 20, 5);
    CharacterIterator* test2 = new StringCharacterIterator(testText, 5);
    CharacterIterator* test3 = new StringCharacterIterator(testText, 2, 20, 5);
    CharacterIterator* test4 = new StringCharacterIterator(testText2);
    CharacterIterator* test5 = test1->clone();

    if (test1d->startIndex() < 0)
        errln("Construction failed: startIndex is negative");
    if (test1d->endIndex() > testText.length())
        errln("Construction failed: endIndex is greater than the text length");
    if (test1d->getIndex() < test1d->startIndex() || test1d->endIndex() < test1d->getIndex())
        errln("Construction failed: index is invalid");

    if (*test1 == *test2 || *test1 == *test3 || *test1 == *test4)
        errln("Construction or operator== failed: Unequal objects compared equal");
    if (*test1 != *test5)
        errln("clone() or equals() failed: Two clones tested unequal");

    if (test1->hashCode() == test2->hashCode() || test1->hashCode() == test3->hashCode()
                    || test1->hashCode() == test4->hashCode())
        errln("hashCode() failed:  different objects have same hash code");

    if (test1->hashCode() != test5->hashCode())
        errln("hashCode() failed:  identical objects have different hash codes");


    test1->getText(result1);
    test1b->getText(result2);
    test1c->getText(result3);
    if(result1 != result2 ||  result1 != result3)
        errln("construction failed or getText() failed");


    test1->setIndex(5);
    if (*test1 != *test2 || *test1 == *test5)
        errln("setIndex() failed");

    *((StringCharacterIterator*)test1) = *((StringCharacterIterator*)test3);
    if (*test1 != *test3 || *test1 == *test5)
        errln("operator= failed");

    delete test2;
    delete test3;
    delete test4;
    delete test5;
    delete test1b;
    delete test1c;
    delete test1d;
    delete test1e;

   
    StringCharacterIterator* testChar1=new StringCharacterIterator(testText);
    StringCharacterIterator* testChar2=new StringCharacterIterator(testText2);
    StringCharacterIterator* testChar3=(StringCharacterIterator*)test1->clone();

    testChar1->getText(result1);
    testChar2->getText(result2);
    testChar3->getText(result3); 
    if(result1 != result3 || result1 == result2)
        errln("getText() failed");
    testChar3->setText(testText2);
    testChar3->getText(result3);
    if(result1 == result3 || result2 != result3)
        errln("setText() or getText() failed");
    testChar3->setText(testText);
    testChar3->getText(result3);
    if(result1 != result3 || result1 == result2)
        errln("setText() or getText() round-trip failed");

    delete testChar1;
    delete testChar2;
    delete testChar3;
    delete test1;

}
void CharIterTest::TestConstructionAndEqualityUChariter() {
    U_STRING_DECL(testText, "Now is the time for all good men to come to the aid of their country.", 69);
    U_STRING_DECL(testText2, "Don't bother using this string.", 31);

    U_STRING_INIT(testText, "Now is the time for all good men to come to the aid of their country.", 69);
    U_STRING_INIT(testText2, "Don't bother using this string.", 31);

    UnicodeString result, result4, result5;

    UCharCharacterIterator* test1 = new UCharCharacterIterator(testText, u_strlen(testText));
    UCharCharacterIterator* test2 = new UCharCharacterIterator(testText, u_strlen(testText), 5);
    UCharCharacterIterator* test3 = new UCharCharacterIterator(testText, u_strlen(testText), 2, 20, 5);
    UCharCharacterIterator* test4 = new UCharCharacterIterator(testText2, u_strlen(testText2));
    UCharCharacterIterator* test5 = (UCharCharacterIterator*)test1->clone();
    UCharCharacterIterator* test6 = new UCharCharacterIterator(*test1);

    // j785: length=-1 will use u_strlen()
    UCharCharacterIterator* test7a = new UCharCharacterIterator(testText, -1);
    UCharCharacterIterator* test7b = new UCharCharacterIterator(testText, -1);
    UCharCharacterIterator* test7c = new UCharCharacterIterator(testText, -1, 2, 20, 5);

    // Bad parameters.
    UCharCharacterIterator* test8a = new UCharCharacterIterator(testText, -1, -1, 20, 5);
    UCharCharacterIterator* test8b = new UCharCharacterIterator(testText, -1, 2, 100, 5);
    UCharCharacterIterator* test8c = new UCharCharacterIterator(testText, -1, 2, 20, 100);

    if (test8a->startIndex() < 0)
        errln("Construction failed: startIndex is negative");
    if (test8b->endIndex() != u_strlen(testText))
        errln("Construction failed: endIndex is different from the text length");
    if (test8c->getIndex() < test8c->startIndex() || test8c->endIndex() < test8c->getIndex())
        errln("Construction failed: index is invalid");

    if (*test1 == *test2 || *test1 == *test3 || *test1 == *test4 )
        errln("Construction or operator== failed: Unequal objects compared equal");
    if (*test1 != *test5 )
        errln("clone() or equals() failed: Two clones tested unequal");

    if (*test6 != *test1 )
        errln("copy construction or equals() failed: Two copies tested unequal");

    if (test1->hashCode() == test2->hashCode() || test1->hashCode() == test3->hashCode()
                    || test1->hashCode() == test4->hashCode())
        errln("hashCode() failed:  different objects have same hash code");

    if (test1->hashCode() != test5->hashCode())
        errln("hashCode() failed:  identical objects have different hash codes");
     
    test7a->getText(result);
    test7b->getText(result4);
    test7c->getText(result5);

    if(result != UnicodeString(testText) || result4 != result || result5 != result)
        errln("error in construction");
    
    test1->getText(result);
    test4->getText(result4);
    test5->getText(result5); 
    if(result != result5 || result == result4)
        errln("getText() failed");
    test5->setText(testText2, u_strlen(testText2));
    test5->getText(result5);
    if(result == result5 || result4 != result5)
        errln("setText() or getText() failed");
    test5->setText(testText, u_strlen(testText));
    test5->getText(result5);
    if(result != result5 || result == result4)
        errln("setText() or getText() round-trip failed"); 


    test1->setIndex(5);
    if (*test1 != *test2 || *test1 == *test5)
        errln("setIndex() failed");
    test8b->setIndex32(5);
    if (test8b->getIndex()!=5)
        errln("setIndex32() failed");

    *test1 = *test3;
    if (*test1 != *test3 || *test1 == *test5)
        errln("operator= failed");

    delete test1;
    delete test2;
    delete test3;
    delete test4;
    delete test5;
    delete test6;
    delete test7a;
    delete test7b;
    delete test7c;
    delete test8a;
    delete test8b;
    delete test8c;
}


void CharIterTest::TestIteration() {
    UnicodeString text("Now is the time for all good men to come to the aid of their country.");

    UChar c;
    int32_t i;
    {
        StringCharacterIterator   iter(text, 5);

        UnicodeString iterText;
        iter.getText(iterText);
        if (iterText != text)
          errln("iter.getText() failed");

        if (iter.current() != text[(int32_t)5])
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
        c=iter.next();
        if(c!= CharacterIterator::DONE)
            errln("next() didn't return DONE at the end");
        c=iter.setIndex(text.length()+1);
        if(c!= CharacterIterator::DONE)
            errln("setIndex(len+1) didn't return DONE");

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

        c=iter.previous();
        if(c!= CharacterIterator::DONE)
            errln("previous didn't return DONE at the beginning");


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
        c=iter.nextPostInc();
        if(c!= CharacterIterator::DONE)
            errln("nextPostInc() didn't return DONE at the beginning");
    }

    {
        StringCharacterIterator iter(text, 5, 15, 10);
        if (iter.startIndex() != 5 || iter.endIndex() != 15)
            errln("creation of a restricted-range iterator failed");

        if (iter.getIndex() != 10 || iter.current() != text[(int32_t)10])
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
    int32_t i;
    {
        StringCharacterIterator   iter(text, 1);

        UnicodeString iterText;
        iter.getText(iterText);
        if (iterText != text)
          errln("iter.getText() failed");

        if (iter.current32() != text[(int32_t)1])
            errln("Iterator didn't start out in the right place.");

        c=iter.setToStart();
        i=0;
        i=iter.move32(1, CharacterIterator::kStart);
        c=iter.current32();
        if(c != text.char32At(1) || i!=1)
            errln("move32(1, kStart) didn't work correctly expected %X got %X", c, text.char32At(1) );

        i=iter.move32(2, CharacterIterator::kCurrent);
        c=iter.current32();
        if(c != text.char32At(4) || i!=4)
            errln("move32(2, kCurrent) didn't work correctly expected %X got %X i=%ld", c, text.char32At(4), i);
        
        i=iter.move32(-2, CharacterIterator::kCurrent);
        c=iter.current32();
        if(c != text.char32At(1) || i!=1)
            errln("move32(-2, kCurrent) didn't work correctly expected %X got %X i=%d", c, text.char32At(1), i);


        i=iter.move32(-2, CharacterIterator::kEnd);
        c=iter.current32();
        if(c != text.char32At((text.length()-3)) || i!=(text.length()-3))
            errln("move32(-2, kEnd) didn't work correctly expected %X got %X i=%d", c, text.char32At((text.length()-3)), i);
        

        c = iter.first32();
        i = 0;

        if (iter.startIndex() != 0 || iter.endIndex() != text.length())
            errln("startIndex() or endIndex() failed");

        logln("Testing forward iteration...");
        do {
            /* logln("c=%d i=%d char32At=%d", c, i, text.char32At(i)); */
            if (c == CharacterIterator::DONE && i != text.length())
                errln("Iterator reached end prematurely");
            else if(iter.hasNext() == FALSE && i != text.length())
                errln("Iterator reached end prematurely.  Failed at hasNext");
            else if (c != text.char32At(i))
                errln("Character mismatch at position %d, iterator has %X, string has %X", i, c, text.char32At(i));

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

        c=iter.next32();
        if(c!= CharacterIterator::DONE)
            errln("next32 didn't return DONE at the end");
        c=iter.setIndex32(text.length()+1);
        if(c!= CharacterIterator::DONE)
            errln("setIndex32(len+1) didn't return DONE");


        c = iter.last32();
        i = text.length()-1;
        logln("Testing backward iteration...");
        do {
            if (c == CharacterIterator::DONE && i >= 0)
                errln((UnicodeString)"Iterator reached start prematurely for i=" + i);
            else if(iter.hasPrevious() == FALSE && i>0)
                errln((UnicodeString)"Iterator reached start prematurely for i=" + i);
            else if (c != text.char32At(i))
                errln("Character mismatch at position %d, iterator has %X, string has %X", i, c, text.char32At(i));

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

        c=iter.previous32();
        if(c!= CharacterIterator::DONE)
            errln("previous32 didn't return DONE at the beginning");




        //testing first32PostInc, next32PostInc, setTostart
        i = 0;
        c=iter.first32PostInc();
        if(c != text.char32At(i))
            errln("first32PostInc failed.  Expected->%X Got->%X", text.char32At(i), c);
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
                errln("Character mismatch at position %d, iterator has %X, string has %X", i, c, text.char32At(i));

            i=UTF16_NEED_MULTIPLE_UCHAR(c) ? i+2 : i+1;
            if(iter.getIndex() != i)
                errln("getIndex() aftr next32PostInc() isn't working right");
            if(iter.current32() != text.char32At(i))
                errln("current() after next32PostInc() isn't working right");
        } while (iter.hasNext());
        c=iter.next32PostInc();
        if(c!= CharacterIterator::DONE)
            errln("next32PostInc() didn't return DONE at the beginning");


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
                errln("Character mismatch at position %d, iterator has %X, string has %X", i, c, text.char32At(i));

            if (iter.current32() != c)
                errln("current32() isn't working right");
            if(iter.setIndex32(i) != c)
                errln("setIndex32() isn't working right");

            if (c != CharacterIterator::DONE) {
                c = iter.next32();
                i=UTF16_NEED_MULTIPLE_UCHAR(c) ? i+2 : i+1;
            }
        } while (c != CharacterIterator::DONE);
        c=iter.next32();
        if(c != CharacterIterator::DONE) 
            errln("error in next32()");


           
        c=iter.last32();
        i = 10;
        logln("Testing backward iteration over a range...");
        do {
            if (c == CharacterIterator::DONE && i >= 5)
                errln("Iterator reached start prematurely");
            else if(iter.hasPrevious() == FALSE && i > 5)
                errln("Iterator reached start prematurely");
            else if (c != text.char32At(i))
                errln("Character mismatch at position %d, iterator has %X, string has %X", i, c, text.char32At(i));
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
        c=iter.previous32();
        if(c!= CharacterIterator::DONE)
            errln("error on previous32");

                
    }
}

void CharIterTest::TestUCharIterator(UCharIterator *iter, CharacterIterator &ci,
                                     const char *moves, const char *which) {
    int32_t m;
    UChar32 c, c2;
    UBool h, h2;

    for(m=0;; ++m) {
        // move both iter and s[index]
        switch(moves[m]) {
        case '0':
            h=iter->hasNext(iter);
            h2=ci.hasNext();
            c=iter->current(iter);
            c2=ci.current();
            break;
        case '|':
            h=iter->hasNext(iter);
            h2=ci.hasNext();
            c=uiter_current32(iter);
            c2=ci.current32();
            break;

        case '+':
            h=iter->hasNext(iter);
            h2=ci.hasNext();
            c=iter->next(iter);
            c2=ci.nextPostInc();
            break;
        case '>':
            h=iter->hasNext(iter);
            h2=ci.hasNext();
            c=uiter_next32(iter);
            c2=ci.next32PostInc();
            break;

        case '-':
            h=iter->hasPrevious(iter);
            h2=ci.hasPrevious();
            c=iter->previous(iter);
            c2=ci.previous();
            break;
        case '<':
            h=iter->hasPrevious(iter);
            h2=ci.hasPrevious();
            c=uiter_previous32(iter);
            c2=ci.previous32();
            break;

        case '2':
            h=h2=FALSE;
            c=(UChar32)iter->move(iter, 2, UITER_CURRENT);
            c2=(UChar32)ci.move(2, CharacterIterator::kCurrent);
            break;

        case '8':
            h=h2=FALSE;
            c=(UChar32)iter->move(iter, -2, UITER_CURRENT);
            c2=(UChar32)ci.move(-2, CharacterIterator::kCurrent);
            break;

        case 0:
            return;
        default:
            errln("error: unexpected move character '%c' in \"%s\"", moves[m], moves);
            return;
        }

        // compare results
        if(c2==0xffff) {
            c2=(UChar32)-1;
        }
        if(c!=c2 || h!=h2 || ci.getIndex()!=iter->getIndex(iter, UITER_CURRENT)) {
            errln("error: UCharIterator(%s) misbehaving at \"%s\"[%d]='%c'", which, moves, m, moves[m]);
        }
    }
}

void CharIterTest::TestUCharIterator() {
    // test string of length 8
    UnicodeString s=UnicodeString("a \\U00010001b\\U0010fffdz", "").unescape();
    const char *const moves=
        "0+++++++++" // 10 moves per line
        "----0-----"
        ">>|>>>>>>>"
        "<<|<<<<<<<"
        "22+>8>-8+2";

    StringCharacterIterator sci(s), compareCI(s);

    UCharIterator sIter, cIter, rIter;

    uiter_setString(&sIter, s.getBuffer(), s.length());
    uiter_setCharacterIterator(&cIter, &sci);
    uiter_setReplaceable(&rIter, &s);

    TestUCharIterator(&sIter, compareCI, moves, "uiter_setString");
    compareCI.setIndex(0);
    TestUCharIterator(&cIter, compareCI, moves, "uiter_setCharacterIterator");
    compareCI.setIndex(0);
    TestUCharIterator(&rIter, compareCI, moves, "uiter_setReplaceable");

    // test move & getIndex some more
    sIter.start=2;
    sIter.index=3;
    sIter.limit=5;
    if( sIter.getIndex(&sIter, UITER_ZERO)!=0 ||
        sIter.getIndex(&sIter, UITER_START)!=2 ||
        sIter.getIndex(&sIter, UITER_CURRENT)!=3 ||
        sIter.getIndex(&sIter, UITER_LIMIT)!=5 ||
        sIter.getIndex(&sIter, UITER_LENGTH)!=s.length()
    ) {
        errln("error: UCharIterator(string).getIndex returns wrong index");
    }

    if( sIter.move(&sIter, 4, UITER_ZERO)!=4 ||
        sIter.move(&sIter, 1, UITER_START)!=3 ||
        sIter.move(&sIter, 3, UITER_CURRENT)!=5 ||
        sIter.move(&sIter, -1, UITER_LIMIT)!=4 ||
        sIter.move(&sIter, -5, UITER_LENGTH)!=3 ||
        sIter.move(&sIter, 0, UITER_CURRENT)!=sIter.getIndex(&sIter, UITER_CURRENT) ||
        sIter.getIndex(&sIter, UITER_CURRENT)!=3
    ) {
        errln("error: UCharIterator(string).move sets/returns wrong index");
    }

    sci=StringCharacterIterator(s, 2, 5, 3);
    uiter_setCharacterIterator(&cIter, &sci);
    if( cIter.getIndex(&cIter, UITER_ZERO)!=0 ||
        cIter.getIndex(&cIter, UITER_START)!=2 ||
        cIter.getIndex(&cIter, UITER_CURRENT)!=3 ||
        cIter.getIndex(&cIter, UITER_LIMIT)!=5 ||
        cIter.getIndex(&cIter, UITER_LENGTH)!=s.length()
    ) {
        errln("error: UCharIterator(character iterator).getIndex returns wrong index");
    }

    if( cIter.move(&cIter, 4, UITER_ZERO)!=4 ||
        cIter.move(&cIter, 1, UITER_START)!=3 ||
        cIter.move(&cIter, 3, UITER_CURRENT)!=5 ||
        cIter.move(&cIter, -1, UITER_LIMIT)!=4 ||
        cIter.move(&cIter, -5, UITER_LENGTH)!=3 ||
        cIter.move(&cIter, 0, UITER_CURRENT)!=cIter.getIndex(&cIter, UITER_CURRENT) ||
        cIter.getIndex(&cIter, UITER_CURRENT)!=3
    ) {
        errln("error: UCharIterator(character iterator).move sets/returns wrong index");
    }
}
