/**
 *    File:    mac_gencnval.cpp
 *    
 *    Mac Codewarrior plugin version of gencnval.
 *    
 *    Sean K. Todd (stodd@broadjump.com)
 *    Marty Saxton (msaxton@broadjump.com)
 */

// local headers
#include "mac_utils.h"
//#include "cmemory.h"
//#include "utypes.h"

// Toolbox headers
#include "TextUtils.h"

//#include <string>

// global variables
CWPluginContext gPluginContext;

extern "C" {
int main( int argc, char* argv[]);
//pascal OSErr FSpGetFullPath(const FSSpec *spec, short *fullPathLength, Handle *fullPath);
}


// plugin compiler exports.
#if CW_USE_PRAGMA_EXPORT
#pragma export on
#endif

//    define what file types are handled
// hmm don't want to associate any file extensions with this compiler
// it only works on convrtrs.txt

CWPLUGIN_ENTRY( CWPlugin_GetDefaultMappingList)( const CWExtMapList** defaultMappingList)
{
    static CWExtensionMapping  sExtension = { 'TEXT', ".txt", 0 };
    static CWExtMapList        sExtensionMapList = { kCurrentCWExtMapListVersion, 1, &sExtension };

    *defaultMappingList = &sExtensionMapList;
    return cwNoErr;
}


//    define what operations this plugin handles
CWPLUGIN_ENTRY( CWPlugin_GetDropInFlags)( const DropInFlags** flags, long* flagsSize)
{
    static const DropInFlags sFlags = {
        kCurrentDropInFlagsVersion,
        CWDROPINCOMPILERTYPE,
        DROPINCOMPILERLINKERAPIVERSION,
        ( kGeneratescode | kCompMultiTargAware | kCompAlwaysReload),
        Lang_MISC,
        DROPINCOMPILERLINKERAPIVERSION
    };
    *flags = &sFlags;
    *flagsSize = sizeof(sFlags);
    return cwNoErr;
}

//    define what platforms are supported by this plugin
//
//    '****' - specifies any type
CWPLUGIN_ENTRY( CWPlugin_GetTargetList)( const CWTargetList** targetList)
{
    static CWDataType sCPU = '****';
    static CWDataType sOS = '****';
    static CWTargetList sTargetList = { kCurrentCWTargetListVersion, 1, &sCPU, 1, &sOS };
    *targetList = &sTargetList;
    return cwNoErr;
}

//    define the plugin's onscreen name
CWPLUGIN_ENTRY( CWPlugin_GetDropInName)( const char** dropinName)
{
    static const char* sDropInName = "gencnval";
    *dropinName = sDropInName;
    return cwNoErr;
}

#if CW_USE_PRAGMA_EXPORT
#pragma export off
#endif

static CWResult  Compile( CWPluginContext context)
{
    // get the FileSpec of the file being processed
    CWResult err = CWGetMainFileSpec( context, &gSourceFile);
    if (!CWSUCCESS(err))
        return (err);

    // get the index of the file to process, from the target link order
    long fileNum;
    err = CWGetMainFileNumber( context, &fileNum);
    if (!CWSUCCESS(err))
        return (err);

    // get the name of the source file to compile
    gSourceFileName = p2c_strdup( gSourceFile.name);

    if (gSourceFileName == NULL)
        return cwErrOutOfMemory;
    
    // get the user specified directory from the Target Settings panel (if one exists)
    err = CWGetOutputFileDirectory( gPluginContext, &gOutputFile);

    if (!CWSUCCESS(err)) {
        // else generate the output file into the project target's data directory
        err = CWGetSuggestedObjectFileSpec( context, fileNum, &gOutputFile);

        if (!CWSUCCESS(err))
            return (err);
    }

/*    // set the destination directory
    Handle destH = 0;
    short  len   = 0;

    OSErr theErr = FSpGetFullPath( &gOutputFile, &len, &destH );
    
    char* theDestPath;
    char pathBuffer[1024];

    if( destH ) {

        ::HLock(destH);

        uprv_memcpy(pathBuffer,  ((char*)(*destH)), len);
        pathBuffer[len] = 0;
        theDestPath = pathBuffer;

        ::DisposeHandle(destH);
        destH = 0;
    }


    // get the source directory
    Handle srcH = 0;
    len = 0;

    theErr = FSpGetFullPath( &gSourceFile, &len, &srcH );
    
    char* theSrcPath;
    char srcBuffer[1024];

    if( srcH ) {

        ::HLock(srcH);

        uprv_memcpy(srcBuffer,  ((char*)(*srcH)), len);
        srcBuffer[len] = 0;
        theSrcPath = srcBuffer;

        ::DisposeHandle(srcH);
        srcH = 0;
        
        // truncate file name from full path string
        std::string str = theSrcPath;
        str = str.substr( 0, str.find_last_of( ":" ) + 1);
        std::strcpy(theSrcPath, str.c_str());
    }
*/
    // set the destination directory
    char* theDestPath = "";
    char pathBuffer[1024];
    
    get_path_string( &theDestPath, pathBuffer, &gOutputFile, false);

    // set the source directory
    char* theSrcPath = "";
    char srcBuffer[1024];
    
    get_path_string( &theSrcPath, srcBuffer, &gSourceFile, true);

    int argc = 5;
    char* argv[] = { "gencnval", "-d", theDestPath, "-s", theSrcPath };
    
    if ( setjmp( exit_jump) == 0) {

        // Need to test that we can locate our file here so we don't
        // have to in fopen(). That way, we won't get error messages
        // from makeconv.c when .dat files (that we don't care about) 
        // aren't found.
//        err = LocateFile(gPluginContext, gSourceFileName, gSourceFile);

        if ( main( argc, argv) != 0)
            err = cwErrRequestFailed;
    }
    else {
        // evidently the good old exit function got called.
        if ( exit_status != 0)
            err = cwErrRequestFailed;
    }
    
    // if the compilation succeeded, tell CodeWarrior about the output file.
    // this ensures several things:  1. if the output file is deleted by the user,
    // then the IDE will know to recompile it, which is good for dirty builds,
    // where the output files may be hand deleted; 2. if the user elects to remove
    // objects, the output files are deleted. Thanks to robv@metrowerks.com for
    // pointing this new CWPro4 API out.
    if ( err == cwNoErr) {
        CWObjectData objectData;
        ::BlockZero( &objectData, sizeof( objectData));
        
        // for fun, show how large the output file is in the data area.
        long dataSize, rsrcSize;
        if (FSpGetFileSize( &gOutputFile, &dataSize, &rsrcSize) == noErr)
            objectData.idatasize = dataSize;
        
        // tell the IDE that this file was generated by the compiler.
        objectData.objectfile = &gOutputFile;
        
        err = CWStoreObjectData( context, fileNum, &objectData);
    }
    else {
        // an error occured, delete the output file, which might be a partial file.
        if ( gOutputFile.name[0] != 0) {
            ::FSpDelete(&gOutputFile);
        }
    }

    delete[] gSourceFileName;
    gSourceFileName = NULL;
    return (err);
}


// main entry-point for makeconv compiler plugin
pascal short  gencnval_compiler( CWPluginContext context)
{
    short        result = cwNoErr;
    long        request;
    
    // Get the value indicating the task the IDE is currently asking the
    // plugin to perform and make sure no error was evoked.
    if ( CWGetPluginRequest( context, &request) != cwNoErr)
        return cwErrRequestFailed;
    
    gPluginContext = context;
    result = cwNoErr;
        
    // dispatch on compiler request
    switch (request)
    {
    case reqInitCompiler: break;        // compiler has just been loaded into memory
        
    case reqTermCompiler:  break;      // compiler is about to be unloaded from memory
        
    case reqCompile:
        /* compile a source file */
        result = Compile( context);
        break;
        
    default:  
        result = cwErrRequestFailed;
        break;
    }
    
    gPluginContext = 0;
    return (result);
}

