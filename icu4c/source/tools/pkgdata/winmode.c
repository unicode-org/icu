/******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  winmode.c
*   encoding:   ANSI X3.4 (1968)
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000july14
*   created by: Vladimir Weinstein
*
*   This program packages the ICU data into different forms
*   (DLL, common data, etc.) 
*/

#include <stdio.h>
#include <stdlib.h>
#include "unicode/utypes.h"
#include "unicode/putil.h"
#include "cmemory.h"
#include "cstring.h"
#include "filestrm.h"
#include "toolutil.h"
#include "unewdata.h"
#include "uoptions.h"
#include "pkgtypes.h"
#include "makefile.h"

#define WINBUILDMODE (*(o->options)=='R'?"Release":"Debug")

void writeCmnRules(UPKGOptions *o,  FileStream *makefile, CharList **objects)
{
  char tmp[1024];
  CharList *oTail = NULL;
  CharList *infiles;
  CharList *parents = NULL, *commands = NULL;

  infiles = o->filePaths;

  sprintf(tmp, "$(TARGETDIR)\\$(CMNTARGET) : $(DATAFILEPATHS)\n\t@$(GENCMN) -C \"%s\" -d %s -n %s 1000000 <<\n", 
	  o->comment, o->targetDir, o->shortName);
    T_FileStream_writeLine(makefile, tmp);

  for(;infiles;infiles = infiles->next) {
	  sprintf(tmp, "%s\n", infiles->str);
	  T_FileStream_writeLine(makefile, tmp);
  }
  sprintf(tmp, "<<\n");
  T_FileStream_writeLine(makefile, tmp);
}



void pkg_mode_windows(UPKGOptions *o, FileStream *makefile, UErrorCode *status) {
  char tmp[1024];
  char tmp2[1024];
  CharList *tail = NULL;
  CharList *objects = NULL;
  const char *separator = o->icuroot[uprv_strlen(o->icuroot)-1]=='\\'?"":"\\";
  UBool isDll = (uprv_strcmp(o->mode, "dll") == 0);

  if(U_FAILURE(*status)) { 
    return;
  }

  sprintf(tmp2, "ICUROOT=%s\n\n", o->icuroot);
  T_FileStream_writeLine(makefile, tmp2);

  sprintf(tmp2,
	  "GENCMN = $(ICUROOT)%ssource\\tools\\gencmn\\%s\\gencmn.exe\n",
	  separator, WINBUILDMODE);
  T_FileStream_writeLine(makefile, tmp2);

  if(isDll) {
      uprv_strcpy(tmp, LIB_PREFIX);
      uprv_strcat(tmp, o->shortName);
      uprv_strcat(tmp, UDATA_SO_SUFFIX);

      if(o->nooutput || o->verbose) {
        fprintf(stderr, "# Output %s file: %s%s%s\n", UDATA_SO_SUFFIX, o->targetDir, U_FILE_SEP_STRING, tmp);
      }

      if(o->nooutput) {
        *status = U_ZERO_ERROR;
        return;
      }

      sprintf(tmp2, "# DLL file to make:\nDLLTARGET=%s\n\n", tmp);
      T_FileStream_writeLine(makefile, tmp2);

	  sprintf(tmp2, 
		  "LINK32 = link.exe\n"
		  "LINK32_FLAGS = /out:\"$(TARGETDIR)\\$(DLLTARGET)\" /DLL /NOENTRY /base:\"0x4ad00000\" /comment:\"%s\"\n",
		  o->comment
		);
      T_FileStream_writeLine(makefile, tmp2);

	  sprintf(tmp2,
		  "GENCCODE = $(ICUROOT)%ssource\\tools\\genccode\\%s\\genccode.exe\n",
		  separator, WINBUILDMODE);
      T_FileStream_writeLine(makefile, tmp2);



      uprv_strcpy(tmp, UDATA_CMN_PREFIX);
      uprv_strcat(tmp, o->shortName);
      uprv_strcat(tmp, UDATA_CMN_INTERMEDIATE_SUFFIX);
      uprv_strcat(tmp, OBJ_SUFFIX);

      sprintf(tmp2, "# intermediate obj file:\nCMNOBJTARGET=%s\n\n", tmp);
      T_FileStream_writeLine(makefile, tmp2);
  }
  uprv_strcpy(tmp, UDATA_CMN_PREFIX);
  uprv_strcat(tmp, o->shortName);
  uprv_strcat(tmp, UDATA_CMN_SUFFIX);
  
   

	if(o->nooutput || o->verbose) {
	  fprintf(stderr, "# Output file: %s%s%s\n", o->targetDir, U_FILE_SEP_STRING, tmp);
	}

	if(o->nooutput) {
	  *status = U_ZERO_ERROR;
	  return;
	}

    sprintf(tmp2, "# common file to make:\nCMNTARGET=%s\n\n", tmp);
    T_FileStream_writeLine(makefile, tmp2);


  if(isDll) {
      sprintf(tmp, "all: $(TARGETDIR)\\$(DLLTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp);

      sprintf(tmp, "$(TARGETDIR)\\$(DLLTARGET): $(TARGETDIR)\\$(CMNOBJTARGET)\n"
				    "\t@$(LINK32) $(LINK32_FLAGS) $(TARGETDIR)\\$(CMNOBJTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp);
      sprintf(tmp, "$(TARGETDIR)\\$(CMNOBJTARGET): $(TARGETDIR)\\$(CMNTARGET)\n"
				    "\t@$(GENCCODE) $(GENCOPTIONS) -o -d $(TARGETDIR) $(TARGETDIR)\\$(CMNTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp);

      sprintf(tmp2, 
          "clean:\n"
          "\t-@erase $(TARGETDIR)\\$(DLLTARGET)\n"
          "\t-@erase $(TARGETDIR)\\$(CMNOBJTARGET)\n"
          "\t-@erase $(TARGETDIR)\\$(CMNTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp2);
  } else {

      sprintf(tmp, "all: $(TARGETDIR)\\$(CMNTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp);

      sprintf(tmp2, 
          "clean:\n"
          "\t-@erase $(TARGETDIR)\\$(CMNTARGET)\n\n");
      T_FileStream_writeLine(makefile, tmp2);
  }

  sprintf(tmp2, "rebuild: clean all\n\n");
  T_FileStream_writeLine(makefile, tmp2);

	/* Write compile rules */
  writeCmnRules(o, makefile, &objects);
}

