/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-1999, International Business Machines Corporation and
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

#include "cintltst.h"
#include <stdio.h>
#include <string.h>
#include "unicode/uchar.h"
#include "unicode/ustring.h"
#include "unicode/ucnv.h"
#include "unicode/ures.h"

static char* _testDirectory=NULL;
int main ( int argc, const char **argv )
{
    TestNode *root;

    /* initial check for the default converter */
    UErrorCode errorCode = U_ZERO_ERROR;
    UResourceBundle *rb;
    UConverter *cnv = ucnv_open(NULL, &errorCode);
    if(cnv != NULL) {
        /* ok */
        ucnv_close(cnv);
    } else {
        fprintf(stderr,
            "*** Failure! The default converter cannot be opened.\n"
            "*** Check the ICU_DATA environment variable and \n"
            "*** check that the data files are present.\n");
        return 1;
    }

    /* try more data */
    cnv = ucnv_open("iso-8859-7", &errorCode);
    if(cnv != 0) {
        /* ok */
        ucnv_close(cnv);
    } else {
        fprintf(stderr,
                "*** Failure! The converter for iso-8859-7 cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and \n"
                "*** check that the data files are present.\n");
        return 1;
    }

    rb = ures_open(0, "en", &errorCode);
    if(U_SUCCESS(errorCode)) {
        /* ok */
        ures_close(rb);
    } else {
        fprintf(stderr,
                "*** Failure! The \"en\" locale resource bundle cannot be opened.\n"
                "*** Check the ICU_DATA environment variable and \n"
                "*** check that the data files are present.\n");
        return 1;
    }

    root = NULL;
    addAllTests(&root);
    processArgs(root, argc, argv);

    return 0;
}

void 
ctest_pathnameInContext( char* fullname, int32_t maxsize, const char* relPath ) 
{
    char mainDirBuffer[200];
    char* mainDir;
    const char inpSepChar = '|';
    char* tmp;
    int32_t lenMainDir;
    int32_t lenRelPath ;   

#if defined(_WIN32) || defined(WIN32) || defined(__OS2__) || defined(OS2)
        mainDir = getenv("ICU_DATA");
        if(mainDir!=NULL) {
            strcpy(mainDirBuffer, mainDir);
            strcat(mainDirBuffer, "..\\..");
        } else {
            mainDirBuffer[0]='\0';
        }
        mainDir=mainDirBuffer;
#elif defined(XP_MAC)
        Str255 volName;
        int16_t volNum;
        OSErr err = GetVol( volName, &volNum );
        if (err != noErr) volName[0] = 0;
        mainDir = (char*) &(volName[1]);
        mainDir[volName[0]] = 0;
#else
        strcpy(mainDirBuffer, u_getDataDirectory());
        strcat(mainDirBuffer, ".." U_FILE_SEP_STRING);
        mainDir = mainDirBuffer;
#endif

    lenMainDir = strlen( mainDir );
    if(lenMainDir > 0 && mainDir[lenMainDir - 1] != U_FILE_SEP_CHAR) {
        mainDir[lenMainDir++] = U_FILE_SEP_CHAR;
        mainDir[lenMainDir] = 0;
    }

    if (relPath[0] == '|') relPath++;
    lenRelPath = strlen( relPath );
    if (maxsize < lenMainDir + lenRelPath + 2) { fullname[0] = 0; return; }
    strcpy( fullname, mainDir );
    strcat( fullname, U_FILE_SEP_STRING );
    strcat( fullname, relPath );
    strchr( fullname, inpSepChar );
    tmp = strchr(fullname, inpSepChar);
    while (tmp) {
        *tmp = U_FILE_SEP_CHAR;
        tmp = strchr( tmp+1, inpSepChar );
    }
}

const char*
ctest_getTestDirectory()
{
    if (_testDirectory == NULL) 
    {
#if defined(_AIX) || defined(SOLARIS) || defined(LINUX) || defined(HPUX) || defined(POSIX)
        ctest_setTestDirectory("source|test|testdata|");
#else
        ctest_setTestDirectory("icu|source|test|testdata|");
#endif
    }
    return _testDirectory;
}

void
ctest_setTestDirectory(const char* newDir) 
{
    char newTestDir[256];
    ctest_pathnameInContext(newTestDir, sizeof(newTestDir), newDir); 
    if(_testDirectory != NULL)
        free(_testDirectory);
    _testDirectory = (char*) malloc(sizeof(char*) * (strlen(newTestDir) + 1));
    strcpy(_testDirectory, newTestDir);
}


char *austrdup(const UChar* unichars)
{
    int   length;
    char *newString;

    length    = u_strlen ( unichars );
    newString = (char*)malloc  ( sizeof( char ) * ( length + 1 ) );
 
    if ( newString == NULL )
        return NULL;

    u_austrcpy ( newString, unichars );

    return newString;
}
