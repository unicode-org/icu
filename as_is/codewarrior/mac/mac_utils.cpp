/**
 *    File:    mac_utils.cpp
 *    
 *  Routines
 *  --------
 *
 *    get_path_string
 *    FSpGetFileSize
 *    p2c_strdup
 *    exit
 *
 *  Description
 *  -----------
 *
 *    Contains functions needed for Macintosh versions of command-line compilers
 *
 *  Authors
 *  -------
 *
 *    Sean K. Todd (stodd@broadjump.com)
 *    Marty Saxton (msaxton@broadjump.com)
 */    

#include "mac_utils.h"

// icu headers
#include "cmemory.h"
#include "utypes.h"

// msl headers
#include <string>


extern "C" pascal OSErr  FSpGetFullPath( const FSSpec *spec, short *fullPathLength, Handle *fullPath);

CWFileSpec  gOutputFile;
CWFileSpec  gSourceFile;
char*       gDestPath        = NULL;
char*       gSourceFileName  = NULL;

/**
 *    get_path_string
 *
 *    Creates a string with the full path (with or without the file name) from
 *    a FSSpec. Command-line tools expect full paths but Macs deal with files
 *    via filespecs. The limitation to using full paths is that it is possible
 *    to have two mounted volumes with the same name on a Mac. Filespecs can
 *    distinguish between the two (by using volume reference numbers) but full
 *    paths cannot.
 *
 */
 
void get_path_string( char**       oPath, 
                      char         oPathBuffer[],
                      CWFileSpec*  iFileSpec, 
                      bool         doTruncate
                    )
{
    Handle  theHandle = 0;
    short    len = 0;

    OSErr  theErr = FSpGetFullPath( iFileSpec, &len, &theHandle );

    if ( theHandle && !theErr) {

        ::HLock( theHandle);

        uprv_memcpy( oPathBuffer, ( static_cast< char*>( *theHandle)), len);
        oPathBuffer[ len ] = 0;
        *oPath = oPathBuffer;

        ::DisposeHandle( theHandle);
        theHandle = 0;
        
        if ( doTruncate) {
            // truncate file name from full path string
            std::string str = *oPath;
            str = str.substr( 0, str.find_last_of( ":" ) + 1);
            std::strcpy( *oPath, str.c_str());
        }
    }
}

/**
 *
 *    FSpGetFileSize
 *
 *    This function code was derived from code in MoreFilesExtras.h which is part 
 *    of 'MoreFiles' (Apple Sample Code).
 *
 */

pascal OSErr  FSpGetFileSize( const FSSpec* spec, long* dataSize, long* rsrcSize)
{
    HParamBlockRec pb;
    
    pb.fileParam.ioNamePtr   = const_cast< StringPtr>( spec->name);
    pb.fileParam.ioVRefNum   = spec->vRefNum;
    pb.fileParam.ioFVersNum  = 0;
    pb.fileParam.ioDirID     = spec->parID;
    pb.fileParam.ioFDirIndex = 0;

    OSErr error = PBHGetFInfoSync( &pb);

    if ( noErr == error ) {
        *dataSize  = pb.fileParam.ioFlLgLen;
        *rsrcSize  = pb.fileParam.ioFlRLgLen;
    }
    
    return ( error );
}

/**
 *    p2c_strdup
 *
 *    Creates a duplicate c string from a pascal string.
 *
 *    NOTE:  The user is responsible for calling delete[]
 *           when he/she is finished using the string in
 *           order to prevent a memory leak.
 *
 */

char* p2c_strdup( StringPtr pstr)
{
    size_t len  = pstr[0];
    char*  cstr = new char[1 + len];

    if ( cstr != NULL) {
        ::BlockMoveData( pstr + 1, cstr, len);
        cstr[len] = '\0';
    }
    return cstr;
}

/**
 *    exit
 *
 *    simply throw us out of here!
 */

jmp_buf  exit_jump;
int      exit_status = 0;

void std::exit( int status)
{
    exit_status = status;
    longjmp( exit_jump, -1);
}
 
/*****************************************************************************/
/*
StringPtr c2p_strcpy( StringPtr pstr, const char* cstr)
{
    size_t len = ::strlen(cstr);
    if (len > 255) len = 255;
    BlockMoveData(cstr, pstr + 1, len);
    pstr[0] = len;
    return pstr;
}
*/
/*****************************************************************************/
/*
CWResult LocateFile(CWPluginContext context, const char* filename, FSSpec& file)
{
    // prefill the CWFileInfo struct
    CWFileInfo fileinfo;
    BlockZero(&fileinfo, sizeof(fileinfo));
    // memset(&fileinfo, 0, sizeof(fileinfo));
    fileinfo.fullsearch = true;
    fileinfo.suppressload = true;
    fileinfo.dependencyType = cwNormalDependency;
    fileinfo.isdependentoffile = kCurrentCompiledFile;

    // locate the file name using the project's access paths
    CWResult err = CWFindAndLoadFile(context, filename, &fileinfo);
    if (err == cwNoErr) {
        file = fileinfo.filespec;
    } else if (err == cwErrFileNotFound) {
        char errmsg[200];
        sprintf(errmsg, "Can't locate file \"%s\".", filename);
        CWResult callbackResult = CWReportMessage(context, 0, errmsg, 0, messagetypeError, 0);
    }
    
    return (err);
}
*/
/**
 * Substitute for standard fopen, treats certain filenames specially,
 * and also considers the mode argument. If a file is being opened
 * for reading, the file is assumed to be locateable using CodeWarrior's
 * standard access paths. If it's for writing, the file is opened in
 * the current project's output directory.
 */
/* 
extern "C" FILE * FSp_fopen(ConstFSSpecPtr spec, const char * open_mode);
 
 
FILE* std::fopen(const char* filename, const char *mode)
{
    FSSpec filespec;
    CWResult err = noErr;
    do {
        if (filename == gSourcePath || strcmp(filename, gSourcePath) == 0) {
            // opening the main source file.
            filespec = gSourceFile;
        } else if (mode[0] == 'w') {
            // if an output file, open it in the current compilation's output directory.
            c2p_strcpy(filespec.name, filename);
            filespec.vRefNum = gOutputFile.vRefNum;
            filespec.parID = gOutputFile.parID;
            c2p_strcpy(gOutputFile.name, filename);
        } else {
            // an input file, use CodeWarrior's search paths to find the named source file.
//            err = LocateFile(gPluginContext, filename, filespec);
        }
    } while (0);
    // if all went well, we have a file to open.
    return (err == noErr ? FSp_fopen(&filespec, mode) : NULL);
}
*/