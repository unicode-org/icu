/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CU_CAPITST.C
*
* Modification History:
*        Name                      Description            
*     Madhu Katragadda              Ported for C API
*********************************************************************************
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "ccapitst.h"
#include "unicode/uloc.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "cintltst.h"
#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "cmemory.h"

#define NUM_CODEPAGE 1
#define MAX_FILE_LEN 1024*20
#define UCS_FILE_NAME_SIZE 100

/*writes and entire UChar* (string) along with a BOM to a file*/
void WriteToFile(const UChar *a, FILE *myfile); 
/*Case insensitive compare*/
int32_t strCaseIcmp(const char* a1,const char * a2); 
/*returns an action other than the one provided*/
UConverterFromUCallback otherUnicodeAction(UConverterFromUCallback MIA);
UConverterToUCallback otherCharAction(UConverterToUCallback MIA);


void addTestConvert(TestNode** root)
{
    addTest(root, &TestConvert, "tsconv/ccapitst/TestConvert");

}

void TestConvert() 
{
    char                myptr[4];
    char                save[4];
    char                subchar [4]         =   {(char)0xBE, (char)0xEF};
    int32_t             testLong1           =   0;
    int16_t             rest                =   0;
    FILE*               f                   =   NULL;
    FILE*               f2                  =   NULL;
    int32_t             uniLen              =   0;
    int32_t             len                 =   0;
    int32_t             x                   =   0;
    FILE*               ucs_file_in         =   NULL;
    UChar                BOM                 =   0x0000;
    UChar                myUChar           =   0x0000;
    char                myChar              =   0x00;
    char                mytarget[MAX_FILE_LEN];
    char*               mytarget_1=mytarget;
    char*               mytarget_use        = mytarget;
    UChar*                consumedUni         =   NULL;
    char*               consumedChar        =   NULL;
    char*               consumed            =   NULL;
    char                output_cp_buffer    [MAX_FILE_LEN];
    UChar                ucs_file_buffer     [MAX_FILE_LEN];
    UChar*                ucs_file_buffer_use = ucs_file_buffer;
    UChar                my_ucs_file_buffer  [MAX_FILE_LEN];
    UChar*                my_ucs_file_buffer_1=my_ucs_file_buffer;
    int32_t             i                   =   0;
    int8_t                ii                  =   0;
    uint16_t            ij                  =   0;
    int32_t             j                   =   0;
    int32_t             k                   =   0;
    uint16_t            codepage_index      =   0;
    uint16_t            count;
    int32_t             cp                  =   0;
    UErrorCode          err                 =   U_ZERO_ERROR;
    const char*            available_conv;  
    char                ucs_file_name[UCS_FILE_NAME_SIZE];
    UConverterFromUCallback          MIA1;
    UConverterToUCallback              MIA2;
    UChar                myUnitarget[MAX_FILE_LEN];
    UChar                *myUnitarget_1 = myUnitarget;
    UConverter*            someConverters[5];
    UConverter*         myConverter = 0;
    UChar*                displayname = 0;
   
    const char* locale;

    UChar* uchar1 = 0;
    UChar* uchar2 = 0;
    UChar* uchar3 = 0;
    int32_t targetcapacity2;
    int32_t targetcapacity;
    int32_t targetsize;
    int32_t disnamelen;

    const UChar* tmp_ucs_buf;
    const UChar* tmp_consumedUni=NULL;
    const char* tmp_mytarget_use;
    const char* tmp_consumed; 
    
    /******************************************************************
                                Checking Unicode -> ksc
     ******************************************************************/

    const char*      CodePagesToTest[NUM_CODEPAGE]       =
    {
       "IBM-949"

        
    }; 
    const uint16_t CodePageNumberToTest[NUM_CODEPAGE]             =
    {
        949
    };
    

    const int32_t        CodePagesAsciiControls[NUM_CODEPAGE]    =
    { 
        0xFFFFFFFF
            
    
    };

    const int32_t        CodePagesOtherControls[NUM_CODEPAGE]    =
    {
         0x00000005
    };


    const int8_t     CodePagesMinChars[NUM_CODEPAGE] =
    { 
        1
    
    };

    const int8_t     CodePagesMaxChars[NUM_CODEPAGE] =
    { 
        2
    
    };

    const int16_t        CodePagesSubstitutionChars[NUM_CODEPAGE]    =
    { 
        (int16_t)0xAFFE
    
    };

    const char* CodePagesTestFiles[NUM_CODEPAGE]    =
    { 
      "uni-text.txt"
    };

    
    const UConverterPlatform        CodePagesPlatform[NUM_CODEPAGE]    =
    { 
        UCNV_IBM
    
    };

    const UConverterToUCallback CodePagesMissingCharAction[NUM_CODEPAGE] =
    {
      UCNV_TO_U_CALLBACK_SUBSTITUTE
    };
    
    const UConverterFromUCallback CodePagesMissingUnicodeAction[NUM_CODEPAGE] =
    {
      UCNV_FROM_U_CALLBACK_SUBSTITUTE
    };

    const char* CodePagesLocale[NUM_CODEPAGE] =
    {
        "ko_KR"
    };

    UChar CodePagesFlakySequence[NUM_CODEPAGE][20] =
    {
        {(UChar)0xAC10,(UChar)0xAC11, (UChar)0xAC12, (UChar)0xAC13 , (UChar)0xAC14, (UChar)0xAC15, (UChar)0xAC16, (UChar)0xAC17, (UChar)0xd7a4 /*Offensive Codepoint*/, (UChar)0xAC14, (UChar)0xAC15}
    };
    
    char CodePagesFlakyCharSequence[NUM_CODEPAGE][20] =
    {
        {   (char)0xB0, (char)0xA8,
            (char)0xB0, (char)0xA9,
            (char)0xB0, (char)0xAA,
            (char)0xB0, (char)0xAB,
            (char)0xb0, (char)0xff,/*Offensive Codepoint*/
            (char)0xB0, (char)0xAC,
            (char)0xB0, (char)0xAD
        }
    };

    UConverterFromUCallback oldFromUAction = NULL;
    UConverterToUCallback oldToUAction = NULL;
    void* oldFromUContext = NULL;
    void* oldToUContext = NULL;

    /* flush the converter cache to get a consistent state before the flushing is tested */
    ucnv_flushCache();

    /*Calling all the UnicodeConverterCPP API and checking functionality*/
  
        /*Tests ucnv_getAvailableName(), getAvialableCount()*/
        
    log_verbose("Testing ucnv_countAvailable()...");
    
    testLong1=ucnv_countAvailable();
    log_verbose("Number of available Codepages:    %d\n", testLong1);
    
    log_verbose("\n---Testing ucnv_getAvailableName..");  /*need to check this out */
    
    available_conv = ucnv_getAvailableName(testLong1);
       /*test ucnv_getAvailableName with err condition*/
    log_verbose("\n---Testing ucnv_getAvailableName..with index < 0 ");
    available_conv = ucnv_getAvailableName(-1);
    if(available_conv != NULL){
        log_err("ucnv_getAvailableName() with index < 0) should return NULL\n");
    }
    
    /* Test ucnv_countAliases() etc. */
    count = ucnv_countAliases("utf-8", &err);
    if(U_FAILURE(err)) {
        log_err("FAILURE! ucnv_countAliases(\"utf-8\") -> %s\n", myErrorName(err));
    } else if(count <= 0) {
        log_err("FAILURE! ucnv_countAliases(\"utf-8\") -> %d aliases\n", count);
    } else {
        /* try to get the aliases individually */
        const char *alias;
        alias = ucnv_getAlias("utf-8", 0, &err);
        if(U_FAILURE(err)) {
            log_err("FAILURE! ucnv_getAlias(\"utf-8\", 0) -> %s\n", myErrorName(err));
        } else if(uprv_strcmp("UTF8", alias) != 0) {
            log_err("FAILURE! ucnv_getAlias(\"utf-8\", 0) -> %s instead of UTF8\n", alias);
        } else {
            uint16_t i;
            for(i = 0; i < count; ++i) {
                alias = ucnv_getAlias("utf-8", i, &err);
                if(U_FAILURE(err)) {
                    log_err("FAILURE! ucnv_getAlias(\"utf-8\", %d) -> %s\n", i, myErrorName(err));
                } else if(uprv_strlen(alias) > 20) {
                    /* sanity check */
                    log_err("FAILURE! ucnv_getAlias(\"utf-8\", %d) -> alias %s insanely long, corrupt?!\n", i, alias);
                } else {
                    log_verbose("alias %d for utf-8: %s\n", i, alias);
                }
            }
            if(U_SUCCESS(err)) {
                /* try to fill an array with all aliases */
                const char **aliases;
                aliases=(const char **)uprv_malloc(count * sizeof(const char *));
                if(aliases != 0) {
                    ucnv_getAliases("utf-8", aliases, &err);
                    if(U_FAILURE(err)) {
                        log_err("FAILURE! ucnv_getAliases(\"utf-8\") -> %s\n", myErrorName(err));
                    } else {
                        for(i = 0; i < count; ++i) {
                            /* compare the pointers with the ones returned individually */
                            alias = ucnv_getAlias("utf-8", i, &err);
                            if(U_FAILURE(err)) {
                                log_err("FAILURE! ucnv_getAlias(\"utf-8\", %d) -> %s\n", i, myErrorName(err));
                            } else if(aliases[i] != alias) {
                                log_err("FAILURE! ucnv_getAliases(\"utf-8\")[%d] != ucnv_getAlias(\"utf-8\", %d)\n", i, i);
                            }
                        }
                    }
                    uprv_free((char **)aliases);
                }
            }
        }
    }
     /*Testing ucnv_openU()*/
    {
        UChar converterName[]={ 0x0069, 0x0062, 0x006d, 0x002d, 0x0039, 0x0034, 0x0033, 0x0000}; /*ibm-943*/
        const char *illegalNameChars={ "ibm-943 ibm-943 ibm-943 ibm-943 ibm-943 ibm-943 ibm-943 ibm-943 ibm-943 ibm-943"};
        UChar illegalName[100];
        UConverter *converter=NULL;
        err=U_ZERO_ERROR;
        converter=ucnv_openU(converterName, &err);
        if(U_FAILURE(err)){
            log_err("FAILURE! ucnv_openU(ibm-943, err) failed. %s\n", myErrorName(err));
        }
        ucnv_close(converter);
        err=U_ZERO_ERROR;
        converter=ucnv_openU(NULL, &err);
        if(U_FAILURE(err)){
            log_err("FAILURE! ucnv_openU(NULL, err)  failed. %s\n", myErrorName(err));
        }
        ucnv_close(converter);
        /*testing with error value*/
        err=U_ILLEGAL_ARGUMENT_ERROR;
        converter=ucnv_openU(converterName, &err);
        if(!(converter == NULL)){
            log_err("FAILURE! ucnv_openU(ibm-943, U_ILLEGAL_ARGUMENT_ERROR) is expected to fail\n");
        }
        ucnv_close(converter);
        err=U_ZERO_ERROR;
        u_uastrcpy(illegalName, "");
        u_uastrcpy(illegalName, illegalNameChars);
        ucnv_openU(illegalName, &err);
        if(!(err==U_ILLEGAL_ARGUMENT_ERROR)){
            log_err("FAILURE! ucnv_openU(illegalName, err) is expected to fail\n");
        }
        err=U_ZERO_ERROR;
      
    }
      /*Testing ucnv_convert()*/
    {
        int32_t targetLimit=0, sourceLimit=0, i=0, targetCapacity=0;
        const char source[]={ (char)0x00, (char)0x04, (char)0x05, (char)0x06, (char)0xa2, (char)0xb4, (char)0x00};
        const char expectedTarget[]={ (char)0x00, (char)0x37, (char)0x2d, (char)0x2e, (char)0x0e, (char)0x49, (char)0x62, (char)0x0f, (char)0x00};
        char *target;
        sourceLimit=sizeof(source)/sizeof(source[0]);
        err=U_ZERO_ERROR;
        targetLimit=0;
            
        targetCapacity=ucnv_convert("ibm-1364", "ibm-1363", NULL, targetLimit , source, sourceLimit, &err);
        if(err = U_BUFFER_OVERFLOW_ERROR){
            err=U_ZERO_ERROR;
            targetLimit=targetCapacity+1;
            target=(char*)malloc(sizeof(char) * targetLimit);
            targetCapacity=ucnv_convert("ibm-1364", "ibm-1363", target, targetLimit , source, sourceLimit, &err);
            if(U_FAILURE(err)){
                log_err("FAILURE! ucnv_convert(ibm-1363->ibm-1364) failed. %s\n", myErrorName(err));
            }
        }
        for(i=0; i<targetCapacity; i++){
            if(target[i] != expectedTarget[i]){
                log_err("FAIL: ucnv_convert(ibm-1363->ibm-1364) failed.at index \n i=%d,  Expected: %lx Got: %lx\n", i, (UChar)expectedTarget[i], (uint8_t)target[i]);
            }
        }
        /*Test error conditions*/
        i=ucnv_convert("ibm-1364", "ibm-1363", target, targetLimit , source, 0, &err);
        if(i !=0){
            log_err("FAILURE! ucnv_convert() with sourceLimit=0 is expected to return 0\n");
        }
        ucnv_convert("ibm-1364", "ibm-1363", target, targetLimit , source, -1, &err);
        if(!(U_FAILURE(err) && err==U_ILLEGAL_ARGUMENT_ERROR)){
            log_err("FAILURE! ucnv_convert() with sourceLimit=-1 is expected to fail\n");
        }
        sourceLimit=sizeof(source)/sizeof(source[0]);
        i=ucnv_convert("ibm-1364", "ibm-1363", target, targetLimit , source, sourceLimit, &err);
        if(i !=0 ){
            log_err("FAILURE! ucnv_convert() with err=U_ILLEGAL_ARGUMENT_ERROR is expected to return 0\n");
        }
        err=U_ZERO_ERROR;
        sourceLimit=sizeof(source)/sizeof(source[0]);
        targetLimit=0;
        i=ucnv_convert("ibm-1364", "ibm-1363", target, targetLimit , source, sourceLimit, &err);
        if(!(U_FAILURE(err) && err==U_BUFFER_OVERFLOW_ERROR)){
            log_err("FAILURE! ucnv_convert() with targetLimit=0 is expected to throw U_BUFFER_OVERFLOW_ERROR\n");
        }
        err=U_ZERO_ERROR;
        free(target);
        ucnv_flushCache();
    }

    /*Testing ucnv_open()*/

    someConverters[0] = ucnv_open("ibm-949", &err);
    if (U_FAILURE(err)) { log_err("FAILURE!  %s\n", myErrorName(err)); }
    
    someConverters[1] = ucnv_open("ibm-949", &err);
    if (U_FAILURE(err)) { log_err("FAILURE!  %s\n", myErrorName(err)); }
    
    someConverters[2] = ucnv_open("ibm-949", &err);
    if (U_FAILURE(err)) { log_err("FAILURE! %s\n", myErrorName(err)); }
    
    someConverters[3] = ucnv_open("ibm-834", &err);
    if (U_FAILURE(err)) { log_err("FAILURE! %s\n", myErrorName(err)); }
    
    someConverters[4] = ucnv_open("ibm-943", &err);
    if (U_FAILURE(err)) { log_err("FAILURE! %s\n", myErrorName(err));}

    

    
    /* Testing ucnv_flushCache() */
    log_verbose("\n---Testing ucnv_flushCache...\n");
        if (ucnv_flushCache()==0)
        log_verbose("Flush cache ok\n");
    else 
        log_err("Flush Cache failed\n");
    
    /*testing ucnv_close() and ucnv_flushCache() */
     ucnv_close(someConverters[0]);
    ucnv_close(someConverters[1]);
    ucnv_close(someConverters[2]);
    ucnv_close(someConverters[3]);
    
        if (j=ucnv_flushCache()==2) 
        log_verbose("Flush cache ok\n");  /*because first, second and third are same  */
    else 
        log_err("Flush Cache failed or there is an error in ucnv_close()\n");
    
    ucnv_close(someConverters[4]);
    if (ucnv_flushCache()==1) 
        log_verbose("Flush cache ok\n");
    else 
        log_err("Flush Cache failed\n");

    /*Testing ucnv_openCCSID and ucnv_open with error conditions*/
    log_verbose("\n---Testing ucnv_open with err ! = U_ZERO_ERROR...\n");
    err=U_ILLEGAL_ARGUMENT_ERROR;
    if(ucnv_open(NULL, &err) != NULL){
        log_err("ucnv_open with err != U_ZERO_ERROR is supposed to fail\n");
    }
    if(ucnv_openCCSID(1051, UCNV_IBM, &err) != NULL){
        log_err("ucnv_open with err != U_ZERO_ERROR is supposed to fail\n");
    }
    err=U_ZERO_ERROR;
    
    /* Testing ucnv_openCCSID(), ucnv_open(), ucnv_getName() */
    log_verbose("\n---Testing ucnv_open default...\n");
    someConverters[0] = ucnv_open(NULL,&err);
    someConverters[1] = ucnv_open(NULL,&err);
    someConverters[2] = ucnv_open("utf8", &err);
    someConverters[3] = ucnv_openCCSID(949,UCNV_IBM,&err);
    ucnv_close(ucnv_openCCSID(1051, UCNV_IBM, &err)); /* test for j350; ucnv_close(NULL) is safe */
    if (U_FAILURE(err)){ log_err("FAILURE! %s\n", myErrorName(err));}
    
     /* Testing ucnv_getName()*/
    /*default code page */
    ucnv_getName(someConverters[0], &err);
    if(U_FAILURE(err)) {
        log_err("getName[0] failed\n");
    } else {
        log_verbose("getName(someConverters[0]) returned %s\n", ucnv_getName(someConverters[0], &err));
    }
    ucnv_getName(someConverters[1], &err);
    if(U_FAILURE(err)) {
        log_err("getName[1] failed\n");
    } else {
        log_verbose("getName(someConverters[1]) returned %s\n", ucnv_getName(someConverters[1], &err));
    }

    /*Testing ucnv_getDefaultName() and ucnv_setDefaultNAme()*/
    {
        const char* defaultName=ucnv_getDefaultName();
        log_verbose("getDefaultName returned %s\n", defaultName);
   
        /*change the default name by setting it */
        ucnv_setDefaultName("changed");
        if(strcmp(ucnv_getDefaultName(), "changed")==0)
            log_verbose("setDefaultName o.k");
        else
            log_err("setDefaultName failed");  
        /*set the default name back*/
        ucnv_setDefaultName(defaultName);
    }
    
        ucnv_close(someConverters[0]);
        ucnv_close(someConverters[1]);
        ucnv_close(someConverters[2]);
        ucnv_close(someConverters[3]);   
    
       
    for (codepage_index=0; codepage_index <  NUM_CODEPAGE; ++codepage_index)
    {
        i = 0;  
    
    strcpy(ucs_file_name, ctest_getTestDirectory());
    strcat(ucs_file_name, CodePagesTestFiles[codepage_index]);

    ucs_file_in = fopen(ucs_file_name,"rb");
        if (!ucs_file_in) 
        {
            log_err("Couldn't open the Unicode file [%s]... Exiting...\n", ucs_file_name);
		return;	
        }

     /*Creates a converter and testing ucnv_openCCSID(u_int code_page, platform, errstatus*/

    /*     myConverter =ucnv_openCCSID(CodePageNumberToTest[codepage_index],UCNV_IBM, &err); */
    /*  ucnv_flushCache(); */
    myConverter =ucnv_open( "ibm-949", &err);
        if (!myConverter || U_FAILURE(err))   
        {
            log_err("Error creating the convertor \n");
            
		return;
        }
    
    /*testing for ucnv_getName()  */
    log_verbose("Testing ucnv_getName()...\n");
    ucnv_getName(myConverter, &err);
    if(U_FAILURE(err))
        log_err("Error in getName\n");
    else
    {
        log_verbose("getName o.k. %s\n", ucnv_getName(myConverter, &err));
    }
    if (strCaseIcmp(ucnv_getName(myConverter, &err), CodePagesToTest[codepage_index]))
        log_err("getName failed\n");
    else 
        log_verbose("getName ok\n");
    /*Test getName with error condition*/
    { 
        const char* name=0;
        err=U_ILLEGAL_ARGUMENT_ERROR;
        log_verbose("Testing ucnv_getName with err != U_ZERO_ERROR");
        name=ucnv_getName(myConverter, &err);
        if(name != NULL){
            log_err("ucnv_getName() with err != U_ZERO_ERROR is expected to fail");
        }
        err=U_ZERO_ERROR;
    }

    
    /*Tests ucnv_getMaxCharSize() and ucnv_getMinCharSize()*/
    
    log_verbose("Testing ucnv_getMaxCharSize()...\n");
        if (ucnv_getMaxCharSize(myConverter)==CodePagesMaxChars[codepage_index])  
            log_verbose("Max byte per character OK\n");
        else 
            log_err("Max byte per character failed\n");
    
    log_verbose("\n---Testing ucnv_getMinCharSize()...\n");
        if (ucnv_getMinCharSize(myConverter)==CodePagesMinChars[codepage_index])  
            log_verbose("Min byte per character OK\n");
        else 
            log_err("Min byte per character failed\n");
      

    /*Testing for ucnv_getSubstChars() and ucnv_setSubstChars()*/
    log_verbose("\n---Testing ucnv_getSubstChars...\n");
    ii=4;
    ucnv_getSubstChars(myConverter, myptr, &ii, &err);
    
    for(x=0;x<ii;x++) 
        rest = ((unsigned char)rest << 8) + (unsigned char)myptr[x];
    if (rest==CodePagesSubstitutionChars[codepage_index])  
        log_verbose("Substitution character ok\n");
    else 
        log_err("Substitution character failed.\n");
    
    log_verbose("\n---Testing ucnv_setSubstChars RoundTrip Test ...\n");
    ucnv_setSubstChars(myConverter, myptr, ii, &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err)); }
    ucnv_getSubstChars(myConverter,save, &ii, &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err)); }
    
    if (strncmp(save, myptr, ii)) 
        log_err("Saved substitution character failed\n");
    else 
        log_verbose("Saved substitution character ok\n");
    
    /*Testing for ucnv_getSubstChars() and ucnv_setSubstChars() with error conditions*/ 
    log_verbose("\n---Testing ucnv_getSubstChars.. with len < minBytesPerChar\n");
    ii=1;
    ucnv_getSubstChars(myConverter, myptr, &ii, &err);
    if(err != U_INDEX_OUTOFBOUNDS_ERROR){
        log_err("ucnv_getSubstChars() with len < minBytesPerChar should throw U_INDEX_OUTOFBOUNDS_ERROR Got %s\n", myErrorName(err));
    }
    err=U_ZERO_ERROR;
    ii=4;
    ucnv_getSubstChars(myConverter, myptr, &ii, &err);
    log_verbose("\n---Testing ucnv_setSubstChars.. with len < minBytesPerChar\n");
    ucnv_setSubstChars(myConverter, myptr, 0, &err);
    if(err != U_ILLEGAL_ARGUMENT_ERROR){
        log_err("ucnv_setSubstChars() with len < minBytesPerChar should throw U_ILLEGAL_ARGUMENT_ERROR Got %s\n", myErrorName(err));
    }
    log_verbose("\n---Testing ucnv_setSubstChars.. with err != U_ZERO_ERROR \n");
    strcpy(myptr, "abc");
    ucnv_setSubstChars(myConverter, myptr, ii, &err);
    err=U_ZERO_ERROR;
    ucnv_getSubstChars(myConverter, save, &ii, &err);
    if(strncmp(save, myptr, ii) == 0){
        log_err("uncv_setSubstChars() with err != U_ZERO_ERROR shouldn't set the SubstChars and just return\n");
    }
    log_verbose("\n---Testing ucnv_getSubstChars.. with err != U_ZERO_ERROR \n");
    err=U_ZERO_ERROR;
    strcpy(myptr, "abc");
    ucnv_setSubstChars(myConverter, myptr, ii, &err);
    err=U_ILLEGAL_ARGUMENT_ERROR;
    ucnv_getSubstChars(myConverter, save, &ii, &err);
    if(strncmp(save, myptr, ii) == 0){
        log_err("uncv_setSubstChars() with err != U_ZERO_ERROR shouldn't fill the SubstChars in the buffer, it just returns\n");
    }
    err=U_ZERO_ERROR;
    /*------*/
    
    /*resetState  ucnv_reset()*/
    log_verbose("\n---Testing ucnv_reset()..\n");
    ucnv_reset(myConverter);
    {
         UChar32 c;
         const uint8_t in[]={  0x1b, 0x25, 0x42, 0x31, 0x32, 0x61, 0xc0, 0x80, 0xe0, 0x80, 0x80, 0xf0, 0x80, 0x80, 0x80};
         const char *source=(const char *)in, *limit=(const char *)in+sizeof(in);
         UConverter *cnv=ucnv_open("iso-2022", &err);
         if(U_FAILURE(err)) {
            log_err("Unable to open a iso-2022 converter: %s\n", u_errorName(err));
         }
         c=ucnv_getNextUChar(cnv, &source, limit, &err);
         if(U_FAILURE(err) || c != (UChar32)0x0031) {
            log_err("ucnv_getNextUChar() failed: %s\n", u_errorName(err));
         }
         ucnv_reset(cnv);
         ucnv_close(cnv);
         
    }
    
    /*getDisplayName*/
    log_verbose("\n---Testing ucnv_getDisplayName()...\n");
    locale=CodePagesLocale[codepage_index];
    displayname=(UChar*)malloc(1 * sizeof(UChar));
    len=0;
    disnamelen = ucnv_getDisplayName(myConverter,locale,displayname, len, &err); 
    if(err==U_BUFFER_OVERFLOW_ERROR)
      {    
         err=U_ZERO_ERROR;
         displayname=(UChar*)realloc(displayname, (disnamelen+1) * sizeof(UChar));
         ucnv_getDisplayName(myConverter,locale,displayname,disnamelen+1, &err);
         if(U_FAILURE(err))
         {
           log_err("getDisplayName failed the error is  %s\n", myErrorName(err));
         }
         else
           log_verbose(" getDisplayName o.k.\n");
      }
     /*test ucnv_getDiaplayName with error condition*/
     log_verbose("\n---Testing ucnv_getDisplayName()...\n");
     err= U_ILLEGAL_ARGUMENT_ERROR;
     len=ucnv_getDisplayName(myConverter,locale,displayname,disnamelen+1, &err);  
     if( len !=0 ){
           log_err("ucnv_getDisplayName() with err != U_ZERO_ERROR is supposed to return 0\n");
     }
     err=U_ZERO_ERROR;
    
    /* testing ucnv_setFromUCallBack() and ucnv_getFromUCallBack()*/
    MIA1 = ucnv_getFromUCallBack(myConverter);
            
    log_verbose("\n---Testing ucnv_setFromUCallBack...\n");
    ucnv_setFromUCallBack(myConverter, otherUnicodeAction(MIA1), NULL, &oldFromUAction, &oldFromUContext, &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err)); }
    
    if (ucnv_getFromUCallBack(myConverter) != otherUnicodeAction(MIA1)) 
        log_err("get From UCallBack failed\n");
    else 
        log_verbose("get From UCallBack ok\n");

    log_verbose("\n---Testing getFromUCallBack Roundtrip...\n");
    ucnv_setFromUCallBack(myConverter,MIA1, NULL, &oldFromUAction, &oldFromUContext, &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err));  }
    
    if (ucnv_getFromUCallBack(myConverter)!= MIA1) 
        log_err("get From UCallBack action failed\n");
    else 
        log_verbose("get From UCallBack action ok\n");
    
    /*testing ucnv_setToUCallBack with error conditions*/
    err=U_ILLEGAL_ARGUMENT_ERROR;
    log_verbose("\n---Testing setFromUCallBack. with err != U_ZERO_ERROR..\n");
    ucnv_setFromUCallBack(myConverter, otherUnicodeAction(MIA1), NULL, &oldFromUAction, &oldFromUContext, &err);
    if(ucnv_getFromUCallBack(myConverter) == otherUnicodeAction(MIA1)){
        log_err("To setFromUCallBack with err != U_ZERO_ERROR is supposed to fail\n");
    }
    err=U_ZERO_ERROR;

    
    /*testing ucnv_setToUCallBack() and ucnv_getToUCallBack()*/
    MIA2 = ucnv_getToUCallBack(myConverter);
    
    log_verbose("\n---Testing setTo UCallBack...\n");
    ucnv_setToUCallBack(myConverter,otherCharAction(MIA2), NULL, &oldToUAction, &oldToUContext, &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err));}

    if (ucnv_getToUCallBack(myConverter) != otherCharAction(MIA2)) 
        log_err("To UCallBack failed\n");
    else 
        log_verbose("To UCallBack ok\n");
    
    log_verbose("\n---Testing setTo UCallBack Roundtrip...\n");
    ucnv_setToUCallBack(myConverter,MIA2, NULL, &oldToUAction, &oldToUContext, &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err));  }
    
    if (ucnv_getToUCallBack(myConverter) != MIA2)
        log_err("To UCallBack failed\n");
    else 
        log_verbose("To UCallBack ok\n");

    /*testing ucnv_setToUCallBack with error conditions*/
    err=U_ILLEGAL_ARGUMENT_ERROR;
    log_verbose("\n---Testing setToUCallBack. with err != U_ZERO_ERROR..\n");
    ucnv_setToUCallBack(myConverter,otherCharAction(MIA2), NULL, &oldToUAction, &oldToUContext, &err);
    if (ucnv_getToUCallBack(myConverter) == otherCharAction(MIA2)){ 
        log_err("To setToUCallBack with err != U_ZERO_ERROR is supposed to fail\n");
    }
    err=U_ZERO_ERROR;


    /*getcodepageid testing ucnv_getCCSID() */
    log_verbose("\n----Testing getCCSID....\n");
    cp =    ucnv_getCCSID(myConverter,&err);
    if (U_FAILURE(err)) 
    {
        log_err("FAILURE!..... %s\n", myErrorName(err));
    }
    if (cp != CodePageNumberToTest[codepage_index]) 
        log_err("Codepage number test failed\n");
    else 
        log_verbose("Codepage number test OK\n");
    
    /*testing ucnv_getCCSID() with err != U_ZERO_ERROR*/
    err=U_ILLEGAL_ARGUMENT_ERROR;
    if( ucnv_getCCSID(myConverter,&err) != -1){
        log_err("ucnv_getCCSID() with err != U_ZERO_ERROR is supposed to fail\n");
    }
    err=U_ZERO_ERROR;
    
    /*getCodepagePlatform testing ucnv_getPlatform()*/
    log_verbose("\n---Testing getCodepagePlatform ..\n");
    if (CodePagesPlatform[codepage_index]!=ucnv_getPlatform(myConverter, &err))
        log_err("Platform codepage test failed\n");
    else 
        log_verbose("Platform codepage test ok\n");
    
    if (U_FAILURE(err)) 
    { 
        log_err("FAILURE! %s\n", myErrorName(err));
    }
    /*testing ucnv_getPlatform() with err != U_ZERO_ERROR*/
    err= U_ILLEGAL_ARGUMENT_ERROR;
    if(ucnv_getPlatform(myConverter, &err) != UCNV_UNKNOWN){
        log_err("ucnv)getPlatform with err != U_ZERO_ERROR is supposed to fail\n");
    }
    err=U_ZERO_ERROR;
    
     
    /*Reads the BOM*/
        fread(&BOM, sizeof(UChar), 1, ucs_file_in);
        if (BOM!=0xFEFF && BOM!=0xFFFE) 
          {
            log_err("File Missing BOM...Bailing!\n");
		return;
          }

        
     /*Reads in the file*/
     while(!feof(ucs_file_in)&&(i+=fread(ucs_file_buffer+i, sizeof(UChar), 1, ucs_file_in)))
        {
            myUChar = ucs_file_buffer[i-1];
            
            ucs_file_buffer[i-1] = (BOM==0xFEFF)?myUChar:((myUChar >> 8) | (myUChar << 8)); /*adjust if BIG_ENDIAN*/
        }

      myUChar = ucs_file_buffer[i-1];
      ucs_file_buffer[i-1] = (BOM==0xFEFF)?myUChar:((myUChar >> 8) | (myUChar << 8)); /*adjust if BIG_ENDIAN Corner Case*/


     /*testing ucnv_fromUChars() and ucnv_toUChars() */
     /*uchar1---fromUChar--->output_cp_buffer --toUChar--->uchar2*/

      uchar1=(UChar*)malloc(sizeof(UChar) * (u_strlen(ucs_file_buffer)+1));
      u_uastrcpy(uchar1,"");
      u_strncpy(uchar1,ucs_file_buffer,i);

      uchar3=(UChar*)malloc(sizeof(UChar)*(u_strlen(ucs_file_buffer)+1));
      u_uastrcpy(uchar3,"");
      u_strncpy(uchar3,ucs_file_buffer,i);
            
      /*Calls the Conversion Routine */
      testLong1 = MAX_FILE_LEN;
      log_verbose("\n---Testing ucnv_fromUChars()\n");
           targetcapacity = ucnv_fromUChars(myConverter, output_cp_buffer, testLong1,  uchar1, -1, &err);
      if (U_FAILURE(err))  
      {
            log_err("\nFAILURE...%s\n", myErrorName(err));
      }
      else
          log_verbose(" ucnv_fromUChars() o.k.\n");
      /*the codepage intermediate buffer should be null terminated */
      output_cp_buffer[targetcapacity]='\0';
      
      /*test the conversion routine */
      log_verbose("\n---Testing ucnv_toUChars()\n");
      /*call it first time for trapping the targetcapacity and size needed to allocate memory for the buffer uchar2 */
      targetcapacity2=0; 
      targetsize = ucnv_toUChars(myConverter,
                 NULL,
                 targetcapacity2,
                 output_cp_buffer,
                 strlen(output_cp_buffer),
                 &err);
      /*if there is an buffer overflow then trap the values and pass them and make the actual call*/

      if(err==U_BUFFER_OVERFLOW_ERROR)
      {    
         err=U_ZERO_ERROR;
         uchar2=(UChar*)malloc((targetsize) * sizeof(UChar));
              targetsize = ucnv_toUChars(myConverter, 
                   uchar2,
                   targetsize, 
                   output_cp_buffer,
                   strlen(output_cp_buffer),
                   &err);
         
         if(U_FAILURE(err))
           log_err("ucnv_toUChars() FAILED %s\n", myErrorName(err));
         else
           log_verbose(" ucnv_toUChars() o.k.\n");

    if(u_strcmp(uchar1,uchar2)!=0) 
      log_err("equality test failed with convertion routine\n");         
      }
      else
    {
      log_err("ERR: calling toUChars: Didn't get U_BUFFER_OVERFLOW .. expected it.\n");
    }
     /*Testing ucnv_fromUChars and ucnv_toUChars wwith error conditions*/
      err=U_ILLEGAL_ARGUMENT_ERROR;
      log_verbose("\n---Testing ucnv_fromUChars() with err != U_ZERO_ERROR\n");
      targetcapacity = ucnv_fromUChars(myConverter, output_cp_buffer, testLong1,  uchar1, -1, &err);
      if (targetcapacity !=0) {
            log_err("\nFAILURE: ucnv_fromUChars with err != U_ZERO_ERROR is expected to fail and return 0\n");
      }
      err=U_ZERO_ERROR;
      log_verbose("\n---Testing ucnv_fromUChars() with converter=NULL\n");
      targetcapacity = ucnv_fromUChars(NULL, output_cp_buffer, testLong1,  uchar1, -1, &err);
      if (targetcapacity !=0 || err != U_ILLEGAL_ARGUMENT_ERROR) {
            log_err("\nFAILURE: ucnv_fromUChars with converter=NULL is expected to fail\n");
      }
      err=U_ZERO_ERROR;
      log_verbose("\n---Testing ucnv_fromUChars() with sourceLength = 0\n");
      targetcapacity = ucnv_fromUChars(myConverter, output_cp_buffer, testLong1,  uchar1, 0, &err);
      if (targetcapacity !=0) {
            log_err("\nFAILURE: ucnv_fromUChars with sourceLength is expected to fail and return 0\n");
      }
      log_verbose("\n---Testing ucnv_fromUChars() with targetLenth = 0\n");
      targetcapacity = ucnv_fromUChars(myConverter, output_cp_buffer, 0,  uchar1, -1, &err);
      if (err != U_BUFFER_OVERFLOW_ERROR) {
            log_err("\nFAILURE: ucnv_fromUChars with targetLength is expected to fail and throw U_BUFFER_OVERFLOW_ERROR\n");
      }
      /*toUChars with error conditions*/
      targetsize = ucnv_toUChars(myConverter, uchar2, targetsize, output_cp_buffer, strlen(output_cp_buffer), &err);
      if(targetsize != 0){
          log_err("\nFAILURE: ucnv_toUChars with err != U_ZERO_ERROR is expected to fail and return 0\n");
      }
      err=U_ZERO_ERROR;
      targetsize = ucnv_toUChars(myConverter, uchar2, -1, output_cp_buffer, strlen(output_cp_buffer), &err);
      if(targetsize != 0 || err != U_ILLEGAL_ARGUMENT_ERROR){
          log_err("\nFAILURE: ucnv_toUChars with targetsize < 0 is expected to throw U_ILLEGAL_ARGUMENT_ERROR and return 0\n");
      }
      err=U_ZERO_ERROR;
      targetsize = ucnv_toUChars(myConverter, uchar2, 0, output_cp_buffer, 0, &err);
      if (targetsize !=0) {
            log_err("\nFAILURE: ucnv_toUChars with sourceLength is expected to fail and return 0\n");
      }
      targetcapacity2=0; 
      targetsize = ucnv_toUChars(myConverter, NULL, targetcapacity2, output_cp_buffer,  strlen(output_cp_buffer), &err);
      if (err != U_BUFFER_OVERFLOW_ERROR) {
            log_err("\nFAILURE: ucnv_toUChars with targetLength is expected to fail and throw U_BUFFER_OVERFLOW_ERROR\n");
      }
      err=U_ZERO_ERROR;
      /*-----*/


     /*testing for ucnv_fromUnicode() and ucnv_toUnicode() */
         /*Clean up re-usable vars*/
     j=0;
     log_verbose("Testing ucnv_fromUnicode().....\n");
     tmp_ucs_buf=ucs_file_buffer_use; 
     ucnv_fromUnicode(myConverter, &mytarget_1,
                 mytarget + MAX_FILE_LEN,
                 &tmp_ucs_buf,
                 ucs_file_buffer_use+i,
                 NULL,
                 TRUE,
                 &err);
     consumedUni = (UChar*)tmp_consumedUni;
    
     if (U_FAILURE(err)) 
      { 
         log_err("FAILURE! %s\n", myErrorName(err));
     }
     else
         log_verbose("ucnv_fromUnicode()   o.k.\n");

    /*Uni1 ----ToUnicode----> Cp2 ----FromUnicode---->Uni3 */
      log_verbose("Testing ucnv_toUnicode().....\n");
      tmp_mytarget_use=mytarget_use;
      tmp_consumed = consumed;
     ucnv_toUnicode(myConverter, &my_ucs_file_buffer_1,
               my_ucs_file_buffer + MAX_FILE_LEN,
               &tmp_mytarget_use,
               mytarget_use+strlen((char*)mytarget_use),
               NULL,
               FALSE,
               &err);
      consumed = (char*)tmp_consumed;
     if (U_FAILURE(err)) 
     { log_err("FAILURE! %s\n", myErrorName(err)); }
     else
         log_verbose("ucnv_toUnicode()  o.k.\n");
    
  
    log_verbose("\n---Testing   RoundTrip ...\n");
    
    
    u_strncpy(uchar3, my_ucs_file_buffer,i);
    
    if(u_strcmp(uchar1,uchar3)==0)
        log_verbose("Equality test o.k.\n");
    else 
        log_err("Equality test failed\n");

    /*sanity compare */
    if(uchar2 == NULL)
      {
    log_err("uchar2 was NULL (ccapitst.c line %d), couldn't do sanity check\n", __LINE__);
      }
    else
      {
    if(u_strcmp(uchar2, uchar3)==0)
      log_verbose("Equality test o.k.\n");
    else
      log_err("Equality test failed\n");
      }

    fclose(ucs_file_in);    
    ucnv_close(myConverter);
    free(displayname); 
    if (uchar1 != 0) free(uchar1);
    if (uchar2 != 0) free(uchar2);
    if (uchar3 != 0) free(uchar3);    
    }
    
}

void WriteToFile(const UChar *a, FILE *myfile)
{
      uint32_t  size    =  u_strlen(a);
      uint16_t  i       =   0;
      UChar   b       =   0xFEFF;

     /*Writes the BOM*/
     fwrite(&b, sizeof(UChar), 1, myfile);
     for (i=0; i< size; i++)
     {
         b = a[i];
         fwrite(&b, sizeof(UChar), 1, myfile);
     }
     return;
}

     
int32_t strCaseIcmp(const char* a1, const char * a2)
{
    int32_t i=0, ret=0;
    while(a1[i]&&a2[i]) 
    { 
        ret += tolower(a1[i])-tolower(a2[i]); 
        i++;
    }
    return ret;
}

UConverterFromUCallback otherUnicodeAction(UConverterFromUCallback MIA)
{
    return (MIA==(UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP)?(UConverterFromUCallback)UCNV_FROM_U_CALLBACK_SUBSTITUTE:(UConverterFromUCallback)UCNV_FROM_U_CALLBACK_STOP;
}


UConverterToUCallback otherCharAction(UConverterToUCallback MIA)

{
    return (MIA==(UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP)?(UConverterToUCallback)UCNV_TO_U_CALLBACK_SUBSTITUTE:(UConverterToUCallback)UCNV_TO_U_CALLBACK_STOP;
}
