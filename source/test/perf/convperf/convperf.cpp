/*
**********************************************************************
* Copyright (c) 2002-2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
**********************************************************************
*/
/** 
 * This Program tests the performance of ICU's Normalization engine against Windows
 * to run it use the command like
 *
 * c:\normperf.exe -s C:\work\ICUCupertinoRep\icu4c\collation-perf-data  -i 10 -p 15 -f TestNames_Asian.txt -u -e UTF-8  -l
 */
#include "convperf.h"
#include "data.h"

int main(int argc, const char* argv[]){
    UErrorCode status = U_ZERO_ERROR;
    ConverterPerformanceTest test(argc, argv, status);
    if(U_FAILURE(status)){
        return status;
    }
    if(test.run()==FALSE){
        fprintf(stderr,"FAILED: Tests could not be run please check the arguments.\n");
        return -1;
    }
    return 0;
}


ConverterPerformanceTest::ConverterPerformanceTest(int32_t argc, const char* argv[], UErrorCode& status)
: UPerfTest(argc,argv,status){
    
}

ConverterPerformanceTest::~ConverterPerformanceTest(){

}

UPerfFunction* ConverterPerformanceTest::runIndexedTest(int32_t index, UBool exec,const char* &name, char* par) {
    switch (index) {
        TESTCASE(0,TestICU_UTF8_ToUnicode);
        TESTCASE(1,TestICU_UTF8_FromUnicode);
        TESTCASE(2,TestWinANSI_UTF8_ToUnicode);
        TESTCASE(3,TestWinANSI_UTF8_FromUnicode);
        TESTCASE(4,TestWinIML2_UTF8_ToUnicode);
        TESTCASE(5,TestWinIML2_UTF8_FromUnicode);
        
        TESTCASE(6,TestICU_Latin1_ToUnicode);
        TESTCASE(7,TestICU_Latin1_FromUnicode);
        TESTCASE(8,TestWinIML2_Latin1_ToUnicode);
        TESTCASE(9,TestWinIML2_Latin1_FromUnicode);
        
        TESTCASE(10,TestICU_Latin8_ToUnicode);
        TESTCASE(11,TestICU_Latin8_FromUnicode);
        TESTCASE(12,TestWinIML2_Latin8_ToUnicode);
        TESTCASE(13,TestWinIML2_Latin8_FromUnicode);

        TESTCASE(14,TestICU_EBCDIC_Arabic_ToUnicode);
        TESTCASE(15,TestICU_EBCDIC_Arabic_FromUnicode);
        TESTCASE(16,TestWinIML2_EBCDIC_Arabic_ToUnicode);
        TESTCASE(17,TestWinIML2_EBCDIC_Arabic_FromUnicode);

        TESTCASE(18,TestICU_SJIS_ToUnicode);
        TESTCASE(19,TestICU_SJIS_FromUnicode);
        TESTCASE(20,TestWinIML2_SJIS_ToUnicode);
        TESTCASE(21,TestWinIML2_SJIS_FromUnicode);

        TESTCASE(22,TestICU_EUCJP_ToUnicode);
        TESTCASE(23,TestICU_EUCJP_FromUnicode);
        TESTCASE(24,TestWinIML2_EUCJP_ToUnicode);
        TESTCASE(25,TestWinIML2_EUCJP_FromUnicode);

        TESTCASE(26,TestICU_GB2312_FromUnicode);
        TESTCASE(27,TestICU_GB2312_ToUnicode);
        TESTCASE(28,TestWinIML2_GB2312_ToUnicode);
        TESTCASE(29,TestWinIML2_GB2312_FromUnicode);

        TESTCASE(30,TestICU_ISO2022KR_ToUnicode);
        TESTCASE(31,TestICU_ISO2022KR_FromUnicode);
        TESTCASE(32,TestWinIML2_ISO2022KR_ToUnicode);
        TESTCASE(33,TestWinIML2_ISO2022KR_FromUnicode);

        TESTCASE(34,TestICU_ISO2022JP_ToUnicode);
        TESTCASE(35,TestICU_ISO2022JP_FromUnicode);
        TESTCASE(36,TestWinIML2_ISO2022JP_ToUnicode);
        TESTCASE(37,TestWinIML2_ISO2022JP_FromUnicode);

        TESTCASE(38,TestWinANSI_Latin1_ToUnicode);
        TESTCASE(39,TestWinANSI_Latin1_FromUnicode);        
        
        TESTCASE(40,TestWinANSI_Latin8_ToUnicode);
        TESTCASE(41,TestWinANSI_Latin8_FromUnicode);
        
        TESTCASE(42,TestWinANSI_SJIS_ToUnicode);
        TESTCASE(43,TestWinANSI_SJIS_FromUnicode);
        
        TESTCASE(44,TestWinANSI_EUCJP_ToUnicode);
        TESTCASE(45,TestWinANSI_EUCJP_FromUnicode);
        
        TESTCASE(46,TestWinANSI_GB2312_ToUnicode);
        TESTCASE(47,TestWinANSI_GB2312_FromUnicode);
        
        TESTCASE(48,TestWinANSI_ISO2022KR_ToUnicode);
        TESTCASE(49,TestWinANSI_ISO2022KR_FromUnicode);        
        
        TESTCASE(50,TestWinANSI_ISO2022JP_ToUnicode);
        TESTCASE(51,TestWinANSI_ISO2022JP_FromUnicode);

        default: 
            name = ""; 
            return NULL;
    }
    return NULL;

}

UPerfFunction* ConverterPerformanceTest::TestICU_UTF8_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("utf-8",utf8_uniSource, LENGTHOF(utf8_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_UTF8_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("utf-8",(char*)utf8_encSource, LENGTHOF(utf8_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_UTF8_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("utf-8",utf8_uniSource, LENGTHOF(utf8_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_UTF8_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("utf-8",(char*)utf8_encSource, LENGTHOF(utf8_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction* ConverterPerformanceTest::TestWinANSI_UTF8_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("utf-8",utf8_uniSource, LENGTHOF(utf8_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_UTF8_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("utf-8",(char*)utf8_encSource, LENGTHOF(utf8_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

//################

UPerfFunction* ConverterPerformanceTest::TestICU_Latin1_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("iso-8859-1",latin1_uniSource, LENGTHOF(latin1_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_Latin1_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("iso-8859-1",(char*)latin1_encSource, LENGTHOF(latin1_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_Latin1_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("iso-8859-1",latin1_uniSource, LENGTHOF(latin1_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_Latin1_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("iso-8859-1",(char*)latin1_encSource, LENGTHOF(latin1_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction* ConverterPerformanceTest::TestWinANSI_Latin1_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("iso-8859-1",latin1_uniSource, LENGTHOF(latin1_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_Latin1_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("iso-8859-1",(char*)latin1_encSource, LENGTHOF(latin1_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

//##################

UPerfFunction* ConverterPerformanceTest::TestICU_Latin8_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("iso-8859-8",latin8_uniSource, LENGTHOF(latin8_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_Latin8_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("iso-8859-8",(char*)latin8_encSource, LENGTHOF(latin8_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_Latin8_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("iso-8859-8",latin8_uniSource, LENGTHOF(latin8_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_Latin8_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("iso-8859-8",(char*)latin8_encSource, LENGTHOF(latin8_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}
UPerfFunction* ConverterPerformanceTest::TestWinANSI_Latin8_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("iso-8859-8",latin8_uniSource, LENGTHOF(latin8_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_Latin8_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("iso-8859-8",(char*)latin8_encSource, LENGTHOF(latin8_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

//#################


UPerfFunction* ConverterPerformanceTest::TestICU_EBCDIC_Arabic_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("x-EBCDIC-Arabic",ebcdic_arabic_uniSource, LENGTHOF(ebcdic_arabic_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_EBCDIC_Arabic_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("x-EBCDIC-Arabic",(char*)ebcdic_arabic_encSource, LENGTHOF(ebcdic_arabic_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_EBCDIC_Arabic_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("x-EBCDIC-Arabic",ebcdic_arabic_uniSource, LENGTHOF(ebcdic_arabic_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_EBCDIC_Arabic_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("x-EBCDIC-Arabic",(char*)ebcdic_arabic_encSource, LENGTHOF(ebcdic_arabic_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction* ConverterPerformanceTest::TestWinANSI_EBCDIC_Arabic_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("x-EBCDIC-Arabic",ebcdic_arabic_uniSource, LENGTHOF(ebcdic_arabic_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_EBCDIC_Arabic_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("x-EBCDIC-Arabic",(char*)ebcdic_arabic_encSource, LENGTHOF(ebcdic_arabic_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}
//#################


UPerfFunction* ConverterPerformanceTest::TestICU_SJIS_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("sjis",sjis_uniSource, LENGTHOF(sjis_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_SJIS_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("sjis",(char*)sjis_encSource, LENGTHOF(sjis_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_SJIS_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("sjis",sjis_uniSource, LENGTHOF(sjis_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_SJIS_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("sjis",(char*)sjis_encSource, LENGTHOF(sjis_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction* ConverterPerformanceTest::TestWinANSI_SJIS_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("sjis",sjis_uniSource, LENGTHOF(sjis_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_SJIS_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("sjis",(char*)sjis_encSource, LENGTHOF(sjis_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


//#################


UPerfFunction* ConverterPerformanceTest::TestICU_EUCJP_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("euc-jp",eucjp_uniSource, LENGTHOF(eucjp_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_EUCJP_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("euc-jp",(char*)eucjp_encSource, LENGTHOF(eucjp_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_EUCJP_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("euc-jp",eucjp_uniSource, LENGTHOF(eucjp_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_EUCJP_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("euc-jp",(char*)eucjp_encSource, LENGTHOF(eucjp_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction* ConverterPerformanceTest::TestWinANSI_EUCJP_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("euc-jp",eucjp_uniSource, LENGTHOF(eucjp_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_EUCJP_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("euc-jp",(char*)eucjp_encSource, LENGTHOF(eucjp_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


//#################


UPerfFunction* ConverterPerformanceTest::TestICU_GB2312_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("gb2312",gb2312_uniSource, LENGTHOF(gb2312_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_GB2312_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("gb2312",(char*)gb2312_encSource, LENGTHOF(gb2312_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_GB2312_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("gb2312",gb2312_uniSource, LENGTHOF(gb2312_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_GB2312_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("gb2312",(char*)gb2312_encSource, LENGTHOF(gb2312_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction* ConverterPerformanceTest::TestWinANSI_GB2312_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("gb2312",gb2312_uniSource, LENGTHOF(gb2312_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_GB2312_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("gb2312",(char*)gb2312_encSource, LENGTHOF(gb2312_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

//#################


UPerfFunction* ConverterPerformanceTest::TestICU_ISO2022KR_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("iso-2022-kr",iso2022kr_uniSource, LENGTHOF(iso2022kr_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_ISO2022KR_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("iso-2022-kr",(char*)iso2022kr_encSource, LENGTHOF(iso2022kr_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_ISO2022KR_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("iso-2022-kr",iso2022kr_uniSource, LENGTHOF(iso2022kr_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_ISO2022KR_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("iso-2022-kr",(char*)iso2022kr_encSource, LENGTHOF(iso2022kr_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction* ConverterPerformanceTest::TestWinANSI_ISO2022KR_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("iso-2022-kr",iso2022kr_uniSource, LENGTHOF(iso2022kr_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_ISO2022KR_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("iso-2022-kr",(char*)iso2022kr_encSource, LENGTHOF(iso2022kr_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}
//#################


UPerfFunction* ConverterPerformanceTest::TestICU_ISO2022JP_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    ICUFromUnicodePerfFunction* pf = new ICUFromUnicodePerfFunction("iso-2022-jp",iso2022jp_uniSource, LENGTHOF(iso2022jp_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestICU_ISO2022JP_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new ICUToUnicodePerfFunction("iso-2022-jp",(char*)iso2022jp_encSource, LENGTHOF(iso2022jp_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}


UPerfFunction* ConverterPerformanceTest::TestWinIML2_ISO2022JP_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2FromUnicodePerfFunction("iso-2022-jp",iso2022jp_uniSource, LENGTHOF(iso2022jp_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinIML2_ISO2022JP_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinIMultiLanguage2ToUnicodePerfFunction("iso-2022-jp",(char*)iso2022jp_encSource, LENGTHOF(iso2022jp_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction* ConverterPerformanceTest::TestWinANSI_ISO2022JP_FromUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIFromUnicodePerfFunction("iso-2022-jp",iso2022jp_uniSource, LENGTHOF(iso2022jp_uniSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}

UPerfFunction*  ConverterPerformanceTest::TestWinANSI_ISO2022JP_ToUnicode(){
    UErrorCode status = U_ZERO_ERROR;
    UPerfFunction* pf = new WinANSIToUnicodePerfFunction("iso-2022-jp",(char*)iso2022jp_encSource, LENGTHOF(iso2022jp_encSource), status);
    if(U_FAILURE(status)){
        return NULL;
    }
    return pf;
}