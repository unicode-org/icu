/******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  filemode.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000sep28
*   created by: Steven \u24C7 Loomis
*
*   The mode which uses raw files (i.e. does nothing until installation).
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "pkgtypes.h"
#include "makefile.h"

/* The file we will make will look like this:

(where /out is the full path to the output dir)

SOURCES=/out/filea /out/fileb ./somewhere/filec ../somewhereelse/filed

TARGETS=/out/filea /out/fileb /out/filec /out/filed

all: $(TARGETS)

/out/filec /out/filed: ../somewhere/filec ../somewhereelse/filed
      $(INSTALL_DATA) $? $(OUTDIR)

install: all
      $(INSTALL_DATA) $(TARGETS) $(instdir)


==Note:==  
  The only items in the first '$(INSTALL_DATA)' are files NOT already in the out dir!


*/



void pkg_mode_files(UPKGOptions *o, FileStream *makefile, UErrorCode *status)
{
  char tmp[1024], tmp2[1024];
  CharList *tail = NULL, *infiles = NULL;

  CharList *copyFilesLeft = NULL;  /* left hand side of the copy rule*/
  CharList *copyFilesRight = NULL; /* rhs "" "" */

  CharList *copyFilesLeftTail = NULL;
  CharList *copyFilesRightTail = NULL;

  CharList *copyCommands = NULL;

  const char *baseName;

  for(infiles = o->filePaths;infiles;infiles = infiles->next)
  {
    baseName = findBasename(infiles->str);

    uprv_strcpy(tmp, o->targetDir);
    uprv_strcat(tmp, U_FILE_SEP_STRING);
    uprv_strcat(tmp, baseName);
    
    o->outFiles = pkg_appendToList(o->outFiles, &tail, uprv_strdup(tmp));

    if(strcmp(tmp, infiles->str) == 0)
    {
      /* fprintf(stderr, "### NOT copying: %s\n", tmp); */
      /*  no copy needed.. */
      continue;
    }

    uprv_strcpy(tmp2, o->targetDir);
    uprv_strcat(tmp2, U_FILE_SEP_STRING);
    uprv_strcat(tmp2, U_FILE_SEP_STRING);
    uprv_strcat(tmp2, baseName);
    
    if(strcmp(tmp2, infiles->str) == 0)
    {
      /* fprintf(stderr, "### NOT copying: %s\n", tmp2); */
      /*  no copy needed.. */
      continue;
    }
    
    /* left hand side: target path, target name */
    copyFilesLeft = pkg_appendToList(copyFilesLeft, &copyFilesLeftTail, uprv_strdup(tmp));

    /* rhs:  source path */
    copyFilesRight = pkg_appendToList(copyFilesRight, &copyFilesRightTail, uprv_strdup(infiles->str));
    
  }

  if(o->nooutput || o->verbose) {
    CharList *i;
    fprintf(stdout, "# Output files: ");
    for(i = o->outFiles; i; i=i->next) {
      printf("%s ", i->str);
    }
    printf("\n");
  }
  
  if(o->nooutput) {
    *status = U_ZERO_ERROR;
    return;
  }

  /* these are also the files to delete */
  T_FileStream_writeLine(makefile, "COPIEDDEST= ");
  pkg_writeCharListWrap(makefile, copyFilesLeft, " ", " \\\n");
  T_FileStream_writeLine(makefile, "\n");

  
  T_FileStream_writeLine(makefile, "all: $(COPIEDDEST)\n\n");

  /* commands for the make rule */
  tail = NULL;
  copyCommands =  pkg_appendToList(copyCommands, &tail, uprv_strdup("$(INSTALL_DATA) $? $(TARGETDIR)"));
  
  if(copyFilesRight != NULL)
  {

    pkg_mak_writeStanza(makefile, o, "$(COPIEDDEST)", copyFilesRight,
                        copyCommands);
    
    T_FileStream_writeLine(makefile, "clean:\n\t-$(RMV) $(COPIEDDEST) $(MAKEFILE)");
    T_FileStream_writeLine(makefile, "\n\n");

  }
  else
  {
    T_FileStream_writeLine(makefile, "clean:\n\n");
  }
    
  sprintf(tmp, "install: $(COPIEDDEST)\n"
          "\t$(INSTALL-S) $(DATAFILEPATHS) $(INSTALLTO)\n\n");

  T_FileStream_writeLine(makefile, tmp);
}


