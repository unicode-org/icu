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

#ifdef _WIN32
        mainDir = "";   /*  The "intlwork" directory is part of the relative path*/
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


const char* myErrorName(UErrorCode status)
{
      switch (status)
    {
    case ZERO_ERROR:                return "ZERO_ERROR";
    case ILLEGAL_ARGUMENT_ERROR:    return "ILLEGAL_ARGUMENT_ERROR";
    case MISSING_RESOURCE_ERROR:    return "MISSING_RESOURCE_ERROR";
    case INVALID_FORMAT_ERROR:      return "INVALID_FORMAT_ERROR";
    case FILE_ACCESS_ERROR:         return "FILE_ACCESS_ERROR";
    case INTERNAL_PROGRAM_ERROR:    return "INTERNAL_PROGRAM_ERROR";
    case MESSAGE_PARSE_ERROR:       return "MESSAGE_PARSE_ERROR";
    case MEMORY_ALLOCATION_ERROR:   return "MEMORY_ALLOCATION_ERROR";
    case PARSE_ERROR:               return "PARSE_ERROR";
    case INVALID_CHAR_FOUND:        return "INVALID_CHAR_FOUND";
    case TRUNCATED_CHAR_FOUND:        return "TRUNCATED_CHAR_FOUND";
    case ILLEGAL_CHAR_FOUND:        return "ILLEGAL_CHAR_FOUND";
    case INVALID_TABLE_FORMAT:        return "INVALID_TABLE_FORMAT";
    case INVALID_TABLE_FILE:        return "INVALID_TABLE_FILE";
    case BUFFER_OVERFLOW_ERROR:        return "BUFFER_OVERFLOW_ERROR";
    case USING_FALLBACK_ERROR:        return "USING_FALLBACK_ERROR";
    case USING_DEFAULT_ERROR:        return "USING_DEFAULT_ERROR";
    default:                        return "[BOGUS ErrorCode]";
    }
}

