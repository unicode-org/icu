/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*                                                                                       *
*   (C) Copyright International Business Machines Corporation,  2000                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
************************************************************************
*   Date        Name        Description
*   1/03/2000   Madhu        Creation.
************************************************************************/

#include "ittrans.h"
#include "transapi.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/rbt.h"
#include "unicode/unifilt.h"
#include "unicode/cpdtrans.h"
#include <string.h>
#include <stdio.h>
#include "unicode/rep.h"

//Just to make it easier to use with UChar array.
static UnicodeString CharsToUnicodeString(const char* chars)
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
int32_t getInt(UnicodeString str)
{
	int len=str.length();
	char *alias;
	char *buffer=new char[len+1];
	alias=buffer;
	for(int i=0; i< len; i++){
		*alias=(char)str.charAt(i);
		alias++;
	}

	*alias='\0';
//	printf("this is buffer %s and value is %d", buffer, atoi(buffer));
	return atoi(buffer);
}

	    
          

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void TransliteratorAPITest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln((UnicodeString)"TestSuite Transliterator API ");
    switch (index) {
     
        case 0: name = "TestgetInverse"; if (exec) TestgetInverse(); break;
        case 1: name = "TestgetID"; if (exec) TestgetID(); break;
		case 2: name = "TestGetDisplayName"; if (exec) TestGetDisplayName(); break;
        case 3: name = "TestTransliterate1"; if (exec) TestTransliterate1(); break;
        case 4: name = "TestTransliterate2"; if (exec) TestTransliterate2(); break;
        case 5: name = "TestTransliterate3"; if (exec) TestTransliterate3(); break;
        case 6: name = "TestSimpleKeyboardTransliterator"; if (exec) TestSimpleKeyboardTransliterator(); break;
        case 7: name = "TestKeyboardTransliterator1"; if (exec) TestKeyboardTransliterator1(); break;
        case 8: name = "TestKeyboardTransliterator2"; if (exec) TestKeyboardTransliterator2(); break;
        case 9: name = "TestKeyboardTransliterator3"; if (exec) TestKeyboardTransliterator3(); break;
		case 10: name = "TestGetAdoptFilter"; if (exec) TestGetAdoptFilter(); break;
		case 11: name = "TestClone"; if (exec) TestClone(); break;
                 
        default: name = ""; break; /*needed to end loop*/
    }
}


void TransliteratorAPITest::TestgetID() {
         UnicodeString trans="Latin-Greek";
		 UnicodeString ID;
	     Transliterator* t= Transliterator::createInstance(trans);
		 if(t==0)
			 errln("FAIL: construction");
		 else{
		 ID= t->getID();
         if( ID != trans)
            errln("FAIL: getID returned " + ID + " instead of Latin-Greek");
		 }
		
         for (int i=0; i<Transliterator::countAvailableIDs(); i++){
			ID = (UnicodeString) Transliterator::getAvailableID(i);
            t = Transliterator::createInstance(ID);
			if(t == 0){
				errln("FAIL: " + ID);
				continue;
			}
            if(ID != t->getID())
                errln("FAIL: getID() returned " + t->getID() + " instead of " + ID);
         }
		 delete t;

		 
		 Transliterator* t1=Transliterator::createInstance("Latin-Devanagari");
		 Transliterator* t2=Transliterator::createInstance("Latin-Hebrew");
		 if(t1 ==0 || t2 == 0){
			 errln("FAIL: construction");
			 return;
		 }
		 Transliterator* t3=t1->clone();
		 Transliterator* t4=t2->clone();

		 if(t1->getID() != t3->getID() || t2->getID() != t4->getID() || 
			 t1->getID() == t4->getID() || t2->getID() == t3->getID() || 
			 t1->getID()== t4->getID() )
				errln("FAIL: getID or clone failed");

         

		 Transliterator* t5=Transliterator::createInstance("Latin-Devanagari");
		 if(t5 == 0)
			 errln("FAIL: construction");
		 if(t1->getID() != t5->getID() || t5->getID() != t3->getID() || t1->getID() != t3->getID())
			 errln("FAIL: getID or clone failed");
			 

		 delete t1;
		 delete t2;
		 delete t5;
		 

 }
void TransliteratorAPITest::TestgetInverse() {
         Transliterator* t1    = Transliterator::createInstance("Kana-Latin");
         Transliterator* invt1 = Transliterator::createInstance("Latin-Kana");
         Transliterator* t2    = Transliterator::createInstance("Latin-Devanagari");
         Transliterator* invt2 = Transliterator::createInstance("Devanagari-Latin");
         if(t1 == 0 || invt1 == 0 || t2 == 0 || invt2 == 0)
             errln("FAIL: in instantiation");
			 

         Transliterator* inverse1=t1->createInverse();
         Transliterator* inverse2=t2->createInverse();
         if(inverse1->getID() != invt1->getID() || inverse2->getID() != invt2->getID()
            || inverse1->getID() == invt2->getID() || inverse2->getID() == invt1->getID() ) 
            errln("FAIL: getInverse() ");

         UnicodeString TransID[]={
           "Halfwidth-Fullwidth",
           "Fullwidth-Halfwidth",
           "Greek-Latin" ,
           "Latin-Greek", 
           "Arabic-Latin",
           "Latin-Arabic",
           "Kana-Latin",
           "Latin-Kana",
           "Hebrew-Latin",
           "Latin-Hebrew",
           "Cyrillic-Latin", 
           "Latin-Cyrillic", 
           "Devanagari-Latin", 
           "Latin-Devanagari", 
           "Unicode-Hex",
           "Hex-Unicode"
         };
         for(int i=0; i<sizeof(TransID)/sizeof(TransID[0]); i=i+2){
			 t1=Transliterator::createInstance(TransID[i]);
			 if(t1 == 0){
               errln("FAIL: in instantiation for" + TransID[i]);
			   continue;
			 }
             inverse1=t1->createInverse();
             if(inverse1->getID() != TransID[i+1] )
                 errln("FAIL :getInverse() for " + TransID[i] + " returned " + inverse1->getID() + " instead of " + TransID[i+1]);
         }
         delete t1;
		 delete t2;
		 delete invt1;
		 delete invt2;
   
 }
void TransliteratorAPITest::TestClone(){
    Transliterator *t1, *t2, *t3, *t4;
	t1=Transliterator::createInstance("Latin-Devanagari");
	t2=Transliterator::createInstance("Latin-Hebrew");
	if(t1 == 0 || t2 == 0){
		errln("FAIL: construction");
		return;
	}
	t3=t1->clone();
	t4=t2->clone();

	if(t1->getID() != t3->getID() || t2->getID() != t4->getID() )
		errln("FAIL: clone or getID failed");
	if(t1->getID()==t4->getID() || t2->getID()==t3->getID() ||  t1->getID()==t4->getID())
		errln("FAIL: clone or getID failed");
     
	delete t1;
	delete t2;

}

void TransliteratorAPITest::TestGetDisplayName() {
    UnicodeString dispNames[]= { 
             //ID, displayName
              "CurlyQuotes-StraightQuotes" ,"CurlyQuotes to StraightQuotes",
              "Unicode-Hex"                ,"Unicode to Hex Escape",
              "Halfwidth-Fullwidth"        ,"Halfwidth to Fullwidth" ,
              "Latin-Arabic"               ,"Latin to Arabic"      ,
              "Latin-Devanagari"           ,"Latin to Devanagari"  ,
              "Greek-Latin"                ,"Greek to Latin"       ,
              "Arabic-Latin"               ,"Arabic to Latin"      ,
              "Hex-Unicode"                ,"Hex Escape to Unicode",
              "Cyrillic-Latin"             ,"Cyrillic to Latin"    ,
              "Latin-Greek"                ,"Latin to Greek"       ,
              "Latin-Kana"                 ,"Latin to Kana"        ,
              "Latin-Hebrew"               ,"Latin to Hebrew"      ,
              "Kana-Latin"                 ,"Kana to Latin"        
          };
        UnicodeString name="";
		Transliterator* t;
		UnicodeString message;
        for (int32_t i=0; i<sizeof(dispNames)/sizeof(dispNames[0]); i=i+2 ) {
            t = Transliterator::createInstance(dispNames[i+0]);
			if(t==0){
			 errln("FAIL: construction: " + dispNames[i+0]);
			 continue;
			}
			t->getDisplayName(t->getID(), name);
            message="Display name for ID:" + t->getID();
            doTest(message, name, dispNames[i+1]);
            name=""; 
		    t->getDisplayName(t->getID(), Locale::US, name);
			message="Display name for on english locale ID:" + t->getID();
		    doTest(message, name, dispNames[i+1]);
			name="";
			
        }

delete t;
   

} 

void TransliteratorAPITest::TestTransliterate1(){

	     
         UnicodeString Data[]={ 
             //ID, input string, transliterated string
             "Unicode-Hex",         "hello",                               "\\u0068\\u0065\\u006C\\u006C\\u006F" ,
             "Hex-Unicode",         "\\u0068\\u0065\\u006C\\u006C\\u006F", "hello"  ,
             "Latin-Devanagari",    "bhaarata",                            CharsToUnicodeString("\\u092D\\u093E\\u0930\\u0924") ,
             "Devanagari-Latin",    CharsToUnicodeString("\\u092D\\u093E\\u0930\\u0924"),        "bhaaarata" ,
         //  "Contracted-Expanded", CharsToUnicodeString("\\u00C0\\u00C1\\u0042"),               CharsToUnicodeString("\\u0041\\u0300\\u0041\\u0301\\u0042") ,
         //  "Expanded-Contracted", CharsToUnicodeString("\\u0041\\u0300\\u0041\\u0301\\u0042"), CharsToUnicodeString("\\u00C0\\u00C1\\u0042") ,
             "Latin-Arabic",        "aap",                                 CharsToUnicodeString("\\u0627\\u06A4")     ,
             "Arabic-Latin",        CharsToUnicodeString("\\u0627\\u06A4"),                      "aap" 
         };
                  
       
         UnicodeString gotResult;
		 UnicodeString temp;
		 UnicodeString message;
         Transliterator* t;
         logln("Testing transliterate");
        
         for(int i=0;i<sizeof(Data)/sizeof(Data[0]); i=i+3){
			t=Transliterator::createInstance(Data[i+0]);
            if(t==0){
			 errln("FAIL: construction: " + Data[i+0]);
			 continue;
			}
            gotResult = Data[i+1];
            t->transliterate(gotResult);
			message=t->getID() + "->transliterate(UnicodeString, UnicodeString) for\n\t Source:" + Data[i+1];
            doTest(message, gotResult, Data[i+2]);

            //doubt here
            temp=Data[i+1];
            t->transliterate(temp);
			message=t->getID() + "->transliterate(Replaceable) for \n\tSource:" +Data[i][1];
            doTest(message, temp, Data[i+2]);
			
         }
		 delete t;

         
}
void TransliteratorAPITest::TestTransliterate2(){
         //testing tranliterate(String text, int start, int limit, StringBuffer result)
       UnicodeString Data2[]={
             //ID, input string, start, limit, transliterated string
             "Unicode-Hex",         "hello! How are you?",  "0", "5", "\\u0068\\u0065\\u006C\\u006C\\u006F", "\\u0068\\u0065\\u006C\\u006C\\u006F! How are you?" ,
             "Unicode-Hex",         "hello! How are you?",  "7", "12", "\\u0048\\u006F\\u0077\\u0020\\u0061", "hello! \\u0048\\u006F\\u0077\\u0020\\u0061re you?",
             "Hex-Unicode",         CharsToUnicodeString("\\u0068\\u0065\\u006C\\u006C\\u006F\\u0021\\u0020"), "0", "5",  "hello", "hello! "  ,
           //  "Contracted-Expanded", CharsToUnicodeString("\\u00C0\\u00C1\\u0042"),        "1", "2",  CharsToUnicodeString("\\u0041\\u0301"), CharsToUnicodeString("\\u00C0\\u0041\\u0301\\u0042") ,
             "Devanagari-Latin",    CharsToUnicodeString("\\u092D\\u093E\\u0930\\u0924"), "0", "1",  "bha", CharsToUnicodeString("bha\\u093E\\u0930\\u0924") ,
             "Devanagari-Latin",    CharsToUnicodeString("\\u092D\\u093E\\u0930\\u0924"), "1", "2",  "aa", CharsToUnicodeString("\\u092Daa\\u0930\\u0924")  

         };
         logln("\n   Testing transliterate(String, int, int, StringBuffer)");
         Transliterator* t;
         UnicodeString gotResBuf;
		 UnicodeString temp;
         int32_t start, limit;
         for(int32_t i=0; i<sizeof(Data2)/sizeof(Data2[0]); i=i+6){
             t=Transliterator::createInstance(Data2[i+0]);
             if(t==0){
			 errln("FAIL: construction: " + Data2[i+0]);
			 continue;
			 }
		     start=getInt(Data2[i+2]);
			 limit=getInt(Data2[i+3]);
             Data2[i+1].extractBetween(start, limit, gotResBuf);
             t->transliterate(gotResBuf);
			 //  errln("FAIL: calling transliterate on " + t->getID());
             doTest(t->getID() + ".transliterate(UnicodeString, int32_t, int32_t, UnicodeString):(" + start + "," + limit + ")  for \n\t source: " + Data2[i+1], gotResBuf, Data2[i+4]);
          
             temp=Data2[i+1];
			 t->transliterate(temp, start, limit);
			 doTest(t->getID() + ".transliterate(Replaceable, int32_t, int32_t, ):(" + start + "," + limit + ")  for \n\t source: " + Data2[i+1], temp, Data2[i+5]);
         }

         
         logln("\n   Try calling transliterate with illegal start and limit values");
         t=Transliterator::createInstance("Unicode-Hex");
         gotResBuf = temp = "try start greater than limit";
         t->transliterate(gotResBuf, 10, 5);
         if(gotResBuf == temp)
              logln("OK: start greater than limit value handled correctly");
		  else
		 	   errln("FAIL: start greater than limit value returned" + gotResBuf);
        
		 delete t; 


}
void TransliteratorAPITest::TestTransliterate3(){
         UnicodeString rs="This is the replaceable String";
         UnicodeString Data[] = {
             "0",  "0",  "This is the replaceable String",
             "2",  "3",  "Th\\u0069s is the replaceable String",
             "21", "23", "Th\\u0069s is the repl\\u0061\\u0063eable String",
             "14", "17", "Th\\u0069s is t\\u0068\\u0065\\u0020repl\\u0061\\u0063eable String",
             
         };
         int start, limit;
		 UnicodeString message;
		 Transliterator *t=Transliterator::createInstance("Unicode-Hex");
		 if(t == 0)
			 errln("FAIL : construction");
         for(int i=0; i<sizeof(Data)/sizeof(Data[0]); i=i+3){
             start=getInt(Data[i+0]);
             limit=getInt(Data[i+1]);
             t->transliterate(rs, start, limit);
			 message=t->getID() + ".transliterate(ReplaceableString, start, limit):("+start+","+limit+"):";
             doTest(message, rs, Data[i+2]); 
         }
}

void TransliteratorAPITest::TestSimpleKeyboardTransliterator(){
         logln("simple call to transliterate");
		 UErrorCode status=U_ZERO_ERROR;
         Transliterator* t=Transliterator::createInstance("Unicode-Hex");
		 if(t == 0)
			 errln("FAIL : construction");
         Transliterator::Position index={19,20,20};
         UnicodeString rs= "Transliterate this-''";
         UnicodeString insertion="abc";
         UnicodeString expected="Transliterate this-'\\u0061\\u0062\\u0063'";
         t->transliterate(rs, index, insertion, status);
         if(U_FAILURE(status))
			 errln("FAIL: " + t->getID()+ ".translitere(Replaceable, int[], UnicodeString, UErrorCode)-->" + (UnicodeString)u_errorName(status));
         t->finishTransliteration(rs, index);
		 UnicodeString message="transliterate";
		 doTest(message, rs, expected);
          
         logln("try calling transliterate with invalid index values");
         Transliterator::Position index1[]={
             //START, LIMIT, CURSOR
             {10, 10, 12},   //invalid since CURSOR>LIMIT valid:-START <= CURSOR <= LIMIT
             {17, 16, 17},   //invalid since START>LIMIT valid:-0<=START<=LIMIT
             {-1, 16, 14},   //invalid since START<0
             {3,  50, 2 }    //invalid since LIMIT>text.length()
         };
         for(int i=0; i<sizeof(index1)/sizeof(index1[0]); i++){
           status=U_ZERO_ERROR;
           t->transliterate(rs, index1[i], insertion, status);
           if(status == U_ILLEGAL_ARGUMENT_ERROR)
			   logln("OK: invalid index values handled correctly");
           else
			   errln("FAIL: invalid index values didn't throw U_ILLEGAL_ARGUMENT_ERROR throw" + (UnicodeString)u_errorName(status));
         }

		 delete t;
} 
void TransliteratorAPITest::TestKeyboardTransliterator1(){
        UnicodeString Data[]={
            //insertion, buffer
            "a",   "\\u0061"                                           ,
            "bbb", "\\u0061\\u0062\\u0062\\u0062"                      ,
            "ca",  "\\u0061\\u0062\\u0062\\u0062\\u0063\\u0061"        ,
            " ",   "\\u0061\\u0062\\u0062\\u0062\\u0063\\u0061\\u0020" ,
            "",  "\\u0061\\u0062\\u0062\\u0062\\u0063\\u0061\\u0020"   ,
			
			"a",   "\\u0061"                                           ,
			"b",   "\\u0061\\u0062"                                    ,
			"z",   "\\u0061\\u0062\\u007A"                             ,
			"",    "\\u0061\\u0062\\u007A"                              

        };
		Transliterator* t=Transliterator::createInstance("Unicode-Hex");
        //keyboardAux(t, Data);
		Transliterator::Position index = {0, 0, 0};
	    UErrorCode status=U_ZERO_ERROR;
        UnicodeString s;
	int i;
        logln("Testing transliterate(Replaceable, int32_t, UnicodeString, UErrorCode)");
        for (i=0; i<10; i=i+2) {
           UnicodeString log;
           if (Data[i+0] != "") {
               log = s + " + " + Data[i+0] + " -> ";
               t->transliterate(s, index, Data[i+0], status);
               if(U_FAILURE(status)){
                   errln("FAIL: " + t->getID()+ ".transliterate(Replaceable, int32_t[], UnicodeString, UErrorCode)-->" + (UnicodeString)u_errorName(status));
				   continue;
			   }
           }else {
               log = s + " => ";
               t->finishTransliteration(s, index);
           }
		   // Show the start index '{' and the cursor '|'
           displayOutput(s, Data[i+1], log, index);
               
        }
        
		s="";
		status=U_ZERO_ERROR;
        index.start = index.limit = index.cursor = 0;
		logln("Testing transliterate(Replaceable, int32_t, UChar, UErrorCode)");
		for(i=10; i<sizeof(Data)/sizeof(Data[0]); i=i+2){
			UnicodeString log;
             if (Data[i+0] != "") {
               log = s + " + " + Data[i+0] + " -> ";
               UChar c=Data[i+0].charAt(0);
               t->transliterate(s, index, c, status);
               if(U_FAILURE(status))
                   errln("FAIL: " + t->getID()+ ".transliterate(Replaceable, int32_t[], UChar, UErrorCode)-->" + (UnicodeString)u_errorName(status));
			       continue;
			 }else {
               log = s + " => ";
               t->finishTransliteration(s, index);
			 }
		    // Show the start index '{' and the cursor '|'
            displayOutput(s, Data[i+1], log, index); 
		}

       delete t;
}
void TransliteratorAPITest::TestKeyboardTransliterator2(){
          UnicodeString Data[]={
             //insertion, buffer, index[START], index[LIMIT], index[CURSOR]
             //data for Unicode-Hex
             "abc",    "Initial String: add-\\u0061\\u0062\\u0063-",                     "19", "20", "20",
             "a",      "In\\u0069\\u0061tial String: add-\\u0061\\u0062\\u0063-",        "2",  "3",  "2" ,
             "b",      "\\u0062In\\u0069\\u0061tial String: add-\\u0061\\u0062\\u0063-", "0",  "0",  "0" ,
             "",     "\\u0062In\\u0069\\u0061tial String: add-\\u0061\\u0062\\u0063-", "0",  "0",  "0" ,
             //data for Latin-Devanagiri
             "aa",     CharsToUnicodeString("Hindi -\\u0906-"),                    "6",  "7",  "6",
             "maa",    CharsToUnicodeString("Hindi -\\u0906\\u092E\\u093E-"),        "7",  "8",  "7",
             "raa",    CharsToUnicodeString("Hi\\u0930\\u093Endi -\\u0906\\u092E\\u093E-"),        "1", "2", "2",
             "",       CharsToUnicodeString("Hi\\u0930\\u093Endi -\\u0906\\u092E\\u093E-"),        "1", "2", "2"
             //data for contracted-Expanded
          //   CharsToUnicodeString("\\u00C1"), CharsToUnicodeString("Ad\\u0041\\u0301d here:"),             "1",  "2",  "1" ,
          //   CharsToUnicodeString("\\u00C0"), CharsToUnicodeString("Ad\\u0041\\u0301d here:\\u0041\\u0300"), "11", "11", "11",
          //   "",     CharsToUnicodeString("Ad\\u0041\\u0301d here:\\u0041\\u0300"), "11", "11", "11",
         };
        Transliterator *t;
        UnicodeString rs;
        UnicodeString dataStr;
		logln("Testing transliterate(Replaceable, int32_t, UnicodeString, UErrorCode)");       
        
		rs="Initial String: add--";
        t=Transliterator::createInstance("Unicode-Hex");
		if(t == 0)
			errln("FAIL : construction");
		keyboardAux(t, Data, rs, 0, 20);

        rs="Hindi --";
		t=Transliterator::createInstance("Latin-Devanagari");
		if(t == 0)
			errln("FAIL : construction");
        keyboardAux(t, Data, rs, 20, 40);

		
      //  rs="Add here:";
     //   t=Transliterator::createInstance("Contracted-Expanded");
     //   keyboardAux(t, Data, rs, 35, 55);


		delete t;
} 
void TransliteratorAPITest::TestKeyboardTransliterator3(){
	UnicodeString s="This is the main string";
	 UnicodeString Data[] = {
		"0", "0", "0",  "This is the main string",
	    "1", "3", "2",  "Th\\u0069s is the main string",
		"20", "21", "20",  "Th\\u0069s is the mai\\u006E string"
	};
	
	UErrorCode status=U_ZERO_ERROR;
	Transliterator::Position index={0, 0, 0};
	logln("Testing transliterate(Replaceable, int32_t, UErrorCode)");
	Transliterator *t=Transliterator::createInstance("Unicode-Hex");
	if(t == 0)
			errln("FAIL : construction");
	for(int32_t i=0; i<sizeof(Data)/sizeof(Data[0]); i=i+4){
		UnicodeString log;
		index.start=getInt(Data[i+0]);
        index.limit=getInt(Data[i+1]);
        index.cursor=getInt(Data[i+2]);
        t->transliterate(s, index, status);
        if(U_FAILURE(status)){
           errln("FAIL: " + t->getID()+ ".transliterate(Replaceable, int32_t[], UErrorCode)-->" + (UnicodeString)u_errorName(status));
		   continue;
		}
		t->finishTransliteration(s, index);
		log = s + " => ";
		// Show the start index '{' and the cursor '|'
        displayOutput(s, Data[i+3], log, index); 
		}

	delete t;
}

/**
 * Used by TestFiltering().
 */
class TestFilter1 : public UnicodeFilter {
    virtual UnicodeFilter* clone() const {
        return new TestFilter1(*this);
    }
    virtual bool_t contains(UChar c) const {
       if(c=='c' || c=='a' || c=='C' || c=='A')
          return FALSE;
       else
          return TRUE;
    }
};
class TestFilter2 : public UnicodeFilter {
    virtual UnicodeFilter* clone() const {
        return new TestFilter2(*this);
    }
    virtual bool_t contains(UChar c) const {
        if(c=='e' || c=='l')
           return FALSE;
        else
           return TRUE;
    }
};
class TestFilter3 : public UnicodeFilter {
    virtual UnicodeFilter* clone() const {
        return new TestFilter3(*this);
    }
    virtual bool_t contains(UChar c) const {
        if(c=='o' || c=='w')
           return FALSE;
        else
           return TRUE;
    }
};


void TransliteratorAPITest::TestGetAdoptFilter(){
	Transliterator *t=Transliterator::createInstance("Unicode-Hex");
	if(t == 0)
		errln("FAIL : construction");
    const UnicodeFilter *u=t->getFilter();
    if(u != NULL){
      errln("FAIL: getFilter failed. Didn't return null when the transliterator used no filtering");
    }
          
    UnicodeString got, temp, message;
    UnicodeString data="ABCabcbbCBa";
    temp = data;
	t->transliterate(temp);
	t->adoptFilter(new TestFilter1);

    got = data;
	t->transliterate(got);
    UnicodeString exp="A\\u0042Ca\\u0062c\\u0062\\u0062C\\u0042a";
	message="transliteration after adoptFilter(a,A,c,C) ";
    doTest(message, got, exp);
         
    logln("Testing round trip");
    t->adoptFilter((UnicodeFilter*)u);
    if(t->getFilter() == NULL)
       logln("OK: adoptFilter and getFilter round trip worked");
    else
       errln("FAIL: adoptFilter or getFilter round trip failed");  

    got = data;
    t->transliterate(got);
    exp="\\u0041\\u0042\\u0043\\u0061\\u0062\\u0063\\u0062\\u0062\\u0043\\u0042\\u0061";
    message="transliteration after adopting null filter";
    doTest(message, got, exp);
    message="adoptFilter round trip"; 
	doTest("adoptFilter round trip", got, temp);

    t->adoptFilter(new TestFilter2);
    data="heelloe";
	exp="\\u0068eell\\u006Fe";
    got = data;
	t->transliterate(got);
	message="transliteration using (e,l) filter";
    doTest("transliteration using (e,l) filter", got, exp);
	
	data="are well";
	exp="\\u0061\\u0072e\\u0020\\u0077ell";
    got = data;
    t->transliterate(got);
	doTest(message, got, exp);
    
    t->adoptFilter(new TestFilter3);
	data="ho, wow!";
	exp="\\u0068o\\u002C\\u0020wow\\u0021";
    got = data;
    t->transliterate(got);
	message="transliteration using (o,w) filter";
    doTest("transliteration using (o,w) filter", got, exp);

    data="owl";
	exp="ow\\u006C";
    got = data;
    t->transliterate(got);
	doTest("transliteration using (o,w) filter", got, exp);

	delete t;
	
}



void TransliteratorAPITest::keyboardAux(Transliterator *t, UnicodeString DATA[], UnicodeString& s, int32_t begin, int32_t end) {
        Transliterator::Position index = {0, 0, 0};
		UErrorCode status=U_ZERO_ERROR;
        for (int32_t i=begin; i<end; i=i+5) {
            UnicodeString log;
            if (DATA[i+0] != "") {
                 log = s + " + " + DATA[0] + " -> ";
                 index.start=getInt(DATA[i+2]);
                 index.limit=getInt(DATA[i+3]);
                 index.cursor=getInt(DATA[i+4]);
                 t->transliterate(s, index, DATA[i+0], status);
                 if(U_FAILURE(status)){
					 errln("FAIL: " + t->getID()+ ".transliterate(Replaceable, int32_t[], UnicodeString, UErrorCode)-->" + (UnicodeString)u_errorName(status));
				 continue;
				 }
			} else {
               log = s + " => ";
               t->finishTransliteration(s, index);
            }
			 // Show the start index '{' and the cursor '|'
          displayOutput(s, DATA[i+1], log, index);
            
        }
}

void TransliteratorAPITest::displayOutput(const UnicodeString& got, const UnicodeString& expected, UnicodeString& log, Transliterator::Position& index){
		 // Show the start index '{' and the cursor '|'
			UnicodeString a, b, c;
			got.extractBetween(0, index.start, a);
			got.extractBetween(index.start, index.cursor, b);
			got.extractBetween(index.cursor, got.length(), c);
			log.append(a).
				append('{').
				append(b).
				append('|').
				append(c);
            if (got == expected) 
                logln("OK:" + prettify(log));
            else 
                errln("FAIL: " + prettify(log)  + ", expected " + prettify(expected));
}


/*Internal Functions used*/
void TransliteratorAPITest::doTest(const UnicodeString& message, const UnicodeString& result, const UnicodeString& expected){
	    if (result == expected) 
            logln((UnicodeString)"Ok: " + prettify(message) + " passed \"" + prettify(expected) + "\"");
        else 
            errln((UnicodeString)"FAIL:" + message + " failed  Got-->" + prettify(result)+ ", Expected--> " + prettify(expected) );

 }




