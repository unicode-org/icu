/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*******************************************************************************
*
* File CRESTST.C
*
* Modification History:
*        Name                     Description
*     Madhu Katragadda            Ported for C API
*  06/14/99     stephen           Updated for RB API changes (no suffix).
********************************************************************************
*/


#include "unicode/utypes.h"
#include "cintltst.h"
#include "unicode/ustring.h"
#include "cstring.h"
#include "filestrm.h"
#include "cmemory.h"

#define RESTEST_HEAP_CHECK 0

#include "unicode/ures.h"
#include "crestst.h"
#include "unicode/ctest.h"

static void TestOpenDirect(void);
static void TestFallback(void);
static void TestFileStream(void);
/*****************************************************************************/

const UChar kERROR[] = { 0x0045 /*E*/, 0x0052 /*'R'*/, 0x0052 /*'R'*/,
             0x004F /*'O'*/, 0x0052/*'R'*/, 0x0000 /*'\0'*/};

/*****************************************************************************/

enum E_Where
{
  e_Root,
  e_te,
  e_te_IN,
  e_Where_count
};
typedef enum E_Where E_Where;
/*****************************************************************************/

#define CONFIRM_EQ(actual,expected) if (u_strcmp(expected,actual)==0){ record_pass(); } else { record_fail(); log_err("%s  returned  %s  instead of %s\n", action, austrdup(actual), austrdup(expected)); }

#define CONFIRM_ErrorCode(actual,expected) if ((expected)==(actual)) { record_pass(); } else { record_fail();  log_err("%s returned  %s  instead of %s\n", action, myErrorName(actual), myErrorName(expected)); }


/* Array of our test objects */

static struct
{
  const char* name;
  UErrorCode expected_constructor_status;
  E_Where where;
  UBool like[e_Where_count];
  UBool inherits[e_Where_count];
} param[] =
{
  /* "te" means test */
  /* "IN" means inherits */
  /* "NE" or "ne" means "does not exist" */

  { "root",         U_ZERO_ERROR,             e_Root,    { TRUE, FALSE, FALSE }, { TRUE, FALSE, FALSE } },
  { "te",           U_ZERO_ERROR,             e_te,      { FALSE, TRUE, FALSE }, { TRUE, TRUE, FALSE } },
  { "te_IN",        U_ZERO_ERROR,             e_te_IN,   { FALSE, FALSE, TRUE }, { TRUE, TRUE, TRUE } },
  { "te_NE",        U_USING_FALLBACK_WARNING, e_te,      { FALSE, TRUE, FALSE }, { TRUE, TRUE, FALSE } },
  { "te_IN_NE",     U_USING_FALLBACK_WARNING, e_te_IN,   { FALSE, FALSE, TRUE }, { TRUE, TRUE, TRUE } },
  { "ne",           U_USING_DEFAULT_WARNING,  e_Root,    { TRUE, FALSE, FALSE }, { TRUE, FALSE, FALSE } }
};

static int32_t bundles_count = sizeof(param) / sizeof(param[0]);



/***************************************************************************************/

/* Array of our test objects */

void addResourceBundleTest(TestNode** root);

void addResourceBundleTest(TestNode** root)
{
    addTest(root, &TestConstruction1, "tsutil/crestst/TestConstruction1");
    addTest(root, &TestOpenDirect, "tsutil/crestst/TestOpenDirect");
    addTest(root, &TestResourceBundles, "tsutil/crestst/TestResourceBundle");
    addTest(root, &TestFallback, "tsutil/crestst/TestFallback");
    addTest(root, &TestAliasConflict, "tsutil/crestst/TestAliasConflict");
    addTest(root, &TestFileStream, "tsutil/crestst/TestFileStream");
}


/***************************************************************************************/
void TestAliasConflict(void) {
    UErrorCode status = U_ZERO_ERROR;
    UResourceBundle *he = NULL;
    UResourceBundle *iw = NULL;
    const UChar *result = NULL;
    int32_t resultLen;

    he = ures_open(NULL, "he", &status);
    iw = ures_open(NULL, "iw", &status);
    if(U_FAILURE(status)) {
        log_err("Failed to get resource with %s\n", myErrorName(status));
    }
    ures_close(iw);
    result = ures_getStringByKey(he, "localPatternChars", &resultLen, &status);
    if(U_FAILURE(status) || result == NULL) {
        log_err("Failed to get resource with %s\n", myErrorName(status));
    }
    ures_close(he);
}


void TestResourceBundles()
{
    testTag("only_in_Root", TRUE, FALSE, FALSE);
    testTag("in_Root_te", TRUE, TRUE, FALSE);
    testTag("in_Root_te_te_IN", TRUE, TRUE, TRUE);
    testTag("in_Root_te_IN", TRUE, FALSE, TRUE);
    testTag("only_in_te", FALSE, TRUE, FALSE);
    testTag("only_in_te_IN", FALSE, FALSE, TRUE);
    testTag("in_te_te_IN", FALSE, TRUE, TRUE);
    testTag("nonexistent", FALSE, FALSE, FALSE);

    log_verbose("Passed:=  %d   Failed=   %d \n", pass, fail);
}

void TestConstruction1()
{
    UResourceBundle *test1 = 0, *test2 = 0;
    const UChar *result1, *result2;
    int32_t resultLen;
    UChar temp[7];

    UErrorCode   err = U_ZERO_ERROR;
    const char* testdatapath ;
    const char*      locale="te_IN";

    log_verbose("Testing ures_open()......\n");


    testdatapath=loadTestData(&err);
    if(U_FAILURE(err))
    {
        log_err("Could not load testdata.dat %s \n",myErrorName(err));
        return;
    }

    test1=ures_open(testdatapath, NULL, &err);
    if(U_FAILURE(err))
    {
        log_err("construction of %s did not succeed :  %s \n",NULL, myErrorName(err));
        return;
    }


    test2=ures_open(testdatapath, locale, &err);
    if(U_FAILURE(err))
    {
        log_err("construction of %s did not succeed :  %s \n",locale, myErrorName(err));
        return;
    }
    result1= ures_getStringByKey(test1, "string_in_Root_te_te_IN", &resultLen, &err);
    result2= ures_getStringByKey(test2, "string_in_Root_te_te_IN", &resultLen, &err);


    if (U_FAILURE(err)) {
        log_err("Something threw an error in TestConstruction(): %s\n", myErrorName(err));
        return;
    }

    u_uastrcpy(temp, "TE_IN");

    if(u_strcmp(result2, temp)!=0)
    {
        int n;

        log_err("Construction test failed for ures_open();\n");
        if(!VERBOSITY)
            log_info("(run verbose for more information)\n");

        log_verbose("\nGot->");
        for(n=0;result2[n];n++)
        {
            log_verbose("%04X ",result2[n]);
        }
        log_verbose("<\n");

        log_verbose("\nWant>");
        for(n=0;temp[n];n++)
        {
            log_verbose("%04X ",temp[n]);
        }
        log_verbose("<\n");

    }

    log_verbose("for string_in_Root_te_te_IN, default.txt had  %s\n", austrdup(result1));
    log_verbose("for string_in_Root_te_te_IN, te_IN.txt had %s\n", austrdup(result2));

    /* Test getVersionNumber*/
    log_verbose("Testing version number\n");
    log_verbose("for getVersionNumber :  %s\n", ures_getVersionNumber(test1));

    ures_close(test1);
    ures_close(test2);
}

/*****************************************************************************/
/*****************************************************************************/

UBool testTag(const char* frag,
           UBool in_Root,
           UBool in_te,
           UBool in_te_IN)
{
    int32_t passNum=pass;

    /* Make array from input params */

    UBool is_in[3];
    const char *NAME[] = { "ROOT", "TE", "TE_IN" };

    /* Now try to load the desired items */
    UResourceBundle* theBundle = NULL;
    char tag[99];
    char action[256];
    UErrorCode status = U_ZERO_ERROR,expected_resource_status = U_ZERO_ERROR;
    UChar* base = NULL;
    UChar* expected_string = NULL;
    const UChar* string = NULL;
    char item_tag[10];
    int32_t i,j;
    int32_t actual_bundle;
    int32_t resultLen;
    const char *testdatapath = loadTestData(&status);

    is_in[0] = in_Root;
    is_in[1] = in_te;
    is_in[2] = in_te_IN;

    strcpy(item_tag, "tag");

    status = U_ZERO_ERROR;
    theBundle = ures_open(testdatapath, "root", &status);
    if(U_FAILURE(status))
    {
        ures_close(theBundle);
        log_err("Couldn't open root bundle in %s", testdatapath);
        return FALSE;
    }
    ures_close(theBundle);
    theBundle = NULL;


    for (i=0; i<bundles_count; ++i)
    {
        strcpy(action,"construction for");
        strcat(action, param[i].name);


        status = U_ZERO_ERROR;

        theBundle = ures_open(testdatapath, param[i].name, &status);
        /*theBundle = ures_open("c:\\icu\\icu\\source\\test\\testdata\\testdata", param[i].name, &status);*/

        CONFIRM_ErrorCode(status,param[i].expected_constructor_status);



        if(i == 5)
            actual_bundle = 0; /* ne -> default */
        else if(i == 3)
            actual_bundle = 1; /* te_NE -> te */
        else if(i == 4)
            actual_bundle = 2; /* te_IN_NE -> te_IN */
        else
            actual_bundle = i;

        expected_resource_status = U_MISSING_RESOURCE_ERROR;
        for (j=e_te_IN; j>=e_Root; --j)
        {
            if (is_in[j] && param[i].inherits[j])
            {

                if(j == actual_bundle) /* it's in the same bundle OR it's a nonexistent=default bundle (5) */
                    expected_resource_status = U_ZERO_ERROR;
                else if(j == 0)
                    expected_resource_status = U_USING_DEFAULT_WARNING;
                else
                    expected_resource_status = U_USING_FALLBACK_WARNING;

                log_verbose("%s[%d]::%s: in<%d:%s> inherits<%d:%s>.  actual_bundle=%s\n",
                            param[i].name, 
                            i,
                            frag,
                            j,
                            is_in[j]?"Yes":"No",
                            j,
                            param[i].inherits[j]?"Yes":"No",
                            param[actual_bundle].name);

                break;
            }
        }

        for (j=param[i].where; j>=0; --j)
        {
            if (is_in[j])
            {
                if(base != NULL) {
                    free(base);
                    base = NULL;
                }

                base=(UChar*)malloc(sizeof(UChar)*(strlen(NAME[j]) + 1));
                u_uastrcpy(base,NAME[j]);

                break;
            }
            else {
                if(base != NULL) {
                    free(base);
                    base = NULL;
                }
                base = (UChar*) malloc(sizeof(UChar) * 1);
                *base = 0x0000;
            }
        }

        /*-------------------------------------------------------------------- */
        /* string */

        strcpy(tag,"string_");
        strcat(tag,frag);

        strcpy(action,param[i].name);
        strcat(action, ".ures_get(" );
        strcat(action,tag);
        strcat(action, ")");

        string=    kERROR;

        status = U_ZERO_ERROR;

        ures_getStringByKey(theBundle, tag, &resultLen, &status);
        if(U_SUCCESS(status))
        {
            status = U_ZERO_ERROR;
            string=ures_getStringByKey(theBundle, tag, &resultLen, &status);
        }

        log_verbose("%s got %d, expected %d\n", action, status, expected_resource_status);

        CONFIRM_ErrorCode(status, expected_resource_status);


        if(U_SUCCESS(status)){
            expected_string=(UChar*)malloc(sizeof(UChar)*(u_strlen(base) + 3));
            u_strcpy(expected_string,base);

        }
        else
        {
            expected_string = (UChar*)malloc(sizeof(UChar)*(u_strlen(kERROR) + 1));
            u_strcpy(expected_string,kERROR);

        }

        CONFIRM_EQ(string, expected_string);

        free(expected_string);
        ures_close(theBundle);
    }
    free(base);
    return (UBool)(passNum == pass);
}

void record_pass()
{
  ++pass;
}

void record_fail()
{
  ++fail;
}

/**
 * Test to make sure that the U_USING_FALLBACK_ERROR and U_USING_DEFAULT_ERROR
 * are set correctly
 */

static void TestFallback()
{
    UErrorCode status = U_ZERO_ERROR;
    UResourceBundle *fr_FR = NULL;
    const UChar *junk; /* ignored */
    int32_t resultLen;

    log_verbose("Opening fr_FR..");
    fr_FR = ures_open(NULL, "fr_FR", &status);
    if(U_FAILURE(status))
    {
        log_err("Couldn't open fr_FR - %d\n", status);
        return;
    }

    status = U_ZERO_ERROR;


    /* clear it out..  just do some calls to get the gears turning */
    junk = ures_getStringByKey(fr_FR, "LocaleID", &resultLen, &status);
    status = U_ZERO_ERROR;
    junk = ures_getStringByKey(fr_FR, "LocaleString", &resultLen, &status);
    status = U_ZERO_ERROR;
    junk = ures_getStringByKey(fr_FR, "LocaleID", &resultLen, &status);
    status = U_ZERO_ERROR;

    /* OK first one. This should be a Default value. */
    junk = ures_getStringByKey(fr_FR, "%%PREEURO", &resultLen, &status);
    if(status != U_USING_DEFAULT_WARNING)
    {
        log_data_err("Expected U_USING_DEFAULT_ERROR when trying to get %%PREEURO from fr_FR, got %s\n",
            u_errorName(status));
    }

    status = U_ZERO_ERROR;

    /* and this is a Fallback, to fr */
    junk = ures_getStringByKey(fr_FR, "DayNames", &resultLen, &status);
    if(status != U_USING_FALLBACK_WARNING)
    {
        log_data_err("Expected U_USING_FALLBACK_ERROR when trying to get DayNames from fr_FR, got %s\n", 
            u_errorName(status));
    }

    status = U_ZERO_ERROR;

    ures_close(fr_FR);
}

static void
TestOpenDirect(void) {
    UResourceBundle *translit_index, *item;
    UErrorCode errorCode;

    /*
     * test that ures_openDirect() opens a resource bundle
     * where one can look up its own items but not fallback items
     * from root or similar
     */
    errorCode=U_ZERO_ERROR;
    translit_index=ures_openDirect(NULL, "translit_index", &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("ures_openDirect(\"translit_index\") failed: %s\n", u_errorName(errorCode));
        return;
    }

    if(0!=uprv_strcmp("translit_index", ures_getLocale(translit_index, &errorCode))) {
        log_err("ures_openDirect(\"translit_index\").getLocale()!=translit_index\n");
    }
    errorCode=U_ZERO_ERROR;

    /* try an item in translit_index, must work */
    item=ures_getByKey(translit_index, "RuleBasedTransliteratorIDs", NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        log_err("translit_index.getByKey(local key) failed: %s\n", u_errorName(errorCode));
        errorCode=U_ZERO_ERROR;
    } else {
        ures_close(item);
    }

    /* try an item in root, must fail */
    item=ures_getByKey(translit_index, "Languages", NULL, &errorCode);
    if(U_FAILURE(errorCode)) {
        errorCode=U_ZERO_ERROR;
    } else {
        log_err("translit_index.getByKey(root key) succeeded!\n");
        ures_close(item);
    }
    ures_close(translit_index);

    /* now make sure that "translit_index" will not work with ures_open() */
    errorCode=U_ZERO_ERROR;
    translit_index=ures_open(NULL, "translit_index", &errorCode);
    if(U_FAILURE(errorCode) || errorCode==U_USING_DEFAULT_WARNING || errorCode==U_USING_FALLBACK_WARNING) {
        /* falling back to default or root is ok */
        errorCode=U_ZERO_ERROR;
    } else if(0!=uprv_strcmp("translit_INDEX", ures_getLocale(translit_index, &errorCode))) {
        /* Opening this file will work in "files mode" on Windows and the Mac,
           which have case insensitive file systems */
        log_err("ures_open(\"translit_index\") succeeded, should fail! Got: %s\n", u_errorName(errorCode));
    }
    ures_close(translit_index);

    /* ures_openDirect("translit_index_WronG") must fail */
    translit_index=ures_openDirect(NULL, "translit_index_WronG", &errorCode);
    if(U_FAILURE(errorCode)) {
        errorCode=U_ZERO_ERROR;
    } else {
        log_err("ures_openDirect(\"translit_index_WronG\") succeeded, should fail!\n");
    }
    ures_close(translit_index);
}

static void TestFileStream(void){
    int32_t c = 0;
    int32_t c1=0;
    UErrorCode status = U_ZERO_ERROR;
    const char* testdatapath = loadTestData(&status);
    char* fileName = (char*) uprv_malloc(uprv_strlen(testdatapath) +10);
    FileStream* stream = NULL;
    /* these should not be closed */
    FileStream* pStdin  = T_FileStream_stdin();
    FileStream* pStdout = T_FileStream_stdout();
    FileStream* pStderr = T_FileStream_stderr();

    const char* testline = "This is a test line";
    int32_t bufLen =uprv_strlen(testline)+10;
    char* buf = (char*) uprv_malloc(bufLen);
    int32_t retLen = 0;

    if(pStdin==NULL){
        log_err("failed to get T_FileStream_stdin()");
    }
    if(pStdout==NULL){
        log_err("failed to get T_FileStream_stdout()");
    }
    if(pStderr==NULL){
        log_err("failed to get T_FileStream_stderr()");
    }

    uprv_strcpy(fileName,testdatapath);
    uprv_strcat(fileName,".dat");
    stream = T_FileStream_open(fileName, "r");
    if(stream==NULL){
        log_data_err("T_FileStream_open failed to open %s\n",fileName);
    }
    if(!T_FileStream_file_exists(fileName)){
        log_data_err("T_FileStream_file_exists failed to verify existence of %s \n",fileName);
    }

    retLen=T_FileStream_read(stream,&c,1);
    if(retLen==0){
        log_data_err("T_FileStream_read failed to read from %s \n",fileName);
    }
    retLen=0;
    T_FileStream_rewind(stream);
    T_FileStream_read(stream,&c1,1);
    if(c!=c1){
        log_data_err("T_FileStream_rewind failed to rewind %s \n",fileName);
    }
    T_FileStream_rewind(stream);
    c1 = T_FileStream_peek(stream);
    if(c!=c1){
        log_data_err("T_FileStream_peek failed to peekd %s \n",fileName);
    }
    c = T_FileStream_getc(stream);
    T_FileStream_ungetc(c,stream);
    if(c!= T_FileStream_getc(stream)){
        log_data_err("T_FileStream_ungetc failed to d %s \n",fileName);
    }

    if(T_FileStream_size(stream)<=0){
        log_data_err("T_FileStream_size failed to d %s \n",fileName);
    }
    if(T_FileStream_error(stream)){
        log_data_err("T_FileStream_error shouldn't have an error %s\n",fileName);
    }
    if(!T_FileStream_error(NULL)){
        log_err("T_FileStream_error didn't get an error %s\n",fileName);
    }
    T_FileStream_putc(stream, 0x20);
    if(!T_FileStream_error(stream)){
        /*
         Warning 
         writing to a read-only file may not consistently fail on all platforms
         (e.g. HP-UX, FreeBSD, MacOSX)
        */
        log_verbose("T_FileStream_error didn't get an error when writing to a readonly file %s\n",fileName);
    }

    T_FileStream_close(stream);
    /* test writing function */
    stream=NULL;
    uprv_strcpy(fileName,testdatapath);
    uprv_strcat(fileName,".tmp");
    stream = T_FileStream_open(fileName,"w+");

    if(stream == NULL){
        log_data_err("Could not open %s for writing\n",fileName);
    }
    c= '$';
    T_FileStream_putc(stream,c);
    T_FileStream_rewind(stream);
    if(c != T_FileStream_getc(stream)){
        log_data_err("T_FileStream_putc failed %s\n",fileName);
    }

    T_FileStream_rewind(stream);
    T_FileStream_writeLine(stream,testline);
    T_FileStream_rewind(stream);
    T_FileStream_readLine(stream,buf,bufLen);
    if(uprv_strncmp(testline, buf,uprv_strlen(buf))!=0){
        log_data_err("T_FileStream_writeLine failed %s\n",fileName);
    }

    T_FileStream_rewind(stream);
    T_FileStream_write(stream,testline,uprv_strlen(testline));
    T_FileStream_rewind(stream);
    retLen = T_FileStream_read(stream, buf, bufLen);
    if(uprv_strncmp(testline, buf,retLen)!=0){
        log_data_err("T_FileStream_write failed %s\n",fileName);
    }

    T_FileStream_close(stream);

    if(!T_FileStream_remove(fileName)){
        log_data_err("T_FileStream_remove failed to delete %s\n",fileName);
    }


    uprv_free(fileName);
    uprv_free(buf);

}
