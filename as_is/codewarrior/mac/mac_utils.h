/**
 *    File:    mac_utils.h
 *    
 *    Functions needed for Macintosh versions of command-line compilers
 *
 *    Sean K. Todd (stodd@broadjump.com)
 *    Marty Saxton (msaxton@broadjump.com)
 */
    

// Toolbox headers
#include <Files.h>
#include <MacTypes.h>

// MSL headers
#include <stdlib.h>
#include <setjmp.h>

// CodeWarrior Plugin SDK headers
#include <CWPlugins.h>
#include <CWPLuginErrors.h>
#include <DropInCompilerLinker.h>
#include <CompilerMapping.h>

// external variables
extern jmp_buf     exit_jump;
extern int         exit_status;
extern char*       gSourceFileName;
extern char*       gDestPath;
extern CWFileSpec  gSourceFile;
extern CWFileSpec  gOutputFile;


void             get_path_string( char**       oPath, 
                                  char         oPathBuffer[],
                                  CWFileSpec*  iFileSpec, 
                                  bool         doTruncate
                                );

pascal  OSErr    FSpGetFileSize( const FSSpec *spec, long *dataSize, long *rsrcSize);

char*            p2c_strdup( StringPtr pstr);

void             std::exit( int status);


//CWResult LocateFile(CWPluginContext context, const char* filename, FSSpec& file);
//StringPtr    c2p_strcpy(StringPtr pstr, const char* cstr);
