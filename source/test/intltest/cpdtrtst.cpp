/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright International Business Machines Corporation,  2000                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
************************************************************************
*   Date        Name        Description
*   03/09/2000   Madhu        Creation.
************************************************************************/

#include "ittrans.h"
#include "cpdtrtst.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/cpdtrans.h"
#include "intltest.h"
#include <string.h>
#include <stdio.h>

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void CompoundTransliteratorTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln((UnicodeString)"TestSuite CompoundTransliterator API ");
    switch (index) {
     
        case 0: name = "TestConstruction"; if (exec) TestConstruction(); break;
        case 1: name = "TestCloneEqual"; if (exec) TestCloneEqual(); break;
        case 2: name = "TestGetCount"; if (exec) TestGetCount(); break;
		case 3: name = "TestGetSetAdoptTransliterator"; if (exec) TestGetSetAdoptTransliterator(); break;
        case 4: name = "TestTransliterate"; if (exec) TestTransliterate(); break;
       
        default: name = ""; break; /*needed to end loop*/
    }
}
void CompoundTransliteratorTest::TestConstruction(){
	 logln("Testing the construction of the compound Transliterator");
   UnicodeString names[]={"Greek-Latin", "Latin-Devanagari", "Devanagari-Latin", "Latin-Greek"};
   Transliterator* t1=Transliterator::createInstance(names[0]);
   Transliterator* t2=Transliterator::createInstance(names[1]);
   Transliterator* t3=Transliterator::createInstance(names[2]);
   Transliterator* t4=Transliterator::createInstance(names[3]);
   if(t1 == 0 || t2 == 0 || t3 == 0 || t4 == 0){
	   errln("Transliterator construction failed");
	   return;
   }


   Transliterator* transarray1[]={t1};
   Transliterator* transarray2[]={t1, t4};
   Transliterator* transarray3[]={t4, t1, t2};
   Transliterator* transarray4[]={t1, t2, t3, t4};

   Transliterator** transarray[]={transarray1, transarray2, transarray3, transarray4};
   const UnicodeString IDs[]={
	   names[0], 
	   names[0]+";"+names[3], 
	   names[3]+";"+names[1]+";"+names[2], 
	   names[0]+";"+names[1]+";"+names[2]+";"+names[3] 
   };

   uint16_t i=0;
   for(i=0; i<4; i++){

	   CompoundTransliterator *cpdtrans=new CompoundTransliterator(IDs[i]);
	   if(cpdtrans == 0){
		   errln("Construction using CompoundTransliterator(UnicodeString&, Direction, UnicodeFilter*)  failed");
	   }
       else{
		   delete cpdtrans;
	   }
	   
	   CompoundTransliterator *cpdtrans2=new CompoundTransliterator(transarray[i], i+1);
	   if(cpdtrans2 == 0){
		   errln("Construction using CompoundTransliterator(Transliterator* const transliterators[], "
                           "int32_t count, UnicodeFilter* adoptedFilter = 0)  failed");
		   continue;
	   }
	   CompoundTransliterator *copycpd=new CompoundTransliterator(*cpdtrans2);
	   if(copycpd->getCount() != cpdtrans2->getCount() || copycpd->getID() != cpdtrans2->getID()) {
		   errln("Copy construction failed");
		   continue;
	   }
         
	   
	   delete copycpd;
	   delete cpdtrans2;

	}
   delete t1;
   delete t2;
   delete t3;
   delete t4;
 
}

void CompoundTransliteratorTest::TestCloneEqual(){ 
	logln("Testing the clone() and equality operator functions of Compound Transliterator");
	CompoundTransliterator  *ct1=new CompoundTransliterator("Greek-Latin;Latin-Devanagari");
	if(ct1 == 0){
		errln("construction failed");
		return;
	}
	CompoundTransliterator  *ct2=new CompoundTransliterator("Greek-Latin");
    if(ct2 == 0){
		errln("construction failed");
		return;
	}
	CompoundTransliterator *copyct1=new CompoundTransliterator(*ct1);
	if(copyct1 == 0){
		errln("copy construction failed");
		return;
	}
	CompoundTransliterator *copyct2=new CompoundTransliterator(*ct2);
	if(copyct2 == 0){
		errln("copy construction failed");
		return;
	}
	CompoundTransliterator equalct1=*copyct1;
	CompoundTransliterator equalct2=*copyct2;

	if(copyct1->getID()     != ct1->getID()    || copyct2->getID()    != ct2->getID()    || 
		copyct1->getCount() != ct1->getCount() || copyct2->getCount() != ct2->getCount() ||
		copyct2->getID()    == ct1->getID()    || copyct1->getID()    == ct2->getID()    ||
		copyct2->getCount() == ct1->getCount() || copyct1->getCount() == ct2->getCount() ){
		errln("Error: copy constructors failed");
	}

	if(equalct1.getID()     != ct1->getID()        || equalct2.getID()    != ct2->getID()     || 
		equalct1.getID()    != copyct1->getID()    || equalct2.getID()    != copyct2->getID() || 
		equalct1.getCount() != ct1->getCount()     || equalct2.getCount() != ct2->getCount()  ||
		copyct2->getID()    == ct1->getID()        || copyct1->getID()    == ct2->getID()     ||
		equalct1.getCount() != copyct1->getCount() || equalct2.getCount() != copyct2->getCount() ||
		equalct2.getCount() == ct1->getCount()     || equalct1.getCount() == ct2->getCount() ) {
		errln("Error: =operator or copy constructor failed");
	}

	CompoundTransliterator *clonect1a=(CompoundTransliterator*)ct1->clone();
	CompoundTransliterator *clonect1b=(CompoundTransliterator*)equalct1.clone();
	CompoundTransliterator *clonect2a=(CompoundTransliterator*)ct2->clone();
	CompoundTransliterator *clonect2b=(CompoundTransliterator*)copyct2->clone();


	if(clonect1a->getID()  != ct1->getID()       || clonect1a->getCount() != ct1->getCount()        ||
		clonect1a->getID() != clonect1b->getID() || clonect1a->getCount() != clonect1b->getCount()  ||
		clonect1a->getID() != equalct1.getID()   || clonect1a->getCount() != equalct1.getCount()    ||
		clonect1a->getID() != copyct1->getID()   || clonect1a->getCount() != copyct1->getCount()    ||

		clonect2b->getID() != ct2->getID()       || clonect2a->getCount() != ct2->getCount()        ||
		clonect2a->getID() != clonect2b->getID() || clonect2a->getCount() != clonect2b->getCount()  ||
		clonect2a->getID() != equalct2.getID()   || clonect2a->getCount() != equalct2.getCount()    ||
		clonect2b->getID() != copyct2->getID()   || clonect2b->getCount() != copyct2->getCount()  ) {
		errln("Error: clone() failed");
	}
		
    delete ct1;
	delete ct2;
	delete copyct1;
	delete copyct2;
	
}

void CompoundTransliteratorTest::TestGetCount(){
	logln("Testing the getCount() API of CompoundTransliterator");
	CompoundTransliterator *ct1=new CompoundTransliterator("Halfwidth-Fullwidth;Fullwidth-Halfwidth");
	CompoundTransliterator *ct2=new CompoundTransliterator("Unicode-Hex;Hex-Unicode;Cyrillic-Latin;Latin-Cyrillic");
	CompoundTransliterator *ct3=(CompoundTransliterator*)ct1;
	CompoundTransliterator *ct4=new CompoundTransliterator("Latin-Devanagari");
    CompoundTransliterator *ct5=new CompoundTransliterator(*ct4);

 
	if(ct1->getCount() == ct2->getCount() || ct1->getCount() != ct3->getCount() || 
		ct2->getCount() == ct3->getCount() || 
		ct4->getCount() != ct5->getCount() || ct4->getCount() == ct1->getCount() ||
		ct4->getCount() == ct2->getCount() || ct4->getCount() == ct3->getCount()  ||
		ct5->getCount() == ct2->getCount() || ct5->getCount() == ct3->getCount()  ) {
		errln("Error: getCount() failed");
	}
	delete ct1;
	delete ct2;
	delete ct4;
	delete ct5;
}

void CompoundTransliteratorTest::TestGetSetAdoptTransliterator(){
	logln("Testing the getTransliterator() API of CompoundTransliterator");
	UnicodeString ID("Latin-Greek;Greek-Latin;Latin-Devanagari;Devanagari-Latin;Latin-Cyrillic;Cyrillic-Latin;Unicode-Hex;Hex-Unicode");
	CompoundTransliterator *ct1=new CompoundTransliterator(ID);
    if(ct1 == 0){
		errln("CompoundTransliterator construction failed");
		return;
	}
	int32_t count=ct1->getCount();
    UnicodeString *array=split(ID, ';', count);
	int i;
	for(i=0; i < count; i++){
		UnicodeString child= ct1->getTransliterator(i).getID();
		if(child != *(array+i)){
			errln("Error getTransliterator() failed: Expected->" + *(array+i) + " Got->" + child);
		}else {
			logln("OK: getTransliterator() passed: Expected->" + *(array+i) + " Got->" + child);
		}
	}
    
    
	logln("Testing setTransliterator() API of CompoundTransliterator");
	UnicodeString ID2("Hex-Unicode;Unicode-Hex;Latin-Cyrillic;Cyrillic-Latin;Halfwidth-Fullwidth;Fullwidth-Halfwidth");
    array=split(ID2, ';', count);
	Transliterator** transarray=new Transliterator*[count];
	for(i=0;i<count;i++){
		transarray[i]=Transliterator::createInstance(*(array+i));
		logln("The ID for the transltierator created is " + transarray[i]->getID());
	}
    /*setTransliterator and adoptTransliterator */
	
	ct1->setTransliterators(transarray, count);
	if(ct1->getCount() != count || ct1->getID() != "Latin-kana"){
		errln((UnicodeString)"Error: setTransliterators) failed.\n\t Count:- expected->" + count + (UnicodeString)".  got->" + ct1->getCount() +
			                                       (UnicodeString)"\n\tID   :- expected->" + ID2 + (UnicodeString)".  got->" + ct1->getID());
	}
	else{
		logln("OK: setTransliterators() passed"); 
	}
	/*UnicodeString temp;
	for(i=0;i<count-1;i++){
		temp.append(ct1->getTransliterator(i).getID());
		temp.append(";");
	}
    temp.append(ct1->getTransliterator(i).getID());
	if(temp != ID2){
		errln("Error: setTransliterator() failed.  Expected->" + ID2 + "\nGot->" + temp);
	}
	else{
		logln("OK: setTransliterator() passed");
	}*/
	logln("Testing adoptTransliterator() API of CompoundTransliterator");
    Transliterator *transarray2[]={Transliterator::createInstance("Latin-Kana")};
	ct1->adoptTransliterators(transarray2, 1);
	if(ct1->getCount() != 1 || ct1->getID() != "Latin-kana"){
		errln((UnicodeString)"Error: adoptTransliterators) failed.\n\t Count:- expected->1" + (UnicodeString)".  got->" + ct1->getCount() +
			                                       (UnicodeString)"\n\tID   :- expected->Latin-Kana" + (UnicodeString)".  got->" + ct1->getID());
	}
	else{
		logln("OK: adoptTranslterator() passed");
	}
//	delete ct1;
 
}
/**
 * Splits a UnicodeString
 */
UnicodeString* CompoundTransliteratorTest::split(const UnicodeString& str, UChar seperator, int32_t& count) {
    
	//get the count
	int32_t i;
	count =1;
    for(i=0; i<str.length(); i++){
		if(str.charAt(i) == seperator)
			count++;
	}
    // make an array 
    UnicodeString* result = new UnicodeString[count];
    int32_t last = 0;
    int32_t current = 0;
    for (i = 0; i < str.length(); ++i) {
        if (str.charAt(i) == seperator) {
            str.extractBetween(last, i, result[current]);
            last = i+1;
			current++;
        }
    }
    str.extractBetween(last, i, result[current]);
    return result;
}
void CompoundTransliteratorTest::TestTransliterate(){
	logln("Testing the handleTransliterate() API of CompoundTransliterator");
	CompoundTransliterator *ct1=new CompoundTransliterator("Unicode-Hex;Hex-Unicode");
	if(ct1 == 0){
		errln("CompoundTransliterator construction failed");
	}else {
		UnicodeString s("abcabc");
		expect(*ct1, s, s);
		Transliterator::Position index(0, 0);
		UnicodeString rsource2(s);
		UnicodeString expectedResult=s;
		ct1->handleTransliterate(rsource2, index, FALSE);
		expectAux(ct1->getID() + ":String, index(0,0,0), incremental=FALSE", rsource2 + "->" + rsource2, rsource2==expectedResult, expectedResult);
		index=Transliterator::Position(1,3,2);
		UnicodeString rsource3(s);
		ct1->handleTransliterate(rsource3, index, TRUE); 
		expectAux(ct1->getID() + ":String, index(1,2,3), incremental=TRUE", rsource3 + "->" + rsource3, rsource3==expectedResult, expectedResult);

		delete ct1;
	}
   UnicodeString Data[]={
             //ID, input string, transliterated string
             "Unicode-Hex;Hex-Unicode;Unicode-Hex",     "hello",  "\\u0068\\u0065\\u006C\\u006C\\u006F", 
             "Unicode-Hex;Hex-Unicode",                 "hello! How are you?",  "hello! How are you?",
			 "Devanagari-Latin;Latin-Devanagari",        CharsToUnicodeString("\\u092D\\u0948'\\u0930'\\u0935"),  CharsToUnicodeString("\\u092D\\u093E\\u0907\\u0930\\u0935"),
	    	 "Latin-Cyrillic;Cyrillic-Latin",           "a'b'k'd'e'f'g'h'i'j'Shch'shch'zh'h", "abkdefghijShchshchzhh",
			 "Latin-Greek;Greek-Latin",                 "ABGabgAKLMN", "ABGabgAKLMN",
			 "Latin-Arabic;Arabic-Latin",               "Ad'r'a'b'i'k'dh'dd'gh", "Adrabikdhddgh"


  };
	int i;
    for(i=0; i<sizeof(Data)/sizeof(Data[0]); i=i+3){
		CompoundTransliterator *ct2=new CompoundTransliterator(Data[i+0]);
		if(ct1 == 0){
		errln("CompoundTransliterator construction failed for " + Data[i+0]);
		continue;
		}
		expect(*ct2, Data[i+1], Data[i+2]);
	}
    		
}



//======================================================================
// Support methods
//======================================================================
void CompoundTransliteratorTest::expect(const CompoundTransliterator& t,
                                const UnicodeString& source,
                                const UnicodeString& expectedResult) {
   
    UnicodeString rsource(source);
    t.transliterate(rsource);
    expectAux(t.getID() + ":Replaceable", source + "->" + rsource, rsource==expectedResult, expectedResult);

	// Test handleTransliterate (incremental) transliteration -- 
    rsource.remove();
	rsource.append(source);
    Transliterator::Position index(0,source.length(),0);
	t.handleTransliterate(rsource, index, TRUE);
	expectAux(t.getID() + ":handleTransliterate ", source + "->" + rsource, rsource==expectedResult, expectedResult);

}

void CompoundTransliteratorTest::expectAux(const UnicodeString& tag,
                                   const UnicodeString& summary, bool_t pass,
                                   const UnicodeString& expectedResult) {
    if (pass) {
        logln(UnicodeString("(")+tag+") " + prettify(summary));
    } else {
        errln(UnicodeString("FAIL: (")+tag+") "
              + prettify(summary)
              + ", expected " + prettify(expectedResult));
    }
}

