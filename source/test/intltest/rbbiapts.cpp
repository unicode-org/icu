/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1999-2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Date        Name        Description
*   12/14/99    Madhu        Creation.
*   01/12/2000  Madhu        updated for changed API
************************************************************************/


#include "unicode/utypes.h"
#include "unicode/unicode.h"
#include "intltest.h"
#include "unicode/rbbi.h"
#include "unicode/schriter.h"
#include "rbbiapts.h"
#include "string.h"
#include "stdio.h"

/**
 * API Test the RuleBasedBreakIterator class
 */



void RBBIAPITest::TestCloneEquals()
{
	   
      UErrorCode status=U_ZERO_ERROR;
      RuleBasedBreakIterator* bi1     = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
	  RuleBasedBreakIterator* biequal = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
      RuleBasedBreakIterator* bi3     = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
      RuleBasedBreakIterator* bi2     = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
      if(U_FAILURE(status)){
		  errln((UnicodeString)"FAIL : in construction");
		  return;
	  }
	  
     
      UnicodeString testString="Testing word break iterators's clone() and equals()";
      bi1->setText(testString);
      bi2->setText(testString);
      biequal->setText(testString);
     
	  bi3->setText("hello");

      logln((UnicodeString)"Testing equals()");
    
	  logln((UnicodeString)"Testing == and !=");
	  if(*bi1 != *biequal || *bi1 == *bi2 || *bi1 == *bi3)
		    errln((UnicodeString)"ERROR:1 RBBI's == and !- operator failed.");  

	  if(*bi2 == *biequal || *bi2 == *bi1  || *biequal == *bi3)
		    errln((UnicodeString)"ERROR:2 RBBI's == and != operator  failed.");  

      
      
      logln((UnicodeString)"Testing clone()");
      RuleBasedBreakIterator* bi1clone=(RuleBasedBreakIterator*)bi1->clone();
      RuleBasedBreakIterator* bi2clone=(RuleBasedBreakIterator*)bi2->clone();

	  if(*bi1clone != *bi1 || *bi1clone  != *biequal  ||  
         *bi1clone == *bi3 || *bi1clone == *bi2)
          errln((UnicodeString)"ERROR:1 RBBI's clone() method failed");
      
      if(*bi2clone == *bi1 || *bi2clone == *biequal ||  
         *bi2clone == *bi3 || *bi2clone != *bi2)
          errln((UnicodeString)"ERROR:2 RBBI's clone() method failed");
      
      if(bi1->getText() != bi1clone->getText()   ||
         bi2clone->getText() != bi2->getText()   || 
          *bi2clone == *bi1clone )
          errln((UnicodeString)"ERROR: RBBI's clone() method failed");

	  delete bi1;
	  delete bi3;
	  delete bi2;
	  delete biequal;
}
void RBBIAPITest::TestgetRules()
{
	UErrorCode status=U_ZERO_ERROR;

	RuleBasedBreakIterator* bi1=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
    RuleBasedBreakIterator* bi2=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
	if(U_FAILURE(status)){
		errln((UnicodeString)"FAIL: in construction");
		return;
	}
    
    
     
     logln((UnicodeString)"Testing toString()");
	 
	 bi1->setText((UnicodeString)"Hello there");
	  

   
     RuleBasedBreakIterator* bi3 =(RuleBasedBreakIterator*)bi1->clone();

     UnicodeString temp=bi1->getRules();
     UnicodeString temp2=bi2->getRules();
     UnicodeString temp3=bi3->getRules();
     if( temp2.compare(temp3) ==0 || temp.compare(temp2) == 0 || temp.compare(temp3) != 0)
         errln((UnicodeString)"ERROR: error in getRules() method");
     
	 delete bi1;
	 delete bi2;
    
}
void RBBIAPITest::TestHashCode()
{
     UErrorCode status=U_ZERO_ERROR;
	 RuleBasedBreakIterator* bi1     = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
   	 RuleBasedBreakIterator* bi3     = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
     RuleBasedBreakIterator* bi2     = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
     if(U_FAILURE(status)){
		 errln((UnicodeString)"FAIL : in construction");
		 return;
	 }
   
     
     logln((UnicodeString)"Testing hashCode()");

	 bi1->setText((UnicodeString)"Hash code");
	 bi2->setText((UnicodeString)"Hash code");
	 bi3->setText((UnicodeString)"Hash code");

	 RuleBasedBreakIterator* bi1clone= (RuleBasedBreakIterator*)bi1->clone();
     RuleBasedBreakIterator* bi2clone= (RuleBasedBreakIterator*)bi2->clone();
  
     if(bi1->hashCode() != bi1clone->hashCode() ||  bi1->hashCode() != bi3->hashCode() ||
         bi1clone->hashCode() != bi3->hashCode() || bi2->hashCode() != bi2clone->hashCode())
         errln((UnicodeString)"ERROR: identical objects have different hasecodes");
      
     if(bi1->hashCode() == bi2->hashCode() ||  bi2->hashCode() == bi3->hashCode() ||
         bi1clone->hashCode() == bi2clone->hashCode() || bi1clone->hashCode() == bi2->hashCode())
         errln((UnicodeString)"ERROR: different objects have same hasecodes");
	 
	 delete bi1;
	 delete bi2;
	 delete bi3;
	        
}
void RBBIAPITest::TestGetSetAdoptText()
{
    logln((UnicodeString)"Testing getText setText ");
	UErrorCode status=U_ZERO_ERROR;
	UnicodeString str1="first string.";
	UnicodeString str2="Second string.";
    RuleBasedBreakIterator* charIter1 = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
	RuleBasedBreakIterator* wordIter1 = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
    if(U_FAILURE(status)){
		errln((UnicodeString)"FAIL : in construction");
			return;
    }


    CharacterIterator* text1= new StringCharacterIterator(str1);
    CharacterIterator* text2= new StringCharacterIterator(str2);
    
    wordIter1->setText(str1);
    if(wordIter1->getText() != *text1)
       errln((UnicodeString)"ERROR:1 error in setText or getText ");
    if(wordIter1->current() != 0)
        errln((UnicodeString)"ERROR:1 setText did not set the iteration position to the beginning of the text, it is" + wordIter1->current() + (UnicodeString)"\n");
    
	wordIter1->next(2);
    
    wordIter1->setText(str2);
    if(wordIter1->current() != 0)
        errln((UnicodeString)"ERROR:2 setText did not reset the iteration position to the beginning of the text, it is" + wordIter1->current() + (UnicodeString)"\n");
    
    
	charIter1->adoptText(text1);
    if( wordIter1->getText() == charIter1->getText() || 
        wordIter1->getText() != *text2 ||  charIter1->getText() != *text1 )
        errln((UnicodeString)"ERROR:2 error is getText or setText()");

    RuleBasedBreakIterator* rb=(RuleBasedBreakIterator*)wordIter1->clone();
	rb->adoptText(text1);
	if(rb->getText() != *text1)
	    errln((UnicodeString)"ERROR:1 error in adoptText ");
	rb->adoptText(text2);
	if(rb->getText() != *text2)
	    errln((UnicodeString)"ERROR:2 error in adoptText ");


     delete wordIter1;
		

 }   
void RBBIAPITest::TestFirstNextFollowing()
{
     int32_t p, q;
	 UErrorCode status=U_ZERO_ERROR;
	 UnicodeString testString="This is a word break. Isn't it? 2.25";
     logln((UnicodeString)"Testing first() and next(), following() with custom rules");
     logln((UnicodeString)"testing word iterator - string :- \"" + prettify(testString) + (UnicodeString)"\"\n");

     RuleBasedBreakIterator* wordIter1 = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
     if(U_FAILURE(status))
		 errln("FAIL : in construction");
	 else{
     wordIter1->setText(testString);
     p = wordIter1->first();
     if(p !=0 )
         errln((UnicodeString)"ERROR: first() returned" + p + (UnicodeString)"instead of 0");
          
	 q=wordIter1->next(9);
	 doTest(testString, p,  q, 20, "This is a word break");
     p=q;
     q=wordIter1->next();
     doTest(testString, p, q, 21, ".");
     p=q;
     q=wordIter1->next(3);
     doTest(testString, p, q, 28, " Isn't ");
     p=q;
     q=wordIter1->next(2);
     doTest(testString, p, q, 31, "it?");
     //logln((UnicodeString)"Testing following(int)");
     q=wordIter1->following(2);
     doTest(testString, 2, q, 4, "is");
     q=wordIter1->following(22);
     doTest(testString, 22, q, 27, "Isn't"); 
     wordIter1->last();   
     p=wordIter1->next();
     q=wordIter1->following(wordIter1->last());
     if(p != RuleBasedBreakIterator::DONE || q != RuleBasedBreakIterator::DONE)
        errln((UnicodeString)"ERROR: next()/following() at last position returned #" + 
		    p + (UnicodeString)" and " + q + (UnicodeString)" instead of" + testString.length() + (UnicodeString)"\n");
     
     }
	 
	 status=U_ZERO_ERROR;
     RuleBasedBreakIterator* charIter1 = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
	 if(U_FAILURE(status))
		 errln("FAIL : in construction");
	 else{
     testString=CharsToUnicodeString("Write hindi here. \\u092d\\u093e\\u0930\\u0924 \\u0938\\u0941\\u0902\\u0926\\u0930 \\u0939\\u094c\\u0964");
     logln((UnicodeString)"testing char iter - string:- \"" + prettify(testString) + (UnicodeString)"\"");
     charIter1->setText(testString);
     p = charIter1->first();
     if(p !=0 )
        errln((UnicodeString)"ERROR: first() returned" + p + (UnicodeString)"instead of 0");
     q=charIter1->next();
     doTest(testString, p, q, 1, "W");
     p=q;
     q=charIter1->next(4);
     doTest(testString, p, q, 5, "rite");
     p=q;                              
     q=charIter1->next(12);
     doTest(testString, p, q, 17, " hindi here.");
     p=q;
     q=charIter1->next(-6);
     doTest(testString, p, q, 11, " here.");
     p=q;
     q=charIter1->next(6);
     doTest(testString, p, q, 17, " here."); 
    // hindi starts here
     p=q;
     q=charIter1->next(4);
     doTest(testString, p, q, 22, " \\u092d\\u093e\\u0930\\u0924");
     p=q;
     q=charIter1->next(2);
     doTest(testString, p, q, 26, " \\u0938\\u0941\\u0902");
	 
     q=charIter1->following(24);
     doTest(testString, 24, q, 26, "\\u0941\\u0902");
     q=charIter1->following(20);
     doTest(testString, 20, q, 21, "\\u0930");
     p=charIter1->following(charIter1->last());
     q=charIter1->next(charIter1->last());
     if(p != RuleBasedBreakIterator::DONE || q != RuleBasedBreakIterator::DONE)
        errln((UnicodeString)"ERROR: following()/next() at last position returned #" +
			p + (UnicodeString)" and " + q + (UnicodeString)" instead of" + testString.length());
     
     
	 }

	 status=U_ZERO_ERROR;
     testString="Hello! how are you? I'am fine. Thankyou. How are you doing? This\n costs $20,00,000.";
     RuleBasedBreakIterator* sentIter1=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createSentenceInstance(Locale::getDefault(), status);
     if(U_FAILURE(status))
		 errln("FAIL : in construction");
	 else{
     logln((UnicodeString)"testing sentence iter - String:- \"" + prettify(testString) + (UnicodeString)"\""); 
     sentIter1->setText(testString);
     p = sentIter1->first();
     if(p !=0 )
        errln((UnicodeString)"ERROR: first() returned" + p + (UnicodeString)"instead of 0");
     q=sentIter1->next();
     doTest(testString, p, q, 7,  "Hello! ");
     p=q;
     q=sentIter1->next(2);
     doTest(testString, p, q, 31, "how are you? I'am fine. ");
     p=q;        
     q=sentIter1->next(-2);
     doTest(testString, p, q, 7, "how are you? I'am fine. ");
     p=q;
     q=sentIter1->next(4);
     doTest(testString, p, q, 60, "how are you? I'am fine. Thankyou. How are you doing? ");
     p=q; 
     q=sentIter1->next();
     doTest(testString, p, q, 83, "This\n costs $20,00,000.");
     q=sentIter1->following(1);
     doTest(testString, 1, q, 7, "ello! ");
     q=sentIter1->following(10);
     doTest(testString, 10, q, 20,  " are you? ");
     q=sentIter1->following(20);
     doTest(testString, 20, q, 31, "I'am fine. ");  
     p=sentIter1->following(sentIter1->last());
     q=sentIter1->next(sentIter1->last());
     if(p != RuleBasedBreakIterator::DONE || q != RuleBasedBreakIterator::DONE)
        errln((UnicodeString)"ERROR: following()/next() at last position returned #" + 
		    p + (UnicodeString)" and " + q + (UnicodeString)" instead of" + testString.length());
     
	 }

     status=U_ZERO_ERROR;
	 testString=CharsToUnicodeString("Hello! how\r\n (are)\r you? I'am fine- Thankyou. foo\\u00a0bar How, are, you? This, costs $20,00,000.");     
	 logln("(UnicodeString)testing line iter - String:- \"" + prettify(testString) + (UnicodeString)"\""); 
     RuleBasedBreakIterator* lineIter1=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createLineInstance(Locale::getDefault(), status);
     if(U_FAILURE(status))
		 errln("FAIL : in construction");
	 else{
	 lineIter1->setText(testString);
     p = lineIter1->first();
     if(p !=0 )
        errln((UnicodeString)"ERROR: first() returned" + p + (UnicodeString)"instead of 0");
     q=lineIter1->next();
     doTest(testString, p, q, 7,  "Hello! ");
     p=q;
     p=q;
     q=lineIter1->next(4);
     doTest(testString, p, q, 20, "how\r\n (are)\r ");
     p=q;        
     q=lineIter1->next(-4);
     doTest(testString, p, q, 7, "how\r\n (are)\r ");
     p=q;
     q=lineIter1->next(6);
     doTest(testString, p, q, 30, "how\r\n (are)\r you? I'am ");
     p=q;
     q=lineIter1->next();
     doTest(testString, p, q, 36, "fine- ");
     p=q;
     q=lineIter1->next(2);
     doTest(testString, p, q, 54, "Thankyou. foo\\u00a0bar ");
     q=lineIter1->following(60);
     doTest(testString, 60, q, 64, "re, ");
     q=lineIter1->following(1);
     doTest(testString, 1, q, 7, "ello! ");
     q=lineIter1->following(10);
     doTest(testString, 10, q, 12,  "\r\n");
     q=lineIter1->following(20);
     doTest(testString, 20, q, 25, "you? ");  
     p=lineIter1->following(lineIter1->last());
     q=lineIter1->next(lineIter1->last());
     if(p != RuleBasedBreakIterator::DONE || q != RuleBasedBreakIterator::DONE)
        errln((UnicodeString)"ERROR: following()/next() at last position returned #" + 
		    p + (UnicodeString)" and " + q + (UnicodeString)" instead of" + testString.length());
     }
    delete wordIter1;
	delete charIter1;
    delete sentIter1;
	delete lineIter1;
  }
void RBBIAPITest::TestLastPreviousPreceding()
{
	  int32_t p, q;
	  UErrorCode status=U_ZERO_ERROR;
	  UnicodeString testString="This is a word break. Isn't it? 2.25 dollars";
      logln((UnicodeString)"Testing last(),previous(), preceding() with custom rules");
      logln((UnicodeString)"testing word iteration for string \"" + prettify(testString) + (UnicodeString)"\"");

	  RuleBasedBreakIterator *wordIter1 = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
      if(U_FAILURE(status))
		  errln("FAIL : in construction");
	  else{
      wordIter1->setText(testString);
      p = wordIter1->last();
      if(p !=testString.length() ){
          errln((UnicodeString)"ERROR: first() returned" + p + (UnicodeString)"instead of" + testString.length());
      }

      q=wordIter1->previous();
      doTest(testString, p, q, 37, "dollars");
      p=q;
      q=wordIter1->previous();
      doTest(testString, p, q, 36, " ");
      q=wordIter1->preceding(25);
      doTest(testString, 25, q, 22, "Isn");
      p=q;
      q=wordIter1->previous();
      doTest(testString, p, q, 21, " ");
      q=wordIter1->preceding(20);
      doTest(testString, 20, q, 15, "break");  
      p=wordIter1->preceding(wordIter1->first());
      if(p != RuleBasedBreakIterator::DONE)
         errln((UnicodeString)"ERROR: preceding()  at starting position returned #" + p + (UnicodeString)" instead of 0");

      }

      status=U_ZERO_ERROR;
      testString=CharsToUnicodeString("Write hindi here. \\u092d\\u093e\\u0930\\u0924 \\u0938\\u0941\\u0902\\u0926\\u0930 \\u0939\\u094c\\u0964");
      logln((UnicodeString)"testing character iteration for string \" " + prettify(testString) + (UnicodeString)"\" \n");
      
	  
	  RuleBasedBreakIterator *charIter1 = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
	  if(U_FAILURE(status))
		  errln("FAIL : in construction");
	  else{	  
	  charIter1->setText(testString);
      p = charIter1->last();
      if(p != testString.length() )
          errln((UnicodeString)"ERROR: first() returned" + p + (UnicodeString)"instead of" + testString.length());
      q=charIter1->previous();
      doTest(testString, p, q, 31, "\\u0964");
      p=q;
      q=charIter1->previous();
      doTest(testString, p, q, 29, "\\u0939\\u094c");
      q=charIter1->preceding(26);
      doTest(testString, 26, q, 23, "\\u0938\\u0941\\u0902");
      q=charIter1->preceding(16);
      doTest(testString, 16, q, 15, "e");
      p=q;
      q=charIter1->previous();
      doTest(testString, p, q, 14, "r"); 
      charIter1->first();
      p=charIter1->previous();
      q=charIter1->preceding(charIter1->first());
      if(p != RuleBasedBreakIterator::DONE || q != RuleBasedBreakIterator::DONE)
          errln((UnicodeString)"ERROR: previous()/preceding() at starting position returned #" +
		      p + (UnicodeString)" and " + q + (UnicodeString)" instead of 0\n");      
      
	  }


	  status=U_ZERO_ERROR;
	  testString="Hello! how are you? I'am fine. Thankyou. How are you doing? This\n costs $20,00,000.";
      logln((UnicodeString)"testing sentence iter - String:- \"" + prettify(testString) + (UnicodeString)"\""); 

	  
      RuleBasedBreakIterator* sentIter1=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createSentenceInstance(Locale::getDefault(), status);
	  if(U_FAILURE(status))
		  errln("FAIL : in construction");
	  else{
      sentIter1->setText(testString);
      p = sentIter1->last();
      if(p != testString.length() )
        errln((UnicodeString)"ERROR: last() returned" + p + (UnicodeString)"instead of " + testString.length());
      q=sentIter1->previous();
      doTest(testString, p, q, 60, "This\n costs $20,00,000.");
      p=q;
      q=sentIter1->previous();
      doTest(testString, p, q, 41, "How are you doing? ");
      q=sentIter1->preceding(40);
      doTest(testString, 40, q, 31, "Thankyou.");
      q=sentIter1->preceding(25);
      doTest(testString, 25, q, 20, "I'am "); 
      sentIter1->first();
      p=sentIter1->previous();
      q=sentIter1->preceding(sentIter1->first());
      if(p != RuleBasedBreakIterator::DONE || q != RuleBasedBreakIterator::DONE)
          errln((UnicodeString)"ERROR: previous()/preceding() at starting position returned #" +
		      p + (UnicodeString)" and " + q + (UnicodeString)" instead of 0\n");
      
	  }

	  status=U_ZERO_ERROR;
      testString="Hello! how are you? I'am fine. Thankyou. How are you doing? This\n costs $20,00,000.";
      logln((UnicodeString)"testing line iter - String:- \"" + prettify(testString) + (UnicodeString)"\""); 

	  RuleBasedBreakIterator* lineIter1=(RuleBasedBreakIterator*)RuleBasedBreakIterator::createLineInstance(Locale::getDefault(), status);
      if(U_FAILURE(status))
		  errln("FAIL : in construction");
	  else{
      lineIter1->setText(testString);
      p = lineIter1->last();
      if(p != testString.length() )
        errln((UnicodeString)"ERROR: last() returned" + p + (UnicodeString)"instead of " + testString.length());
      q=lineIter1->previous();
      doTest(testString, p, q, 72, "$20,00,000.");
      p=q;
      q=lineIter1->previous();
      doTest(testString, p, q, 66, "costs ");
      q=lineIter1->preceding(40);
      doTest(testString, 40, q, 31, "Thankyou.");
      q=lineIter1->preceding(25);
      doTest(testString, 25, q, 20, "I'am "); 
      lineIter1->first();
      p=lineIter1->previous();
      q=lineIter1->preceding(sentIter1->first());
      if(p != RuleBasedBreakIterator::DONE || q != RuleBasedBreakIterator::DONE)
          errln((UnicodeString)"ERROR: previous()/preceding() at starting position returned #" + 
		      p + (UnicodeString)" and " +  q + (UnicodeString)" instead of 0\n");

	  }
      
	  delete sentIter1;
	  delete charIter1;
	  delete wordIter1;
	  delete lineIter1;
}
void RBBIAPITest::TestIsBoundary(){
	UErrorCode status=U_ZERO_ERROR;
	UnicodeString testString1=CharsToUnicodeString("Write here. \\u092d\\u093e\\u0930\\u0924 \\u0938\\u0941\\u0902\\u0926\\u0930 \\u0939\\u094c\\u0964");

    RuleBasedBreakIterator* charIter1 = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createCharacterInstance(Locale::getDefault(), status);
	if(U_FAILURE(status))
		errln("FAIL: in construction");
	else{
         charIter1->setText(testString1);
		 int32_t bounds1[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 15, 16, 17, 20, 21, 22, 23, 25, 26};
		 doBoundaryTest(*charIter1, testString1, bounds1);
	}


	RuleBasedBreakIterator* wordIter2 = (RuleBasedBreakIterator*)RuleBasedBreakIterator::createWordInstance(Locale::getDefault(), status);
    if(U_FAILURE(status))
		errln("FAIL : in construction");
	else{  
		wordIter2->setText(testString1);
		int32_t bounds2[] = {0, 5, 6, 10, 11, 12, 16, 17, 22, 23, 26};
		doBoundaryTest(*wordIter2, testString1, bounds2);
	}
    delete wordIter2;
	delete charIter1;
}

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void RBBIAPITest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln((UnicodeString)"TestSuite RuleBasedBreakIterator API ");
    switch (index) {
     //   case 0: name = "TestConstruction"; if (exec) TestConstruction(); break;
        case 0: name = "TestCloneEquals"; if (exec) TestCloneEquals(); break;
        case 1: name = "TestgetRules"; if (exec) TestgetRules(); break;
        case 2: name = "TestHashCode"; if (exec) TestHashCode(); break;
        case 3: name = "TestGetSetAdoptText"; if (exec) TestGetSetAdoptText(); break;
        case 4: name = "TestFirstNextFollowing"; if (exec) TestFirstNextFollowing(); break;
        case 5: name = "TestLastPreviousPreceding"; if (exec) TestLastPreviousPreceding(); break;
        case 6: name = "TestIsBoundary"; if (exec) TestIsBoundary(); break;
                   
        default: name = ""; break; /*needed to end loop*/
    }
}

//---------------------------------------------
//Internal subroutines
//---------------------------------------------

void RBBIAPITest::doBoundaryTest(RuleBasedBreakIterator& bi, UnicodeString& text, int32_t *boundaries){
     logln((UnicodeString)"testIsBoundary():");
        int32_t p = 0;
        bool_t isB;
        for (int32_t i = 0; i < text.length(); i++) {
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
void RBBIAPITest::doTest(UnicodeString& testString, int32_t start, int32_t gotoffset, int32_t expectedOffset, const char* expectedString){
    UnicodeString selected;
	UnicodeString expected=CharsToUnicodeString(expectedString);

    if(gotoffset != expectedOffset)
         errln((UnicodeString)"ERROR:****returned #" + gotoffset + (UnicodeString)" instead of #" + expectedOffset);
    if(start <= gotoffset){
        testString.extractBetween(start, gotoffset, selected);  
    }
    else{
        testString.extractBetween(gotoffset, start, selected);
    }
    if(selected.compare(expected) != 0)
         errln(prettify((UnicodeString)"ERROR:****selected \"" + selected + "\" instead of \"" + expected + "\""));
    else
        logln(prettify("****selected \"" + selected + "\""));
   }  
       



  
