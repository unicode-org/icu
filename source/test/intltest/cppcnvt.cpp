/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include "unicode/locid.h"
#include "unicode/unistr.h"
#include "unicode/convert.h"
#include "unicode/ucnv_err.h"
#include <iostream.h>
#include "cppcnvt.h"

#define NUM_CODEPAGE 1
#define MAX_FILE_LEN 1024*20
#define UCS_FILE_NAME_SIZE 100

/*writes and entire UnicodeString along with a BOM to a file*/
void WriteToFile(const UnicodeString *a, FILE *myfile); 
/*Case insensitive compare*/
int32_t strCaseIcmp(const char* a1,const char * a2); 
/*returns an action other than the one provided*/
UConverterFromUCallback otherUnicodeAction(UConverterFromUCallback MIA);
UConverterToUCallback otherCharAction(UConverterToUCallback MIA);
/*Asciifies the UErrorCodes*/
#define myErrorName(errorCode) u_errorName(errorCode)

void ConvertTest::runIndexedTest( int32_t index, bool_t exec, char* &name, char* par )
{
    if (exec) logln("TestSuite ConvertTest: ");
    switch (index) {
        case 0: name = "TestConvert"; if (exec) TestConvert(); break;
        case 1: name = "TestAmbiguous"; if (exec) TestAmbiguous(); break;
        default: name = ""; break; //needed to end loop
    }
}

void ConvertTest::TestConvert() 
{
    char                subchar [4]         =   {(char)0xBE, (char)0xEF};
    char                myptr[4];
    char                save[4];
    int32_t             testLong1;
    int16_t             rest                =   0;
    FILE*               f                   =   NULL;
    FILE*               f2                  =   NULL;
    int32_t             uniLen              =   0;
    int32_t             len                 =   0;
    int32_t             x                   =   0;
    FILE*               ucs_file_in         =   NULL;
    UChar             BOM                 =   0x0000;
    UChar             myUChar           =   0x0000;
    char                myChar              =   0x00;
    char                mytarget[MAX_FILE_LEN];
    char*               mytarget_1 = mytarget;
    char*               mytarget_use        = mytarget;
    UChar*            consumedUni         =   NULL;
    char*               consumedChar        =   NULL;
    char*               consumed            =   NULL;
    char                output_cp_buffer    [MAX_FILE_LEN];
    UChar             ucs_file_buffer     [MAX_FILE_LEN];
    UChar*            ucs_file_buffer_use = ucs_file_buffer;
    UChar             my_ucs_file_buffer  [MAX_FILE_LEN];
    UChar*            my_ucs_file_buffer_1 = my_ucs_file_buffer;
    int32_t             i                   =   0;
    int8_t             ii                  =   0;
    uint16_t            ij                  =   0;
    int32_t             j                   =   0;
    int32_t             k                   =   0;
    uint16_t            codepage_index      =   0;
    int32_t             cp                  =   0;
    UErrorCode           err                 =   U_ZERO_ERROR;
    const char* const*  available_conv      =   NULL;
    char       ucs_file_name[UCS_FILE_NAME_SIZE];
    UConverterFromUCallback          MIA1;
    UConverterToUCallback MIA2;
    UChar             myUnitarget[MAX_FILE_LEN];
    UChar             *myUnitarget_1 = myUnitarget;
    UnicodeConverterCPP* someConverters[5];
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

    const Locale CodePagesLocale[NUM_CODEPAGE] =
    {
        Locale::KOREAN
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
    /*Calling all the UnicodeConverterCPP API and checking functionality*/
  
        /*Tests UnicodeConverterCPP::getAvailableNames*/
    logln("\n---Testing UnicodeConverterCPP::getAvailableNames...");
    available_conv = UnicodeConverterCPP::getAvailableNames(testLong1, err);

    if (U_FAILURE(err)) 
    {
        errln("Error getting Available names!");
        return;
    }
    logln("Number of available Codepages:\t" + testLong1);
    while (testLong1--) logln((UnicodeString)"\t\t[" + available_conv[testLong1] + (UnicodeString)"]");

    ucnv_flushCache();

    someConverters[0] = new UnicodeConverterCPP("ibm-949",err);
    if (U_FAILURE(err)) errln ((UnicodeString)"FAILURE! " + myErrorName(err));
    someConverters[1] = new UnicodeConverterCPP("ibm-949",err);
    if (U_FAILURE(err)) errln ((UnicodeString)"FAILURE! " + myErrorName(err));
    someConverters[2] = new UnicodeConverterCPP("ibm-949",err);
    if (U_FAILURE(err)) errln ((UnicodeString)"FAILURE! " + myErrorName(err));
    someConverters[3] = new UnicodeConverterCPP("ibm-834", err);
    if (U_FAILURE(err)) errln ((UnicodeString)"FAILURE! " + myErrorName(err));
    someConverters[4] = new UnicodeConverterCPP("ibm-943", err);
    if (U_FAILURE(err)) errln ((UnicodeString)"FAILURE! " + myErrorName(err));
   
    logln("\n---Testing UnicodeConverterCPP::flushCache...");
    if (UnicodeConverterCPP::flushCache()==0) logln("Flush cache ok");
    else errln("Flush Cache failed");
    
    delete someConverters[0];
    delete someConverters[1];
    delete someConverters[2];
    delete someConverters[3];
    if (j=UnicodeConverterCPP::flushCache()==2) logln("Flush cache ok");
    else errln("Flush Cache failed");
    
    delete someConverters[4];
    if (UnicodeConverterCPP::flushCache()==1) logln("Flush cache ok");
    else errln("Flush Cache failed");

    logln("\n---Testing UnicodeConverterCPP::UnicodeConverterCPP()...");
    someConverters[0] = new UnicodeConverterCPP;
    someConverters[1] = new UnicodeConverterCPP;
    someConverters[2] = new UnicodeConverterCPP("utf8", err);
    if (U_FAILURE(err)) errln ((UnicodeString)"FAILURE! " + err);
#ifdef WIN32   
	if ((strcmp(someConverters[1]->getName(err),"IBM-1252")==0)&&
    (strcmp(someConverters[0]->getName(err),"IBM-1252")==0))
      logln("getName ok");
    else errln("getName failed");
    logln(someConverters[1]->getName(err));
#else
    if ((strcmp(someConverters[1]->getName(err),"LATIN_1")==0)&&
    (strcmp(someConverters[0]->getName(err),"LATIN_1")==0))
      logln("getName ok");
    else errln("getName failed");
    logln(someConverters[1]->getName(err));
#endif
    logln("\n---Testing UnicodeConverterCPP::operator==...");
    if (((*someConverters[1] == *someConverters[0])==TRUE)&&
    (*someConverters[1] == *someConverters[2])==FALSE)
      logln("Equality test ok");
    else errln("Equality test failed");
    
    logln("\n---Testing UnicodeConverterCPP::operator!=...");
    if (((*someConverters[1] != *someConverters[0])==FALSE)&&
    (*someConverters[1] != *someConverters[2])==TRUE)
      logln("Not Equal test ok");
    else errln("Not Equal test failed");
    
    logln("\n---Testing UnicodeConverterCPP::operator=...");
    someConverters[3] = new UnicodeConverterCPP;
    *someConverters[3] = *someConverters[2];
    if ((*someConverters[2] == *someConverters[3]))
      logln("Equality test ok");
    else errln("Equality test failed");
   
    delete someConverters[0];
    delete someConverters[1];
    delete someConverters[2];
    delete someConverters[3];

    for (codepage_index=0; codepage_index <  NUM_CODEPAGE; codepage_index++)
    {
        i = 0;  
    strcpy(ucs_file_name, IntlTest::getTestDirectory());
    strcat(ucs_file_name, CodePagesTestFiles[codepage_index]);

    ucs_file_in = fopen(ucs_file_name, "rb");
        if (!ucs_file_in) 
        {
            errln("Couldn't open the Unicode file...");
            return;
        }

        /*Creates a converter*/

    UnicodeConverterCPP* myConverter = new UnicodeConverterCPP(CodePageNumberToTest[codepage_index],UCNV_IBM, err);
    
        if (!myConverter)   
        {
            errln("Error Creating the converter from " + (UnicodeString)CodePagesToTest[codepage_index] + " codepage.\nMake sure you have ran the uconvdef tool to create " + (UnicodeString)CodePagesToTest[codepage_index] + ".cnv in the locales directory"); 
            return;
        }

    
        /*Tests getMaxBytesPerChar and getMinBytesPerChar*/
    logln("\n---Testing UnicodeConverterCPP::getMaxBytesPerChar...");
    if (myConverter->getMaxBytesPerChar()==CodePagesMaxChars[codepage_index])  logln("Max byte per character OK");
    else errln("Max byte per character failed");
    
    logln("\n---Testing UnicodeConverterCPP::getMinBytesPerChar...");
    if (myConverter->getMinBytesPerChar()==CodePagesMinChars[codepage_index])  logln("Min byte per character OK");
    else errln("Min byte per character failed");
      

        /*getSubstitutions char*/
    logln("\n---Testing UnicodeConverterCPP::getSubstitutionChars...");
    ii=4;
    myConverter->getSubstitutionChars(myptr,ii,err);
   
    for(x=0;x<ii;x++) rest = ((unsigned char)rest << 8) + (unsigned char)myptr[x];
    if (rest==CodePagesSubstitutionChars[codepage_index])   logln("Substitution character ok");
    else errln("Substitution character failed.");
    
    
    
    logln("\n---Testing UnicodeConverterCPP::setSubstitutionChars RoundTrip Test ...");
    myConverter->setSubstitutionChars(myptr, ii, err);
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));
    myConverter->getSubstitutionChars(save, ii, err);
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));
    if (strncmp(save, myptr, ii)) errln("Saved substitution character failed");
    else logln("Saved substitution character ok");
    
    /*resetState*/
    logln("\n---Testing UnicodeConverterCPP::resetState...");
    myConverter->resetState();
    
    
    /*getName*/
    UnicodeString* testUnistr          =   new UnicodeString();
    logln("\n---Testing UnicodeConverterCPP::getName...");
    if (strCaseIcmp(myConverter->getName(err),
            CodePagesToTest[codepage_index])) errln("getName failed");
    else logln("getName ok");
    
    /*getDisplayName*/
    testUnistr->remove();
    logln("\n---Testing UnicodeConverterCPP::getDisplayName...");
    myConverter->getDisplayName(CodePagesLocale[codepage_index],*testUnistr);

    /*printUChar(T_UnicodeString_getUChars(testUnistr));
    logln("\nAbove is DisplayName!!");*/
    

        /*getMissingUnicodeAction*/
    /*    logln("\n---Testing UnicodeConverterCPP::getMissingUnicodeAction...");
    if ((MIA1 = myConverter->getMissingUnicodeAction()) != CodePagesMissingUnicodeAction[codepage_index]) errln("Missing action failed");
    else logln("Missing action ok");*/



        /*getMissingCharAction*/
    /*    logln("\n---Testing UnicodeConverterCPP::getMissingCharAction...");
    if ((MIA2 = myConverter->getMissingCharAction()) != CodePagesMissingCharAction[codepage_index]) errln("Missing action failed");
    else logln("Missing action ok");*/

    MIA1 = myConverter->getMissingUnicodeAction();
    MIA2 = myConverter->getMissingCharAction();

        /*setMissingUnicodeAction*/
    logln("\n---Testing UnicodeConverterCPP::setMissingUnicodeAction...");
    myConverter->setMissingUnicodeAction(otherUnicodeAction(MIA1),err);
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));    
    if (myConverter->getMissingUnicodeAction() != otherUnicodeAction(MIA1)) logln("Missing action failed");
    else logln("Missing action ok");


    logln("\n---Testing UnicodeConverterCPP::setMissingUnicodeAction Roundtrip...");
    myConverter->setMissingUnicodeAction(MIA1, err);
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));    
    if (myConverter->getMissingUnicodeAction() != MIA1) errln("Missing action failed");
    else logln("Missing action ok");


        /*setMissingCharAction*/
    logln("\n---Testing UnicodeConverterCPP::setMissingCharAction...");
    myConverter->setMissingCharAction(otherCharAction(MIA2),err);
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));
    if (myConverter->getMissingCharAction() != otherCharAction(MIA2)) errln("Missing action failed");
    else logln("Missing action ok");
    
    logln("\n---Testing UnicodeConverterCPP::setMissingCharAction Roundtrip...");
    myConverter->setMissingCharAction(MIA2, err);
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));    
    if (myConverter->getMissingCharAction() != MIA2) errln("Missing action failed");
    else logln("Missing action ok");


        /*getCodepage*/
    logln("\n---Testing UnicodeConverterCPP::getCodepage...");
    cp =    myConverter->getCodepage(err);
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));    
    if (cp != CodePageNumberToTest[codepage_index]) errln("Codepage number test failed");
    else logln("Codepage number test OK");
    

    
        /*getCodepagePlatform*/
    logln("\n---Testing UnicodeConverterCPP::getCodepagePlatform ...");
    if (CodePagesPlatform[codepage_index]!=myConverter->getCodepagePlatform(err)) errln("Platform codepage test failed");
    else logln("Platform codepage test ok");
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));  


        /*Reads the BOM*/
        fread(&BOM, sizeof(UChar), 1, ucs_file_in);
        if (BOM!=0xFEFF && BOM!=0xFFFE) 
          {
            errln("File Missing BOM...Bailing!");
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

      UnicodeString* uniString  = new UnicodeString(ucs_file_buffer,i);
      UnicodeString* uniString3 = new UnicodeString(ucs_file_buffer,i);
      UnicodeString* uniString2 = new UnicodeString();
    
    
        /*Calls the Conversion Routine*/
        
        testLong1 = MAX_FILE_LEN;

        logln("\n---Testing UnicodeConverterCPP::fromUnicodeString");
        myConverter->fromUnicodeString(output_cp_buffer, testLong1, *uniString, err);
        if (U_FAILURE(err))   logln("\nFAILURE...");

        
        

            /******************************************************************
                                    Checking ksc -> Unicode
            ******************************************************************/

        /*Clean up re-usable vars*/
        j=0;
    
    
        /*Calls the Conversion Routine*/
        /*Uni1 ----ToUnicodeString----> Cp1 ----FromUnicodeString---->Uni2*/

        logln("\n---Testing UnicodeConverterCPP::toUnicodeString");
        myConverter->toUnicodeString(*uniString2 , output_cp_buffer, testLong1,  err);

        if (U_FAILURE(err))   logln ("FAILURE! " + (UnicodeString)myErrorName(err));



    logln("\n---Testing UnicodeString RoundTrip ...");
   
    /*check if Uni1 == Uni2*/
    if (uniString->compare(*uniString2)) errln("Equality check test failed");
    else logln("Equality check test ok");

    /*AIX Compiler hacks*/
    const UChar* tmp_ucs_buf = ucs_file_buffer_use; 
    const UChar* tmp_consumedUni = NULL;

    myConverter->fromUnicode(mytarget_1,
                 mytarget + MAX_FILE_LEN,
                 tmp_ucs_buf,
                 ucs_file_buffer_use+i,
                 NULL,
                 TRUE,
                 err);
    //    consumedUni = (UChar*)tmp_consumedUni;
    
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));
    
    /*Uni1 ----ToUnicode----> Cp2 ----FromUnicode---->Uni3*/

        /*AIX Compiler hacks*/
    const char* tmp_mytarget_use = mytarget_use;
    const char* tmp_consumed = consumed;
    myConverter->toUnicode(my_ucs_file_buffer_1,
               my_ucs_file_buffer + MAX_FILE_LEN,
               tmp_mytarget_use,
               mytarget_use+strlen((char*)mytarget_use),
               NULL,
               FALSE,
               err);
        consumed = (char*)tmp_consumed;
    if (U_FAILURE(err)) errln ("FAILURE! " + (UnicodeString)myErrorName(err));
    
    
    logln("\n---Testing UChar* RoundTrip ...");
   // uniString3->remove();
    uniString3->replace(0, uniString3->length(), my_ucs_file_buffer, i);
    //uniString3 = new UnicodeString(my_ucs_file_buffer,i);
    
    /*checks if Uni1 == Uni3*/ 
    if (uniString->compare(*uniString3)) errln("Equality test failed");
    else logln("Equality test ok");

    /*checks if Uni2 == Uni3 This is a sanity check for the consistency of the
    UnicodeString and Unicode Convters*/ 
    logln("\n---Testing Consistency between UChar* and UnicodeString Conversion...");
    if (uniString2->compare(*uniString3)) errln("Equality test failed");
    else logln("Equality test ok");

    logln("\n---Testing Regression 1100057 ...");
    char mySJIS[12] = {(char)0xFA, (char)0X51, (char)0XB8, (char)0XDB, (char)0XBD, (char)0XCB, (char)0XDB, (char)0XCC, (char)0XDE, (char)0XD0 , (char)0XFA, (char)0X50};
    

    UnicodeConverterCPP SJIS(943, UCNV_IBM, err);
    UnicodeString myString;


    SJIS.toUnicodeString(myString, mySJIS, 12, err);
    if (U_FAILURE(err)||(myString.length()!=10)) errln("toUnicodeString test failed");
    else logln("toUnicodeString test ok");

    fclose(ucs_file_in);    
    delete myConverter;
#ifndef WIN32
    delete uniString;
    delete uniString2;
    delete uniString3;
    delete testUnistr;
#endif
  
    }
}

void WriteToFile(const UnicodeString *a, FILE *myfile)
{
      uint32_t  size    =   a->length();
      uint16_t  i       =   0;
      UChar   b       =   0xFEFF;

     /*Writes the BOM*/
     fwrite(&b, sizeof(UChar), 1, myfile);
     for (i=0; i< size; i++)
     {
         b = (*a)[i];
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
void ConvertTest::TestAmbiguous() 
{
    UErrorCode status = U_ZERO_ERROR;
    UnicodeConverterCPP *ascii_cnv = 0, *sjis_cnv = 0;
    const char *target = "\\usr\\local\\share\\data\\icutest.txt";
    UnicodeString asciiResult, sjisResult;
    
    sjis_cnv = new UnicodeConverterCPP("SJIS", status);
    if (U_FAILURE(status))
    {
	    errln("Failed to create a SJIS converter\n");
        return;
    }
    ascii_cnv = new UnicodeConverterCPP("LATIN-1", status);
    if (U_FAILURE(status))
    {
	    errln("Failed to create a SJIS converter\n");
        delete sjis_cnv;
        return;
    }
    /* convert target from SJIS to Unicode */
    sjis_cnv->toUnicodeString(sjisResult, target, strlen(target), status);
    if (U_FAILURE(status))
    {
        errln("Failed to convert the SJIS string.\n");
        delete sjis_cnv;
        delete ascii_cnv;
        return;
    }

    /* convert target from Latin-1 to Unicode */
    ascii_cnv->toUnicodeString(asciiResult, target, strlen(target), status);
    if (U_FAILURE(status))
    {
        errln("Failed to convert the Latin-1 string.\n");
        delete sjis_cnv;
        delete ascii_cnv;
        return;
    }
    
    if (!sjis_cnv->isAmbiguous())
    {
        errln("SJIS converter should contain ambiguous character mappings.\n");
        delete sjis_cnv;
        delete ascii_cnv;
        return;
    }
    if (sjisResult == asciiResult)
    {
        errln("File separators for SJIS don't need to be fixed.\n");
    }
    sjis_cnv->fixFileSeparator(sjisResult);
    if (sjisResult != asciiResult)
    {
        errln("Fixing file separator for SJIS failed.\n");
    }
    delete sjis_cnv;
    delete ascii_cnv;

}
