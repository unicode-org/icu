/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2003, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/********************************************************************************
*
* File CINTLTST.C
*
* Modification History:
*        Name                     Description
*     Madhu Katragadda               Creation
*********************************************************************************
*/

/*The main root for C API tests*/

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"

#include "cintltst.h"
#include "umutex.h"

#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "unicode/ucnv.h"
#include "unicode/ures.h"
#include "unicode/uclean.h"
#include "unicode/ucal.h"

#ifdef XP_MAC_CONSOLE
#   include <console.h>
#endif

#define CTST_LEAK_CHECK 1
#ifdef CTST_LEAK_CHECK
U_CFUNC void ctst_freeAll(void);
U_CFUNC void ctst_init(void);
#endif

static char* _testDataPath=NULL;

/*
 *  Forward Declarations
 */
void ctest_setICU_DATA(void);

static UBool gMutexInitialized = FALSE;

static void TestMutex(void) {
    if (!gMutexInitialized) {
        log_verbose("*** Failure! The global mutex was not initialized.\n"
                "*** Make sure the right linker was used.\n");
    }
}

U_CFUNC void addSetup(TestNode** root);

void addSetup(TestNode** root)
{
    addTest(root, &TestMutex,    "setup/TestMutex");
}

#if UCONFIG_NO_LEGACY_CONVERSION
#   define TRY_CNV_1 "iso-8859-1"
#   define TRY_CNV_2 "ibm-1208"
#else
#   define TRY_CNV_1 "iso-8859-7"
#   define TRY_CNV_2 "sjis"
#endif

int main(int argc, const char* const argv[])
{
    int nerrors = 0;
    int warnOnMissingData = 0;
    int i, j;
    TestNode *root;
    const char *warnOrErr = "Failure"; 
#if !UCONFIG_NO_FORMATTING
    const char *zone = "America/Los_Angeles";
#endif
    const char** argv2;

    /* initial check for the default converter */
    UErrorCode errorCode = U_ZERO_ERROR;
    UResourceBundle *rb;
    UConverter *cnv;

    /* This must be tested before using anything! */
    gMutexInitialized = umtx_isInitialized(NULL);

    argv2 = (const char**) malloc(sizeof(char*) * argc);
    if (argv2 == NULL) {
      printf("*** Error: Out of memory (too many cmd line args?)\n");
      return 1;
    }
    argv2[0] = argv[0];

    /* Checkargs */
    for(i=1,j=1;i<argc;i++) {
        argv2[j++] = argv[i];
        if(!strcmp(argv[i],"-w")) {
            warnOnMissingData = 1;
            warnOrErr = "Warning";
        }
#if !UCONFIG_NO_FORMATTING
        else if (strcmp( argv[i], "-tz") == 0) {
          zone = 0;
          if ((i+1) < argc) {
            switch (argv[i+1][0]) {
            case 0:
              ++i; /* consume empty string in {-tz ""} */
              break;
            case '-':
            case '/':
              break; /* don't process next arg if it is -x or /x */
            default:
              zone = argv[++i]; /* next arg is zone */
              break;
            }
          }
          /* Consume args so processArgs doesn't see them (below).
           * This is ugly but it will get fixed when we do the reorg
           * of arg processing...later.  We can't do all this in
           * processArgs because that library (ctestfw) doesn't link
           * common nor i18n, by design. */
          --j;
        }
#endif
    }
    argc = j;

    while (REPEAT_TESTS > 0) {

#ifdef CTST_LEAK_CHECK
        ctst_init();
#endif
        /* try opening the data from dll instead of the dat file */
        cnv = ucnv_open(TRY_CNV_1, &errorCode);
        if(cnv != 0) {
            /* ok */
            ucnv_close(cnv);
        } else {
            fprintf(stderr,
                    "#### WARNING! The converter for " TRY_CNV_1 " cannot be loaded from data dll/so."
                    "Proceeding to load data from dat file.\n");
            errorCode = U_ZERO_ERROR;

            ctest_setICU_DATA();
        }

        /* If no ICU_DATA environment was set, try to fake up one. */
        /* fprintf(stderr, "u_getDataDirectory() = %s\n", u_getDataDirectory()); */

#ifdef XP_MAC_CONSOLE
        argc = ccommand((char***)&argv);
#endif

        cnv  = ucnv_open(NULL, &errorCode);
        if(cnv != NULL) {
            /* ok */
            ucnv_close(cnv);
        } else {
            fprintf(stderr,
                "*** %s! The default converter cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and \n"
                "*** check that the data files are present.\n", warnOrErr);
            if(warnOnMissingData == 0) {
                fprintf(stderr, "*** Exitting.  Use the '-w' option if data files were\n*** purposely removed, to continue test anyway.\n");
                return 1;
            }
        }

        /* try more data */
        cnv = ucnv_open(TRY_CNV_2, &errorCode);
        if(cnv != 0) {
            /* ok */
            ucnv_close(cnv);
        } else {
            fprintf(stderr,
                    "*** %s! The converter for " TRY_CNV_2 " cannot be opened.\n"
                    "*** Check the ICU_DATA environment variable and \n"
                    "*** check that the data files are present.\n", warnOrErr);
            if(warnOnMissingData == 0) {
                fprintf(stderr, "*** Exitting.  Use the '-w' option if data files were\n*** purposely removed, to continue test anyway.\n");
                return 1;
            }
        }

        rb = ures_open(NULL, "en", &errorCode);
        if(U_SUCCESS(errorCode)) {
            /* ok */
            ures_close(rb);
        } else {
            fprintf(stderr,
                    "*** %s! The \"en\" locale resource bundle cannot be opened.\n"
                    "*** Check the ICU_DATA environment variable and \n"
                    "*** check that the data files are present.\n", warnOrErr);
            if(warnOnMissingData == 0) {
                fprintf(stderr, "*** Exitting.  Use the '-w' option if data files were\n*** purposely removed, to continue test anyway.\n");
                return 1;
            }
        }

        fprintf(stdout, "Default locale for this run is %s\n", uloc_getDefault());
#if !UCONFIG_NO_FORMATTING
        /* Set the default time zone */
        if (zone != 0) {
          UErrorCode ec = U_ZERO_ERROR;
          UChar zoneID[256];
          u_uastrncpy(zoneID, zone, 255);
          zoneID[255] = 0;
          ucal_setDefaultTimeZone(zoneID, &ec);
          if (U_FAILURE(ec)) {
            printf("*** Error: Failed to set default time zone to \"%s\": %s\n",
                      zone, u_errorName(ec));
            u_cleanup();
            return 1;
          }
        }
        fprintf(stdout, "Default time zone for this run is %s\n",
                  (zone!=0) ? zone : "UNSET");
#endif
 
        root = NULL;
        addAllTests(&root);
        nerrors = processArgs(root, argc, argv2);
        if (--REPEAT_TESTS > 0) {
            printf("Repeating tests %d more time(s)\n", REPEAT_TESTS);
        }
        cleanUpTestTree(root);
	free((char **)argv2); /* cast away const to silence the compiler */
#ifdef CTST_LEAK_CHECK
        ctst_freeAll();

        /* To check for leaks */

        u_cleanup(); /* nuke the hashtable.. so that any still-open cnvs are leaked */
#endif
    }

    if (!gMutexInitialized) {
        fprintf(stderr,
            "#### WARNING!\n"
            "  The global mutex was not initialized during C++ static initialization.\n"
            "  You must explicitly initialize ICU by calling u_init() before using ICU in multiple threads.\n"
            "  If you are using ICU in a single threaded application, use of u_init() is recommended,\n"
            "  but is not required.\n"
            "#### WARNING!\n"
            );
    }

    return nerrors ? 1 : 0;
}

/*
static void ctest_appendToDataDirectory(const char *toAppend)
{
    const char *oldPath ="";
    char newBuf [1024];
    char *newPath = newBuf;
    int32_t oldLen;
    int32_t newLen;

    if((toAppend == NULL) || (*toAppend == 0)) {
        return;
    }

    oldPath = u_getDataDirectory();
    if( (oldPath==NULL) || (*oldPath == 0)) {
        u_setDataDirectory(toAppend);
    } else {
        oldLen = strlen(oldPath);
        newLen = strlen(toAppend)+1+oldLen;

        if(newLen > 1022)
        {
            newPath = (char *)ctst_malloc(newLen);
        }

        strcpy(newPath, oldPath);
        strcpy(newPath+oldLen, U_PATH_SEP_STRING);
        strcpy(newPath+oldLen+1, toAppend);

        u_setDataDirectory(newPath);

        if(newPath != newBuf)
        {
            free(newPath);
        }
    }
}
*/

void
ctest_pathnameInContext( char* fullname, int32_t maxsize, const char* relPath )
{
    char mainDirBuffer[1024];
    char* mainDir = NULL;
    const char *dataDirectory = ctest_dataOutDir();
    const char inpSepChar = '|';
    char* tmp;
    int32_t lenMainDir;
    int32_t lenRelPath;

#ifdef XP_MAC
    Str255 volName;
    int16_t volNum;
    OSErr err = GetVol( volName, &volNum );
    if (err != noErr)
        volName[0] = 0;
    mainDir = (char*) &(volName[1]);
    mainDir[volName[0]] = 0;
#else
    if (dataDirectory != NULL) {
        strcpy(mainDirBuffer, dataDirectory);
        strcat(mainDirBuffer, ".." U_FILE_SEP_STRING);
    } else {
        mainDirBuffer[0]='\0';
    }
    mainDir = mainDirBuffer;
#endif

    lenMainDir = (int32_t)strlen(mainDir);
    if(lenMainDir > 0 && mainDir[lenMainDir - 1] != U_FILE_SEP_CHAR) {
        mainDir[lenMainDir++] = U_FILE_SEP_CHAR;
        mainDir[lenMainDir] = 0;
    }

    if (relPath[0] == '|')
        relPath++;
    lenRelPath = (int32_t)strlen(relPath);
    if (maxsize < lenMainDir + lenRelPath + 2) {
        fullname[0] = 0;
        return;
    }
    strcpy(fullname, mainDir);
    /*strcat(fullname, U_FILE_SEP_STRING);*/
    strcat(fullname, relPath);
    strchr(fullname, inpSepChar);
    tmp = strchr(fullname, inpSepChar);
    while (tmp) {
        *tmp = U_FILE_SEP_CHAR;
        tmp = strchr(tmp+1, inpSepChar);
    }
}


/* returns the path to icu/source/data */
const char *  ctest_dataSrcDir()
{
    static const char *dataSrcDir = NULL;

    if(dataSrcDir) {
        return dataSrcDir;
    }

    /* U_TOPSRCDIR is set by the makefiles on UNIXes when building cintltst and intltst
    //              to point to the top of the build hierarchy, which may or
    //              may not be the same as the source directory, depending on
    //              the configure options used.  At any rate,
    //              set the data path to the built data from this directory.
    //              The value is complete with quotes, so it can be used
    //              as-is as a string constant.
    */
#if defined (U_TOPSRCDIR)
    {
        dataSrcDir = U_TOPSRCDIR  U_FILE_SEP_STRING "data" U_FILE_SEP_STRING;
    }
#else

    /* On Windows, the file name obtained from __FILE__ includes a full path.
     *             This file is "wherever\icu\source\test\cintltst\cintltst.c"
     *             Change to    "wherever\icu\source\data"
     */
    {
        static char p[sizeof(__FILE__) + 20];
        char *pBackSlash;
        int i;

        dataSrcDir = p;
        strcpy(p, __FILE__);
        /* We want to back over three '\' chars.                            */
        /*   Only Windows should end up here, so looking for '\' is safe.   */
        for (i=1; i<=3; i++) {
            pBackSlash = strrchr(p, U_FILE_SEP_CHAR);
            if (pBackSlash != NULL) {
                *pBackSlash = 0;        /* Truncate the string at the '\'   */
            }
        }

        if (pBackSlash != NULL) {
            /* We found and truncated three names from the path.
             *  Now append "source\data" and set the environment
             */
            strcpy(pBackSlash, U_FILE_SEP_STRING "data" U_FILE_SEP_STRING );
        }
        else {
            /* __FILE__ on MSVC7 does not contain the directory */
            strcpy(p, ".."U_FILE_SEP_STRING".."U_FILE_SEP_STRING "data" U_FILE_SEP_STRING);
        }
    }
#endif

    return dataSrcDir;

}

/* returns the path to icu/source/data/out */
const char *ctest_dataOutDir()
{
    static const char *dataOutDir = NULL;

    if(dataOutDir) {
        return dataOutDir;
    }

    /* U_TOPBUILDDIR is set by the makefiles on UNIXes when building cintltst and intltst
    //              to point to the top of the build hierarchy, which may or
    //              may not be the same as the source directory, depending on
    //              the configure options used.  At any rate,
    //              set the data path to the built data from this directory.
    //              The value is complete with quotes, so it can be used
    //              as-is as a string constant.
    */
#if defined (U_TOPBUILDDIR)
    {
        dataOutDir = U_TOPBUILDDIR  U_FILE_SEP_STRING "data"U_FILE_SEP_STRING"out"U_FILE_SEP_STRING;
    }
#else

    /* On Windows, the file name obtained from __FILE__ includes a full path.
     *             This file is "wherever\icu\source\test\cintltst\cintltst.c"
     *             Change to    "wherever\icu\source\data"
     */
    {
        static char p[sizeof(__FILE__) + 20];
        char *pBackSlash;
        int i;

        dataOutDir = p;
        strcpy(p, __FILE__);
        /* We want to back over three '\' chars.                            */
        /*   Only Windows should end up here, so looking for '\' is safe.   */
        for (i=1; i<=3; i++) {
            pBackSlash = strrchr(p, U_FILE_SEP_CHAR);
            if (pBackSlash != NULL) {
                *pBackSlash = 0;        /* Truncate the string at the '\'   */
            }
        }

        if (pBackSlash != NULL) {
            /* We found and truncated three names from the path.
             *  Now append "source\data" and set the environment
             */
            strcpy(pBackSlash, U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING);
        }
        else {
            /* __FILE__ on MSVC7 does not contain the directory */
            strcpy(p, ".."U_FILE_SEP_STRING".."U_FILE_SEP_STRING "data" U_FILE_SEP_STRING "out" U_FILE_SEP_STRING);
        }
    }
#endif

    return dataOutDir;
}

/*  ctest_setICU_DATA  - if the ICU_DATA environment variable is not already
 *                       set, try to deduce the directory in which ICU was built,
 *                       and set ICU_DATA to "icu/source/data" in that location.
 *                       The intent is to allow the tests to have a good chance
 *                       of running without requiring that the user manually set
 *                       ICU_DATA.  Common data isn't a problem, since it is
 *                       picked up via a static (build time) reference, but the
 *                       tests dynamically load some data.
 */
void ctest_setICU_DATA() {


    /* No location for the data dir was identifiable.
     *   Add other fallbacks for the test data location here if the need arises
     */
    u_setDataDirectory(ctest_dataOutDir());
}

UChar* CharsToUChars(const char* str) {
    /* Might be faster to just use uprv_strlen() as the preflight len - liu */
    int32_t len = u_unescape(str, 0, 0); /* preflight */
    /* Do NOT use malloc() - we are supposed to be acting like user code! */
    UChar *buf = (UChar*) malloc(sizeof(UChar) * (len + 1));
    u_unescape(str, buf, len + 1);
    return buf;
}

char *austrdup(const UChar* unichars)
{
    int   length;
    char *newString;

    length    = u_strlen ( unichars );
    /*newString = (char*)malloc  ( sizeof( char ) * 4 * ( length + 1 ) );*/ /* this leaks for now */
    newString = (char*)ctst_malloc  ( sizeof( char ) * 4 * ( length + 1 ) ); /* this shouldn't */

    if ( newString == NULL )
        return NULL;

    u_austrcpy ( newString, unichars );

    return newString;
}

char *aescstrdup(const UChar* unichars,int32_t length){
    char *newString,*targetLimit,*target;
    UConverterFromUCallback cb;
    const void *p;
    UErrorCode errorCode = U_ZERO_ERROR;
#if U_CHARSET_FAMILY==U_EBCDIC_FAMILY
#   ifdef OS390
        static const char convName[] = "ibm-1047";
#   else
        static const char convName[] = "ibm-37";
#   endif
#else
    static const char convName[] = "US-ASCII";
#endif
    UConverter* conv = ucnv_open(convName, &errorCode);
    if(length==-1){
        length = u_strlen( unichars);
    }
    newString = (char*)ctst_malloc ( sizeof(char) * 8 * (length +1));
    target = newString;
    targetLimit = newString+sizeof(char) * 8 * (length +1);
    ucnv_setFromUCallBack(conv, UCNV_FROM_U_CALLBACK_ESCAPE, UCNV_ESCAPE_JAVA, &cb, &p, &errorCode);
    ucnv_fromUnicode(conv,&target,targetLimit, &unichars, (UChar*)(unichars+length),NULL,TRUE,&errorCode);
    ucnv_close(conv);
    *target = '\0';
    return newString;
}

const char* loadTestData(UErrorCode* err){
    const char*      directory=NULL;
    UResourceBundle* test =NULL;
    char* tdpath=NULL;
    const char* tdrelativepath = ".."U_FILE_SEP_STRING".."U_FILE_SEP_STRING"test"U_FILE_SEP_STRING"testdata"U_FILE_SEP_STRING"out"U_FILE_SEP_STRING;
    if( _testDataPath == NULL){
        directory= ctest_dataOutDir();

        tdpath = (char*) ctst_malloc(sizeof(char) *(( strlen(directory) * strlen(tdrelativepath)) + 10));


        /* u_getDataDirectory shoul return \source\data ... set the
         * directory to ..\source\data\..\test\testdata\out\testdata
         *
         * Fallback: When Memory mapped file is built
         * ..\source\data\out\..\..\test\testdata\out\testdata
         */
        strcpy(tdpath, directory);
        strcat(tdpath, tdrelativepath);
        strcat(tdpath,"testdata");


        test=ures_open(tdpath, "testtypes", err);

        /* Fall back did not succeed either so return */
        if(U_FAILURE(*err)){
            *err = U_FILE_ACCESS_ERROR;
            log_err("Could not load testtypes.res in testdata bundle with path %s - %s\n", tdpath, u_errorName(*err));
            return "";
        }
        ures_close(test);
        _testDataPath = tdpath;
        return _testDataPath;
    }
    return _testDataPath;
}

#define CTST_MAX_ALLOC 10000
/* Array used as a queue */
static void * ctst_allocated_stuff[CTST_MAX_ALLOC];
static int ctst_allocated = 0;
static UBool ctst_free = FALSE;

void ctst_init(void) {
    int i;
    for(i=0; i<CTST_MAX_ALLOC; i++) {
        ctst_allocated_stuff[i] = NULL;
    }
}

void *ctst_malloc(size_t size) {
    if(ctst_allocated >= CTST_MAX_ALLOC - 1) {
        ctst_allocated = 0;
        ctst_free = TRUE;
    }
    if(ctst_allocated_stuff[ctst_allocated]) {
        free(ctst_allocated_stuff[ctst_allocated]);
    }
    return ctst_allocated_stuff[ctst_allocated++] = malloc(size);
}

#ifdef CTST_LEAK_CHECK
void ctst_freeAll() {
    int i;
    if(ctst_free == 0) {
        for(i=0; i<ctst_allocated; i++) {
            free(ctst_allocated_stuff[i]);
        }
    } else {
        for(i=0; i<CTST_MAX_ALLOC; i++) {
            free(ctst_allocated_stuff[i]);
        }
    }
    _testDataPath=NULL;
}
#endif
