/*
*****************************************************************************************
*                                                                                       *
* COPYRIGHT:                                                                            *
*   (C) Copyright Taligent, Inc.,  1996                                                 *
*   (C) Copyright International Business Machines Corporation,  1999                    *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.                  *
*   US Government Users Restricted Rights - Use, duplication, or disclosure             *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                              *
*                                                                                       *
*****************************************************************************************
********************************************************************************
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

    /*Calling all the UnicodeConverterCPP API and checking functionality*/
  
        /*Tests ucnv_getAvailableName(), getAvialableCount()*/
        
    log_verbose("Testing ucnv_countAvailable()...");
    
    testLong1=ucnv_countAvailable();
    log_verbose("Number of available Codepages:    %d\n", testLong1);
    
    log_verbose("\n---Testing ucnv_getAvailableName..");  /*need to check this out */
    
    available_conv = ucnv_getAvailableName(testLong1);
    
    
     
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

    
    /* Testing ucnv_openCCSID(), ucnv_open(), ucnv_getName() */
    log_verbose("\n---Testing ucnv_open default...\n");
    someConverters[0] = ucnv_open(NULL,&err);
    someConverters[1] = ucnv_open(NULL,&err);
    someConverters[2] = ucnv_open("utf8", &err);
    someConverters[3] = ucnv_openCCSID(949,UCNV_IBM,&err);
    if (U_FAILURE(err)){ log_err("FAILURE! %s\n", myErrorName(err));}
    
    /* Testing ucnv_getName()*/
	/*default code page */
#ifdef WIN32
	if ((strcmp(ucnv_getName(someConverters[0], &err), "IBM-1252")==0)&&
    (strcmp(ucnv_getName(someConverters[1], &err), "IBM-1252")==0))
      log_verbose("getName ok\n");
    else 
        log_err("getName failed\n");
#else
    if ((strcmp(ucnv_getName(someConverters[0], &err), "LATIN_1")==0)&&
    (strcmp(ucnv_getName(someConverters[1], &err), "LATIN_1")==0))
      log_verbose("getName ok\n");
    else 
        log_err("getName failed\n");
#endif    
  
    /*Testing ucnv_getDefaultName() and ucnv_setDefaultNAme()*/
#ifdef WIN32
    if(strcmp(ucnv_getDefaultName(), "ibm-1252")==0)
      log_verbose("getDefaultName o.k.\n");
    else
      log_err("getDefaultName failed \n");
#else
    if(strcmp(ucnv_getDefaultName(), "LATIN_1")==0)
      log_verbose("getDefaultName o.k.\n");
    else
      log_err("getDefaultName failed\n");
#endif
   
	/*chnage the default name by setting it */
    ucnv_setDefaultName("changed");
    if(strcmp(ucnv_getDefaultName(), "changed")==0)
      log_verbose("setDefaultName o.k");
    else
      log_err("setDefaultName failed");
    /*set it back to the original name */
    
    ucnv_setDefaultName("LATIN_1");
    
        


   
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
            exit(0);
        }

     /*Creates a converter and testing ucnv_openCCSID(u_int code_page, platform, errstatus*/

	/*     myConverter =ucnv_openCCSID(CodePageNumberToTest[codepage_index],UCNV_IBM, &err); */
	/*	ucnv_flushCache(); */
	myConverter =ucnv_open( "ibm-949", &err);
        if (!myConverter || U_FAILURE(err))   
        {
            log_err("Error creating the convertor \n");
            
            exit(0);
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
    
    
    
    
    
    
    /*resetState  ucnv_reset()*/
    log_verbose("\n---Testing ucnv_reset()..\n");
    ucnv_reset(myConverter);
    
    
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
    

    
    /* testing getMissingUnicodeAction and setMissingUnicodeAction */
    MIA1 = ucnv_getFromUCallBack(myConverter);
            
    log_verbose("\n---Testing ucnv_setFromUCallBack...\n");
    ucnv_setFromUCallBack(myConverter,otherUnicodeAction(MIA1), &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err)); }
    
    if (ucnv_getFromUCallBack(myConverter) != otherUnicodeAction(MIA1)) 
        log_err("get From UCallBack failed\n");
    else 
        log_verbose("get From UCallBack ok\n");

    log_verbose("\n---Testing getFromUCallBack Roundtrip...\n");
    ucnv_setFromUCallBack(myConverter,MIA1, &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err));  }
    
    if (ucnv_getFromUCallBack(myConverter)!= MIA1) 
        log_err("get From UCallBack action failed\n");
    else 
        log_verbose("get From UCallBack action ok\n");


    
    /*testin ucnv_setMissingCharAction() and ucnv_getMissingCharAction()*/
    MIA2 = ucnv_getToUCallBack(myConverter);
    
    log_verbose("\n---Testing setTo UCallBack...\n");
    ucnv_setToUCallBack(myConverter,otherCharAction(MIA2),&err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err));}

    if (ucnv_getToUCallBack(myConverter) != otherCharAction(MIA2)) 
        log_err("To UCallBack failed\n");
    else 
        log_verbose("To UCallBack ok\n");
    
    log_verbose("\n---Testing setTo UCallBack Roundtrip...\n");
    ucnv_setToUCallBack(myConverter,MIA2, &err);
    if (U_FAILURE(err)) 
    { log_err("FAILURE! %s\n", myErrorName(err));  }
    
    if (ucnv_getToUCallBack(myConverter) != MIA2)
        log_err("To UCallBack failed\n");
    else 
        log_verbose("To UCallBack ok\n");


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


     
    /*Reads the BOM*/
        fread(&BOM, sizeof(UChar), 1, ucs_file_in);
        if (BOM!=0xFEFF && BOM!=0xFFFE) 
          {
            log_err("File Missing BOM...Bailing!\n");
            exit(0);
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
           targetcapacity = ucnv_fromUChars(myConverter, output_cp_buffer, testLong1,  uchar1, &err);
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
