/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CREGRTST.C
*
* Modification History:
*        Name                     Description            
*     Madhu Katragadda            Ported for C API, added extra functions and tests
*********************************************************************************
*/

/* C FUNCTIONALITY AND REGRESSION TEST FOR BREAKITERATOR */

#include <stdlib.h>

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/uloc.h"
#include "unicode/ubrk.h"
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "cintltst.h"
#include "cregrtst.h"
#include "ccolltst.h"
#include "cstring.h"

/* -------------------------------------------------------------------------- */
/**
 * "Vector" structure for holding test tables
 * (this strucure is actually a linked list, but we use the name and API of the
 * java.util.Vector class to keep as much of our test code as possible the same.)
 */
struct Vector1 {
     UChar *text;
    struct Vector1 *link;
};
typedef struct Vector1 Vector;

void addElement(Vector *q, const char* string)
{

    Vector *p;

    p=(Vector*)malloc(sizeof(Vector));
    p->text=(UChar*)malloc(sizeof(UChar) * (uprv_strlen(string)+1));
    u_uastrcpy(p->text, string);
    p->link=NULL;
    while(q->link!=NULL)
        q=q->link;
    q->link=p;

}
UChar* addElement2(Vector *q, const UChar* string)
{
    Vector *p;

    p=(Vector*)malloc(sizeof(Vector));
    p->text=(UChar*)malloc(sizeof(UChar) * (u_strlen(string)+1));
    u_strcpy(p->text, string);
    p->link=NULL;
    while(q->link!=NULL)
        q=q->link;
    q->link=p;

    return (UChar *)string;

}

void cleanupVector(Vector *q) {
    Vector *p;
    while(q != NULL) {
        p = q->link;
        free(q->text);
        free(q);
        q = p;
    }
}

int32_t Count(Vector *q)
{
    int32_t c=0;
    while(q!=NULL){
        q=q->link;
        c++;
    }
    return c;
}

UChar* elementAt(Vector *q, int32_t pos)
{
    int32_t i=0;
    if(q==NULL)
        return NULL;
    for(i=0;i<pos;i++)
        q=q->link;
    return (q->text);
}
/* Just to make it easier to use with UChar array.*/
    
UChar* UCharToUCharArray(const UChar uchar)
{
    UChar *buffer;
    UChar *alias;
    buffer=(UChar*)malloc(sizeof(uchar) * 2);
    alias=buffer;
    *alias=uchar;
    alias++;
    *alias=0x0000;
    return buffer;
}


UChar* extractBetween(int32_t start, int32_t end, UChar* text)
{
    UChar* result;
    UChar* temp;
    temp=(UChar*)malloc(sizeof(UChar) * ((u_strlen(text)-start)+1));
    result=(UChar*)malloc(sizeof(UChar) * ((end-start)+1));
    u_strcpy(temp, &text[start]);
    u_strncpy(result, temp, end-start);
    result[end-start] = 0;
    free(temp);
    return result;
}
/* -------------------------------------------------------------------------------------- */
/**
 * BrealIterator Regression Test is medium top level test class for everything in the C BreakIterator API
 * (ubrk.h and ubrk.c).
 */

 

const UChar cannedTestArray[] = {
    0x0001, 0x0002, 0x0003, 0x0004, 0x0020, 0x0021, 0x005c, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0028, 0x0029,
    0x002b, 0x002d, 0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x003c, 0x003d, 0x003e, 0x0041, 0x0042, 0x0043, 0x0044,
    0x0045, 0x005B, 0x005d, 0x005e,    0x005f, 0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x007b, 0x007d, 0x007c, 
    0x002c, 0x00a0, 0x00a2, 0x00a3, 0x00a4, 0x00a5, 0x00a6, 0x00a7, 0x00a8, 0x00a9, 0x00ab, 0x00ad, 0x00ae, 0x00af, 
    0x00b0, 0x00b2, 0x00b3, 0x00b4, 0x00b9, 0x00bb, 0x00bc, 0x00bd, 0x02b0, 0x02b1, 0x02b2, 0x02b3, 0x02b4, 0x0300, 
    0x0301, 0x0302, 0x0303, 0x0304, 0x05d0, 0x05d1, 0x05d2, 0x05d3, 0x05d4, 0x0903, 0x093e, 0x093f, 0x0940, 0x0949, 
    0x0f3a, 0x0f3b, 0x2000, 0x2001, 0x2002, 0x200c, 0x200d, 0x200e, 0x200f, 0x2010, 0x2011, 0x2012, 0x2028, 0x2029, 
    0x202a, 0x203e, 0x203f, 0x2040, 0x20dd, 0x20de, 0x20df, 0x20e0, 0x2160, 0x2161, 0x2162, 0x2163, 0x2164, 0x0000
};




/*--------------------------------------------- */
/* setup methods */
/*--------------------------------------------- */

void AllocateTextBoundary()
{

    cannedTestChars=(UChar*)malloc(sizeof(UChar) * (u_strlen(cannedTestArray) + 10));
    u_uastrcpy(cannedTestChars,"");
    u_uastrcpy(cannedTestChars,"0x0000");
    u_strcat(cannedTestChars, cannedTestArray);

}

void FreeTextBoundary()
{
    free(cannedTestChars);
}

/*Add Word Data*/
void addTestWordData()
{
    int32_t elems;
    
    
    wordSelectionData=(Vector*)malloc(sizeof(Vector));
    wordSelectionData->text=(UChar*)malloc(sizeof(UChar) * 6);
    u_uastrcpy(wordSelectionData->text, "12,34");
    wordSelectionData->link=NULL;
        
    addElement(wordSelectionData, " ");
    free(addElement2(wordSelectionData, UCharToUCharArray((UChar)(0x00A2))));   /*cent sign */
    free(addElement2(wordSelectionData, UCharToUCharArray((UChar)(0x00A3))));   /*pound sign */
    free(addElement2(wordSelectionData, UCharToUCharArray((UChar)(0x00A4))));   /*currency sign */
    free(addElement2(wordSelectionData, UCharToUCharArray((UChar)(0x00A5))));   /*yen sign */
    addElement(wordSelectionData, "alpha-beta-gamma");
    addElement(wordSelectionData, ".");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "Badges");
    addElement(wordSelectionData, "?");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "BADGES");
    addElement(wordSelectionData, "!");
    addElement(wordSelectionData, "?");
    addElement(wordSelectionData, "!");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "We");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "don't");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "need");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "no");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "STINKING");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "BADGES");
    addElement(wordSelectionData, "!");
    addElement(wordSelectionData, "!");
    addElement(wordSelectionData, "!");

    addElement(wordSelectionData, "012.566,5");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "123.3434,900");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "1000,233,456.000");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "1,23.322%");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "123.1222");

    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "$123,000.20");

    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "179.01%");

    addElement(wordSelectionData, "Hello");
    addElement(wordSelectionData, ",");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "how");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "are");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "you");
    addElement(wordSelectionData, " ");
    addElement(wordSelectionData, "X");
    addElement(wordSelectionData, " ");

    addElement(wordSelectionData, "Now");
    addElement(wordSelectionData, "\r");
    addElement(wordSelectionData, "is");
    addElement(wordSelectionData, "\n");
    addElement(wordSelectionData, "the");
    addElement(wordSelectionData, "\r\n");
    addElement(wordSelectionData, "time");
    addElement(wordSelectionData, "\n");
    addElement(wordSelectionData, "\r");
    addElement(wordSelectionData, "for");
    addElement(wordSelectionData, "\r");
    addElement(wordSelectionData, "\r");
    addElement(wordSelectionData, "all");
    addElement(wordSelectionData, " ");

    /* to test for bug #4097779 */
    free(addElement2(wordSelectionData, CharsToUChars("aa\\u0300a")));
    addElement(wordSelectionData, " ");

   /* to test for bug #4098467
     What follows is a string of Korean characters (I found it in the Yellow Pages
     ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
     it correctly), first as precomposed syllables, and then as conjoining jamo.
     Both sequences should be semantically identical and break the same way.
     precomposed syllables... */
    free(addElement2(wordSelectionData, CharsToUChars("\\uc0c1\\ud56d")));
    addElement(wordSelectionData, " ");
    free(addElement2(wordSelectionData, CharsToUChars("\\ud55c\\uc778")));
    addElement(wordSelectionData, " ");
    free(addElement2(wordSelectionData, CharsToUChars("\\uc5f0\\ud569")));
    addElement(wordSelectionData, " ");
    free(addElement2(wordSelectionData, CharsToUChars("\\uc7a5\\ub85c\\uad50\\ud68c")));
    addElement(wordSelectionData, " ");
    /* conjoining jamo... */
    free(addElement2(wordSelectionData, CharsToUChars("\\u1109\\u1161\\u11bc\\u1112\\u1161\\u11bc")));
    addElement(wordSelectionData, " ");
    free(addElement2(wordSelectionData, CharsToUChars("\\u1112\\u1161\\u11ab\\u110b\\u1175\\u11ab")));
    addElement(wordSelectionData, " ");
    free(addElement2(wordSelectionData, CharsToUChars("\\u110b\\u1167\\u11ab\\u1112\\u1161\\u11b8")));
    addElement(wordSelectionData, " ");
    free(addElement2(wordSelectionData, CharsToUChars("\\u110c\\u1161\\u11bc\\u1105\\u1169\\u1100\\u116d\\u1112\\u116c")));
    addElement(wordSelectionData, " ");

    /* this is a test for bug #4117554: the ideographic iteration mark (U+3005) should
       count as a Kanji character for the purposes of word breaking */
    addElement(wordSelectionData, "abc"); 
    free(addElement2(wordSelectionData, CharsToUChars("\\u4e01\\u4e02\\u3005\\u4e03\\u4e03")));
    addElement(wordSelectionData, "abc");

    elems= Count(wordSelectionData);
    log_verbose("In word, the no: of words are: %d\n", elems);
    testWordText = createTestData(wordSelectionData, elems);
    
    
}

const UChar kParagraphSeparator = 0x2029;
const UChar kLineSeparator = 0x2028;

/**
 * Add Sentence Data
 */
void addTestSentenceData()
{
    int32_t elems;
    UChar temp[100];
    UChar *td;

    sentenceSelectionData=(Vector*)malloc(sizeof(Vector));
    sentenceSelectionData->text=(UChar*)malloc(sizeof(UChar) * (strlen("This is a simple sample sentence. ")+1));
    u_uastrcpy(sentenceSelectionData->text, "This is a simple sample sentence. ");
    sentenceSelectionData->link=NULL;
   
   /* addElement(sentenceSelectionData, "This is a simple sample sentence. "); */
    addElement(sentenceSelectionData, "(This is it.) ");
    addElement(sentenceSelectionData, "This is a simple sample sentence. ");
    addElement(sentenceSelectionData, "\"This isn\'t it.\" ");
    addElement(sentenceSelectionData, "Hi! ");
    addElement(sentenceSelectionData, "This is a simple sample sentence. ");
    addElement(sentenceSelectionData, "It does not have to make any sense as you can see. ");
    addElement(sentenceSelectionData, "Nel mezzo del cammin di nostra vita, mi ritrovai in una selva oscura. ");
    addElement(sentenceSelectionData, "Che la dritta via aveo smarrita. ");
    addElement(sentenceSelectionData, "He said, that I said, that you said!! ");

    u_uastrcpy(temp, "Don't rock the boat");
    td = UCharToUCharArray(kParagraphSeparator);
    u_strcat(temp, td);
    free(td);
    addElement2(sentenceSelectionData, temp);

    addElement(sentenceSelectionData, "Because I am the daddy, that is why. ");
    addElement(sentenceSelectionData, "Not on my time (el timo.)! ");

    u_uastrcpy(temp, "So what!!");
    td = UCharToUCharArray(kParagraphSeparator);
    u_strcat(temp, td);
    free(td);
    addElement2(sentenceSelectionData, temp);
    
    addElement(sentenceSelectionData, "\"But now,\" he said, \"I know!\" ");
    addElement(sentenceSelectionData, "Harris thumbed down several, including \"Away We Go\" (which became the huge success Oklahoma!). ");
    addElement(sentenceSelectionData, "One species, B. anthracis, is highly virulent.\n");
    addElement(sentenceSelectionData, "Wolf said about Sounder:\"Beautifully thought-out and directed.\" ");
    addElement(sentenceSelectionData, "Have you ever said, \"This is where\tI shall live\"? ");
    addElement(sentenceSelectionData, "He answered, \"You may not!\" ");
    addElement(sentenceSelectionData, "Another popular saying is: \"How do you do?\". ");
    addElement(sentenceSelectionData, "Yet another popular saying is: \'I\'m fine thanks.\' ");
    addElement(sentenceSelectionData, "What is the proper use of the abbreviation pp.? ");
    addElement(sentenceSelectionData, "Yes, I am definatelly 12\" tall!!");

    /* test for bug #4113835: \n and \r count as spaces, not as paragraph breaks */
    u_uastrcpy(temp, "Now\ris\nthe\r\ntime\n\rfor\r\rall");
    td = UCharToUCharArray(kParagraphSeparator);
    u_strcat(temp, td);
    free(td);
    addElement2(sentenceSelectionData, temp);

    /* test for bug #4117554: Treat fullwidth variants of .!? the same as their
       normal counterparts */
    free(addElement2(sentenceSelectionData, CharsToUChars("I know I'm right\\uff0e ")));
    free(addElement2(sentenceSelectionData, CharsToUChars("Right\\uff1f ")));
    free(addElement2(sentenceSelectionData, CharsToUChars("Right\\uff01 ")));

    /* test for bug #4117554: Break sentence between a sentence terminator and
       opening punctuation */
    addElement(sentenceSelectionData, "no?");
        u_uastrcpy(temp, "(yes)");
        td = CharsToUChars("\\u2029");
     u_strcat(temp, td);
     free(td);
    addElement2(sentenceSelectionData, temp);

    /* test for bug #4158381: Don't break sentence after period if it isn't
       followed by a space */
    addElement(sentenceSelectionData, "Test <code>Flags.Flag</code> class.  ");
     u_uastrcpy(temp, "Another test.");
     td = CharsToUChars("\\u2029");
     u_strcat(temp, td);
     free(td);
    addElement2(sentenceSelectionData, temp);
    
    /* test for bug #4158381: No breaks when there are no terminators around  */
    addElement(sentenceSelectionData, "<P>Provides a set of &quot;lightweight&quot; (all-java<FONT SIZE=\"-2\"><SUP>TM</SUP></FONT> language) components that, to the maximum degree possible, work the same on all platforms.  ");
     u_uastrcpy(temp, "Another test.");
     td = CharsToUChars("\\u2029");
     u_strcat(temp, td);
     free(td);    
    addElement2(sentenceSelectionData, temp);
    
    /* test for bug #4143071: Make sure sentences that end with digits work right */
    addElement(sentenceSelectionData, "Today is the 27th of May, 1998.  ");
    addElement(sentenceSelectionData, "Tomorrow with be 28 May 1998.  ");
     u_uastrcpy(temp, "The day after will be the 30th.");
     td = CharsToUChars("\\u2029");
     u_strcat(temp, td);
     free(td);    
    addElement2(sentenceSelectionData, temp);
    
    /* test for bug #4152416: Make sure sentences ending with a capital
       letter are treated correctly */
    addElement(sentenceSelectionData, "The type of all primitive <code>boolean</code> values accessed in the target VM.  ");
     u_uastrcpy(temp, "Calls to xxx will return an implementor of this interface.");
     td = CharsToUChars("\\u2029");
     u_strcat(temp, td);
     free(td);    
    addElement2(sentenceSelectionData, temp);


    /* test for bug #4152117: Make sure sentence breaking is handling
       punctuation correctly  */
    addElement(sentenceSelectionData, "Constructs a randomly generated BigInteger, uniformly distributed over the range <tt>0</tt> to <tt>(2<sup>numBits</sup> - 1)</tt>, inclusive.  ");
    addElement(sentenceSelectionData, "The uniformity of the distribution assumes that a fair source of random bits is provided in <tt>rnd</tt>.  ");
     u_uastrcpy(temp, "Note that this constructor always constructs a non-negative BigInteger.");
     td = CharsToUChars("\\u2029");
     u_strcat(temp, td);
     free(td);    
    addElement2(sentenceSelectionData, temp);
    
    elems = Count(sentenceSelectionData);
    log_verbose("In sentence: the no: of sentences are %d\n", elems);
    testSentenceText = createTestData(sentenceSelectionData, elems);

    
}

/**
 * Add Line Data
 */
 
void addTestLineData()
{
    int32_t elems;
    
    lineSelectionData=(Vector*)malloc(sizeof(Vector));
    lineSelectionData->text=(UChar*)malloc(sizeof(UChar) * 7);
    u_uastrcpy(lineSelectionData->text, "Multi-");
    lineSelectionData->link=NULL;
    
    /* lineSelectionData->addElement("Multi-"); */
    addElement(lineSelectionData, "Level ");
    addElement(lineSelectionData, "example ");
    addElement(lineSelectionData, "of ");
    addElement(lineSelectionData, "a ");
    addElement(lineSelectionData, "semi-");
    addElement(lineSelectionData, "idiotic ");
    addElement(lineSelectionData, "non-");
    addElement(lineSelectionData, "sensical ");
    addElement(lineSelectionData, "(non-");
    addElement(lineSelectionData, "important) ");
    addElement(lineSelectionData, "sentence. ");

    addElement(lineSelectionData, "Hi  ");
    addElement(lineSelectionData, "Hello ");
    addElement(lineSelectionData, "How\n");
    addElement(lineSelectionData, "are\r");
    
    
    free(addElement2(lineSelectionData, CharsToUChars("you\\u2028"))); /* lineSeperator */
    
    addElement(lineSelectionData, "fine.\t");
    addElement(lineSelectionData, "good.  ");

    addElement(lineSelectionData, "Now\r");
    addElement(lineSelectionData, "is\n");
    addElement(lineSelectionData, "the\r\n");
    addElement(lineSelectionData, "time\n");
    addElement(lineSelectionData, "\r");
    addElement(lineSelectionData, "for\r");
    addElement(lineSelectionData, "\r");
    addElement(lineSelectionData, "all ");

    /* to test for bug #4068133  */
    free(addElement2(lineSelectionData, CharsToUChars("\\u96f6")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u4e00\\u3002")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u4e8c\\u3001")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u4e09\\u3002\\u3001")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u56db\\u3001\\u3002\\u3001")));
       
    
    free(addElement2(lineSelectionData, CharsToUChars("\\u4e94,")));
    
    free(addElement2(lineSelectionData, CharsToUChars("\\u516d.")));

    free(addElement2(lineSelectionData, CharsToUChars("\\u4e03.\\u3001,\\u3002")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u516b")));

    /* to test for bug #4086052 */
    free(addElement2(lineSelectionData, CharsToUChars("foo\\u00a0bar ")));
   
    /* to test for bug #4097920 */
    addElement(lineSelectionData, "dog,");
    addElement(lineSelectionData, "cat,");
    addElement(lineSelectionData, "mouse ");
    addElement(lineSelectionData, "(one)");
    addElement(lineSelectionData, "(two)\n");

    /* to test for bug #4035266 */
    addElement(lineSelectionData, "The ");
    addElement(lineSelectionData, "balance ");
    addElement(lineSelectionData, "is ");
    addElement(lineSelectionData, "$-23,456.78, ");
    addElement(lineSelectionData, "not ");
    addElement(lineSelectionData, "-$32,456.78!\n");

    /* to test for bug #4098467
       What follows is a string of Korean characters (I found it in the Yellow Pages
       ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
       it correctly), first as precomposed syllables, and then as conjoining jamo.
       Both sequences should be semantically identical and break the same way.
       precomposed syllables... */
    free(addElement2(lineSelectionData, CharsToUChars("\\uc0c1\\ud56d ")));
    free(addElement2(lineSelectionData, CharsToUChars("\\ud55c\\uc778 ")));
    free(addElement2(lineSelectionData, CharsToUChars("\\uc5f0\\ud569 ")));
    free(addElement2(lineSelectionData, CharsToUChars("\\uc7a5\\ub85c\\uad50\\ud68c ")));
    /* conjoining jamo... */
    free(addElement2(lineSelectionData, CharsToUChars("\\u1109\\u1161\\u11bc\\u1112\\u1161\\u11bc ")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u1112\\u1161\\u11ab\\u110b\\u1175\\u11ab ")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u110b\\u1167\\u11ab\\u1112\\u1161\\u11b8 ")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u110c\\u1161\\u11bc\\u1105\\u1169\\u1100\\u116d\\u1112\\u116c")));

    /* to test for bug #4117554: Fullwidth .!? should be treated as postJwrd */
    free(addElement2(lineSelectionData, CharsToUChars("\\u4e01\\uff0e")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u4e02\\uff01")));
    free(addElement2(lineSelectionData, CharsToUChars("\\u4e03\\uff1f")));

    elems = Count(lineSelectionData);
    log_verbose("In line: the no: of lines are %d\n", elems);
    testLineText = createTestData(lineSelectionData, elems);

    
}

/*

const UChar* graveS = "S" + (UniChar)0x0300;
const UChar* acuteBelowI = "i" + UCharToUCharArray(0x0317);
const UChar* acuteE = "e" + UCharToUCharArray(0x0301);
const UChar* circumflexA = "a" + UCharToUCharArray(0x0302);
const UChar* tildeE = "e" + UCharToUCharArray(0x0303);
*/

/**
 * Add Character Data
 */
void addTestCharacterData()
{
    int32_t elems;
    UChar temp[10];
    UChar *td;

    characterSelectionData=(Vector*)malloc(sizeof(Vector));
    characterSelectionData->text=(UChar*)malloc(sizeof(UChar) * 2);
    u_uastrcpy(characterSelectionData->text, "B");
    characterSelectionData->link=NULL;

    u_uastrcpy(temp, "S");
     td = UCharToUCharArray(0x0317);
     u_strcat(temp, td);
     free(td);    
    /*u_strcat(temp, UCharToUCharArray(0x0317));*/
    addElement2(characterSelectionData, temp); /* graveS */
  
    u_uastrcpy(temp, "i");
     td = UCharToUCharArray(0x0301);
     u_strcat(temp, td);
     free(td);    
    /*u_strcat(temp, UCharToUCharArray(0x0301));*/
    addElement2(characterSelectionData, temp); /* acuteBelowI */
    
    addElement(characterSelectionData, "m");
    addElement(characterSelectionData, "p");
    addElement(characterSelectionData, "l");
 
    u_uastrcpy(temp, "e");
     td = UCharToUCharArray(0x0301);
     u_strcat(temp, td);
     free(td);    
    addElement2(characterSelectionData, temp);/* acuteE */
    
    addElement(characterSelectionData, " ");
    addElement(characterSelectionData, "s");
 
    u_uastrcpy(temp, "a");
     td = UCharToUCharArray(0x0302);
     u_strcat(temp, td);
     free(td);    
    addElement2(characterSelectionData, temp);/* circumflexA */
    
    addElement(characterSelectionData, "m");
    addElement(characterSelectionData, "p");
    addElement(characterSelectionData, "l");
  
    u_uastrcpy(temp, "e");
     td = UCharToUCharArray(0x0303);
     u_strcat(temp, td);
     free(td);    
    addElement2(characterSelectionData, temp); /* tildeE */
    
    addElement(characterSelectionData, ".");
    addElement(characterSelectionData, "w");
    
    u_uastrcpy(temp, "a");
     td = UCharToUCharArray(0x0302);
     u_strcat(temp, td);
     free(td);    
    addElement2(characterSelectionData, temp);/* circumflexA */

    addElement(characterSelectionData, "w");
    addElement(characterSelectionData, "a");
    addElement(characterSelectionData, "f");
    addElement(characterSelectionData, "q");
    addElement(characterSelectionData, "\n");
    addElement(characterSelectionData, "\r");
    addElement(characterSelectionData, "\r\n");
    addElement(characterSelectionData, "\n");
    addElement(characterSelectionData, "E");
    /* to test for bug #4098467
       What follows is a string of Korean characters (I found it in the Yellow Pages
       ad for the Korean Presbyterian Church of San Francisco, and I hope I transcribed
       it correctly), first as precomposed syllables, and then as conjoining jamo.
       Both sequences should be semantically identical and break the same way.
       precomposed syllables... */
    free(addElement2(characterSelectionData, CharsToUChars("\\uc0c1")));
    free(addElement2(characterSelectionData, CharsToUChars("\\ud56d")));
    addElement(characterSelectionData, " ");
    free(addElement2(characterSelectionData, CharsToUChars("\\ud55c")));
    free(addElement2(characterSelectionData, CharsToUChars("\\uc778")));
    addElement(characterSelectionData, " ");
    free(addElement2(characterSelectionData, CharsToUChars("\\uc5f0")));
    free(addElement2(characterSelectionData, CharsToUChars("\\ud569")));
    addElement(characterSelectionData, " ");
    free(addElement2(characterSelectionData, CharsToUChars("\\uc7a5")));
    free(addElement2(characterSelectionData, CharsToUChars("\\ub85c")));
    free(addElement2(characterSelectionData, CharsToUChars("\\uad50")));
    free(addElement2(characterSelectionData, CharsToUChars("\\ud68c")));
    addElement(characterSelectionData, " ");
    /* conjoining jamo... */
    free(addElement2(characterSelectionData, CharsToUChars("\\u1109\\u1161\\u11bc")));
    free(addElement2(characterSelectionData, CharsToUChars("\\u1112\\u1161\\u11bc")));
    addElement(characterSelectionData, " ");
    free(addElement2(characterSelectionData, CharsToUChars("\\u1112\\u1161\\u11ab")));
    free(addElement2(characterSelectionData, CharsToUChars("\\u110b\\u1175\\u11ab")));
    addElement(characterSelectionData, " ");
    free(addElement2(characterSelectionData, CharsToUChars("\\u110b\\u1167\\u11ab")));
    free(addElement2(characterSelectionData, CharsToUChars("\\u1112\\u1161\\u11b8")));
    addElement(characterSelectionData, " ");
    free(addElement2(characterSelectionData, CharsToUChars("\\u110c\\u1161\\u11bc")));
    free(addElement2(characterSelectionData, CharsToUChars("\\u1105\\u1169")));
    free(addElement2(characterSelectionData, CharsToUChars("\\u1100\\u116d")));
    free(addElement2(characterSelectionData, CharsToUChars("\\u1112\\u116c")));

    elems = Count(characterSelectionData);
    log_verbose("In character: the no: of characters are %d", elems);
    testCharacterText = createTestData(characterSelectionData, elems);
}

UChar* createTestData(Vector *select, int32_t e)
{
  int32_t i, len;
  UChar* result;
  result=(UChar*)malloc(sizeof(UChar) * 2);
  u_uastrcpy(result, "");
  i=0;
  while (i<e) {
      len=u_strlen(result)+1;
      result=(UChar*)realloc(result, sizeof(UChar) * (len + u_strlen(elementAt(select,i))));
      u_strcat(result, elementAt(select,i));
      i++;
  }

  return result;
}

/*---------------------------------------------
   SentenceBreak tests
  --------------------------------------------- */

void TestForwardSentenceSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestSentenceData();
    e = ubrk_open(UBRK_SENTENCE, "en_US", testSentenceText, u_strlen(testSentenceText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
/*    sample(e, testSentenceText); */
    log_verbose("Testing forward sentence selection.....\n");
    doForwardSelectionTest(e, testSentenceText, sentenceSelectionData);
    ubrk_close(e);
    cleanupVector(sentenceSelectionData);
    free(testSentenceText);
    /*free(sentenceSelectionData);*/
}

void TestFirstSentenceSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestSentenceData();
    e = ubrk_open(UBRK_SENTENCE, "en_US", testSentenceText, u_strlen(testSentenceText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing first sentence selection.....\n");
    doFirstSelectionTest(e, testSentenceText, sentenceSelectionData);
    ubrk_close(e);
    cleanupVector(sentenceSelectionData);
    free(testSentenceText);
    /*free(sentenceSelectionData);*/
}

void TestLastSentenceSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestSentenceData();
    e = ubrk_open(UBRK_SENTENCE, "en_US", testSentenceText, u_strlen(testSentenceText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing last sentence selection.....\n");
    doLastSelectionTest(e, testSentenceText, sentenceSelectionData);
    ubrk_close(e);
    cleanupVector(sentenceSelectionData);
    free(testSentenceText);
    /*free(sentenceSelectionData);*/
}

void TestBackwardSentenceSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestSentenceData();
    e = ubrk_open(UBRK_SENTENCE, "en_US", testSentenceText, u_strlen(testSentenceText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing backward sentence selection.....\n");
    doBackwardSelectionTest(e, testSentenceText, sentenceSelectionData);
    ubrk_close(e);
    cleanupVector(sentenceSelectionData);
    free(testSentenceText);
    /*free(sentenceSelectionData);*/
}

void TestForwardSentenceIndexSelection()
{
   UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestSentenceData();
    e = ubrk_open(UBRK_SENTENCE, "en_US", testSentenceText, u_strlen(testSentenceText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing sentence forward index selection.....\n");
    doForwardIndexSelectionTest(e, testSentenceText, sentenceSelectionData);
    ubrk_close(e);
    cleanupVector(sentenceSelectionData);
    free(testSentenceText);
    /*free(sentenceSelectionData);*/
}

void TestBackwardSentenceIndexSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestSentenceData();
    e = ubrk_open(UBRK_SENTENCE, "en_US", testSentenceText, u_strlen(testSentenceText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing sentence backward index selection.....\n");
    doBackwardIndexSelectionTest(e, testSentenceText, sentenceSelectionData);
    ubrk_close(e);
    cleanupVector(sentenceSelectionData);
    free(testSentenceText);
    /*free(sentenceSelectionData);*/
}


void TestSentenceInvariants()
{
    int x;
    UChar *s;
    UChar *tempStr;
AllocateTextBoundary();
    x=u_strlen(cannedTestChars);
    s=(UChar*)malloc(sizeof(UChar) * (x + 15));
    u_strcpy(s, cannedTestChars);
    tempStr = CharsToUChars(".,\\u3001\\u3002\\u3041\\u3042\\u3043\\ufeff");
    u_strcat(s, tempStr);
    free(tempStr);
    log_verbose("Testing sentence Other invariants.....\n");
    doOtherInvariantTest(UBRK_SENTENCE, s);
    free(s);
FreeTextBoundary();
}

/*---------------------------------------------
   WordBreak tests
  --------------------------------------------- */

void TestForwardWordSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestWordData();
    e = ubrk_open(UBRK_WORD, "en_US", testWordText, u_strlen(testWordText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
/*    sample(e, testWordText); */
    log_verbose("Testing forward word selection.....\n");
    doForwardSelectionTest(e, testWordText, wordSelectionData);
    doForwardSelectionTest(e, testWordText, wordSelectionData);
    ubrk_close(e);
    cleanupVector(wordSelectionData);
    free(testWordText);
    /*free(wordSelectionData);*/
}

void TestFirstWordSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestWordData();
    e = ubrk_open(UBRK_WORD, "en_US", testWordText, u_strlen(testWordText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing first word selection.....\n");
    doFirstSelectionTest(e, testWordText, wordSelectionData);
    ubrk_close(e);
    cleanupVector(wordSelectionData);
    free(testWordText);
    /*free(wordSelectionData);*/
}

void TestLastWordSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestWordData();
    e = ubrk_open(UBRK_WORD, "en_US", testWordText, u_strlen(testWordText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing last word selection.....\n");
    doLastSelectionTest(e, testWordText, wordSelectionData);
    ubrk_close(e);
    cleanupVector(wordSelectionData);
    free(testWordText);
    /*free(wordSelectionData);*/
}

void TestBackwardWordSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestWordData();
    e = ubrk_open(UBRK_WORD, "en_US", testWordText, u_strlen(testWordText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing backward word selection.....\n");
    doBackwardSelectionTest(e, testWordText, wordSelectionData);
    ubrk_close(e);
    cleanupVector(wordSelectionData);
    free(testWordText);
    /*free(wordSelectionData);*/
}

void TestForwardWordIndexSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestWordData();
    e = ubrk_open(UBRK_WORD, "en_US", testWordText, u_strlen(testWordText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing forward word index selection.....\n");
    doForwardIndexSelectionTest(e, testWordText, wordSelectionData);
    ubrk_close(e);
    cleanupVector(wordSelectionData);
    free(testWordText);
    /*free(wordSelectionData);*/
}

void TestBackwardWordIndexSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestWordData();
    e = ubrk_open(UBRK_WORD, "en_US", testWordText, u_strlen(testWordText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing backward word index selection.....\n");
    doBackwardIndexSelectionTest(e, testWordText, wordSelectionData);
    ubrk_close(e);
    cleanupVector(wordSelectionData);
    free(testWordText);
    /*free(wordSelectionData);*/
}

void TestWordInvariants()
{
    UChar *s;
    UChar *tempStr;
    int x;
AllocateTextBoundary();
    x=u_strlen(cannedTestChars);
    s=(UChar*)malloc(sizeof(UChar) * (x + 15));
    u_strcpy(s, cannedTestChars);
    tempStr = CharsToUChars("\',.\\u3041\\u3042\\u3043\\u309b\\u309c\\u30a1\\u30a2\\u30a3\\u4e00\\u4e01\\u4e02");
    u_strcat(s, tempStr);
    free(tempStr);
    log_verbose("Testing word break invariant.....\n");
    doBreakInvariantTest(UBRK_WORD, s);
    u_strcpy(s, cannedTestChars);
    tempStr = CharsToUChars("\',.\\u3041\\u3042\\u3043\\u309b\\u309c\\u30a1\\u30a2\\u30a3\\u4e00\\u4e01\\u4e02");
    u_strcat(s, tempStr);
    free(tempStr);
    doOtherInvariantTest(UBRK_WORD, s);
    free(s);
FreeTextBoundary();    
}

/*---------------------------------------------
   LineBreak tests
 --------------------------------------------- */

void TestForwardLineSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestLineData();
    e = ubrk_open(UBRK_LINE, "en_US", testLineText, u_strlen(testLineText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing forward line selection.....\n");
    doForwardSelectionTest(e, testLineText, lineSelectionData);
    ubrk_close(e);
    cleanupVector(lineSelectionData);
    free(testLineText);
}

void TestFirstLineSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestLineData();
    e = ubrk_open(UBRK_LINE, "en_US", testLineText, u_strlen(testLineText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing first line selection.....\n");
    doFirstSelectionTest(e, testLineText, lineSelectionData);
    ubrk_close(e);
    cleanupVector(lineSelectionData);
    free(testLineText);
    /*free(lineSelectionData);*/
}

void TestLastLineSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestLineData();
    e = ubrk_open(UBRK_LINE, "en_US", testLineText, u_strlen(testLineText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing last line selection.....\n");
    doLastSelectionTest(e, testLineText, lineSelectionData);
    ubrk_close(e);
    cleanupVector(lineSelectionData);
    free(testLineText);
    /*free(lineSelectionData);*/
}

void TestBackwardLineSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestLineData();
    e = ubrk_open(UBRK_LINE, "en_US", testLineText, u_strlen(testLineText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing backward line selection.....\n");
    doBackwardSelectionTest(e, testLineText, lineSelectionData);
    ubrk_close(e);
    cleanupVector(lineSelectionData);
    free(testLineText);
    /*free(lineSelectionData);*/
}

void TestForwardLineIndexSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestLineData();
    e = ubrk_open(UBRK_LINE, "en_US", testLineText, u_strlen(testLineText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing forward line index selection.....\n");
    doForwardIndexSelectionTest(e, testLineText, lineSelectionData);
    ubrk_close(e);
    cleanupVector(lineSelectionData);
    free(testLineText);
    /*free(lineSelectionData);*/
}

void TestBackwardLineIndexSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestLineData();
    e = ubrk_open(UBRK_LINE, "en_US", testLineText, u_strlen(testLineText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing backward line index selection.....\n");
    doBackwardIndexSelectionTest(e, testLineText, lineSelectionData);
    ubrk_close(e);
    cleanupVector(lineSelectionData);
    free(testLineText);
    /*free(lineSelectionData);*/
}

void TestLineInvariants()
{
    int errorCount,l;
    int32_t i, j, k;
    UChar c;
    UBreakIterator *e;
    UErrorCode status = U_ZERO_ERROR;
    UChar noBreak[10], dashes[10];
    UBool saw2;
    UChar work[5];
    UChar *s, *ustr;
    int32_t sLen;

AllocateTextBoundary();
    s=(UChar*)malloc(sizeof(UChar) * (u_strlen(cannedTestChars) + 20));
    u_strcpy(s, cannedTestChars);
    ustr = CharsToUChars(".,;:\\u3001\\u3002\\u3041\\u3042\\u3043\\u3044\\u3045\\u30a3\\u4e00\\u4e01\\u4e02");
    u_strcat(s, ustr);
    free(ustr);
    log_verbose("Testing line break Invariant.....\n");
    doBreakInvariantTest(UBRK_LINE, s);
    log_verbose("Testing line other Invariant....\n");
    doOtherInvariantTest(UBRK_LINE, s);



    u_uastrcpy(work, "aaa");
    /* in addition to the other invariants, a line-break iterator should make sure that:
       it doesn't break around the non-breaking characters */
    e = ubrk_open(UBRK_LINE, "en_US", work, u_strlen(work), &status);
    errorCount=0;
    status=U_ZERO_ERROR;
    ustr = CharsToUChars("\\u00a0\\u2007\\u2011\\ufeff");
    u_strcpy(noBreak, ustr);
    free(ustr);
    sLen = u_strlen(s);
    for (i = 0; i < sLen; i++) {
        c = s[i];
        if (c == '\r' || c == '\n' || c == 0x2029 || c == 0x2028 || c == 0x0003)
            continue;
        work[0] = c;
        for (j = 0; j < u_strlen(noBreak); j++) {
            work[1] = noBreak[j];
            for (k = 0; k < sLen; k++) {
                work[2] = s[k];                
                ubrk_setText(e, work, u_strlen(work), &status);
                if(U_FAILURE(status)){
                    log_err("FAIL: Error in opening the word break Iterator in testLineInvaiants:\n %s\n", myErrorName(status));
                    return;
                }
                for (l = ubrk_first(e); l != UBRK_DONE; l = ubrk_next(e))
                    if (l == 1 || l == 2) {
                        log_err("Got break between U+%s  and U+%s\n", austrdup(UCharToUCharArray(work[l - 1])),
                                                          austrdup(UCharToUCharArray(work[l])) );

                        errorCount++;
                        if (errorCount >= 75)
                            return;
                    }
            }
        }
    }

    /* it does break after hyphens (unless they're followed by a digit, a non-spacing mark,
       a currency symbol, a non-breaking space, or a line or paragraph separator) */
    ustr = CharsToUChars("-\\u00ad\\u2010\\u2012\\u2013\\u2014");
    u_strcpy(dashes, ustr);
    free(ustr);

    for (i = 0; i < sLen; i++) {
        work[0] = s[i];
        for (j = 0; j < u_strlen(dashes); j++) {
            work[1] = dashes[j];
            for (k = 0; k < sLen; k++) {
                c = s[k];
                if (u_charType(c) == U_DECIMAL_DIGIT_NUMBER ||
                    u_charType(c) == U_OTHER_NUMBER ||
                    u_charType(c) == U_NON_SPACING_MARK ||
                    u_charType(c) == U_ENCLOSING_MARK ||
                    u_charType(c) == U_CURRENCY_SYMBOL ||
                    u_charType(c) == U_SPACE_SEPARATOR ||
                    u_charType(c) == U_DASH_PUNCTUATION ||
                    u_charType(c) == U_CONTROL_CHAR ||
                    u_charType(c) == U_FORMAT_CHAR ||
                    c == '\n' || c == '\r' || c == 0x2028 || c == 0x2029 ||
                    c == 0x0003 || c == 0x00a0 || c == 0x2007 || c == 0x2011 ||
                    c == 0xfeff)
                    continue;
                work[2] = c;
                ubrk_setText(e, work, u_strlen(work), &status);
                if(U_FAILURE(status)){
                    log_err("FAIL: Error in setting text on the word break Iterator in testLineInvaiants:\n %s \n", myErrorName(status));
                    return;
                }
                saw2 = FALSE;
                for (l = ubrk_first(e); l != UBRK_DONE; l = ubrk_next(e))
                    if (l == 2)
                        saw2 = TRUE;
                if (!saw2) {
                    log_err("Didn't get break between U+%s  and U+%s\n", austrdup(UCharToUCharArray(work[1])),
                        austrdup(UCharToUCharArray(work[2])) );
                    errorCount++;
                    if (errorCount >= 75)
                        return;
                }
            }
        }
    }
   ubrk_close(e);
   free(s);
FreeTextBoundary();
}
/*---------------------------------------------
   CharacterBreak tests
  --------------------------------------------- */

void TestForwardCharacterSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestCharacterData();
    e = ubrk_open(UBRK_CHARACTER, "en_US", testCharacterText, u_strlen(testCharacterText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing forward character selection.....\n");
    doForwardSelectionTest(e, testCharacterText, characterSelectionData);
    ubrk_close(e);
    cleanupVector(characterSelectionData);    
    free(testCharacterText);
    /*free(characterSelectionData);*/
}

void TestFirstCharacterSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestCharacterData();
    e = ubrk_open(UBRK_CHARACTER, "en_US", testCharacterText, u_strlen(testCharacterText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing first character selection.....\n");
    doFirstSelectionTest(e, testCharacterText, characterSelectionData);
    ubrk_close(e);
    cleanupVector(characterSelectionData);    
    free(testCharacterText);
    /*free(characterSelectionData);*/
}

void TestLastCharacterSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestCharacterData();
    e = ubrk_open(UBRK_CHARACTER, "en_US", testCharacterText, u_strlen(testCharacterText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing last character selection.....\n");
    doLastSelectionTest(e, testCharacterText, characterSelectionData);
    ubrk_close(e);
    cleanupVector(characterSelectionData);    
    free(testCharacterText);
    /*free(characterSelectionData);*/
}

void TestBackwardCharacterSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestCharacterData();
    e = ubrk_open(UBRK_CHARACTER, "en_US", testCharacterText, u_strlen(testCharacterText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing backward character selection.....\n");
    doBackwardSelectionTest(e, testCharacterText, characterSelectionData);
    ubrk_close(e);
    cleanupVector(characterSelectionData);    
    free(testCharacterText);
    /*free(characterSelectionData);*/
}

void TestForwardCharacterIndexSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestCharacterData();
    e = ubrk_open(UBRK_CHARACTER, "en_US", testCharacterText, u_strlen(testCharacterText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing forward index character selection.....\n");
    doForwardIndexSelectionTest(e, testCharacterText, characterSelectionData);
    ubrk_close(e);
    cleanupVector(characterSelectionData);    
    free(testCharacterText);
    /*free(characterSelectionData);*/
}

void TestBackwardCharacterIndexSelection()
{
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator *e;
    addTestCharacterData();
    e = ubrk_open(UBRK_CHARACTER, "en_US", testCharacterText, u_strlen(testCharacterText), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    }
    log_verbose("Testing backward character index selection.....\n");
    doBackwardSelectionTest(e, testCharacterText, characterSelectionData);
    ubrk_close(e);
    cleanupVector(characterSelectionData);    
    free(testCharacterText);
    /*free(characterSelectionData);*/
}

void TestCharacterInvariants()
{
   UChar *s;
   UChar *tempStr;

AllocateTextBoundary();
   s=(UChar*)malloc(sizeof(UChar) * (u_strlen(cannedTestChars) + 15));
   u_strcpy(s, cannedTestChars);
   tempStr = CharsToUChars("\\u1100\\u1101\\u1102\\u1160\\u1161\\u1162\\u11a8\\u11a9\\u11aa");
   u_strcat(s, tempStr);
   free(tempStr);
   log_verbose("Testing character break invariant.....\n");
   doBreakInvariantTest(UBRK_CHARACTER, s);
   u_strcpy(s, cannedTestChars);
   tempStr = CharsToUChars("\\u1100\\u1101\\u1102\\u1160\\u1161\\u1162\\u11a8\\u11a9\\u11aa");
   u_strcat(s, tempStr);
   free(tempStr);
   log_verbose("Testing character other invariant.....\n");
   doOtherInvariantTest(UBRK_CHARACTER, s);
   free(s);
FreeTextBoundary();   
}
/*---------------------------------------------
   other tests
  --------------------------------------------- */


void TestPreceding()
{
    int32_t p1, p2, p3, p4, f, p;
    UBreakIterator *e;
    UChar words3[15];
    UErrorCode status = U_ZERO_ERROR;
    u_uastrcpy(words3, "aaa bbb ccc");
    log_verbose("Testting preceding...\n");
    e  = ubrk_open(UBRK_WORD, "en_US", words3, u_strlen(words3), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in ubrk_open() for word breakiterator: %s\n", myErrorName(status));
    }
    
    ubrk_first(e);
    p1 = ubrk_next(e);
    p2 = ubrk_next(e);
    p3 = ubrk_next(e);
    p4 = ubrk_next(e);
    f = ubrk_following(e, p2+1);
    p = ubrk_preceding(e, p2+1);
    
    if (f!=p3) log_err("Error in TestPreceding: %d!=%d\n", (int32_t)f, (int32_t)p3);
    if (p!=p2) log_err("Error in TestPreceding: %d!=%d\n", (int32_t)p, (int32_t)p2);
   
    ubrk_close(e);
}

/**
 * @bug 4068137
 */
void TestEndBehaviour()
{
    int32_t end, previous;
    UErrorCode status = U_ZERO_ERROR;
    UBreakIterator* wb;
    UChar testString[5];
    u_uastrcpy(testString, "boo");
    log_verbose("Testing end behaviour\n");
    wb = ubrk_open(UBRK_WORD, "en_US", testString, u_strlen(testString), &status);
    if(U_FAILURE(status)){
        log_err("FAIL: Error in opening the word break Iterator: %s\n", myErrorName(status));
        return;
    } 
    
   
    end=ubrk_last(wb);
    previous=ubrk_previous(wb);
    log_verbose("end= %d and previous=%d   %d\n", end, previous, ubrk_previous(wb));
    
    
    ubrk_close(wb);
}


/*---------------------------------------------
   Test implementation routines
  --------------------------------------------- */

void doForwardSelectionTest(UBreakIterator* iterator, UChar* testText, Vector* result)
{
    UChar *expectedResult, *selectionResult;
    int32_t lastOffset, offset;
    int32_t forwardSelectionCounter = 0;
    int32_t forwardSelectionOffset = 0;
    
    log_verbose("doForwardSelectionTest text of length: %d\n", u_strlen(testText));

       
    lastOffset = ubrk_first(iterator);
    offset       = ubrk_next(iterator);
    while(offset!=UBRK_DONE && forwardSelectionCounter < Count(result)) {
        
        if (offset != ubrk_current(iterator)){
            log_err("current() failed: it returned %d and offset was %d\n", ubrk_current(iterator), offset);
        }
    expectedResult =elementAt(result, forwardSelectionCounter);
    forwardSelectionOffset += u_strlen(expectedResult);
        
        selectionResult=extractBetween(lastOffset, offset, testText);
        if (offset != forwardSelectionOffset) {
            log_err("\n*** Selection #%d\n expected : %s - length %d\n\rselected : %s - length %d\n", 
                forwardSelectionCounter, austrdup(expectedResult), u_strlen(expectedResult), 
                                         austrdup(selectionResult), u_strlen(selectionResult) );
        }
        log_verbose("#%d [\"%d\",\"%d\"] : %s\n", forwardSelectionCounter, lastOffset, offset, 
                                                            austrdup(selectionResult));

        forwardSelectionCounter++;
        lastOffset = offset;
        offset = ubrk_next(iterator);
        free(selectionResult);
    }
    if (forwardSelectionCounter < Count(result) - 1){
        log_err("\n*** Selection #%d not found at offset %d !!!\n", forwardSelectionCounter, offset);
    }
    else if (forwardSelectionCounter >= Count(result) && offset != UBRK_DONE){
        log_err("\n*** Selection #%d should not exist at offset %d !!!\n", forwardSelectionCounter, offset);
    }
}
void doBackwardSelectionTest(UBreakIterator* iterator, UChar* testText, Vector* result)
{
    UChar* expectedResult;
    UChar* selectionResult;
    int32_t backwardSelectionCounter, neededOffset, lastOffset, offset;
    backwardSelectionCounter = (Count(result) - 1);
    neededOffset = u_strlen(testText);
    lastOffset = ubrk_last(iterator);
    offset = ubrk_previous(iterator);
    
    log_verbose("doBackwardSelectionTest text of length: %d\n", u_strlen(testText));
    while(offset != UBRK_DONE)
    {
        expectedResult = elementAt(result, backwardSelectionCounter);
        neededOffset -= u_strlen(expectedResult);
        selectionResult=extractBetween(offset, lastOffset, testText);
        if(offset != neededOffset) {
            log_err("\n*** Selection #%d\nExpected : %d > %s < \n\rSelected : %d > %s < \n", 
                backwardSelectionCounter, neededOffset, austrdup(expectedResult), 
                                            offset, austrdup(selectionResult) );
        }

        log_verbose("#%d : %s\n", backwardSelectionCounter, selectionResult);
        backwardSelectionCounter--;
        lastOffset = offset;
        offset = ubrk_previous(iterator);
        free(selectionResult);
    }
    if (backwardSelectionCounter >= 0 && offset != UBRK_DONE){
        log_err("*** Selection #%d not found!!!\n", backwardSelectionCounter);
    }
}

void doFirstSelectionTest(UBreakIterator* iterator, UChar* testText, Vector* result)
{
    int32_t selectionStart, selectionEnd;
    UChar* expectedFirstSelection=NULL;
    UChar* tempFirst = NULL;
    UBool success = TRUE;
    
    log_verbose("doFirstSelectionTest.......\n"); 
    
    selectionStart = ubrk_first(iterator);
    selectionEnd   = ubrk_next(iterator);
    if(selectionEnd != UBRK_DONE) {
        
        tempFirst=extractBetween(selectionStart, selectionEnd, testText);
        expectedFirstSelection = elementAt(result,0);

        if(u_strcmp(tempFirst,expectedFirstSelection)!=0) {
            log_err("\n### Error in doFirstSelectionTest. First selection not equal to what expected\n");
            log_err("Expected: %s - length %d\n\rSelected: %s - length %d\n", 
                austrdup(expectedFirstSelection), u_strlen(expectedFirstSelection),
                austrdup(tempFirst), u_strlen(tempFirst));
            success = FALSE;
        }
    }
    else if (selectionStart != 0 || u_strlen(testText)!= 0) {
        log_err("\n### Error in doFirstSelectionTest. Could not get first selection.\n\r start= %d end= %d\n",
                                            selectionStart, selectionEnd);
        success = FALSE;
    }

    if(success) {
        log_verbose("doFirstSelectionTest\n\nExpexcted first selection: %s\nCalculated first selection: %s is correct\n",
            austrdup(expectedFirstSelection), austrdup(tempFirst) );
        
    }
    if(tempFirst!= NULL) {
            free(tempFirst);
    }

}

void doLastSelectionTest(UBreakIterator* iterator, UChar* testText, Vector* result)
{
    int32_t selectionEnd, selectionStart;
    UChar *expectedLastSelection=NULL;
    UChar *tempLast = NULL;
    UBool success = TRUE;
  
    log_verbose("doLastSelectionTest.......\n"); 

    selectionEnd = ubrk_last(iterator);
    selectionStart = ubrk_previous(iterator);

    
    if(selectionStart != UBRK_DONE) {
        tempLast=extractBetween(selectionStart, selectionEnd, testText);
        expectedLastSelection = elementAt(result,Count(result)-1);
        if(u_strcmp(tempLast,expectedLastSelection)!=0) {
            log_err("\n\n### Error in doLastSelectionTest. Last selection not equal to what expected.\n");
            log_err("Expected: %s - length %d\n\r Selected: %s - length %d\n", 
                    austrdup(expectedLastSelection), u_strlen(expectedLastSelection), 
                    austrdup(tempLast), u_strlen(tempLast) );
            success = FALSE;
              
        }
    }
    else if (selectionEnd != 0 || u_strlen(testText)!= 0) {
        log_err("\n### Error in doLastSelectionTest. Could not get last selection. [%d,%d]\n", selectionStart, 
                                                                    selectionEnd);
        success = FALSE;
    }
    if(success) {
        log_verbose("doLastSelectionTest\n\nExpected Last selection: %s \n", austrdup(expectedLastSelection));
        log_verbose("Calculated last Selection: %s is correct\n",  austrdup(tempLast) );
    }
    
    if(tempLast!=NULL) {
            free(tempLast);
    }
}

/**
 * @bug 4052418 4068139
 */
void doForwardIndexSelectionTest(UBreakIterator* iterator, UChar* testText, Vector* result)
{
    int32_t arrayCount, textLength;
    int32_t selBegin, selEnd, current, entry, pos;
    int32_t offset;
    
    log_verbose("doForwardIndexSelectionTest text of length: %d\n", u_strlen(testText));
    arrayCount = Count(result);
    textLength = u_strlen(testText);
   
    for(offset = 0; offset < textLength; offset++) {
        selBegin = ubrk_preceding(iterator, offset);
        selEnd = ubrk_following(iterator, offset);
        
        entry = 0;
        pos = 0;
        if (selBegin != UBRK_DONE) {
            while (pos < selBegin && entry < arrayCount) {
                pos += u_strlen(elementAt(result, entry));
                ++entry;
            }
            if (pos != selBegin) {
                log_err("With offset = %d, got back spurious %d from preceding\n", offset, selBegin);
                continue;
            }
            else {
                pos += u_strlen(elementAt(result, entry));
                ++entry;
            }
        }
        current=ubrk_current(iterator);
        if(pos==current){
             if (pos != selEnd) {
            log_err("With offset = %d, got back erroneous %d from follwoing\n", offset, selEnd);
            continue;
            }
        }
    }
}

/**
 * @bug 4052418 4068139
 */
void doBackwardIndexSelectionTest(UBreakIterator* iterator, UChar* testText, Vector* result)
{
    int32_t arrayCount, textLength;
    int32_t selBegin, selEnd, current, entry, pos;
    int32_t offset;
    
    log_verbose("doBackwardIndexSelectionTest text of length: %d\n", u_strlen(testText));
    arrayCount = Count(result);
    textLength = u_strlen(testText);

    for(offset = textLength-1; offset >= 0; offset--) {
         selBegin = ubrk_preceding(iterator, offset);
         selEnd = ubrk_following(iterator, offset);
        
        entry = 0;
        pos = 0;
        if (selBegin != UBRK_DONE) {
            while (pos < selBegin && entry < arrayCount) {
               pos += u_strlen(elementAt(result, entry));
                ++entry;
            }
            if (pos != selBegin) {
                log_err("With offset = %d, got back spurious %d from preceding\n", offset, selBegin);
                continue;
            }
            else {
                pos += u_strlen(elementAt(result, entry));
                ++entry;
            }
        }
        current=ubrk_current(iterator);
        if(pos==current){
            if (pos != selEnd) {
                log_err("With offset = %d, got back erroneous %d from following\n", offset, selEnd);
                continue;
            }
        }
    }
}



void doBreakInvariantTest(UBreakIteratorType type, UChar* testChars)
{
    int l,k;
    UBreakIterator *tb;
    int32_t i, j;
    UErrorCode status = U_ZERO_ERROR;
    UChar work[4]; 
    UChar breaks[10];
    UChar c;
    UChar *ustr;
    UBool seen2;
    int errorCount = 0;
    status=U_ZERO_ERROR;

    u_uastrcpy(work, "aaa");

    log_verbose("doBreakInvariantTest text of length: %d\n", u_strlen(testChars));
    /* a break should always occur after CR (unless followed by LF), LF, PS, and LS */

    ustr = CharsToUChars("\r\n\\u2029\\u2028");
    u_strcpy(breaks, ustr);
    free(ustr);
    
    tb = ubrk_open(type, "en_US", work, u_strlen(work), &status);
    
    for (i = 0; i < u_strlen(breaks); i++) {
        work[1] = breaks[i];
        for (j = 0; j < u_strlen(testChars); j++) {
            work[0] = testChars[j];
            for (k = 0; k < u_strlen(testChars); k++) {
                c = testChars[k];

                /* if a cr is followed by lf, ps, ls or etx, don't do the check (that's
                   not supposed to work) */
                if (work[1] == '\r' && (c == '\n' || c == 0x2029
                        || c == 0x2028 || c == 0x0003))
                    continue;

                work[2] = testChars[k];
                ubrk_setText(tb, work, u_strlen(work), &status);
                if(U_FAILURE(status)){
                    log_err("ERROR in opening the breakIterator in doVariant Function: %s\n", myErrorName(status));
                }
                seen2 = FALSE;
                for (l = ubrk_first(tb); l != UBRK_DONE; l = ubrk_next(tb)) {
                    if (l == 2)
                        seen2 = TRUE;
                }
                if (!seen2) {
                    log_err("No break between U+%s and U+%s\n", austrdup(UCharToUCharArray(work[1])), 
                        austrdup(UCharToUCharArray(work[2])) );
                    errorCount++;
                    if (errorCount >= 75)
                        return;
                }
            }
        }
    }
    ubrk_close(tb);
}

void doOtherInvariantTest(UBreakIteratorType type , UChar* testChars)
{
    int32_t k;
    UBreakIterator *tb;
    int32_t i, j;
    UErrorCode status = U_ZERO_ERROR;
    UChar work[5]; 
    UChar c;
    int32_t errorCount = 0;
    status=U_ZERO_ERROR;
   
    u_uastrcpy(work, "a\r\na");
    
    log_verbose("doOtherInvariantTest text of length: %d\n", u_strlen(testChars));
    
    tb = ubrk_open(type, "en_us", work, u_strlen(work), &status);
    
    /* a break should never occur between CR and LF */
    for (i = 0; i < u_strlen(testChars); i++) {
        work[0] = testChars[i];
        for (j = 0; j < u_strlen(testChars); j++) {
            work[3] = testChars[j];
            ubrk_setText(tb, work, u_strlen(work), &status);
                if(U_FAILURE(status)){
                    log_err("ERROR in opening the breakIterator in doVariant Function: %s\n", myErrorName(status));
                    }
            for ( k = ubrk_first(tb); k != UBRK_DONE; k = ubrk_next(tb))
                if (k == 2) {
                    log_err("Break between CR and LF in string U+%s, U+d U+a U+%s\n", 
                        austrdup(UCharToUCharArray(work[0])), austrdup(UCharToUCharArray(work[3])) );
                    errorCount++;
                    if (errorCount >= 75)
                        return;
                }
        }
    }

    /* a break should never occur before a non-spacing mark, unless the preceding
       character is CR, LF, PS, or LS */
    u_uastrcpy(work,"aaaa");
    for (i = 0; i < u_strlen(testChars); i++) {
        c = testChars[i];
        if (c == '\n' || c == '\r' || c == 0x2029 || c == 0x2028 || c == 0x0003)
            continue;
        work[1] = c;
        for (j = 0; j < u_strlen(testChars); j++) {
            c = testChars[j];
            if ((u_charType(c) != U_NON_SPACING_MARK) && 
                (u_charType(c) != U_ENCLOSING_MARK))
                continue;
            work[2] = c;
            ubrk_setText(tb, work, u_strlen(work), &status);
                if(U_FAILURE(status)){
                    log_err("ERROR in opening the breakIterator in doOtherVariant Function %s\n", myErrorName(status));
                    }
            for (k = ubrk_first(tb); k != UBRK_DONE; k = ubrk_next(tb))
                if (k == 2) {
                    log_err("Break between U+%s and U+%s\n", austrdup(UCharToUCharArray(work[1])), 
                                            austrdup(UCharToUCharArray(work[2])) );
                    errorCount++;
                    if (errorCount >= 75)
                        return;
                }
        }
    }
    ubrk_close(tb);
}

void sample(UBreakIterator* tb, UChar* text)
{

    int32_t start, end;
    UChar*   substring;
    log_verbose("-------------------------\n");
    log_verbose("%s  of length %d\n", austrdup(text), u_strlen(text));
    
    start = ubrk_first(tb);
    for (end = ubrk_next(tb); end != UBRK_DONE; end = ubrk_next(tb)) {
        substring=extractBetween(start, end, text);
        log_err("[%d,%d] \"%s\" \n", start, end, austrdup(substring) );
        start = end;
        free(substring);
    }
    
}

void addBrkIterRegrTest(TestNode** root);

void addBrkIterRegrTest(TestNode** root)
{

#if 0
    /*  These tests are removed becaue
     *     1.  The test data is completely redundant with that in the C++ break iterator tests
     *     2.  The data here is stale, and I don't want to copy all of the changes from the C++ tests, and
     *     3.  The C API is covered by the API tests.
     */
        
    addTest(root, &TestForwardWordSelection,        "tstxtbd/cregrtst/TestForwardWordSelection"    );
    addTest(root, &TestBackwardWordSelection,       "tstxtbd/cregrtst/TestBackwardWordSelection"   );
    addTest(root, &TestFirstWordSelection,            "tstxtbd/cregrtst/TestFirstWordSelection"    );
    addTest(root, &TestLastWordSelection,            "tstxtbd/cregrtst/TestLastWordSelection"    );
    addTest(root, &TestForwardWordIndexSelection,    "tstxtbd/cregrtst/TestForwardWordIndexSelection");
    addTest(root, &TestBackwardWordIndexSelection,    "tstxtbd/cregrtst/TestBackwardWordIndexSelection");
    addTest(root, &TestForwardSentenceSelection,    "tstxtbd/cregrtst/TestForwardSentenceSelection");
    addTest(root, &TestBackwardSentenceSelection,    "tstxtbd/cregrtst/TestBackwardSentenceSelection");
    addTest(root, &TestFirstSentenceSelection,        "tstxtbd/cregrtst/TestFirstSentenceSelection");
    addTest(root, &TestLastSentenceSelection,        "tstxtbd/cregrtst/TestLastSentenceSelection");
    addTest(root, &TestForwardSentenceIndexSelection,  "tstxtbd/cregrtst/TestForwardSentenceIndexSelection");
    addTest(root, &TestBackwardSentenceIndexSelection, "tstxtbd/cregrtst/TestBackwardSentenceIndexSelection");

    addTest(root, &TestForwardLineSelection,        "tstxtbd/cregrtst/TestForwardLineSelection");
    addTest(root, &TestBackwardLineSelection,        "tstxtbd/cregrtst/TestBackwardLineSelection");
    addTest(root, &TestFirstLineSelection,            "tstxtbd/cregrtst/TestFirstLineSelection");
    addTest(root, &TestLastLineSelection,            "tstxtbd/cregrtst/TestLastLineSelection");
    addTest(root, &TestForwardLineIndexSelection,    "tstxtbd/cregrtst/TestForwardLineIndexSelection");
    addTest(root, &TestBackwardLineIndexSelection,  "tstxtbd/cregrtst/TestBackwardLineIndexSelection");

    addTest(root, &TestForwardCharacterSelection,    "tstxtbd/cregrtst/TestForwardCharacterSelection");
    addTest(root, &TestBackwardCharacterSelection,  "tstxtbd/cregrtst/TestBackwardCharacterSelection");
    addTest(root, &TestFirstCharacterSelection,        "tstxtbd/cregrtst/TestFirstCharacterSelection");
    addTest(root, &TestLastCharacterSelection,        "tstxtbd/cregrtst/TestLastCharacterSelection");
    addTest(root, &TestForwardCharacterIndexSelection,  "tstxtbd/cregrtst/TestForwardCharacterIndexSelection");
    addTest(root, &TestBackwardCharacterIndexSelection, "tstxtbd/cregrtst/TestBackwardCharacterIndexSelection");

    addTest(root, &TestPreceding,    "tstxtbd/cregrtst/TestPreceding");
    addTest(root, &TestEndBehaviour, "tstxtbd/cregrtst/TestEndBehaviour");

    addTest(root, &TestWordInvariants,      "tstxtbd/cregrtst/TestWordInvariants");
    addTest(root, &TestSentenceInvariants,  "tstxtbd/cregrtst/TestSentenceInvariants");
    addTest(root, &TestCharacterInvariants, "tstxtbd/cregrtst/TestCharacterInvariants");
    addTest(root, &TestLineInvariants,      "tstxtbd/cregrtst/TestLineInvariants");
#endif
   
}

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
