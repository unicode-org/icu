/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/************************************************************************
*   Date        Name        Description
*   10/03/2001   Ram        Creation.
************************************************************************/

#include "ittrans.h"
#include "indictrn.h"
#include "unicode/utypes.h"
#include "unicode/translit.h"
#include "unicode/rbt.h"
#include "unicode/unifilt.h"
#include "unicode/cpdtrans.h"
#include "unicode/nultrans.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include "unicode/rep.h"
#include "unicode/locid.h"

//---------------------------------------------
// runIndexedTest
//---------------------------------------------

void IndicLatinTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    if (exec) logln((UnicodeString)"TestSuite Indic Latin Rules ");
    switch (index) {
        case 0: name = "TestDevanagariLatinRT";if(exec) TestDevanagariLatinRT();break;
        case 1: name = "TestTeluguLatinRT";if(exec) TestTeluguLatinRT();break;
        case 2: name = "TestSanskritLatinRT";if(exec) TestSanskritLatinRT();break;
        case 3: name = "TestCompoundLatinRT";if(exec) TestCompoundLatinRT();break;
       /* case 2: name = "TestGujaratiLatinRT";if(exec) TestGujaratiLatinRT();break;
        case 3: name = "TestTamilLatinRT";if(exec) TestTamilLatinRT();break;
        case 4: name = "TestMalayalamLatinRT";if(exec) TestMalayalamLatinRT();break;
        case 5: name = "TestGurmukhiLatinRT";if(exec) TestGurmukhiLatinRT();break;
        case 6: name = "TestBengaliLatinRT";if(exec) TestBengaliLatinRT();break;
        case 7: name = "TestOriyaLatinRT";if(exec) TestOriyaLatinRT();break;
        case 8: name = "TestTamilLatinRT";if(exec) TestTamilLatinRT();break;*/
        default: name = ""; break; /*needed to end loop*/
    }
}
/*Internal Functions used*/
void IndicLatinTest::doTest(const UnicodeString& message, const UnicodeString& result, const UnicodeString& expected){
    if (prettify(result) == prettify(expected)) 
        logln((UnicodeString)"Ok: " + prettify(message) + " passed \"" + prettify(expected) + "\"");
    else 
        errln((UnicodeString)"FAIL:" + message + " failed  Got-->" + prettify(result)+ ", Expected--> " + prettify(expected) );
}
/* this test performs  test of rules in ISO 15915 */
void IndicLatinTest::TestDevanagariLatinRT(){
    const int MAX_LEN= 52;
    const char* const source[MAX_LEN] = {
        "bh\\u0101rata",
        "kra",
        "k\\u1E63a",
        "khra",
        "gra",
        "\\u1E45ra",
        "cra",
        "chra",
        "j\\u00F1a",
        "jhra",
        "\\u00F1ra",
        "\\u1E6Dya",
        "\\u1E6Dhra",
        "\\u1E0Dya",
      //"r\\u0323ya", // \u095c is not valid in Devanagari
        "\\u1E0Dhya",
        "\\u1E5Bhra",
        "\\u1E47ra",
        "tta",
        "thra",
        "dda",
        "dhra",
        "nna",
        "pra",
        "phra",
        "bra",
        "bhra",
        "mra",
        "\\u1E49ra",
      //"l\\u0331ra",
        "yra",
        "\\u1E8Fra",
      //"l-",
        "vra",
        "\\u015Bra",
        "\\u1E63ra",
        "sra",
        "hma",
        "\\u1E6D\\u1E6Da",
        "\\u1E6D\\u1E6Dha",
        "\\u1E6Dh\\u1E6Dha",
        "\\u1E0D\\u1E0Da",
        "\\u1E0D\\u1E0Dha",
        "\\u1E6Dya",
        "\\u1E6Dhya",
        "\\u1E0Dya",
        "\\u1E0Dhya",
        // Not roundtrippable -- 
        // \\u0939\\u094d\\u094d\\u092E  - hma
        // \\u0939\\u094d\\u092E         - hma
        // CharsToUnicodeString("hma"),
        "hya",
        "\\u015Br\\u0325a",
        "\\u015Bca",
        "\\u0115",
        "san\\u0304j\\u012Bb s\\u0113nagupta",
        "\\u0101nand vaddir\\u0101ju",    
        "\\u0101",
        "a"
    };
    const char* const expected[MAX_LEN] = {
        "\\u092D\\u093E\\u0930\\u0924",   /* bha\\u0304rata */
        "\\u0915\\u094D\\u0930",          /* kra         */
        "\\u0915\\u094D\\u0937",          /* ks\\u0323a  */
        "\\u0916\\u094D\\u0930",          /* khra        */
        "\\u0917\\u094D\\u0930",          /* gra         */
        "\\u0919\\u094D\\u0930",          /* n\\u0307ra  */
        "\\u091A\\u094D\\u0930",          /* cra         */
        "\\u091B\\u094D\\u0930",          /* chra        */
        "\\u091C\\u094D\\u091E",          /* jn\\u0303a  */
        "\\u091D\\u094D\\u0930",          /* jhra        */
        "\\u091E\\u094D\\u0930",          /* n\\u0303ra  */
        "\\u091F\\u094D\\u092F",          /* t\\u0323ya  */
        "\\u0920\\u094D\\u0930",          /* t\\u0323hra */
        "\\u0921\\u094D\\u092F",          /* d\\u0323ya  */
      //"\\u095C\\u094D\\u092F",        /* r\\u0323ya  */ // \u095c is not valid in Devanagari
        "\\u0922\\u094D\\u092F",          /* d\\u0323hya */
        "\\u0922\\u093C\\u094D\\u0930",   /* r\\u0323hra */
        "\\u0923\\u094D\\u0930",          /* n\\u0323ra  */
        "\\u0924\\u094D\\u0924",          /* tta         */
        "\\u0925\\u094D\\u0930",          /* thra        */
        "\\u0926\\u094D\\u0926",          /* dda         */
        "\\u0927\\u094D\\u0930",          /* dhra        */
        "\\u0928\\u094D\\u0928",          /* nna         */
        "\\u092A\\u094D\\u0930",          /* pra         */
        "\\u092B\\u094D\\u0930",          /* phra        */
        "\\u092C\\u094D\\u0930",          /* bra         */
        "\\u092D\\u094D\\u0930",          /* bhra        */
        "\\u092E\\u094D\\u0930",          /* mra         */
        "\\u0929\\u094D\\u0930",          /* n\\u0331ra  */
      //"\\u0934\\u094D\\u0930",        /* l\\u0331ra  */
        "\\u092F\\u094D\\u0930",          /* yra         */
        "\\u092F\\u093C\\u094D\\u0930",   /* y\\u0307ra  */
      //"l-",
        "\\u0935\\u094D\\u0930",          /* vra         */
        "\\u0936\\u094D\\u0930",          /* s\\u0301ra  */
        "\\u0937\\u094D\\u0930",          /* s\\u0323ra  */
        "\\u0938\\u094D\\u0930",          /* sra         */
        "\\u0939\\u094d\\u092E",          /* hma         */
        "\\u091F\\u094D\\u091F",          /* t\\u0323t\\u0323a  */
        "\\u091F\\u094D\\u0920",          /* t\\u0323t\\u0323ha */
        "\\u0920\\u094D\\u0920",          /* t\\u0323ht\\u0323ha*/
        "\\u0921\\u094D\\u0921",          /* d\\u0323d\\u0323a  */
        "\\u0921\\u094D\\u0922",          /* d\\u0323d\\u0323ha */
        "\\u091F\\u094D\\u092F",          /* t\\u0323ya  */
        "\\u0920\\u094D\\u092F",          /* t\\u0323hya */
        "\\u0921\\u094D\\u092F",          /* d\\u0323ya  */
        "\\u0922\\u094D\\u092F",          /* d\\u0323hya */
     // "hma",                         /* hma         */
        "\\u0939\\u094D\\u092F",          /* hya         */
        "\\u0936\\u0943\\u0905",          /* s\\u0301r\\u0325a  */
        "\\u0936\\u094D\\u091A",          /* s\\u0301ca  */
        "\\u090d",                        /* e\\u0306    */
        "\\u0938\\u0902\\u091C\\u0940\\u092C\\u094D \\u0938\\u0947\\u0928\\u0917\\u0941\\u092A\\u094D\\u0924",
        "\\u0906\\u0928\\u0902\\u0926\\u094D \\u0935\\u0926\\u094D\\u0926\\u093F\\u0930\\u093E\\u091C\\u0941",    
        "\\u0906",
        "\\u0905",
    };
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError;
    UnicodeString message;
    Transliterator* latinToDev=Transliterator::createInstance("Latin-Devanagari", UTRANS_FORWARD, parseError, status);
    Transliterator* devToLatin=Transliterator::createInstance("Devanagari-Latin", UTRANS_FORWARD, parseError, status);
    if(U_FAILURE(status)){
        errln("FAIL: construction " +   UnicodeString(" Error: ") + u_errorName(status));
        errln("PreContext: " + prettify(parseError.preContext) + " PostContext: " + prettify( parseError.postContext) );
        return;
    }
    UnicodeString gotResult;
    for(int i= 0; i<MAX_LEN; i++){
        gotResult = source[i];
        expect(*latinToDev,CharsToUnicodeString(source[i]),CharsToUnicodeString(expected[i]));
        expect(*devToLatin,CharsToUnicodeString(expected[i]),CharsToUnicodeString(source[i]));
    }
        
}
void IndicLatinTest::TestTeluguLatinRT(){
    const int MAX_LEN=10;
    const char* const source[MAX_LEN] = {   
        "raghur\\u0101m vi\\u015Bvan\\u0101dha",                         /* Raghuram Viswanadha    */
        "\\u0101nand vaddir\\u0101ju",                                   /* Anand Vaddiraju 	   */ 	   
        "r\\u0101j\\u012Bv ka\\u015Barab\\u0101da",                      /* Rajeev Kasarabada      */ 
        "san\\u0304j\\u012Bv ka\\u015Barab\\u0101da",                    /* sanjeev kasarabada     */
        "san\\u0304j\\u012Bb sen'gupta",                                 /* sanjib sengupata 	   */ 	   
        "amar\\u0113ndra hanum\\u0101nula",                              /* Amarendra hanumanula   */ 
        "ravi kum\\u0101r vi\\u015Bvan\\u0101dha",                       /* Ravi Kumar Viswanadha  */
        "\\u0101ditya kandr\\u0113gula",                                 /* Aditya Kandregula      */
        "\\u015Br\\u012Bdhar ka\\u1E47\\u1E6Dama\\u015Be\\u1E6D\\u1E6Di",/* Shridhar Kantamsetty   */
        "m\\u0101dhav de\\u015Be\\u1E6D\\u1E6Di"                         /* Madhav Desetty         */
    };

    const char* const expected[MAX_LEN] = {
        "\\u0c30\\u0c18\\u0c41\\u0c30\\u0c3e\\u0c2e\\u0c4d \\u0c35\\u0c3f\\u0c36\\u0c4d\\u0c35\\u0c28\\u0c3e\\u0c27",     
        "\\u0c06\\u0c28\\u0c02\\u0c26\\u0c4d \\u0C35\\u0C26\\u0C4D\\u0C26\\u0C3F\\u0C30\\u0C3E\\u0C1C\\u0C41",     
        "\\u0c30\\u0c3e\\u0c1c\\u0c40\\u0c35\\u0c4d \\u0c15\\u0c36\\u0c30\\u0c2c\\u0c3e\\u0c26",
        "\\u0c38\\u0c02\\u0c1c\\u0c40\\u0c35\\u0c4d \\u0c15\\u0c36\\u0c30\\u0c2c\\u0c3e\\u0c26",
        "\\u0c38\\u0c02\\u0c1c\\u0c40\\u0c2c\\u0c4d \\u0c38\\u0c46\\u0c28\\u0c4d\\u0c17\\u0c41\\u0c2a\\u0c4d\\u0c24",
        "\\u0c05\\u0c2e\\u0c30\\u0c47\\u0c02\\u0c26\\u0c4d\\u0c30 \\u0c39\\u0c28\\u0c41\\u0c2e\\u0c3e\\u0c28\\u0c41\\u0c32",
        "\\u0c30\\u0c35\\u0c3f \\u0c15\\u0c41\\u0c2e\\u0c3e\\u0c30\\u0c4d \\u0c35\\u0c3f\\u0c36\\u0c4d\\u0c35\\u0c28\\u0c3e\\u0c27",
        "\\u0c06\\u0c26\\u0c3f\\u0c24\\u0c4d\\u0c2f \\u0C15\\u0C02\\u0C26\\u0C4D\\u0C30\\u0C47\\u0C17\\u0C41\\u0c32",
        "\\u0c36\\u0c4d\\u0c30\\u0c40\\u0C27\\u0C30\\u0C4D \\u0c15\\u0c02\\u0c1f\\u0c2e\\u0c36\\u0c46\\u0c1f\\u0c4d\\u0c1f\\u0c3f",
        "\\u0c2e\\u0c3e\\u0c27\\u0c35\\u0c4d \\u0c26\\u0c46\\u0c36\\u0c46\\u0c1f\\u0c4d\\u0c1f\\u0c3f",
    };

    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError;
    UnicodeString message;
    Transliterator* latinToDev=Transliterator::createInstance("Latin-Telugu", UTRANS_FORWARD, parseError, status);
    Transliterator* devToLatin=Transliterator::createInstance("Telugu-Latin", UTRANS_FORWARD, parseError, status);
    if(U_FAILURE(status)){
        errln("FAIL: construction " +   UnicodeString(" Error: ") + u_errorName(status));
        errln("PreContext: " + prettify(parseError.preContext) + " PostContext: " + prettify( parseError.postContext) );
        return;
    }
    UnicodeString gotResult;
    for(int i= 0; i<MAX_LEN; i++){
        gotResult = source[i];
        expect(*latinToDev,CharsToUnicodeString(source[i]),CharsToUnicodeString(expected[i]));
        expect(*devToLatin,CharsToUnicodeString(expected[i]),CharsToUnicodeString(source[i]));
    }
}

void IndicLatinTest::TestSanskritLatinRT(){
    const int MAX_LEN =15;
    const char* const source[MAX_LEN] = {
        "\\u015Br\\u012Bmad",
        "bhagavadg\\u012Bt\\u0101",
        "adhy\\u0101ya",
        "arjuna",
        "vi\\u1E63\\u0101da",
        "y\\u014Dga",
        "dhr\\u0325tar\\u0101\\u1E63\\u1E6Dra",
        "uv\\u0101cr\\u0325",
        "dharmak\\u1E63\\u0113tr\\u0113",
        "kuruk\\u1E63\\u0113tr\\u0113",
        "samav\\u0113t\\u0101",
        "yuyutsava-\\u1E25",
        "m\\u0101mak\\u0101-\\u1E25",
     // "p\\u0101\\u1E47\\u1E0Dav\\u0101\\u015Bcaiva",
        "kimakurvata",
        "san\\u0304java",
    };
    const char* const expected[MAX_LEN] = {
        "\\u0936\\u094d\\u0930\\u0940\\u092e\\u0926\\u094d",
        "\\u092d\\u0917\\u0935\\u0926\\u094d\\u0917\\u0940\\u0924\\u093e",
        "\\u0905\\u0927\\u094d\\u092f\\u093e\\u092f",
        "\\u0905\\u0930\\u094d\\u091c\\u0941\\u0928",
        "\\u0935\\u093f\\u0937\\u093e\\u0926",
        "\\u092f\\u094b\\u0917",
        "\\u0927\\u0943\\u0924\\u0930\\u093e\\u0937\\u094d\\u091f\\u094d\\u0930",
        "\\u0909\\u0935\\u093E\\u091A\\u0943",
        "\\u0927\\u0930\\u094d\\u092e\\u0915\\u094d\\u0937\\u0947\\u0924\\u094d\\u0930\\u0947",
        "\\u0915\\u0941\\u0930\\u0941\\u0915\\u094d\\u0937\\u0947\\u0924\\u094d\\u0930\\u0947",
        "\\u0938\\u092e\\u0935\\u0947\\u0924\\u093e",
        "\\u092f\\u0941\\u092f\\u0941\\u0924\\u094d\\u0938\\u0935\\u0903",
        "\\u092e\\u093e\\u092e\\u0915\\u093e\\u0903",
      //"\\u092a\\u093e\\u0923\\u094d\\u0921\\u0935\\u093e\\u0936\\u094d\\u091a\\u0948\\u0935",
        "\\u0915\\u093f\\u092e\\u0915\\u0941\\u0930\\u094d\\u0935\\u0924",
        "\\u0938\\u0902\\u091c\\u0935",
    };
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError;
    UnicodeString message;
    Transliterator* latinToDev=Transliterator::createInstance("Latin-Devanagari", UTRANS_FORWARD, parseError, status);
    Transliterator* devToLatin=Transliterator::createInstance("Devanagari-Latin", UTRANS_FORWARD, parseError, status);
    if(U_FAILURE(status)){
        errln("FAIL: construction " +   UnicodeString(" Error: ") + u_errorName(status));
        errln("PreContext: " + prettify(parseError.preContext) + " PostContext: " + prettify( parseError.postContext) );
        return;
    }
    UnicodeString gotResult;
    for(int i= 0; i<MAX_LEN; i++){
        gotResult = source[i];
        expect(*latinToDev,CharsToUnicodeString(source[i]),CharsToUnicodeString(expected[i]));
        expect(*devToLatin,CharsToUnicodeString(expected[i]),CharsToUnicodeString(source[i]));
    }
}


void IndicLatinTest::TestCompoundLatinRT(){
#if ((U_ICU_VERSION_MAJOR_NUM != 1) || (U_ICU_VERSION_MINOR_NUM !=9))

    const int MAX_LEN =15;
    const char* const source[MAX_LEN] = {
        "\\u015Br\\u012Bmad",
        "bhagavadg\\u012Bt\\u0101",
        "adhy\\u0101ya",
        "arjuna",
        "vi\\u1E63\\u0101da",
        "y\\u014Dga",
        "dhr\\u0325tar\\u0101\\u1E63\\u1E6Dra",
        "uv\\u0101cr\\u0325",
        "dharmak\\u1E63\\u0113tr\\u0113",
        "kuruk\\u1E63\\u0113tr\\u0113",
        "samav\\u0113t\\u0101",
        "yuyutsava-\\u1E25",
        "m\\u0101mak\\u0101-\\u1E25",
     // "p\\u0101\\u1E47\\u1E0Dav\\u0101\\u015Bcaiva",
        "kimakurvata",
        "san\\u0304java"
    };
    const char* const expected[MAX_LEN] = {
        "\\u0936\\u094d\\u0930\\u0940\\u092e\\u0926\\u094d",
        "\\u092d\\u0917\\u0935\\u0926\\u094d\\u0917\\u0940\\u0924\\u093e",
        "\\u0905\\u0927\\u094d\\u092f\\u093e\\u092f",
        "\\u0905\\u0930\\u094d\\u091c\\u0941\\u0928",
        "\\u0935\\u093f\\u0937\\u093e\\u0926",
        "\\u092f\\u094b\\u0917",
        "\\u0927\\u0943\\u0924\\u0930\\u093e\\u0937\\u094d\\u091f\\u094d\\u0930",
        "\\u0909\\u0935\\u093E\\u091A\\u0943",
        "\\u0927\\u0930\\u094d\\u092e\\u0915\\u094d\\u0937\\u0947\\u0924\\u094d\\u0930\\u0947",
        "\\u0915\\u0941\\u0930\\u0941\\u0915\\u094d\\u0937\\u0947\\u0924\\u094d\\u0930\\u0947",
        "\\u0938\\u092e\\u0935\\u0947\\u0924\\u093e",
        "\\u092f\\u0941\\u092f\\u0941\\u0924\\u094d\\u0938\\u0935\\u0903",
        "\\u092e\\u093e\\u092e\\u0915\\u093e\\u0903",
    //  "\\u092a\\u093e\\u0923\\u094d\\u0921\\u0935\\u093e\\u0936\\u094d\\u091a\\u0948\\u0935",
        "\\u0915\\u093f\\u092e\\u0915\\u0941\\u0930\\u094d\\u0935\\u0924",
        "\\u0938\\u0902\\u091c\\u0935"
    };
    UErrorCode status = U_ZERO_ERROR;
    UParseError parseError;
    UnicodeString message;
    Transliterator* latinToDevToLatin=Transliterator::createInstance("Latin-Devanagari;Devanagari-Latin", UTRANS_FORWARD, parseError, status);
    Transliterator* devToLatinToDev=Transliterator::createInstance("Devanagari-Latin;Latin-Devanagari", UTRANS_FORWARD, parseError, status);
    if(U_FAILURE(status)){
        errln("FAIL: construction " +   UnicodeString(" Error: ") + u_errorName(status));
        errln("PreContext: " + prettify(parseError.preContext) + " PostContext: " + prettify( parseError.postContext) );
        return;
    }
    UnicodeString gotResult;
    for(int i= 0; i<MAX_LEN; i++){
        gotResult = source[i];
        expect(*latinToDevToLatin,CharsToUnicodeString(source[i]),CharsToUnicodeString(source[i]));
        expect(*devToLatinToDev,CharsToUnicodeString(expected[i]),CharsToUnicodeString(expected[i]));
    }
#endif
}


