/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1998-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File test.c
*
* Modification History:
*
*   Date          Name        Description
*   02/22/2000    Madhu       Creation
*******************************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/udata.h"
#include "unicode/uchar.h"
#include "unicode/ucnv.h"
#include "unicode/ures.h"
#include "unicode/ustring.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "udatamem.h"
#include "cintltst.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <stdio.h>

#ifdef WIN32
#include <io.h>
#else
#include <unistd.h>
#endif

/* includes for TestSwapData() */
#include "udataswp.h"

/* swapping implementations in common */
#include "uresdata.h"
#include "ucnv_io.h"
#include "uprops.h"
#include "ucol_swp.h"
#include "ucnv_bld.h"
#include "unormimp.h"
#include "sprpimpl.h"
#include "propname.h"
#include "rbbidata.h"

/* other definitions and prototypes */

#define LENGTHOF(array) (int32_t)(sizeof(array)/sizeof((array)[0]))

static void TestUDataOpen(void);
static void TestUDataOpenChoiceDemo1(void);
static void TestUDataOpenChoiceDemo2(void);
static void TestUDataGetInfo(void);
static void TestUDataGetMemory(void);
static void TestUDataSetAppData(void);
static void TestErrorConditions(void);
static void TestAppData(void);
static void TestICUDataName(void);
static void TestSwapData(void);

void addUDataTest(TestNode** root);

void
addUDataTest(TestNode** root)
{
    addTest(root, &TestUDataOpen,       "udatatst/TestUDataOpen"      );
    addTest(root, &TestUDataOpenChoiceDemo1, "udatatst/TestUDataOpenChoiceDemo1");
    addTest(root, &TestUDataOpenChoiceDemo2, "udatatst/TestUDataOpenChoiceDemo2"); 
    addTest(root, &TestUDataGetInfo,    "udatatst/TestUDataGetInfo"   );
    addTest(root, &TestUDataGetMemory,  "udatatst/TestUDataGetMemory" );
    addTest(root, &TestUDataSetAppData, "udatatst/TestUDataSetAppData" );
    addTest(root, &TestErrorConditions, "udatatst/TestErrorConditions");
    addTest(root, &TestAppData, "udatatst/TestAppData" );
    addTest(root, &TestICUDataName, "udatatst/TestICUDataName" );
    addTest(root, &TestSwapData, "udatatst/TestSwapData" );
}

#if 0
static void lots_of_mallocs()
{
    int q;
    for(q=1;q<100;q++)
    {
        free(malloc(q));
        malloc(q*2);
    }

}
#endif

static void TestUDataOpen(){
    UDataMemory *result;
    UErrorCode status=U_ZERO_ERROR;
    const char* memMap[][2]={
        {"root", "res"},
        {"unorm", "icu"},
        {"cnvalias", "icu"},
        {"unames",   "icu"},
        {"ibm-37_P100-1995",   "cnv"}
    };
    const char* name            = "test";
    const char* type            = "icu";
    const char  dirSepString[]  = {U_FILE_SEP_CHAR, 0};
    const char  pathSepString[] = {U_PATH_SEP_CHAR, 0};


    char* path=(char*)malloc(sizeof(char) * (strlen(ctest_dataOutDir())
                                           + strlen(U_ICUDATA_NAME)
                                           + strlen("/build")+1 ) );

    char        *icuDataFilePath = 0;
    struct stat stat_buf;
    
    const char* testPath=loadTestData(&status);

    /* lots_of_mallocs(); */

    strcat(strcpy(path, ctest_dataOutDir()), U_ICUDATA_NAME);

    log_verbose("Testing udata_open()\n");
    result=udata_open(testPath, type, name, &status);
    if(U_FAILURE(status)){
        log_err("FAIL: udata_open() failed for path = %s, name=%s, type=%s, \n errorcode=%s\n", testPath, name, type, myErrorName(status));
    } else {
        log_verbose("PASS: udata_open worked\n");
        udata_close(result);
    }

    /* If the ICU system common data file is present in this confiugration,   
     *   verify that udata_open can explicitly fetch items from it.
     *   If packaging mode == dll, the file may not exist.  So, if the file is 
     *   missing, skip this test without error.
     */
    icuDataFilePath = (char *)malloc(strlen(path) + 10);
    strcpy(icuDataFilePath, path);
    strcat(icuDataFilePath, ".dat");
    /* lots_of_mallocs(); */
    if (stat(icuDataFilePath, &stat_buf) == 0)
    {
        int i;
        log_verbose("Testing udata_open() on %s\n", icuDataFilePath);
        for(i=0; i<sizeof(memMap)/sizeof(memMap[0]); i++){
            /* lots_of_mallocs(); */
            status=U_ZERO_ERROR;
            result=udata_open(path, memMap[i][1], memMap[i][0], &status);
            if(U_FAILURE(status)) {
                log_data_err("FAIL: udata_open() failed for path = %s, name=%s, type=%s, \n errorcode=%s\n", path, memMap[i][0], memMap[i][1], myErrorName(status));
            } else {
                log_verbose("PASS: udata_open worked for path = %s, name=%s, type=%s\n",  path, memMap[i][0], memMap[i][1]);
                udata_close(result);
            }
        }
    }
    else
    {
    /* lots_of_mallocs(); */
         log_verbose("Skipping tests of udata_open() on %s.  File not present in this configuration.\n",
             icuDataFilePath);
    }
    free(icuDataFilePath);
    icuDataFilePath = NULL;
    /* lots_of_mallocs(); */

    /* If the ICU individual files used to build the ICU system common data are
     *   present in this configuration,   
     *   verify that udata_open can explicitly open them.
     *   These data files are present in the ICU data/build directory after a build
     *    completes.  Tests are most commonly run with the data directory pointing
     *    back into this directory structure, but this is not required.  Soooo, if
     *    the files are missing, skip this test without error.
     */
    /* lots_of_mallocs(); */
    icuDataFilePath = (char *)malloc(strlen(ctest_dataOutDir()) + 50);
    strcpy(icuDataFilePath, ctest_dataOutDir());
    strcat(icuDataFilePath, "build");
    strcat(icuDataFilePath, dirSepString);
    strcat(icuDataFilePath, U_ICUDATA_NAME);
    strcat(icuDataFilePath, "_");
    strcat(icuDataFilePath, "unorm.icu");

    /* lots_of_mallocs(); */
/*    if (stat(icuDataFilePath, &stat_buf) == 0)*/
    {
        int i;
        log_verbose("%s exists, so..\n", icuDataFilePath);
        strcpy(icuDataFilePath, ctest_dataOutDir());
        strcat(icuDataFilePath, "build");
        strcat(icuDataFilePath, dirSepString);
        strcat(icuDataFilePath, U_ICUDATA_NAME);
        log_verbose("Testing udata_open() on %s\n", icuDataFilePath);
        for(i=0; i<sizeof(memMap)/sizeof(memMap[0]); i++){
            status=U_ZERO_ERROR;
            result=udata_open(icuDataFilePath, memMap[i][1], memMap[i][0], &status);
            if(U_FAILURE(status)) {
                log_data_err("FAIL: udata_open() failed for path = %s, name=%s, type=%s, \n errorcode=%s\n", icuDataFilePath, memMap[i][0], memMap[i][1], myErrorName(status));
            } else {
                log_verbose("PASS: udata_open worked for path = %s, name=%s, type=%s\n",  icuDataFilePath, memMap[i][0], memMap[i][1]);
                udata_close(result);
            }
        }
    }
/*    else
    {
         log_verbose("Skipping tests of udata_open() on %s.  File not present in this configuration.\n",
             icuDataFilePath);
    }*/

    free(icuDataFilePath);
    icuDataFilePath = NULL;

    /*
     * Test fallback file names for open of separate data files.
     *    With these params to udata_open:
     *       path = wherever/testdata
     *       type = typ
     *       name = nam
     *     these files will be tried first:
     *              wherever/testudata_nam.typ
     *              testudata_nam.typ
     *  A test data file named testudata_nam.typ exists for the purpose of testing this.
     */
    log_verbose("Testing udata_open, with base_name.type style fallback to individual file.\n");

    status = U_ZERO_ERROR;
    result = udata_open( testPath, "typ", "nam", &status);
    if (status != U_ZERO_ERROR) {
        log_err("FAIL: udata_open( \"%s\", \"typ\", \"nam\") returned status %s\n", testPath, u_errorName(status));
    }
    udata_close(result);
    free(icuDataFilePath);

    
    /* This type of path is deprecated */
    /*
     * Another fallback test.   Paths ending with a trailing directory separator
     *    take a slightly different code path, with the "base name" from the path
     *    being empty in the internal udata_open logic.
     */

/*      log_verbose("Testing udata_open, with path containing a trailing directory separator.\n"); */
/*      icuDataFilePath = (char *)malloc(strlen(u_getDataDirectory()) + 50); */
/*      strcpy(icuDataFilePath, testPath); */
/*      status = U_ZERO_ERROR; */
/*      result = udata_open( icuDataFilePath, "cnv", "test1", &status); */
/*      if (status != U_ZERO_ERROR) { */
/*          log_err("FAIL: udata_open( \"%s\", \"cnv\", \"test1\") returned status %s\n", icuDataFilePath, u_errorName(status)); */
/*      } */
/*      udata_close(result); */
/*      free(icuDataFilePath); */


    log_verbose("Testing udata_open() with a non existing binary file\n");
    result=udata_open("testdata", "tst", "nonexist", &status);
    if(status==U_FILE_ACCESS_ERROR){
        log_verbose("Opening udata_open with non-existing file handled correctly.\n");
        status=U_ZERO_ERROR;
    } else {
        log_err("calling udata_open with non-existing file  [testdata | nonexist.tst] not handled correctly\n.  Expected: U_FILE_ACCESS_ERROR, Got: %s\n", myErrorName(status));
        if(U_SUCCESS(status)) {
            udata_close(result);
        }
    }

    if(result != NULL){
        log_err("calling udata_open with non-existing file didn't return a null value\n");
    } else {
        log_verbose("calling udat_open with non-existing file returned null as expected\n");
    }

    /*
     *  Try opening data with absurdly long path and name, to trigger buffer size 
     *   overflow handling code.
     */
    {
        char longTestPath[1024];    /* Implementation goes to heap at length of 128.  */
        char longName[1024];

        /* Try a very long nonexistent directory path.  
         * udata_open should still succeed.  Opening with the path will fail,
         * then fall back to skipping the directory portion of the path.
         */
        log_verbose("Testing udata_open() with really long names\n");
        longTestPath[0] = 0;
        strcat(longTestPath, "bogus_directory_name");
        while (strlen(longTestPath) < 500) {
            strcat(longTestPath, dirSepString);
            strcat(longTestPath, "bogus_directory_name");
        }
        strcat(longTestPath, pathSepString);
        strcat(longTestPath, testPath);
        result=udata_open(longTestPath, type, name, &status);
        if(U_FAILURE(status)){
            log_err("FAIL: udata_open() failed for path = %s\n name=%s, type=%s, \n errorcode=%s\n",
                longTestPath, name, type, myErrorName(status));
        } else {
            log_verbose("PASS: udata_open worked\n");
            udata_close(result);
        }

        /* Try a very long name.  Won't open, but shouldn't blow up.
         */
        longName[0] = 0;
        while (strlen(longName) < 500) {
            strcat(longName, name);
            strcat(longName, "_");
        }
        strcat(longName, dirSepString);
        strcat(longName, name);

        result=udata_open(longTestPath, type, longName, &status);
        if (status != U_FILE_ACCESS_ERROR) {
            log_err("FAIL: udata_open() failed for path = %s\n name=%s, type=%s, \n errorcode=%s\n",
                longTestPath, longName, type, myErrorName(status));
        }
        udata_close(result);
    }

    free(path);
}



static void TestUDataSetAppData(){
/*    UDataMemory      *dataItem;*/

    UErrorCode        status=U_ZERO_ERROR;
    int               fileHandle = 0;              /* We are going to read the testdata.dat file */
    struct stat       statBuf;
    size_t            fileSize = 0;
    char             *fileBuf = 0;

    size_t            i;
       
    /* Open the testdata.dat file, using normal   */
    const char* tdrelativepath = loadTestData(&status);
    char* filePath=(char*)malloc(sizeof(char) * (strlen(tdrelativepath) + strlen(".dat") +1 +strlen(tdrelativepath)) );

    strcpy(filePath, tdrelativepath);
    strcat(filePath, ".dat");

    log_verbose("Testing udata_setAppData() with %s\n", filePath);

#if defined(WIN32) || defined(U_CYGWIN)
    fileHandle = open( filePath, O_RDONLY | O_BINARY );
#else
    fileHandle = open( filePath, O_RDONLY);
#endif
    if( fileHandle == -1 ) {
        log_err("FAIL: TestUDataSetAppData() can not open(\"%s\", O_RDONLY)\n", filePath);
        goto cleanupAndReturn;
    }

    /* 
     *Find the size of testdata.dat, and read the whole thing into memory
     */
    if (fstat(fileHandle, &statBuf) == 0) {
        fileSize = statBuf.st_size;
    }
    if (fileSize == 0) {
        log_err("FAIL: TestUDataSetAppData() can not find size of file \"%s\".\n", filePath);
        goto cleanupAndReturn;
    }

    fileBuf = (char *)ctst_malloc(fileSize);
    if (fileBuf == 0) {
        log_err("FAIL: TestUDataSetAppData() can not malloc(%d) for file \"%s\".\n", fileSize, filePath);
        goto cleanupAndReturn;
    }

    i = read(fileHandle, fileBuf, fileSize);
    if (i != fileSize) {
        log_err("FAIL: TestUDataSetAppData() error reading file \"%s\" size=%d read=%d.\n", filePath, fileSize, i);
        goto cleanupAndReturn;
    }

    /*
     * Got testdata.dat into memory, now we try setAppData using the memory image.
     */

    status=U_ZERO_ERROR;
    udata_setAppData("appData1", fileBuf, &status); 
    if (status != U_ZERO_ERROR) {
        log_err("FAIL: TestUDataSetAppData(): udata_setAppData(\"appData1\", fileBuf, status) "
                " returned status of %s\n", u_errorName(status));
        goto cleanupAndReturn;
    }

    udata_setAppData("appData2", fileBuf, &status); 
    if (status != U_ZERO_ERROR) {
        log_err("FAIL: TestUDataSetAppData(): udata_setAppData(\"appData2\", fileBuf, status) "
                " returned status of %s\n", u_errorName(status));
        goto cleanupAndReturn;
    }

    /*  If we try to setAppData with the same name a second time, we should get a 
     *    a using default warning.
     */
    udata_setAppData("appData2", fileBuf, &status); 
    if (status != U_USING_DEFAULT_WARNING) {
        log_err("FAIL: TestUDataSetAppData(): udata_setAppData(\"appData2\", fileBuf, status) "
                " returned status of %s, expected U_USING_DEFAULT_WARNING.\n", u_errorName(status));
    }


    /** It is no longer  correct to use udata_setAppData to change the 
        package of a contained item.
        
        dataItem = udata_open("appData1", "res", "te_IN", &status); **/

cleanupAndReturn:
    /*  Note:  fileBuf is not deleted because ICU retains a pointer to it
     *         forever (until ICU is shut down).
     */
    if (fileHandle > 0) {
        close(fileHandle);
    }
    free(filePath);
    return;
}


static UBool U_CALLCONV
isAcceptable1(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {

    if( pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x43 &&   /* dataFormat="CvAl" */
        pInfo->dataFormat[1]==0x76 &&
        pInfo->dataFormat[2]==0x41 &&
        pInfo->dataFormat[3]==0x6c &&
        pInfo->formatVersion[0]==3 )
    {
        log_verbose("The data from \"%s.%s\" IS acceptable using the verifing function isAcceptable1()\n", name, type);
        return TRUE;
    } else {
        log_verbose("The data from \"%s.%s\" IS NOT acceptable using the verifing function isAcceptable1():-\n"
            "\tsize              = %d\n"
            "\tisBigEndian       = %d\n"
            "\tcharsetFamily     = %d\n"
            "\tformatVersion[0]  = %d\n"
            "\tdataVersion[0]    = %d\n"
            "\tdataFormat        = %c%c%c%c\n",
            name, type, pInfo->size,  pInfo->isBigEndian, pInfo->charsetFamily, pInfo->formatVersion[0], 
            pInfo->dataVersion[0], pInfo->dataFormat[0], pInfo->dataFormat[1], pInfo->dataFormat[2], 
            pInfo->dataFormat[3]);  
        log_verbose("Call another verifing function to accept the data\n");
        return FALSE;
    }
}

static UBool U_CALLCONV
isAcceptable2(void *context, 
             const char *type, const char *name,
      const UDataInfo *pInfo){
    UVersionInfo unicodeVersion;

    u_getUnicodeVersion(unicodeVersion);

    if( pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x75 &&   /* dataFormat="unam" */
        pInfo->dataFormat[1]==0x6e &&
        pInfo->dataFormat[2]==0x61 &&
        pInfo->dataFormat[3]==0x6d &&
        pInfo->formatVersion[0]==1 &&
        pInfo->dataVersion[0]==unicodeVersion[0] )
    {
        log_verbose("The data from \"%s.%s\" IS acceptable using the verifing function isAcceptable2()\n", name, type);
        return TRUE;
    } else {
        log_verbose("The data from \"%s.%s\" IS NOT acceptable using the verifing function isAcceptable2()\n", name, type);

        return FALSE;
    }


}
static UBool U_CALLCONV
isAcceptable3(void *context, 
             const char *type, const char *name,
             const UDataInfo *pInfo){

    if( pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x54 &&   /* dataFormat="test" */
        pInfo->dataFormat[1]==0x65 &&
        pInfo->dataFormat[2]==0x73 &&
        pInfo->dataFormat[3]==0x74 &&
        pInfo->formatVersion[0]==1 &&
        pInfo->dataVersion[0]==1   ) {
        log_verbose("The data from \"%s.%s\" IS acceptable using the verifing function isAcceptable3()\n", name, type);

        return TRUE;
    } else {
        log_verbose("The data from \"%s.%s\" IS NOT acceptable using the verifing function isAcceptable3()\n", name, type);
        return FALSE;
    }


}

static void TestUDataOpenChoiceDemo1() {
    UDataMemory *result;
    UErrorCode status=U_ZERO_ERROR;
	
    const char* name[]={
        "cnvalias",
        "unames",
        "test"
    };
    const char* type="icu";
    const char* testPath="testdata";

    result=udata_openChoice(NULL, "icu", name[0], isAcceptable1, NULL, &status);
    if(U_FAILURE(status)){
        log_err("FAIL: udata_openChoice() failed name=%s, type=%s, \n errorcode=%s\n", name[0], type, myErrorName(status));
    } else {
        log_verbose("PASS: udata_openChoice worked\n");
        udata_close(result);
    }

    result=udata_openChoice(NULL, type, name[1], isAcceptable1, NULL, &status);
    if(U_FAILURE(status)){
        status=U_ZERO_ERROR;
        result=udata_openChoice(NULL, type, name[1], isAcceptable2, NULL, &status);
        if(U_FAILURE(status)){
            log_err("FAIL: udata_openChoice() failed name=%s, type=%s, \n errorcode=%s\n", name[1], type, myErrorName(status));
        }
    }

    if(U_SUCCESS(status)){
        udata_close(result);
    }

    result=udata_openChoice(testPath, type, name[2], isAcceptable1, NULL, &status);
    if(U_FAILURE(status)){
        status=U_ZERO_ERROR;
        result=udata_openChoice(testPath, type, name[2], isAcceptable3, NULL, &status);
        if(U_FAILURE(status)){
            log_err("FAIL: udata_openChoice() failed path=%s name=%s, type=%s, \n errorcode=%s\n", testPath, name[2], type, myErrorName(status));
        }
    }

    if(U_SUCCESS(status)){
        udata_close(result);
    }
}

static UBool U_CALLCONV
isAcceptable(void *context, 
             const char *type, const char *name,
             const UDataInfo *pInfo){
    if( pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x54 &&   /* dataFormat="test" */
        pInfo->dataFormat[1]==0x65 &&
        pInfo->dataFormat[2]==0x73 &&
        pInfo->dataFormat[3]==0x74 &&
        pInfo->formatVersion[0]==1 &&
        pInfo->dataVersion[0]==1   &&
        *((int*)context) == 2 ) {
        log_verbose("The data from\"%s.%s\" IS acceptable using the verifing function isAcceptable()\n", name, type);

        return TRUE;
    } else {
        log_verbose("The data from \"%s.%s\" IS NOT acceptable using the verifing function isAcceptable()\n", name, type);
        return FALSE;
    }
}


/* This test checks to see if the isAcceptable function is being called correctly. */

static void TestUDataOpenChoiceDemo2() {
    UDataMemory *result;
    UErrorCode status=U_ZERO_ERROR;
    int i;
    int p=2;

    const char* name="test";
    const char* type="icu";
    const char* path = loadTestData(&status);

    result=udata_openChoice(path, type, name, isAcceptable, &p, &status);
    if(U_FAILURE(status)){
        log_err("failed to load data at p=%s t=%s n=%s, isAcceptable", path, type, name);
    }
    if(U_SUCCESS(status) ) {
        udata_close(result);
    }

    p=0;
    for(i=0;i<2; i++){
        result=udata_openChoice(path, type, name, isAcceptable, &p, &status);
        if(p<2) {
            if(U_FAILURE(status) && status==U_INVALID_FORMAT_ERROR){
                log_verbose("Loads the data but rejects it as expected %s\n", myErrorName(status));
                status=U_ZERO_ERROR;
                p++;
            }
            else {
                log_err("FAIL: failed to either load the data or to reject the loaded data. ERROR=%s\n", myErrorName(status) );
            }
        }
        else if(p == 2) {
            if(U_FAILURE(status)) {
                log_err("FAIL: failed to load the data and accept it.  ERROR=%s\n", myErrorName(status) );
            }
            else {
                log_verbose("Loads the data and accepts it for p==2 as expected\n");
                udata_close(result);
            }
        }
    }
}


static void TestUDataGetInfo() {

    UDataMemory *result;
    /* UDataInfo cf. udata.h */
    static UDataInfo dataInfo={
    30,    /*sizeof(UDataInfo),*/
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x54, 0x65, 0x73, 0x74},     /* dataFormat="Test" */
    {9, 0, 0, 0},                 /* formatVersion */
    {4, 0, 0, 0}                  /* dataVersion */
    };
    UErrorCode status=U_ZERO_ERROR;
    const char* name="cnvalias";
    const char* name2="test";
    const char* type="icu";

    const char* testPath=loadTestData(&status);

    log_verbose("Testing udata_getInfo() for cnvalias.icu\n");
    result=udata_open(NULL, "icu", name, &status);
    if(U_FAILURE(status)){
        log_err("FAIL: udata_open() failed for path = NULL, name=%s, type=%s, \n errorcode=%s\n",  name, type, myErrorName(status));
        return;
    }
    udata_getInfo(result, &dataInfo);
    if(dataInfo.size==20            &&  dataInfo.size!=30 &&
        dataInfo.isBigEndian==U_IS_BIG_ENDIAN       &&  
        dataInfo.charsetFamily==U_CHARSET_FAMILY    &&
        dataInfo.dataFormat[0]==0x43 &&  dataInfo.dataFormat[0]!=0x54 && /* dataFormat="CvAl" and not "Test". The values are set for cnvalias.dat*/
        dataInfo.dataFormat[1]==0x76 &&  dataInfo.dataFormat[1]!=0x65 &&
        dataInfo.dataFormat[2]==0x41 &&  dataInfo.dataFormat[2]!=0x73 &&
        dataInfo.dataFormat[3]==0x6c &&  dataInfo.dataFormat[3]!=0x74 &&
        dataInfo.formatVersion[0]!=9 && /*formatVersion is also set to the one for cnvalias*/
        dataInfo.dataVersion[0]!=4   && /*dataVersion*/
        dataInfo.dataVersion[1]!=0   ){
            log_verbose("PASS: udata_getInfo() filled in the right values\n");
    } else {
        log_err("FAIL: udata_getInfo() filled in the wrong values\n");
    }
    udata_close(result);


    log_verbose("Testing udata_getInfo() for test.icu\n");
    result=udata_open(testPath, type, name2, &status);
    if(U_FAILURE(status)) {
       log_err("FAIL: udata_open() failed for path=%s name2=%s, type=%s, \n errorcode=%s\n", testPath, name2, type, myErrorName(status));
       return;
    }
    udata_getInfo(result, &dataInfo);
    if(dataInfo.size==20             &&
        dataInfo.isBigEndian==U_IS_BIG_ENDIAN       &&  
        dataInfo.charsetFamily==U_CHARSET_FAMILY    &&
        dataInfo.dataFormat[0]==0x54 &&   /* dataFormat="Test". The values are set for test.dat*/
        dataInfo.dataFormat[1]==0x65 && 
        dataInfo.dataFormat[2]==0x73 &&  
        dataInfo.dataFormat[3]==0x74 &&  
        dataInfo.formatVersion[0]==1 &&  /*formatVersion is also set to the one for test*/
        dataInfo.dataVersion[0]==1   &&  /*dataVersion*/
        dataInfo.dataVersion[1]==0   )
    {
        log_verbose("PASS: udata_getInfo() filled in the right values\n");
    } else {
        log_err("FAIL: udata_getInfo() filled in the wrong values\n");
    }
    udata_close(result);
}

static void TestUDataGetMemory() {

    UDataMemory *result;
    const int32_t *table=NULL;
    uint16_t* intValue=0;
    UErrorCode status=U_ZERO_ERROR;
    const char* name="cnvalias";
    const char* type;

    const char* name2="test";

    const char* testPath = loadTestData(&status);

    type="icu";
    log_verbose("Testing udata_getMemory() for \"cnvalias.icu\"\n");
    result=udata_openChoice(NULL, type, name, isAcceptable1, NULL, &status);
    if(U_FAILURE(status)){
        log_err("FAIL: udata_openChoice() failed for name=%s, type=%s, \n errorcode=%s\n", name, type, myErrorName(status));
        return;
    }
    table=(const int32_t *)udata_getMemory(result);

    /* The alias table may list more converters than what's actually available now. [grhoten] */
    if(ucnv_countAvailable() > table[1])      /*???*/
        log_err("FAIL: udata_getMemory() failed ucnv_countAvailable returned = %d, expected = %d\n", ucnv_countAvailable(), table[1+2*(*table)]);

    udata_close(result);

    type="icu";
    log_verbose("Testing udata_getMemory for \"test.icu\"()\n");
    result=udata_openChoice(testPath, type, name2, isAcceptable3, NULL, &status);
    if(U_FAILURE(status)){
        log_err("FAIL: udata_openChoice() failed for path=%s name=%s, type=%s, \n errorcode=%s\n", testPath, name2, type, myErrorName(status));
        return;
    }
    intValue=(uint16_t *)udata_getMemory(result);
    /*printf("%d ..... %s", *(intValue), intValue+1));*/
    if( *intValue != 2000 || strcmp((char*)(intValue+1), "YEAR") != 0 )
        log_err("FAIL: udata_getMemory() failed: intValue :- Expected:2000 Got:%d \n\tstringValue:- Expected:YEAR Got:%s\n", *intValue, (intValue+1));

    udata_close(result);

}

static void TestErrorConditions(){

    UDataMemory *result=NULL;
    UErrorCode status=U_ZERO_ERROR;
    uint16_t* intValue=0;
    static UDataInfo dataInfo={
        30,    /*sizeof(UDataInfo),*/
        0,

        U_IS_BIG_ENDIAN,
        U_CHARSET_FAMILY,
        sizeof(UChar),
        0,

        {0x54, 0x65, 0x73, 0x74},     /* dataFormat="Test" */
        {9, 0, 0, 0},                 /* formatVersion */
        {4, 0, 0, 0}                  /* dataVersion */
    };

    const char* name = "test";
    const char* type="icu";

    const char *testPath = loadTestData(&status);

    status = U_ILLEGAL_ARGUMENT_ERROR;
    /*Try udata_open with status != U_ZERO_ERROR*/
    log_verbose("Testing udata_open() with status != U_ZERO_ERROR\n");
    result=udata_open(testPath, type, name, &status);
    if(result != NULL){
        log_err("FAIL: udata_open() is supposed to fail for path = %s, name=%s, type=%s, \n errorcode !=U_ZERO_ERROR\n", testPath, name, type);
        udata_close(result);

    } else {
        log_verbose("PASS: udata_open with errorCode != U_ZERO_ERROR failed as expected\n");
    }

    /*Try udata_open with data name=NULL*/
    log_verbose("Testing udata_open() with data name=NULL\n");
    status=U_ZERO_ERROR;
    result=udata_open(testPath, type, NULL, &status);
    if(U_FAILURE(status)){
        if(status != U_ILLEGAL_ARGUMENT_ERROR || result != NULL){
            log_err("FAIL: udata_open() with name=NULL should return NULL and errocode U_ILLEGAL_ARGUMENT_ERROR, GOT: errorcode=%s\n", myErrorName(status));
        }else{
            log_verbose("PASS: udata_open with name=NULL failed as expected and errorcode = %s as expected\n", myErrorName(status));
        }
    }else{
        log_err("FAIL: udata_open() with data name=NULL is supposed to fail for path = %s, name=NULL type=%s errorcode=U_ZERO_ERROR \n", testPath, type);
        udata_close(result);
    }


    /*Try udata_openChoice with status != U_ZERO_ERROR*/
    log_verbose("Testing udata_openChoice() with status != U_ZERO_ERROR\n");
    status=U_ILLEGAL_ARGUMENT_ERROR;
    result=udata_openChoice(testPath, type, name, isAcceptable3, NULL, &status);
    if(result != NULL){
        log_err("FAIL: udata_openChoice() is supposed to fail for path = %s, name=%s, type=%s, \n errorcode != U_ZERO_ERROR\n", testPath, name, type);
        udata_close(result);
    } else {
        log_verbose("PASS: udata_openChoice() with errorCode != U_ZERO_ERROR failed as expected\n");
    }

    /*Try udata_open with data name=NULL*/
    log_verbose("Testing udata_openChoice() with data name=NULL\n");
    status=U_ZERO_ERROR;
    result=udata_openChoice(testPath, type, NULL, isAcceptable3, NULL, &status);
    if(U_FAILURE(status)){
        if(status != U_ILLEGAL_ARGUMENT_ERROR || result != NULL){
            log_err("FAIL: udata_openChoice() with name=NULL should return NULL and errocode U_ILLEGAL_ARGUMENT_ERROR, GOT: errorcode=%s\n", myErrorName(status));
        }else{
            log_verbose("PASS: udata_openChoice with name=NULL failed as expected and errorcode = %s as expected\n", myErrorName(status));
        }    
    }else{
        log_err("FAIL: udata_openChoice() with data name=NULL is supposed to fail for path = %s, name=NULL type=%s errorcode=U_ZERO_ERROR \n", testPath, type);
        udata_close(result);
    }

    /*Try udata_getMemory with UDataMemory=NULL*/
    log_verbose("Testing udata_getMemory with UDataMemory=NULL\n");
    intValue=(uint16_t*)udata_getMemory(NULL);
    if(intValue != NULL){
        log_err("FAIL: udata_getMemory with UDataMemory = NULL is supposed to fail\n");
    }

    /*Try udata_getInfo with UDataMemory=NULL*/
    status=U_ZERO_ERROR;
    udata_getInfo(NULL, &dataInfo);
    if(dataInfo.size != 0){
        log_err("FAIL : udata_getInfo with UDataMemory = NULL us supposed to fail\n");
    }

    /*Try udata_openChoice with a non existing binary file*/
    log_verbose("Testing udata_openChoice() with a non existing binary file\n");
    result=udata_openChoice(testPath, "tst", "nonexist", isAcceptable3, NULL, &status);
    if(status==U_FILE_ACCESS_ERROR){
        log_verbose("Opening udata_openChoice with non-existing file handled correctly.\n");
        status=U_ZERO_ERROR;
    } else {
        log_err("calling udata_open with non-existing file not handled correctly\n.  Expected: U_FILE_ACCESS_ERROR, Got: %s\n", myErrorName(status));
        if(U_SUCCESS(status)) {
            udata_close(result);
        }
    }

    if(result != NULL){
        log_err("calling udata_open with non-existing file didn't return a null value\n");
    } else {
        log_verbose("calling udat_open with non-existing file returned null as expected\n");
    }
}

/* Test whether apps and ICU can each have their own root.res */
static void TestAppData()
{
  UResourceBundle *icu, *app;
  UResourceBundle *tmp = NULL;
  UResourceBundle *tmp2 = NULL;
  
  const UChar *appString;
  const UChar *icuString;

  int32_t len;

  UErrorCode status = U_ZERO_ERROR;
  char testMsgBuf[256];

  const char* testPath=loadTestData(&status);

  icu = ures_open(NULL, "root", &status);
  if(U_FAILURE(status))
  { 
     log_err("%s:%d: Couldn't open root ICU bundle- %s", __FILE__, __LINE__, u_errorName(status));
     return;
  }
  /*  log_info("Open icu root: %s size_%d\n", u_errorName(status), ures_getSize(icu)); */
  status = U_ZERO_ERROR;
  
  app = ures_open(testPath, "root", &status);
  if(U_FAILURE(status))
  { 
     log_err("%s:%d: Couldn't open app ICU bundle [%s]- %s", __FILE__, __LINE__, testPath, u_errorName(status));
     return;
  }
  /* log_info("Open  app: %s, size %d\n", u_errorName(status), ures_getSize(app)); */

  tmp = ures_getByKey(icu, "Version", tmp, &status);
  if(U_FAILURE(status))
  { 
     log_err("%s:%d: Couldn't get Version string from ICU root bundle- %s", __FILE__, __LINE__, u_errorName(status));
     return;
  }

  icuString =  ures_getString(tmp,  &len, &status);
  if(U_FAILURE(status))
  { 
     log_err("%s:%d: Couldn't get string from Version string from ICU root bundle- %s", __FILE__, __LINE__, u_errorName(status));
     return;
  }
  /* log_info("icuString=%p - %s\n", icuString, austrdup(icuString)); */


  tmp2 = ures_getByKey(app, "Version", tmp2, &status);
  if(U_FAILURE(status))
  { 
    log_err("%s:%d: Couldn't get Version string from App root bundle- %s", __FILE__, __LINE__, u_errorName(status));
     return;
  }

  appString =  ures_getString(tmp2,  &len, &status);
  if(U_FAILURE(status))
  { 
     log_err("%s:%d: Couldn't get string from Version string from App root bundle- %s", __FILE__, __LINE__, u_errorName(status));
     return;
  }

  /* log_info("appString=%p - %s\n", appString, austrdup(appString)); */


  if(!u_strcmp(icuString, appString))
  {
    log_err("%s:%d: Error! Expected ICU and App root version strings to be DIFFERENT but they are both %s and %s\n", __FILE__, __LINE__, austrdup(icuString),
    austrdup(appString));
  }
  else
  {
    log_verbose("%s:%d:  appstr=%s, icustr=%s\n", __FILE__,
      __LINE__, u_austrcpy(testMsgBuf, appString), u_austrcpy(testMsgBuf, icuString));
  }

  ures_close(tmp);
  ures_close(tmp2);
  ures_close(icu);
  ures_close(app);
}

static void TestICUDataName()
{
    UVersionInfo icuVersion;
    char expectDataName[20];
    unsigned int expectLen = 8;

    char typeChar  = '?';

    /* Print out the version # we have .. */
    log_verbose("utypes.h says U_ICUDATA_NAME = %s\n", U_ICUDATA_NAME);

    /* Build up the version # we expect to get */
    u_getVersion(icuVersion);

    switch(U_CHARSET_FAMILY)
    {
    case U_ASCII_FAMILY:
          switch(U_IS_BIG_ENDIAN)
          {
          case 1:
                typeChar = 'b';
                break;
          case 0:
                typeChar = 'l';
                break;
          default:
                log_err("Expected 1 or 0 for U_IS_BIG_ENDIAN, got %d!\n", (int)U_IS_BIG_ENDIAN);
                /* return; */
          }
          break;
    case U_EBCDIC_FAMILY:
        typeChar = 'e';
        break;
    }

    sprintf(expectDataName, "%s%d%d%c",
                "icudt",
                (int)icuVersion[0],
                (int)icuVersion[1],
                typeChar);

    log_verbose("Expected: %s\n", expectDataName);
    if(uprv_strlen(expectDataName) != expectLen)
    {
        log_err("*Expected* length is wrong (test err?), should be %d is %d\n",
            expectLen, uprv_strlen(expectDataName));
    }

    if(uprv_strlen(U_ICUDATA_NAME) != expectLen)
    {
        log_err("U_ICUDATA_NAME length should be %d is %d\n",
            expectLen, uprv_strlen(U_ICUDATA_NAME));
    }
    
    if(uprv_strcmp(U_ICUDATA_NAME, expectDataName))
    {
        log_err("U_ICUDATA_NAME should be %s but is %s\n",
                expectDataName, U_ICUDATA_NAME);
    }

        /* ICUDATA_NAME comes from the build system on *nix */
#ifdef ICUDATA_NAME
    if(uprv_strcmp(U_ICUDATA_NAME, ICUDATA_NAME))
    {
        log_err("ICUDATA_NAME  and U_ICUDATA_NAME don't match: "
            "ICUDATA_NAME=%s, U_ICUDATA_NAME=%s.  Check configure.in, icudefs.mk.in, utypes.h...\n",  ICUDATA_NAME, U_ICUDATA_NAME);
    }
    else
    {
        log_verbose("ICUDATA_NAME=%s (from icudefs.mk), U_ICUDATA_NAME=%s (from utypes.h)\n", ICUDATA_NAME, U_ICUDATA_NAME);
    }
#endif

}

/* test data swapping ------------------------------------------------------- */

/* test cases for maximum data swapping code coverage */
static const struct {
    const char *name, *type;
    UDataSwapFn *swapFn;
} swapCases[]={
    /* resource bundles */

    /* resource bundle with many data types */
    {"*testtypes",               "res", ures_swap},
    /* resource bundle with collation data */
    {"ja",                       "res", ures_swap},
    /* resource bundle with options-only collation data */
    {"ru",                       "res", ures_swap},
    {"el",                       "res", ures_swap},
    /* ICU's root */
    {"root",                     "res", ures_swap},

    /* ICU 2.6 resource bundle - data format 1.0, without indexes[] (little-endian ASCII) */
    {"*icu26_testtypes",         "res", ures_swap},
    /* same for big-endian EBCDIC */
    {"*icu26e_testtypes",        "res", ures_swap},

#if !UCONFIG_NO_COLLATION
    /* standalone collation data files */
    {"ucadata",                  "icu", ucol_swap},
    {"invuca",                   "icu", ucol_swapInverseUCA},
#endif

#if !UCONFIG_NO_LEGACY_CONVERSION
    /* conversion table files */

    /* SBCS conversion table file without extension */
    {"ibm-913_P100-2000",        "cnv", ucnv_swap},
    /* EBCDIC_STATEFUL conversion table file with extension */
    {"ibm-1390_P110-2003",       "cnv", ucnv_swap},
    /* DBCS extension-only conversion table file */
    {"ibm-16684_P110-2003",      "cnv", ucnv_swap},
    /* EUC-TW (3-byte) conversion table file without extension */
    {"ibm-964_P110-1999",        "cnv", ucnv_swap},
    /* GB 18030 (4-byte) conversion table file without extension */
    {"gb18030",                  "cnv", ucnv_swap},
    /* MBCS conversion table file with extension */
    {"*test4x",                  "cnv", ucnv_swap},

    /* alias table */
    {"cnvalias",                 "icu", ucnv_swapAliases},
#endif

#if !UCONFIG_NO_IDNA
    {"uidna",                    "spp", usprep_swap},
#endif

#if !UCONFIG_NO_BREAK_ITERATION
    {"char",                     "brk", ubrk_swap},
#endif

    /* the last item should not be #if'ed so that it can reliably omit the last comma */

    /* Unicode properties */
    {"unames",                   "icu", uchar_swapNames},
    {"pnames",                   "icu", upname_swap},
#if !UCONFIG_NO_NORMALIZATION
    {"unorm",                    "icu", unorm_swap},
#endif
    {"uprops",                   "icu", uprops_swap}
};

#define SWAP_BUFFER_SIZE 1000000

static void U_CALLCONV
printError(void *context, const char *fmt, va_list args) {
    vlog_info("[swap] ", fmt, args);
}

static void
TestSwapCase(UDataMemory *pData, const char *name,
             UDataSwapFn *swapFn,
             uint8_t *buffer, uint8_t *buffer2) {
    UDataSwapper *ds;
    const void *inData, *inHeader;
    int32_t length, dataLength, length2, headerLength;

    UErrorCode errorCode;

    UBool inEndian, oppositeEndian;
    uint8_t inCharset, oppositeCharset;

    inData=udata_getMemory(pData);

    /*
     * get the data length if possible, to verify that swapping and preflighting
     * handles the entire data
     */
    dataLength=udata_getLength(pData);

    /*
     * get the header and its length
     * all of the swap implementation functions require the header to be included
     */
    inHeader=udata_getRawMemory(pData);
    headerLength=(int32_t)((const char *)inData-(const char *)inHeader);

    /* first swap to opposite endianness but same charset family */
    errorCode=U_ZERO_ERROR;
    ds=udata_openSwapperForInputData(inHeader, headerLength,
            !U_IS_BIG_ENDIAN, U_CHARSET_FAMILY, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("udata_openSwapperForInputData(%s->!isBig+same charset) failed - %s\n",
                name, u_errorName(errorCode));
        return;
    }

    inEndian=ds->inIsBigEndian;
    inCharset=ds->inCharset;

    oppositeEndian=!inEndian;
    oppositeCharset= inCharset==U_ASCII_FAMILY ? U_EBCDIC_FAMILY : U_ASCII_FAMILY;

    /* make this test work with data files that are built for a different platform */
    if(inEndian!=U_IS_BIG_ENDIAN || inCharset!=U_CHARSET_FAMILY) {
        udata_closeSwapper(ds);
        ds=udata_openSwapper(inEndian, inCharset, oppositeEndian, inCharset, &errorCode);
        if(U_FAILURE(errorCode)) {
            log_err("udata_openSwapper(%s->!isBig+same charset) failed - %s\n",
                    name, u_errorName(errorCode));
            return;
        }
    }

    ds->printError=printError;

    /* preflight the length */
    length=swapFn(ds, inHeader, -1, NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("swapFn(preflight %s->!isBig+same charset) failed - %s\n",
                name, u_errorName(errorCode));
        udata_closeSwapper(ds);
        return;
    }

    /* compare the preflighted length against the data length */
    if(dataLength>=0 && (length+15)<(headerLength+dataLength)) {
        log_err("swapFn(preflight %s->!isBig+same charset) length too small: %d < data length %d\n",
                name, length, (headerLength+dataLength));
        udata_closeSwapper(ds);
        return;
    }

    /* swap, not in-place */
    length2=swapFn(ds, inHeader, length, buffer, &errorCode);
    udata_closeSwapper(ds);
    if(U_FAILURE(errorCode)) {
        log_err("swapFn(%s->!isBig+same charset) failed - %s\n",
                name, u_errorName(errorCode));
        return;
    }

    /* compare the swap length against the preflighted length */
    if(length2!=length) {
        log_err("swapFn(%s->!isBig+same charset) length differs from preflighting: %d != preflighted %d\n",
                name, length2, length);
        return;
    }

    /* next swap to opposite charset family */
    ds=udata_openSwapper(oppositeEndian, inCharset,
                         oppositeEndian, oppositeCharset,
                         &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("udata_openSwapper(%s->!isBig+other charset) failed - %s\n",
                name, u_errorName(errorCode));
        return;
    }
    ds->printError=printError;

    /* swap in-place */
    length2=swapFn(ds, buffer, length, buffer, &errorCode);
    udata_closeSwapper(ds);
    if(U_FAILURE(errorCode)) {
        log_err("swapFn(%s->!isBig+other charset) failed - %s\n",
                name, u_errorName(errorCode));
        return;
    }

    /* compare the swap length against the original length */
    if(length2!=length) {
        log_err("swapFn(%s->!isBig+other charset) length differs from original: %d != original %d\n",
                name, length2, length);
        return;
    }

    /* finally swap to original platform values */
    ds=udata_openSwapper(oppositeEndian, oppositeCharset,
                         inEndian, inCharset,
                         &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("udata_openSwapper(%s->back to original) failed - %s\n",
                name, u_errorName(errorCode));
        return;
    }
    ds->printError=printError;

    /* swap, not in-place */
    length2=swapFn(ds, buffer, length, buffer2, &errorCode);
    udata_closeSwapper(ds);
    if(U_FAILURE(errorCode)) {
        log_err("swapFn(%s->back to original) failed - %s\n",
                name, u_errorName(errorCode));
        return;
    }

    /* compare the swap length against the original length */
    if(length2!=length) {
        log_err("swapFn(%s->back to original) length differs from original: %d != original %d\n",
                name, length2, length);
        return;
    }

    /* compare the final contents with the original */
    if(0!=uprv_memcmp(inHeader, buffer2, length)) {
        const uint8_t *original;
        uint8_t diff[8];
        int32_t i, j;

        log_err("swapFn(%s->back to original) contents differs from original\n",
                name);

        /* find the first difference */
        original=(const uint8_t *)inHeader;
        for(i=0; i<length && original[i]==buffer2[i]; ++i) {}

        /* find the next byte that is the same */
        for(j=i+1; j<length && original[j]!=buffer2[j]; ++j) {}
        log_info("    difference at index %d=0x%x, until index %d=0x%x\n", i, i, j, j);

        /* round down to the last 4-boundary for better result output */
        i&=~3;
        log_info("showing bytes from index %d=0x%x (length %d=0x%x):\n", i, i, length, length);

        /* print 8 bytes but limit to the buffer contents */
        length2=i+sizeof(diff);
        if(length2>length) {
            length2=length;
        }

        /* print the original bytes */
        uprv_memset(diff, 0, sizeof(diff));
        for(j=i; j<length2; ++j) {
            diff[j-i]=original[j];
        }
        log_info("    original: %02x %02x %02x %02x %02x %02x %02x %02x\n",
            diff[0], diff[1], diff[2], diff[3], diff[4], diff[5], diff[6], diff[7]);

        /* print the swapped bytes */
        uprv_memset(diff, 0, sizeof(diff));
        for(j=i; j<length2; ++j) {
            diff[j-i]=buffer2[j];
        }
        log_info("    swapped:  %02x %02x %02x %02x %02x %02x %02x %02x\n",
            diff[0], diff[1], diff[2], diff[3], diff[4], diff[5], diff[6], diff[7]);
    }
}

static void
TestSwapData() {
    char name[100];
    UDataMemory *pData;
    uint8_t *buffer;
    const char *pkg, *nm;

    UErrorCode errorCode;
    int32_t i;

    buffer=(uint8_t *)uprv_malloc(2*SWAP_BUFFER_SIZE);
    if(buffer==NULL) {
        log_err("unable to allocate %d bytes\n", 2*SWAP_BUFFER_SIZE);
        return;
    }

    for(i=0; i<LENGTHOF(swapCases); ++i) {
        /* build the name for logging */
        errorCode=U_ZERO_ERROR;
        if(swapCases[i].name[0]=='*') {
            pkg=loadTestData(&errorCode);
            nm=swapCases[i].name+1;
            uprv_strcpy(name, "testdata");
        } else {
            pkg=NULL;
            nm=swapCases[i].name;
            uprv_strcpy(name, "NULL");
        }
        uprv_strcat(name, "/");
        uprv_strcat(name, nm);
        uprv_strcat(name, ".");
        uprv_strcat(name, swapCases[i].type);

        pData=udata_open(pkg, swapCases[i].type, nm, &errorCode);
        if(U_SUCCESS(errorCode)) {
            TestSwapCase(pData, name, swapCases[i].swapFn, buffer, buffer+SWAP_BUFFER_SIZE);
            udata_close(pData);
        } else {
            log_data_err("udata_open(%s) failed - %s\n",
                name, u_errorName(errorCode));
        }
    }

    uprv_free(buffer);
}
