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
#include "uchar.h"
#include "ustring.h"

static char* _testDirectory=NULL;
int main ( int argc, char **argv )
{
      
  
  TestNode *root = NULL;
  addAllTests(&root);
  processArgs(root, argc, argv);
 

   return 0;

    
}

void 
ctest_pathnameInContext( char* fullname, int32_t maxsize, const char* relPath ) 
{
    char* mainDir;
    char  sepChar;

    const char inpSepChar = '|';
    char* tmp;
    char sepString[2] ;
    int32_t lenMainDir;
    int32_t lenRelPath ;   

#if defined(_WIN32) || defined(WIN32) || defined(__OS2__) || defined(OS2)
        char mainDirBuffer[200];
        mainDir = getenv("ICU_DATA");
        if(mainDir!=NULL) {
            strcpy(mainDirBuffer, mainDir);
            strcat(mainDirBuffer, "..\\..");
        } else {
            mainDirBuffer[0]='\0';
        }
        mainDir=mainDirBuffer;
        sepChar = '\\';
#elif defined(_AIX) || defined(SOLARIS) || defined(LINUX) || defined(HPUX)
        mainDir = getenv("HOME");
        sepChar = '/';
    
#elif defined(XP_MAC)
        Str255 volName;
        int16_t volNum;
        OSErr err = GetVol( volName, &volNum );
        if (err != noErr) volName[0] = 0;
        mainDir = (char*) &(volName[1]);
        mainDir[volName[0]] = 0;
        sepChar = ':';
#else
        mainDir = "";
        sepChar = '/';
#endif
    sepString[0] = sepChar;
    sepString[1] = 0;
    if (relPath[0] == '|') relPath++;

    lenMainDir = strlen( mainDir );
    
      lenRelPath = strlen( relPath );
    if (maxsize < lenMainDir + lenRelPath + 2) { fullname[0] = 0; return; }
    strcpy( fullname, mainDir );
    strcat( fullname, sepString );
    strcat( fullname, relPath );
    strchr( fullname, inpSepChar );
    tmp = strchr(fullname, inpSepChar);
    while (tmp) {
        *tmp = sepChar;
        tmp = strchr( tmp+1, inpSepChar );
    }
}
const char*
ctest_getTestDirectory()
{
    if (_testDirectory == NULL) 
    {
      ctest_setTestDirectory("icu|source|test|testdata|");
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
